package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.MarketItem
import com.example.data.TesseraRepository
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
import java.util.UUID

class SupabaseMarketSyncManager(
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

    private var lastUploadedHash: Int? = null
    private var isUpdatingFromRemote = false

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
        lastUploadedHash = null
        context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("market_share_id", newId)
            .apply()
        triggerSync()
        return newId
    }

    fun startContinuousSync() {
        if (_activeShareId.value == null) {
            generateNewShareId()
        }

        // 1. Observe local changes and push to Supabase
        if (localSyncJob?.isActive != true) {
            localSyncJob = scope.launch {
                repository.pendingMarketItems.collect { items ->
                    if (!isUpdatingFromRemote) {
                        uploadListToSupabase(items)
                    }
                }
            }
        }

        // 2. Poll remote changes from Supabase (to pull items added on Web)
        if (remotePollJob?.isActive != true) {
            remotePollJob = scope.launch {
                while (isActive) {
                    delay(5000)
                    pullFromSupabase()
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
            pullFromSupabase()
            val items = repository.pendingMarketItems.first()
            uploadListToSupabase(items)
        }
    }

    suspend fun pullFromSupabase() {
        val shareId = _activeShareId.value ?: return
        withContext(Dispatchers.IO) {
            try {
                val result = SupabaseClientProvider.getDocument("shared_market_lists", shareId)
                if (result.isSuccess) {
                    val responseStr = result.getOrNull() ?: return@withContext
                    val jsonArray = JSONArray(responseStr)
                    if (jsonArray.length() == 0) return@withContext
                    val docObj = jsonArray.getJSONObject(0)
                    val itemsJson = docObj.optJSONArray("items") ?: return@withContext

                    val remoteItems = mutableListOf<MarketItem>()
                    for (i in 0 until itemsJson.length()) {
                        val itemObj = itemsJson.getJSONObject(i)
                        val name = itemObj.optString("name", "")
                        if (name.isBlank()) continue
                        val isChecked = itemObj.optBoolean("isChecked", false)
                        val isBought = itemObj.optBoolean("isBought", false)
                        val price = itemObj.optDouble("price", 0.0)
                        val quantity = itemObj.optDouble("quantity", 1.0)
                        val unit = itemObj.optString("unit", "un")
                        val category = itemObj.optString("category", "Geral")

                        remoteItems.add(
                            MarketItem(
                                name = name,
                                isChecked = isChecked,
                                isBought = isBought,
                                orderIndex = i,
                                price = price,
                                quantity = quantity,
                                unit = unit,
                                category = category
                            )
                        )
                    }

                    mergeRemoteItems(remoteItems)
                }
            } catch (e: Exception) {
                Log.w("SupabaseMarketSync", "Failed to pull from Supabase: ${e.message}")
            }
        }
    }

    private suspend fun mergeRemoteItems(remoteItems: List<MarketItem>) {
        if (remoteItems.isEmpty()) return
        val localItems = repository.pendingMarketItems.first()

        val inserts = mutableListOf<MarketItem>()
        val updates = mutableListOf<MarketItem>()

        remoteItems.forEach { remoteItem ->
            val existing = localItems.find { it.name.equals(remoteItem.name, ignoreCase = true) }
            if (existing != null) {
                if (existing.isChecked != remoteItem.isChecked ||
                    existing.price != remoteItem.price ||
                    existing.quantity != remoteItem.quantity ||
                    existing.unit != remoteItem.unit ||
                    existing.isBought != remoteItem.isBought
                ) {
                    updates.add(
                        existing.copy(
                            isChecked = remoteItem.isChecked,
                            price = remoteItem.price,
                            quantity = remoteItem.quantity,
                            unit = remoteItem.unit,
                            isBought = remoteItem.isBought
                        )
                    )
                }
            } else {
                inserts.add(remoteItem)
            }
        }

        if (inserts.isNotEmpty() || updates.isNotEmpty()) {
            isUpdatingFromRemote = true
            try {
                repository.syncMarketItems(inserts, updates, emptyList())
                val updatedLocal = repository.pendingMarketItems.first()
                lastUploadedHash = computeHash(updatedLocal)
            } finally {
                isUpdatingFromRemote = false
            }
        }
    }

    private fun computeHash(items: List<MarketItem>): Int {
        return items.map { "${it.name}|${it.isChecked}|${it.isBought}|${it.price}|${it.quantity}|${it.category}" }.hashCode()
    }

    private suspend fun uploadListToSupabase(items: List<MarketItem>) {
        val shareId = _activeShareId.value ?: return
        val currentHash = computeHash(items)

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

