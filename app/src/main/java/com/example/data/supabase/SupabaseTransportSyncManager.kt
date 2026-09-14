package com.example.data.supabase

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupabaseTransportSyncManager(
    private val context: Context
) {
    private val TAG = "SupabaseTransportSync"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    private val prefs = context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
    private val appPrefs = context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)

    private val hubId: String = prefs.getString("transport_hub_id", null)?.takeIf { it.isNotBlank() }
        ?: "transport_default".also {
            prefs.edit().putString("transport_hub_id", it).apply()
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
                    Log.e(TAG, "Erro no loop de sincronização de transporte", e)
                }
                delay(12000)
            }
        }
    }

    suspend fun pullFromSupabase() {
        if (!SupabaseClientProvider.isConfigured()) return
        withContext(Dispatchers.IO) {
            try {
                val result = SupabaseClientProvider.getDocument("shared_transport_hub", hubId)
                val jsonStr = result.getOrNull()
                if (!jsonStr.isNullOrBlank() && jsonStr != "[]") {
                    val jsonObj = if (jsonStr.startsWith("[")) {
                        JSONArray(jsonStr).optJSONObject(0) ?: JSONObject()
                    } else {
                        JSONObject(jsonStr)
                    }

                    val dataObj = jsonObj.optJSONObject("data") ?: jsonObj
                    val remoteLines = dataObj.optJSONArray("monitored_metro_lines") ?: JSONArray()
                    if (remoteLines.length() > 0 && !isUpdatingFromRemote) {
                        isUpdatingFromRemote = true
                        try {
                            val newKeys = mutableSetOf<String>()
                            for (i in 0 until remoteLines.length()) {
                                val code = remoteLines.optString(i, "")
                                if (code.isNotBlank()) {
                                    newKeys.add("1_$code") // formato do app: 1_1, 2_4, 3_9, etc.
                                    newKeys.add("2_$code")
                                    newKeys.add("3_$code")
                                    newKeys.add(code)
                                }
                            }
                            if (newKeys.isNotEmpty()) {
                                val current = appPrefs.getStringSet("metro_monitored_lines", emptySet()) ?: emptySet()
                                if (!current.containsAll(newKeys)) {
                                    val merged = current.toMutableSet().apply { addAll(newKeys) }
                                    appPrefs.edit().putStringSet("metro_monitored_lines", merged).apply()
                                    Log.d(TAG, "Linhas favoritas do metrô atualizadas pelo Supabase/Telegram: ${newKeys.size}")
                                }
                            }
                        } finally {
                            isUpdatingFromRemote = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao puxar transporte do Supabase", e)
            }
        }
    }

    suspend fun pushToSupabase() {
        if (!SupabaseClientProvider.isConfigured() || isUpdatingFromRemote) return
        withContext(Dispatchers.IO) {
            try {
                val currentMonitored = appPrefs.getStringSet("metro_monitored_lines", emptySet()) ?: emptySet()
                val currentHash = currentMonitored.hashCode()
                if (currentHash == lastUploadedHash) return@withContext

                _syncStatus.value = SyncStatus.SYNCING

                // Extrai códigos simples de linhas (ex: 1_1 -> "1", 2_4 -> "4", 3_9 -> "9")
                val cleanLineCodes = currentMonitored.map {
                    if (it.contains("_")) it.substringAfter("_") else it
                }.distinct()

                val linesArray = JSONArray()
                cleanLineCodes.forEach { linesArray.put(it) }

                val dataPayload = JSONObject().apply {
                    put("monitored_metro_lines", linesArray)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                }

                val fullDoc = JSONObject().apply {
                    put("id", hubId)
                    put("title", "Transporte & Mobilidade")
                    put("data", dataPayload)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                }

                val postResult = SupabaseClientProvider.postOrUpdate("shared_transport_hub", fullDoc.toString())
                if (postResult.isSuccess) {
                    lastUploadedHash = currentHash
                    _syncStatus.value = SyncStatus.SYNCED
                    Log.d(TAG, "Linhas de transporte sincronizadas no Supabase: ${cleanLineCodes.size} linhas")
                } else {
                    _syncStatus.value = SyncStatus.ERROR
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao enviar transporte para o Supabase", e)
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
