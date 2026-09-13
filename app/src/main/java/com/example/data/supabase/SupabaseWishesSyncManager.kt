package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.PurchaseGoal
import com.example.data.TesseraRepository
import com.example.notifications.NotificationHelper
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupabaseWishesSyncManager(
    private val context: Context,
    private val repository: TesseraRepository
) {
    private val TAG = "SupabaseWishesSync"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    private val prefs = context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
    private val hubId: String = prefs.getString("wishes_hub_id", null)?.takeIf { it.isNotBlank() } 
        ?: "wishes_default".also {
            prefs.edit().putString("wishes_hub_id", it).apply()
        }

    private val notifiedWishIds = mutableSetOf<String>().apply {
        addAll(prefs.getStringSet("notified_wish_ids", emptySet()) ?: emptySet())
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private var isUpdatingFromRemote = false
    private var lastUploadedHash: Int? = null

    enum class SyncStatus {
        IDLE,
        SYNCING,
        SYNCED,
        ERROR
    }

    fun getShareUrl(): String {
        return "${SupabaseClientProvider.getWebBaseUrl()}/wishes/$hubId"
    }

    fun startContinuousSync() {
        if (syncJob?.isActive == true) return

        syncJob = scope.launch {
            while (isActive) {
                try {
                    pullFromSupabase()
                    pushToSupabase()
                } catch (e: Exception) {
                    Log.e(TAG, "Erro no loop de sincronização de desejos", e)
                }
                delay(6000)
            }
        }
    }

    suspend fun pullFromSupabase() {
        if (!SupabaseClientProvider.isConfigured()) return
        withContext(Dispatchers.IO) {
            try {
                val result = SupabaseClientProvider.getDocument("shared_wishes_hub", hubId)
                val jsonStr = result.getOrNull()
                if (!jsonStr.isNullOrBlank() && jsonStr != "[]") {
                    val jsonObj = if (jsonStr.startsWith("[")) {
                        JSONArray(jsonStr).optJSONObject(0) ?: JSONObject()
                    } else {
                        JSONObject(jsonStr)
                    }

                    val itemsArray = jsonObj.optJSONArray("items") ?: JSONArray()
                    val localGoals = repository.allPurchaseGoals.first()

                    var hasNewFromWeb = false

                    for (i in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.optJSONObject(i) ?: continue
                        val idStr = itemObj.optString("id", "")
                        val title = itemObj.optString("title", "").trim()
                        if (title.isBlank()) continue

                        val targetValue = itemObj.optDouble("targetValue", 0.0)
                        val currentValue = itemObj.optDouble("currentValue", 0.0)
                        val imageUrl = itemObj.optString("imageUrl", "")
                        val buyUrl = itemObj.optString("buyUrl", "")
                        val category = itemObj.optString("category", "Geral")
                        val priority = itemObj.optString("priorityClassification", "Moderado")
                        val isBought = itemObj.optBoolean("isBought", false)
                        val createdBy = itemObj.optString("created_by", "")

                        // Encontra meta local com o mesmo título
                        val existing = localGoals.find { it.title.equals(title, ignoreCase = true) }

                        if (existing == null) {
                            // Não precisa de aprovação: insere diretamente no Room
                            val newGoal = PurchaseGoal(
                                title = title,
                                targetValue = targetValue,
                                currentValue = if (isBought) targetValue else currentValue,
                                imageUrl = imageUrl,
                                deadlineTimestamp = 0L,
                                colorHex = "#2DD4BF",
                                priorityOrder = when (priority) {
                                    "Urgente" -> 1
                                    "Alta" -> 2
                                    "Moderado" -> 3
                                    else -> 4
                                },
                                priorityClassification = priority,
                                isBought = isBought,
                                buyUrl = buyUrl,
                                category = category
                            )
                            isUpdatingFromRemote = true
                            repository.insertPurchaseGoal(newGoal)
                            isUpdatingFromRemote = false

                            // Notifica no aparelho se criado pela Web
                            if (createdBy != "App" && !notifiedWishIds.contains(idStr)) {
                                notifiedWishIds.add(idStr)
                                hasNewFromWeb = true
                                val priceFormatted = String.format(Locale("pt", "BR"), "R$ %,.2f", targetValue)
                                NotificationHelper.showTaskNoticeNotification(
                                    context = context,
                                    title = "✨ Novo Desejo Adicionado!",
                                    description = "$title ($priceFormatted)",
                                    timeOrDate = category,
                                    taskId = idStr.ifBlank { title }
                                )
                            }
                        } else {
                            // Sincroniza status se alterado na Web (ex: comprado)
                            var needsUpdate = false
                            var updated = existing

                            if (isBought != existing.isBought) {
                                updated = updated.copy(
                                    isBought = isBought,
                                    currentValue = if (isBought) existing.targetValue else existing.currentValue
                                )
                                needsUpdate = true
                            }
                            if (existing.buyUrl.isBlank() && buyUrl.isNotBlank()) {
                                updated = updated.copy(buyUrl = buyUrl)
                                needsUpdate = true
                            }
                            if (existing.imageUrl.isBlank() && imageUrl.isNotBlank()) {
                                updated = updated.copy(imageUrl = imageUrl)
                                needsUpdate = true
                            }

                            if (needsUpdate) {
                                isUpdatingFromRemote = true
                                repository.updatePurchaseGoal(updated)
                                isUpdatingFromRemote = false
                            }
                        }
                    }

                    if (hasNewFromWeb) {
                        prefs.edit().putStringSet("notified_wish_ids", notifiedWishIds).apply()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao puxar desejos do Supabase", e)
            }
        }
    }

    suspend fun pushToSupabase() {
        if (isUpdatingFromRemote || !SupabaseClientProvider.isConfigured()) return
        withContext(Dispatchers.IO) {
            try {
                val localGoals = repository.allPurchaseGoals.first()
                val currentHash = localGoals.hashCode()
                if (currentHash == lastUploadedHash) return@withContext

                _syncStatus.value = SyncStatus.SYNCING

                val itemsArray = JSONArray()
                localGoals.forEach { goal ->
                    val itemObj = JSONObject().apply {
                        put("id", "goal_${goal.id}")
                        put("title", goal.title)
                        put("targetValue", goal.targetValue)
                        put("currentValue", goal.currentValue)
                        put("imageUrl", goal.imageUrl)
                        put("buyUrl", goal.buyUrl)
                        put("category", goal.category)
                        put("priorityClassification", goal.priorityClassification)
                        put("isBought", goal.isBought)
                        put("created_by", "App")
                        put("created_at", System.currentTimeMillis())
                    }
                    itemsArray.put(itemObj)
                }

                val payload = JSONObject().apply {
                    put("id", hubId)
                    put("title", "Lista de Desejos")
                    put("items", itemsArray)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                }

                val result = SupabaseClientProvider.postOrUpdate("shared_wishes_hub", payload.toString())
                if (result.isSuccess) {
                    lastUploadedHash = currentHash
                    _syncStatus.value = SyncStatus.SYNCED
                } else {
                    _syncStatus.value = SyncStatus.ERROR
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao enviar desejos para o Supabase", e)
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }
}
