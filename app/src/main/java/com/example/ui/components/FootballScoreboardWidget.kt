package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FootballMatchInfo
import com.example.data.MatchDetail
import com.example.viewmodel.TesseraViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FootballScoreboardWidget(
    viewModel: TesseraViewModel,
    modifier: Modifier = Modifier
) {
    val matches by viewModel.footballMatches.collectAsState()
    val isLoading by viewModel.isLoadingFootball.collectAsState()

    var selectedTab by remember { mutableStateOf("Flamengo") }

    // Encontra a partida do time selecionado
    val currentMatchInfo = remember(matches, selectedTab) {
        matches.find { it.teamName.equals(selectedTab, ignoreCase = true) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(PremiumGlassModifier)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header: Título + Botão de Recarregar
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
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PLACAR DE FUTEBOL",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                } else {
                    IconButton(
                        onClick = { viewModel.fetchFootballScores() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recarregar",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Seleção de Times (Abas modernas com pílula deslizante)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x0AFFFFFF))
                    .border(1.dp, Color(0x0AFFFFFF), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val teams = listOf("Flamengo", "Brasil")
                teams.forEach { team ->
                    val isSelected = team == selectedTab
                    
                    // Animação de background e escala
                    val backgroundTabColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                        animationSpec = tween(300),
                        label = "TabBgColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(backgroundTabColor)
                            .bounceClick { selectedTab = team }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (team == "Brasil") "SELEÇÃO BRASILEIRA" else team.uppercase(),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Conteúdo principal (Placar / Próximo Jogo) com transição suave de conteúdo
            AnimatedContent(
                targetState = currentMatchInfo,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
                },
                label = "MatchContent"
            ) { matchInfo ->
                if (matchInfo == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum dado de jogo disponível",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // 1. ÚLTIMO JOGO (Placar Principal)
                        matchInfo.lastMatch?.let { last ->
                            ScoreboardCard(match = last, title = "ÚLTIMA PARTIDA")
                        }

                        // Separador sutil caso existam ambos os jogos
                        if (matchInfo.lastMatch != null && matchInfo.nextMatch != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0x0AFFFFFF))
                            )
                        }

                        // 2. PRÓXIMO JOGO
                        matchInfo.nextMatch?.let { next ->
                            NextMatchCard(match = next)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreboardCard(match: MatchDetail, title: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Rótulo sutil do tipo de partida
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )
            Text(
                text = match.leagueName,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Placar em linha
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x04FFFFFF))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Time da Casa
            Row(
                modifier = Modifier.weight(1.2f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = match.homeTeamLogo,
                    contentDescription = match.homeTeamName,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = match.homeTeamName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Placar Minimalista Central
            Row(
                modifier = Modifier.weight(0.8f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val homeScore = match.homeGoals?.toString() ?: "-"
                val awayScore = match.awayGoals?.toString() ?: "-"
                
                Text(
                    text = homeScore,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = " : ",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = awayScore,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }

            // Time de Fora
            Row(
                modifier = Modifier.weight(1.2f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = match.awayTeamName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = match.awayTeamLogo,
                    contentDescription = match.awayTeamName,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun NextMatchCard(match: MatchDetail) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Rótulo sutil de Próxima Partida
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRÓXIMA PARTIDA",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )
            Text(
                text = match.leagueName,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Card minimalista do Próximo Jogo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x04FFFFFF))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Confronto com Escudos e Nomes em coluna para minimalismo compacto
            Column(
                modifier = Modifier.weight(1.8f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Mandante
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = match.homeTeamLogo,
                        contentDescription = match.homeTeamName,
                        modifier = Modifier.size(18.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = match.homeTeamName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Visitante
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = match.awayTeamLogo,
                        contentDescription = match.awayTeamName,
                        modifier = Modifier.size(18.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = match.awayTeamName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Horário / Data em destaque do lado direito
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = match.dateFormatted.substringBefore(" "),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = match.dateFormatted.substringAfter(" ", "20:00") + "h",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
