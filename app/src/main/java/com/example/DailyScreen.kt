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
import androidx.compose.ui.geometry.Offset
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
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen(
    viewModel: TesseraViewModel,
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

    // Quick Dialog States
    var showQuickExpenseDialog by remember { mutableStateOf(false) }
    var showQuickWeightDialog by remember { mutableStateOf(false) }

    // Cascade Animation States
    var animateHeader by remember { mutableStateOf(false) }
    var animateInsight by remember { mutableStateOf(false) }
    var animateRings by remember { mutableStateOf(false) }
    var animateInteractiveCards by remember { mutableStateOf(false) }
    var animateQuickActions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateHeader = true
        delay(60L)
        animateInsight = true
        delay(60L)
        animateRings = true
        delay(60L)
        animateInteractiveCards = true
        delay(60L)
        animateQuickActions = true
    }

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

    // Calculations
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.value }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.value }
    val financeBalance = totalIncome - totalExpense

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

    // Dynamic Assistant Briefing Generator
    val assistantInsight = remember(financeBalance, todaySteps, completedHabits, petEvents) {
        val financeStr = if (transactions.isEmpty()) {
            "Suas finanças estão limpas de registros hoje."
        } else if (financeBalance >= 0) {
            "Seu balanço mensal está positivo em R$ ${String.format(Locale("pt", "BR"), "%,.2f", financeBalance)}. Excelente controle!"
        } else {
            "Suas despesas superaram suas receitas neste mês por R$ ${String.format(Locale("pt", "BR"), "%,.2f", -financeBalance)}. Fique atento."
        }

        val healthStr = if (todaySteps == 0L) {
            "Ainda não registramos passos para hoje. Que tal caminhar um pouco?"
        } else if (todaySteps < 6000) {
            "Você deu $todaySteps passos hoje. Falta pouco para atingir um nível ativo!"
        } else {
            "Parabéns! $todaySteps passos registrados. Seu corpo agradece o movimento."
        }

        val habitStr = if (totalHabits == 0) {
            "Nenhum hábito cadastrado para hoje."
        } else if (completedHabits == totalHabits) {
            "Todos os seus $totalHabits hábitos foram concluídos! Dia perfeito."
        } else {
            "Você concluiu $completedHabits de $totalHabits hábitos hoje. Continue focado!"
        }

        val petStr = if (totalPetEvents == 0) {
            "Nenhuma tarefa de pets cadastrada."
        } else {
            val pending = petEvents.count { !it.isCompleted }
            if (pending == 0) {
                "Todas as tarefas de Marie & Churchill foram cumpridas."
            } else {
                "Lembrete: Marie & Churchill possuem $pending atividades aguardando você."
            }
        }

        "Olá! $financeStr $healthStr $habitStr $petStr"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C0F0F),
                        Color(0xFF060808)
                    )
                )
            )
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
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Header Typography with animation
            AnimatedVisibility(
                visible = animateHeader,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(initialOffsetY = { 30 })
            ) {
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
            }

            // AI Insight Card
            AnimatedVisibility(
                visible = animateInsight,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 30 })
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
                        .padding(24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ASSISTENTE TESSERA",
                                color = PrimaryTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = assistantInsight,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Multi Ring Activity Progress (Canvas)
            AnimatedVisibility(
                visible = animateRings,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 40 })
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .then(PremiumGlassModifier)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "METAS DE ATIVIDADE DIÁRIA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Apple/Oura Style Multi Rings
                        Box(
                            modifier = Modifier.size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val stepsPct = (todaySteps.toFloat() / 10000f).coerceIn(0f, 1f)
                            val habitsPct = if (totalHabits > 0) (completedHabits.toFloat() / totalHabits.toFloat()) else 0f
                            val petsPct = if (totalPetEvents > 0) (completedPetEvents.toFloat() / totalPetEvents.toFloat()) else 0f

                            val animatedStepsPct by animateFloatAsState(stepsPct, tween(1500, easing = FastOutSlowInEasing), label = "StepsRingAnim")
                            val animatedHabitsPct by animateFloatAsState(habitsPct, tween(1500, easing = FastOutSlowInEasing), label = "HabitsRingAnim")
                            val animatedPetsPct by animateFloatAsState(petsPct, tween(1500, easing = FastOutSlowInEasing), label = "PetsRingAnim")

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeW = 7.dp.toPx()
                                val spacing = 2.dp.toPx()

                                // Ring 1: Steps (Outer - Teal)
                                val radius1 = (size.width - strokeW) / 2
                                drawArc(color = PrimaryTeal.copy(alpha = 0.08f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = strokeW), alpha = 0.5f)
                                drawArc(color = PrimaryTeal, startAngle = -90f, sweepAngle = 360f * animatedStepsPct, useCenter = false, style = Stroke(width = strokeW, cap = StrokeCap.Round))

                                // Ring 2: Habits (Middle - Gold)
                                val radius2 = radius1 - strokeW - spacing
                                drawArc(color = SecondaryGold.copy(alpha = 0.08f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = strokeW), alpha = 0.5f, topLeft = Offset(strokeW + spacing, strokeW + spacing), size = androidx.compose.ui.geometry.Size(radius2 * 2, radius2 * 2))
                                drawArc(color = SecondaryGold, startAngle = -90f, sweepAngle = 360f * animatedHabitsPct, useCenter = false, style = Stroke(width = strokeW, cap = StrokeCap.Round), topLeft = Offset(strokeW + spacing, strokeW + spacing), size = androidx.compose.ui.geometry.Size(radius2 * 2, radius2 * 2))

                                // Ring 3: Pets (Inner - Purple)
                                val radius3 = radius2 - strokeW - spacing
                                val offset3 = (strokeW + spacing) * 2
                                drawArc(color = TertiaryPurple.copy(alpha = 0.08f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = strokeW), alpha = 0.5f, topLeft = Offset(offset3, offset3), size = androidx.compose.ui.geometry.Size(radius3 * 2, radius3 * 2))
                                drawArc(color = TertiaryPurple, startAngle = -90f, sweepAngle = 360f * animatedPetsPct, useCenter = false, style = Stroke(width = strokeW, cap = StrokeCap.Round), topLeft = Offset(offset3, offset3), size = androidx.compose.ui.geometry.Size(radius3 * 2, radius3 * 2))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("HOJE", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.Check, null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Labels explanation
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            MetasRingLabel(label = "Passos ($todaySteps / 10k)", color = PrimaryTeal)
                            MetasRingLabel(label = "Habitos ($completedHabits / $totalHabits)", color = SecondaryGold)
                            MetasRingLabel(label = "Tarefas Pets ($completedPetEvents / $totalPetEvents)", color = TertiaryPurple)
                        }
                    }
                }
            }

            // Interactive Modules (Habits & Pets)
            AnimatedVisibility(
                visible = animateInteractiveCards,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 50 })
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    
                    // Habits Interactive Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .then(PremiumGlassModifier)
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("LISTA DE HÁBITOS DE HOJE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)
                            Icon(Icons.Outlined.CheckCircle, null, tint = SecondaryGold, modifier = Modifier.size(18.dp))
                        }

                        if (habits.isEmpty()) {
                            Text("Nenhum hábito cadastrado para hoje.", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                habits.forEach { habit ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.03f))
                                            .clickable { viewModel.toggleHabitCompleted(habit) }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(habit.colorHex)).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Map icon name to icon
                                                val iconVector = when(habit.iconName) {
                                                    "WaterDrop" -> Icons.Outlined.WaterDrop
                                                    "MenuBook" -> Icons.Outlined.MenuBook
                                                    "SelfImprovement" -> Icons.Outlined.SelfImprovement
                                                    else -> Icons.Outlined.Check
                                                }
                                                Icon(iconVector, null, tint = Color(android.graphics.Color.parseColor(habit.colorHex)), modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = habit.name,
                                                color = if (habit.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Checkbox(
                                            checked = habit.isCompleted,
                                            onCheckedChange = { viewModel.toggleHabitCompleted(habit) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = SecondaryGold,
                                                uncheckedColor = Color.White.copy(alpha = 0.2f),
                                                checkmarkColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pet Events Interactive Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .then(PremiumGlassModifier)
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TAREFAS PETZ DE HOJE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)
                            Icon(Icons.Outlined.Pets, null, tint = TertiaryPurple, modifier = Modifier.size(18.dp))
                        }

                        if (petEvents.isEmpty()) {
                            Text("Nenhuma atividade de pets registrada.", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                petEvents.forEach { event ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.03f))
                                            .clickable { viewModel.togglePetEventCompleted(event) }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(TertiaryPurple.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (event.title.contains("Alimentação")) Icons.Outlined.Restaurant else Icons.Outlined.DirectionsRun,
                                                    contentDescription = null,
                                                    tint = TertiaryPurple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "${event.petName} - ${event.title}",
                                                    color = if (event.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = event.time,
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        Checkbox(
                                            checked = event.isCompleted,
                                            onCheckedChange = { viewModel.togglePetEventCompleted(event) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = TertiaryPurple,
                                                uncheckedColor = Color.White.copy(alpha = 0.2f),
                                                checkmarkColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions Panel
            AnimatedVisibility(
                visible = animateQuickActions,
                enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 60 })
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "AÇÕES DIÁRIAS EXPRESSAS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quick log water
                        QuickActionButton(
                            label = "Tomar Água",
                            icon = Icons.Outlined.WaterDrop,
                            color = Color(0xFF4FC3F7),
                            onClick = {
                                val waterHabit = habits.find { it.name.contains("Hidratação") || it.name.contains("Água") }
                                if (waterHabit != null) {
                                    if (!waterHabit.isCompleted) {
                                        viewModel.toggleHabitCompleted(waterHabit)
                                        Toast.makeText(context, "Hábito de Hidratação atualizado!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Hidratação de hoje já está concluída!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Complete a default steps or log manually
                                    viewModel.addManualStepsRecord(500, System.currentTimeMillis() - 600000, System.currentTimeMillis())
                                    Toast.makeText(context, "Registrado 500 passos como atividade de hidratação!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Quick weight register
                        QuickActionButton(
                            label = "Registrar Peso",
                            icon = Icons.Outlined.Scale,
                            color = PrimaryTeal,
                            onClick = { showQuickWeightDialog = true },
                            modifier = Modifier.weight(1f)
                        )

                        // Quick expense log
                        QuickActionButton(
                            label = "Nova Despesa",
                            icon = Icons.Outlined.AccountBalanceWallet,
                            color = Color(0xFFFF8A65),
                            onClick = { showQuickExpenseDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
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
                    Text("REGISTRAR DESPESA EXPRESSA", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    
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

    // Quick Weight Dialog
    if (showQuickWeightDialog) {
        var weightInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showQuickWeightDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF070909))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("REGISTRAR PESO HOJE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Peso (kg)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showQuickWeightDialog = false }) {
                            Text("CANCELAR", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val weightVal = weightInput.toDoubleOrNull()
                                if (weightVal != null) {
                                    viewModel.addManualWeightRecord(weightVal)
                                    Toast.makeText(context, "Peso de $weightVal kg salvo!", Toast.LENGTH_SHORT).show()
                                }
                                showQuickWeightDialog = false
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

@Composable
fun MetasRingLabel(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f))
                    .border(0.5.dp, color.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
