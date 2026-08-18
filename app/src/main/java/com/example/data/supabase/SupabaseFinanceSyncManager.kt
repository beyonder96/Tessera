package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.TesseraRepository
import com.example.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class SupabaseFinanceSyncManager(
    private val context: Context,
    private val repository: TesseraRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _activeShareId = MutableStateFlow<String?>(null)
    val activeShareId: StateFlow<String?> = _activeShareId

    private var lastUploadedHash: Int? = null

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

    fun startContinuousSync() {
        if (syncJob?.isActive == true) return
        if (_activeShareId.value == null) {
            generateNewShareId()
        }

        syncJob = scope.launch {
            repository.allTransactions.collect { transactions ->
                uploadDashboardToSupabase(transactions)
            }
        }
    }

    fun stopContinuousSync() {
        syncJob?.cancel()
        syncJob = null
        _syncStatus.value = SyncStatus.IDLE
    }

    fun triggerSync() {
        lastUploadedHash = null
        scope.launch {
            val transactions = repository.allTransactions.first()
            uploadDashboardToSupabase(transactions)
        }
    }

    private suspend fun uploadDashboardToSupabase(transactions: List<Transaction>) {
        val shareId = _activeShareId.value ?: return
        val currentHash = transactions.hashCode()
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

                val payload = JSONObject().apply {
                    put("id", shareId)
                    put("title", "Resumo Financeiro Tessera")
                    put("month_label", monthLabel)
                    put("total_balance", totalBalance)
                    put("monthly_income", monthlyIncome)
                    put("monthly_expense", monthlyExpense)
                    put("categories", categoriesArray)
                    put("transactions", txArray)
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
