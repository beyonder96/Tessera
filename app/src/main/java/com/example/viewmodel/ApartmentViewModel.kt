package com.example.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.supabase.SupabaseClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ApartmentUiState(
    val progress: Float = 0.75f,
    val expectedDate: String = "Dez 2026",
    val tempDate: String = "Dez 2026",
    val isPlaying: Boolean = false,
    val isSyncing: Boolean = false,
    val showDateDialog: Boolean = false,
    val userToastMessage: String? = null
)

class ApartmentViewModel(
    private val sharedPrefs: SharedPreferences,
    autoRefresh: Boolean = true
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ApartmentUiState(
            progress = sharedPrefs.getFloat("apartment_progress", 0.75f),
            expectedDate = sharedPrefs.getString("apartment_date", "Dez 2026") ?: "Dez 2026",
            tempDate = sharedPrefs.getString("apartment_date", "Dez 2026") ?: "Dez 2026"
        )
    )
    val uiState: StateFlow<ApartmentUiState> = _uiState.asStateFlow()

    private var simulationJob: Job? = null

    init {
        if (autoRefresh) {
            refreshFromCloud(showFeedback = false)
        }
    }

    fun onPlayPauseClicked() {
        if (_uiState.value.isPlaying) {
            pauseSimulation()
        } else {
            startSimulation()
        }
    }

    fun startSimulation() {
        _uiState.update { it.copy(isPlaying = true) }
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while (_uiState.value.isPlaying && _uiState.value.progress < 1f) {
                delay(100)
                val newProgress = (_uiState.value.progress + 0.005f).coerceAtMost(1f)
                _uiState.update { it.copy(progress = newProgress) }
                sharedPrefs.edit().putFloat("apartment_progress", newProgress).apply()
                if (newProgress >= 1f) {
                    _uiState.update { it.copy(isPlaying = false) }
                    break
                }
            }
            if (!_uiState.value.isPlaying) {
                syncWithCloud(_uiState.value.progress, _uiState.value.expectedDate)
            }
        }
    }

    fun pauseSimulation() {
        simulationJob?.cancel()
        _uiState.update { it.copy(isPlaying = false) }
        syncWithCloud(_uiState.value.progress, _uiState.value.expectedDate)
    }

    fun onSliderProgressChanged(newProgress: Float) {
        _uiState.update { it.copy(progress = newProgress) }
        sharedPrefs.edit().putFloat("apartment_progress", newProgress).apply()
    }

    fun onSliderProgressFinished() {
        syncWithCloud(_uiState.value.progress, _uiState.value.expectedDate)
    }

    fun onOpenDateDialog() {
        _uiState.update { it.copy(showDateDialog = true, tempDate = it.expectedDate) }
    }

    fun onDismissDateDialog() {
        _uiState.update { it.copy(showDateDialog = false) }
    }

    fun onTempDateChanged(newDate: String) {
        _uiState.update { it.copy(tempDate = newDate) }
    }

    fun onSaveExpectedDate() {
        val newDate = _uiState.value.tempDate
        _uiState.update { it.copy(expectedDate = newDate, showDateDialog = false) }
        sharedPrefs.edit().putString("apartment_date", newDate).apply()
        syncWithCloud(_uiState.value.progress, newDate)
    }

    fun syncWithCloud(newProgress: Float, newDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("action", "sync_apartment")
                    put("progress", (newProgress * 100).toInt())
                    put("expected_date", newDate)
                }.toString()
                SupabaseClientProvider.invokeFunction("telegram-bot", payload)
            } catch (e: Exception) {
                Log.w("ApartmentViewModel", "Erro ao sincronizar com nuvem: ${e.message}")
            }
        }
    }

    fun refreshFromCloud(showFeedback: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val payload = JSONObject().apply { put("action", "get_apartment") }.toString()
                    SupabaseClientProvider.invokeFunction("telegram-bot", payload)
                }
                if (result.isSuccess) {
                    val jsonStr = result.getOrNull() ?: ""
                    val obj = JSONObject(jsonStr)
                    if (obj.optBoolean("ok")) {
                        val data = obj.optJSONObject("data")
                        if (data != null) {
                            val currentProg = _uiState.value.progress
                            val remoteProg = data.optInt("progress", (currentProg * 100).toInt()) / 100f
                            val remoteDate = data.optString("expected_date", _uiState.value.expectedDate)
                            _uiState.update {
                                it.copy(
                                    progress = remoteProg,
                                    expectedDate = remoteDate,
                                    tempDate = remoteDate,
                                    userToastMessage = if (showFeedback) "Sincronizado com o Bot: ${(remoteProg * 100).toInt()}%!" else null
                                )
                            }
                            sharedPrefs.edit()
                                .putFloat("apartment_progress", remoteProg)
                                .putString("apartment_date", remoteDate)
                                .apply()
                        }
                    }
                } else if (showFeedback) {
                    _uiState.update { it.copy(userToastMessage = "Não foi possível conectar com o Bot") }
                }
            } catch (e: Exception) {
                if (showFeedback) {
                    _uiState.update { it.copy(userToastMessage = "Erro na sincronização: ${e.message}") }
                }
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    fun clearUserToastMessage() {
        _uiState.update { it.copy(userToastMessage = null) }
    }
}

class ApartmentViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val sharedPrefs = context.applicationContext.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)
        return ApartmentViewModel(sharedPrefs) as T
    }
}
