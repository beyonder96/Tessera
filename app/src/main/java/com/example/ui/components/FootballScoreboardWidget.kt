package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Stadium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.data.DetailedFixture
import com.example.viewmodel.TesseraViewModel

@Composable
fun DetailedMatchWidget(
    viewModel: TesseraViewModel,
    modifier: Modifier = Modifier
) {
    val match by viewModel.featuredMatch.collectAsState()
    val isLoading by viewModel.isLoadingFootball.collectAsState()
    var selectedTab by remember { mutableStateOf("RESUMO") }
    val tabs = listOf("RESUMO", "EVENTOS", "ESCALAÇÕES")

    LaunchedEffect(Unit) {
        if (match == null && !isLoading) {
            viewModel.fetchFootballScores()
        }
    }

    // Thermostat / Scoreboard card background gradient matching HTML reference
    val cardGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFF101012),
            0.50f to Color(0xFF0F0F11),
            0.75f to Color(0xFF1A0A06),
            1.0f to Color(0xFF5E1603)
        )
    )

    val scoreGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.35f to Color(0xFFE4E4E7),
            0.75f to Color(0xFFDE4C4C),
            1.0f to Color(0xFFF97316)
        )
    )

    // Pulsing animation for LIVE indicator
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val shape = RoundedCornerShape(36.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardGradient)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(24.dp)
    ) {
        // Bottom warm inner glow & rim edge
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF5500).copy(alpha = 0.40f),
                        Color(0xFF882200).copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 1.1f),
                    radius = size.width * 0.7f
                )
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFFAA00).copy(alpha = 0.85f),
                        Color.Transparent
                    )
                ),
                start = Offset(size.width * 0.15f, size.height - 1.5f),
                end = Offset(size.width * 0.85f, size.height - 1.5f),
                strokeWidth = 3f
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Header Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val leagueText = match?.matchDetail?.leagueName?.takeIf { it.isNotBlank() } ?: "PRÓXIMA PARTIDA"
                Text(
                    text = leagueText.uppercase(),
                    color = Color(0xFFA1A1AA),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFFF97316), strokeWidth = 2.dp)
                    }

                    val isMatchLive = match?.matchDetail?.statusShort in listOf("LIVE", "1H", "2H", "HT", "IN_PLAY")

                    // Live / Scheduled Pill Indicator
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isMatchLive) "LIVE" else "PRÓXIMO JOGO",
                                color = Color(0xFFD4D4D8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isMatchLive) Color(0xFFEF4444).copy(alpha = pulseAlpha) else Color(0xFF10B981))
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.fetchFootballScores() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (match == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLoading) "Carregando placar ao vivo..." else "Nenhuma partida em andamento no momento.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            } else {
                val m = match!!

                // Main Scoreboard Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Home Team
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        TeamLogo(
                            logoUrl = m.matchDetail.homeTeamLogo,
                            teamName = m.matchDetail.homeTeamName,
                            size = 72
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = m.matchDetail.homeTeamName.uppercase(),
                            color = Color(0xFFE4E4E7),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Score Display Center
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (m.matchDetail.dateFormatted.isNotBlank()) "MATCH-DAY • ${m.matchDetail.dateFormatted.uppercase()}" else "MATCH-DAY",
                            color = Color(0xFF71717A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Score Numbers with Vertical Heat Gradient
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = m.matchDetail.homeGoals?.toString() ?: "0",
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                style = TextStyle(
                                    brush = scoreGradient,
                                    letterSpacing = (-0.05).em
                                )
                            )
                            Text(
                                text = ":",
                                color = Color(0xFF52525B),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 12.dp)
                            )
                            Text(
                                text = m.matchDetail.awayGoals?.toString() ?: "0",
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                style = TextStyle(
                                    brush = scoreGradient,
                                    letterSpacing = (-0.05).em
                                )
                            )
                        }

                        // Match Status / Time
                        val statusText = when {
                            m.matchDetail.statusShort == "LIVE" || m.matchDetail.statusShort == "IN_PLAY" || m.matchDetail.statusShort == "2H" -> "2ND HALF 74'"
                            m.matchDetail.statusShort == "1H" -> "1ST HALF"
                            m.matchDetail.statusShort == "HT" -> "INTERVALO"
                            m.matchDetail.statusShort == "FT" -> "FIM DE JOGO"
                            else -> m.matchDetail.statusShort.ifEmpty { "EM ANDAMENTO" }
                        }

                        Text(
                            text = statusText.uppercase(),
                            color = Color(0xFFF97316),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    // Away Team
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        TeamLogo(
                            logoUrl = m.matchDetail.awayTeamLogo,
                            teamName = m.matchDetail.awayTeamName,
                            size = 72
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = m.matchDetail.awayTeamName.uppercase(),
                            color = Color(0xFFE4E4E7),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Tabs Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.30f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Animated Tab Content
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "TabContent"
                ) { targetTab ->
                    when (targetTab) {
                        "RESUMO" -> MatchSummaryTab(m)
                        "EVENTOS" -> MatchEventsTab(m)
                        "ESCALAÇÕES" -> MatchLineupsTab(m)
                    }
                }
            }
        }
    }
}

@Composable
fun MatchSummaryTab(match: DetailedFixture) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Text(text = "Data e Hora", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
            Text(text = match.matchDetail.dateFormatted, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        
        if (!match.venueName.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Outlined.Stadium, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    Text(text = "Estádio", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
                Text(text = match.venueName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun MatchEventsTab(match: DetailedFixture) {
    val events = match.events
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Nenhum evento registrado até o momento.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            events.take(5).forEach { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "${event.minute}'", fontWeight = FontWeight.Bold, color = Color(0xFFF97316), fontSize = 12.sp)
                        Text(text = event.playerName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(text = event.typeName.uppercase(), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun MatchLineupsTab(match: DetailedFixture) {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("Escalações disponíveis próximo ao início do jogo.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
    }
}

@Composable
fun TeamLogo(logoUrl: String, teamName: String, size: Int = 48) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl.isNotBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = teamName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size((size * 0.7f).dp)
            )
        } else {
            Text(
                text = if (teamName.isNotBlank()) teamName.take(2).uppercase() else "FC",
                color = Color.White,
                fontSize = (size * 0.35f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
