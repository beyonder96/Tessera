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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
    val status: String,
    val action: String = "create",
    val targetTxId: Long? = null,
    val originalTitle: String? = null,
    val originalAmount: Double? = null,
    val accountOrCardName: String? = null
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
    private var cachedAccountsJson = JSONArray()
    private var cachedCardsJson = JSONArray()
    @Volatile
    private var hasInitialPullCompleted = false

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
        val newId = UUID.randomUUID().toString()
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

        // 1. Poll remote suggestions from Supabase (executa com prioridade máxima)
        if (remotePollJob?.isActive != true) {
            remotePollJob = scope.launch {
                while (isActive) {
                    pullSuggestionsFromSupabase()
                    hasInitialPullCompleted = true
                    delay(5000)
                }
            }
        }

        // 2. Observe local transactions, accounts, cards, benefits and debts and push to Supabase in real-time
        if (localSyncJob?.isActive != true) {
            localSyncJob = scope.launch {
                // Aguarda até o primeiro pull remoto concluir (máx 3s para tolerar modo offline sem travar)
                var waitCycles = 0
                while (!hasInitialPullCompleted && waitCycles < 30 && isActive) {
                    delay(100)
                    waitCycles++
                }

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
            hasInitialPullCompleted = true
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

                    // Sincronizar e armazenar contas criadas na Web
                    val remoteAccounts = docObj.optJSONArray("accounts")
                    if (remoteAccounts != null) {
                        cachedAccountsJson = remoteAccounts
                        try {
                            val localAccounts = repository.allBankAccounts.first()
                            for (i in 0 until remoteAccounts.length()) {
                                val accObj = remoteAccounts.getJSONObject(i)
                                val name = accObj.optString("name", "").trim()
                                if (name.isNotEmpty() && localAccounts.none { it.name.equals(name, ignoreCase = true) }) {
                                    repository.insertBankAccount(
                                        BankAccount(
                                            name = name,
                                            balance = accObj.optDouble("balance", 0.0),
                                            type = accObj.optString("type", "Corrente"),
                                            colorHex = accObj.optString("color_hex", "#4A90E2")
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("SupabaseFinanceSync", "Erro ao sincronizar contas da Web: ${e.message}")
                        }
                    }

                    // Sincronizar e armazenar cartões criados na Web
                    val remoteCards = docObj.optJSONArray("cards")
                    if (remoteCards != null) {
                        cachedCardsJson = remoteCards
                        try {
                            val localCards = repository.allCreditCards.first()
                            for (i in 0 until remoteCards.length()) {
                                val cardObj = remoteCards.getJSONObject(i)
                                val name = cardObj.optString("name", "").trim()
                                if (name.isNotEmpty() && localCards.none { it.name.equals(name, ignoreCase = true) }) {
                                    repository.insertCreditCard(
                                        CreditCard(
                                            name = name,
                                            limit = cardObj.optDouble("limit", 0.0),
                                            usedLimit = cardObj.optDouble("used_limit", 0.0),
                                            numberLastFour = "0000",
                                            colorHex = cardObj.optString("color_hex", "#71D7CD"),
                                            holderName = "Web"
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("SupabaseFinanceSync", "Erro ao sincronizar cartões da Web: ${e.message}")
                        }
                    }

                    val pendingList = mutableListOf<FinanceSuggestion>()
                    for (i in 0 until suggestionsJson.length()) {
                        val sugObj = suggestionsJson.getJSONObject(i)
                        val status = sugObj.optString("status", "pending")
                        val isAutoApproved = status == "auto_approved" || sugObj.optBoolean("auto_approved", false)

                        if (isAutoApproved && status != "approved" && status != "rejected") {
                            // Transação de conta/cartão próprio: aprova e insere no Room imediatamente
                            handleAutoApprovedSuggestion(sugObj)
                        } else if (status == "pending") {
                            val action = sugObj.optString("action", "create")
                            val targetTxId = if (sugObj.has("target_tx_id") && !sugObj.isNull("target_tx_id")) sugObj.optLong("target_tx_id") else null
                            val originalTitle = if (sugObj.has("original_title") && !sugObj.isNull("original_title")) sugObj.optString("original_title") else null
                            val originalAmount = if (sugObj.has("original_amount") && !sugObj.isNull("original_amount")) sugObj.optDouble("original_amount") else null
                            val accountOrCard = if (sugObj.has("account_or_card_name") && !sugObj.isNull("account_or_card_name")) sugObj.optString("account_or_card_name") else null

                            pendingList.add(
                                FinanceSuggestion(
                                    id = sugObj.optString("id", UUID.randomUUID().toString()),
                                    title = sugObj.optString("title", "Sem título"),
                                    amount = sugObj.optDouble("amount", 0.0),
                                    type = sugObj.optString("type", "expense"),
                                    category = sugObj.optString("category", "Geral"),
                                    date = sugObj.optString("date", ""),
                                    createdAt = sugObj.optString("created_at", ""),
                                    status = status,
                                    action = action,
                                    targetTxId = targetTxId,
                                    originalTitle = originalTitle,
                                    originalAmount = originalAmount,
                                    accountOrCardName = accountOrCard
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

    private suspend fun handleAutoApprovedSuggestion(sugObj: JSONObject) {
        try {
            val sugId = sugObj.optString("id", UUID.randomUUID().toString())
            val sugTitle = sugObj.optString("title", "Sem título")
            val sugAmount = sugObj.optDouble("amount", 0.0)
            val isInc = sugObj.optString("type", "expense").equals("income", ignoreCase = true)
            val sugCat = sugObj.optString("category", "Geral")
            val accountOrCard = if (sugObj.has("account_or_card_name") && !sugObj.isNull("account_or_card_name")) sugObj.optString("account_or_card_name") else ""
            val dueDateLong = sugObj.optLong("due_date", 0L).let { if (it > 0L) it else System.currentTimeMillis() }

            val newTx = Transaction(
                title = sugTitle,
                subtitle = "Via Web • $sugCat",
                value = sugAmount,
                isIncome = isInc,
                timestamp = dueDateLong,
                category = sugCat,
                accountOrCardName = accountOrCard,
                isRealized = true,
                isRecurrent = false,
                recurrenceInterval = "Mensal",
                dueDate = dueDateLong
            )
            repository.insertTransaction(newTx)

            if (accountOrCard.isNotEmpty()) {
                val bankAccounts = repository.allBankAccounts.first()
                val matchingAccount = bankAccounts.find { it.name.equals(accountOrCard, ignoreCase = true) }
                if (matchingAccount != null) {
                    val newBalance = if (isInc) matchingAccount.balance + sugAmount else matchingAccount.balance - sugAmount
                    repository.insertBankAccount(matchingAccount.copy(balance = newBalance))
                } else {
                    val cards = repository.allCreditCards.first()
                    val matchingCard = cards.find { it.name.equals(accountOrCard, ignoreCase = true) }
                    if (matchingCard != null) {
                        val newUsed = if (isInc) matchingCard.usedLimit - sugAmount else matchingCard.usedLimit + sugAmount
                        repository.insertCreditCard(matchingCard.copy(usedLimit = newUsed.coerceAtLeast(0.0)))
                    }
                }
            }
            updateRemoteSuggestionStatus(sugId, "approved")
        } catch (e: Exception) {
            Log.e("SupabaseFinanceSync", "Erro ao processar sugestao auto-aprovada", e)
        }
    }

    fun approveSuggestion(
        suggestion: FinanceSuggestion,
        accountOrCardName: String = "",
        onApproveTransaction: (Transaction) -> Unit,
        onUpdateTransaction: ((Transaction) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.IO) {
            val isIncome = suggestion.type.equals("income", ignoreCase = true)
            if (suggestion.action.equals("edit", ignoreCase = true) && suggestion.targetTxId != null) {
                val allTxs = repository.allTransactions.first()
                val existingTx = allTxs.find { it.id.toLong() == suggestion.targetTxId }
                if (existingTx != null) {
                    val updatedTx = existingTx.copy(
                        title = suggestion.title,
                        value = suggestion.amount,
                        isIncome = isIncome,
                        category = suggestion.category,
                        accountOrCardName = if (!suggestion.accountOrCardName.isNullOrBlank()) suggestion.accountOrCardName else existingTx.accountOrCardName
                    )
                    if (onUpdateTransaction != null) {
                        onUpdateTransaction(updatedTx)
                    } else {
                        repository.insertTransaction(updatedTx)
                    }
                }
            } else {
                val newTx = Transaction(
                    title = suggestion.title,
                    subtitle = "Via Web • ${suggestion.category}",
                    value = suggestion.amount,
                    isIncome = isIncome,
                    timestamp = System.currentTimeMillis(),
                    category = suggestion.category,
                    accountOrCardName = if (!suggestion.accountOrCardName.isNullOrBlank()) suggestion.accountOrCardName else accountOrCardName,
                    isRealized = true,
                    isRecurrent = false,
                    recurrenceInterval = "Mensal"
                )

                // 1. Add locally to Room
                onApproveTransaction(newTx)
            }

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

        // 1. Dívidas Ativas (Debts) - Combina tabela debts com lançamentos vencidos não realizados (igual DebtsScreen)
        val overdueTxs = transactions.filter { !it.isIncome && !it.isRealized && it.dueDate > 0L && it.dueDate < System.currentTimeMillis() }
        val syntheticOverdueDebts = overdueTxs.map { tx ->
            com.example.data.Debt(
                id = -tx.id,
                title = tx.title,
                description = "Vencida em ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(tx.dueDate))}",
                value = tx.value,
                dueDate = tx.dueDate,
                isPaid = false,
                creditorName = if (tx.accountOrCardName.isNotBlank()) tx.accountOrCardName else "Lançamento Vencido",
                installmentsTotal = 1,
                installmentsPaid = 0
            )
        }
        val combinedDebts = debts + syntheticOverdueDebts
        val activeDebts = combinedDebts.filter { !it.isPaid }
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
        val creditCardNames = cards.map { it.name.trim().lowercase() }
        val isInstallmentTx: (Transaction) -> Boolean = { tx ->
            val txAcc = tx.accountOrCardName.trim().lowercase()
            !tx.isIncome && (
                tx.subtitle.contains("Parcela", ignoreCase = true) ||
                tx.subtitle.contains("Parc.", ignoreCase = true) ||
                tx.subtitle.contains("de", ignoreCase = true) ||
                tx.title.contains("Parcela", ignoreCase = true) ||
                tx.title.contains("Parcelado", ignoreCase = true) ||
                tx.title.contains("Parcelamento", ignoreCase = true) ||
                tx.category.contains("Parcelad", ignoreCase = true) ||
                Regex("""\(\d+/\d+\)""").containsMatchIn(tx.title) ||
                Regex("""\b\d+/\d+\b""").containsMatchIn(tx.title) ||
                Regex("""\b\d+x\b""", RegexOption.IGNORE_CASE).containsMatchIn(tx.title) ||
                tx.title.trim().equals("Cartao", ignoreCase = true) ||
                tx.title.trim().equals("Cartão", ignoreCase = true) ||
                (txAcc.isNotBlank() && creditCardNames.contains(txAcc)) ||
                txAcc.contains("credito") ||
                txAcc.contains("crédito")
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
            val isMine = run {
                for (i in 0 until cachedAccountsJson.length()) {
                    val obj = cachedAccountsJson.optJSONObject(i)
                    if (obj?.optString("name")?.equals(acc.name, ignoreCase = true) == true) {
                        return@run obj.optBoolean("is_mine", false)
                    }
                }
                false
            }
            accountsArray.put(JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("type", acc.type)
                put("balance", acc.balance)
                put("color_hex", acc.colorHex)
                if (isMine) put("is_mine", true)
            })
        }
        for (i in 0 until cachedAccountsJson.length()) {
            val obj = cachedAccountsJson.optJSONObject(i) ?: continue
            val accName = obj.optString("name", "")
            if (accName.isNotEmpty() && accounts.none { it.name.equals(accName, ignoreCase = true) }) {
                accountsArray.put(obj)
            }
        }

        // 4. Cartões de Crédito e Benefício (Cards)
        val cardsArray = JSONArray()
        cards.forEach { card ->
            val isMine = run {
                for (i in 0 until cachedCardsJson.length()) {
                    val obj = cachedCardsJson.optJSONObject(i)
                    if (obj?.optString("name")?.equals(card.name, ignoreCase = true) == true) {
                        return@run obj.optBoolean("is_mine", false)
                    }
                }
                false
            }
            val cardMonthTxsSum = currentMonthTransactions.filter { 
                !it.isIncome && it.accountOrCardName.equals(card.name, ignoreCase = true) 
            }.sumOf { it.value }
            val effectiveUsedLimit = maxOf(card.usedLimit, cardMonthTxsSum)

            cardsArray.put(JSONObject().apply {
                put("id", card.id)
                put("name", card.name)
                put("type", "credit")
                put("limit", card.limit)
                put("used_limit", effectiveUsedLimit)
                put("available_limit", (card.limit - effectiveUsedLimit).coerceAtLeast(0.0))
                put("color_hex", card.colorHex)
                if (isMine) put("is_mine", true)
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
        for (i in 0 until cachedCardsJson.length()) {
            val obj = cachedCardsJson.optJSONObject(i) ?: continue
            val cardName = obj.optString("name", "")
            if (cardName.isNotEmpty() && cards.none { it.name.equals(cardName, ignoreCase = true) } && benefits.none { it.name.equals(cardName, ignoreCase = true) }) {
                cardsArray.put(obj)
            }
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
                // Se as sugestões em cache ainda não foram carregadas, tenta um pull antes de sobrescrever
                if (cachedSuggestionsJson.length() == 0) {
                    pullSuggestionsFromSupabase()
                }

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

                // Histórico enriquecido com as últimas 200 transações ordenadas cronologicamente
                val txArray = JSONArray()
                transactions.sortedByDescending { if (it.dueDate > 0L) it.dueDate else it.timestamp }.take(200).forEach { tx ->
                    val obj = JSONObject().apply {
                        put("id", tx.id)
                        put("title", tx.title)
                        put("subtitle", tx.subtitle)
                        put("category", tx.category)
                        put("amount", tx.value)
                        put("type", if (tx.isIncome) "income" else "expense")
                        put("date", if (tx.dueDate > 0L) tx.dueDate else tx.timestamp)
                        put("due_date", tx.dueDate)
                        put("is_realized", tx.isRealized)
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

