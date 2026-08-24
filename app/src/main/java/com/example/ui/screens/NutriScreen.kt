package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.HealthProfile
import com.example.data.MealRecord
import com.example.data.MealType
import com.example.ui.components.*
import com.example.ui.theme.PrimaryTeal
import com.example.viewmodel.TesseraViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.lazy.LazyRow
import com.example.data.WaterRecord

@Composable
fun NutriScreen(
    viewModel: TesseraViewModel,
    modifier: Modifier = Modifier
) {
    val healthProfile by viewModel.healthProfile.collectAsState()
    val allMealRecords by viewModel.allMealRecords.collectAsState()
    val allWaterRecords by viewModel.allWaterRecords.collectAsState()

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

    // Refeições do dia
    val dayMeals = remember(allMealRecords, currentDateStr) {
        allMealRecords.filter { it.date == currentDateStr }
    }

    val totalCalories = remember(dayMeals) { dayMeals.sumOf { it.calories } }
    val totalProtein = remember(dayMeals) { dayMeals.sumOf { it.protein } }
    val totalCarbs = remember(dayMeals) { dayMeals.sumOf { it.carbs } }
    val totalFat = remember(dayMeals) { dayMeals.sumOf { it.fat } }
    val totalFiber = remember(dayMeals) { dayMeals.sumOf { it.fiber } }

    val calorieGoal = healthProfile?.dailyCalorieGoal ?: 2000.0
    val proteinGoal = healthProfile?.dailyProteinGoal ?: 140.0
    val carbGoal = healthProfile?.dailyCarbGoal ?: 220.0
    val fatGoal = healthProfile?.dailyFatGoal ?: 60.0
    val fiberGoal = healthProfile?.dailyFiberGoal ?: 30.0

    // Consumo de água do dia
    val dayWaterRecords = remember(allWaterRecords, currentDateStr) {
        allWaterRecords.filter { it.date == currentDateStr }
    }
    val totalWaterMl = remember(dayWaterRecords) { dayWaterRecords.sumOf { it.amountMl } }
    val waterGoalMl = healthProfile?.dailyWaterGoalMl ?: 2000

    var activeMealTypeToAdd by remember { mutableStateOf<MealType?>(null) }
    var showEditGoalsDialog by remember { mutableStateOf(false) }
    var showCustomWaterDialog by remember { mutableStateOf(false) }
    var showEditWaterGoalDialog by remember { mutableStateOf(false) }

    if (activeMealTypeToAdd != null) {
        AddFoodBottomSheet(
            selectedMealType = activeMealTypeToAdd!!,
            selectedDate = currentDateStr,
            viewModel = viewModel,
            onDismiss = { activeMealTypeToAdd = null }
        )
    }

    if (showEditGoalsDialog) {
        EditGoalsDialog(
            currentCalories = calorieGoal,
            currentProtein = proteinGoal,
            currentCarbs = carbGoal,
            currentFat = fatGoal,
            currentFiber = fiberGoal,
            currentWater = waterGoalMl,
            onDismiss = { showEditGoalsDialog = false },
            onSave = { cal, prot, carbs, fat, fib, water ->
                viewModel.updateNutritionGoals(cal, prot, carbs, fat, fib, water)
                showEditGoalsDialog = false
            }
        )
    }

    if (showCustomWaterDialog) {
        AddCustomWaterDialog(
            onDismiss = { showCustomWaterDialog = false },
            onConfirm = { amount ->
                viewModel.addWaterRecord(amount, currentDateStr)
                showCustomWaterDialog = false
            }
        )
    }

    if (showEditWaterGoalDialog) {
        EditWaterGoalDialog(
            currentGoalMl = waterGoalMl,
            onDismiss = { showEditWaterGoalDialog = false },
            onSave = { newGoal ->
                viewModel.updateWaterGoal(newGoal)
                showEditWaterGoalDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 100.dp, bottom = 140.dp)
    ) {
        // Date Selector Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(themedSubtleBackground())
                    .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = currentDateCalendar.timeInMillis
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        currentDateCalendar = newCal
                    }
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Dia anterior", tint = MaterialTheme.colorScheme.onBackground)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isToday) "Hoje, ${displayDateFormat.format(currentDateCalendar.time)}" else displayDateFormat.format(currentDateCalendar.time),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = currentDateCalendar.timeInMillis
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        currentDateCalendar = newCal
                    }
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Próximo dia", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        // Daily Water Tracking Card
        item {
            DailyWaterTrackerCard(
                totalWaterMl = totalWaterMl,
                waterGoalMl = waterGoalMl,
                records = dayWaterRecords,
                onAddWater = { amount -> viewModel.addWaterRecord(amount, currentDateStr) },
                onDeleteRecord = { record -> viewModel.deleteWaterRecord(record) },
                onEditGoal = { showEditWaterGoalDialog = true },
                onCustomAdd = { showCustomWaterDialog = true }
            )
        }

        // Daily Calories & Macros Summary Card
        item {
            DailyMacroSummaryCard(
                totalCalories = totalCalories,
                calorieGoal = calorieGoal,
                totalProtein = totalProtein,
                proteinGoal = proteinGoal,
                totalCarbs = totalCarbs,
                carbGoal = carbGoal,
                totalFat = totalFat,
                fatGoal = fatGoal,
                totalFiber = totalFiber,
                fiberGoal = fiberGoal,
                onEditGoals = { showEditGoalsDialog = true }
            )
        }

        // Meal Sections (Café da Manhã, Almoço, Lanche, Jantar, Ceia)
        items(MealType.values()) { mealType ->
            val mealsForType = dayMeals.filter { it.mealType == mealType.name }
            MealSectionCard(
                mealType = mealType,
                meals = mealsForType,
                onAddFood = { activeMealTypeToAdd = mealType },
                onDeleteMeal = { viewModel.deleteMealRecord(it) }
            )
        }
    }
}

