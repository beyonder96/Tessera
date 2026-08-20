package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object MetroCptmApi {
    private const val TAG = "MetroCptmApi"
    private const val ARTESP_WEB_URL = "https://ccm.artesp.sp.gov.br/metroferroviario/status-linhas/"
    private const val ARTESP_API_URL = "https://ccm.artesp.sp.gov.br/metroferroviario/api/status/"
    private const val CPTM_API_URL = "https://api.cptm.sp.gov.br/AppCPTM/v1/Linhas/ObterStatus"

    private const val CACHE_TTL_MS = 60_000L // 60 segundos de cache para evitar rate-limit (429)

    private var cachedStatuses: List<MetroEmpresaStatus>? = null
    private var lastFetchTimestamp: Long = 0L

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    // Configuração estrutural padrão de todas as empresas e linhas
    val defaultEmpresas = listOf(
        MetroEmpresaConfig(
            id = 1,
            nome = "Metrô de São Paulo",
            fiscalizacaoArtesp = false,
            linhas = listOf(
                MetroLinhaConfig("Azul", "1"),
                MetroLinhaConfig("Verde", "2"),
                MetroLinhaConfig("Vermelha", "3"),
                MetroLinhaConfig("Prata", "15")
            )
        ),
        MetroEmpresaConfig(
            id = 2,
            nome = "ViaQuatro / ViaMobilidade",
            fiscalizacaoArtesp = true,
            linhas = listOf(
                MetroLinhaConfig("Amarela", "4"),
                MetroLinhaConfig("Lilás", "5"),
                MetroLinhaConfig("Diamante", "8"),
                MetroLinhaConfig("Esmeralda", "9")
            )
        ),
        MetroEmpresaConfig(
            id = 3,
            nome = "CPTM / TIC Trens",
            fiscalizacaoArtesp = true,
            linhas = listOf(
                MetroLinhaConfig("Rubi", "7"),
                MetroLinhaConfig("Turquesa", "10"),
                MetroLinhaConfig("Coral", "11"),
                MetroLinhaConfig("Safira", "12"),
                MetroLinhaConfig("Jade", "13")
            )
        )
    )

    /**
     * Busca o status em tempo real de todas as linhas de metrô e trens de São Paulo.
     * 1. Usa cache se dentro do TTL de 60s (a menos que forceRefresh seja true).
     * 2. Se apiKey for fornecida, tenta a API REST oficial do CCM ARTESP.
     * 3. Scraping ao vivo da página oficial da ARTESP via Jsoup.
     * 4. Enriquecimento/fallback com a API oficial da CPTM.
     */
    suspend fun getLiveMetroAndTrainStatus(
        apiKey: String? = null,
        forceRefresh: Boolean = false
    ): List<MetroEmpresaStatus> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedStatuses != null && (now - lastFetchTimestamp < CACHE_TTL_MS)) {
            return@withContext cachedStatuses!!
        }

        val statusMap = mutableMapOf<String, MetroLinhaStatusDetail>()
        val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // 1. Tenta API REST Oficial da ARTESP se chave informada
        if (!apiKey.isNullOrBlank()) {
            try {
                val apiStatuses = fetchFromArtespRestApi(apiKey)
                if (apiStatuses.isNotEmpty()) {
                    statusMap.putAll(apiStatuses)
                    Log.d(TAG, "Status obtido via ARTESP REST API (${apiStatuses.size} linhas)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha na API REST da ARTESP com chave: ${e.message}")
            }
        }

        // 2. Scraping ao vivo da página pública do Portal CCM ARTESP (13 linhas)
        if (statusMap.size < 13) {
            try {
                val scrapedStatuses = scrapeFromArtespWeb(currentTimeStr)
                if (scrapedStatuses.isNotEmpty()) {
                    // Preenche linhas que ainda não foram obtidas ou sobrescreve com dados do portal
                    scrapedStatuses.forEach { (code, detail) ->
                        if (!statusMap.containsKey(code)) {
                            statusMap[code] = detail
                        }
                    }
                    Log.d(TAG, "Status obtido via Scraping ARTESP Web (${scrapedStatuses.size} linhas)")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao ler portal web da ARTESP: ${e.message}")
            }
        }

        // 3. Consulta API Oficial da CPTM (para complementar/enriquecer linhas da CPTM)
        try {
            val cptmStatuses = fetchFromCptmApi(currentTimeStr)
            if (cptmStatuses.isNotEmpty()) {
                cptmStatuses.forEach { (code, detail) ->
                    // Se não tiver status ou se o status CPTM for mais recente / específico
                    if (!statusMap.containsKey(code) || (statusMap[code]?.operacaoNormal == true && !detail.operacaoNormal)) {
                        statusMap[code] = detail
                    }
                }
                Log.d(TAG, "Status CPTM atualizado via API CPTM Oficial (${cptmStatuses.size} linhas)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Falha na API CPTM: ${e.message}")
        }

        // Se nenhuma das fontes retornou dados e não temos cache
        if (statusMap.isEmpty()) {
            if (cachedStatuses != null) {
                Log.w(TAG, "Rede inacessível, retornando cache anterior.")
                return@withContext cachedStatuses!!
            }
            throw IOException("Não foi possível obter o status ao vivo das linhas. Verifique sua conexão com a internet.")
        }

        // Constrói a lista estruturada das 3 empresas com os status obtidos
        val result = defaultEmpresas.map { empresa ->
            val linhasStatus = empresa.linhas?.map { linha ->
                val detail = statusMap[linha.codigo] ?: MetroLinhaStatusDetail(
                    situacao = "Operação Normal",
                    classificacao = "Normal",
                    operacaoNormal = true,
                    atualizadoEm = null,
                    atualizadoHa = "Atualizado às $currentTimeStr"
                )
                MetroLinhaStatus(
                    nome = linha.nome,
                    codigo = linha.codigo,
                    ativa = true,
                    status = detail
                )
            }
            MetroEmpresaStatus(
                id = empresa.id,
                nome = empresa.nome,
                fiscalizacaoArtesp = empresa.fiscalizacaoArtesp,
                linhas = linhasStatus
            )
        }

        cachedStatuses = result
        lastFetchTimestamp = System.currentTimeMillis()
        result
    }

    /**
     * Scraping ao vivo da página HTML oficial da ARTESP: https://ccm.artesp.sp.gov.br/metroferroviario/status-linhas/
     */
    private fun scrapeFromArtespWeb(currentTimeStr: String): Map<String, MetroLinhaStatusDetail> {
        val result = mutableMapOf<String, MetroLinhaStatusDetail>()

        val doc = Jsoup.connect(ARTESP_WEB_URL)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
            .timeout(14_000)
            .get()

        // Localiza todos os cards de linhas
        val cards = doc.select("div.bg-white.rounded-xl.border")
        for (card in cards) {
            val titleEl = card.selectFirst("h3") ?: continue
            val titleText = titleEl.text().trim() // ex: "Linha 7-Rubi", "Linha 1-Azul", "Linha 15-Prata"

            val lineCode = extractLineCode(titleText) ?: continue

            // Badge de status no canto superior direito
            val statusBadgeEl = card.selectFirst("div.flex-shrink-0 span")
            val badgeText = statusBadgeEl?.text()?.trim() ?: "Operação Normal"
            val badgeClass = statusBadgeEl?.className() ?: ""

            // Bloco de Situação detalhada
            val situacaoEl = card.selectFirst("div.bg-gray-50 strong:matchesOwn(Situa)")?.parent()
            var situacaoText = situacaoEl?.text()?.replace("Situação:", "")?.trim() ?: badgeText
            if (situacaoText.isBlank()) situacaoText = badgeText

            // Atualizado há
            val updatedEl = card.selectFirst("span:contains(Atualizado)")
            val updatedText = updatedEl?.text()?.trim() ?: "Atualizado às $currentTimeStr"

            // Normalidade
            val isNormal = (badgeClass.contains("bg-green") || badgeText.contains("Normal", ignoreCase = true)) &&
                    !situacaoText.contains("Reduzida", ignoreCase = true) &&
                    !situacaoText.contains("Parcial", ignoreCase = true) &&
                    !situacaoText.contains("Paralisada", ignoreCase = true) &&
                    !situacaoText.contains("Interrompida", ignoreCase = true)

            result[lineCode] = MetroLinhaStatusDetail(
                situacao = situacaoText,
                classificacao = if (isNormal) "Normal" else badgeText,
                operacaoNormal = isNormal,
                atualizadoEm = null,
                atualizadoHa = updatedText
            )
        }

        return result
    }

    /**
     * Consulta a API REST oficial da CPTM: https://api.cptm.sp.gov.br/AppCPTM/v1/Linhas/ObterStatus
     */
    private fun fetchFromCptmApi(currentTimeStr: String): Map<String, MetroLinhaStatusDetail> {
        val request = Request.Builder()
            .url(CPTM_API_URL)
            .header("User-Agent", "Tessera/2.0 (Android)")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyMap()

        val jsonStr = response.body?.string() ?: return emptyMap()
        val jsonArray = JSONArray(jsonStr)
        val result = mutableMapOf<String, MetroLinhaStatusDetail>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val linhaId = obj.optInt("linhaId", 0)
            if (linhaId <= 0) continue

            val status = obj.optString("status", "Operação Normal").trim()
            val descricao = obj.optString("descricao", "").trim()
            val dataGeracao = obj.optString("dataGeracao", null)

            val fullSituacao = if (descricao.isNotBlank() && !descricao.equals(status, ignoreCase = true)) {
                "$status - $descricao"
            } else {
                status
            }

            val isNormal = status.contains("Normal", ignoreCase = true)

            result[linhaId.toString()] = MetroLinhaStatusDetail(
                situacao = fullSituacao,
                classificacao = if (isNormal) "Normal" else status,
                operacaoNormal = isNormal,
                atualizadoEm = dataGeracao,
                atualizadoHa = "Atualizado às $currentTimeStr"
            )
        }

        return result
    }

    /**
     * Consulta a API REST Oficial da ARTESP (quando o usuário possui API Key)
     */
    private fun fetchFromArtespRestApi(apiKey: String): Map<String, MetroLinhaStatusDetail> {
        val request = Request.Builder()
            .url(ARTESP_API_URL)
            .header("Authorization", "Api-Key $apiKey")
            .header("User-Agent", "Tessera/2.0 (Android)")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyMap()

        val jsonStr = response.body?.string() ?: return emptyMap()
        val rootObj = JSONObject(jsonStr)
        val empresasArray = rootObj.optJSONArray("empresas") ?: return emptyMap()
        val result = mutableMapOf<String, MetroLinhaStatusDetail>()

        for (i in 0 until empresasArray.length()) {
            val emp = empresasArray.getJSONObject(i)
            val linhasArr = emp.optJSONArray("linhas") ?: continue
            for (j in 0 until linhasArr.length()) {
                val linha = linhasArr.getJSONObject(j)
                val codigo = linha.optString("codigo", "")
                val statusObj = linha.optJSONObject("status")
                if (statusObj != null && codigo.isNotEmpty()) {
                    val situacao = statusObj.optString("situacao", "Operação Normal")
                    val isNormal = statusObj.optBoolean("operacao_normal", situacao.contains("Normal", ignoreCase = true))
                    result[codigo] = MetroLinhaStatusDetail(
                        situacao = situacao,
                        classificacao = statusObj.optString("classificacao", if (isNormal) "Normal" else "Alerta"),
                        operacaoNormal = isNormal,
                        atualizadoEm = statusObj.optString("atualizado_em", null),
                        atualizadoHa = statusObj.optString("atualizado_ha", "Agora")
                    )
                }
            }
        }
        return result
    }

    /**
     * Extrai o número/código da linha a partir de nomes como "Linha 7-Rubi", "Linha 15 - Prata", "Linha 1"
     */
    private fun extractLineCode(title: String): String? {
        val regex = Regex("""Linha\s+(\d+)""", RegexOption.IGNORE_CASE)
        val match = regex.find(title)
        return match?.groupValues?.getOrNull(1)
    }
}

