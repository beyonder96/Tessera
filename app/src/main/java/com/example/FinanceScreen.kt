package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import androidx.compose.foundation.shape.CircleShape

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.TesseraViewModel
import com.example.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    
    val totalIncome = allTransactions.filter { it.isIncome }.sumOf { it.value }
    val totalExpense = allTransactions.filter { !it.isIncome }.sumOf { it.value }
    val balance = totalIncome - totalExpense
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, subtitle, value, isIncome, category ->
                viewModel.addTransaction(title, subtitle, value, isIncome, category)
                showAddDialog = false
            }
        )
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF71D7CD),
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar transação")
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Patrimônio",
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                // Use a proper locale for currency later, just formatting nicely
                text = String.format("R$ %,.2f", balance),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                color = Color(0xFFDFE3E2)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            EvolutionSection()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AccountsSection(totalIncome = totalIncome, totalExpense = totalExpense)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            RecentTransactionsSection(transactions = allTransactions)
            
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun EvolutionSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Evolução",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    color = Color(0xFFDFE3E2)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Últimos 6 meses",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFBDC9C6)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Selecionar período",
                        tint = Color(0xFFBDC9C6),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Grid Lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        Divider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                    }
                }
                
                // Bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val barHeights = listOf(0.4f, 0.5f, 0.45f, 0.6f, 0.75f, 0.85f)
                    val barColors = listOf(
                        Color(0x3371D7CD),
                        Color(0x3371D7CD),
                        Color(0x3371D7CD),
                        Color(0x3371D7CD),
                        Color(0x3371D7CD),
                        Color(0x8071D7CD)
                    )
                    
                    barHeights.forEachIndexed { index, height ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(height)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColors[index])
                        )
                    }
                }
                
                // "R$45k" label for the last bar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = (180 * 0.85).dp + 8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1B2120), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "R$45k",
                            color = Color(0xFF71D7CD),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun").forEach { month ->
                    Text(
                        text = month,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0x99BDC9C6),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountsSection(totalIncome: Double, totalExpense: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Resumo",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = Color(0xFFDFE3E2),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        AccountItem(
            icon = Icons.Outlined.AccountBalanceWallet,
            title = "Entradas",
            subtitle = "Soma de todos os ganhos",
            value = String.format("R$ %,.2f", totalIncome),
            iconColor = Color(0xFF71D7CD),
            iconBgColor = Color(0xFF262B2A)
        )
        
        AccountItem(
            icon = Icons.Outlined.Payment,
            title = "Saídas",
            subtitle = "Soma de todos os gastos",
            value = String.format("R$ %,.2f", totalExpense),
            iconColor = Color(0xFFF9A8A8),
            iconBgColor = Color(0xFF262B2A)
        )
    }
}

@Composable
fun AccountItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: String,
    iconColor: Color,
    iconBgColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        color = Color(0xFFDFE3E2)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFBDC9C6)
                    )
                }
            }
            
            Text(
                text = value,
                fontSize = 16.sp,
                color = Color(0xFFDFE3E2)
            )
        }
    }
}

