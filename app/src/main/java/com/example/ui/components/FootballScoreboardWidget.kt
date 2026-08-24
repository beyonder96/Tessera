package com.example.ui.components
import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.example.data.DetailedFixture
import com.example.viewmodel.TesseraViewModel
import com.example.ui.theme.thermalCard

@Composable
fun DetailedMatchWidget(
    viewModel: TesseraViewModel,
    modifier: Modifier = Modifier
) {
    val match by viewModel.featuredMatch.collectAsState()
    val matchStandings by viewModel.matchStandings.collectAsState()
    val isLoading by viewModel.isLoadingFootball.collectAsState()
    var selectedTab by remember { mutableStateOf("RESUMO") }
    val tabs = listOf("RESUMO", "EVENTOS", "ESCALAÇÕES")
    val pagerState = rememberPagerState(pageCount = { 2 })

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
            .thermalCard(cornerRadius = 28.dp, elevation = 20.dp)
            .padding(24.dp)
    ) {

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            if (page == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Header Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val leagueText = formatLeagueName(match?.matchDetail?.leagueName)
                        Text(
                            text = leagueText.uppercase(),
                            color = Color(0xFFA1A1AA),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
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
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (isMatchLive) "LIVE" else "PRÓXIMO JOGO",
                                        color = MaterialTheme.colorScheme.onSurface,
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
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        val m = match!!

                        // Main Scoreboard Section
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Match-Day Date Header
                            Text(
                                text = if (m.matchDetail.dateFormatted.isNotBlank()) "MATCH-DAY • ${m.matchDetail.dateFormatted.uppercase()}" else "MATCH-DAY",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Scoreboard Row: Home Logo, Placar (0 : 0), Away Logo alinhados na mesma linha
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Home Logo
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TeamLogo(
                                        logoUrl = m.matchDetail.homeTeamLogo,
                                        teamName = m.matchDetail.homeTeamName,
                                        size = 68
                                    )
                                }

                                // Placar Central
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = m.matchDetail.homeGoals?.toString() ?: "0",
                                        fontSize = 72.sp,
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
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Light,
                                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                    )
                                    Text(
                                        text = m.matchDetail.awayGoals?.toString() ?: "0",
                                        fontSize = 72.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif,
                                        style = TextStyle(
                                            brush = scoreGradient,
                                            letterSpacing = (-0.05).em
                                        )
                                    )
                                }

                                // Away Logo
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TeamLogo(
                                        logoUrl = m.matchDetail.awayTeamLogo,
                                        teamName = m.matchDetail.awayTeamName,
                                        size = 68
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Linha de Nomes dos Times e Status (NS / AO VIVO)
                            val statusText = when {
                                m.matchDetail.statusShort == "LIVE" || m.matchDetail.statusShort == "IN_PLAY" || m.matchDetail.statusShort == "2H" -> "2ND HALF 74'"
                                m.matchDetail.statusShort == "1H" -> "1ST HALF"
                                m.matchDetail.statusShort == "HT" -> "INTERVALO"
                                m.matchDetail.statusShort == "FT" -> "FIM DE JOGO"
                                else -> m.matchDetail.statusShort.ifEmpty { "EM ANDAMENTO" }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Nome Time Casa Completo
                                Text(
                                    text = m.matchDetail.homeTeamName.uppercase(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                // Status da Partida (NS, AO VIVO, etc.)
                                Text(
                                    text = statusText.uppercase(),
                                    color = Color(0xFFF97316),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                // Nome Time Visitante Completo
                                Text(
                                    text = m.matchDetail.awayTeamName.uppercase(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                // Tabs Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
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
            } // close Column
            } else { // close if (page == 0)
                StandingsCard(
                    standingsData = matchStandings,
                    homeTeamName = match?.matchDetail?.homeTeamName,
                    awayTeamName = match?.matchDetail?.awayTeamName
                )
            }
        } // close HorizontalPager
        
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color(0xFFF97316) else Color.White.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(6.dp)
                )
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
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Text(text = "Data e Hora", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
            Text(text = match.matchDetail.dateFormatted, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        
        if (!match.venueName.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Outlined.Stadium, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    Text(text = "Estádio", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                Text(text = match.venueName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun MatchEventsTab(match: DetailedFixture) {
    val events = match.events
    if (events.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Nenhum evento registrado até o momento.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            events.take(5).forEach { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "${event.minute}'", fontWeight = FontWeight.Bold, color = Color(0xFFF97316), fontSize = 12.sp)
                        Text(text = event.playerName, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(text = event.typeName.uppercase(), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun MatchLineupsTab(match: DetailedFixture) {
    if (match.homeLineup.isEmpty() && match.awayLineup.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Escalações disponíveis próximo ao início do jogo.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(match.matchDetail.homeTeamName.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                match.homeLineup.take(11).forEach { player ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${player.position ?: "-"}", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.width(16.dp))
                        Text(player.playerName, color = MaterialTheme.colorScheme.onBackground, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(match.matchDetail.awayTeamName.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                match.awayLineup.take(11).forEach { player ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${player.position ?: "-"}", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.width(16.dp))
                        Text(player.playerName, color = MaterialTheme.colorScheme.onBackground, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun TeamLogo(logoUrl: String, teamName: String, size: Int = 48) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl.isNotBlank() && !isError) {
            coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(logoUrl.replace("http://", "https://"))
                    .crossfade(true)
                    .build(),
                contentDescription = teamName,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.size((size * 0.7f).dp),
                onState = { state ->
                    if (state is coil.compose.AsyncImagePainter.State.Error) {
                        isError = true
                        android.util.Log.e("TeamLogo", "Failed to load logo: $logoUrl", state.result.throwable)
                    }
                }
            )
        } else {
            Text(
                text = if (teamName.isNotBlank()) teamName.take(2).uppercase() else "FC",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                fontSize = (size * 0.35f).sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}

@Composable
fun StandingsCard(
    standingsData: com.example.data.apifootball.StandingsData?,
    homeTeamName: String?,
    awayTeamName: String?
) {
    if (standingsData == null) {
        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Text("Classificação não disponível.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 13.sp)
        }
        return
    }
    
    val league = standingsData.league
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (league.logo != null) {
                    TeamLogo(league.logo, league.name, size = 22)
                }
                Text(
                    text = league.name.uppercase(),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "${league.season}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        val allStandings = league.standings.firstOrNull() ?: emptyList()
        if (allStandings.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Sem dados na tabela.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                Text("CLUBE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text("PTS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text("SG", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
            }
            
            allStandings.forEach { rank -> 
                val isFavorite = rank.team.name.equals(homeTeamName, ignoreCase = true) || rank.team.name.equals(awayTeamName, ignoreCase = true) || rank.team.name.contains("Flamengo", ignoreCase = true)
                val zoneBorderColor = when {
                    rank.rank <= 4 -> Color(0xFF10B981) // Libertadores G4
                    rank.rank in 5..6 -> Color(0xFF38BDF8) // Pré-Libertadores
                    rank.rank in 7..12 -> Color(0xFF6366F1) // Sul-Americana
                    rank.rank >= 17 -> Color(0xFFEF4444) // Rebaixamento Z4
                    else -> Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFavorite) Color(0xFFF97316).copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (isFavorite) Color(0xFFF97316).copy(alpha = 0.4f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 5.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (zoneBorderColor != Color.Transparent) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .width(3.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(zoneBorderColor)
                            )
                        }
                        Text(
                            text = "${rank.rank}",
                            color = if (isFavorite) Color(0xFFF97316) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (isFavorite) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (rank.team.logo != null) {
                        TeamLogo(rank.team.logo, rank.team.name, size = 18)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = rank.team.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                        fontWeight = if (isFavorite) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${rank.points}",
                        color = if (isFavorite) Color(0xFFF97316) else MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${rank.goalsDiff}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun formatLeagueName(rawName: String?): String {
    if (rawName.isNullOrBlank()) return "PRÓXIMA PARTIDA"
    val clean = rawName.trim()
    return when {
        clean.contains("Brazilian Serie A", ignoreCase = true) ||
        clean.contains("Brasileiro Serie A", ignoreCase = true) ||
        clean.contains("Brasileirao", ignoreCase = true) -> "Brasileirão Série A"

        clean.contains("Brazilian Serie B", ignoreCase = true) ||
        clean.contains("Brasileiro Serie B", ignoreCase = true) -> "Brasileirão Série B"

        clean.contains("Copa do Brasil", ignoreCase = true) -> "Copa do Brasil"

        clean.contains("Libertadores", ignoreCase = true) -> "Libertadores"

        clean.contains("Sudamericana", ignoreCase = true) ||
        clean.contains("Sul-Americana", ignoreCase = true) -> "Sul-Americana"

        clean.contains("Champions League", ignoreCase = true) -> "Champions League"

        clean.contains("Premier League", ignoreCase = true) -> "Premier League"

        clean.contains("La Liga", ignoreCase = true) -> "La Liga"

        clean.contains("Serie A", ignoreCase = true) -> "Série A"

        clean.contains("Paulista", ignoreCase = true) -> "Paulistão"
        clean.contains("Carioca", ignoreCase = true) -> "Carioca"

        else -> clean
    }
}
