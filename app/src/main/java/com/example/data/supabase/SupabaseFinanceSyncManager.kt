package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.TesseraRepository
import com.example.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

        // 1. Observe local transactions and push to Supabase
        if (localSyncJob?.isActive != true) {
            localSyncJob = scope.launch {
                repository.allTransactions.collect { transactions ->
                    uploadDashboardToSupabase(transactions)
                }
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
            uploadDashboardToSupabase(transactions)
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

    private suspend fun uploadDashboardToSupabase(transactions: List<Transaction>) {
        val shareId = _activeShareId.value ?: return
        val currentHash = (transactions.hashCode() * 31) +
                (currentSpendableBalance?.hashCode() ?: 0) * 17 +
                (currentSalaryValue?.hashCode() ?: 0) * 13 +
                (currentCommittedValue?.hashCode() ?: 0) * 7 +
                (currentCommittedPercentage?.hashCode() ?: 0)
        if (currentHash == lastUploadedHash) return

        _syncStatus.value = SyncStatus.SYNCING
        withContext(Dispatchers.IO) {
            try {
                val calendar = Calendar.getInstance()
                val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR")) ?: "Mês Atual"
                val year = calendar.get(Calendar.YEAR)
                val monthLabel = "$monthName de $year".replaceFirstChar { it.uppercase() }

                // Cálculos de saldo, receitas e despesas
                var totalBalance = 0.0
                var monthlyIncome = 0.0
                var monthlyExpense = 0.0
                val categoryMap = mutableMapOf<String, Double>()

                transactions.forEach { tx ->
                    if (tx.isIncome) {
                        totalBalance += tx.value
                        monthlyIncome += tx.value
                    } else {
                        totalBalance -= tx.value
                        monthlyExpense += tx.value
                        categoryMap[tx.category] = (categoryMap[tx.category] ?: 0.0) + tx.value
                    }
                }

                // Categorias JSON
                val categoriesArray = JSONArray()
                categoryMap.entries.sortedByDescending { it.value }.take(8).forEach { entry ->
                    val catObj = JSONObject().apply {
                        put("name", entry.key)
                        put("amount", entry.value)
                        put("percentage", if (monthlyExpense > 0) (entry.value / monthlyExpense) * 100 else 0.0)
                    }
                    categoriesArray.put(catObj)
                }

                // Últimas 20 transações JSON
                val txArray = JSONArray()
                transactions.take(20).forEach { tx ->
                    val obj = JSONObject().apply {
                        put("id", tx.id)
                        put("title", tx.title)
                        put("category", tx.category)
                        put("amount", tx.value)
                        put("type", if (tx.isIncome) "income" else "expense")
                        put("date", tx.timestamp)
                    }
                    txArray.put(obj)
                }

                val accounts = repository.allBankAccounts.first()
                val checkingBalance = accounts.filter { it.type == "Corrente" }.sumOf { it.balance }
                val fallbackSpendable = if (accounts.isNotEmpty()) checkingBalance else totalBalance

                val finalSpendable = currentSpendableBalance ?: fallbackSpendable
                val finalSalary = currentSalaryValue ?: monthlyIncome
                val finalCommitted = currentCommittedValue ?: monthlyExpense
                val finalCommittedPercent = currentCommittedPercentage ?: if (finalSalary > 0) (finalCommitted / finalSalary) * 100 else 0.0

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
                    put("suggestions", cachedSuggestionsJson)
                    put("is_live", true)
                    put("updated_at", java.time.Instant.now().toString())
                }.toString()

                val result = SupabaseClientProvider.postOrUpdate("shared_finance_dashboards", payload)
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

