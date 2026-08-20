package com.example.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.provider.Settings
import android.util.Log
import com.example.data.media.ActiveMediaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MediaHubManager {
    private const val TAG = "MediaHubManager"

    private val _activeMediaState = MutableStateFlow<ActiveMediaState?>(null)
    val activeMediaState: StateFlow<ActiveMediaState?> = _activeMediaState.asStateFlow()

    private var activeController: MediaController? = null

    fun updateMediaState(state: ActiveMediaState?) {
        _activeMediaState.value = state
    }

    fun setActiveController(controller: MediaController?) {
        activeController = controller
    }

    fun play() {
        try {
            activeController?.transportControls?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar comando play", e)
        }
    }

    fun pause() {
        try {
            activeController?.transportControls?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar comando pause", e)
        }
    }

    fun togglePlayPause() {
        val isPlaying = _activeMediaState.value?.isPlaying == true
        if (isPlaying) pause() else play()
    }

    fun skipToNext() {
        try {
            activeController?.transportControls?.skipToNext()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar comando skipToNext", e)
        }
    }

    fun skipToPrevious() {
        try {
            activeController?.transportControls?.skipToPrevious()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar comando skipToPrevious", e)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            activeController?.transportControls?.seekTo(positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar comando seekTo", e)
        }
    }

    /**
     * Verifica se o usuário concedeu a permissão de Notification Listener necessária para acessar MediaSession.
     */
    fun isNotificationListenerGranted(context: Context): Boolean {
        val cn = ComponentName(context, TesseraMediaNotificationListener::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }

    /**
     * Abre a tela exata de configurações do Android para habilitar o serviço.
     */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir configurações de notificação", e)
        }
    }
}
