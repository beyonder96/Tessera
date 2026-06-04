package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BankAccount
import com.example.data.CreditCard
import com.example.data.Transaction
import com.example.ui.components.PremiumGlassModifier
import com.example.viewmodel.TesseraViewModel
import java.util.*

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF71D7CD) // Fallback neon teal
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showManageDialog by remember { mutableStateOf(false) }
    var showAdjustBalanceDialog by remember { mutableStateOf(false) }
    
    var isPrivacyModeEnabled by remember { mutableStateOf(true) }
    var isCardsExpanded by remember { mutableStateOf(true) }
    var isAccountsExpanded by remember { mutableStateOf(true) }
    
    var selectedFilterName by remember { mutableStateOf<String?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    // Group accounts balances by type
    val checkingBalance = bankAccounts.filter { it.type == "Corrente" }.sumOf { it.balance }
    val savingsBalance = bankAccounts.filter { it.type == "Poupança" }.sumOf { it.balance }
    val investmentBalance = bankAccounts.filter { it.type == "Investimento" }.sumOf { it.balance }

    // Computed transaction filtering
    val filteredTransactions = remember(allTransactions, selectedFilterName) {
        if (selectedFilterName == null) {
            allTransactions
        } else {
            allTransactions.filter { it.accountOrCardName == selectedFilterName }
        }
    }

    // Calculations based on the active filter
    val totalIncome = filteredTransactions.filter { it.isIncome }.sumOf { it.value }
    val totalExpense = filteredTransactions.filter { !it.isIncome }.sumOf { it.value }
    val balance = totalIncome - totalExpense

    val rawScore = if (totalIncome > 0) {
        ((totalIncome - totalExpense) / totalIncome) * 100
    } else if (totalExpense == 0.0 && totalIncome == 0.0) {
        0.0
    } else {
        0.0 
    }
    val score = rawScore.coerceIn(0.0, 100.0).toInt()

    if (showAddDialog) {
        AddTransactionDialog(
            bankAccounts = bankAccounts,
            creditCards = creditCards,
            editingTransaction = editingTransaction,
            onDismiss = { 
                showAddDialog = false
                editingTransaction = null
            },
            onAdd = { title, value, isIncome, category, origin ->
                viewModel.addTransaction(title, "", value, isIncome, category, origin)
                showAddDialog = false
                editingTransaction = null
            },
            onUpdate = { oldTx, newTx ->
                viewModel.updateTransaction(oldTx, newTx)
                showAddDialog = false
                editingTransaction = null
            },
            onDelete = { tx ->
                viewModel.deleteTransaction(tx)
                showAddDialog = false
                editingTransaction = null
            }
        )
    }

    if (showManageDialog) {
        ManageAccountsAndCardsDialog(
            bankAccounts = bankAccounts,
            creditCards = creditCards,
            viewModel = viewModel,
            onDismiss = { showManageDialog = false }
        )
    }

    if (showAdjustBalanceDialog) {
        AdjustBalancesDialog(
            bankAccounts = bankAccounts,
            viewModel = viewModel,
            onDismiss = { showAdjustBalanceDialog = false }
        )
    }

    Scaffold(
        containerColor = Color(0xFF070909),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Patrimônio",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
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
                    IconButton(onClick = { isPrivacyModeEnabled = !isPrivacyModeEnabled }) {
                        Icon(
                            imageVector = if (isPrivacyModeEnabled) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Modo Privacidade",
                            tint = Color(0xFFBDC9C6)
                        )
                    }
                    IconButton(onClick = { 
                        editingTransaction = null
                        showAddDialog = true 
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar transação",
                            tint = Color(0xFF71D7CD)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF070909).copy(alpha = 0.85f),
                    scrolledContainerColor = Color(0xFF070909).copy(alpha = 0.95f),
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

            // 1. Balance Header with reactive filtered states & category breakdown
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdjustBalanceDialog = true }
            ) {
                BalanceHeaderSection(
                    balance = balance,
                    selectedFilterName = selectedFilterName,
                    bankAccounts = bankAccounts,
                    creditCards = creditCards,
                    checkingBalance = checkingBalance,
                    savingsBalance = savingsBalance,
                    investmentBalance = investmentBalance,
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    onClearFilter = { selectedFilterName = null }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Holographic Credit Cards
            SectionHeaderWithAction(
                title = "Seus Cartões",
                isExpanded = isCardsExpanded,
                onToggleExpand = { isCardsExpanded = !isCardsExpanded },
                onAddClick = { showManageDialog = true }
            )
            CreditCardsCarousel(
                creditCards = creditCards,
                selectedFilterName = selectedFilterName,
                isExpanded = isCardsExpanded,
                onCardClick = { cardName ->
                    selectedFilterName = if (selectedFilterName == cardName) null else cardName
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3. Liquid Glass Bank Accounts (Adjusted size to prevent cutoffs)
            SectionHeaderWithAction(
                title = "Suas Contas",
                isExpanded = isAccountsExpanded,
                onToggleExpand = { isAccountsExpanded = !isAccountsExpanded },
                onAddClick = { showManageDialog = true }
            )
            BankAccountsSection(
                bankAccounts = bankAccounts,
                selectedFilterName = selectedFilterName,
                isExpanded = isAccountsExpanded,
                onAccountClick = { accountName ->
                    selectedFilterName = if (selectedFilterName == accountName) null else accountName
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. Analytics & Core Dashboards (Reactive to filtered lists)
            FinancialScoreRing(score = score, income = totalIncome, expense = totalExpense)
            Spacer(modifier = Modifier.height(24.dp))
            SmoothEvolutionChart(transactions = filteredTransactions)
            Spacer(modifier = Modifier.height(24.dp))
            CategoryBreakdown(transactions = filteredTransactions)
            Spacer(modifier = Modifier.height(24.dp))
            RecentTransactionsSection(
                transactions = filteredTransactions, 
                bankAccounts = bankAccounts, 
                creditCards = creditCards,
                onTransactionClick = { transaction ->
                    editingTransaction = transaction
                    showAddDialog = true
                }
            )
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun BalanceHeaderSection(
    balance: Double,
    selectedFilterName: String?,
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    checkingBalance: Double,
    savingsBalance: Double,
    investmentBalance: Double,
    isPrivacyModeEnabled: Boolean,
    onClearFilter: () -> Unit
) {
    val activeCard = creditCards.find { it.name == selectedFilterName }
    val activeAccount = bankAccounts.find { it.name == selectedFilterName }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    activeCard != null -> "LIMITE UTILIZADO • ${activeCard.name.uppercase()}"
                    activeAccount != null -> "SALDO DISPONÍVEL • ${activeAccount.name.uppercase()}"
                    else -> "SALDO PATRIMONIAL GLOBAL"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    activeCard != null -> parseHexColor(activeCard.colorHex)
                    activeAccount != null -> parseHexColor(activeAccount.colorHex)
                    else -> Color(0xFF71D7CD)
                },
                letterSpacing = 1.5.sp
            )

            if (selectedFilterName != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1AFFFFFF))
                        .clickable { onClearFilter() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Limpar Filtro", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0x66FFFFFF), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajustar", fontSize = 11.sp, color = Color(0x66FFFFFF), fontWeight = FontWeight.Medium)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        val displayValue = when {
            activeCard != null -> activeCard.usedLimit
            activeAccount != null -> activeAccount.balance
            else -> checkingBalance + savingsBalance + investmentBalance
        }

        Text(
            text = String.format(Locale("pt", "BR"), "R$ %,.2f", displayValue),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 42.sp,
            color = Color(0xFFDFE3E2),
            modifier = Modifier.blur(if (isPrivacyModeEnabled) 16.dp else 0.dp)
        )

        if (activeCard != null) {
            Text(
                text = String.format(Locale("pt", "BR"), "Limite disponível: R$ %,.2f", activeCard.limit - activeCard.usedLimit),
                fontSize = 13.sp,
                color = Color(0xFF99A5A3)
            )
        } else if (activeAccount != null) {
            Text(
                text = "Conta tipo ${activeAccount.type}",
                fontSize = 13.sp,
                color = Color(0xFF99A5A3)
            )
        } else {
            // Three Glass pills for Corrente, Poupança, Investimentos breakdown
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Corrente
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0CFFFFFF))
                        .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("Corrente", fontSize = 9.sp, color = Color(0x99BDC9C6), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %,.2f", checkingBalance), 
                            fontSize = 11.sp, 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.blur(if (isPrivacyModeEnabled) 8.dp else 0.dp)
                        )
                    }
                }
                // Poupança
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0CFFFFFF))
                        .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("Poupança", fontSize = 9.sp, color = Color(0x99BDC9C6), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %,.2f", savingsBalance), 
                            fontSize = 11.sp, 
                            color = Color(0xFF71D7CD), 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.blur(if (isPrivacyModeEnabled) 8.dp else 0.dp)
                        )
                    }
                }
                // Investimento
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0CFFFFFF))
                        .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("Investimento", fontSize = 9.sp, color = Color(0x99BDC9C6), fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale("pt", "BR"), "R$ %,.2f", investmentBalance), 
                            fontSize = 11.sp, 
                            color = Color(0xFFEAB308), 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.blur(if (isPrivacyModeEnabled) 8.dp else 0.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeaderWithAction(
    title: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onToggleExpand() }
        ) {
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Recolher" else "Expandir",
                tint = Color(0xFFBDC9C6),
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Gerenciar",
                tint = Color(0xFF71D7CD)
            )
        }
    }
}

