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
import androidx.compose.ui.draw.clip
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
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val purchaseGoals by viewModel.allPurchaseGoals.collectAsStateWithLifecycle()

    var showAddPurchaseGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF070909), // Oura deep black
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Metas & Hábitos",
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
                    IconButton(onClick = { showAddPurchaseGoalDialog = true }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add Goal", tint = Color(0xFFBDC9C6))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                items(habits, key = { it.id }) { habit ->
                    HabitCard(habit = habit, onToggle = { viewModel.toggleHabitCompleted(habit) })
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
                items(purchaseGoals, key = { it.id }) { goal ->
                    PurchaseGoalCard(
                        goal = goal, 
                        onAddFunds = { amount -> viewModel.updatePurchaseGoalProgress(goal, amount) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
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
fun HabitCard(habit: Habit, onToggle: () -> Unit) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF070909), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun PurchaseGoalCard(goal: PurchaseGoal, onAddFunds: (Double) -> Unit) {
    val progress = if (goal.targetValue > 0) (goal.currentValue / goal.targetValue).toFloat() else 0f
    val color = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Color(0xFFF9A826) }
    
    var showAddFunds by remember { mutableStateOf(false) }
    var fundsAmount by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
    ) {
        // Image Header
        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            AsyncImage(
                model = goal.imageUrl,
                contentDescription = goal.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color(0x33000000), Color(0xCC070909))
                )
            ))
            
            // Top Right Percentage
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
            val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1500, easing = FastOutSlowInEasing))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color(0x1AFFFFFF))) {
                Box(modifier = Modifier.fillMaxWidth(animatedProgress).height(6.dp).clip(CircleShape).background(color))
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
