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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.BankAccount
import com.example.data.CreditCard
import com.example.data.PurchaseGoal
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishesScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val purchaseGoals by viewModel.allPurchaseGoals.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<PurchaseGoal?>(null) }

    val activeGoals = remember(purchaseGoals) { purchaseGoals.filter { !it.isBought } }
    val boughtGoals = remember(purchaseGoals) { purchaseGoals.filter { it.isBought } }

    Scaffold(
        containerColor = Color(0xFF070909), // Oura deep black
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Desejos",
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
                    IconButton(onClick = { showAddGoalDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Adicionar Desejo",
                            tint = Color(0xFFBDC9C6)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
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

            // Seção: Ativos
            item {
                SectionHeaderWishes("DESEJOS ATIVOS", Icons.Outlined.StarBorder, Color(0xFFF9A826))
            }

            if (activeGoals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum desejo ativo cadastrado.\nToque no '+' para planejar sua próxima conquista!",
                            color = Color(0xFF5E6D6A),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                items(activeGoals, key = { "active_${it.id}" }) { goal ->
                    PurchaseGoalPremiumCard(
                        goal = goal,
                        bankAccounts = bankAccounts,
                        creditCards = creditCards,
                        onAddFunds = { amount, origin ->
                            viewModel.addFundsToPurchaseGoal(goal, amount, origin)
                        },
                        onBuy = { origin ->
                            viewModel.buyPurchaseGoal(goal, origin)
                        },
                        onEditClick = { goalToEdit = goal }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Seção: Realizados
            if (boughtGoals.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item {
                    SectionHeaderWishes("DESEJOS REALIZADOS", Icons.Outlined.CheckCircle, Color(0xFF71D7CD))
                }

                items(boughtGoals, key = { "bought_${it.id}" }) { goal ->
                    BoughtGoalCard(goal = goal, onDelete = { viewModel.deletePurchaseGoal(goal) })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showAddGoalDialog) {
        AddPurchaseGoalDialogWishes(
            onDismiss = { showAddGoalDialog = false },
            onSave = { title, target, current, url, deadline, color, priorityOrder, priorityClassification ->
                viewModel.addPurchaseGoal(title, target, current, url, deadline, color, priorityOrder, priorityClassification)
                showAddGoalDialog = false
            }
        )
    }

    if (goalToEdit != null) {
        EditPurchaseGoalDialogWishes(
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
fun SectionHeaderWishes(title: String, icon: ImageVector, tintColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = tintColor
        )
    }
}

@Composable
fun PurchaseGoalPremiumCard(
    goal: PurchaseGoal,
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    onAddFunds: (Double, String) -> Unit,
    onBuy: (String) -> Unit,
    onEditClick: () -> Unit
) {
    val progress = (goal.currentValue / goal.targetValue).coerceIn(0.0, 1.0)
    val color = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Color(0xFFF9A826) }
    val daysLeft = ((goal.deadlineTimestamp - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)

    var showAddFundsSection by remember { mutableStateOf(false) }
    var showBuySection by remember { mutableStateOf(false) }
    var fundsAmount by remember { mutableStateOf("") }

    val origins = remember(bankAccounts, creditCards) {
        bankAccounts.map { it.name } + creditCards.map { it.name }
    }
    var selectedOrigin by remember { mutableStateOf(origins.firstOrNull() ?: "") }
    var showOriginDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box {
            // Imagem do Produto
            AsyncImage(
                model = goal.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            // Gradiente Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF070909))))
            )

            // Editar
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(36.dp)
                    .background(Color(0x66000000), CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Editar Meta",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Badge Porcentagem
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x66000000))
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
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
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

        // Progresso e Detalhes
        Column(modifier = Modifier.padding(20.dp)) {
            val formattedCurrent = String.format(Locale("pt", "BR"), "R$ %,.2f", goal.currentValue)
            val formattedTarget = String.format(Locale("pt", "BR"), "R$ %,.2f", goal.targetValue)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(formattedCurrent, fontSize = 22.sp, color = Color(0xFFDFE3E2), fontWeight = FontWeight.Bold)
                Text("de $formattedTarget", fontSize = 14.sp, color = Color(0xFF81928F))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de Progresso Animada
            val animatedProgress by animateFloatAsState(
                targetValue = progress.toFloat(),
                animationSpec = tween(1500, easing = FastOutSlowInEasing),
                label = "WishProgress"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x1AFFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(color.copy(alpha = 0.7f), color)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prazo & Prioridade
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF81928F),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Prazo: $daysLeft dias",
                        fontSize = 12.sp,
                        color = Color(0xFF81928F)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeColor = when (goal.priorityClassification) {
                        "Urgente" -> Color(0xFFEF4444)
                        "Moderado" -> Color(0xFFF9A826)
                        else -> Color(0xFF71D7CD)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = goal.priorityClassification.uppercase(),
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fluxo de Integração Financeira: Aporte de Saldo
            AnimatedContent(
                targetState = showAddFundsSection to showBuySection,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "FinancialWishesIntegration"
            ) { (isAddingFunds, isBuying) ->
                when {
                    isAddingFunds -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
                                .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text("Aportar Financeiro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = fundsAmount,
                                    onValueChange = { fundsAmount = it },
                                    label = { Text("Valor (R$)", color = Color(0xFF81928F)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1.3f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = color,
                                        unfocusedBorderColor = Color(0xFF3D4947),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Origem de débito
                                Box(modifier = Modifier.weight(1.7f)) {
                                    OutlinedTextField(
                                        value = selectedOrigin,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Debitar de", color = Color(0xFF81928F)) },
                                        trailingIcon = {
                                            IconButton(onClick = { showOriginDropdown = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF3D4947),
                                            unfocusedBorderColor = Color(0xFF3D4947),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    
                                    DropdownMenu(
                                        expanded = showOriginDropdown,
                                        onDismissRequest = { showOriginDropdown = false },
                                        modifier = Modifier.background(Color(0xFF131817))
                                    ) {
                                        origins.forEach { origin ->
                                            DropdownMenuItem(
                                                text = { Text(origin, color = Color.White) },
                                                onClick = {
                                                    selectedOrigin = origin
                                                    showOriginDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showAddFundsSection = false }) {
                                    Text("Cancelar", color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val added = fundsAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
                                        if (added > 0.0 && selectedOrigin.isNotEmpty()) {
                                            onAddFunds(added, selectedOrigin)
                                            showAddFundsSection = false
                                            fundsAmount = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = color)
                                ) {
                                    Text("Confirmar Aporte", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    isBuying -> {
                        val remainingValue = (goal.targetValue - goal.currentValue).coerceAtLeast(0.0)
                        val formattedRemaining = String.format(Locale("pt", "BR"), "R$ %,.2f", remainingValue)
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
                                .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text("Realizar Compra", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            if (remainingValue > 0.0) {
                                Text(
                                    "Falta $formattedRemaining para atingir o valor alvo. De qual conta deseja pagar este saldo restante?",
                                    color = Color(0xFFBDC9C6),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedOrigin,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Pagar saldo restante com", color = Color(0xFF81928F)) },
                                        trailingIcon = {
                                            IconButton(onClick = { showOriginDropdown = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF3D4947),
                                            unfocusedBorderColor = Color(0xFF3D4947),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    
                                    DropdownMenu(
                                        expanded = showOriginDropdown,
                                        onDismissRequest = { showOriginDropdown = false },
                                        modifier = Modifier.background(Color(0xFF131817))
                                    ) {
                                        origins.forEach { origin ->
                                            DropdownMenuItem(
                                                text = { Text(origin, color = Color.White) },
                                                onClick = {
                                                    selectedOrigin = origin
                                                    showOriginDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "Desejo totalmente financiado! Confirme a compra para marcar como realizado.",
                                    color = Color(0xFF71D7CD),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showBuySection = false }) {
                                    Text("Cancelar", color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onBuy(selectedOrigin)
                                        showBuySection = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                                ) {
                                    Text("Efetivar Compra 🛍️", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Botão Aportar
                            OutlinedButton(
                                onClick = { showAddFundsSection = true },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, color),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
                            ) {
                                Icon(Icons.Outlined.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aportar", fontWeight = FontWeight.Bold)
                            }

                            // Botão Comprar / Concluir
                            Button(
                                onClick = { showBuySection = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (progress >= 1.0) Color(0xFF71D7CD) else Color(0x33FFFFFF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = if (progress >= 1.0) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (progress >= 1.0) "Comprar!" else "Comprar",
                                    color = if (progress >= 1.0) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
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
fun BoughtGoalCard(goal: PurchaseGoal, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(0.5.dp, Color(0xFF71D7CD).copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = goal.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF71D7CD).copy(alpha = 0.15f))
                            .border(0.5.dp, Color(0xFF71D7CD).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CONQUISTADO 🌟",
                            color = Color(0xFF71D7CD),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %,.2f", goal.targetValue),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Deletar Conquista", tint = Color.Gray)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF141918),
            title = { Text("Excluir Conquista?", color = Color.White) },
            text = { Text("Isso removerá este item de desejo realizado da lista. Esta ação não afetará suas transações financeiras passadas.", color = Color(0xFFBDC9C6)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Remover", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseGoalDialogWishes(
    onDismiss: () -> Unit,
    onSave: (title: String, target: Double, current: Double, url: String, deadline: Long, color: String, priorityOrder: Int, priorityClassification: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var priorityOrder by remember { mutableStateOf("1") }
    var selectedClassification by remember { mutableStateOf("Moderado") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141918),
        title = { Text("Novo Desejo", color = Color(0xFFDFE3E2)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("O que você deseja comprar?", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target, onValueChange = { target = it }, label = { Text("Valor Alvo (R$)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url, onValueChange = { url = it }, label = { Text("URL da Imagem (Opcional)", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priorityOrder, onValueChange = { priorityOrder = it.filter { c -> c.isDigit() } }, label = { Text("Ordem de Prioridade (Numérica)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Classificação de Prioridade", color = Color(0xFF81928F), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Leve", "Moderado", "Urgente").forEach { level ->
                            val isSelected = selectedClassification == level
                            val chipBg = when (level) {
                                "Urgente" -> if (isSelected) Color(0xFFEF4444) else Color(0x1AEF4444)
                                "Moderado" -> if (isSelected) Color(0xFFF9A826) else Color(0x1AF9A826)
                                else -> if (isSelected) Color(0xFF71D7CD) else Color(0x1A71D7CD)
                            }
                            val chipTextColor = if (isSelected) Color.Black else when (level) {
                                "Urgente" -> Color(0xFFEF4444)
                                "Moderado" -> Color(0xFFF9A826)
                                else -> Color(0xFF71D7CD)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(1.dp, if (isSelected) Color.White else chipTextColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { selectedClassification = level }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(level, color = chipTextColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = target.replace(",", ".").toDoubleOrNull() ?: 0.0
                val pOrd = priorityOrder.toIntOrNull() ?: 1
                val defaultUrl = if (url.isBlank()) "https://images.unsplash.com/photo-1555626906-fcf10d6851b4?q=80&w=800&auto=format&fit=crop" else url
                val colorHex = when (selectedClassification) {
                    "Urgente" -> "#EF4444"
                    "Moderado" -> "#F9A826"
                    else -> "#71D7CD"
                }
                onSave(title, t, 0.0, defaultUrl, System.currentTimeMillis() + 86400000L * 30, colorHex, pOrd, selectedClassification)
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
fun EditPurchaseGoalDialogWishes(
    goal: PurchaseGoal,
    onDismiss: () -> Unit,
    onSave: (PurchaseGoal) -> Unit,
    onDelete: (PurchaseGoal) -> Unit
) {
    var title by remember { mutableStateOf(goal.title) }
    var target by remember { mutableStateOf(goal.targetValue.toString()) }
    var current by remember { mutableStateOf(goal.currentValue.toString()) }
    var url by remember { mutableStateOf(goal.imageUrl) }
    var priorityOrder by remember { mutableStateOf(goal.priorityOrder.toString()) }
    var selectedClassification by remember { mutableStateOf(goal.priorityClassification) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141918),
        title = { Text("Editar Desejo", color = Color(0xFFDFE3E2)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("O que você deseja comprar?", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target, onValueChange = { target = it }, label = { Text("Valor Alvo (R$)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = current, onValueChange = { current = it }, label = { Text("Valor Atual Salvo (R$)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url, onValueChange = { url = it }, label = { Text("URL da Imagem", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priorityOrder, onValueChange = { priorityOrder = it.filter { c -> c.isDigit() } }, label = { Text("Ordem de Prioridade (Numérica)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Classificação de Prioridade", color = Color(0xFF81928F), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Leve", "Moderado", "Urgente").forEach { level ->
                            val isSelected = selectedClassification == level
                            val chipBg = when (level) {
                                "Urgente" -> if (isSelected) Color(0xFFEF4444) else Color(0x1AEF4444)
                                "Moderado" -> if (isSelected) Color(0xFFF9A826) else Color(0x1AF9A826)
                                else -> if (isSelected) Color(0xFF71D7CD) else Color(0x1A71D7CD)
                            }
                            val chipTextColor = if (isSelected) Color.Black else when (level) {
                                "Urgente" -> Color(0xFFEF4444)
                                "Moderado" -> Color(0xFFF9A826)
                                else -> Color(0xFF71D7CD)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(1.dp, if (isSelected) Color.White else chipTextColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { selectedClassification = level }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(level, color = chipTextColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
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
                    val pOrd = priorityOrder.toIntOrNull() ?: goal.priorityOrder
                    val colorHex = when (selectedClassification) {
                        "Urgente" -> "#EF4444"
                        "Moderado" -> "#F9A826"
                        else -> "#71D7CD"
                    }
                    onSave(goal.copy(title = title, targetValue = t, currentValue = c, imageUrl = url, priorityOrder = pOrd, priorityClassification = selectedClassification, colorHex = colorHex))
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
