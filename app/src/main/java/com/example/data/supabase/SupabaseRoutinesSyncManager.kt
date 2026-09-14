package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.Habit
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupabaseRoutinesSyncManager(
    private val context: Context,
    private val repository: TesseraRepository
) {
    private val TAG = "SupabaseRoutinesSync"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    private val prefs = context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
    private val hubId: String = prefs.getString("routines_hub_id", null)?.takeIf { it.isNotBlank() }
        ?: "routines_default".also {
            prefs.edit().putString("routines_hub_id", it).apply()
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

    fun startContinuousSync() {
        if (syncJob?.isActive == true) return

        syncJob = scope.launch {
            while (isActive) {
                try {
                    pullFromSupabase()
                    pushToSupabase()
                } catch (e: Exception) {
                    Log.e(TAG, "Erro no loop de sincronização de rotinas", e)
                }
                delay(9000)
            }
        }
    }

    suspend fun pullFromSupabase() {
        if (!SupabaseClientProvider.isConfigured()) return
        withContext(Dispatchers.IO) {
            try {
                val result = SupabaseClientProvider.getDocument("shared_routines_hub", hubId)
                val jsonStr = result.getOrNull()
                if (!jsonStr.isNullOrBlank() && jsonStr != "[]") {
                    val jsonObj = if (jsonStr.startsWith("[")) {
                        JSONArray(jsonStr).optJSONObject(0) ?: JSONObject()
                    } else {
                        JSONObject(jsonStr)
                    }

                    val dataObj = jsonObj.optJSONObject("data") ?: jsonObj
                    val remoteHabits = dataObj.optJSONArray("habits") ?: JSONArray()
                    val localHabits = repository.allHabits.first()

                    if (remoteHabits.length() > 0 && !isUpdatingFromRemote) {
                        isUpdatingFromRemote = true
                        try {
                            for (i in 0 until remoteHabits.length()) {
                                val rh = remoteHabits.optJSONObject(i) ?: continue
                                val name = rh.optString("name", "").trim()
                                val isCompleted = rh.optBoolean("is_completed_today", false)
                                val remoteStreak = rh.optInt("streak", 0)

                                val local = localHabits.find { it.name.equals(name, ignoreCase = true) }
                                if (local != null) {
                                    if (local.isCompleted != isCompleted || (isCompleted && local.streak < remoteStreak)) {
                                        repository.updateHabit(
                                            local.copy(
                                                isCompleted = isCompleted,
                                                streak = if (isCompleted) maxOf(local.streak, remoteStreak) else local.streak
                                            )
                                        )
                                        Log.d(TAG, "Hábito '${local.name}' sincronizado com estado remoto: isCompleted=$isCompleted")
                                    }
                                }
                            }
                        } finally {
                            isUpdatingFromRemote = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao puxar rotinas do Supabase", e)
            }
        }
    }

    suspend fun pushToSupabase() {
        if (!SupabaseClientProvider.isConfigured() || isUpdatingFromRemote) return
        withContext(Dispatchers.IO) {
            try {
                val localHabits = repository.allHabits.first()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val currentHash = (localHabits.joinToString { "${it.id}_${it.isCompleted}_${it.streak}" } + todayStr).hashCode()
                if (currentHash == lastUploadedHash) return@withContext

                _syncStatus.value = SyncStatus.SYNCING

                val habitsArray = JSONArray()
                localHabits.forEach { h ->
                    habitsArray.put(
                        JSONObject().apply {
                            put("id", "h_${h.id}")
                            put("name", h.name)
                            put("icon", h.iconName)
                            put("streak", h.streak)
                            put("is_completed_today", h.isCompleted)
                            put("color_hex", h.colorHex)
                        }
                    )
                }

                val dataPayload = JSONObject().apply {
                    put("habits", habitsArray)
                    put("date", todayStr)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                }

                val fullDoc = JSONObject().apply {
                    put("id", hubId)
                    put("title", "Rotinas & Hábitos")
                    put("data", dataPayload)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                }

                val postResult = SupabaseClientProvider.postOrUpdate("shared_routines_hub", fullDoc.toString())
                if (postResult.isSuccess) {
                    lastUploadedHash = currentHash
                    _syncStatus.value = SyncStatus.SYNCED
                    Log.d(TAG, "Rotinas e hábitos sincronizados no Supabase: ${localHabits.size} hábitos")
                } else {
                    _syncStatus.value = SyncStatus.ERROR
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao enviar hábitos para o Supabase", e)
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }

    fun triggerSync() {
        scope.launch {
            pullFromSupabase()
            pushToSupabase()
        }
    }
}
