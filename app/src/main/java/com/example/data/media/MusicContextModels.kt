package com.example.data.media

import android.graphics.Bitmap
import android.net.Uri

/**
 * Estado ao vivo de reprodução de mídia capturado do sistema Android.
 */
data class ActiveMediaState(
    val title: String,
    val artist: String,
    val album: String = "",
    val packageName: String = "",
    val appDisplayName: String = "",
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val artworkBitmap: Bitmap? = null,
    val artworkUri: Uri? = null,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val lastPositionUpdateTime: Long = android.os.SystemClock.elapsedRealtime(),
    val playbackSpeed: Float = 1.0f
) {
    fun getCalculatedPositionMs(): Long {
        if (!isPlaying) return currentPositionMs
        val elapsed = android.os.SystemClock.elapsedRealtime() - lastPositionUpdateTime
        if (elapsed <= 0) return currentPositionMs
        val calculated = currentPositionMs + (elapsed * playbackSpeed).toLong()
        return if (durationMs > 0) calculated.coerceIn(0L, durationMs) else calculated.coerceAtLeast(0L)
    }
}

/**
 * Verso de uma letra com anotação contextual opcional (ex: fatos de bastidores do Genius).
 */
data class LyricLine(
    val text: String,
    val timestampMs: Long? = null,
    val hasAnnotation: Boolean = false,
    val annotationText: String? = null
)

/**
 * Informações de Letras e Anotações Editoriais.
 */
data class TrackLyricsInfo(
    val plainLyrics: String,
    val lines: List<LyricLine> = emptyList(),
    val sourceUrl: String? = null,
    val isInstrumental: Boolean = false,
    val isSynced: Boolean = false
)

/**
 * Ficha técnica e créditos de gravação (MusicBrainz).
 */
data class TrackTechnicalCredits(
    val composers: List<String> = emptyList(),
    val producers: List<String> = emptyList(),
    val recordLabel: String? = null,
    val studio: String? = null,
    val releaseDate: String? = null,
    val isrc: String? = null,
    val bpm: Int? = null,
    val key: String? = null,
    val recordingLocation: String? = null
)

/**
 * Vídeo complementar / Atalho de Busca (YouTube).
 */
data class TrackVideoMedia(
    val id: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val category: VideoCategory,
    val channelTitle: String,
    val youtubeQuery: String = ""
)

enum class VideoCategory(val displayName: String) {
    OFFICIAL_MUSIC_VIDEO("Clipe Oficial"),
    LIVE_PERFORMANCE("Ao Vivo"),
    BEHIND_THE_SCENES("Bastidores / Documentário"),
    COVER_OR_REMIX("Versão Alternativa")
}

/**
 * Tópico/Capítulo de podcast longo.
 */
data class PodcastChapter(
    val timestampMs: Long,
    val timeFormatted: String,
    val title: String,
    val description: String? = null
)

/**
 * Dossiê consolidado de contexto musical.
 */
data class MusicContextDossier(
    val trackTitle: String,
    val artistName: String,
    val albumName: String?,
    val lyricsInfo: TrackLyricsInfo?,
    val technicalCredits: TrackTechnicalCredits?,
    val relatedVideos: List<TrackVideoMedia> = emptyList(),
    val podcastChapters: List<PodcastChapter> = emptyList(),
    val isPodcast: Boolean = false,
    val isInstrumental: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis(),
    val isOfflineCache: Boolean = false
)
