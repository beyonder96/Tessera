package com.example.viewmodel

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Random

enum class FocusMode(val title: String, val icon: ImageVector) {
    FOCUS_TIMER("Focus Timer", Icons.Outlined.CenterFocusStrong),
    QUICK_NAP("Quick Nap", Icons.Outlined.DoNotDisturbOn),
    BREATHING("Breathing", Icons.Outlined.Eco)
}

// Binaural beats sound player (4Hz Theta wave for focus)
open class FocusSoundPlayer {
    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isPlaying = false

    open fun start() {
        if (isPlaying) return
        isPlaying = true
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        try {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack?.play()
            
            Thread {
                val buffer = ShortArray(bufferSize)
                val random = Random()
                var lastLeft = 0f
                var lastRight = 0f
                
                var phaseLeft = 0f
                var phaseRight = 0f
                val sampleRateF = 44100f
                
                // Theta wave binaural difference: 100Hz in left ear, 104Hz in right ear -> 4Hz difference
                val freqLeft = 100f
                val freqRight = 104f
                val phaseIncLeft = 2f * Math.PI.toFloat() * freqLeft / sampleRateF
                val phaseIncRight = 2f * Math.PI.toFloat() * freqRight / sampleRateF
                
                var time = 0L
                try {
                    while (isPlaying) {
                        for (i in 0 until buffer.size step 2) {
                            // Soft brown noise simulating serene ocean waves
                            val whiteL = random.nextGaussian().toFloat() * 550f
                            val whiteR = random.nextGaussian().toFloat() * 550f
                            
                            lastLeft = (lastLeft * 0.98f) + (whiteL * 0.05f)
                            lastRight = (lastRight * 0.98f) + (whiteR * 0.05f)
                            
                            // Binaural sine hum with dynamic volume swells (6-second cycle)
                            val swell = 0.5f + 0.3f * Math.sin(2.0 * Math.PI * time / (sampleRateF * 6f)).toFloat()
                            val sineL = Math.sin(phaseLeft.toDouble()).toFloat() * 1100f * swell
                            val sineR = Math.sin(phaseRight.toDouble()).toFloat() * 1100f * swell
                            
                            phaseLeft += phaseIncLeft
                            if (phaseLeft > 2f * Math.PI.toFloat()) phaseLeft -= 2f * Math.PI.toFloat()
                            
                            phaseRight += phaseIncRight
                            if (phaseRight > 2f * Math.PI.toFloat()) phaseRight -= 2f * Math.PI.toFloat()
                            
                            val mixedL = lastLeft + sineL
                            val mixedR = lastRight + sineR
                            
                            if (i < buffer.size) {
                                buffer[i] = mixedL.coerceIn(-32768f, 32767f).toInt().toShort()
                            }
                            if (i + 1 < buffer.size) {
                                buffer[i + 1] = mixedR.coerceIn(-32768f, 32767f).toInt().toShort()
                            }
                            time++
                        }
                        if (isPlaying) {
                            audioTrack?.write(buffer, 0, buffer.size)
                        }
                    }
                } catch (e: Exception) {
                    // Silently terminate audio loop on track release
                }
            }.start()
        } catch (e: Exception) {}
    }

    open fun stop() {
        isPlaying = false
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null
    }
}

data class PomodoroUiState(
    val selectedMode: FocusMode = FocusMode.FOCUS_TIMER,
    val focusDuration: Int = 25,
    val napDuration: Int = 20,
    val breathingDuration: Int = 3,
    val secondsLeft: Int = 25 * 60,
    val isRunning: Boolean = false,
    val selectedSoundscape: String = "Ocean",
    val focusModeType: String = "Goal Timer",
    val showSoundscapeDialog: Boolean = false
) {
    val currentDuration: Int
        get() = when (selectedMode) {
            FocusMode.FOCUS_TIMER -> focusDuration
            FocusMode.QUICK_NAP -> napDuration
            FocusMode.BREATHING -> breathingDuration
        }

    val durationRange: ClosedFloatingPointRange<Float>
        get() = when (selectedMode) {
            FocusMode.FOCUS_TIMER -> 1f..120f
            FocusMode.QUICK_NAP -> 5f..60f
            FocusMode.BREATHING -> 1f..15f
        }
}

class PomodoroViewModel(
    private val soundPlayer: FocusSoundPlayer = FocusSoundPlayer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun onSelectMode(mode: FocusMode) {
        onStopTimer()
        _uiState.update { state ->
            val newDuration = when (mode) {
                FocusMode.FOCUS_TIMER -> state.focusDuration
                FocusMode.QUICK_NAP -> state.napDuration
                FocusMode.BREATHING -> state.breathingDuration
            }
            state.copy(
                selectedMode = mode,
                isRunning = false,
                secondsLeft = newDuration * 60
            )
        }
    }

    fun onUpdateDuration(value: Int) {
        _uiState.update { state ->
            val updated = when (state.selectedMode) {
                FocusMode.FOCUS_TIMER -> state.copy(focusDuration = value)
                FocusMode.QUICK_NAP -> state.copy(napDuration = value)
                FocusMode.BREATHING -> state.copy(breathingDuration = value)
            }
            if (!state.isRunning) {
                updated.copy(secondsLeft = value * 60)
            } else {
                updated
            }
        }
    }

    fun onStartTimer() {
        val currentDuration = _uiState.value.currentDuration
        _uiState.update {
            it.copy(
                secondsLeft = currentDuration * 60,
                isRunning = true
            )
        }

        if (_uiState.value.selectedSoundscape == "Ocean") {
            soundPlayer.start()
        }

        startCountdown()
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.secondsLeft > 0 && _uiState.value.isRunning) {
                delay(1000L)
                _uiState.update { it.copy(secondsLeft = (it.secondsLeft - 1).coerceAtLeast(0)) }
            }
            if (_uiState.value.secondsLeft <= 0) {
                onStopTimer()
            }
        }
    }

    fun onTick() {
        _uiState.update { state ->
            val remaining = (state.secondsLeft - 1).coerceAtLeast(0)
            state.copy(secondsLeft = remaining)
        }
        if (_uiState.value.secondsLeft <= 0) {
            onStopTimer()
        }
    }

    fun onStopTimer() {
        timerJob?.cancel()
        soundPlayer.stop()
        _uiState.update {
            it.copy(
                isRunning = false,
                secondsLeft = it.currentDuration * 60
            )
        }
    }

    fun onSelectSoundscape(sound: String) {
        _uiState.update {
            it.copy(
                selectedSoundscape = sound,
                showSoundscapeDialog = false
            )
        }
        if (_uiState.value.isRunning) {
            if (sound == "Ocean") {
                soundPlayer.start()
            } else {
                soundPlayer.stop()
            }
        }
    }

    fun onToggleFocusModeType() {
        _uiState.update {
            it.copy(
                focusModeType = if (it.focusModeType == "Goal Timer") "Stopwatch" else "Goal Timer"
            )
        }
    }

    fun onShowSoundscapeDialog(show: Boolean) {
        _uiState.update { it.copy(showSoundscapeDialog = show) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundPlayer.stop()
    }
}
