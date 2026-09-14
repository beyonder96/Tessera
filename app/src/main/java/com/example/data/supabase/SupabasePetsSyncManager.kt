package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.PetEntity
import com.example.data.PetEvent
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

class SupabasePetsSyncManager(
    private val context: Context,
    private val repository: TesseraRepository
) {
    private val TAG = "SupabasePetsSync"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    private val prefs = context.getSharedPreferences("tessera_supabase_prefs", Context.MODE_PRIVATE)
    private val hubId: String = prefs.getString("pets_hub_id", null)?.takeIf { it.isNotBlank() }
        ?: "pets_default".also {
            prefs.edit().putString("pets_hub_id", it).apply()
        }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus

    private val _dailyCareStatus = MutableStateFlow(JSONObject())
    val dailyCareStatus: StateFlow<JSONObject> = _dailyCareStatus

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
                    Log.e(TAG, "Erro no loop de sincronização de pets", e)
                }
                delay(10000)
            }
        }
    }

    suspend fun pullFromSupabase() {
        if (!SupabaseClientProvider.isConfigured()) return
        withContext(Dispatchers.IO) {
            try {
                val result = SupabaseClientProvider.getDocument("shared_pets_hub", hubId)
                val jsonStr = result.getOrNull()
                if (!jsonStr.isNullOrBlank() && jsonStr != "[]") {
                    val jsonObj = if (jsonStr.startsWith("[")) {
                        JSONArray(jsonStr).optJSONObject(0) ?: JSONObject()
                    } else {
                        JSONObject(jsonStr)
                    }

                    val dataObj = jsonObj.optJSONObject("data") ?: jsonObj
                    val careObj = dataObj.optJSONObject("daily_care")
                    if (careObj != null) {
                        _dailyCareStatus.value = careObj
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao puxar dados de pets do Supabase", e)
            }
        }
    }

    suspend fun pushToSupabase() {
        if (!SupabaseClientProvider.isConfigured() || isUpdatingFromRemote) return
        withContext(Dispatchers.IO) {
            try {
                val localPets = repository.allPets.first()
                val localEvents = repository.allPetEvents.first()
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val currentHash = (localPets.joinToString { "${it.id}_${it.name}_${it.breed}" } + localEvents.size.toString()).hashCode()
                if (currentHash == lastUploadedHash) return@withContext

                _syncStatus.value = SyncStatus.SYNCING

                val petsArray = JSONArray()
                localPets.forEach { p ->
                    petsArray.put(
                        JSONObject().apply {
                            put("id", "pet_${p.id}")
                            put("name", p.name)
                            put("breed", p.breed)
                            put("species", "Cachorro")
                            put("rga", p.rga)
                            put("microchip", p.microchip)
                            put("sex", p.sex.name)
                            put("is_castrated", p.isCastrated)
                        }
                    )
                }

                val eventsArray = JSONArray()
                localEvents.forEach { ev ->
                    eventsArray.put(
                        JSONObject().apply {
                            put("id", "ev_${ev.id}")
                            put("pet_name", ev.petName)
                            put("title", ev.title)
                            put("time", ev.time)
                            put("is_completed", ev.isCompleted)
                            put("is_next", ev.isNext)
                        }
                    )
                }

                val dataPayload = JSONObject().apply {
                    put("pets", petsArray)
                    put("events", eventsArray)
                    put("daily_care", _dailyCareStatus.value)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                }

                val fullDoc = JSONObject().apply {
                    put("id", hubId)
                    put("title", "Central Petz")
                    put("data", dataPayload)
                    put("updated_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                }

                val postResult = SupabaseClientProvider.postOrUpdate("shared_pets_hub", fullDoc.toString())
                if (postResult.isSuccess) {
                    lastUploadedHash = currentHash
                    _syncStatus.value = SyncStatus.SYNCED
                    Log.d(TAG, "Pets sincronizados no Supabase: ${localPets.size} pets, ${localEvents.size} eventos")
                } else {
                    _syncStatus.value = SyncStatus.ERROR
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao enviar pets para o Supabase", e)
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
