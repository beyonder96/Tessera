package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

data class SpotifyTrack(
    val id: String,
    val name: String,
    val artistName: String,
    val albumImageUrl: String,
    val playedAt: String
)

@Composable
fun SpotifyRecentlyPlayedWidget(
    accessToken: String?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    val context = LocalContext.current
    var tracks by remember { mutableStateOf<List<SpotifyTrack>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(accessToken) {
        if (accessToken.isNullOrBlank()) {
            tracks = null
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.IO) {
                val url = URL("https://api.spotify.com/v1/me/player/recently-played?limit=4")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
                
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
                } else if (connection.responseCode == 401) {
                    // Token expirado/inválido
                    null
                } else {
                    null
                }
            }
            if (result != null) {
                tracks = result
            } else {
                errorMessage = "Sua sessão do Spotify expirou ou falhou ao carregar."
                onDisconnectClick() // Desconecta para forçar re-login em caso de erro 401/nulo
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

    if (accessToken.isNullOrBlank()) {
        // Estado Desconectado: Card de Promoção de Login
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x0CFFFFFF))
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SPOTIFY INTEGRATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1DB954),
                        letterSpacing = 1.5.sp
                    )
                }

                Text(
                    text = "Conecte seu Spotify para exibir suas músicas tocadas recentemente diretamente neste painel diário.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 20.sp
                )

                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text(
                        text = "CONECTAR SPOTIFY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    } else {
        // Estado Conectado: Lista de Músicas Tocadas Recentemente
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x0CFFFFFF))
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Headphones,
                            contentDescription = null,
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RECENTLY PLAYED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1DB954),
                            letterSpacing = 1.5.sp
                        )
                    }

                    Text(
                        text = "DESCONECTAR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clickable { onDisconnectClick() }
                            .padding(4.dp)
                    )
                }

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
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
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
}
