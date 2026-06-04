package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Habit
import com.example.data.PurchaseGoal
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val purchaseGoals by viewModel.allPurchaseGoals.collectAsStateWithLifecycle()

    var showAddPurchaseGoalDialog by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var goalToEdit by remember { mutableStateOf<PurchaseGoal?>(null) }

    var selectedTab by remember(viewModel.selectedGoalsTab) { mutableStateOf(viewModel.selectedGoalsTab) }
    var showExpansionMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFF070909), // Oura deep black
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (selectedTab) {
                                1 -> "Chronos"
                                2 -> "Focus"
                                else -> "Foco & Rotinas"
                            },
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 28.sp,
                            color = Color(0xFFDFE3E2)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onHomeClick) {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = "Home",
                                tint = Color(0xFFBDC9C6)
                            )
                        }
                    },
                    actions = {
                        if (selectedTab == 0) {
                            IconButton(onClick = { showExpansionMenu = true }) {
                                Icon(Icons.Outlined.Add, contentDescription = "Adicionar", tint = Color(0xFFBDC9C6))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Internal navigation tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141918))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Rituais", "Chronos", "Focus").forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val bgSelectedColor = when (index) {
                            1 -> Color(0xFF0F2624) // Teal dark
                            2 -> Color(0xFF1D0F1C) // Purple/Black
                            else -> Color(0xFF26200F) // Gold/Black
                        }
                        val textAccentColor = when (index) {
                            1 -> Color(0xFF71D7CD)
                            2 -> Color(0xFFD7B4F3)
                            else -> Color(0xFFF9A826)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) bgSelectedColor else Color.Transparent)
                                .clickable { 
                                    selectedTab = index 
                                    viewModel.selectedGoalsTab = index
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) textAccentColor else Color(0xFF81928F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "MainTabsContent"
                ) { tab ->
                    when (tab) {
                        1 -> ChronosScreen(viewModel = viewModel)
                        2 -> PomodoroScreen()
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                contentPadding = PaddingValues(bottom = 120.dp)
                            ) {
                                item { Spacer(modifier = Modifier.height(16.dp)) }

                                // Section: Hábitos (Rituais)
                                item {
                                    SectionHeader("RITUAIS DIÁRIOS", Icons.Outlined.Spa)
                                }
                                
                                if (habits.isEmpty()) {
                                    item {
                                        Text("Nenhum hábito configurado.", color = Color(0xFF5E6D6A), modifier = Modifier.padding(vertical = 16.dp))
                                    }
                                } else {
                                    items(habits, key = { "habit_${it.id}" }) { habit ->
                                        HabitCard(
                                            habit = habit, 
                                            onToggle = { viewModel.toggleHabitCompleted(habit) },
                                            onEditClick = { habitToEdit = habit }
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                }

                                item { Spacer(modifier = Modifier.height(40.dp)) }

                                // Section: Metas de Compra
                                item {
                                    SectionHeader("LISTA DE DESEJOS", Icons.Outlined.StarBorder)
                                }

                                if (purchaseGoals.isEmpty()) {
                                    item {
                                        Text("Nenhuma meta de compra configurada.", color = Color(0xFF5E6D6A), modifier = Modifier.padding(vertical = 16.dp))
                                    }
                                } else {
                                    items(purchaseGoals, key = { "goal_${it.id}" }) { goal ->
                                        PurchaseGoalCard(
                                            goal = goal, 
                                            onAddFunds = { amount -> viewModel.updatePurchaseGoalProgress(goal, amount) },
                                            onEditClick = { goalToEdit = goal }
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Premium Expansion Menu Overlay
        AnimatedVisibility(
            visible = showExpansionMenu,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000)) // 90% black
                    .zIndex(100f)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left half sliding from left: Nova Meta
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .animateEnterExit(
                                enter = slideInHorizontally(animationSpec = tween(400, easing = EaseOutQuad)) { -it },
                                exit = slideOutHorizontally(animationSpec = tween(400, easing = EaseInQuad)) { -it }
                            )
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1E170A), Color(0xFF0D0A05))
                                )
                            )
                            .clickable {
                                showExpansionMenu = false
                                showAddPurchaseGoalDialog = true
                            }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = null,
                                tint = Color(0xFFF9A826),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nova Meta",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Defina seus desejos materiais e economize com propósito.",
                                fontSize = 12.sp,
                                color = Color(0xFFBDC9C6),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Right half sliding from right: Novo Ritual
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .animateEnterExit(
                                enter = slideInHorizontally(animationSpec = tween(400, easing = EaseOutQuad)) { it },
                                exit = slideOutHorizontally(animationSpec = tween(400, easing = EaseInQuad)) { it }
                            )
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF0B1E1C), Color(0xFF050D0C))
                                )
                            )
                            .clickable {
                                showExpansionMenu = false
                                showAddHabitDialog = true
                            }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Spa,
                                contentDescription = null,
                                tint = Color(0xFF71D7CD),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Novo Ritual",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Construa hábitos diários consistentes e saudáveis.",
                                fontSize = 12.sp,
                                color = Color(0xFFBDC9C6),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Close button at top-right
                IconButton(
                    onClick = { showExpansionMenu = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .background(Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                }
            }
        }
    }

    if (showAddPurchaseGoalDialog) {
        AddPurchaseGoalDialog(
            onDismiss = { showAddPurchaseGoalDialog = false },
            onSave = { title, target, current, url, deadline, color -> 
                viewModel.addPurchaseGoal(title, target, current, url, deadline, color)
                showAddPurchaseGoalDialog = false
            }
        )
    }

    if (showAddHabitDialog) {
        AddHabitDialog(
            onDismiss = { showAddHabitDialog = false },
            onSave = { name, iconName, colorHex ->
                viewModel.addHabit(name, iconName, colorHex)
                showAddHabitDialog = false
            }
        )
    }

    if (habitToEdit != null) {
        EditHabitDialog(
            habit = habitToEdit!!,
            onDismiss = { habitToEdit = null },
            onSave = { updatedHabit ->
                viewModel.updateHabit(updatedHabit)
                habitToEdit = null
            },
            onDelete = { habitToDelete ->
                viewModel.deleteHabit(habitToDelete)
                habitToEdit = null
            }
        )
    }

    if (goalToEdit != null) {
        EditPurchaseGoalDialog(
            goal = goalToEdit!!,
            onDismiss = { goalToEdit = null },
            onSave = { updatedGoal ->
                viewModel.updatePurchaseGoal(updatedGoal)
                goalToEdit = null
            },
            onDelete = { goalToDelete ->
                viewModel.deletePurchaseGoal(goalToDelete)
                goalToEdit = null
            }
        )
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF81928F), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color(0xFF81928F)
        )
    }
}

