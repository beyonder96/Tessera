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
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel
import com.example.viewmodel.PetViewModel
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Chat Message structure local to this screen
data class ChatMessage(val id: String, val text: String, val isUser: Boolean)

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

    // Local Chat Message list and input states
    var chatInputText by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isThinking by remember { mutableStateOf(false) }



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
    val userName = remember { sharedPrefs.getString("user_profile_name", "Maria") ?: "Maria" }

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
        } else "04:30"
    }

    val activeTasksText = remember(habits) {
        val pendingCount = habits.count { !it.isCompleted }
        if (pendingCount > 0) {
            "Hoje suas principais tarefas estão concentradas no período da tarde, deixando sua manhã livre para focar."
        } else {
            "Hoje você concluiu todos os seus rituais e tarefas! Sua mente está livre para descansar."
        }
    }

    val personalizedAISummary = remember(sleepText, activeTasksText) {
        "Você dormiu $sleepText. $activeTasksText"
    }

    // Cascade animation entry triggers
    var animateItems by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
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
                    .padding(bottom = 60.dp),
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

                        // 2. CORE FEATURE - TESSERA AI CHAT INTEGRATION CARD
                        TesseraAIChatCard(
                            personalizedSummary = personalizedAISummary,
                            messages = chatMessages,
                            isThinking = isThinking,
                            inputText = chatInputText,
                            onInputChange = { chatInputText = it },
                            onSendMessage = {
                                val userText = chatInputText
                                chatInputText = ""
                                val msgId = System.currentTimeMillis().toString()
                                chatMessages = chatMessages + ChatMessage(msgId, userText, true)
                                isThinking = true
                                
                                coroutineScope.launch {
                                    val petsString = if (petEvents.isEmpty()) "Nenhum compromisso pendente hoje." else petEvents.joinToString("; ") { "${it.petName}: ${it.title} (${if(it.isCompleted) "Concluído" else "Pendente"})" }
                                    val marketString = if (marketItems.isEmpty()) "Nenhuma compra pendente." else marketItems.joinToString("; ") { "${it.name} (${it.quantity} ${it.unit})${if (it.isChecked || it.isBought) " (Comprado)" else " (Pendente)"}" }
                                    val medsString = if (medications.isEmpty()) "Nenhum medicamento agendado." else medications.joinToString("; ") { "${it.name} (${it.dosage}) às ${it.time} - ${if (it.isTaken) "Tomado" else "Pendente"}" }
                                    
                                    val hiddenContext = """
                                        [Contexto] Nome do Usuário: $userName
                                        [Contexto] Patrimônio consolidado: R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", totalPatrimony)}
                                        [Contexto] Compromissos dos Pets: $petsString
                                        [Contexto] Lista de Compras (Mercado): $marketString
                                        [Contexto] Medicamentos e Remédios: $medsString
                                        
                                        Pergunta do usuário: "$userText"
                                    """.trimIndent()
                                    
                                    val response = if (viewModel.isLocalLLMActive) {
                                        try {
                                            viewModel.generateAIResponse(hiddenContext)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            "A IA local não pôde responder."
                                        }
                                    } else {
                                        kotlinx.coroutines.delay(1200)
                                        val cleanText = userText.lowercase()
                                        when {
                                            cleanText.contains("sono") || cleanText.contains("dormir") -> {
                                                "Você registrou $sleepText de sono com eficiência estimada de $sleepEfficiency% de ontem para hoje. Excelente nível de descanso!"
                                            }
                                            cleanText.contains("tarefa") || cleanText.contains("foco") || cleanText.contains("hábito") -> {
                                                val completed = habits.count { it.isCompleted }
                                                val total = habits.size
                                                "Você concluiu $completed de $total rituais hoje. O foco está concentrado para a tarde!"
                                            }
                                            else -> {
                                                "Olá, $userName! Analisei o status de hoje. Patrimônio em R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", totalPatrimony)}. Pets e medicamentos monitorados. Como posso te auxiliar?"
                                            }
                                        }
                                    }
                                    chatMessages = chatMessages + ChatMessage("resp_$msgId", response, false)
                                    isThinking = false
                                }
                            },
                            onResetChat = { chatMessages = emptyList() }
                        )

                        // 3. METRICS & TRACKING CARDS ROW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                SleepCyclesCard(
                                    efficiency = sleepEfficiency,
                                    startTime = startTimeText,
                                    endTime = endTimeText
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                var moodValue by remember { mutableStateOf(0.5f) }
                                InnerStateCard(
                                    value = moodValue,
                                    onValueChange = { moodValue = it }
                                )
                            }
                        }

                        // 4. CONNECTIVITY FLOATING DOCK/PILL
                        ConnectivityDock(
                            onSpotifyClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:open"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
                                    context.startActivity(intent)
                                }
                            },
                            onTwitterClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("twitter://timeline"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com"))
                                    context.startActivity(intent)
                                }
                            },
                            onBellClick = {
                                Toast.makeText(context, "Todas as notificações locais estão em dia.", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // 5. FOOTER - QUIET THE MIND CAROUSEL
                        QuietTheMindSection()
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
                    Text(
                        text = tempVal,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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

// 2. TesseraAIChatCard Component
@Composable
fun TesseraAIChatCard(
    personalizedSummary: String,
    messages: List<ChatMessage>,
    isThinking: Boolean,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onResetChat: () -> Unit
) {
    // Pulse animation for border glowing neon accent
    val infiniteTransition = rememberInfiniteTransition(label = "NeonChatGlow")
    val pulseGlowVal by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ChatPulseAlpha"
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
            // Chat Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF71D7CD),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TESSERA AI ASSISTANT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF71D7CD),
                        letterSpacing = 1.5.sp
                    )
                }
                
                if (messages.isNotEmpty()) {
                    Text(
                        text = "VER RESUMO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0x66FFFFFF),
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clickable { onResetChat() }
                            .padding(4.dp)
                    )
                }
            }

            // Chat Messages / AI Summary display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                if (messages.isEmpty()) {
                    Text(
                        text = personalizedSummary,
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 26.sp
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        messages.forEach { message ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (message.isUser) 16.dp else 4.dp,
                                                bottomEnd = if (message.isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .background(if (message.isUser) Color(0xFF1C1C28) else Color(0x1F71D7CD))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = message.text,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                        
                        if (isThinking) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                                        .background(Color(0x0AFFFFFF))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val pulseAnim = rememberInfiniteTransition(label = "ProcessingChat")
                                        val alpha by pulseAnim.animateFloat(
                                            initialValue = 0.3f, targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(600, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "DotAlpha"
                                        )
                                        Icon(Icons.Outlined.AutoAwesome, null, tint = Color(0xFF71D7CD).copy(alpha = alpha), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Processando...", color = Color(0xFF71D7CD).copy(alpha = alpha), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sleek Chat Input Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(28.dp))
                    .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Conversar com Tessera AI...",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 13.sp
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        cursorBrush = SolidColor(Color(0xFF71D7CD))
                    )
                }
                
                IconButton(
                    onClick = { /* Decorative or placeholder voice command */ },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Comando de voz",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isThinking) Color(0xFF71D7CD) else Color(0x1AFFFFFF))
                        .clickable(enabled = inputText.isNotBlank() && !isThinking) { onSendMessage() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar Mensagem",
                        tint = if (inputText.isNotBlank() && !isThinking) Color.Black else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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
    onSpotifyClick: () -> Unit,
    onTwitterClick: () -> Unit,
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
            // Custom drawn Spotify Icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onSpotifyClick() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(18.dp)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.7f),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.7f),
                        startAngle = -140f,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(2.dp.toPx(), 4.dp.toPx()),
                        size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx())
                    )
                    drawArc(
                        color = Color.White.copy(alpha = 0.7f),
                        startAngle = -140f,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(4.dp.toPx(), 7.dp.toPx()),
                        size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx())
                    )
                }
            }

            // Custom drawn X (Twitter) Icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onTwitterClick() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(size.width, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

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
fun QuietTheMindSection() {
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
