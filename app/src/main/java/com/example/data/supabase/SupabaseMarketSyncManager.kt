package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.MarketItem
import com.example.data.TesseraRepository
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
import java.util.UUID

class SupabaseMarketSyncManager(
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
        _activeShareId.value = prefs.getString("market_share_id", null)
    }

    fun getShareUrl(): String {
        val id = _activeShareId.value ?: generateNewShareId()
        return "${SupabaseClientProvider.getWebBaseUrl()}/market/$id"
    }

    fun generateNewShareId(): String {
        val newId = UUID.randomUUID().toString().take(8)
        _activeShareId.value = newId
        context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("market_share_id", newId)
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
            repository.pendingMarketItems.collect { items ->
                uploadListToSupabase(items)
            }
        }
    }

    fun stopContinuousSync() {
        syncJob?.cancel()
        syncJob = null
        _syncStatus.value = SyncStatus.IDLE
    }

    fun triggerSync() {
        scope.launch {
            val items = repository.pendingMarketItems.first()
            uploadListToSupabase(items)
        }
    }

    private suspend fun uploadListToSupabase(items: List<MarketItem>) {
        val shareId = _activeShareId.value ?: return
        val currentHash = items.map { "${it.name}|${it.isChecked}|${it.isBought}|${it.price}|${it.quantity}|${it.category}" }.hashCode()

        if (currentHash == lastUploadedHash) return

        _syncStatus.value = SyncStatus.SYNCING
        withContext(Dispatchers.IO) {
            try {
                val jsonArray = JSONArray()
                items.forEach { item ->
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("name", item.name)
                        put("isChecked", item.isChecked)
                        put("isBought", item.isBought)
                        put("price", item.price)
                        put("quantity", item.quantity)
                        put("unit", item.unit)
                        put("category", item.category)
                    }
                    jsonArray.put(obj)
                }

                val payload = JSONObject().apply {
                    put("id", shareId)
                    put("title", "Lista de Mercado")
                    put("items", jsonArray)
                    put("updated_at", java.time.Instant.now().toString())
                }.toString()

                val result = SupabaseClientProvider.postOrUpdate("shared_market_lists", payload)
                if (result.isSuccess) {
                    lastUploadedHash = currentHash
                    _syncStatus.value = SyncStatus.SYNCED
                    Log.d("SupabaseMarketSync", "Successfully synced market list $shareId with Supabase")
                } else {
                    _syncStatus.value = SyncStatus.ERROR
                    Log.w("SupabaseMarketSync", "Failed to sync market list: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                Log.e("SupabaseMarketSync", "Exception syncing market list", e)
            }
        }
    }
}
