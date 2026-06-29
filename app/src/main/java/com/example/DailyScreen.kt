package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.SpotifyRecentlyPlayedWidget
import com.example.ui.components.PremiumWeatherWidget
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel
import com.example.viewmodel.PetViewModel
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    viewModel: TesseraViewModel,
    petViewModel: PetViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 1. Database State Collection
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val weightRecords by viewModel.allWeightRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val stepsRecords by viewModel.allStepsRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val sleepRecords by viewModel.allSleepRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val petEvents by viewModel.allPetEvents.collectAsStateWithLifecycle(initialValue = emptyList())
    val marketItems by viewModel.pendingMarketItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val habits by viewModel.allHabits.collectAsStateWithLifecycle(initialValue = emptyList())
    val healthProfile by viewModel.healthProfile.collectAsStateWithLifecycle(initialValue = null)
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val medications by viewModel.allMedications.collectAsStateWithLifecycle(initialValue = emptyList())
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle(initialValue = null)
    val dailyBriefingText by viewModel.dailyBriefingText.collectAsStateWithLifecycle(initialValue = null)
    val newsArticles by viewModel.newsArticles.collectAsStateWithLifecycle(initialValue = emptyList())
    val spotifyAccessToken by viewModel.spotifyAccessToken.collectAsStateWithLifecycle(initialValue = null)

    val spotifyAuthLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val response = com.spotify.sdk.android.auth.AuthorizationClient.getResponse(result.resultCode, result.data)
        when (response.type) {
            com.spotify.sdk.android.auth.AuthorizationResponse.Type.TOKEN -> {
                viewModel.saveSpotifyToken(response.accessToken)
                android.widget.Toast.makeText(context, "Conectado ao Spotify com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
            }
            com.spotify.sdk.android.auth.AuthorizationResponse.Type.ERROR -> {
                android.widget.Toast.makeText(context, "Erro ao conectar ao Spotify: " + response.error, android.widget.Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }



    var activeMindSession by remember { mutableStateOf<Pair<String, String>?>(null) }



    // Time calculations
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
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }

    // Load user profile name if exists, fallback to dynamic request defaults
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE) }
    val userName = remember { sharedPrefs.getString("user_name", "Kenned") ?: "Kenned" }

    // Calculations for the dynamic summary
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.value }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.value }
    val totalPatrimony = bankAccounts.sumOf { it.balance }
    
    val latestSleepRecord = sleepRecords.lastOrNull()
    val latestSleep = latestSleepRecord?.durationHours ?: 7.5
    
    val sleepText = remember(latestSleep) {
        val hours = latestSleep.toInt()
        val minutes = ((latestSleep - hours) * 60).toInt()
        if (minutes > 0) "${hours}h${minutes.toString().padStart(2, '0')}" else "${hours}h"
    }

    val sleepEfficiency = remember(latestSleep) {
        if (latestSleep == 0.0) 92
        else {
            val base = 88 + (latestSleep % 1.0 * 8).toInt()
            base.coerceIn(60, 98)
        }
    }

    val startTimeText = remember(latestSleepRecord) {
        if (latestSleepRecord != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = latestSleepRecord.startTime }
            String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        } else "22:00"
    }
    val endTimeText = remember(latestSleepRecord) {
        if (latestSleepRecord != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = latestSleepRecord.endTime }
            String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        } else "05:30"
    }

    val activeTasksText = remember(habits) {
        val pendingCount = habits.count { !it.isCompleted }
        if (pendingCount > 0) {
            "Hoje suas principais tarefas estão concentradas no período da tarde, deixando sua manhã livre para focar."
        } else {
            "Hoje você concluiu todos os seus rituais e tarefas! Sua mente está livre para descansar."
        }
    }

    val personalizedAISummary = dailyBriefingText ?: remember(sleepText, activeTasksText) {
        "Você dormiu $sleepText. $activeTasksText"
    }

    // Cascade animation entry triggers
    var animateItems by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.refreshAIInsightsAndMetric()
        delay(80)
        animateItems = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0714), // Deep luxury space purple top
                        Color(0xFF06060B), // Deep blue-black center
                        Color(0xFF020204)  // Pitch black bottom
                    )
                )
            )
    ) {
        // Dynamic Breathing Background Glows
        val infiniteTransition = rememberInfiniteTransition(label = "BackgroundGlow")
        val glowAlpha1 by infiniteTransition.animateFloat(
            initialValue = 0.04f,
            targetValue = 0.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(6000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Glow1"
        )
        val glowAlpha2 by infiniteTransition.animateFloat(
            initialValue = 0.03f,
            targetValue = 0.10f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Glow2"
        )
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(7000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowScale"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Purple blob top right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF9E8AF0).copy(alpha = glowAlpha1), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.2f),
                    radius = size.width * 0.9f * glowScale
                ),
                radius = size.width * 0.9f,
                center = Offset(size.width * 0.85f, size.height * 0.2f)
            )
            // Cyan/Blue blob middle left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF71D7CD).copy(alpha = glowAlpha2), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.6f),
                    radius = size.width * 0.7f * glowScale
                ),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.1f, size.height * 0.6f)
            )
        }

        // Main Scroll Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Minimalist Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x0AFFFFFF), CircleShape)
                        .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar à Home",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "NOW BRIEFING",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 2.5.sp
                )
                Box(modifier = Modifier.size(40.dp)) // Anchor balance spacer
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Entrance Transition
                AnimatedVisibility(
                    visible = animateItems,
                    enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 40 })
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. GREETING & CELESTIAL ARC
                        HeaderGreetingSection(
                            greeting = greeting,
                            userName = userName,
                            hour = hour,
                            weatherState = weatherState
                        )

                        // 2. PREMIUM WEATHER WIDGET
                        PremiumWeatherWidget(weatherState)

                        // 4. CONNECTIVITY FLOATING DOCK/PILL
                        ConnectivityDock(
                            onBellClick = {
                                Toast.makeText(context, "Todas as notificações locais estão em dia.", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // 4.5 SPOTIFY RECENTLY PLAYED WIDGET
                        SpotifyRecentlyPlayedWidget(
                            accessToken = spotifyAccessToken,
                            onConnectClick = {
                                val clientId = "516e4fdbb4e040bfbab885614b32c8bb"
                                val redirectUri = "tessera://spotify-callback"
                                val scopes = "user-read-recently-played"
                                val authUrl = "https://accounts.spotify.com/authorize?client_id=$clientId&response_type=token&redirect_uri=$redirectUri&scope=$scopes"
                                
                                val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder().build()
                                customTabsIntent.launchUrl(context, android.net.Uri.parse(authUrl))
                            },
                            onDisconnectClick = {
                                viewModel.disconnectSpotify()
                            }
                        )
                        
                        // 4.6 X TIMELINE WIDGET
                        // X Timeline removida a pedido do usuário

                        // 4.7 NEWS EXPANDED WIDGET
                        // Notícias removidas a pedido do usuário

                    }
                }
            }
        }
    }
}

