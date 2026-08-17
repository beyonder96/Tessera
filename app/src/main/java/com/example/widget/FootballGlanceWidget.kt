package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
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
        var league = "Brasileirão Série A"

        try {
            withTimeoutOrNull(2500) {
                withContext(Dispatchers.IO) {
                    val prefs = context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)
                    val configuredTeams = prefs.getStringSet("football_teams", setOf("Flamengo (Principal)")) ?: setOf("Flamengo (Principal)")
                    val primaryTeam = configuredTeams.firstOrNull() ?: "Flamengo"
                    val cleanTeamName = primaryTeam.replace("(principal)", "").replace("(equipe principal)", "").trim().lowercase()

                    val teamId = TheSportsDbApi.knownBrazilianTeams.entries.find { cleanTeamName.contains(it.key) }?.value ?: "134287"

                    // Tenta buscar próximo jogo ou último jogo
                    val nextEvents = try {
                        val nextResponse = TheSportsDbApi.service.getNextEvents(teamId)
                        nextResponse.events ?: nextResponse.results ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val lastEvents = if (nextEvents.isEmpty()) {
                        try {
                            val lastResponse = TheSportsDbApi.service.getLastEvents(teamId)
                            lastResponse.events ?: lastResponse.results ?: emptyList()
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
                        league = event.strLeague ?: "Brasileirão Série A"

                        val rawStatus = event.strStatus ?: "NS"
                        status = when (rawStatus) {
                            "FT", "AOT", "PEN" -> "ENCERRADO"
                            "1H", "2H", "HT", "LIVE" -> "AO VIVO"
                            else -> "PRÓXIMO"
                        }

                        val dateFormatted = buildString {
                            event.dateEvent?.let { d ->
                                val parts = d.split("-")
                                if (parts.size == 3) append("${parts[2]}/${parts[1]}") else append(d)
                            }
                            val time = event.strTimeLocal ?: event.strTime
                            if (!time.isNullOrBlank()) {
                                if (isNotEmpty()) append(" ")
                                append(time.take(5))
                            }
                        }
                        matchDate = dateFormatted.ifBlank { "Em breve" }
                    }
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
                league = league
            )
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
    league: String
) {
    val openAppAction = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            putExtra("route", "daily")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    )

    val bgProvider = ColorProvider(day = Color(0xF2FFFFFF), night = Color(0xF2111318))
    val textPrimary = ColorProvider(day = Color(0xFF0F172A), night = Color(0xFFF8FAFC))
    val textSecondary = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))
    val accentOrange = ColorProvider(day = Color(0xFFEA580C), night = Color(0xFFF97316))
    val cardSurface = ColorProvider(day = Color(0x0F0F172A), night = Color(0x1AFFFFFF))

    val statusBg = when (status) {
        "AO VIVO" -> ColorProvider(day = Color(0x26EF4444), night = Color(0x33EF4444))
        "ENCERRADO" -> ColorProvider(day = Color(0x1A64748B), night = Color(0x2694A3B8))
        else -> ColorProvider(day = Color(0x1AEA580C), night = Color(0x26F97316))
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
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(openAppAction),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Top Header Row
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "PLACAR",
                style = TextStyle(color = accentOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = status,
                style = TextStyle(color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier
                    .background(statusBg)
                    .cornerRadius(6.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = league,
            style = TextStyle(color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        )

        Spacer(modifier = GlanceModifier.height(10.dp))

        // Match Info Box
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(cardSurface)
                .cornerRadius(14.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Team
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = homeTeam,
                    style = TextStyle(color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }

            // Score / VS Center Area
            Column(
                modifier = GlanceModifier.width(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (homeScore != null && awayScore != null) {
                    Text(
                        text = "$homeScore - $awayScore",
                        style = TextStyle(color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                } else {
                    Text(
                        text = "VS",
                        style = TextStyle(color = accentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = matchDate,
                        style = TextStyle(color = textSecondary, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }

            // Away Team
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = awayTeam,
                    style = TextStyle(color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
    }
}
