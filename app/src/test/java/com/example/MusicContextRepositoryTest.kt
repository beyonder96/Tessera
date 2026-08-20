package com.example

import com.example.data.media.MusicContextRepository
import com.example.data.media.VideoCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MusicContextRepositoryTest {

    @Test
    fun testPodcastDetectionAndChapters() = runBlocking {
        val dossier = MusicContextRepository.getMusicContextDossier(
            title = "Ep. 120 - A Revolução da Inteligência Artificial",
            artist = "Tech Podcast Brasil",
            album = "Podcast Series",
            durationMs = 3600000L, // 1 hora
            packageName = "com.spotify.music"
        )

        assertTrue(dossier.isPodcast)
        assertFalse(dossier.isInstrumental)
        assertNotNull(dossier.podcastChapters)
        assertTrue(dossier.podcastChapters.isNotEmpty())
        assertEquals(4, dossier.podcastChapters.size)
        assertEquals("00:00", dossier.podcastChapters.first().timeFormatted)
    }

    @Test
    fun testInstrumentalDetection() = runBlocking {
        val dossier = MusicContextRepository.getMusicContextDossier(
            title = "Midnight Coffee (Lofi Chillhop Instrumental)",
            artist = "Chill Beats Co.",
            album = "Lofi Sessions",
            durationMs = 150000L,
            packageName = "com.aspiro.tidal"
        )

        assertFalse(dossier.isPodcast)
        assertTrue(dossier.isInstrumental)
        assertNotNull(dossier.technicalCredits)
        assertTrue(dossier.relatedVideos.isNotEmpty())
        assertTrue(dossier.relatedVideos.any { it.category == VideoCategory.OFFICIAL_MUSIC_VIDEO })
    }

    @Test
    fun testRegularTrackDossierStructure() = runBlocking {
        val dossier = MusicContextRepository.getMusicContextDossier(
            title = "The Scientist",
            artist = "Coldplay",
            album = "A Rush of Blood to the Head",
            durationMs = 309000L,
            packageName = "com.spotify.music"
        )

        assertEquals("The Scientist", dossier.trackTitle)
        assertEquals("Coldplay", dossier.artistName)
        assertNotNull(dossier.technicalCredits)
        assertTrue(dossier.relatedVideos.isNotEmpty())
    }
}