// 1. HeaderGreetingSection Component
@Composable
fun HeaderGreetingSection(
    greeting: String,
    userName: String,
    hour: Int,
    weatherState: TesseraViewModel.WeatherInfo?
) {
    val tempVal = if (weatherState != null) "${weatherState.temp.toInt()}°C" else "18°C"
    
    // Choose dynamic text based on the celestial arc status
    val celestialState = when (hour) {
        in 5..11 -> "Dawn / Golden Sun"
        in 12..17 -> "Day / The Peak"
        else -> "Night / Full Moon"
    }

    // Glowing Neon Theme Color for the path tracker dot
    val glowColor = when (hour) {
        in 5..11 -> Color(0xFFFFB74D) // dawn gold
        in 12..17 -> Color(0xFF4FC3F7) // day sky blue
        else -> Color(0xFFD7B4F3) // night lilac
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$greeting, $userName",
            fontFamily = FontFamily.Serif,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(28.dp))

        // Celestial Arc Glass Card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x08FFFFFF))
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Header of Arc Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = celestialState.substringBefore(" / ").uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = celestialState.substringAfter(" / "),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = tempVal,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = weatherState?.city ?: "São Paulo",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Semicircle celestial arc path drawn inside Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        val arcTopLeft = Offset(10.dp.toPx(), 10.dp.toPx())
                        val arcSize = Size(width - 20.dp.toPx(), height * 2 - 20.dp.toPx())
                        
                        // Draw empty background track
                        drawArc(
                            color = Color.White.copy(alpha = 0.1f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        
                        // Compute dot progress on the arc based on hours
                        val dotProgress = when (hour) {
                            in 5..11 -> (hour - 5) / 7f
                            in 12..17 -> (hour - 12) / 6f
                            else -> {
                                if (hour >= 18) (hour - 18) / 10f else (hour + 6) / 10f
                            }
                        }
                        val sweepAngle = 180f * dotProgress
                        
                        // Active colored glow arc
                        drawArc(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = 0.1f),
                                    glowColor
                                )
                            ),
                            startAngle = 180f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                        
                        // Calculate coordinates of the dot
                        val angleRad = Math.toRadians((180f + sweepAngle).toDouble())
                        val rx = (width - 20.dp.toPx()) / 2f
                        val ry = (height * 2 - 20.dp.toPx()) / 2f
                        val cx = width / 2f
                        val cy = height - 10.dp.toPx()
                        
                        val dotX = cx + rx * Math.cos(angleRad)
                        val dotY = cy + ry * Math.sin(angleRad)
                        
                        // Glowing outer circle for dot
                        drawCircle(
                            color = glowColor,
                            radius = 8.dp.toPx(),
                            center = Offset(dotX.toFloat(), dotY.toFloat()),
                            alpha = 0.4f
                        )
                        // Inner bright dot
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(dotX.toFloat(), dotY.toFloat())
                        )
                    }
                }
            }
        }
    }
}

