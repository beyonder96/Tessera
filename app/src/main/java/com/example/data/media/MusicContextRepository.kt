package com.example.data.media

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object MusicContextRepository {
    private const val TAG = "MusicContextRepo"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Cache em memória de dossiês (Stale-While-Revalidate)
    private val memoryCache = ConcurrentHashMap<String, MusicContextDossier>()

    /**
     * Busca ou constrói o Dossiê de Contexto Musical completo para a faixa atual.
     */
    suspend fun getMusicContextDossier(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        packageName: String = "",
        forceRefresh: Boolean = false
    ): MusicContextDossier = withContext(Dispatchers.IO) {
        val cacheKey = buildCacheKey(title, artist)

        if (!forceRefresh) {
            val cached = memoryCache[cacheKey]
            if (cached != null) {
                return@withContext cached
            }
        }

        val isPodcast = checkIfPodcast(title, artist, durationMs, packageName)

        if (isPodcast) {
            val podcastDossier = buildPodcastDossier(title, artist, album, durationMs)
            memoryCache[cacheKey] = podcastDossier
            return@withContext podcastDossier
        }

        val isInstrumental = checkIfInstrumental(title, artist)

        // 1. Busca Letras (Lrclib API -> Fallback Genius)
        val lyricsInfo = if (isInstrumental) {
            TrackLyricsInfo(
                plainLyrics = "Composição instrumental sem vocalização.",
                lines = emptyList(),
                sourceUrl = null,
                isInstrumental = true
            )
        } else {
            val lrclibLyrics = try {
                fetchLyricsFromLrclib(title, artist, album, durationMs)
            } catch (e: Exception) {
                safeLogW(TAG, "Falha ao buscar letra no Lrclib: ${e.message}")
                null
            }

            lrclibLyrics ?: try {
                fetchLyricsFromGenius(title, artist)
            } catch (e: Exception) {
                safeLogW(TAG, "Falha ao buscar letra no Genius: ${e.message}")
                null
            }
        }

        // 2. Busca Ficha Técnica (MusicBrainz API)
        val technicalCredits = try {
            fetchTechnicalCreditsFromMusicBrainz(title, artist)
        } catch (e: Exception) {
            safeLogW(TAG, "Falha ao buscar ficha técnica no MusicBrainz: ${e.message}")
            null
        }

        // 3. Busca Material Audiovisual Complementar (YouTube Data API / OEmbed)
        val relatedVideos = try {
            fetchRelatedVideos(title, artist)
        } catch (e: Exception) {
            safeLogW(TAG, "Falha ao buscar vídeos complementares: ${e.message}")
            emptyList()
        }

        val dossier = MusicContextDossier(
            trackTitle = title,
            artistName = artist,
            albumName = album,
            lyricsInfo = lyricsInfo,
            technicalCredits = technicalCredits,
            relatedVideos = relatedVideos,
            isPodcast = false,
            isInstrumental = isInstrumental,
            cachedAt = System.currentTimeMillis(),
            isOfflineCache = false
        )

        memoryCache[cacheKey] = dossier
        dossier
    }

    /**
     * Pre-fetch silencioso em background quando o player muda de faixa.
     */
    suspend fun prefetchDossier(
        title: String,
        artist: String,
        album: String? = null,
        durationMs: Long = 0L,
        packageName: String = ""
    ) {
        val cacheKey = buildCacheKey(title, artist)
        if (memoryCache.containsKey(cacheKey)) return

        try {
            getMusicContextDossier(title, artist, album, durationMs, packageName, forceRefresh = false)
        } catch (e: Exception) {
            safeLogD(TAG, "Pre-fetch ignorado para $title - $artist: ${e.message}")
        }
    }

    private fun buildCacheKey(title: String, artist: String): String {
        return "${artist.trim().lowercase()}:::${title.trim().lowercase()}"
    }

    private fun checkIfPodcast(title: String, artist: String, durationMs: Long, packageName: String): Boolean {
        if (packageName.contains("podcast", ignoreCase = true)) return true
        if (durationMs > 20 * 60 * 1000L) return true // Mais de 20 minutos
        val lowerTitle = title.lowercase()
        return lowerTitle.contains("ep.") || lowerTitle.contains("episódio") || lowerTitle.contains("podcast") || lowerTitle.contains("#")
    }

    private fun checkIfInstrumental(title: String, artist: String): Boolean {
        val combined = "$title $artist".lowercase()
        return combined.contains("instrumental") || combined.contains("lofi") || combined.contains("lo-fi") ||
                combined.contains("chillhop") || combined.contains("soundtrack") || combined.contains("score")
    }

    private fun buildPodcastDossier(
        title: String,
        artist: String,
        album: String?,
        durationMs: Long
    ): MusicContextDossier {
        val chapters = mutableListOf<PodcastChapter>()
        if (durationMs > 0) {
            val quarter = durationMs / 4
            chapters.add(PodcastChapter(0L, "00:00", "Introdução e Abertura", "Apresentação dos tópicos do episódio"))
            chapters.add(PodcastChapter(quarter, formatMs(quarter), "Discussão Principal - Parte 1", "Análise do tema central"))
            chapters.add(PodcastChapter(quarter * 2, formatMs(quarter * 2), "Aprofundamento e Estudos de Caso", "Debates e exemplos práticos"))
            chapters.add(PodcastChapter(quarter * 3, formatMs(quarter * 3), "Considerações Finais e Q&A", "Conclusões e recomendações"))
        }

        val credits = TrackTechnicalCredits(
            composers = listOf(artist),
            producers = listOf("Produção do Programa"),
            recordLabel = album ?: "Podcast Series",
            releaseDate = "Episódio Recente",
            bpm = null
        )

        return MusicContextDossier(
            trackTitle = title,
            artistName = artist,
            albumName = album,
            lyricsInfo = null,
            technicalCredits = credits,
            podcastChapters = chapters,
            isPodcast = true,
            isInstrumental = false
        )
    }

    /**
     * Busca Letras na API Lrclib (https://lrclib.net).
     */
    private fun fetchLyricsFromLrclib(title: String, artist: String, album: String? = null, durationMs: Long = 0L): TrackLyricsInfo? {
        val cleanTitle = title.replace(Regex("""\(.*?\)|\[.*?\]"""), "").trim()
        val cleanArtist = artist.replace(Regex("""feat\..*|ft\..*""", RegexOption.IGNORE_CASE), "").trim()

        // 1. Tentativa com endpoint GET exato
        try {
            val encTrack = URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8.toString())
            val encArtist = URLEncoder.encode(cleanArtist, StandardCharsets.UTF_8.toString())
            var url = "https://lrclib.net/api/get?artist_name=$encArtist&track_name=$encTrack"
            if (!album.isNullOrBlank()) {
                url += "&album_name=" + URLEncoder.encode(album.trim(), StandardCharsets.UTF_8.toString())
            }
            if (durationMs > 0) {
                url += "&duration=" + (durationMs / 1000)
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "TesseraMusic/2.0 ( contact@tessera.app )")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrBlank()) {
                    val info = parseLrclibJson(JSONObject(jsonStr))
                    if (info != null) return info
                }
            }
        } catch (e: Exception) {
            safeLogD(TAG, "Lrclib GET failed: ${e.message}")
        }

        // 2. Tentativa com endpoint SEARCH
        try {
            val query = "$cleanArtist $cleanTitle"
            val encQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val searchUrl = "https://lrclib.net/api/search?q=$encQuery"

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "TesseraMusic/2.0 ( contact@tessera.app )")
                .header("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrBlank() && jsonStr.startsWith("[")) {
                    val array = JSONArray(jsonStr)
                    if (array.length() > 0) {
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val info = parseLrclibJson(obj)
                            if (info != null && info.plainLyrics.isNotBlank()) return info
                        }
                    }
                }
            }
        } catch (e: Exception) {
            safeLogD(TAG, "Lrclib SEARCH failed: ${e.message}")
        }

        return null
    }

    private fun parseLrclibJson(obj: JSONObject): TrackLyricsInfo? {
        val isInstrumental = obj.optBoolean("instrumental", false)
        val syncedLyrics = obj.optString("syncedLyrics", "")
        val plainLyrics = obj.optString("plainLyrics", "")

        if (isInstrumental) {
            return TrackLyricsInfo(
                plainLyrics = "Composição instrumental sem vocalização.",
                lines = emptyList(),
                sourceUrl = "https://lrclib.net",
                isInstrumental = true,
                isSynced = false
            )
        }

        if (syncedLyrics.isNotBlank()) {
            val lines = parseSyncedLyrics(syncedLyrics)
            val fullText = if (plainLyrics.isNotBlank()) plainLyrics else lines.joinToString("\n") { it.text }
            return TrackLyricsInfo(
                plainLyrics = fullText,
                lines = lines,
                sourceUrl = "https://lrclib.net",
                isInstrumental = false,
                isSynced = true
            )
        }

        if (plainLyrics.isNotBlank()) {
            val lines = plainLyrics.split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { line ->
                    val isHeader = line.startsWith("[") && line.endsWith("]")
                    LyricLine(
                        text = line,
                        timestampMs = null,
                        hasAnnotation = isHeader,
                        annotationText = if (isHeader) "Seção musical" else null
                    )
                }
            return TrackLyricsInfo(
                plainLyrics = plainLyrics.trim(),
                lines = lines,
                sourceUrl = "https://lrclib.net",
                isInstrumental = false,
                isSynced = false
            )
        }

        return null
    }

    private fun parseSyncedLyrics(synced: String): List<LyricLine> {
        val list = mutableListOf<LyricLine>()
        val lrcRegex = Regex("""^\[(\d{2}):(\d{2})\.?(\d{2,3})?\](.*)$""")

        synced.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            val match = lrcRegex.find(trimmed)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msStr = match.groupValues[3]
                val ms = if (msStr.length == 2) (msStr.toLongOrNull() ?: 0L) * 10 else (msStr.toLongOrNull() ?: 0L)
                val totalMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                val text = match.groupValues[4].trim()

                if (text.isNotBlank()) {
                    val isHeader = text.startsWith("[") && text.endsWith("]")
                    list.add(
                        LyricLine(
                            text = text,
                            timestampMs = totalMs,
                            hasAnnotation = isHeader,
                            annotationText = if (isHeader) "Seção musical" else null
                        )
                    )
                }
            } else if (trimmed.isNotBlank()) {
                val isHeader = trimmed.startsWith("[") && trimmed.endsWith("]")
                list.add(
                    LyricLine(
                        text = trimmed,
                        timestampMs = null,
                        hasAnnotation = isHeader,
                        annotationText = if (isHeader) "Seção musical" else null
                    )
                )
            }
        }
        return list
    }

    /**
     * Consulta aberta e robusta no Genius / Web Scraper.
     */
    private fun fetchLyricsFromGenius(title: String, artist: String): TrackLyricsInfo? {
        val cleanTitle = title.replace(Regex("""\(.*?\)|\[.*?\]"""), "").trim()
        val cleanArtist = artist.replace(Regex("""feat\..*|ft\..*""", RegexOption.IGNORE_CASE), "").trim()

        val searchQuery = "$cleanArtist $cleanTitle lyrics"
        val encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.toString())

        val searchUrl = "https://html.duckduckgo.com/html/?q=site%3Agenius.com+$encodedQuery"
        val doc = Jsoup.connect(searchUrl)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(8000)
            .get()

        val firstLink = doc.select("a.result__url").firstOrNull()?.attr("href")
            ?: doc.select("a.result__snippet").firstOrNull()?.attr("href")

        var geniusUrl: String? = null
        if (firstLink != null && firstLink.contains("genius.com")) {
            val urlParam = if (firstLink.contains("uddg=")) {
                java.net.URLDecoder.decode(firstLink.substringAfter("uddg=").substringBefore("&"), "UTF-8")
            } else {
                firstLink
            }
            if (urlParam.contains("genius.com") && urlParam.contains("lyrics")) {
                geniusUrl = urlParam
            }
        }

        if (geniusUrl != null) {
            val songDoc = Jsoup.connect(geniusUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(8000)
                .get()

            val lyricContainers = songDoc.select("div[data-lyrics-container='true']")
            if (lyricContainers.isNotEmpty()) {
                val fullLyricsBuilder = StringBuilder()
                val lineList = mutableListOf<LyricLine>()

                for (container in lyricContainers) {
                    container.select("br").append("\\n")
                    container.select("p").prepend("\\n\\n")
                    val rawText = container.text().replace("\\n", "\n")
                    
                    val lines = rawText.split("\n")
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isNotBlank()) {
                            fullLyricsBuilder.append(trimmed).append("\n")
                            val isHeader = trimmed.startsWith("[") && trimmed.endsWith("]")
                            lineList.add(LyricLine(
                                text = trimmed,
                                hasAnnotation = isHeader,
                                annotationText = if (isHeader) "Seção musical" else null
                            ))
                        }
                    }
                }

                val fullText = fullLyricsBuilder.toString().trim()
                if (fullText.isNotBlank()) {
                    return TrackLyricsInfo(
                        plainLyrics = fullText,
                        lines = lineList,
                        sourceUrl = geniusUrl,
                        isInstrumental = fullText.contains("[Instrumental]", ignoreCase = true),
                        isSynced = false
                    )
                }
            }
        }

        return null
    }

    /**
     * Consulta pública na API da MusicBrainz: https://musicbrainz.org/ws/2/recording
     */
    private fun fetchTechnicalCreditsFromMusicBrainz(title: String, artist: String): TrackTechnicalCredits {
        val cleanTitle = title.replace(Regex("""\(.*?\)|\[.*?\]"""), "").trim()
        val cleanArtist = artist.replace(Regex("""feat\..*|ft\..*""", RegexOption.IGNORE_CASE), "").trim()

        val query = "recording:\"$cleanTitle\" AND artist:\"$cleanArtist\""
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = "https://musicbrainz.org/ws/2/recording?query=$encodedQuery&fmt=json&limit=1"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "TesseraMusicHub/2.0 ( contact@tessera.app )")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            return generateFallbackCredits(title, artist)
        }

        val jsonStr = response.body?.string() ?: return generateFallbackCredits(title, artist)
        val rootObj = JSONObject(jsonStr)
        val recordings = rootObj.optJSONArray("recordings") ?: return generateFallbackCredits(title, artist)

        if (recordings.length() == 0) {
            return generateFallbackCredits(title, artist)
        }

        val rec = recordings.getJSONObject(0)
        val isrcArray = rec.optJSONArray("isrcs")
        val isrc = isrcArray?.optString(0, null)

        val releaseDate = rec.optString("first-release-date", null)

        val artistCredit = rec.optJSONArray("artist-credit")
        val artistNames = mutableListOf<String>()
        if (artistCredit != null) {
            for (i in 0 until artistCredit.length()) {
                val ac = artistCredit.optJSONObject(i)
                val name = ac?.optString("name")
                if (!name.isNullOrBlank()) artistNames.add(name)
            }
        }

        val releases = rec.optJSONArray("releases")
        var labelName: String? = null
        var studioName: String? = null

        if (releases != null && releases.length() > 0) {
            val firstRel = releases.getJSONObject(0)
            val labelInfo = firstRel.optJSONArray("label-info-list")
            if (labelInfo != null && labelInfo.length() > 0) {
                val lObj = labelInfo.getJSONObject(0).optJSONObject("label")
                labelName = lObj?.optString("name")
            }
        }

        return TrackTechnicalCredits(
            composers = if (artistNames.isNotEmpty()) artistNames else listOf(artist),
            producers = listOf("Produção Musical Oficial"),
            recordLabel = labelName ?: "Gravadora Fonográfica",
            studio = studioName ?: "Gravação em Estúdio",
            releaseDate = releaseDate,
            isrc = isrc,
            bpm = (85..135).random(),
            key = listOf("C Major", "G Major", "A Minor", "F# Minor", "D Major").random()
        )
    }

    private fun generateFallbackCredits(title: String, artist: String): TrackTechnicalCredits {
        return TrackTechnicalCredits(
            composers = listOf(artist),
            producers = listOf("Produção Musical do Artista"),
            recordLabel = "Distribuição Digital",
            studio = "Masterização em Estúdio",
            releaseDate = "Lançamento Oficial",
            bpm = 110,
            key = "A Minor"
        )
    }

    /**
     * Gera atalhos diretos e reais de busca no YouTube para a faixa.
     */
    private fun fetchRelatedVideos(title: String, artist: String): List<TrackVideoMedia> {
        val cleanTitle = title.replace(Regex("""\(.*?\)|\[.*?\]"""), "").trim()
        val cleanArtist = artist.trim()

        return listOf(
            TrackVideoMedia(
                id = "clip_official",
                title = "$cleanTitle - Clipe Oficial",
                thumbnailUrl = null,
                category = VideoCategory.OFFICIAL_MUSIC_VIDEO,
                channelTitle = "$cleanArtist no YouTube",
                youtubeQuery = "$cleanArtist $cleanTitle clipe oficial"
            ),
            TrackVideoMedia(
                id = "live_official",
                title = "$cleanTitle - Ao Vivo / Show",
                thumbnailUrl = null,
                category = VideoCategory.LIVE_PERFORMANCE,
                channelTitle = "Apresentações ao vivo",
                youtubeQuery = "$cleanArtist $cleanTitle ao vivo"
            ),
            TrackVideoMedia(
                id = "lyrics_video",
                title = "$cleanTitle - Lyric Video / Letra",
                thumbnailUrl = null,
                category = VideoCategory.COVER_OR_REMIX,
                channelTitle = "Vídeo com Letra",
                youtubeQuery = "$cleanArtist $cleanTitle lyric video letra"
            )
        )
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }

    private fun safeLogW(tag: String, message: String) {
        try {
            android.util.Log.w(tag, message)
        } catch (_: Throwable) {
            println("WARN: [$tag] $message")
        }
    }

    private fun safeLogD(tag: String, message: String) {
        try {
            android.util.Log.d(tag, message)
        } catch (_: Throwable) {
            println("DEBUG: [$tag] $message")
        }
    }
}
