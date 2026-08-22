package com.example.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
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

        // Padrões de Data comuns em extratos brasileiros: DD/MM/YYYY, DD/MM/YY, DD/MM, DD MMM
        val dateRegex = Pattern.compile("""\b(\d{1,2})[/.-](\d{1,2})(?:[/.-](\d{2,4}))?\b""")
        // Padrão de valor em reais: R$ 1.234,56 ou 1.234,56 ou -1.234,56 ou 1234.56
        val moneyRegex = Pattern.compile("""(?:R\$\s*)?([-+]?\s*(?:\d{1,3}(?:\.\d{3})*|\d+)[,.]\d{2})\s*([DCdc\-+])?""")

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || isHeaderOrSummaryLine(line)) continue

            val dateMatcher = dateRegex.matcher(line)
            if (!dateMatcher.find()) continue

            val day = dateMatcher.group(1)?.toIntOrNull() ?: continue
            val month = dateMatcher.group(2)?.toIntOrNull() ?: continue
            val yearGroup = dateMatcher.group(3)
            val year = when {
                yearGroup == null -> currentYear
                yearGroup.length == 2 -> 2000 + (yearGroup.toIntOrNull() ?: 26)
                else -> yearGroup.toIntOrNull() ?: currentYear
            }

            val dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month, year)
            val timestamp = parseDateToTimestamp(day, month, year)

            // Remove a data da linha para processar descrição e valor
            val lineWithoutDate = line.substring(dateMatcher.end()).trim()
            if (lineWithoutDate.isBlank()) continue

            // Busca valor monetário
            val moneyMatcher = moneyRegex.matcher(lineWithoutDate)
            var lastFoundAmount: Double? = null
            var lastIndicator: String? = null
            var amountEndIndex = -1
            var amountStartIndex = -1

            while (moneyMatcher.find()) {
                val valueStr = moneyMatcher.group(1)?.replace(" ", "") ?: continue
                val indicator = moneyMatcher.group(2)
                val cleanAmount = parseAmount(valueStr)
                if (cleanAmount > 0.0) {
                    lastFoundAmount = cleanAmount
                    lastIndicator = indicator
                    amountStartIndex = moneyMatcher.start()
                    amountEndIndex = moneyMatcher.end()
                }
            }

            if (lastFoundAmount == null || lastFoundAmount == 0.0) continue

            // Descrição da transação
            val descPart = if (amountStartIndex > 0) {
                lineWithoutDate.substring(0, amountStartIndex).trim()
            } else {
                lineWithoutDate.substring(amountEndIndex).trim()
            }

            val cleanTitle = cleanDescription(descPart)
            if (cleanTitle.length < 3 || isIgnorableTitle(cleanTitle)) continue

            // Determinar se é Receita ou Despesa
            val isIncome = determineIsIncome(line, lastIndicator)
            val category = inferCategory(cleanTitle, isIncome)

            transactions.add(
                ParsedStatementTransaction(
                    title = cleanTitle,
                    dateFormatted = String.format(Locale.getDefault(), "%02d/%02d", day, month),
                    timestamp = timestamp,
                    amount = lastFoundAmount,
                    isIncome = isIncome,
                    category = category,
                    isSelected = true
                )
            )
        }

        return transactions
    }

    private fun isHeaderOrSummaryLine(line: String): Boolean {
        val lower = unaccent(line)
        return lower.contains("extrato") || lower.contains("saldo anterior") ||
                lower.contains("saldo final") || lower.contains("saldo disponivel") ||
                lower.contains("total de entradas") || lower.contains("total de saidas") ||
                lower.contains("pagina") || lower.contains("periodo") ||
                lower.contains("agencia") || lower.contains("conta corrente") ||
                lower.contains("data lancamento") || lower.contains("historico")
    }

    private fun isIgnorableTitle(title: String): Boolean {
        val lower = unaccent(title)
        return lower.startsWith("saldo") || lower.startsWith("total") || lower.startsWith("limite")
    }

    private fun cleanDescription(desc: String): String {
        return desc
            .replace(Regex("""^[-–—\s/.:\d]+"""), "")
            .replace(Regex("""[-–—\s/.:]+$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .replaceFirstChar { it.uppercase() }
    }

    private fun parseAmount(str: String): Double {
        return try {
            val normalized = str
                .replace("R$", "")
                .replace(" ", "")
                .replace("+", "")
                .replace("-", "")

            if (normalized.contains(",") && normalized.contains(".")) {
                // Formato 1.234,56
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

    private fun determineIsIncome(fullLine: String, indicator: String?): Boolean {
        val lower = unaccent(fullLine)

        if (indicator?.equals("C", ignoreCase = true) == true || indicator?.contains("+") == true) return true
        if (indicator?.equals("D", ignoreCase = true) == true || indicator?.contains("-") == true) return false

        // Palavras claras de entrada / receita
        if (lower.contains("recebido") || lower.contains("recebida") || lower.contains("recebimento") ||
            lower.contains("deposito") || lower.contains("salario") || lower.contains("remuneracao") ||
            lower.contains("rendimento") || lower.contains("estorno") || lower.contains("reembolso") ||
            lower.contains("credito em conta") || lower.contains("proventos") || lower.contains("dividendo") ||
            lower.contains("cashback") || lower.contains("+r$") || lower.contains("+ r$")
        ) {
            return true
        }

        // Palavras claras de saída / despesa
        if (lower.contains("enviado") || lower.contains("enviada") || lower.contains("compra") ||
            lower.contains("pagamento") || lower.contains("debito") || lower.contains("saque") ||
            lower.contains("tarifa") || lower.contains("iof") || lower.contains("-r$") || lower.contains("- r$")
        ) {
            return false
        }

        return false
    }

    private fun inferCategory(title: String, isIncome: Boolean): String {
        val lower = unaccent(title)
        if (isIncome) {
            return when {
                lower.contains("salario") || lower.contains("remuneracao") || lower.contains("folha") -> "Salário"
                lower.contains("rendimento") || lower.contains("dividendo") || lower.contains("juros") || lower.contains("cdi") -> "Investimentos"
                lower.contains("estorno") || lower.contains("reembolso") || lower.contains("devolucao") -> "Reembolso"
                else -> "Receitas"
            }
        }

        return when {
            lower.contains("farmacia") || lower.contains("drogaria") || lower.contains("hospital") ||
                    lower.contains("consulta") || lower.contains("laboratorio") || lower.contains("medico") ||
                    lower.contains("dentista") || lower.contains("drogasil") || lower.contains("raia") ||
                    lower.contains("panvel") || lower.contains("pague menos") || lower.contains("exame") -> "Saúde"

            lower.contains("mercado") || lower.contains("supermercado") || lower.contains("padaria") ||
                    lower.contains("hortifruti") || lower.contains("acougue") || lower.contains("restaurante") ||
                    lower.contains("ifood") || lower.contains("rappi") || lower.contains("burger") || lower.contains("pizza") ||
                    lower.contains("lanchonete") || lower.contains("cafe") || lower.contains("chocolat") ||
                    lower.contains("pao de acucar") || lower.contains("carrefour") || lower.contains("extra") ||
                    lower.contains("assai") || lower.contains("atacadao") -> "Alimentação"

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
                    lower.contains("ingresso") || lower.contains("show") -> "Lazer"

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
