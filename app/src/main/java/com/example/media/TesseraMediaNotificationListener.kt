package com.example.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.media.ActiveMediaState

class TesseraMediaNotificationListener : NotificationListenerService() {

    private val TAG = "TesseraMediaListener"
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentController: MediaController? = null

    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveSession(controllers)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            currentController?.let { syncFromController(it) }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            currentController?.let { syncFromController(it) }
        }

        override fun onSessionDestroyed() {
            currentController = null
            MediaHubManager.setActiveController(null)
            MediaHubManager.updateMediaState(null)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = ComponentName(this, TesseraMediaNotificationListener::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(activeSessionsListener, componentName)

            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            updateActiveSession(controllers)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao conectar listener de sessões de mídia", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
            currentController?.unregisterCallback(controllerCallback)
            currentController = null
            MediaHubManager.setActiveController(null)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desconectar listener", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Se a notificação for de um player, checa novamente a sessão ativa
        if (sbn?.notification?.isMediaNotification() == true) {
            refreshControllers()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn?.notification?.isMediaNotification() == true) {
            refreshControllers()
        }
    }

    private fun refreshControllers() {
        try {
            val componentName = ComponentName(this, TesseraMediaNotificationListener::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            updateActiveSession(controllers)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar controllers", e)
        }
    }

    private fun updateActiveSession(controllers: List<MediaController>?) {
        if (controllers.isNullOrEmpty()) {
            currentController?.unregisterCallback(controllerCallback)
            currentController = null
            MediaHubManager.setActiveController(null)
            MediaHubManager.updateMediaState(null)
            return
        }

        // Prioriza o controller que está tocando no momento (STATE_PLAYING)
        val active = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull { it.playbackState != null }
            ?: controllers.first()

        if (currentController?.sessionToken != active.sessionToken) {
            currentController?.unregisterCallback(controllerCallback)
            currentController = active
            currentController?.registerCallback(controllerCallback)
            MediaHubManager.setActiveController(active)
        }

        syncFromController(active)
    }

    private fun syncFromController(controller: MediaController) {
        val metadata = controller.metadata
        val playbackState = controller.playbackState

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: controller.queueTitle?.toString()
            ?: "Faixa Desconhecida"

        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: "Artista Desconhecido"

        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: ""

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playbackState?.position ?: 0L
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

        val bitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)

        val uriStr = metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
        val artUri = if (!uriStr.isNullOrBlank()) Uri.parse(uriStr) else null

        val pkgName = controller.packageName ?: ""
        val appDisplayName = resolveAppDisplayName(pkgName)

        val state = ActiveMediaState(
            title = title,
            artist = artist,
            album = album,
            packageName = pkgName,
            appDisplayName = appDisplayName,
            durationMs = duration,
            currentPositionMs = position,
            isPlaying = isPlaying,
            artworkBitmap = bitmap,
            artworkUri = artUri,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )

        MediaHubManager.updateMediaState(state)
    }

    private fun resolveAppDisplayName(packageName: String): String {
        return when {
            packageName.contains("spotify", ignoreCase = true) -> "Spotify"
            packageName.contains("tidal", ignoreCase = true) -> "Tidal"
            packageName.contains("deezer", ignoreCase = true) -> "Deezer"
            packageName.contains("youtube", ignoreCase = true) -> "YouTube Music"
            packageName.contains("apple", ignoreCase = true) -> "Apple Music"
            packageName.contains("amazon", ignoreCase = true) -> "Amazon Music"
            packageName.contains("soundcloud", ignoreCase = true) -> "SoundCloud"
            packageName.contains("podcast", ignoreCase = true) -> "Podcasts"
            else -> {
                try {
                    val pm = packageManager
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    "Player Local"
                }
            }
        }
    }

    private fun android.app.Notification.isMediaNotification(): Boolean {
        val template = extras.getString(android.app.Notification.EXTRA_TEMPLATE)
        return template != null && template.contains("MediaStyle", ignoreCase = true)
    }
}
