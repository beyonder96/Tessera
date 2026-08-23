package com.example.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.regex.Pattern

data class ParsedStatementTransaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val dateFormatted: String,
    val timestamp: Long,
    val amount: Double,
    val isIncome: Boolean,
    val category: String,
    var isSelected: Boolean = true
)

object BankStatementPdfParser {
    private const val TAG = "BankStatementParser"

    private data class MoneyMatch(
        val rawText: String,
        val indicator: String?,
        val amount: Double,
        val isNegativeRaw: Boolean,
        val isPositiveRaw: Boolean,
        val startIndex: Int,
        val endIndex: Int
    )

    suspend fun parsePdfStatement(context: Context, uri: Uri): List<ParsedStatementTransaction> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<ParsedStatementTransaction>()
        try {
            PDFBoxResourceLoader.init(context)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val stripper = PDFTextStripper()
                    val rawText = stripper.getText(document)
                    val lines = rawText.lines()
                    resultList.addAll(extractTransactionsFromLines(lines))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar extrato em PDF", e)
        }
        return@withContext resultList
    }

    fun extractTransactionsFromLines(lines: List<String>): List<ParsedStatementTransaction> {
        val transactions = mutableListOf<ParsedStatementTransaction>()

        // Padrões de Data comuns em extratos brasileiros: DD/MM/YYYY, DD/MM/YY, DD/MM
        val dateRegex = Pattern.compile("""\b(\d{1,2})[/.-](\d{1,2})(?:[/.-](\d{2,4}))?\b""")
        // Padrão de valor em reais com suporte a sinais e indicadores (evita capturar números de documento como 121.0000BANCO)
        val moneyRegex = Pattern.compile("""(?<![\d.])([-+]?\s*(?:R\$\s*)?[-+]?\s*(?:\d{1,3}(?:\.\d{3})+|\d+)(?:,\d{2}|\.\d{2}(?![\d/])))\s*([DCdc\-+])?(?![a-zA-Z0-9])""")

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Verifica se o extrato utiliza notação explícita de sinal negativo para despesas (como Itaú, Nubank, Inter)
        val hasExplicitNegativeInDoc = lines.any { rawLine ->
            val l = rawLine.trim()
            !isHeaderOrSummaryLine(l) && (
                l.contains(Regex("""-\s*(?:R\$\s*)?\d+[,.]\d{2}""")) ||
                l.contains(Regex("""(?:R\$\s*)?-\d+[,.]\d{2}""")) ||
                l.contains(Regex("""\d+[,.]\d{2}\s*[-D]""", RegexOption.IGNORE_CASE))
            )
        }

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || isHeaderOrSummaryLine(line)) continue

            val dateMatcher = dateRegex.matcher(line)
            if (!dateMatcher.find()) continue

            // A data deve estar no início da linha (geralmente nos primeiros 15 caracteres)
            if (dateMatcher.start() > 15) continue

            val day = dateMatcher.group(1)?.toIntOrNull() ?: continue
            val month = dateMatcher.group(2)?.toIntOrNull() ?: continue
            val yearGroup = dateMatcher.group(3)
            val year = when {
                yearGroup == null -> currentYear
                yearGroup.length == 2 -> 2000 + (yearGroup.toIntOrNull() ?: 26)
                else -> yearGroup.toIntOrNull() ?: currentYear
            }

            val timestamp = parseDateToTimestamp(day, month, year)

            // Remove a data inicial da linha para processar descrição e valor
            val lineWithoutDate = line.substring(dateMatcher.end()).trim()
            if (lineWithoutDate.isBlank()) continue

            // Extrai todas as ocorrências de valores monetários na linha
            val moneyMatcher = moneyRegex.matcher(lineWithoutDate)
            val matches = mutableListOf<MoneyMatch>()

            while (moneyMatcher.find()) {
                val fullRaw = moneyMatcher.group(1)?.trim() ?: continue
                val indicator = moneyMatcher.group(2)?.trim()
                val cleanAmount = parseAmount(fullRaw)

                if (cleanAmount > 0.0) {
                    val isNeg = fullRaw.startsWith("-") || fullRaw.contains("-") ||
                            indicator?.contains("-") == true || indicator.equals("D", ignoreCase = true)
                    val isPos = fullRaw.startsWith("+") || fullRaw.contains("+") ||
                            indicator?.contains("+") == true || indicator.equals("C", ignoreCase = true)

                    matches.add(
                        MoneyMatch(
                            rawText = fullRaw,
                            indicator = indicator,
                            amount = cleanAmount,
                            isNegativeRaw = isNeg,
                            isPositiveRaw = isPos,
                            startIndex = moneyMatcher.start(),
                            endIndex = moneyMatcher.end()
                        )
                    )
                }
            }

            if (matches.isEmpty()) continue

            // O primeiro valor após a descrição é o montante da transação (o segundo, se existir, é o saldo do dia)
            val txMatch = matches.first()

            val descPart = if (txMatch.startIndex > 0) {
                lineWithoutDate.substring(0, txMatch.startIndex).trim()
            } else {
                lineWithoutDate.substring(txMatch.endIndex).trim()
            }

            val cleanTitle = cleanDescription(descPart)
            if (cleanTitle.length < 2 || isIgnorableTitle(cleanTitle)) continue

            // Determinar se é Receita ou Despesa com base no sinal, indicadores e palavras-chave
            val isIncome = determineIsIncome(cleanTitle, txMatch, hasExplicitNegativeInDoc)
            val category = inferCategory(cleanTitle, isIncome)

            transactions.add(
                ParsedStatementTransaction(
                    title = cleanTitle,
                    dateFormatted = String.format(Locale.getDefault(), "%02d/%02d", day, month),
                    timestamp = timestamp,
                    amount = txMatch.amount,
                    isIncome = isIncome,
                    category = category,
                    isSelected = true
                )
            )
        }

        return transactions
    }

    private fun isHeaderOrSummaryLine(line: String): Boolean {
        val lower = unaccent(line).trim()
        if (lower.isBlank()) return true

        // Cabeçalhos, seções e metadados de extrato
        if (lower.startsWith("extrato") || lower.contains("periodo de visualizacao") || lower.contains("emitido em:") ||
            lower.contains("data lancamentos valor") || lower.contains("data lancamento historico") || lower.contains("data historico doc") ||
            lower.contains("agencia:") || lower.contains("conta:") || lower.contains("cpf:") || lower.contains("cnpj:") ||
            lower.contains("pagina ") || lower.contains("saldo em conta") || lower.contains("limite da conta") ||
            lower.contains("total contratado") || lower.contains("o uso do limite") || lower.contains("aviso!") ||
            lower.contains("consultas, informacoes") || lower.contains("portabilidade") || lower.contains("ouvidoria:") ||
            lower.contains("deficiente auditivo") || lower.contains("fale conosco") || lower.contains("central de atendimento") ||
            lower.contains("sac:") || lower.contains("tarifa bancaria") || lower.contains("rendimento bruto")
        ) return true

        // Linhas de saldo e fechamentos diários
        if (lower.startsWith("saldo") || lower.startsWith("sdo ") || lower.startsWith("sdo.") ||
            lower.contains("saldo do dia") || lower.contains("sdo do dia") || lower.contains("saldo anterior") ||
            lower.contains("saldo final") || lower.contains("saldo atual") || lower.contains("saldo disponivel") ||
            lower.contains("saldo total") || lower.contains("saldo bloqueado") || lower.contains("saldo provisorio") ||
            lower.contains("saldo em conta") || lower.contains("saldo credor") || lower.contains("saldo devedor") ||
            lower.contains("saldo aplic aut") || lower.contains("sdo ct/apl") || lower.contains("sdo banco") ||
            lower.contains("sdo disp") || lower.contains("total de entradas") || lower.contains("total de saidas") ||
            lower.contains("resumo dos saldos") || lower.contains("subtotal")
        ) return true

        return false
    }

    private fun isIgnorableTitle(title: String): Boolean {
        val lower = unaccent(title).trim()
        if (lower.isBlank() || lower.length < 2) return true

        return lower.startsWith("saldo") || lower.startsWith("sdo ") || lower.startsWith("sdo.") ||
                lower.startsWith("total") || lower.startsWith("limite") || lower.startsWith("subtotal") ||
                lower.startsWith("resumo") || lower.startsWith("aviso") || lower == "saldo do dia" ||
                lower == "sdo do dia"
    }

    private fun cleanDescription(desc: String): String {
        return desc
            .replace(Regex("""^[-–—\s/.:\d]+"""), "")
            .replace(Regex("""[-–—\s/.:]+$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun parseAmount(str: String): Double {
        return try {
            val normalized = str
                .replace("R$", "")
                .replace(" ", "")
                .replace("+", "")
                .replace("-", "")

            if (normalized.contains(",") && normalized.contains(".")) {
                // Formato brasileiro padrão: 1.234,56
                normalized.replace(".", "").replace(",", ".").toDouble()
            } else if (normalized.contains(",")) {
                normalized.replace(",", ".").toDouble()
            } else {
                normalized.toDouble()
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private fun determineIsIncome(
        title: String,
        txMatch: MoneyMatch,
        hasExplicitNegativeInDoc: Boolean
    ): Boolean {
        val lowerTitle = unaccent(title)

        // 1. Sinais e indicadores explícitos no próprio valor
        if (txMatch.isNegativeRaw) return false
        if (txMatch.isPositiveRaw) return true

        // 2. Palavras-chave fortes de ENTRADA / RECEITA
        if (lowerTitle.contains("salario") || lowerTitle.contains("remuneracao") || lowerTitle.contains("prov/salario") ||
            lowerTitle.contains("rendimento") || lowerTitle.contains("rend pago") || lowerTitle.contains("estorno") ||
            lowerTitle.contains("reembolso") || lowerTitle.contains("devolucao") || lowerTitle.contains("dev pix") ||
            lowerTitle.contains("resgate") || lowerTitle.contains("credito") || lowerTitle.contains("recebida") ||
            lowerTitle.contains("recebido") || lowerTitle.contains("recebimento") || lowerTitle.contains("deposito") ||
            lowerTitle.contains("proventos") || lowerTitle.contains("dividendo") || lowerTitle.contains("cashback") ||
            lowerTitle.contains("sispag") || lowerTitle.startsWith("est ") || lowerTitle.contains(" est ") ||
            lowerTitle.contains("ted recebida") || lowerTitle.contains("doc recebido")
        ) {
            return true
        }

        // 3. Palavras-chave fortes de SAÍDA / DESPESA
        if (lowerTitle.contains("enviado") || lowerTitle.contains("enviada") || lowerTitle.contains("compra") ||
            lowerTitle.contains("pagamento") || lowerTitle.contains("pagto") || lowerTitle.contains("debito") ||
            lowerTitle.contains("saque") || lowerTitle.contains("tarifa") || lowerTitle.contains("iof") ||
            lowerTitle.contains("fatura") || lowerTitle.contains("recarga") || lowerTitle.startsWith("rshop") ||
            lowerTitle.startsWith("rscss") || lowerTitle.startsWith("on ") || lowerTitle.startsWith("pay ")
        ) {
            return false
        }

        // 4. Se o extrato possui valores com sinal negativo explícito para débitos (como Itaú/Nubank/Inter),
        // qualquer transação sem sinal negativo é uma Entrada (Receita).
        if (hasExplicitNegativeInDoc) {
            return true
        }

        // Fallback padrão se não há como determinar
        return false
    }

    private fun inferCategory(title: String, isIncome: Boolean): String {
        val lower = unaccent(title)
        if (isIncome) {
            return when {
                lower.contains("salario") || lower.contains("remuneracao") || lower.contains("folha") -> "Salário"
                lower.contains("rendimento") || lower.contains("dividendo") || lower.contains("juros") ||
                        lower.contains("cdi") || lower.contains("rend pago") || lower.contains("aplic aut") -> "Investimentos"
                lower.contains("estorno") || lower.contains("reembolso") || lower.contains("devolucao") ||
                        lower.contains("dev pix") || lower.contains("est on") -> "Reembolso"
                else -> "Receitas"
            }
        }

        return when {
            lower.contains("farmacia") || lower.contains("drogaria") || lower.contains("hospital") ||
                    lower.contains("consulta") || lower.contains("laboratorio") || lower.contains("medico") ||
                    lower.contains("dentista") || lower.contains("drogasil") || lower.contains("raia") ||
                    lower.contains("panvel") || lower.contains("pague menos") || lower.contains("exame") ||
                    lower.contains("metro farma") -> "Saúde"

            lower.contains("mercado") || lower.contains("supermercado") || lower.contains("padaria") ||
                    lower.contains("hortifruti") || lower.contains("acougue") || lower.contains("restaurante") ||
                    lower.contains("ifood") || lower.contains("rappi") || lower.contains("burger") || lower.contains("pizza") ||
                    lower.contains("lanchonete") || lower.contains("cafe") || lower.contains("chocolat") ||
                    lower.contains("pao de acucar") || lower.contains("carrefour") || lower.contains("extra") ||
                    lower.contains("assai") || lower.contains("atacadao") || lower.contains("99food") ||
                    lower.contains("boteco") || lower.contains("emporiopdoce") || lower.contains("doce sonho") ||
                    lower.contains("paes e doces") -> "Alimentação"

            lower.contains("uber") || lower.contains("99app") || lower.contains("99") ||
                    lower.contains("combustivel") || lower.contains("posto") || lower.contains("gasolina") ||
                    lower.contains("etanol") || lower.contains("estacionamento") || lower.contains("pedagio") ||
                    lower.contains("sem parar") || lower.contains("veloe") || lower.contains("conectcar") ||
                    lower.contains("metro") || lower.contains("cptm") || lower.contains("onibus") ||
                    lower.contains("sptrans") || lower.contains("ipiranga") || lower.contains("shell") -> "Transporte"

            lower.contains("aluguel") || lower.contains("condominio") || lower.contains("enel") ||
                    lower.contains("luz") || lower.contains("energia") || lower.contains("cpfl") ||
                    lower.contains("cemig") || lower.contains("copel") || lower.contains("sabesp") ||
                    lower.contains("agua") || lower.contains("sanepar") || lower.contains("copasa") ||
                    lower.contains("comgas") || lower.contains("ultragaz") || lower.contains("botijao") ||
                    lower.contains("conta de gas") || lower.contains("gas encanado") || lower.contains("internet") ||
                    lower.contains("claro") || lower.contains("vivo") || lower.contains("tim") || lower.contains("iptu") -> "Moradia"

            lower.contains("netflix") || lower.contains("spotify") || lower.contains("cinema") ||
                    lower.contains("steam") || lower.contains("playstation") || lower.contains("xbox") ||
                    lower.contains("nintendo") || lower.contains("jogos") || lower.contains("disney") ||
                    lower.contains("prime") || lower.contains("hbomax") || lower.contains("max") ||
                    lower.contains("ingresso") || lower.contains("show") || lower.contains("lojas americ") ||
                    lower.contains("vivara") || lower.contains("sesc") -> "Lazer"

            lower.contains("pet") || lower.contains("veterinario") || lower.contains("cobasi") ||
                    lower.contains("petz") || lower.contains("petlove") || lower.contains("racao") -> "Petz"

            lower.contains("pix") || lower.contains("ted") || lower.contains("doc") ||
                    lower.contains("transferencia") -> "Transferência"

            else -> "Outros"
        }
    }

    private fun unaccent(text: String): String {
        val temp = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        return temp.replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "").lowercase()
    }

    private fun parseDateToTimestamp(day: Int, month: Int, year: Int): Long {
        return try {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
