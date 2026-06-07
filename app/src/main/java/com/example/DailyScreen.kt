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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PremiumGlassModifier
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

    // State Collection
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val weightRecords by viewModel.allWeightRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val stepsRecords by viewModel.allStepsRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val sleepRecords by viewModel.allSleepRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val petEvents by viewModel.allPetEvents.collectAsStateWithLifecycle(initialValue = emptyList())
    val marketItems by viewModel.pendingMarketItems.collectAsStateWithLifecycle(initialValue = emptyList())
    val habits by viewModel.allHabits.collectAsStateWithLifecycle(initialValue = emptyList())
    val healthProfile by viewModel.healthProfile.collectAsStateWithLifecycle(initialValue = null)
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val pets by petViewModel.allPets.collectAsStateWithLifecycle(initialValue = emptyList())

    // Quick Dialog States
    var showQuickExpenseDialog by remember { mutableStateOf(false) }
    var showQuickWeightDialog by remember { mutableStateOf(false) }

    // Cascade Animation States
    var animateHeader by remember { mutableStateOf(false) }
    var animateInsight by remember { mutableStateOf(false) }
    var animateWellness by remember { mutableStateOf(false) }
    var animateFinance by remember { mutableStateOf(false) }
    var animatePets by remember { mutableStateOf(false) }
    var animateMarket by remember { mutableStateOf(false) }
    var animatePomodoro by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateHeader = true
        delay(60L)
        animateInsight = true
        delay(60L)
        animateWellness = true
        delay(60L)
        animateFinance = true
        delay(60L)
        animatePets = true
        delay(60L)
        animateMarket = true
        delay(60L)
        animatePomodoro = true
    }

    // Calendar & Hour Details
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

    // Calculations
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.value }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.value }
    val financeBalance = totalIncome - totalExpense
    val totalPatrimony = bankAccounts.sumOf { it.balance }

    val latestWeight = weightRecords.lastOrNull()?.weightKg ?: 0.0
    val heightCm = healthProfile?.heightCm ?: 0.0
    val height = if (heightCm > 0.0) heightCm / 100.0 else 1.75
    val bmi = if (latestWeight > 0.0 && height > 0.0) latestWeight / (height * height) else 0.0
    
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
    val latestSleep = sleepRecords.lastOrNull()?.durationHours ?: 0.0

    val completedPetEvents = petEvents.count { it.isCompleted }
    val totalPetEvents = petEvents.size

    val completedHabits = habits.count { it.isCompleted }
    val totalHabits = habits.size

    // Dynamic Assistant Briefing
    val assistantInsight = remember(financeBalance, todaySteps, completedHabits, petEvents) {
        val financeStr = if (transactions.isEmpty()) {
            "Suas finanças locais estão limpas hoje."
        } else if (financeBalance >= 0) {
            "Seu balanço de hoje está sob controle (+R$ ${String.format(Locale("pt", "BR"), "%,.0f", financeBalance)})."
        } else {
            "Seu balanço hoje está negativo em R$ ${String.format(Locale("pt", "BR"), "%,.0f", -financeBalance)}."
        }

        val healthStr = if (todaySteps == 0L) {
            "Ainda sem passos registrados hoje."
        } else if (todaySteps < 8000) {
            "Você deu $todaySteps passos hoje. Vá em frente!"
        } else {
            "Meta de passos batida: $todaySteps hoje!"
        }

        val habitStr = if (totalHabits == 0) {
            "Sem rituais cadastrados."
        } else if (completedHabits == totalHabits) {
            "Todos os hábitos concluídos!"
        } else {
            "Concluídos $completedHabits de $totalHabits hábitos hoje."
        }

        "Olá! $financeStr $healthStr $habitStr"
    }

    // Hydration Local State persistency simulator via SharedPreferences
    val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE) }
    var waterIntakeMl by remember { mutableStateOf(sharedPrefs.getInt("water_intake_$todayDateStr", 0)) }

    fun addWater(amount: Int) {
        val newVal = waterIntakeMl + amount
        waterIntakeMl = newVal
        sharedPrefs.edit().putInt("water_intake_$todayDateStr", newVal).apply()
        
        // Se bater 2000ml, ativa automaticamente o hábito de água/hidratação se disponível
        if (newVal >= 2000) {
            val waterHabit = habits.find { it.name.contains("Hidratação") || it.name.contains("Água") }
            if (waterHabit != null && !waterHabit.isCompleted) {
                viewModel.toggleHabitCompleted(waterHabit)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF070909), Color(0xFF030404))
                )
            )
            .statusBarsPadding()
    ) {
        // Top Header
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
                text = "TESSERA DAILY",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF71D7CD),
                letterSpacing = 2.5.sp
            )
            Box(modifier = Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Dynamic Weather & Time Header Card
            AnimatedVisibility(
                visible = animateHeader,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 30 })
            ) {
                WeatherHeaderCard(hour, weekDayStr, dayOfMonth, monthName, greeting)
            }

            // 2. Gemma AI Advice Card
            AnimatedVisibility(
                visible = animateInsight,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 30 })
            ) {
                GemmaAICard(
                    insight = assistantInsight,
                    habits = habits,
                    viewModel = viewModel,
                    context = context,
                    onWaterLogged = { addWater(250) },
                    onNewExpenseClick = { showQuickExpenseDialog = true }
                )
            }

            // 3. Wellness Core Card (Steps & Animated Hydration)
            AnimatedVisibility(
                visible = animateWellness,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 30 })
            ) {
                WellnessCard(
                    todaySteps = todaySteps,
                    latestSleep = latestSleep,
                    waterIntakeMl = waterIntakeMl,
                    onAddWater = { amount -> addWater(amount) },
                    onAddManualSteps = {
                        viewModel.addManualStepsRecord(1000, System.currentTimeMillis() - 600000L, System.currentTimeMillis())
                        Toast.makeText(context, "+1.000 passos adicionados localmente!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 4. Finance Consolidated Card
            AnimatedVisibility(
                visible = animateFinance,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 30 })
            ) {
                FinanceDailyCard(
                    totalPatrimony = totalPatrimony,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    transactions = transactions,
                    onAddExpenseClick = { showQuickExpenseDialog = true }
                )
            }

            // 5. Pets Core Card (Marie & Churchill Health / Vaccines / Tasks)
            AnimatedVisibility(
                visible = animatePets,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 30 })
            ) {
                PetsDailyCard(
                    pets = pets,
                    petEvents = petEvents,
                    petViewModel = petViewModel,
                    viewModel = viewModel
                )
            }

            // 6. Shopping & Market Card
            AnimatedVisibility(
                visible = animateMarket,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 30 })
            ) {
                MarketDailyCard(
                    marketItems = marketItems,
                    viewModel = viewModel
                )
            }

            // 7. Focus Pomodoro Connection Card
            AnimatedVisibility(
                visible = animatePomodoro,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 30 })
            ) {
                PomodoroDailyCard(onNavigate)
            }
        }
    }

    // Quick Expense Dialog
    if (showQuickExpenseDialog) {
        var expenseValue by remember { mutableStateOf("") }
        var expenseTitle by remember { mutableStateOf("") }
        var expenseCategory by remember { mutableStateOf("Outros") }

        Dialog(onDismissRequest = { showQuickExpenseDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF070909))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("REGISTRAR DESPESA", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    
                    OutlinedTextField(
                        value = expenseTitle,
                        onValueChange = { expenseTitle = it },
                        label = { Text("Descrição (Ex: Café)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = expenseValue,
                        onValueChange = { expenseValue = it },
                        label = { Text("Valor (R$)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showQuickExpenseDialog = false }) {
                            Text("CANCELAR", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val valD = expenseValue.toDoubleOrNull()
                                if (valD != null && expenseTitle.isNotBlank()) {
                                    viewModel.addTransaction(
                                        title = expenseTitle,
                                        subtitle = "Despesa Expressa",
                                        value = valD,
                                        isIncome = false,
                                        category = expenseCategory
                                    )
                                    Toast.makeText(context, "Despesa registrada!", Toast.LENGTH_SHORT).show()
                                }
                                showQuickExpenseDialog = false
                             },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black)
                        ) {
                            Text("SALVAR")
                        }
                    }
                }
            }
        }
    }
}

