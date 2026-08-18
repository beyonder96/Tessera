package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.TheSportsDbApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class FootballGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var homeTeam = "Flamengo"
        var awayTeam = "Adversário"
        var homeScore: String? = null
        var awayScore: String? = null
        var matchDate = "Em breve"
        var status = "PRÓXIMO"
        var league = "Brasileirão"
        var homeLogoUrl: String? = null
        var awayLogoUrl: String? = null
        var homeLogoBitmap: Bitmap? = null
        var awayLogoBitmap: Bitmap? = null

        try {
            withTimeoutOrNull(3000) {
                withContext(Dispatchers.IO) {
                    val prefs = context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)
                    val configuredTeams = prefs.getStringSet("football_teams", setOf("Flamengo (Principal)")) ?: setOf("Flamengo (Principal)")
                    val primaryTeam = configuredTeams.firstOrNull() ?: "Flamengo"
                    val cleanTeamName = primaryTeam.replace("(principal)", "").replace("(equipe principal)", "").trim().lowercase()

                    val teamId = TheSportsDbApi.knownBrazilianTeams.entries.find { cleanTeamName.contains(it.key) }?.value ?: "134287"

                    val nextEvents = try {
                        TheSportsDbApi.service.getNextEvents(teamId).events ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val lastEvents = if (nextEvents.isEmpty()) {
                        try {
                            TheSportsDbApi.service.getLastEvents(teamId).events ?: emptyList()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } else emptyList()

                    val event = nextEvents.firstOrNull() ?: lastEvents.firstOrNull()
                    if (event != null) {
                        homeTeam = event.strHomeTeam ?: "Flamengo"
                        awayTeam = event.strAwayTeam ?: "Adversário"
                        homeScore = event.intHomeScore
                        awayScore = event.intAwayScore
                        league = (event.strLeague ?: "Brasileirão").replace("Campeonato Brasileiro", "Brasileirão")
                        homeLogoUrl = event.strHomeTeamBadge
                        awayLogoUrl = event.strAwayTeamBadge

                        val rawStatus = event.strStatus ?: "NS"
                        status = when (rawStatus) {
                            "FT", "AOT", "PEN" -> "ENCERRADO"
                            "1H", "2H", "HT", "LIVE" -> "AO VIVO"
                            else -> "PRÓXIMO"
                        }

                        matchDate = com.example.data.formatUtcMatchDateTime(
                            event.dateEvent,
                            event.strTime
                        ).ifBlank { "Em breve" }
                    }

                    // Carrega os Bitmaps das logos (se disponível)
                    homeLogoBitmap = loadTeamBadgeBitmap(context, homeLogoUrl, homeTeam)
                    awayLogoBitmap = loadTeamBadgeBitmap(context, awayLogoUrl, awayTeam)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        provideContent {
            FootballWidgetContent(
                context = context,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                homeScore = homeScore,
                awayScore = awayScore,
                matchDate = matchDate,
                status = status,
                league = league,
                homeLogoBitmap = homeLogoBitmap,
                awayLogoBitmap = awayLogoBitmap
            )
        }
    }

    private suspend fun loadTeamBadgeBitmap(context: Context, url: String?, teamName: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        try {
            val loader = coil.ImageLoader(context)
            val request = coil.request.ImageRequest.Builder(context)
                .data(url.replace("http://", "https://"))
                .allowHardware(false) // Essencial para RemoteViews do Android
                .size(64, 64)
                .build()
            val result = (loader.execute(request) as? coil.request.SuccessResult)?.drawable
            (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            null
        }
    }
}

@androidx.compose.runtime.Composable
fun FootballWidgetContent(
    context: Context,
    homeTeam: String,
    awayTeam: String,
    homeScore: String?,
    awayScore: String?,
    matchDate: String,
    status: String,
    league: String,
    homeLogoBitmap: Bitmap?,
    awayLogoBitmap: Bitmap?
) {
    val openAppAction = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            putExtra("route", "daily")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    )

    val bgProvider = ColorProvider(day = Color(0xF8FFFFFF), night = Color(0xF0121316))
    val textPrimary = ColorProvider(day = Color(0xFF0F172A), night = Color(0xFFF8FAFC))
    val textSecondary = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))
    val accentOrange = ColorProvider(day = Color(0xFFEA580C), night = Color(0xFFF97316))
    val cardSurface = ColorProvider(day = Color(0x0F0F172A), night = Color(0x18FFFFFF))

    val statusBg = when (status) {
        "AO VIVO" -> ColorProvider(day = Color(0x26EF4444), night = Color(0x33EF4444))
        "ENCERRADO" -> ColorProvider(day = Color(0x1464748B), night = Color(0x2094A3B8))
        else -> ColorProvider(day = Color(0x1AEA580C), night = Color(0x22F97316))
    }
    val statusColor = when (status) {
        "AO VIVO" -> ColorProvider(day = Color(0xFFDC2626), night = Color(0xFFEF4444))
        "ENCERRADO" -> textSecondary
        else -> accentOrange
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgProvider)
            .appWidgetBackground()
            .cornerRadius(18.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(openAppAction),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Header minimalista
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = league.uppercase(),
                style = TextStyle(color = textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = status,
                style = TextStyle(color = statusColor, fontSize = 8.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier
                    .background(statusBg)
                    .cornerRadius(4.dp)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        // Match Info Row com Logos
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(cardSurface)
                .cornerRadius(12.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Team
            Row(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                if (homeLogoBitmap != null) {
                    Image(
                        provider = ImageProvider(homeLogoBitmap),
                        contentDescription = homeTeam,
                        modifier = GlanceModifier.size(20.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                }
                Text(
                    text = homeTeam.take(10),
                    style = TextStyle(color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }

            // Score / VS
            Box(
                modifier = GlanceModifier.padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (homeScore != null && awayScore != null) {
                    Text(
                        text = "$homeScore - $awayScore",
                        style = TextStyle(color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "VS",
                            style = TextStyle(color = accentOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = matchDate.take(11),
                            style = TextStyle(color = textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            // Away Team
            Row(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = awayTeam.take(10),
                    style = TextStyle(color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                if (awayLogoBitmap != null) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Image(
                        provider = ImageProvider(awayLogoBitmap),
                        contentDescription = awayTeam,
                        modifier = GlanceModifier.size(20.dp)
                    )
                }
            }
        }
    }
}
