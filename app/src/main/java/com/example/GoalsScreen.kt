package com.example

import androidx.compose.ui.text.TextStyle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
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

    var showAddHabitDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }

    var selectedTab by remember(viewModel.selectedGoalsTab) { mutableStateOf(viewModel.selectedGoalsTab) }

    val rituaisLazyListState = rememberLazyListState()
    val chronosLazyListState = rememberLazyListState()
    val pomodoroScrollState = rememberScrollState()

    val isCompact by remember(selectedTab) {
        derivedStateOf {
            when (selectedTab) {
                1 -> chronosLazyListState.firstVisibleItemIndex > 0 || chronosLazyListState.firstVisibleItemScrollOffset > 100
                2 -> pomodoroScrollState.value > 100
                else -> rituaisLazyListState.firstVisibleItemIndex > 0 || rituaisLazyListState.firstVisibleItemScrollOffset > 100
            }
        }
    }

    val normalAlpha by animateFloatAsState(targetValue = if (isCompact) 0f else 1f, animationSpec = tween(250), label = "normalAlpha")
    val compactAlpha by animateFloatAsState(targetValue = if (isCompact) 1f else 0f, animationSpec = tween(250), label = "compactAlpha")

    val accentColor = when (selectedTab) {
        1 -> Color(0xFF71D7CD)
        2 -> Color(0xFFD7B4F3)
        else -> Color(0xFFF9A826)
    }

    val titleText = when (selectedTab) {
        1 -> "Chronos"
        2 -> "Focus"
        else -> "Foco & Rotinas"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFF070909), // Oura deep black
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {}
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(modifier = Modifier.height(72.dp))

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
                        1 -> ChronosScreen(viewModel = viewModel, listState = chronosLazyListState)
                        2 -> PomodoroScreen(scrollState = pomodoroScrollState)
                        else -> {
                            LazyColumn(
                                state = rituaisLazyListState,
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
                            }
                        }
                    }
                }
            }
        }

        // Floating overlay top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // 1. Barra Normal
            if (normalAlpha > 0.05f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = normalAlpha
                            scaleX = 0.92f + (normalAlpha * 0.08f)
                            scaleY = 0.92f + (normalAlpha * 0.08f)
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onHomeClick, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = "Home",
                                tint = Color(0xFFBDC9C6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = titleText.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedTab == 0) {
                            IconButton(onClick = { showAddHabitDialog = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.Add, contentDescription = "Adicionar", tint = Color(0xFFBDC9C6), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
            
            // 2. Barra Compacta
            if (compactAlpha > 0.05f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = compactAlpha
                            translationY = (1f - compactAlpha) * (-20f)
                        }
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            1 -> Icons.Outlined.HourglassEmpty
                            2 -> Icons.Outlined.Timer
                            else -> Icons.Outlined.Spa
                        },
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                    val shimmerOffset by infiniteTransition.animateFloat(
                        initialValue = -400f,
                        targetValue = 400f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "shimmerOffset"
                    )
                    
                    val nameGlowBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            accentColor,
                            Color.White,
                            accentColor,
                            Color.White
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 150f, 150f)
                    )
                    
                    Text(
                        text = titleText.uppercase(),
                        style = TextStyle(
                            brush = nameGlowBrush,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Serif
                        )
                    )
                }
            }
        }
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