@Composable
private fun DailyMacroSummaryCard(
    totalCalories: Double,
    calorieGoal: Double,
    totalProtein: Double,
    proteinGoal: Double,
    totalCarbs: Double,
    carbGoal: Double,
    totalFat: Double,
    fatGoal: Double,
    totalFiber: Double,
    fiberGoal: Double,
    onEditGoals: () -> Unit
) {
    val remainingCalories = (calorieGoal - totalCalories).coerceAtLeast(0.0)
    val calProgress = (totalCalories / calorieGoal).toFloat().coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header with Calorie Balance and Settings icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RESUMO NUTRICIONAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${totalCalories.toInt()}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = " / ${calorieGoal.toInt()} kcal",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onEditGoals,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(themedSubtleBackground())
                        .border(0.5.dp, themedSubtleBorder(), CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = "Editar Metas",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Calorie Linear Progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { calProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryTeal,
                    trackColor = Color(0x1AFFFFFF)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (totalCalories <= calorieGoal) "Restam ${remainingCalories.toInt()} kcal" else "Meta ultrapassada em ${(totalCalories - calorieGoal).toInt()} kcal",
                        fontSize = 11.sp,
                        color = if (totalCalories <= calorieGoal) PrimaryTeal else Color(0xFFFF8A80),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(calProgress * 100).toInt()}% da meta",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(themedSubtleBorder()))

            // 4 Macro Bars Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroColItem("Proteína", totalProtein, proteinGoal, Color(0xFF64B5F6))
                MacroColItem("Carbo", totalCarbs, carbGoal, Color(0xFFFFB74D))
                MacroColItem("Gordura", totalFat, fatGoal, Color(0xFFE57373))
                MacroColItem("Fibras", totalFiber, fiberGoal, Color(0xFF81C784))
            }
        }
    }
}