@Composable
fun RecentTransactionsSection(transactions: List<Transaction>) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transações Recentes",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                color = Color(0xFFDFE3E2)
            )
            
            Text(
                text = "Ver todas",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF71D7CD)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x08FFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
        ) {
            Column {
                if (transactions.isEmpty()) {
                    Text("Sem transações", color = Color(0xFFBDC9C6), modifier = Modifier.padding(16.dp))
                }
                
                val dateFormat = SimpleDateFormat("dd/MM, HH:mm", Locale.getDefault())
                transactions.forEachIndexed { index, transaction ->
                    val icon = when (transaction.category) {
                        "Food" -> Icons.Outlined.Restaurant
                        "Market" -> Icons.Outlined.ShoppingCart
                        "Salary" -> Icons.Outlined.Payments
                        else -> Icons.Outlined.AttachMoney
                    }
                    val isIncome = transaction.isIncome
                    val dateFormatted = dateFormat.format(Date(transaction.timestamp))
                    val valueFormatted = String.format("R$ %,.2f", transaction.value)
                    
                    TransactionItem(
                        icon = icon,
                        title = transaction.title,
                        subtitle = dateFormatted,
                        value = if (isIncome) "+ $valueFormatted" else "- $valueFormatted",
                        iconColor = if (isIncome) Color(0xFF71D7CD) else Color(0xFFBDC9C6),
                        iconBgColor = if (isIncome) Color(0x1A71D7CD) else Color(0xFF262B2A),
                        valueColor = if (isIncome) Color(0xFF71D7CD) else Color(0xFFDFE3E2)
                    )
                    
                    if (index < transactions.size - 1) {
                        HorizontalDivider(color = Color(0x1A879391))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: String,
    iconColor: Color,
    iconBgColor: Color,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = Color(0xFFDFE3E2)
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFBDC9C6)
                )
            }
        }
        
        Text(
            text = value,
            fontSize = 16.sp,
            color = valueColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Boolean, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var valueStr by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) } // Default to SAÍDA (false)
    var dateStr by remember { mutableStateOf("30/05/2026") }
    var selectedCategory by remember { mutableStateOf("GERAL") }
    var destinationAccount by remember { mutableStateOf("") }
    var transactionConfig by remember { mutableStateOf("TRANSAÇÃO ÚNICA") }
    
    val categories = listOf(
        CategoryItem("GERAL", "📌"),
        CategoryItem("SUPERMERCADO", "🛒"),
        CategoryItem("RESERVA", "🏦"),
        CategoryItem("MORADIA", "🏠"),
        CategoryItem("ALIMENTAÇÃO", "🍔"),
        CategoryItem("SAÚDE", "🩺"),
        CategoryItem("LAZER", "🎉"),
        CategoryItem("TRANSPORTE", "🚗"),
        CategoryItem("EDUCAÇÃO", "📚"),
        CategoryItem("ASSINATURAS", "📺")
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF070909)) // Sleek dark slate
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                // Header (Nova Transação)
                Text(
                    text = "NOVA TRANSAÇÃO",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // DESCRIÇÃO
                Text(
                    text = "DESCRIÇÃO",
                    color = Color(0xFF879391),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Nome da transação", color = Color(0xFF55605E), fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF131817),
                        unfocusedContainerColor = Color(0xFF131817),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // VALOR & DATA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VALOR (R$)",
                            color = Color(0xFF879391),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextField(
                            value = valueStr,
                            onValueChange = { valueStr = it },
                            placeholder = { Text("0,00", color = Color(0xFF55605E), fontSize = 14.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF131817),
                                unfocusedContainerColor = Color(0xFF131817),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DATA",
                            color = Color(0xFF879391),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextField(
                            value = dateStr,
                            onValueChange = { dateStr = it },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF131817),
                                unfocusedContainerColor = Color(0xFF131817),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = Color(0xFF879391),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // TOGGLE ENTRADA / SAÍDA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ENTRADA
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isIncome) Color(0xFF2ECC71) else Color(0xFF131817))
                            .clickable { isIncome = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ENTRADA",
                            color = if (isIncome) Color.White else Color(0xFF879391),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    // SAÍDA
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isIncome) Color(0xFFFF2D55) else Color(0xFF131817))
                            .clickable { isIncome = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SAÍDA",
                            color = if (!isIncome) Color.White else Color(0xFF879391),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // CATEGORIA
                Text(
                    text = "CATEGORIA",
                    color = Color(0xFF879391),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                // Scrollable category row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat.name
                        Box(
                            modifier = Modifier
                                .size(width = 86.dp, height = 64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131817))
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCategory = cat.name },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(cat.emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.name,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF879391)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // CONTA-DESTINO & CONFIGURAÇÃO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONTA-DESTINO",
                            color = Color(0xFF879391),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131817))
                                .clickable { /* dropdown */ }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (destinationAccount.isEmpty()) "Selecione..." else destinationAccount,
                                    color = if (destinationAccount.isEmpty()) Color(0xFF55605E) else Color.White,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF879391),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONFIGURAÇÃO",
                            color = Color(0xFF879391),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131817))
                                .clickable { /* config */ }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = transactionConfig,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // BOTTOM BUTTONS (CANCELAR / CONFIRMAR LANÇAMENTO)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // CANCELAR
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.4f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Text(
                            text = "CANCELAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    
                    // CONFIRMAR LANÇAMENTO
                    Button(
                        onClick = {
                            val v = valueStr.replace(',', '.').toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && v > 0) {
                                onAdd(title, selectedCategory, v, isIncome, selectedCategory)
                            }
                        },
                        modifier = Modifier
                            .weight(0.6f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text(
                            text = "CONFIRMAR LANÇAMENTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

data class CategoryItem(val name: String, val emoji: String)

