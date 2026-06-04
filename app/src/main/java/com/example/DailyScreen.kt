package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    viewModel: TesseraViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // State Collection
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val weightRecords by viewModel.allWeightRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val stepsRecords by viewModel.allStepsRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val sleepRecords by viewModel.allSleepRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val petEvents by viewModel.allPetEvents.collectAsStateWithLifecycle(initialValue = emptyList())
    val marketItems by viewModel.pendingMarketItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val habits by viewModel.allHabits.collectAsStateWithLifecycle(initialValue = emptyList())
    val healthProfile by viewModel.healthProfile.collectAsStateWithLifecycle(initialValue = null)

    // Calendar Calculations
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
    
    val weekDayStr = when (dayOfWeek) {
        Calendar.SUNDAY -> "Domingo"
        Calendar.MONDAY -> "Segunda-feira"
        Calendar.TUESDAY -> "Terça-feira"
        Calendar.WEDNESDAY -> "Quarta-feira"
        Calendar.THURSDAY -> "Quinta-feira"
        Calendar.FRIDAY -> "Sexta-feira"
        else -> "Sábado"
    }

    val greeting = when (hour) {
        in 0..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }

    // Consolidated Calculations
    // 1. Finance
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.value }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.value }
    val financeBalance = totalIncome - totalExpense

    // 2. Health
    val latestWeight = weightRecords.lastOrNull()?.weightKg ?: 75.2
    val height = healthProfile?.heightCm?.div(100.0) ?: 1.75
    val bmi = latestWeight / (height * height)
    
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayEnd = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
    
    val todaySteps = stepsRecords.filter { it.startTime >= todayStart && it.endTime <= todayEnd }.sumOf { it.count }
    val latestSleep = sleepRecords.lastOrNull()?.durationHours ?: 7.5

    // 3. Pets
    val completedPetEvents = petEvents.count { it.isCompleted }
    val totalPetEvents = petEvents.size

    // 4. Habits
    val completedHabits = habits.count { it.isCompleted }
    val totalHabits = habits.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070909))
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x0AFFFFFF), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Text(
                text = "DAILY BRIEF",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF71D7CD),
                letterSpacing = 2.5.sp
            )
            Box(modifier = Modifier.size(40.dp)) // Spacer to keep title centered
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Header Typography
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    text = "$weekDayStr, $dayOfMonth de $monthName".uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81928F),
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$greeting, Kenned.",
                    fontFamily = FontFamily.Serif,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    lineHeight = 44.sp
                )
                Text(
                    text = "Seu panorama geral consolidado de hoje.",
                    fontSize = 15.sp,
                    color = Color(0xFF81928F)
                )
            }

            // Consolidated Modules Grid/Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "INDICADORES CHAVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81928F),
                    letterSpacing = 2.sp
                )

                // 1. Finance Section Card
                DailySectionCard(
                    title = "FINANÇAS",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconColor = Color(0xFFF9A826),
                    onClick = { onNavigate("finance") }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Balanço Mensal", fontSize = 13.sp, color = Color(0xFF81928F))
                            Text(
                                text = String.format(Locale("pt", "BR"), "R$ %,.2f", financeBalance),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (financeBalance >= 0) Color(0xFF71D7CD) else Color(0xFFFF6B6B)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Receitas / Despesas", fontSize = 12.sp, color = Color(0xFF81928F))
                            Text(
                                text = String.format(Locale("pt", "BR"), "+R$ %,.2f / -R$ %,.2f", totalIncome, totalExpense),
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // 2. Health Section Card
                DailySectionCard(
                    title = "SAÚDE",
                    icon = Icons.Outlined.FavoriteBorder,
                    iconColor = Color(0xFF71D7CD),
                    onClick = { onNavigate("health") }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Passos Hoje", fontSize = 11.sp, color = Color(0xFF81928F))
                            Text("$todaySteps", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sono", fontSize = 11.sp, color = Color(0xFF81928F))
                            Text(String.format(Locale("pt", "BR"), "%.1fh", latestSleep), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Peso / IMC", fontSize = 11.sp, color = Color(0xFF81928F))
                            Text(String.format(Locale("pt", "BR"), "%.1fkg / %.1f", latestWeight, bmi), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // 3. Pets Section Card
                DailySectionCard(
                    title = "PETZ",
                    icon = Icons.Outlined.Pets,
                    iconColor = Color(0xFFD7B4F3),
                    onClick = { onNavigate("petz") }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Atividades para Marie & Churchill", fontSize = 13.sp, color = Color(0xFF81928F))
                        Text(
                            text = "$completedPetEvents de $totalPetEvents concluídas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 4. Market Section Card
                DailySectionCard(
                    title = "MERCADO",
                    icon = Icons.Outlined.ShoppingCart,
                    iconColor = Color(0xFF4D96FF),
                    onClick = { onNavigate("market") }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Itens na lista de compras", fontSize = 13.sp, color = Color(0xFF81928F))
                        Text(
                            text = "${marketItems.size} pendentes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 5. Habits / Routines Section Card
                DailySectionCard(
                    title = "TAREFAS & RITUAIS",
                    icon = Icons.Outlined.Spa,
                    iconColor = Color(0xFFF9A826),
                    onClick = { onNavigate("goals") }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hábitos concluídos", fontSize = 13.sp, color = Color(0xFF81928F))
                        Text(
                            text = "$completedHabits de $totalHabits",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // News Recommendations
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "RECOMENDAÇÕES DE NOTÍCIAS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81928F),
                    letterSpacing = 2.sp
                )

                // News Card 1: X (Twitter) style
                NewsCard(
                    source = "X • @TesseraAI",
                    time = "Há 1h",
                    title = "Novos módulos premium liberados! Chronos e Focus Focus agora trazem timers avançados e ruído branco analógico programado nativamente.",
                    tag = "#PremiumUpdate",
                    tagColor = Color(0xFF71D7CD),
                    icon = Icons.Outlined.AutoAwesome,
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com"))
                        context.startActivity(browserIntent)
                    }
                )

                // News Card 2: Google News style
                NewsCard(
                    source = "Google Notícias",
                    time = "Há 3h",
                    title = "Como rotinas estruturadas aumentam o foco e a produtividade mental em até 42%, segundo novos estudos neurocientíficos.",
                    tag = "Saúde Mental",
                    tagColor = Color(0xFFD7B4F3),
                    icon = Icons.Outlined.Article,
                    onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://news.google.com"))
                        context.startActivity(browserIntent)
                    }
                )
            }

            // Spotify Shortcut Compact Card
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "FUNDO SONORO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81928F),
                    letterSpacing = 2.sp
                )
                
                SpotifyCompactPlayer {
                    val spotifyIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                    if (spotifyIntent != null) {
                        context.startActivity(spotifyIntent)
                    } else {
                        Toast.makeText(context, "Spotify não instalado. Redirecionando para Web Spotify...", Toast.LENGTH_LONG).show()
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
                        context.startActivity(webIntent)
                    }
                }
            }
        }
    }
}

@Composable
fun DailySectionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF81928F),
                        letterSpacing = 1.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF5E6D6A),
                    modifier = Modifier.size(16.dp)
                )
            }
            content()
        }
    }
}

@Composable
fun NewsCard(
    source: String,
    time: String,
    title: String,
    tag: String,
    tagColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tagColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = source,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tagColor
                    )
                }
                Text(
                    text = time,
                    fontSize = 11.sp,
                    color = Color(0xFF81928F)
                )
            }
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tagColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = tag,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = tagColor
                )
            }
        }
    }
}

@Composable
fun SpotifyCompactPlayer(
    onClick: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Album Art Mock
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF222222)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Dreaming - Tessera Focus",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Lofi Girl • Spotify",
                        fontSize = 12.sp,
                        color = Color(0xFF81928F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play / Pause mock control
                IconButton(
                    onClick = { isPlaying = !isPlaying }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Reproduzir",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Spotify Brand Logo Shortcut Icon
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = "Abrir Spotify",
                    tint = Color(0xFF1DB954),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