@Composable
fun HabitCard(habit: Habit, onToggle: () -> Unit, onEditClick: () -> Unit) {
    val icon = when (habit.iconName) {
        "WaterDrop" -> Icons.Outlined.WaterDrop
        "MenuBook" -> Icons.Outlined.MenuBook
        "SelfImprovement" -> Icons.Outlined.SelfImprovement
        else -> Icons.Outlined.TaskAlt
    }
    
    val color = try { Color(android.graphics.Color.parseColor(habit.colorHex)) } catch (e: Exception) { Color(0xFF71D7CD) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(if (habit.isCompleted) PremiumGlassModifier else Modifier.background(Color(0xFF141918)))
            .border(1.dp, if (habit.isCompleted) color.copy(alpha = 0.3f) else Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
            .clickable { onToggle() }
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (habit.isCompleted) color.copy(alpha = 0.15f) else Color(0x0AFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (habit.isCompleted) color else Color(0xFF81928F), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = habit.name,
                    fontSize = 18.sp,
                    color = if (habit.isCompleted) Color(0xFFDFE3E2) else Color(0xFFBDC9C6),
                    fontWeight = if (habit.isCompleted) FontWeight.SemiBold else FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF9A826), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${habit.streak} dias", fontSize = 12.sp, color = Color(0xFF81928F))
                }
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onEditClick) {
                Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = Color(0xFF81928F), modifier = Modifier.size(20.dp))
            }
            
            // Animated Checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (habit.isCompleted) color else Color(0xFF3D4947), CircleShape)
                    .background(if (habit.isCompleted) color else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = habit.isCompleted,
                    enter = scaleIn(tween(300)),
                    exit = scaleOut(tween(300))
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun PurchaseGoalCard(goal: PurchaseGoal, onAddFunds: (Double) -> Unit, onEditClick: () -> Unit) {
    val progress = (goal.currentValue / goal.targetValue).coerceIn(0.0, 1.0)
    val color = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Color(0xFFF9A826) }
    val daysLeft = ((goal.deadlineTimestamp - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)
    var showAddFunds by remember { mutableStateOf(false) }
    var fundsAmount by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box {
            // Image Placeholder
            AsyncImage(
                model = goal.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay
            Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF070909)))))
 
            // Edit button
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(36.dp)
                    .background(Color(0x4D000000), CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Editar Meta",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            // Percentage Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x4D000000))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "${(progress * 100).roundToInt()}%", 
                    color = color, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp
                )
            }
            
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Text(
                    text = goal.title,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        // Details & Progress
        Column(modifier = Modifier.padding(20.dp)) {
            val formattedCurrent = String.format(Locale("pt", "BR"), "R$ %,.2f", goal.currentValue)
            val formattedTarget = String.format(Locale("pt", "BR"), "R$ %,.2f", goal.targetValue)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(formattedCurrent, fontSize = 22.sp, color = Color(0xFFDFE3E2), fontWeight = FontWeight.Bold)
                Text("de $formattedTarget", fontSize = 14.sp, color = Color(0xFF81928F))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar
            val animatedProgress by animateFloatAsState(targetValue = progress.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x1AFFFFFF))) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.7f),
                                    color
                                )
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Prazo com ícone de calendário
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = Color(0xFF81928F),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Prazo: $daysLeft dias restantes",
                    fontSize = 12.sp,
                    color = Color(0xFF81928F)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Actions
            AnimatedContent(targetState = showAddFunds, label = "AddFunds") { isAdding ->
                if (isAdding) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = fundsAmount,
                            onValueChange = { fundsAmount = it },
                            label = { Text("Valor (R$)", color = Color(0xFF5E6D6A), fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = color, unfocusedBorderColor = Color(0xFF3D4947),
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = { showAddFunds = false }, modifier = Modifier.background(Color(0x1AFFFFFF), CircleShape)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Cancelar", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { 
                            val added = fundsAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
                            if (added > 0) onAddFunds(added)
                            showAddFunds = false
                            fundsAmount = ""
                        }, modifier = Modifier.background(color, CircleShape)) {
                            Icon(Icons.Outlined.Check, contentDescription = "Confirmar", tint = Color(0xFF070909))
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0AFFFFFF))
                            .clickable { showAddFunds = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Adicionar Saldo à Meta", color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseGoalDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, target: Double, current: Double, url: String, deadline: Long, color: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141918),
        title = { Text("Nova Meta de Compra", color = Color(0xFFDFE3E2)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("O que você deseja comprar?", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true
                )
                OutlinedTextField(
                    value = target, onValueChange = { target = it }, label = { Text("Valor Alvo (R$)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true
                )
                OutlinedTextField(
                    value = url, onValueChange = { url = it }, label = { Text("URL da Imagem (Opcional)", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = target.replace(",", ".").toDoubleOrNull() ?: 0.0
                val defaultUrl = if (url.isBlank()) "https://images.unsplash.com/photo-1555626906-fcf10d6851b4?q=80&w=800&auto=format&fit=crop" else url
                onSave(title, t, 0.0, defaultUrl, System.currentTimeMillis() + 86400000L * 30, "#F9A826")
            }) {
                Text("Salvar", color = Color(0xFFF9A826))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF81928F)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("WaterDrop") }
    var selectedColor by remember { mutableStateOf("#71D7CD") }

    val icons = listOf(
        "WaterDrop" to Icons.Outlined.WaterDrop,
        "MenuBook" to Icons.Outlined.MenuBook,
        "SelfImprovement" to Icons.Outlined.SelfImprovement,
        "TaskAlt" to Icons.Outlined.TaskAlt
    )
    val colors = listOf("#71D7CD", "#F9A826", "#D7B4F3", "#FF6B6B", "#4D96FF")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141918),
        title = { Text("Novo Hábito / Ritual", color = Color(0xFFDFE3E2)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Nome do Hábito", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true
                )
                
                // Icon Selector
                Column {
                    Text("Selecione um Ícone", color = Color(0xFF81928F), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        icons.forEach { (iconName, iconVector) ->
                            val isSelected = selectedIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0x33FFFFFF) else Color(0x0AFFFFFF))
                                    .border(1.dp, if (isSelected) Color(0xFF71D7CD) else Color.Transparent, CircleShape)
                                    .clickable { selectedIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconVector, contentDescription = null, tint = if (isSelected) Color(0xFF71D7CD) else Color(0xFF81928F))
                            }
                        }
                    }
                }

                // Color Selector
                Column {
                    Text("Selecione uma Cor", color = Color(0xFF81928F), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        colors.forEach { hex ->
                            val isSelected = selectedColor == hex
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(2.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name, selectedIcon, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("Salvar", color = Color(0xFF71D7CD))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF81928F)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onDelete: (Habit) -> Unit
) {
    var name by remember { mutableStateOf(habit.name) }
    var selectedIcon by remember { mutableStateOf(habit.iconName) }
    var selectedColor by remember { mutableStateOf(habit.colorHex) }

    val icons = listOf(
        "WaterDrop" to Icons.Outlined.WaterDrop,
        "MenuBook" to Icons.Outlined.MenuBook,
        "SelfImprovement" to Icons.Outlined.SelfImprovement,
        "TaskAlt" to Icons.Outlined.TaskAlt
    )
    val colors = listOf("#71D7CD", "#F9A826", "#D7B4F3", "#FF6B6B", "#4D96FF")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141918),
        title = { Text("Editar Hábito", color = Color(0xFFDFE3E2)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Nome do Hábito", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true
                )
                
                // Icon Selector
                Column {
                    Text("Selecione um Ícone", color = Color(0xFF81928F), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        icons.forEach { (iconName, iconVector) ->
                            val isSelected = selectedIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0x33FFFFFF) else Color(0x0AFFFFFF))
                                    .border(1.dp, if (isSelected) Color(0xFF71D7CD) else Color.Transparent, CircleShape)
                                    .clickable { selectedIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(iconVector, contentDescription = null, tint = if (isSelected) Color(0xFF71D7CD) else Color(0xFF81928F))
                            }
                        }
                    }
                }

                // Color Selector
                Column {
                    Text("Selecione uma Cor", color = Color(0xFF81928F), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        colors.forEach { hex ->
                            val isSelected = selectedColor == hex
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(2.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onDelete(habit) }) {
                    Text("Excluir", color = Color(0xFFFF5252))
                }
                TextButton(
                    onClick = { if (name.isNotBlank()) onSave(habit.copy(name = name, iconName = selectedIcon, colorHex = selectedColor)) },
                    enabled = name.isNotBlank()
                ) {
                    Text("Salvar", color = Color(0xFF71D7CD))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF81928F)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPurchaseGoalDialog(
    goal: PurchaseGoal,
    onDismiss: () -> Unit,
    onSave: (PurchaseGoal) -> Unit,
    onDelete: (PurchaseGoal) -> Unit
) {
    var title by remember { mutableStateOf(goal.title) }
    var target by remember { mutableStateOf(goal.targetValue.toString()) }
    var current by remember { mutableStateOf(goal.currentValue.toString()) }
    var url by remember { mutableStateOf(goal.imageUrl) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141918),
        title = { Text("Editar Meta de Compra", color = Color(0xFFDFE3E2)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("O que você deseja comprar?", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true
                )
                OutlinedTextField(
                    value = target, onValueChange = { target = it }, label = { Text("Valor Alvo (R$)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true
                )
                OutlinedTextField(
                    value = current, onValueChange = { current = it }, label = { Text("Valor Atual Salvo (R$)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true
                )
                OutlinedTextField(
                    value = url, onValueChange = { url = it }, label = { Text("URL da Imagem", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onDelete(goal) }) {
                    Text("Excluir", color = Color(0xFFFF5252))
                }
                TextButton(onClick = {
                    val t = target.replace(",", ".").toDoubleOrNull() ?: goal.targetValue
                    val c = current.replace(",", ".").toDoubleOrNull() ?: goal.currentValue
                    onSave(goal.copy(title = title, targetValue = t, currentValue = c, imageUrl = url))
                }) {
                    Text("Salvar", color = Color(0xFFF9A826))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF81928F)) }
        }
    )
}