@Composable
private fun MacroColItem(
    label: String,
    current: Double,
    goal: Double,
    color: Color
) {
    val progress = (current / goal).toFloat().coerceIn(0f, 1f)

    Column(
        modifier = Modifier.width(68.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${current.toInt()}g",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
        Text(
            text = "meta ${goal.toInt()}g",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun MealSectionCard(
    mealType: MealType,
    meals: List<MealRecord>,
    onAddFood: () -> Unit,
    onDeleteMeal: (MealRecord) -> Unit
) {
    val totalMealCalories = meals.sumOf { it.calories }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Meal Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (mealType) {
                                MealType.BREAKFAST -> Icons.Outlined.Coffee
                                MealType.LUNCH -> Icons.Outlined.Restaurant
                                MealType.SNACK -> Icons.Outlined.Fastfood
                                MealType.DINNER -> Icons.Outlined.DinnerDining
                                MealType.SUPPER -> Icons.Outlined.Nightlight
                            },
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = mealType.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (totalMealCalories > 0) "${totalMealCalories.toInt()} kcal" else "Nenhum alimento registrado",
                            fontSize = 11.sp,
                            color = if (totalMealCalories > 0) PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = onAddFood,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(themedSubtleBackground())
                        .border(0.5.dp, themedSubtleBorder(), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Adicionar Alimento",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Food Items List
            if (meals.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(themedSubtleBorder()))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    meals.forEach { meal ->
                        MealItemRow(meal = meal, onDelete = { onDeleteMeal(meal) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MealItemRow(
    meal: MealRecord,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(themedSubtleBackground())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (meal.imageUrl != null) {
            AsyncImage(
                model = meal.imageUrl,
                contentDescription = meal.name,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = meal.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${meal.calories.toInt()} kcal",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
                if (meal.protein > 0 || meal.carbs > 0) {
                    Text(
                        text = "• P: ${meal.protein.toInt()}g  C: ${meal.carbs.toInt()}g  G: ${meal.fat.toInt()}g",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = "Excluir",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DailyWaterTrackerCard(
    totalWaterMl: Int,
    waterGoalMl: Int,
    records: List<WaterRecord>,
    onAddWater: (Int) -> Unit,
    onDeleteRecord: (WaterRecord) -> Unit,
    onEditGoal: () -> Unit,
    onCustomAdd: () -> Unit
) {
    val progress = (totalWaterMl.toFloat() / waterGoalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    val remainingMl = (waterGoalMl - totalWaterMl).coerceAtLeast(0)
    val percent = ((totalWaterMl.toFloat() / waterGoalMl.coerceAtLeast(1)) * 100).toInt()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WaterDrop,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "HIDRATAÇÃO DIÁRIA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${totalWaterMl} ml",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = " / ${waterGoalMl} ml",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onEditGoal,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(themedSubtleBackground())
                        .border(0.5.dp, themedSubtleBorder(), CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = "Definir Meta de Água",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF38BDF8).copy(alpha = 0.15f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (totalWaterMl >= waterGoalMl) "🎉 Meta de hidratação atingida!" else "Restam $remainingMl ml",
                        fontSize = 11.sp,
                        color = if (totalWaterMl >= waterGoalMl) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$percent% da meta",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Quick Add Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WaterQuickAddButton("+150ml", modifier = Modifier.weight(1f)) { onAddWater(150) }
                WaterQuickAddButton("+250ml", modifier = Modifier.weight(1f)) { onAddWater(250) }
                WaterQuickAddButton("+500ml", modifier = Modifier.weight(1f)) { onAddWater(500) }
                WaterQuickAddButton("+1L", modifier = Modifier.weight(1f)) { onAddWater(1000) }
                WaterQuickAddButton("+ Outro", isOutlined = true, modifier = Modifier.weight(1.1f)) { onCustomAdd() }
            }

            // Logs of the day
            if (records.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(themedSubtleBorder()))

                Text(
                    text = "Registros de hoje",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(records) { record ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(themedSubtleBackground())
                                .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(10.dp))
                                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = "${record.amountMl}ml",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "(${timeFormat.format(Date(record.timestamp))})",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            IconButton(
                                onClick = { onDeleteRecord(record) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remover",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterQuickAddButton(
    text: String,
    modifier: Modifier = Modifier,
    isOutlined: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOutlined) Color.Transparent else Color(0xFF38BDF8).copy(alpha = 0.12f))
            .border(
                1.dp,
                if (isOutlined) themedSubtleBorder() else Color(0xFF38BDF8).copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isOutlined) MaterialTheme.colorScheme.onBackground else Color(0xFF38BDF8)
        )
    }
}

@Composable
private fun AddCustomWaterDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Registrar Água", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Informe a quantidade consumida em mililitros (ml):", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { char -> char.isDigit() } },
                    label = { Text("Quantidade (ml)") },
                    placeholder = { Text("Ex: 350") },
                    colors = themedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = text.toIntOrNull()
                    if (amount != null && amount > 0) {
                        onConfirm(amount)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black)
            ) {
                Text("ADICIONAR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = themedCardBackground(),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun EditWaterGoalDialog(
    currentGoalMl: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoalMl.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Meta Diária de Água", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Defina sua meta diária de hidratação:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it.filter { char -> char.isDigit() } },
                    label = { Text("Meta diária (ml)") },
                    placeholder = { Text("Ex: 2000") },
                    colors = themedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1500, 2000, 2500, 3000).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (goalText == preset.toString()) Color(0xFF38BDF8).copy(alpha = 0.2f) else themedSubtleBackground())
                                .border(
                                    1.dp,
                                    if (goalText == preset.toString()) Color(0xFF38BDF8) else themedSubtleBorder(),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { goalText = preset.toString() }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset / 1000.0}L",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (goalText == preset.toString()) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val g = goalText.toIntOrNull() ?: currentGoalMl
                    if (g > 0) {
                        onSave(g)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black)
            ) {
                Text("SALVAR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = themedCardBackground(),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun EditGoalsDialog(
    currentCalories: Double,
    currentProtein: Double,
    currentCarbs: Double,
    currentFat: Double,
    currentFiber: Double,
    currentWater: Int,
    onDismiss: () -> Unit,
    onSave: (Double, Double, Double, Double, Double, Int) -> Unit
) {
    var calText by remember { mutableStateOf(currentCalories.toInt().toString()) }
    var protText by remember { mutableStateOf(currentProtein.toInt().toString()) }
    var carbsText by remember { mutableStateOf(currentCarbs.toInt().toString()) }
    var fatText by remember { mutableStateOf(currentFat.toInt().toString()) }
    var fiberText by remember { mutableStateOf(currentFiber.toInt().toString()) }
    var waterText by remember { mutableStateOf(currentWater.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Definir Metas Nutricionais", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = calText,
                        onValueChange = { calText = it },
                        label = { Text("Calorias (kcal)") },
                        modifier = Modifier.weight(1f),
                        colors = themedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = waterText,
                        onValueChange = { waterText = it },
                        label = { Text("Água (ml)") },
                        modifier = Modifier.weight(1f),
                        colors = themedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protText,
                        onValueChange = { protText = it },
                        label = { Text("Prot (g)") },
                        modifier = Modifier.weight(1f),
                        colors = themedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        label = { Text("Carb (g)") },
                        modifier = Modifier.weight(1f),
                        colors = themedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("Gord (g)") },
                        modifier = Modifier.weight(1f),
                        colors = themedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fiberText,
                        onValueChange = { fiberText = it },
                        label = { Text("Fibra (g)") },
                        modifier = Modifier.weight(1f),
                        colors = themedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val c = calText.toDoubleOrNull() ?: currentCalories
                    val p = protText.toDoubleOrNull() ?: currentProtein
                    val cb = carbsText.toDoubleOrNull() ?: currentCarbs
                    val f = fatText.toDoubleOrNull() ?: currentFat
                    val fb = fiberText.toDoubleOrNull() ?: currentFiber
                    val w = waterText.toIntOrNull() ?: currentWater
                    onSave(c, p, cb, f, fb, w)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black)
            ) {
                Text("SALVAR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = themedCardBackground(),
        shape = RoundedCornerShape(20.dp)
    )
}
