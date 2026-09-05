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
import kotlinx.coroutines.flow.combine
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

        // 1. Observe local changes (planejamento + mercado) and push to Supabase
        if (localSyncJob?.isActive != true) {
            localSyncJob = scope.launch {
                combine(repository.pendingMarketItems, repository.shoppingMarketItems) { pending, shopping ->
                    pending + shopping
                }.collect { allActiveItems ->
                    if (!isUpdatingFromRemote) {
                        uploadListToSupabase(allActiveItems)
                    }
                }
            }
        }

        // 2. Poll remote changes from Supabase (to pull items added or checked on Web)
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
            val pending = repository.pendingMarketItems.first()
            val shopping = repository.shoppingMarketItems.first()
            uploadListToSupabase(pending + shopping)
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
                        val inMarket = itemObj.optBoolean("inMarket", false)
                        val needsApproval = itemObj.optBoolean("needsApproval", false)

                        remoteItems.add(
                            MarketItem(
                                name = name,
                                isChecked = isChecked,
                                isBought = isBought,
                                orderIndex = i,
                                price = price,
                                quantity = quantity,
                                unit = unit,
                                category = category,
                                inMarket = inMarket,
                                needsApproval = needsApproval
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
        val localPending = repository.pendingMarketItems.first()
        val localShopping = repository.shoppingMarketItems.first()
        val allLocal = localPending + localShopping

        val inserts = mutableListOf<MarketItem>()
        val updates = mutableListOf<MarketItem>()

        remoteItems.forEach { remoteItem ->
            val existing = allLocal.find { it.name.equals(remoteItem.name, ignoreCase = true) }
            if (existing != null) {
                // Se a web enviou para o mercado, mas o item ainda não foi aprovado localmente:
                val resolvedNeedsApproval = if (remoteItem.inMarket && !existing.inMarket) {
                    true
                } else {
                    existing.needsApproval && remoteItem.needsApproval
                }

                // Se requer aprovação, não deixa entrar no carrinho (isChecked = false) até o usuário aprovar no app
                val resolvedChecked = if (resolvedNeedsApproval) false else existing.isChecked

                val resolvedInMarket = if (remoteItem.inMarket) true else existing.inMarket

                if (existing.isChecked != resolvedChecked ||
                    existing.inMarket != resolvedInMarket ||
                    existing.needsApproval != resolvedNeedsApproval ||
                    existing.price != remoteItem.price ||
                    existing.quantity != remoteItem.quantity ||
                    existing.unit != remoteItem.unit ||
                    existing.isBought != remoteItem.isBought
                ) {
                    updates.add(
                        existing.copy(
                            isChecked = resolvedChecked,
                            inMarket = resolvedInMarket,
                            needsApproval = resolvedNeedsApproval,
                            price = remoteItem.price,
                            quantity = remoteItem.quantity,
                            unit = remoteItem.unit,
                            isBought = remoteItem.isBought
                        )
                    )
                }
            } else {
                // Item novo vindo da web
                val resolvedChecked = if (remoteItem.inMarket && remoteItem.needsApproval) false else remoteItem.isChecked
                inserts.add(remoteItem.copy(isChecked = resolvedChecked))
            }
        }

        if (inserts.isNotEmpty() || updates.isNotEmpty()) {
            isUpdatingFromRemote = true
            try {
                repository.syncMarketItems(inserts, updates, emptyList())
                val updatedPending = repository.pendingMarketItems.first()
                val updatedShopping = repository.shoppingMarketItems.first()
                lastUploadedHash = computeHash(updatedPending + updatedShopping)
            } finally {
                isUpdatingFromRemote = false
            }
        }
    }

    private fun computeHash(items: List<MarketItem>): Int {
        return items.map { "${it.name}|${it.isChecked}|${it.isBought}|${it.price}|${it.quantity}|${it.category}|${it.inMarket}|${it.needsApproval}" }.hashCode()
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
                        put("inMarket", item.inMarket)
                        put("needsApproval", item.needsApproval)
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