// 1. WeatherHeaderCard
@Composable
fun WeatherHeaderCard(hour: Int, weekDayStr: String, dayOfMonth: Int, monthName: String, greeting: String) {
    val skyBrush = when (hour) {
        in 5..11 -> Brush.verticalGradient(colors = listOf(Color(0xFFE0F7FA), Color(0xFF00ACC1)))
        in 12..17 -> Brush.verticalGradient(colors = listOf(Color(0xFF00ACC1), Color(0xFF006064)))
        in 18..19 -> Brush.verticalGradient(colors = listOf(Color(0xFF512DA8), Color(0xFFE91E63)))
        else -> Brush.verticalGradient(colors = listOf(Color(0xFF0B0F19), Color(0xFF020407)))
    }
    val weatherIcon = when (hour) {
        in 5..11 -> Icons.Outlined.WbSunny
        in 12..17 -> Icons.Outlined.LightMode
        in 18..19 -> Icons.Outlined.WbTwilight
        else -> Icons.Outlined.NightlightRound
    }
    val weatherTemp = when (hour) {
        in 5..11 -> "21°C • Manhã Fresca"
        in 12..17 -> "26°C • Sol e Nuvens"
        in 18..19 -> "22°C • Pôr do Sol"
        else -> "18°C • Céu Limpo"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(skyBrush)
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$weekDayStr, $dayOfMonth de $monthName".uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hour in 5..19) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$greeting, Kenned.",
                        fontFamily = FontFamily.Serif,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hour in 5..19) Color.Black else Color.White,
                        lineHeight = 36.sp
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = null,
                        tint = if (hour in 5..17) Color(0xFFF9A826) else if (hour in 18..19) Color(0xFFFFB74D) else Color(0xFFE0F7FA),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = weatherTemp,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (hour in 5..19) Color.Black.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// 2. GemmaAICard
@Composable
fun GemmaAICard(
    insight: String,
    habits: List<com.example.data.Habit>,
    viewModel: TesseraViewModel,
    context: Context,
    onWaterLogged: () -> Unit,
    onNewExpenseClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryTeal.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PrimaryTeal.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TESSERA AI ADVISOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal,
                    letterSpacing = 1.5.sp
                )
            }

            Text(
                text = insight,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // Quick actions inside the AI card
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val waterHabit = habits.find { it.name.contains("Hidratação") || it.name.contains("Água") }
                TextButton(
                    onClick = {
                        if (waterHabit != null) {
                            onWaterLogged()
                            Toast.makeText(context, "Copinho de água somado!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Cadastre o hábito 'Água' primeiro", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = PrimaryTeal.copy(alpha = 0.15f),
                        contentColor = PrimaryTeal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Beber Água", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onNewExpenseClick,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color(0xFFFF8A65).copy(alpha = 0.15f),
                        contentColor = Color(0xFFFF8A65)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lançar Despesa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 3. WellnessCard (Water & Steps)
@Composable
fun WellnessCard(
    todaySteps: Long,
    latestSleep: Double,
    waterIntakeMl: Int,
    onAddWater: (Int) -> Unit,
    onAddManualSteps: () -> Unit
) {
    val progressSteps = (todaySteps / 8000f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "SAÚDE E BEM-ESTAR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.5.sp
            )

            // Steps Progress
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DirectionsWalk, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Passos Hoje", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        text = "$todaySteps / 8.000",
                        color = if (todaySteps >= 8000) PrimaryTeal else Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progressSteps },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape),
                        color = PrimaryTeal,
                        trackColor = Color(0x1AFFFFFF)
                    )
                    IconButton(
                        onClick = onAddManualSteps,
                        modifier = Modifier
                            .size(24.dp)
                            .background(PrimaryTeal.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Mais passos", tint = PrimaryTeal, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0AFFFFFF)))

            // Hydration & Sleep
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Water Tracker
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Hidratação", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    
                    // Cup Animation representation
                    val waterRatio = (waterIntakeMl / 2000f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 55.dp)
                            .border(2.dp, Color(0xFF4FC3F7), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                            .padding(2.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(waterRatio)
                                .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                                .background(Color(0xFF4FC3F7))
                        )
                    }

                    Text("$waterIntakeMl / 2000 ml", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = { onAddWater(250) },
                            colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFF4FC3F7).copy(alpha = 0.1f), contentColor = Color(0xFF4FC3F7)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("+250ml", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { onAddWater(500) },
                            colors = ButtonDefaults.textButtonColors(containerColor = Color(0xFF4FC3F7).copy(alpha = 0.1f), contentColor = Color(0xFF4FC3F7)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("+500ml", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Sleep Summary
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Última Noite", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = Color(0xFFB39DDB), modifier = Modifier.size(36.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format(Locale("pt", "BR"), "%.1f horas", latestSleep),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (latestSleep >= 7.0) "Qualidade Excelente" else "Pode melhorar",
                            color = if (latestSleep >= 7.0) Color(0xFF81C784) else Color(0xFFFFB74D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// 4. FinanceDailyCard
@Composable
fun FinanceDailyCard(
    totalPatrimony: Double,
    totalIncome: Double,
    totalExpense: Double,
    transactions: List<com.example.data.Transaction>,
    onAddExpenseClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FINANÇAS E SALDOS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.5.sp
                )
                IconButton(
                    onClick = onAddExpenseClick,
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFFFF8A65).copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nova Despesa", tint = Color(0xFFFF8A65), modifier = Modifier.size(14.dp))
                }
            }

            // Patrimônio Consolidado
            Column {
                Text("Patrimônio Líquido", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                Text(
                    text = "R$ ${String.format(Locale("pt", "BR"), "%,.2f", totalPatrimony)}",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Mini dual chart
            val total = totalIncome + totalExpense
            val incomeRatio = if (total > 0.0) (totalIncome / total).toFloat() else 0.5f
            val expenseRatio = 1f - incomeRatio

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Receita: R$ ${String.format(Locale("pt", "BR"), "%,.0f", totalIncome)}", color = PrimaryTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Despesa: R$ ${String.format(Locale("pt", "BR"), "%,.0f", totalExpense)}", color = Color(0xFFFF6B6B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                ) {
                    Box(modifier = Modifier.weight(incomeRatio.coerceAtLeast(0.05f)).fillMaxHeight().background(PrimaryTeal))
                    Box(modifier = Modifier.weight(expenseRatio.coerceAtLeast(0.05f)).fillMaxHeight().background(Color(0xFFFF6B6B)))
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0AFFFFFF)))

            // Recent 3 transactions
            Text("LANÇAMENTOS RECENTES", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            
            val recents = transactions.take(3)
            if (recents.isEmpty()) {
                Text("Nenhum lançamento hoje.", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recents.forEach { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (tx.isIncome) PrimaryTeal.copy(alpha = 0.1f) else Color(0xFFFF6B6B).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (tx.isIncome) PrimaryTeal else Color(0xFFFF6B6B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(tx.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(tx.category, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                }
                            }
                            Text(
                                text = "${if (tx.isIncome) "+" else "-"} R$ ${String.format(Locale("pt", "BR"), "%,.2f", tx.value)}",
                                color = if (tx.isIncome) PrimaryTeal else Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// 5. PetsDailyCard (Marie & Churchill vaccine and daily checklist)
@Composable
fun PetsDailyCard(
    pets: List<com.example.data.PetEntity>,
    petEvents: List<com.example.data.PetEvent>,
    petViewModel: PetViewModel,
    viewModel: TesseraViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "PETZ CENTER",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.5.sp
            )

            // Marie & Churchill Cards side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Marie
                val marie = pets.find { it.name.contains("Marie") }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(TertiaryPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("M", color = TertiaryPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Marie", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Golden Retriever", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        }
                    }
                    val v4Expired = petViewModel.isVaccineExpired(marie?.lastV4VaccineDate)
                    val raivaExpired = petViewModel.isVaccineExpired(marie?.lastRaivaVaccineDate)
                    val antiExpired = petViewModel.isAntipulgasExpired(marie?.lastAntipulgasDate)

                    if (v4Expired || raivaExpired || antiExpired) {
                        Text("⚠️ Vacinas Vencidas", color = Color(0xFFFF8A65), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("✅ Saúde em Dia", color = PrimaryTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Churchill
                val churchill = pets.find { it.name.contains("Churchill") }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(TertiaryPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("C", color = TertiaryPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Churchill", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Buldogue Francês", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        }
                    }
                    val cv4Expired = petViewModel.isVaccineExpired(churchill?.lastV4VaccineDate)
                    val craivaExpired = petViewModel.isVaccineExpired(churchill?.lastRaivaVaccineDate)
                    val cantiExpired = petViewModel.isAntipulgasExpired(churchill?.lastAntipulgasDate)

                    if (cv4Expired || craivaExpired || cantiExpired) {
                        Text("⚠️ Pendências Clínicas", color = Color(0xFFFF8A65), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("✅ Saúde em Dia", color = PrimaryTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0AFFFFFF)))

            // Pet daily events checklist
            Text("TAREFAS DE HOJE DOS PETS", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold)

            if (petEvents.isEmpty()) {
                Text("Nenhuma tarefa para Marie & Churchill hoje.", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    petEvents.forEach { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .clickable { viewModel.togglePetEventCompleted(event) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (event.isCompleted) TertiaryPurple else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "${event.petName}: ${event.title} (${event.time})",
                                    color = if (event.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 6. MarketDailyCard (Shopping list checklist and direct quick add input)
@Composable
fun MarketDailyCard(
    marketItems: List<com.example.data.MarketItem>,
    viewModel: TesseraViewModel
) {
    var newItemText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "MERCADO / COMPRAS PENDENTES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.5.sp
            )

            // Direct input to add item
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    placeholder = { Text("Novo item...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = Color(0x1AFFFFFF)
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )

                IconButton(
                    onClick = {
                        if (newItemText.isNotBlank()) {
                            viewModel.addMarketItem(newItemText)
                            newItemText = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryTeal, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0AFFFFFF)))

            // Item Checklist
            if (marketItems.isEmpty()) {
                Text("Nenhum item pendente de compras!", color = PrimaryTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    marketItems.take(5).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.02f))
                                .clickable { viewModel.toggleMarketItemChecked(item) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                val qtyText = if (item.unit == "Kg") "${item.quantity} Kg" else "${item.quantity.toInt()} un"
                                Text(
                                    text = "${item.name} ($qtyText)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    if (marketItems.size > 5) {
                        Text(
                            text = "+ ${marketItems.size - 5} outros itens pendentes...",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// 7. PomodoroDailyCard
@Composable
fun PomodoroDailyCard(onNavigate: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
            .clickable { onNavigate("focus") }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Foco Pomodoro", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Inicie um temporizador de foco produtivo", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Acessar", tint = SecondaryGold)
        }
    }
}
