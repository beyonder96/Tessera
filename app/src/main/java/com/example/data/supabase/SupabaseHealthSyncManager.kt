package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.HealthProfile
import com.example.data.TesseraRepository
import com.example.data.WaterRecord
import com.example.data.WeightRecord
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

class SupabaseHealthSyncManager(
    private val context: Context,
    private val repository: TesseraRepository
) {
    private val TAG = "SupabaseHealthSync"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    private val prefs = context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
    private val hubId: String = prefs.getString("health_hub_id", null)?.takeIf { it.isNotBlank() }
        ?: "health_default".also {
            prefs.edit().putString("health_hub_id", it).apply()
        }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _remoteWaterToday = MutableStateFlow(0)
    val remoteWaterToday: StateFlow<Int> = _remoteWaterToday

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
                    Log.e(TAG, "Erro no loop de sincronização de saúde", e)
                }
                delay(8000)
            }
        }
    }

    suspend fun pullFromSupabase() {
        if (!SupabaseClientProvider.isConfigured()) return
        withContext(Dispatchers.IO) {
            try {
                val result = SupabaseClientProvider.getDocument("shared_health_hub", hubId)
                val jsonStr = result.getOrNull()
                if (!jsonStr.isNullOrBlank() && jsonStr != "[]") {
                    val jsonObj = if (jsonStr.startsWith("[")) {
                        JSONArray(jsonStr).optJSONObject(0) ?: JSONObject()
                    } else {
                        JSONObject(jsonStr)
                    }

                    val dataObj = jsonObj.optJSONObject("data") ?: jsonObj
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val docDate = dataObj.optString("date", "")

                    if (docDate == todayStr || docDate.isBlank()) {
                        val remoteWater = dataObj.optInt("today_water_ml", 0)
                        _remoteWaterToday.value = remoteWater

                        val localWaterRecords = repository.allWaterRecords.first()
                            .filter { it.date == todayStr }
                        val localWaterSum = localWaterRecords.sumOf { it.amountMl }

                        // Se o Telegram ou Nuvem registrou água a mais do que o app tem
                        if (remoteWater > localWaterSum && !isUpdatingFromRemote) {
                            isUpdatingFromRemote = true
                            try {
                                val delta = remoteWater - localWaterSum
                                if (delta > 0) {
                                    repository.insertWaterRecord(
                                        WaterRecord(
                                            amountMl = delta,
                                            timestamp = System.currentTimeMillis(),
                                            date = todayStr
                                        )
                                    )
                                    Log.d(TAG, "Sincronizado +${delta}ml de água vindos da nuvem/Telegram!")
                                }
                            } finally {
                                isUpdatingFromRemote = false
                            }
                        }

                        // Sincroniza peso se registrado remotamente
                        val remoteWeight = dataObj.optDouble("latest_weight", 0.0)
                        if (remoteWeight > 0.0) {
                            val localWeights = repository.allWeightRecords.first()
                            val latestLocal = localWeights.maxByOrNull { it.timestamp }
                            if (latestLocal == null || Math.abs(latestLocal.weightKg - remoteWeight) > 0.05) {
                                repository.insertWeightRecord(
                                    WeightRecord(
                                        weightKg = remoteWeight,
                                        timestamp = System.currentTimeMillis(),
                                        source = "Telegram / Nuvem"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao puxar dados de saúde do Supabase", e)
            }
        }
    }

    suspend fun pushToSupabase() {
        if (!SupabaseClientProvider.isConfigured() || isUpdatingFromRemote) return
        withContext(Dispatchers.IO) {
            try {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val localWaterRecords = repository.allWaterRecords.first()
                    .filter { it.date == todayStr }
                val localWaterSum = localWaterRecords.sumOf { it.amountMl }

                val profile = repository.healthProfile.first() ?: HealthProfile()
                val localWeights = repository.allWeightRecords.first()
                val latestWeight = localWeights.maxByOrNull { it.timestamp }?.weightKg ?: profile.targetWeightKg

                val currentHash = (localWaterSum.toString() + latestWeight.toString() + todayStr).hashCode()
                if (currentHash == lastUploadedHash) return@withContext

                _syncStatus.value = SyncStatus.SYNCING

                val dataPayload = JSONObject().apply {
                    put("today_water_ml", localWaterSum)
                    put("water_goal_ml", profile.dailyWaterGoalMl)
                    put("latest_weight", latestWeight)
                    put("date", todayStr)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                }

                val fullDoc = JSONObject().apply {
                    put("id", hubId)
                    put("title", "Saúde & Bem-Estar")
                    put("data", dataPayload)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                }

                val postResult = SupabaseClientProvider.postOrUpdate("shared_health_hub", fullDoc.toString())
                if (postResult.isSuccess) {
                    lastUploadedHash = currentHash
                    _syncStatus.value = SyncStatus.SYNCED
                    Log.d(TAG, "Saúde sincronizada com sucesso no Supabase: ${localWaterSum}ml")
                } else {
                    _syncStatus.value = SyncStatus.ERROR
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao enviar dados de saúde para o Supabase", e)
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
