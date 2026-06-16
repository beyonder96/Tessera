package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artistName: String,
    val albumImageUrl: String,
    val playedAt: String
)

@Composable
fun SpotifyRecentlyPlayedWidget(accessToken: String? = null) {
    val context = LocalContext.current
    var tracks by remember { mutableStateOf<List<SpotifyTrack>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(accessToken) {
        if (accessToken.isNullOrBlank()) {
            delay(1500)
            tracks = listOf(
                SpotifyTrack("1", "Nightcall", "Kavinsky", "https://i.scdn.co/image/ab67616d000048512410a273e9112fc7e263cde6", "2023-10-27T10:00:00Z"),
                SpotifyTrack("2", "Resonance", "HOME", "https://i.scdn.co/image/ab67616d0000485186fa323136a5369bbde12975", "2023-10-27T09:45:00Z"),
                SpotifyTrack("3", "After Dark", "MrKitty", "https://i.scdn.co/image/ab67616d00004851eb32c4b2caea0af8e8c847e3", "2023-10-27T08:30:00Z"),
                SpotifyTrack("4", "Little Dark Age", "MGMT", "https://i.scdn.co/image/ab67616d000048518b32e2c2ebdd76f2f9f8e404", "2023-10-27T07:15:00Z")
            )
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val result = withContext(Dispatchers.IO) {
                val url = URL("https://api.spotify.com/v1/me/player/recently-played?limit=4")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer " + accessToken)
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val items = json.getJSONArray("items")
                    val parsedTracks = mutableListOf<SpotifyTrack>()
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val track = item.getJSONObject("track")
                        val playedAt = item.getString("played_at")
                        val id = track.getString("id")
                        val name = track.getString("name")
                        val artists = track.getJSONArray("artists")
                        val artistName = if (artists.length() > 0) artists.getJSONObject(0).getString("name") else "Unknown"
                        val images = track.getJSONObject("album").getJSONArray("images")
                        var imageUrl = ""
                        if (images.length() > 0) {
                            imageUrl = images.getJSONObject(if (images.length() > 1) 1 else 0).getString("url")
                        }
                        parsedTracks.add(SpotifyTrack(id, name, artistName, imageUrl, playedAt))
                    }
                    parsedTracks
                } else {
                    null
                }
            }
            if (result != null) {
                tracks = result
            } else {
                errorMessage = "Falha ao carregar."
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: e.toString()
        } finally {
            isLoading = false
        }
    }

    fun formatTimeAgo(dateString: String): String {
        try {
            val instant = java.time.Instant.parse(dateString)
            val past = Date.from(instant)
            val now = Date()
            val diffInSeconds = (now.time - past.time) / 1000

            return when {
                diffInSeconds < 60 -> "agora"
                diffInSeconds < 3600 -> "há " + (diffInSeconds / 60).toString() + " min"
                diffInSeconds < 86400 -> "há " + (diffInSeconds / 3600).toString() + "h"
                else -> "há " + (diffInSeconds / 86400).toString() + "d"
            }
        } catch (e: Exception) {
            return ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "RECENTLY PLAYED",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            if (isLoading) {
                repeat(4) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1AFFFFFF))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.6f).background(Color(0x1AFFFFFF), RoundedCornerShape(4.dp)))
                            Box(modifier = Modifier.height(10.dp).fillMaxWidth(0.4f).background(Color(0x1AFFFFFF), RoundedCornerShape(4.dp)))
                        }
                    }
                }
            } else if (!errorMessage.isNullOrBlank() || tracks.isNullOrEmpty()) {
                Text(
                    text = errorMessage ?: "Nenhum histórico recente.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                tracks?.forEach { track ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:track:" + track.id))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/track/" + track.id))
                                    context.startActivity(intent)
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        AsyncImage(
                            model = track.albumImageUrl,
                            contentDescription = "Capa de " + track.name,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1AFFFFFF)),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.name,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = track.artistName,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        Text(
                            text = formatTimeAgo(track.playedAt),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
