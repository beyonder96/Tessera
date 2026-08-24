package com.example.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
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
    var category: String,
    val accountOrCardName: String = "",
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

    private val KNOWN_CATEGORIES = listOf(
        "Alimentação", "Transporte", "Moradia", "Saúde", "Lazer",
        "Educação", "Petz", "Salário", "Investimentos", "Compras",
        "Reembolso", "Outros", "Transferência", "Receitas", "Ajuste"
    )

    /**
     * Identifica e processa extratos em PDF ou CSV a partir do URI fornecido.
     */
    suspend fun parseStatement(context: Context, uri: Uri): List<ParsedStatementTransaction> = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri)?.lowercase(Locale.getDefault()) ?: ""
        val uriStr = uri.toString().lowercase(Locale.getDefault())

        if (mimeType.contains("csv") || uriStr.endsWith(".csv") || mimeType.contains("text/comma-separated-values")) {
            val csvResult = parseCsvStatement(context, uri)
            if (csvResult.isNotEmpty()) return@withContext csvResult
        }

        // Tenta processar como PDF por padrão
        val pdfResult = parsePdfStatement(context, uri)
        if (pdfResult.isNotEmpty()) return@withContext pdfResult

        // Se falhou como PDF, tenta ler como CSV (caso o MIME tenha vindo genérico como text/plain)
        return@withContext parseCsvStatement(context, uri)
    }

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

    suspend fun parseCsvStatement(context: Context, uri: Uri): List<ParsedStatementTransaction> = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<ParsedStatementTransaction>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val lines = reader.readLines()
                    if (lines.isEmpty()) return@withContext transactions

                    // Detecta delimitador (, ou ;)
                    val firstLine = lines.firstOrNull { it.isNotBlank() } ?: return@withContext transactions
                    val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ";" else ","

                    // Detecta índices das colunas a partir do cabeçalho
                    var dateIdx = -1
                    var titleIdx = -1
                    var categoryIdx = -1
                    var accountIdx = -1
                    var valueIdx = -1
                    var typeIdx = -1

                    val headerTokens = splitCsvLine(firstLine, delimiter).map { unaccent(it).trim().lowercase(Locale.getDefault()) }
                    headerTokens.forEachIndexed { index, header ->
                        when {
                            header.contains("data") || header.contains("date") -> if (dateIdx == -1) dateIdx = index
                            header.contains("titulo") || header.contains("title") || header.contains("descricao") || header.contains("historico") -> if (titleIdx == -1) titleIdx = index
                            header.contains("categoria") || header.contains("category") -> if (categoryIdx == -1) categoryIdx = index
                            header.contains("conta") || header.contains("cartao") || header.contains("account") -> if (accountIdx == -1) accountIdx = index
                            header.contains("valor") || header.contains("value") || header.contains("amount") -> if (valueIdx == -1) valueIdx = index
                            header.contains("tipo") || header.contains("type") -> if (typeIdx == -1) typeIdx = index
                        }
                    }

                    val hasHeader = dateIdx != -1 || titleIdx != -1 || valueIdx != -1
                    val startIndex = if (hasHeader) 1 else 0

                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val dateRegex = Pattern.compile("""\b(\d{1,2})[/.-](\d{1,2})(?:[/.-](\d{2,4}))?\b""")

                    for (i in startIndex until lines.size) {
                        val rawLine = lines[i].trim()
                        if (rawLine.isBlank()) continue

                        val tokens = splitCsvLine(rawLine, delimiter)
                        if (tokens.size < 2) continue

                        var dateStr = if (dateIdx in tokens.indices) tokens[dateIdx] else tokens[0]
                        var titleStr = if (titleIdx in tokens.indices) tokens[titleIdx] else tokens.getOrNull(1) ?: ""
                        var categoryStr = if (categoryIdx in tokens.indices) tokens[categoryIdx] else ""
                        val accountStr = if (accountIdx in tokens.indices) tokens[accountIdx] else ""
                        var valueRaw = if (valueIdx in tokens.indices) tokens[valueIdx] else tokens.lastOrNull() ?: ""
                        val typeStr = if (typeIdx in tokens.indices) tokens[typeIdx] else ""

                        val dateMatcher = dateRegex.matcher(dateStr)
                        if (!dateMatcher.find()) continue

                        val day = dateMatcher.group(1)?.toIntOrNull() ?: continue
                        val month = dateMatcher.group(2)?.toIntOrNull() ?: continue
                        val yearGroup = dateMatcher.group(3)
                        val year = when {
                            yearGroup == null -> currentYear
                            yearGroup.length == 2 -> 2000 + (yearGroup.toIntOrNull() ?: 26)
                            else -> yearGroup.toIntOrNull() ?: currentYear
                        }

                        val timestamp = parseDateToTimestamp(day, month, year)
                        val cleanAmount = parseAmount(valueRaw)
                        if (cleanAmount <= 0.0) continue

                        val isIncome = when {
                            typeStr.contains("receita", ignoreCase = true) || typeStr.contains("entrada", ignoreCase = true) || typeStr.contains("income", ignoreCase = true) || typeStr.equals("C", ignoreCase = true) -> true
                            typeStr.contains("despesa", ignoreCase = true) || typeStr.contains("saida", ignoreCase = true) || typeStr.contains("expense", ignoreCase = true) || typeStr.equals("D", ignoreCase = true) -> false
                            valueRaw.startsWith("+") -> true
                            valueRaw.startsWith("-") -> false
                            else -> determineIsIncome(titleStr, MoneyMatch(valueRaw, null, cleanAmount, false, false, 0, 0), false)
                        }

                        if (categoryStr.isBlank()) {
                            categoryStr = inferCategory(titleStr, isIncome)
                        }

                        transactions.add(
                            ParsedStatementTransaction(
                                title = cleanDescription(titleStr),
                                dateFormatted = String.format(Locale.getDefault(), "%02d/%02d", day, month),
                                timestamp = timestamp,
                                amount = cleanAmount,
                                isIncome = isIncome,
                                category = categoryStr,
                                accountOrCardName = accountStr.trim(),
                                isSelected = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar extrato em CSV", e)
        }
        return@withContext transactions
    }

    private fun splitCsvLine(line: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()

        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch.toString() == delimiter && !inQuotes -> {
                    result.add(current.toString().trim().replace("^\"|\"$".toRegex(), ""))
                    current.setLength(0)
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString().trim().replace("^\"|\"$".toRegex(), ""))
        return result
    }

    fun extractTransactionsFromLines(lines: List<String>): List<ParsedStatementTransaction> {
        val transactions = mutableListOf<ParsedStatementTransaction>()

        val dateRegex = Pattern.compile("""\b(\d{1,2})[/.-](\d{1,2})(?:[/.-](\d{2,4}))?\b""")
        val moneyRegex = Pattern.compile("""(?<![\d.])([-+]?\s*(?:R\$\s*)?[-+]?\s*(?:\d{1,3}(?:\.\d{3})+|\d+)(?:,\d{2}|\.\d{2}(?![\d/])))\s*([DCdc\-+])?(?![a-zA-Z0-9])""")

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // Verifica se é um extrato exportado pelo Tessera
        val isTesseraExport = lines.any { unaccent(it).contains("extrato tessera") }

        // Verifica se o extrato utiliza notação explícita de sinal negativo para despesas
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

            val lineWithoutDate = line.substring(dateMatcher.end()).trim()
            if (lineWithoutDate.isBlank()) continue

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

            val txMatch = matches.first()

            val descPart = if (txMatch.startIndex > 0) {
                lineWithoutDate.substring(0, txMatch.startIndex).trim()
            } else {
                lineWithoutDate.substring(txMatch.endIndex).trim()
            }

            // Extração inteligente de Título, Categoria e Conta
            val (extractedTitle, extractedCategory, extractedAccount) = parseTitleCategoryAndAccount(
                descPart,
                isTesseraExport
            )

            val cleanTitle = cleanDescription(extractedTitle)
            if (cleanTitle.length < 2 || isIgnorableTitle(cleanTitle)) continue

            val isIncome = determineIsIncome(cleanTitle, txMatch, hasExplicitNegativeInDoc)
            val finalCategory = if (extractedCategory.isNotBlank()) {
                extractedCategory
            } else {
                inferCategory(cleanTitle, isIncome)
            }

            transactions.add(
                ParsedStatementTransaction(
                    title = cleanTitle,
                    dateFormatted = String.format(Locale.getDefault(), "%02d/%02d", day, month),
                    timestamp = timestamp,
                    amount = txMatch.amount,
                    isIncome = isIncome,
                    category = finalCategory,
                    accountOrCardName = extractedAccount,
                    isSelected = true
                )
            )
        }

        return transactions
    }

    /**
     * Tenta identificar se no texto descritivo existem colunas separadas de Categoria e Conta/Cartão,
     * especialmente em PDFs exportados pelo Tessera.
     */
    private fun parseTitleCategoryAndAccount(
        rawDesc: String,
        isTesseraExport: Boolean
    ): Triple<String, String, String> {
        val trimmed = rawDesc.trim()

        data class CategoryMatchCandidate(
            val title: String,
            val category: String,
            val account: String,
            val start: Int
        )

        val candidates = mutableListOf<CategoryMatchCandidate>()

        for (cat in KNOWN_CATEGORIES) {
            val catPattern = Pattern.compile("""\b${Pattern.quote(cat)}\b""", Pattern.CASE_INSENSITIVE)
            val matcher = catPattern.matcher(trimmed)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                val before = trimmed.substring(0, start).trim()
                val after = trimmed.substring(end).trim()
                val cleanBefore = cleanDescription(before)

                if (cleanBefore.length >= 2) {
                    candidates.add(CategoryMatchCandidate(cleanBefore, cat, after, start))
                }
            }
        }

        if (candidates.isNotEmpty()) {
            val best = candidates.maxByOrNull { it.start }!!
            return Triple(best.title, best.category, best.account)
        }

        return Triple(trimmed, "", "")
    }

    private fun isHeaderOrSummaryLine(line: String): Boolean {
        val lower = unaccent(line).trim()
        if (lower.isBlank()) return true

        if (lower.startsWith("extrato") || lower.contains("periodo de visualizacao") || lower.contains("emitido em:") ||
            lower.contains("data lancamentos valor") || lower.contains("data lancamento historico") || lower.contains("data historico doc") ||
            lower.contains("data titulo categoria") || lower.contains("conta/cartao valor") ||
            lower.contains("agencia:") || lower.contains("conta:") || lower.contains("cpf:") || lower.contains("cnpj:") ||
            lower.contains("pagina ") || lower.contains("saldo em conta") || lower.contains("limite da conta") ||
            lower.contains("total contratado") || lower.contains("o uso do limite") || lower.contains("aviso!") ||
            lower.contains("consultas, informacoes") || lower.contains("portabilidade") || lower.contains("ouvidoria:") ||
            lower.contains("deficiente auditivo") || lower.contains("fale conosco") || lower.contains("central de atendimento") ||
            lower.contains("sac:") || lower.startsWith("tarifa bancaria") || lower.startsWith("rendimento bruto") ||
            lower.startsWith("fatura:") || lower.contains("resumo da fatura") || lower.contains("total da fatura") ||
            lower.contains("fatura fechada")
        ) return true

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

        if (txMatch.isNegativeRaw) return false
        if (txMatch.isPositiveRaw) return true

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

        if (lowerTitle.contains("enviado") || lowerTitle.contains("enviada") || lowerTitle.contains("compra") ||
            lowerTitle.contains("pagamento") || lowerTitle.contains("pagto") || lowerTitle.contains("debito") ||
            lowerTitle.contains("saque") || lowerTitle.contains("tarifa") || lowerTitle.contains("iof") ||
            lowerTitle.contains("fatura") || lowerTitle.contains("recarga") || lowerTitle.startsWith("rshop") ||
            lowerTitle.startsWith("rscss") || lowerTitle.startsWith("on ") || lowerTitle.startsWith("pay ")
        ) {
            return false
        }

        if (hasExplicitNegativeInDoc) {
            return true
        }

        return false
    }

    private fun inferCategory(title: String, isIncome: Boolean): String {
        val lower = unaccent(title)
        if (isIncome) {
            return when {
                lower.contains("salario") || lower.contains("remuneracao") || lower.contains("folha") || lower.contains("ordenado") -> "Salário"
                lower.contains("rendimento") || lower.contains("dividendo") || lower.contains("juros") ||
                        lower.contains("cdi") || lower.contains("rend pago") || lower.contains("aplic aut") -> "Investimentos"
                lower.contains("estorno") || lower.contains("reembolso") || lower.contains("devolucao") ||
                        lower.contains("dev pix") || lower.contains("est on") -> "Reembolso"
                lower.contains("transf entre contas") || lower.contains("propria") -> "Transferência"
                else -> "Salário"
            }
        }

        return when {
            lower.contains("farmacia") || lower.contains("drogaria") || lower.contains("hospital") ||
                    lower.contains("consulta") || lower.contains("laboratorio") || lower.contains("medico") ||
                    lower.contains("dentista") || lower.contains("drogasil") || lower.contains("raia") ||
                    lower.contains("panvel") || lower.contains("pague menos") || lower.contains("exame") ||
                    lower.contains("metro farma") || lower.contains("ultrafarma") || lower.contains("unimed") ||
                    lower.contains("clinica") -> "Saúde"

            lower.contains("mercad") || lower.contains("supermercado") || lower.contains("padaria") ||
                    lower.contains("mercearia") || lower.contains("sacolao") ||
                    lower.contains("hortifruti") || lower.contains("acougue") || lower.contains("restaurante") ||
                    lower.contains("ifood") || lower.contains("rappi") || lower.contains("burger") || lower.contains("pizza") ||
                    lower.contains("lanchonete") || lower.contains("cafe") || lower.contains("chocolat") ||
                    lower.contains("pao de acucar") || lower.contains("carrefour") || lower.contains("extra") ||
                    lower.contains("assai") || lower.contains("atacadao") || lower.contains("99food") ||
                    lower.contains("boteco") || lower.contains("emporiopdoce") || lower.contains("doce sonho") ||
                    lower.contains("paes e doces") || lower.contains("mcdonald") || lower.contains("habib") ||
                    lower.contains("subway") || lower.contains("bar") || lower.contains("churrascaria") ||
                    lower.contains("sorvete") || lower.contains("acai") -> "Alimentação"

            lower.contains("uber") || lower.contains("99app") || lower.contains("99") ||
                    lower.contains("combustivel") || lower.contains("posto") || lower.contains("gasolina") ||
                    lower.contains("etanol") || lower.contains("estacionamento") || lower.contains("pedagio") ||
                    lower.contains("sem parar") || lower.contains("veloe") || lower.contains("conectcar") ||
                    lower.contains("metro") || lower.contains("cptm") || lower.contains("onibus") ||
                    lower.contains("sptrans") || lower.contains("ipiranga") || lower.contains("shell") ||
                    lower.contains("auto posto") || lower.contains("estapar") -> "Transporte"

            lower.contains("aluguel") || lower.contains("condominio") || lower.contains("enel") ||
                    lower.contains("luz") || lower.contains("energia") || lower.contains("cpfl") ||
                    lower.contains("cemig") || lower.contains("copel") || lower.contains("sabesp") ||
                    lower.contains("agua") || lower.contains("sanepar") || lower.contains("copasa") ||
                    lower.contains("comgas") || lower.contains("ultragaz") || lower.contains("botijao") ||
                    lower.contains("conta de gas") || lower.contains("gas encanado") || lower.contains("internet") ||
                    lower.contains("claro") || lower.contains("vivo") || lower.contains("tim") || lower.contains("iptu") ||
                    lower.contains("light") || lower.contains("neoenergia") -> "Moradia"

            lower.contains("netflix") || lower.contains("spotify") || lower.contains("cinema") ||
                    lower.contains("steam") || lower.contains("playstation") || lower.contains("xbox") ||
                    lower.contains("nintendo") || lower.contains("jogos") || lower.contains("disney") ||
                    lower.contains("prime") || lower.contains("hbomax") || lower.contains("max") ||
                    lower.contains("ingresso") || lower.contains("show") || lower.contains("sympla") ||
                    lower.contains("eventim") || lower.contains("sesc") || lower.contains("deezer") ||
                    lower.contains("apple.com/bill") || lower.contains("crunchyroll") || lower.contains("youtube") -> "Lazer"

            lower.contains("pet") || lower.contains("veterinario") || lower.contains("cobasi") ||
                    lower.contains("petz") || lower.contains("petlove") || lower.contains("racao") -> "Petz"

            lower.contains("escola") || lower.contains("faculdade") || lower.contains("universidade") ||
                    lower.contains("curso") || lower.contains("udemy") || lower.contains("alura") ||
                    lower.contains("fiap") || lower.contains("livraria") || lower.contains("saraiva") ||
                    lower.contains("livro") || lower.contains("educacao") -> "Educação"

            lower.contains("zara") || lower.contains("riachuelo") || lower.contains("renner") ||
                    lower.contains("c&a") || lower.contains("shein") || lower.contains("shopee") ||
                    lower.contains("mercado livre") || lower.contains("amazon") || lower.contains("aliexpress") ||
                    lower.contains("magalu") || lower.contains("magazine") || lower.contains("centauro") ||
                    lower.contains("nike") || lower.contains("adidas") || lower.contains("decathlon") ||
                    lower.contains("vestuario") || lower.contains("calcados") || lower.contains("lojas americ") -> "Compras"

            // Apenas marca como Transferência se for explicitamente entre contas próprias
            lower.contains("transf entre contas") || lower.contains("transferencia entre contas") ||
                    lower.contains("aplicacao poupanca") || lower.contains("investimento cdi") -> "Transferência"

            // Qualquer outro pagamento/PIX/TED de saída não identificado é categorizado como "Outros"
            // garantindo que seja contabilizado no total de gastos comprometidos
            else -> "Outros"
        }
    }

    private fun unaccent(text: String): String {
        val temp = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        return temp.replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "").lowercase(Locale.getDefault())
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
