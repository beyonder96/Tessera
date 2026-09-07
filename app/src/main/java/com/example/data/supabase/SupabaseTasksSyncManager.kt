package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SharedTaskItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val type: String, // "notice" | "task"
    val target_user: String, // "kenned" | "me"
    val due_date: Long? = null,
    val due_time: String? = null,
    val status: String, // "pending" | "approved" | "completed" | "dismissed"
    val created_by: String,
    val created_at: Long,
    val completed_at: Long? = null
)

class SupabaseTasksSyncManager(
    private val context: Context
) {
    private val TAG = "SupabaseTasksSync"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    private val prefs = context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
    private val hubId: String = prefs.getString("tasks_hub_id", null) ?: "tasks_default".also {
        prefs.edit().putString("tasks_hub_id", it).apply()
    }

    private val notifiedTaskIds = mutableSetOf<String>().apply {
        addAll(prefs.getStringSet("notified_task_ids", emptySet()) ?: emptySet())
    }

    private val _tasks = MutableStateFlow<List<SharedTaskItem>>(emptyList())
    val tasks: StateFlow<List<SharedTaskItem>> = _tasks

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Carrega cache local se existir
        loadFromCache()
    }

    fun startContinuousSync() {
        if (syncJob?.isActive == true) return

        syncJob = scope.launch {
            while (isActive) {
                try {
                    pullFromSupabase()
                } catch (e: Exception) {
                    Log.e(TAG, "Erro no loop de sincronização de tarefas", e)
                }
                delay(6000)
            }
        }
    }

    fun getShareUrl(): String {
        return "${SupabaseClientProvider.getWebBaseUrl()}/tasks/$hubId"
    }

    suspend fun pullFromSupabase() {
        if (!SupabaseClientProvider.isConfigured()) return
        try {
            val result = SupabaseClientProvider.getDocument("shared_tasks_hub", hubId)
            val jsonStr = result.getOrNull()
            if (!jsonStr.isNullOrBlank() && jsonStr != "[]") {
                val jsonObj = if (jsonStr.startsWith("[")) {
                    JSONArray(jsonStr).optJSONObject(0) ?: JSONObject()
                } else {
                    JSONObject(jsonStr)
                }

                val itemsArray = jsonObj.optJSONArray("items") ?: JSONArray()
                val parsedItems = mutableListOf<SharedTaskItem>()

                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.optJSONObject(i) ?: continue
                    val id = itemObj.optString("id")
                    if (id.isBlank()) continue

                    val item = SharedTaskItem(
                        id = id,
                        title = itemObj.optString("title", "Sem título"),
                        description = itemObj.optString("description").takeIf { it.isNotBlank() },
                        type = itemObj.optString("type", "notice"),
                        target_user = itemObj.optString("target_user", "kenned"),
                        due_date = if (itemObj.has("due_date") && !itemObj.isNull("due_date")) itemObj.optLong("due_date") else null,
                        due_time = itemObj.optString("due_time").takeIf { it.isNotBlank() },
                        status = itemObj.optString("status", "pending"),
                        created_by = itemObj.optString("created_by", "Web"),
                        created_at = itemObj.optLong("created_at", System.currentTimeMillis()),
                        completed_at = if (itemObj.has("completed_at") && !itemObj.isNull("completed_at")) itemObj.optLong("completed_at") else null
                    )
                    parsedItems.add(item)
                }

                _tasks.value = parsedItems
                val pending = parsedItems.count { it.target_user == "kenned" && it.status == "pending" }
                _pendingCount.value = pending

                // Salva em cache
                prefs.edit().putString("cached_tasks_json", itemsArray.toString()).apply()

                // Checa novos avisos para o Kenned e dispara som prioritário
                checkAndNotifyNewNotices(parsedItems)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao puxar tarefas do Supabase", e)
        }
    }

    private fun checkAndNotifyNewNotices(items: List<SharedTaskItem>) {
        var hasNew = false
        items.forEach { item ->
            if (item.target_user == "kenned" && item.status == "pending" && !notifiedTaskIds.contains(item.id)) {
                notifiedTaskIds.add(item.id)
                hasNew = true

                val timeOrDate = buildString {
                    item.due_date?.let {
                        append(SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(Date(it)))
                    }
                    item.due_time?.let {
                        if (isNotEmpty()) append(" às ")
                        append(it)
                    }
                }.takeIf { it.isNotBlank() }

                NotificationHelper.showTaskNoticeNotification(
                    context = context,
                    title = item.title,
                    description = item.description,
                    timeOrDate = timeOrDate,
                    taskId = item.id
                )
            }
        }

        if (hasNew) {
            prefs.edit().putStringSet("notified_task_ids", notifiedTaskIds).apply()
        }
    }

    fun approveNotice(taskId: String) {
        scope.launch {
            val current = _tasks.value.toMutableList()
            val index = current.indexOfFirst { it.id == taskId }
            if (index >= 0) {
                current[index] = current[index].copy(status = "approved")
                _tasks.value = current
                _pendingCount.value = current.count { it.target_user == "kenned" && it.status == "pending" }
                uploadToSupabase(current)
            }
        }
    }

    fun completeTask(taskId: String) {
        scope.launch {
            val current = _tasks.value.toMutableList()
            val index = current.indexOfFirst { it.id == taskId }
            if (index >= 0) {
                val isDone = current[index].status == "completed"
                val newStatus = if (isDone) "approved" else "completed"
                val completedAt = if (isDone) null else System.currentTimeMillis()
                current[index] = current[index].copy(status = newStatus, completed_at = completedAt)
                _tasks.value = current
                _pendingCount.value = current.count { it.target_user == "kenned" && it.status == "pending" }
                uploadToSupabase(current)
            }
        }
    }

    fun createTask(
        title: String,
        description: String?,
        dueDate: Long?,
        dueTime: String?,
        type: String = "task"
    ) {
        scope.launch {
            val newItem = SharedTaskItem(
                id = "task_android_${System.currentTimeMillis()}",
                title = title.trim(),
                description = description?.trim()?.takeIf { it.isNotBlank() },
                type = type,
                target_user = "kenned",
                due_date = dueDate,
                due_time = dueTime?.trim()?.takeIf { it.isNotBlank() },
                status = "approved",
                created_by = "Kenned",
                created_at = System.currentTimeMillis()
            )

            val updated = listOf(newItem) + _tasks.value
            _tasks.value = updated
            uploadToSupabase(updated)
        }
    }

    fun deleteTask(taskId: String) {
        scope.launch {
            val updated = _tasks.value.filter { it.id != taskId }
            _tasks.value = updated
            _pendingCount.value = updated.count { it.target_user == "kenned" && it.status == "pending" }
            uploadToSupabase(updated)
        }
    }

    private suspend fun uploadToSupabase(items: List<SharedTaskItem>) {
        if (!SupabaseClientProvider.isConfigured()) return
        try {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    item.description?.let { put("description", it) }
                    put("type", item.type)
                    put("target_user", item.target_user)
                    item.due_date?.let { put("due_date", it) }
                    item.due_time?.let { put("due_time", it) }
                    put("status", item.status)
                    put("created_by", item.created_by)
                    put("created_at", item.created_at)
                    item.completed_at?.let { put("completed_at", it) }
                }
                jsonArray.put(obj)
            }

            val payload = JSONObject().apply {
                put("id", hubId)
                put("title", "Tarefas e Lembretes")
                put("items", jsonArray)
                put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
            }

            SupabaseClientProvider.postOrUpdate("shared_tasks_hub", payload.toString())
            prefs.edit().putString("cached_tasks_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao enviar tarefas para o Supabase", e)
        }
    }

    private fun loadFromCache() {
        try {
            val cachedJson = prefs.getString("cached_tasks_json", null) ?: return
            val itemsArray = JSONArray(cachedJson)
            val parsedItems = mutableListOf<SharedTaskItem>()
            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.optJSONObject(i) ?: continue
                val id = itemObj.optString("id")
                if (id.isBlank()) continue
                parsedItems.add(
                    SharedTaskItem(
                        id = id,
                        title = itemObj.optString("title", "Sem título"),
                        description = itemObj.optString("description").takeIf { it.isNotBlank() },
                        type = itemObj.optString("type", "notice"),
                        target_user = itemObj.optString("target_user", "kenned"),
                        due_date = if (itemObj.has("due_date") && !itemObj.isNull("due_date")) itemObj.optLong("due_date") else null,
                        due_time = itemObj.optString("due_time").takeIf { it.isNotBlank() },
                        status = itemObj.optString("status", "pending"),
                        created_by = itemObj.optString("created_by", "Web"),
                        created_at = itemObj.optLong("created_at", System.currentTimeMillis()),
                        completed_at = if (itemObj.has("completed_at") && !itemObj.isNull("completed_at")) itemObj.optLong("completed_at") else null
                    )
                )
            }
            _tasks.value = parsedItems
            _pendingCount.value = parsedItems.count { it.target_user == "kenned" && it.status == "pending" }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao ler cache de tarefas", e)
        }
    }
}
