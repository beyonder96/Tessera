package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityRecord
import com.example.data.StepsRecord
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.bounceClick
import com.example.ui.components.themedCardBackground
import com.example.ui.components.themedCardBorder
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryGold
import com.example.viewmodel.TesseraViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    viewModel: TesseraViewModel,
    modifier: Modifier = Modifier
) {
    val allActivities by viewModel.allActivityRecords.collectAsState()
    val allSteps by viewModel.allStepsRecords.collectAsState()

    var currentDateCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR")) }

    val currentDateStr = remember(currentDateCalendar) {
        dateFormat.format(currentDateCalendar.time)
    }

    val isToday = remember(currentDateCalendar) {
        val today = Calendar.getInstance()
        today.get(Calendar.YEAR) == currentDateCalendar.get(Calendar.YEAR) &&
        today.get(Calendar.DAY_OF_YEAR) == currentDateCalendar.get(Calendar.DAY_OF_YEAR)
    }

    // Passos do dia selecionado
    val daySteps = remember(allSteps, currentDateCalendar) {
        val targetDay = currentDateCalendar.get(Calendar.DAY_OF_YEAR)
        val targetYear = currentDateCalendar.get(Calendar.YEAR)
        allSteps.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
            cal.get(Calendar.YEAR) == targetYear && cal.get(Calendar.DAY_OF_YEAR) == targetDay
        }.maxByOrNull { it.count }?.count ?: 0L
    }

    // Calorias estimadas dos passos (~0.04 kcal por passo)
    val stepCalories = remember(daySteps) { (daySteps * 0.04).toInt() }

    // Atividades registradas do dia
    val dayActivities = remember(allActivities, currentDateStr) {
        allActivities.filter { it.date == currentDateStr }
    }

    val manualCaloriesBurned = remember(dayActivities) {
        dayActivities.sumOf { it.caloriesBurned }.toInt()
    }

    val totalCaloriesBurned = remember(stepCalories, manualCaloriesBurned) {
        stepCalories + manualCaloriesBurned
    }

    // Diálogos de adição
    var showAddCardioDialog by remember { mutableStateOf(false) }
    var showAddStrengthDialog by remember { mutableStateOf(false) }

    val thermalBrush = remember { Brush.linearGradient(listOf(Color(0xFFec4899), Color(0xFFf97316))) }
    val cyanBrush = remember { Brush.linearGradient(listOf(PrimaryTeal, Color(0xFF0284C7))) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 96.dp, bottom = 140.dp)
    ) {
        // Date Selector Bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = currentDateCalendar.timeInMillis
                                add(Calendar.DAY_OF_YEAR, -1)
                            }
                            currentDateCalendar = newCal
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Dia Anterior", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isToday) "HOJE" else displayDateFormat.format(currentDateCalendar.time).uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.sp
                        )
                        if (isToday) {
                            Text(
                                text = displayDateFormat.format(currentDateCalendar.time),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = currentDateCalendar.timeInMillis
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                            currentDateCalendar = newCal
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Dia", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Resumo Diário de Calorias Totais
        item {
            Box(
                modifier = PremiumGlassModifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(themedCardBackground())
                    .border(1.dp, themedCardBorder(), RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CALORIAS QUEIMADAS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$totalCaloriesBurned kcal",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(thermalBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }

                    // Divisão: Passos Google Health vs Atividades Manuais
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Passos Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Outlined.DirectionsWalk, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(14.dp))
                                    Text("Google Health", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$daySteps passos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "~$stepCalories kcal gastas",
                                    fontSize = 11.sp,
                                    color = PrimaryTeal,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Cardio & Treinos Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(14.dp))
                                    Text("Treinos / Cardio", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${dayActivities.size} registradas",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$manualCaloriesBurned kcal gastas",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF97316),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Botões de Ação Rápida: + Cardio e + Treino Muscular
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Botão Cardio (Esteira, Bike...)
                Button(
                    onClick = { showAddCardioDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(thermalBrush, RoundedCornerShape(18.dp))
                        .bounceClick { showAddCardioDialog = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Cardio / Esteira", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Botão Treino Muscular
                Button(
                    onClick = { showAddStrengthDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(cyanBrush, RoundedCornerShape(18.dp))
                        .bounceClick { showAddStrengthDialog = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Grupo Muscular", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Lista de Atividades do Dia
        item {
            Text(
                text = "ATIVIDADES DE HOJE (${dayActivities.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (dayActivities.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.SportsGymnastics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nenhuma atividade manual registrada hoje.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(dayActivities, key = { it.id }) { activity ->
                Box(
                    modifier = PremiumGlassModifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(themedCardBackground())
                        .border(1.dp, themedCardBorder(), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (activity.type == "CARDIO") Color(0xFFF97316).copy(alpha = 0.15f)
                                        else PrimaryTeal.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (activity.type == "CARDIO") Icons.Default.DirectionsRun else Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = if (activity.type == "CARDIO") Color(0xFFF97316) else PrimaryTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = activity.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (activity.durationMinutes > 0) {
                                        Text("${activity.durationMinutes} min", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    if (activity.caloriesBurned > 0) {
                                        Text("• ${activity.caloriesBurned.toInt()} kcal", fontSize = 12.sp, color = PrimaryTeal, fontWeight = FontWeight.SemiBold)
                                    }
                                    activity.muscleGroup?.let {
                                        Text("• $it", fontSize = 12.sp, color = SecondaryGold)
                                    }
                                }
                                if (activity.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = activity.notes,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.deleteActivityRecord(activity) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Histórico de Treinos por Grupo Muscular na Semana
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "GRUPOS MUSCULARES NA SEMANA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 1.5.sp
            )
        }

        item {
            Box(
                modifier = PremiumGlassModifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(themedCardBackground())
                    .border(1.dp, themedCardBorder(), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                val weekStrengthActivities = remember(allActivities) {
                    val cal = Calendar.getInstance()
                    val daysList = (0..6).map { dayOffset ->
                        val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -dayOffset) }
                        val dayStr = dateFormat.format(dayCal.time)
                        val dayName = SimpleDateFormat("EEE", Locale("pt", "BR")).format(dayCal.time).uppercase()
                        val strengthForDay = allActivities.filter { it.date == dayStr && it.type == "STRENGTH" }
                        Triple(dayName, dayStr, strengthForDay)
                    }.reversed()
                    daysList
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for ((dayName, dateStr, acts) in weekStrengthActivities) {
                        val hasWorkout = acts.isNotEmpty()
                        val groupSummary = acts.firstOrNull()?.muscleGroup?.take(3) ?: if (hasWorkout) "OK" else "-"

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(dayName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (hasWorkout) PrimaryTeal
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = groupSummary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasWorkout) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Adicionar Cardio (Esteira, Corrida...)
    if (showAddCardioDialog) {
        AddCardioBottomSheet(
            onDismiss = { showAddCardioDialog = false },
            onConfirm = { title, duration, calories, notes ->
                viewModel.addActivityRecord(
                    type = "CARDIO",
                    title = title,
                    muscleGroup = null,
                    durationMinutes = duration,
                    caloriesBurned = calories,
                    notes = notes,
                    date = currentDateStr
                )
                showAddCardioDialog = false
            }
        )
    }

    // Modal Bottom Sheet: Adicionar Treino por Grupo Muscular
    if (showAddStrengthDialog) {
        AddStrengthBottomSheet(
            onDismiss = { showAddStrengthDialog = false },
            onConfirm = { group, notes, duration, calories ->
                viewModel.addActivityRecord(
                    type = "STRENGTH",
                    title = "Musculação - $group",
                    muscleGroup = group,
                    durationMinutes = duration,
                    caloriesBurned = calories,
                    notes = notes,
                    date = currentDateStr
                )
                showAddStrengthDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardioBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Double, String) -> Unit
) {
    var selectedActivity by remember { mutableStateOf("Esteira") }
    var durationText by remember { mutableStateOf("30") }
    var caloriesText by remember { mutableStateOf("200") }
    var notesText by remember { mutableStateOf("") }

    val cardioOptions = listOf("Esteira", "Bicicleta", "Corrida", "Caminhada", "Natação", "Elíptico", "Outro")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 32.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "NOVA ATIVIDADE CARDIO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal,
                    letterSpacing = 1.5.sp
                )

                // Seletor de Tipo
                Text("Tipo de Exercício", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(cardioOptions) { option ->
                        val isSelected = selectedActivity == option
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                .clickable { selectedActivity = option }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = option,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Tempo (min)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = caloriesText,
                        onValueChange = { caloriesText = it },
                        label = { Text("Calorias (kcal)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Observações (ex: inclinação 3%, vel 8 km/h)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Button(
                    onClick = {
                        val duration = durationText.toIntOrNull() ?: 0
                        val calories = caloriesText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        onConfirm(selectedActivity, duration, calories, notesText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Salvar Atividade", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStrengthBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Double) -> Unit
) {
    var selectedGroup by remember { mutableStateOf("Superiores") }
    var notesText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("50") }
    var caloriesText by remember { mutableStateOf("180") }

    val muscleGroups = listOf("Superiores", "Inferiores", "Abdômen", "Costas", "Peito & Braços", "Full Body")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 32.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "REGISTRAR GRUPO MUSCULAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal,
                    letterSpacing = 1.5.sp
                )

                Text("Grupo de Membros", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(muscleGroups) { group ->
                        val isSelected = selectedGroup == group
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                .clickable { selectedGroup = group }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = group,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Exercícios feitos (ex: Supino, Tríceps corda...)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Tempo (min)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = caloriesText,
                        onValueChange = { caloriesText = it },
                        label = { Text("Calorias est. (kcal)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Button(
                    onClick = {
                        val duration = durationText.toIntOrNull() ?: 0
                        val calories = caloriesText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        onConfirm(selectedGroup, notesText, duration, calories)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Salvar Treino", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}
