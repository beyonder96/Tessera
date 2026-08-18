package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DiretoDosTrensLineStatus(
    val codigo: Int,
    val nome: String,
    val situacao: String,
    val descricao: String?,
    val tipo: String?, // "Metrô" ou "CPTM"
    val modificado: String?
)

object MetroCptmApi {
    private const val TAG = "MetroCptmApi"
    private const val DIRETO_TRENS_URL = "https://api.diretodostrens.com.br/status"
    private const val ARTESP_URL = "https://ccm.artesp.sp.gov.br/metroferroviario/api/status/"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    // Configuração estática de todas as empresas e linhas
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
            fiscalizacaoArtesp = false,
            linhas = listOf(
                MetroLinhaConfig("Amarela", "4"),
                MetroLinhaConfig("Lilás", "5"),
                MetroLinhaConfig("Diamante", "8"),
                MetroLinhaConfig("Esmeralda", "9")
            )
        ),
        MetroEmpresaConfig(
            id = 3,
            nome = "CPTM",
            fiscalizacaoArtesp = false,
            linhas = listOf(
                MetroLinhaConfig("Rubi", "7"),
                MetroLinhaConfig("Turquesa", "10"),
                MetroLinhaConfig("Coral", "11"),
                MetroLinhaConfig("Safira", "12"),
                MetroLinhaConfig("Jade", "13")
            )
        )
    )

    suspend fun getLiveMetroAndTrainStatus(): List<MetroEmpresaStatus> = withContext(Dispatchers.IO) {
        val statusMap = mutableMapOf<String, MetroLinhaStatusDetail>()
        val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        // 1. Tenta Direto dos Trens API
        try {
            val ddtStatuses = fetchFromDiretoDosTrens()
            if (ddtStatuses.isNotEmpty()) {
                ddtStatuses.forEach { item ->
                    val isNormal = item.situacao.contains("Normal", ignoreCase = true)
                    val detail = MetroLinhaStatusDetail(
                        situacao = item.situacao,
                        classificacao = if (isNormal) "Normal" else "Alerta",
                        operacaoNormal = isNormal,
                        atualizadoEm = item.modificado,
                        atualizadoHa = "Atualizado às $currentTimeStr"
                    )
                    statusMap[item.codigo.toString()] = detail
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Direto dos Trens não respondeu ou exigiu token: ${e.message}")
        }

        // 2. Se vazio, tenta ARTESP CCM API
        if (statusMap.isEmpty()) {
            try {
                val artespStatuses = fetchFromArtesp()
                if (artespStatuses.isNotEmpty()) {
                    artespStatuses.forEach { (code, detail) ->
                        statusMap[code] = detail
                    }
                }
            } catch (e2: Exception) {
                Log.d(TAG, "Artesp API status: ${e2.message}")
            }
        }

        // Constrói a lista estruturada das 3 empresas com os status obtidos (ou default Operação Normal)
        defaultEmpresas.map { empresa ->
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
                fiscalizacaoArtesp = false,
                linhas = linhasStatus
            )
        }
    }

    private fun fetchFromDiretoDosTrens(): List<DiretoDosTrensLineStatus> {
        val requestBuilder = Request.Builder()
            .url(DIRETO_TRENS_URL)
            .header("User-Agent", "Tessera/2.0 (Android)")
            .header("Accept", "application/json")

        val response = httpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) return emptyList()

        val jsonStr = response.body?.string() ?: return emptyList()
        val jsonArray = JSONArray(jsonStr)
        val list = mutableListOf<DiretoDosTrensLineStatus>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.has("erro")) continue

            val codigo = obj.optInt("codigo", 0)
            val nome = obj.optString("nome", "")
            val situacao = obj.optString("situacao", obj.optString("status", "Operação Normal"))
            val descricao = obj.optString("descricao", null)
            val tipo = obj.optString("tipo", null)
            val modificado = obj.optString("modificado", null)

            list.add(
                DiretoDosTrensLineStatus(
                    codigo = codigo,
                    nome = nome,
                    situacao = situacao,
                    descricao = descricao,
                    tipo = tipo,
                    modificado = modificado
                )
            )
        }
        return list
    }

    private fun fetchFromArtesp(): Map<String, MetroLinhaStatusDetail> {
        val request = Request.Builder()
            .url(ARTESP_URL)
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
}
