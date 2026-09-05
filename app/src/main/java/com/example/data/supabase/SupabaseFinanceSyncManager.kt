package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.BankAccount
import com.example.data.BenefitCard
import com.example.data.CreditCard
import com.example.data.Debt
import com.example.data.TesseraRepository
import com.example.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class FinanceSuggestion(
    val id: String,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val date: String,
    val createdAt: String,
    val status: String
)

class SupabaseFinanceSyncManager(
    private val context: Context,
    private val repository: TesseraRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var localSyncJob: Job? = null
    private var remotePollJob: Job? = null

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _activeShareId = MutableStateFlow<String?>(null)
    val activeShareId: StateFlow<String?> = _activeShareId

    private val _pendingSuggestions = MutableStateFlow<List<FinanceSuggestion>>(emptyList())
    val pendingSuggestions: StateFlow<List<FinanceSuggestion>> = _pendingSuggestions

    private var lastUploadedHash: Int? = null
    private var cachedSuggestionsJson = JSONArray()

    private var currentSpendableBalance: Double? = null
    private var currentSalaryValue: Double? = null
    private var currentCommittedValue: Double? = null
    private var currentCommittedPercentage: Double? = null

    enum class SyncStatus {
        IDLE,
        SYNCING,
        SYNCED,
        ERROR
    }

    init {
        val prefs = context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
        _activeShareId.value = prefs.getString("finance_share_id", null)
    }

    fun getShareUrl(): String {
        val id = _activeShareId.value ?: generateNewShareId()
        return "${SupabaseClientProvider.getWebBaseUrl()}/finance/$id"
    }

    fun generateNewShareId(): String {
        val newId = UUID.randomUUID().toString().take(8)
        _activeShareId.value = newId
        lastUploadedHash = null
        context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("finance_share_id", newId)
            .apply()
        triggerSync()
        return newId
    }

    fun updateSpendableMetrics(
        spendableBalance: Double,
        salaryValue: Double,
        committedValue: Double,
        committedPercentage: Double
    ) {
        currentSpendableBalance = spendableBalance
        currentSalaryValue = salaryValue
        currentCommittedValue = committedValue
        currentCommittedPercentage = committedPercentage
        lastUploadedHash = null
        triggerSync()
    }

    fun startContinuousSync() {
        if (_activeShareId.value == null) {
            generateNewShareId()
        }

        // 1. Observe local transactions, accounts, cards, benefits and debts and push to Supabase in real-time
        if (localSyncJob?.isActive != true) {
            localSyncJob = scope.launch {
                combine(
                    repository.allTransactions,
                    repository.allBankAccounts,
                    repository.allCreditCards,
                    repository.allBenefitCards,
                    repository.allDebts
                ) { args: Array<Any> ->
                    @Suppress("UNCHECKED_CAST")
                    val transactions = args[0] as List<Transaction>
                    @Suppress("UNCHECKED_CAST")
                    val accounts = args[1] as List<BankAccount>
                    @Suppress("UNCHECKED_CAST")
                    val cards = args[2] as List<CreditCard>
                    @Suppress("UNCHECKED_CAST")
                    val benefits = args[3] as List<BenefitCard>
                    @Suppress("UNCHECKED_CAST")
                    val debts = args[4] as List<Debt>
                    uploadDashboardToSupabase(transactions, accounts, cards, benefits, debts)
                }.collect()
            }
        }

        // 2. Poll remote suggestions from Supabase
        if (remotePollJob?.isActive != true) {
            remotePollJob = scope.launch {
                while (isActive) {
                    pullSuggestionsFromSupabase()
                    delay(5000)
                }
            }
        }
    }

    fun stopContinuousSync() {
        localSyncJob?.cancel()
        localSyncJob = null
        remotePollJob?.cancel()
        remotePollJob = null
        _syncStatus.value = SyncStatus.IDLE
    }

    fun triggerSync() {
        lastUploadedHash = null
        scope.launch {
            pullSuggestionsFromSupabase()
            val transactions = repository.allTransactions.first()
            val accounts = repository.allBankAccounts.first()
            val cards = repository.allCreditCards.first()
            val benefits = repository.allBenefitCards.first()
            val debts = repository.allDebts.first()
            uploadDashboardToSupabase(transactions, accounts, cards, benefits, debts)
        }
    }

    suspend fun pullSuggestionsFromSupabase() {
        val shareId = _activeShareId.value ?: return
        withContext(Dispatchers.IO) {
            try {
                val result = SupabaseClientProvider.getDocument("shared_finance_dashboards", shareId)
                if (result.isSuccess) {
                    val responseStr = result.getOrNull() ?: return@withContext
                    val jsonArray = JSONArray(responseStr)
                    if (jsonArray.length() == 0) return@withContext
                    val docObj = jsonArray.getJSONObject(0)
                    val suggestionsJson = docObj.optJSONArray("suggestions") ?: JSONArray()
                    cachedSuggestionsJson = suggestionsJson

                    val pendingList = mutableListOf<FinanceSuggestion>()
                    for (i in 0 until suggestionsJson.length()) {
                        val sugObj = suggestionsJson.getJSONObject(i)
                        val status = sugObj.optString("status", "pending")
                        if (status == "pending") {
                            pendingList.add(
                                FinanceSuggestion(
                                    id = sugObj.optString("id", UUID.randomUUID().toString()),
                                    title = sugObj.optString("title", "Sem título"),
                                    amount = sugObj.optDouble("amount", 0.0),
                                    type = sugObj.optString("type", "expense"),
                                    category = sugObj.optString("category", "Geral"),
                                    date = sugObj.optString("date", ""),
                                    createdAt = sugObj.optString("created_at", ""),
                                    status = status
                                )
                            )
                        }
                    }
                    _pendingSuggestions.value = pendingList
                }
            } catch (e: Exception) {
                Log.w("SupabaseFinanceSync", "Failed to pull suggestions: ${e.message}")
            }
        }
    }

    fun approveSuggestion(
        suggestion: FinanceSuggestion,
        accountOrCardName: String = "",
        onApproveTransaction: (Transaction) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val isIncome = suggestion.type.equals("income", ignoreCase = true)
            val newTx = Transaction(
                title = suggestion.title,
                subtitle = "Via Web • ${suggestion.category}",
                value = suggestion.amount,
                isIncome = isIncome,
                timestamp = System.currentTimeMillis(),
                category = suggestion.category,
                accountOrCardName = accountOrCardName,
                isRealized = true,
                isRecurrent = false,
                recurrenceInterval = "Mensal"
            )

            // 1. Add locally to Room
            onApproveTransaction(newTx)

            // 2. Mark suggestion as approved in Supabase
            updateRemoteSuggestionStatus(suggestion.id, "approved")
        }
    }

    fun rejectSuggestion(suggestionId: String) {
        scope.launch(Dispatchers.IO) {
            updateRemoteSuggestionStatus(suggestionId, "rejected")
        }
    }

    private suspend fun updateRemoteSuggestionStatus(suggestionId: String, newStatus: String) {
        val shareId = _activeShareId.value ?: return
        withContext(Dispatchers.IO) {
            try {
                val updatedArray = JSONArray()
                for (i in 0 until cachedSuggestionsJson.length()) {
                    val obj = cachedSuggestionsJson.getJSONObject(i)
                    if (obj.optString("id") == suggestionId) {
                        obj.put("status", newStatus)
                    }
                    updatedArray.put(obj)
                }
                cachedSuggestionsJson = updatedArray

                // Update in-memory state
                _pendingSuggestions.value = _pendingSuggestions.value.filter { it.id != suggestionId }

                // Update Supabase
                val payload = JSONObject().apply {
                    put("id", shareId)
                    put("suggestions", updatedArray)
                    put("updated_at", java.time.Instant.now().toString())
                }.toString()

                SupabaseClientProvider.postOrUpdate("shared_finance_dashboards", payload)
            } catch (e: Exception) {
                Log.e("SupabaseFinanceSync", "Error updating suggestion status", e)
            }
        }
    }

    private suspend fun uploadDashboardToSupabase(
        transactions: List<Transaction>,
        accounts: List<BankAccount>,
        cards: List<CreditCard>,
        benefits: List<BenefitCard>,
        debts: List<Debt> = emptyList()
    ) {
        val shareId = _activeShareId.value ?: return

        // Intervalo do mês atual
        val calendar = Calendar.getInstance()
        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR")) ?: "Mês Atual"
        val year = calendar.get(Calendar.YEAR)
        val monthLabel = "$monthName de $year".replaceFirstChar { it.uppercase() }

        val currentMonthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val currentMonthEnd = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val currentMonthTransactions = transactions.filter { it.timestamp in currentMonthStart..currentMonthEnd }

        var totalBalance = 0.0
        val categoryMap = mutableMapOf<String, Double>()

        transactions.forEach { tx ->
            if (tx.isIncome) {
                totalBalance += tx.value
            } else {
                totalBalance -= tx.value
            }
        }

        val monthIncome = currentMonthTransactions.filter { tx ->
            tx.isIncome &&
            !tx.category.trim().equals("Transferência", ignoreCase = true) &&
            !tx.category.trim().equals("Transferencia", ignoreCase = true) &&
            benefits.none { card -> card.name == tx.accountOrCardName }
        }.sumOf { it.value }

        val orphanRecurrentIncome = transactions.filter { tx ->
            tx.isIncome && tx.isRecurrent &&
            !tx.category.trim().equals("Transferência", ignoreCase = true) &&
            !tx.category.trim().equals("Transferencia", ignoreCase = true) &&
            benefits.none { card -> card.name == tx.accountOrCardName } &&
            currentMonthTransactions.none { it.id == tx.id || (it.isRecurrent && it.title.equals(tx.title, ignoreCase = true)) }
        }.sumOf { it.value }

        val checkingBalance = accounts.filter { it.type == "Corrente" }.sumOf { it.balance }
        val totalIncome = monthIncome + orphanRecurrentIncome
        val calculatedIncome = if (totalIncome > 0.0) totalIncome else if (checkingBalance > 0.0) checkingBalance else 0.0

        val monthExpense = currentMonthTransactions.filter { tx ->
            !tx.isIncome &&
            !tx.category.trim().equals("Transferência", ignoreCase = true) &&
            !tx.category.trim().equals("Transferencia", ignoreCase = true) &&
            benefits.none { card -> card.name == tx.accountOrCardName }
        }.sumOf { it.value }

        val orphanRecurrentExpense = transactions.filter { tx ->
            !tx.isIncome && tx.isRecurrent &&
            !tx.category.trim().equals("Transferência", ignoreCase = true) &&
            !tx.category.trim().equals("Transferencia", ignoreCase = true) &&
            benefits.none { card -> card.name == tx.accountOrCardName } &&
            currentMonthTransactions.none { it.id == tx.id || (it.isRecurrent && it.title.equals(tx.title, ignoreCase = true)) }
        }.sumOf { it.value }

        val calculatedExpense = monthExpense + orphanRecurrentExpense

        currentMonthTransactions.filter { !it.isIncome && !it.category.trim().equals("Transferência", ignoreCase = true) && !it.category.trim().equals("Transferencia", ignoreCase = true) }.forEach { tx ->
            categoryMap[tx.category] = (categoryMap[tx.category] ?: 0.0) + tx.value
        }

        // 1. Dívidas Ativas (Debts)
        val activeDebts = debts.filter { !it.isPaid }
        val debtsTotalOwed = activeDebts.sumOf { it.value }
        val debtsTotalPaid = activeDebts.sumOf { debt ->
            val installmentVal = if (debt.installmentsTotal > 0) debt.value / debt.installmentsTotal else debt.value
            installmentVal * debt.installmentsPaid
        }
        val debtsRemaining = debtsTotalOwed - debtsTotalPaid

        val debtsItemsArray = JSONArray()
        activeDebts.forEach { debt ->
            debtsItemsArray.put(JSONObject().apply {
                put("id", debt.id)
                put("title", debt.title)
                put("description", debt.description)
                put("value", debt.value)
                put("due_date", debt.dueDate)
                put("creditor_name", debt.creditorName)
                put("installments_total", debt.installmentsTotal)
                put("installments_paid", debt.installmentsPaid)
            })
        }

        val debtsSummaryObj = JSONObject().apply {
            put("count", activeDebts.size)
            put("total_owed", debtsTotalOwed)
            put("total_paid", debtsTotalPaid)
            put("remaining_to_pay", debtsRemaining)
            put("items", debtsItemsArray)
        }

        // 2. Despesas Parceladas (Installments)
        val isInstallmentTx: (Transaction) -> Boolean = { tx ->
            !tx.isIncome && (
                tx.subtitle.contains("Parcela", ignoreCase = true) ||
                tx.subtitle.contains("Parc.", ignoreCase = true) ||
                tx.title.contains("Parcela", ignoreCase = true) ||
                tx.title.contains("Parcelado", ignoreCase = true) ||
                tx.title.contains("Parcelamento", ignoreCase = true) ||
                tx.category.contains("Parcelad", ignoreCase = true) ||
                Regex("""\(\d+/\d+\)""").containsMatchIn(tx.title) ||
                Regex("""\b\d+/\d+\b""").containsMatchIn(tx.title) ||
                Regex("""\b\d+x\b""", RegexOption.IGNORE_CASE).containsMatchIn(tx.title)
            )
        }
        val allInstallmentTxs = transactions.filter(isInstallmentTx)

        val monthInstallmentTxs = allInstallmentTxs.filter { tx ->
            tx.timestamp in currentMonthStart..currentMonthEnd || (tx.dueDate > 0L && tx.dueDate in currentMonthStart..currentMonthEnd)
        }
        val totalMonthInstallmentValue = monthInstallmentTxs.sumOf { it.value }
        val totalAllInstallmentValue = allInstallmentTxs.sumOf { it.value }

        val installmentsItemsArray = JSONArray()
        monthInstallmentTxs.forEach { tx ->
            installmentsItemsArray.put(JSONObject().apply {
                put("id", tx.id)
                put("title", tx.title)
                put("subtitle", tx.subtitle)
                put("value", tx.value)
                put("category", tx.category)
                put("account_or_card_name", tx.accountOrCardName)
                put("date", if (tx.dueDate > 0L) tx.dueDate else tx.timestamp)
                put("is_current_month", true)
                put("is_realized", tx.isRealized)
            })
        }

        val allInstallmentsItemsArray = JSONArray()
        allInstallmentTxs.forEach { tx ->
            val isCurrentMonth = tx.timestamp in currentMonthStart..currentMonthEnd || (tx.dueDate > 0L && tx.dueDate in currentMonthStart..currentMonthEnd)
            allInstallmentsItemsArray.put(JSONObject().apply {
                put("id", tx.id)
                put("title", tx.title)
                put("subtitle", tx.subtitle)
                put("value", tx.value)
                put("category", tx.category)
                put("account_or_card_name", tx.accountOrCardName)
                put("date", if (tx.dueDate > 0L) tx.dueDate else tx.timestamp)
                put("is_current_month", isCurrentMonth)
                put("is_realized", tx.isRealized)
            })
        }

        // 3. Contas Bancárias (Accounts)
        val accountsArray = JSONArray()
        accounts.forEach { acc ->
            accountsArray.put(JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("type", acc.type)
                put("balance", acc.balance)
                put("color_hex", acc.colorHex)
            })
        }

        // 4. Cartões de Crédito e Benefício (Cards)
        val cardsArray = JSONArray()
        cards.forEach { card ->
            cardsArray.put(JSONObject().apply {
                put("id", card.id)
                put("name", card.name)
                put("type", "credit")
                put("limit", card.limit)
                put("used_limit", card.usedLimit)
                put("available_limit", (card.limit - card.usedLimit).coerceAtLeast(0.0))
                put("color_hex", card.colorHex)
            })
        }
        benefits.forEach { ben ->
            cardsArray.put(JSONObject().apply {
                put("id", ben.id)
                put("name", ben.name)
                put("type", "benefit")
                put("limit", ben.balance)
                put("used_limit", 0.0)
                put("available_limit", ben.balance)
                put("color_hex", ben.colorHex)
            })
        }

        val installmentsSummaryObj = JSONObject().apply {
            put("count", monthInstallmentTxs.size)
            put("total_month_value", totalMonthInstallmentValue)
            put("total_value", totalAllInstallmentValue)
            put("all_count", allInstallmentTxs.size)
            put("items", installmentsItemsArray)
            put("all_items", allInstallmentsItemsArray)
            put("accounts", accountsArray)
            put("cards", cardsArray)
        }

        // 5. Contas Fixas Recorrentes (Recurrents)
        val recurrentTxs = transactions.filter { tx -> !tx.isIncome && tx.isRecurrent }
        val totalRecurrentValue = recurrentTxs.sumOf { it.value }

        val recurrentsItemsArray = JSONArray()
        recurrentTxs.forEach { tx ->
            recurrentsItemsArray.put(JSONObject().apply {
                put("id", tx.id)
                put("title", tx.title)
                put("subtitle", tx.subtitle)
                put("value", tx.value)
                put("category", tx.category)
                put("account_or_card_name", tx.accountOrCardName)
                put("recurrence_interval", tx.recurrenceInterval)
            })
        }

        val recurrentsSummaryObj = JSONObject().apply {
            put("count", recurrentTxs.size)
            put("total_monthly_value", totalRecurrentValue)
            put("items", recurrentsItemsArray)
        }

        val fallbackSpendable = calculatedIncome - calculatedExpense

        val finalSpendable = currentSpendableBalance ?: fallbackSpendable
        val finalSalary = currentSalaryValue ?: calculatedIncome
        val finalCommitted = currentCommittedValue ?: calculatedExpense
        val finalCommittedPercent = currentCommittedPercentage ?: if (finalSalary > 0) ((finalCommitted / finalSalary) * 100.0).coerceIn(0.0, 100.0) else 0.0

        val currentHash = (transactions.hashCode() * 31) +
                (accounts.hashCode() * 19) +
                (cards.hashCode() * 23) +
                (benefits.hashCode() * 29) +
                (finalSpendable.hashCode() * 17) +
                (finalSalary.hashCode() * 13) +
                (finalCommitted.hashCode() * 7) +
                (debts.hashCode() * 5) +
                finalCommittedPercent.hashCode()

        if (currentHash == lastUploadedHash) return

        _syncStatus.value = SyncStatus.SYNCING
        withContext(Dispatchers.IO) {
            try {
                // Categorias JSON
                val categoriesArray = JSONArray()
                categoryMap.entries.sortedByDescending { it.value }.take(8).forEach { entry ->
                    val catObj = JSONObject().apply {
                        put("name", entry.key)
                        put("amount", entry.value)
                        put("percentage", if (calculatedExpense > 0) (entry.value / calculatedExpense) * 100 else 0.0)
                    }
                    categoriesArray.put(catObj)
                }

                // Últimas 30 transações JSON enriquecidas
                val txArray = JSONArray()
                transactions.take(30).forEach { tx ->
                    val obj = JSONObject().apply {
                        put("id", tx.id)
                        put("title", tx.title)
                        put("subtitle", tx.subtitle)
                        put("category", tx.category)
                        put("amount", tx.value)
                        put("type", if (tx.isIncome) "income" else "expense")
                        put("date", tx.timestamp)
                        put("is_recurrent", tx.isRecurrent)
                        put("account_or_card_name", tx.accountOrCardName)
                    }
                    txArray.put(obj)
                }

                val payload = JSONObject().apply {
                    put("id", shareId)
                    put("title", "Resumo Financeiro Tessera")
                    put("month_label", monthLabel)
                    put("total_balance", totalBalance)
                    put("spendable_balance", finalSpendable)
                    put("salary_value", finalSalary)
                    put("committed_value", finalCommitted)
                    put("committed_percentage", finalCommittedPercent)
                    put("categories", categoriesArray)
                    put("transactions", txArray)
                    put("debts", debtsSummaryObj)
                    put("installments", installmentsSummaryObj)
                    put("recurrents", recurrentsSummaryObj)
                    put("accounts", accountsArray)
                    put("cards", cardsArray)
                    put("suggestions", cachedSuggestionsJson)
                    put("is_live", true)
                    put("updated_at", java.time.Instant.now().toString())
                }.toString()

                var result = SupabaseClientProvider.postOrUpdate("shared_finance_dashboards", payload)
                if (!result.isSuccess && result.exceptionOrNull()?.message?.contains("Could not find the column", ignoreCase = true) == true) {
                    // Fallback retrocompatível: remove colunas adicionadas da raiz (accounts e cards continuam seguros em installments)
                    val fallbackPayload = JSONObject(payload).apply {
                        remove("accounts")
                        remove("cards")
                        val err = result.exceptionOrNull()?.message ?: ""
                        if (err.contains("debts", ignoreCase = true)) remove("debts")
                        if (err.contains("installments", ignoreCase = true)) remove("installments")
                        if (err.contains("recurrents", ignoreCase = true)) remove("recurrents")
                    }.toString()
                    result = SupabaseClientProvider.postOrUpdate("shared_finance_dashboards", fallbackPayload)
                }

                if (result.isSuccess) {
                    lastUploadedHash = currentHash
                    _syncStatus.value = SyncStatus.SYNCED
                    Log.d("SupabaseFinanceSync", "Successfully synced finance dashboard $shareId with Supabase")
                } else {
                    _syncStatus.value = SyncStatus.ERROR
                    Log.w("SupabaseFinanceSync", "Failed to sync finance dashboard: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                Log.e("SupabaseFinanceSync", "Exception syncing finance dashboard", e)
            }
        }
    }
}