// 2. DailyBriefingCard Component
@Composable
fun DailyBriefingCard(
    personalizedSummary: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NeonBriefGlow")
    val pulseGlowVal by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BriefPulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x0CFFFFFF))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF71D7CD).copy(alpha = pulseGlowVal),
                        Color(0xFF71D7CD).copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF71D7CD),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TESSERA AI SUMMARY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71D7CD),
                    letterSpacing = 1.5.sp
                )
            }

            Text(
                text = personalizedSummary,
                fontFamily = FontFamily.Serif,
                fontSize = 17.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 26.sp
            )
        }
    }
}

// 3. SleepCyclesCard Component
@Composable
fun SleepCyclesCard(
    efficiency: Int,
    startTime: String,
    endTime: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square card
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x0CFFFFFF))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bedtime,
                    contentDescription = null,
                    tint = Color(0xFFC5B4E3),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SLEEP CYCLES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.2.sp
                )
            }

            // Circular Fine-line Progress Ring
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Thin background track
                    drawCircle(
                        color = Color(0x0DFFFFFF),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    
                    // Thin active progress ring in Lavender/Neon
                    drawArc(
                        color = Color(0xFFC5B4E3),
                        startAngle = -90f,
                        sweepAngle = 360f * (efficiency / 100f),
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$efficiency%",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "EFFICIENCY",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.3f),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Subtitle Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = startTime,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Text(
                    text = "dormir",
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.2f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = endTime,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// 4. InnerStateCard (Mood Check-in) Component
@Composable
fun InnerStateCard(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square card
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x0CFFFFFF))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Spa,
                    contentDescription = null,
                    tint = Color(0xFFA5D6A7),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "INNER STATE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.2.sp
                )
            }

            // Label greeting question
            Text(
                text = "Como você se sente?",
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            // Minimalist Slider with Glowing center dot representation
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Slider(
                        value = value,
                        onValueChange = onValueChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFFC5B4E3),
                            inactiveTrackColor = Color(0x1AFFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Scales and labels horizontal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Calm",
                        fontSize = 9.sp,
                        fontWeight = if (value < 0.35f) FontWeight.Bold else FontWeight.Normal,
                        color = if (value < 0.35f) Color.White else Color.White.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "Lucid",
                        fontSize = 9.sp,
                        fontWeight = if (value in 0.35f..0.65f) FontWeight.Bold else FontWeight.Normal,
                        color = if (value in 0.35f..0.65f) Color.White else Color.White.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "Active",
                        fontSize = 9.sp,
                        fontWeight = if (value > 0.65f) FontWeight.Bold else FontWeight.Normal,
                        color = if (value > 0.65f) Color.White else Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

// 5. ConnectivityDock Component
@Composable
fun ConnectivityDock(
    onBellClick: () -> Unit
) {
    // Compact heavily blurred floating dock
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0x0AFFFFFF))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(32.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bell notification icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBellClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificações",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 6. QuietTheMindSection (Footer Image Carousel)
@Composable
fun QuietTheMindSection(onSessionClick: (Pair<String, String>) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quiet the Mind",
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Ver mais",
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }

        // Horizontal Stack ofRounded image cards
        val mindItems = listOf(
            Pair("Lunar Cycles", "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?q=80&w=400&auto=format&fit=crop"),
            Pair("Still Ocean", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=400&auto=format&fit=crop"),
            Pair("Deep Forest", "https://images.unsplash.com/photo-1448375240586-882707db888b?q=80&w=400&auto=format&fit=crop")
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(end = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(mindItems.size) { index ->
                val (title, imageUrl) = mindItems[index]
                
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(240.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x05FFFFFF))
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
                        .clickable { onSessionClick(Pair(title, imageUrl)) }
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Frosted Glass Bottom Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC000000))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Sessão de relaxamento",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 7. QuietTheMindPlayerDialog Component
@Composable
fun QuietTheMindPlayerDialog(
    sessionTitle: String,
    imageUrl: String,
    onDismiss: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var timeLeftSeconds by remember { mutableStateOf(300) } // 5 minutes default
    
    // Timer ticking
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (timeLeftSeconds > 0) {
                delay(1000)
                timeLeftSeconds--
            }
            isPlaying = false
        }
    }
    
    // Breathing state: 0 = inhale, 1 = hold, 2 = exhale, 3 = hold
    var breathingPhase by remember { mutableStateOf(0) }
    var breathingText by remember { mutableStateOf("Inspire") }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (timeLeftSeconds > 0) {
                // Inhale: 4s
                breathingPhase = 0
                breathingText = "Inspire"
                delay(4000)
                
                // Hold: 4s
                breathingPhase = 1
                breathingText = "Segure"
                delay(4000)
                
                // Exhale: 4s
                breathingPhase = 2
                breathingText = "Expire"
                delay(4000)
                
                // Hold: 4s
                breathingPhase = 3
                breathingText = "Segure"
                delay(4000)
            }
        }
    }
    
    val breathingScale by animateFloatAsState(
        targetValue = when (breathingPhase) {
            0 -> 1.5f
            1 -> 1.5f
            2 -> 1.0f
            else -> 1.0f
        },
        animationSpec = tween(
            durationMillis = 4000,
            easing = LinearEasing
        ),
        label = "BreathingScale"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF0F0E17).copy(alpha = 0.95f))
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(32.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QUIET THE MIND",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5D6A7),
                        letterSpacing = 1.5.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
                
                // Session title & Image
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = sessionTitle,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Sessão de relaxamento ativa",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                
                // Interactive Breathing Circle
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer pulsating halo
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(breathingScale)
                            .clip(CircleShape)
                            .background(Color(0x1FA5D6A7))
                            .border(1.5.dp, Color(0x66A5D6A7), CircleShape)
                    )
                    
                    // Inner core circle
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = breathingText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
                
                // Timer Text
                val minutes = timeLeftSeconds / 60
                val seconds = timeLeftSeconds % 60
                val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)
                Text(
                    text = timeFormatted,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Select time
                    IconButton(
                        onClick = {
                            timeLeftSeconds = (timeLeftSeconds + 60).coerceAtMost(900)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0x0DFFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar 1 min",
                            tint = Color.White
                        )
                    }
                    
                    // Play / Pause
                    FloatingActionButton(
                        onClick = { isPlaying = !isPlaying },
                        containerColor = Color(0xFFA5D6A7),
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Iniciar",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    // Reset to 5:00
                    IconButton(
                        onClick = {
                            timeLeftSeconds = 300
                            isPlaying = false
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0x0DFFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reiniciar",
                            tint = Color.White
                        )
                    }
                }
                
                Text(
                    text = "Acompanhe o ritmo para acalmar a mente.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun NewsExpandedSection(articles: List<com.example.data.NewsArticle>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ÚLTIMAS NOTÍCIAS",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 2.sp
            )
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = Color(0xFF64FFDA),
                modifier = Modifier.size(14.dp)
            )
        }

        articles.forEach { article ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E252B).copy(alpha = 0.6f))
                    .clickable { /* Opcional: abrir URL externa */ }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!article.urlToImage.isNullOrEmpty()) {
                    coil.compose.AsyncImage(
                        model = article.urlToImage,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = article.title ?: "Sem título",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.source?.name ?: "Fonte desconhecida",
                        color = Color(0xFF64FFDA).copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