@Composable
fun CreditCardsCarousel(
    creditCards: List<CreditCard>,
    selectedFilterName: String?,
    isExpanded: Boolean,
    onCardClick: (String) -> Unit
) {
    if (creditCards.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .then(PremiumGlassModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nenhum cartão cadastrado. Toque no '+' para gerenciar.",
                color = Color(0x80BDC9C6),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 13.sp
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            creditCards.forEach { card ->
                val isSelected = selectedFilterName == card.name
                val cardColor = parseHexColor(card.colorHex)
                
                val animatedWidth by animateDpAsState(
                    targetValue = if (isExpanded) 280.dp else 50.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "cardWidth"
                )
                
                Box(
                    modifier = Modifier
                        .width(animatedWidth)
                        .height(160.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    cardColor.copy(alpha = 0.35f),
                                    Color(0x14000000),
                                    Color(0x33000000)
                                )
                            )
                        )
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            brush = Brush.linearGradient(
                                colors = if (isSelected) {
                                    listOf(cardColor, Color.White, cardColor)
                                } else {
                                    listOf(Color.White.copy(alpha = 0.25f), Color(0x05FFFFFF))
                                }
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .clickable { onCardClick(card.name) }
                        .padding(if (animatedWidth < 120.dp) 8.dp else 20.dp)
                ) {
                    if (animatedWidth < 120.dp) {
                        // Collapsed vertical layout
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(cardColor, CircleShape)
                            )
                            
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = card.name.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.graphicsLayer {
                                        rotationZ = -90f
                                    }
                                )
                            }
                            
                            Text(
                                text = card.numberLastFour,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDFE3E2)
                            )
                        }
                    } else {
                        // Original full card content
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = card.name.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    letterSpacing = 1.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row {
                                    Box(modifier = Modifier.size(16.dp).background(cardColor.copy(alpha = 0.8f), CircleShape))
                                    Spacer(modifier = Modifier.width(-6.dp))
                                    Box(modifier = Modifier.size(16.dp).background(Color.White.copy(alpha = 0.3f), CircleShape))
                                }
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp, 22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFFE5C158), Color(0xFFC5A138))
                                            )
                                        )
                                        .border(0.5.dp, Color(0x33000000), RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "••••  ••••  ••••  ${card.numberLastFour}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDFE3E2),
                                    letterSpacing = 1.5.sp
                                )
                            }
                            
                            Column {
                                val ratio = if (card.limit > 0) (card.usedLimit / card.limit).toFloat().coerceIn(0f, 1f) else 0f
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = card.holderName,
                                        fontSize = 11.sp,
                                        color = Color(0x99BDC9C6),
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = String.format(Locale("pt", "BR"), "Disp: R$ %,.2f", card.limit - card.usedLimit),
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0x1AFFFFFF), CircleShape)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio)
                                            .fillMaxHeight()
                                            .background(cardColor, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BankAccountsSection(
    bankAccounts: List<BankAccount>,
    selectedFilterName: String?,
    isExpanded: Boolean,
    onAccountClick: (String) -> Unit
) {
    if (bankAccounts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .then(PremiumGlassModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nenhuma conta cadastrada. Toque no '+' para gerenciar.",
                color = Color(0x80BDC9C6),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 13.sp
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            bankAccounts.forEach { account ->
                val isSelected = selectedFilterName == account.name
                val accountColor = parseHexColor(account.colorHex)
                
                val animatedWidth by animateDpAsState(
                    targetValue = if (isExpanded) 195.dp else 50.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "accountWidth"
                )
                
                Box(
                    modifier = Modifier
                        .width(animatedWidth)
                        .height(90.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x14FFFFFF))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            brush = if (isSelected) SolidColor(accountColor) else SolidColor(Color(0x1AFFFFFF)),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onAccountClick(account.name) }
                        .padding(if (animatedWidth < 120.dp) 6.dp else 12.dp)
                ) {
                    if (animatedWidth < 120.dp) {
                        // Collapsed vertical layout
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(accountColor, CircleShape)
                            )
                            
                            val displayName = if (account.name.length >= 3) account.name.substring(0, 3) else account.name
                            Text(
                                text = displayName.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            
                            val typeInitial = when (account.type) {
                                "Corrente" -> "C"
                                "Poupança" -> "P"
                                "Investimento" -> "I"
                                else -> account.type.take(1)
                            }
                            Text(
                                text = typeInitial,
                                fontSize = 10.sp,
                                color = Color(0xFF71D7CD),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Expanded layout
                        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .clip(CircleShape)
                                    .background(accountColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = account.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = account.type,
                                    fontSize = 10.sp,
                                    color = Color(0x80BDC9C6)
                                )
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %,.2f", account.balance),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF71D7CD),
                                    maxLines = 1
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
fun FinancialScoreRing(score: Int, income: Double, expense: Double) {
    val animateScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "ScoreAnimation"
    )

    val insightText = when {
        score >= 80 -> "Excelente! Sua retenção de capital está fantástica."
        score >= 50 -> "Boa saúde financeira. Mantenha os gastos sob controle."
        score > 0 -> "Atenção: Suas despesas estão quase superando as receitas."
        else -> "Alerta: Você gastou mais do que arrecadou ou não há dados suficientes."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(PremiumGlassModifier)
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color(0x1AFFFFFF),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                    val sweep = (animateScore / 100f) * 270f
                    val ringColor = if (score >= 80) Color(0xFF71D7CD) else if (score >= 50) Color(0xFFEAB308) else Color(0xFFEF4444)
                    
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(ringColor.copy(alpha = 0.5f), ringColor),
                            center = Offset(size.width / 2, size.height / 2)
                        ),
                        startAngle = 135f,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = animateScore.toInt().toString(),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = Color.White
                    )
                    Text(
                        text = "SCORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF71D7CD),
                        letterSpacing = 2.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = insightText,
                fontSize = 14.sp,
                color = Color(0xFFBDC9C6),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RECEITAS", fontSize = 10.sp, color = Color(0x99BDC9C6), letterSpacing = 1.sp)
                    Text(String.format(Locale("pt", "BR"), "R$ %,.2f", income), fontWeight = FontWeight.SemiBold, color = Color(0xFF71D7CD))
                }
                HorizontalDivider(modifier = Modifier.height(30.dp).width(1.dp), color = Color(0x33FFFFFF))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DESPESAS", fontSize = 10.sp, color = Color(0x99BDC9C6), letterSpacing = 1.sp)
                    Text(String.format(Locale("pt", "BR"), "R$ %,.2f", expense), fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun SmoothEvolutionChart(transactions: List<Transaction>) {
    var selectedPeriod by remember { mutableStateOf("Este Mês") }
    val periods = listOf("Semana", "Este Mês", "Ano")
    var expandedPeriod by remember { mutableStateOf(false) }
    
    // Dynamically calculate cumulative balance history from real transactions
    val dataPoints = remember(selectedPeriod, transactions) {
        val now = System.currentTimeMillis()
        val dayMillis = 86400000L
        val pointsCount = 7
        
        val intervals = when (selectedPeriod) {
            "Semana" -> List(pointsCount) { now - (pointsCount - 1 - it) * dayMillis }
            "Este Mês" -> List(pointsCount) { now - (pointsCount - 1 - it) * 5 * dayMillis }
            "Ano" -> List(pointsCount) { now - (pointsCount - 1 - it) * 30 * dayMillis }
            else -> List(pointsCount) { now - (pointsCount - 1 - it) * dayMillis }
        }
        
        intervals.map { t ->
            val txsBefore = transactions.filter { it.timestamp <= t }
            val inc = txsBefore.filter { it.isIncome }.sumOf { it.value }
            val exp = txsBefore.filter { !it.isIncome }.sumOf { it.value }
            (inc - exp).toFloat()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(PremiumGlassModifier)
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Evolução Histórica",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { expandedPeriod = true }
                    ) {
                        Text(
                            text = selectedPeriod,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBDC9C6)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFFBDC9C6), modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = expandedPeriod,
                        onDismissRequest = { expandedPeriod = false },
                        modifier = Modifier.background(Color(0xFF131817))
                    ) {
                        periods.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period, color = Color.White) },
                                onClick = {
                                    selectedPeriod = period
                                    expandedPeriod = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val maxPoint = dataPoints.maxOrNull() ?: 100f
                val minPoint = dataPoints.minOrNull() ?: 0f
                val range = (maxPoint - minPoint).coerceAtLeast(1f)
                
                val widthPerPoint = size.width / (dataPoints.size - 1)
                
                val path = Path()
                val fillPath = Path()
                
                var prevX = 0f
                var prevY = size.height - ((dataPoints[0] - minPoint) / range) * size.height
                
                path.moveTo(prevX, prevY)
                fillPath.moveTo(0f, size.height)
                fillPath.lineTo(prevX, prevY)
                
                for (i in 1 until dataPoints.size) {
                    val currentX = i * widthPerPoint
                    val currentY = size.height - ((dataPoints[i] - minPoint) / range) * size.height
                    
                    val controlX1 = prevX + (currentX - prevX) / 2f
                    val controlY1 = prevY
                    val controlX2 = prevX + (currentX - prevX) / 2f
                    val controlY2 = currentY
                    
                    path.cubicTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY)
                    
                    prevX = currentX
                    prevY = currentY
                }
                
                fillPath.lineTo(size.width, size.height)
                fillPath.close()
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x6671D7CD),
                            Color(0x0071D7CD)
                        )
                    )
                )
                
                drawPath(
                    path = path,
                    color = Color(0xFF71D7CD),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val labels = when (selectedPeriod) {
                    "Semana" -> listOf("S", "T", "Q", "Q", "S", "S", "D")
                    "Este Mês" -> listOf("-30d", "-25d", "-20d", "-15d", "-10d", "-5d", "Hoje")
                    "Ano" -> listOf("-180d", "-150d", "-120d", "-90d", "-60d", "-30d", "Hoje")
                    else -> listOf("S", "T", "Q", "Q", "S", "S", "D")
                }
                labels.forEach { label ->
                    Text(label, fontSize = 11.sp, color = Color(0x66FFFFFF), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdown(transactions: List<Transaction>) {
    val expenses = transactions.filter { !it.isIncome }
    val grouped = expenses.groupBy { 
        it.category.trim().lowercase().replaceFirstChar { char -> 
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() 
        } 
    }.mapValues { it.value.sumOf { t -> t.value } }
    val totalExpense = grouped.values.sum()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(PremiumGlassModifier)
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "Despesas por Categoria",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (grouped.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma despesa registrada.",
                        color = Color(0x80BDC9C6),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 14.sp
                    )
                }
            } else {
                val displayData = grouped.toList().sortedByDescending { it.second }.take(6).toMap()
                val totalToUse = totalExpense.coerceAtLeast(1.0)
                
                displayData.forEach { (category, amount) ->
                    val percentage = (amount / totalToUse).toFloat()
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(category, fontSize = 14.sp, color = Color(0xFFDFE3E2))
                            Text(String.format(Locale("pt", "BR"), "R$ %,.2f", amount), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color(0x1AFFFFFF), CircleShape)) {
                            Box(modifier = Modifier.fillMaxWidth(percentage).fillMaxHeight().background(Color(0xFF71D7CD), CircleShape))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsSection(
    transactions: List<Transaction>, 
    bankAccounts: List<BankAccount>, 
    creditCards: List<CreditCard>,
    onTransactionClick: (Transaction) -> Unit
) {
    val sortedTransactions = transactions.sortedByDescending { it.timestamp }
    
    Column {
        Text(
            text = "Transações Recentes",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (sortedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(PremiumGlassModifier)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma transação registrada.",
                    color = Color(0x99BDC9C6),
                    fontFamily = FontFamily.Serif,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        } else {
            sortedTransactions.take(10).forEach { transaction ->
                Box(modifier = Modifier.clickable { onTransactionClick(transaction) }) {
                    TransactionItem(transaction, bankAccounts, creditCards)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, bankAccounts: List<BankAccount>, creditCards: List<CreditCard>) {
    val icon = when (transaction.category.lowercase()) {
        "alimentação", "comida", "mercado" -> Icons.Outlined.Restaurant
        "transporte", "uber", "carro" -> Icons.Outlined.DirectionsCar
        "saúde", "farmácia" -> Icons.Outlined.MedicalServices
        "lazer", "entretenimento" -> Icons.Outlined.Movie
        "salário", "renda" -> Icons.Outlined.AttachMoney
        else -> Icons.Outlined.Receipt
    }
    
    val color = if (transaction.isIncome) Color(0xFF71D7CD) else Color.White
    val sign = if (transaction.isIncome) "+" else "-"

    val originColor = bankAccounts.find { it.name == transaction.accountOrCardName }?.colorHex
        ?: creditCards.find { it.name == transaction.accountOrCardName }?.colorHex
        ?: "#71D7CD"
    val badgeColor = parseHexColor(originColor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(PremiumGlassModifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0x1AFFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = transaction.subtitle.ifEmpty { transaction.category },
                    fontSize = 12.sp,
                    color = Color(0xFF99A5A3),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (transaction.accountOrCardName.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(0.5.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.accountOrCardName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "$sign R$ ${String.format(Locale("pt", "BR"), "%.2f", transaction.value)}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = color,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    editingTransaction: Transaction?,
    onDismiss: () -> Unit,
    onAdd: (String, Double, Boolean, String, String) -> Unit,
    onUpdate: (Transaction, Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    var title by remember { mutableStateOf(editingTransaction?.title ?: "") }
    var valueStr by remember { mutableStateOf(editingTransaction?.value?.toString() ?: "") }
    var isIncome by remember { mutableStateOf(editingTransaction?.isIncome ?: false) }
    var category by remember { mutableStateOf(editingTransaction?.category ?: "Alimentação") }
    
    val origins = remember(bankAccounts, creditCards) {
        bankAccounts.map { it.name } + creditCards.map { it.name }
    }
    var selectedOrigin by remember { 
        mutableStateOf(editingTransaction?.accountOrCardName ?: origins.firstOrNull() ?: "") 
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xED070909)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingTransaction != null) "Editar Lançamento" else "Nova Transação",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    }
                }

                // Despesa/Receita Toggle Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isIncome) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (!isIncome) Color(0xFFEF4444) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { isIncome = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Despesa", color = if (!isIncome) Color.White else Color(0x66FFFFFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isIncome) Color(0xFF71D7CD).copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (isIncome) Color(0xFF71D7CD) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { isIncome = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Receita", color = if (isIncome) Color.White else Color(0x66FFFFFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Value input (centrally positioned, huge font)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "VALOR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0x66FFFFFF),
                        letterSpacing = 1.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "R$ ",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = if (isIncome) Color(0xFF71D7CD) else Color.White
                        )
                        BasicTextField(
                            value = valueStr,
                            onValueChange = { valueStr = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = Color.White,
                                textAlign = TextAlign.Start
                            ),
                            cursorBrush = SolidColor(Color(0xFF71D7CD)),
                            modifier = Modifier.width(180.dp)
                        )
                    }
                    Box(modifier = Modifier.width(220.dp).height(1.dp).background(Color(0x33FFFFFF)))
                }

                // Origin selector
                Column {
                    Text("DEBITAR/CREDITAR EM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (origins.isEmpty()) {
                        Text("Cadastre uma conta/cartão primeiro", color = Color(0xFFEF4444), fontSize = 12.sp)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            origins.forEach { originName ->
                                val isSelected = selectedOrigin == originName
                                val originColor = bankAccounts.find { it.name == originName }?.colorHex
                                    ?: creditCards.find { it.name == originName }?.colorHex ?: "#71D7CD"
                                val baseColor = parseHexColor(originColor)
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) baseColor.copy(alpha = 0.2f) else Color(0x0AFFFFFF))
                                        .border(1.dp, if (isSelected) baseColor else Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                                        .clickable { selectedOrigin = originName }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(originName, color = if (isSelected) Color.White else Color(0x99FFFFFF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Categories Row
                Column {
                    Text("CATEGORIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    val categories = listOf(
                        "Alimentação" to Icons.Outlined.Restaurant,
                        "Transporte" to Icons.Outlined.DirectionsCar,
                        "Saúde" to Icons.Outlined.MedicalServices,
                        "Lazer" to Icons.Outlined.Movie,
                        "Salário" to Icons.Outlined.AttachMoney,
                        "Outros" to Icons.Outlined.Receipt
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (catName, catIcon) ->
                            val isSelected = category == catName
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF71D7CD).copy(alpha = 0.2f) else Color(0x0AFFFFFF))
                                    .border(1.dp, if (isSelected) Color(0xFF71D7CD) else Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                                    .clickable { category = catName }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(catIcon, contentDescription = null, tint = if (isSelected) Color(0xFF71D7CD) else Color(0x66FFFFFF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(catName, color = if (isSelected) Color.White else Color(0x99FFFFFF), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Description field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Descrição", color = Color(0x66FFFFFF)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF71D7CD),
                        unfocusedBorderColor = Color(0x1AFFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Buttons container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (editingTransaction != null) {
                        // Delete Transaction Button (Glowing Red)
                        Button(
                            onClick = { onDelete(editingTransaction) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", color = Color(0x99FFFFFF), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            val v = valueStr.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && v > 0) {
                                if (editingTransaction != null) {
                                    val updatedTx = editingTransaction.copy(
                                        title = title,
                                        value = v,
                                        isIncome = isIncome,
                                        category = category,
                                        accountOrCardName = selectedOrigin
                                    )
                                    onUpdate(editingTransaction, updatedTx)
                                } else {
                                    onAdd(title, v, isIncome, category, selectedOrigin)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.8f)
                    ) {
                        Text(
                            text = if (editingTransaction != null) "Salvar" else "Registrar",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsAndCardsDialog(
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    viewModel: TesseraViewModel,
    onDismiss: () -> Unit
) {
    var isCardsTab by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }

    // Form inputs
    var name by remember { mutableStateOf("") }
    var limitOrBalance by remember { mutableStateOf("") }
    var cardUsedLimit by remember { mutableStateOf("") }
    var lastFour by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("KENNETH S. O.") }
    var accountType by remember { mutableStateOf("Corrente") }
    
    val colorPalettes = listOf("#8A05BE", "#FF7A00", "#E6C619", "#1C1C1C", "#0088FF", "#71D7CD")
    var selectedColor by remember { mutableStateOf(colorPalettes.first()) }

    // Track items being edited
    var editingAccount by remember { mutableStateOf<BankAccount?>(null) }
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFA070909)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gerenciar Finanças",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    }
                }

                // Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isCardsTab) Color(0xFF71D7CD).copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (!isCardsTab) Color(0xFF71D7CD) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { 
                                isCardsTab = false
                                showForm = false
                                editingAccount = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Contas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCardsTab) Color(0xFF71D7CD).copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (isCardsTab) Color(0xFF71D7CD) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { 
                                isCardsTab = true
                                showForm = false
                                editingCard = null
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cartões", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                if (!showForm) {
                    // List View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isCardsTab) {
                            if (bankAccounts.isEmpty()) {
                                Text("Nenhuma conta cadastrada.", color = Color(0x66FFFFFF), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                            }
                            bankAccounts.forEach { account ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.size(12.dp).background(parseHexColor(account.colorHex), CircleShape))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(account.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(account.type, color = Color(0x80BDC9C6), fontSize = 11.sp)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(String.format(Locale("pt", "BR"), "R$ %,.2f", account.balance), color = Color(0xFF71D7CD), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        // Edit Account Button
                                        IconButton(
                                            onClick = {
                                                editingAccount = account
                                                name = account.name
                                                limitOrBalance = account.balance.toString()
                                                accountType = account.type
                                                selectedColor = account.colorHex
                                                showForm = true
                                            }, 
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF71D7CD), modifier = Modifier.size(15.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { viewModel.deleteBankAccount(account) }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        } else {
                            if (creditCards.isEmpty()) {
                                Text("Nenhum cartão cadastrado.", color = Color(0x66FFFFFF), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                            }
                            creditCards.forEach { card ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.size(12.dp).background(parseHexColor(card.colorHex), CircleShape))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(card.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Final: ${card.numberLastFour}", color = Color(0x80BDC9C6), fontSize = 11.sp)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(String.format(Locale("pt", "BR"), "R$ %,.2f", card.limit), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        // Edit Credit Card Button
                                        IconButton(
                                            onClick = {
                                                editingCard = card
                                                name = card.name
                                                limitOrBalance = card.limit.toString()
                                                cardUsedLimit = card.usedLimit.toString()
                                                lastFour = card.numberLastFour
                                                holder = card.holderName
                                                selectedColor = card.colorHex
                                                showForm = true
                                            }, 
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF71D7CD), modifier = Modifier.size(15.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { viewModel.deleteCreditCard(card) }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { 
                            showForm = true
                            editingAccount = null
                            editingCard = null
                            name = ""
                            limitOrBalance = ""
                            cardUsedLimit = "0"
                            lastFour = ""
                            holder = "KENNETH S. O."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                    ) {
                        Text(if (!isCardsTab) "+ Nova Conta" else "+ Novo Cartão", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Form View (Add/Edit)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (!isCardsTab) {
                                if (editingAccount != null) "Editar Conta" else "Nova Conta Bancária"
                            } else {
                                if (editingCard != null) "Editar Cartão" else "Novo Cartão de Crédito"
                            }, 
                            color = Color(0xFF71D7CD), 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 15.sp
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome (ex: Nubank)", color = Color(0x66FFFFFF)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = limitOrBalance,
                                onValueChange = { limitOrBalance = it },
                                label = { Text(if (!isCardsTab) "Saldo" else "Limite Total", color = Color(0x66FFFFFF)) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                            if (isCardsTab) {
                                OutlinedTextField(
                                    value = cardUsedLimit,
                                    onValueChange = { cardUsedLimit = it },
                                    label = { Text("Fatura Atual", color = Color(0x66FFFFFF)) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (!isCardsTab) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x0AFFFFFF))
                                    .padding(4.dp)
                            ) {
                                listOf("Corrente", "Poupança", "Investimento").forEach { type ->
                                    val isSel = accountType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) Color(0xFF71D7CD).copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { accountType = type }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(type, color = if (isSel) Color.White else Color(0x66FFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = lastFour,
                                    onValueChange = { lastFour = it.take(4) },
                                    label = { Text("Últimos 4 Dígitos", color = Color(0x66FFFFFF)) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = holder,
                                    onValueChange = { holder = it },
                                    label = { Text("Titular", color = Color(0x66FFFFFF)) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }

                        Column {
                            Text("PALETA DE CORES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                colorPalettes.forEach { hex ->
                                    val col = parseHexColor(hex)
                                    val isSel = selectedColor == hex
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (isSel) 2.dp else 0.dp,
                                                color = Color.White,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = hex }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(onClick = { 
                                showForm = false
                                editingAccount = null
                                editingCard = null
                            }) {
                                Text("Cancelar", color = Color(0x99FFFFFF))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    val value = limitOrBalance.toDoubleOrNull() ?: 0.0
                                    if (name.isNotBlank() && value >= 0) {
                                        if (!isCardsTab) {
                                            viewModel.addBankAccount(name, value, accountType, selectedColor, id = editingAccount?.id ?: 0)
                                        } else {
                                            val used = cardUsedLimit.toDoubleOrNull() ?: 0.0
                                            viewModel.addCreditCard(name, value, used, if (lastFour.length == 4) lastFour else "0000", selectedColor, holder, id = editingCard?.id ?: 0)
                                        }
                                        showForm = false
                                        editingAccount = null
                                        editingCard = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                            ) {
                                Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustBalancesDialog(
    bankAccounts: List<BankAccount>,
    viewModel: TesseraViewModel,
    onDismiss: () -> Unit
) {
    // Explicit type SnapshotStateMap declared to avoid compiler type inference bugs
    val balanceEdits = remember(bankAccounts) { 
        mutableStateMapOf<Int, String>().apply { 
            bankAccounts.forEach { put(it.id, it.balance.toString()) } 
        } 
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFB070909)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ajustar Saldos Patrimoniais",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    }
                }

                Text(
                    text = "Ajuste os saldos das suas contas corrente, poupança e investimento para recalcular o saldo patrimonial global.",
                    fontSize = 12.sp,
                    color = Color(0xFF99A5A3),
                    lineHeight = 16.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    bankAccounts.forEach { account ->
                        val editValue = balanceEdits[account.id] ?: "0"
                        val accountColor = parseHexColor(account.colorHex)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0CFFFFFF), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.size(10.dp).background(accountColor, CircleShape))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(account.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(account.type, color = Color(0x66FFFFFF), fontSize = 10.sp)
                                }
                            }
                            
                            BasicTextField(
                                value = editValue,
                                onValueChange = { balanceEdits[account.id] = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF71D7CD),
                                    textAlign = TextAlign.End
                                ),
                                cursorBrush = SolidColor(Color(0xFF71D7CD)),
                                modifier = Modifier
                                    .width(100.dp)
                                    .background(Color(0x14FFFFFF), RoundedCornerShape(6.dp))
                                    .border(0.5.dp, Color(0x2BFFFFFF), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color(0x99FFFFFF))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            bankAccounts.forEach { account ->
                                val newValue = balanceEdits[account.id]?.toDoubleOrNull() ?: account.balance
                                if (newValue != account.balance) {
                                    viewModel.addBankAccount(
                                        name = account.name,
                                        balance = newValue,
                                        type = account.type,
                                        colorHex = account.colorHex,
                                        id = account.id
                                    )
                                }
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                    ) {
                        Text("Salvar Todos", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
