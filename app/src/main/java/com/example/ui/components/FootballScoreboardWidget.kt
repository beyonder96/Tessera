package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Stadium
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x0CFFFFFF))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SportsSoccer,
                        contentDescription = null,
                        tint = Color(0xFF71D7CD),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PARTIDA EM DESTAQUE",
                        color = Color(0xFF71D7CD),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF71D7CD), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { viewModel.fetchFootballScores() }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (match == null) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        Text("Carregando informações da partida...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    } else {
                        Text("Nenhuma partida recente encontrada.", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    }
                }
            } else {
                val m = match!!

                // League, Date and Venue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val headerText = if (m.matchDetail.dateFormatted.isNotBlank()) {
                        "${m.matchDetail.leagueName} • ${m.matchDetail.dateFormatted}"
                    } else {
                        m.matchDetail.leagueName
                    }
                    Text(
                        text = headerText.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    if (!m.venueName.isNullOrEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Outlined.Stadium, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                            Text(text = m.venueName, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                }

                // Main Scoreboard
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x04FFFFFF))
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Home Team
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        val homeLogoReq = remember(m.matchDetail.homeTeamLogo) {
                            coil.request.ImageRequest.Builder(context)
                                .data(m.matchDetail.homeTeamLogo.replace("http://", "https://"))
                                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                                .allowHardware(false)
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(
                            model = homeLogoReq,
                            contentDescription = m.matchDetail.homeTeamName,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = m.matchDetail.homeTeamName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    // Score
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(
                            text = "${m.matchDetail.homeGoals ?: "-"} : ${m.matchDetail.awayGoals ?: "-"}",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = m.matchDetail.statusShort,
                            color = if (m.matchDetail.statusShort == "LIVE" || m.matchDetail.statusShort == "IN_PLAY") Color(0xFFFF5252) else Color(0xFF71D7CD),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Away Team
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        val awayLogoReq = remember(m.matchDetail.awayTeamLogo) {
                            coil.request.ImageRequest.Builder(context)
                                .data(m.matchDetail.awayTeamLogo.replace("http://", "https://"))
                                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                                .allowHardware(false)
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(
                            model = awayLogoReq,
                            contentDescription = m.matchDetail.awayTeamName,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = m.matchDetail.awayTeamName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x0AFFFFFF))
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

                // Tab Content
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
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x04FFFFFF)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Outlined.Schedule, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Text(text = "Data e Hora", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
            Text(text = match.matchDetail.dateFormatted, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x04FFFFFF)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Status da Partida", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            Text(text = if(match.matchDetail.statusShort == "FT") "Finalizado" else if(match.matchDetail.statusShort == "NS") "Não Iniciado" else "Em Andamento", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun MatchEventsTab(match: DetailedFixture) {
    if (match.events.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("Nenhum evento registrado.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            match.events.forEach { event ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0x04FFFFFF)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${event.minute}'",
                        color = Color(0xFF71D7CD),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(36.dp)
                    )
                    
                    val iconColor = when {
                        event.typeName.contains("Goal", ignoreCase = true) -> Color(0xFF4CAF50)
                        event.typeName.contains("Yellow", ignoreCase = true) -> Color(0xFFFFC107)
                        event.typeName.contains("Red", ignoreCase = true) -> Color(0xFFFF5252)
                        event.typeName.contains("Substitution", ignoreCase = true) -> Color(0xFF4FC3F7)
                        else -> Color.White.copy(alpha = 0.6f)
                    }
                    
                    val icon = when {
                        event.typeName.contains("Goal", ignoreCase = true) -> Icons.Outlined.SportsSoccer
                        event.typeName.contains("Substitution", ignoreCase = true) -> Icons.Outlined.CompareArrows
                        else -> Icons.Filled.Square
                    }
                    
                    Icon(
                        imageVector = icon,
                        contentDescription = event.typeName,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = event.playerName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    AsyncImage(
                        model = if (event.isHomeTeam) match.matchDetail.homeTeamLogo else match.matchDetail.awayTeamLogo,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MatchLineupsTab(match: DetailedFixture) {
    if (match.homeLineup.isEmpty() && match.awayLineup.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("Escalações não disponíveis.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Home Team Lineup
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AsyncImage(model = match.matchDetail.homeTeamLogo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(text = match.matchDetail.homeTeamName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                match.homeLineup.forEach { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0x04FFFFFF)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = player.position?.toString() ?: "-",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = player.playerName,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Away Team Lineup
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AsyncImage(model = match.matchDetail.awayTeamLogo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(text = match.matchDetail.awayTeamName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                match.awayLineup.forEach { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0x04FFFFFF)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = player.position?.toString() ?: "-",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = player.playerName,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
