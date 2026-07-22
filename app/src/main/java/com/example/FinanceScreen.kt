package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.BenefitCard
import com.example.data.Transaction
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.AddTransactionBottomSheet
import com.example.ui.components.ManageAccountsAndCardsBottomSheet
import com.example.viewmodel.TesseraViewModel
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
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
fun FinanceScreen(
    onHomeClick: () -> Unit,
    viewModel: TesseraViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToInvoiceHub: (String) -> Unit,
    onNavigateToBenefitHub: (String) -> Unit
) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()
    val benefitCards by viewModel.allBenefitCards.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var editingLongClickAccount by remember { mutableStateOf<BankAccount?>(null) }
    var editingLongClickCreditCard by remember { mutableStateOf<CreditCard?>(null) }
    var editingLongClickBenefitCard by remember { mutableStateOf<BenefitCard?>(null) }
    var showManageDialog by remember { mutableStateOf(false) }
    var showAdjustBalanceDialog by remember { mutableStateOf(false) }
    var showDebtsPanel by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    val sumInvestmentsToBalance = sharedPrefs.getBoolean("sum_investments_to_balance", false)
    val sumInvestmentsToSpendable = sharedPrefs.getBoolean("sum_investments_to_spendable", false)

    var isPrivacyModeEnabled by remember { mutableStateOf(true) }
    var defaultIsIncomeForAdd by remember { mutableStateOf(false) }
    var defaultIsTransferForAdd by remember { mutableStateOf(false) }
    
    var selectedFilterName by remember { mutableStateOf<String?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    if (showDebtsPanel) {
        DebtsScreen(
            viewModel = viewModel,
            onBack = { showDebtsPanel = false }
        )
        return
    }

    LaunchedEffect(Unit) {
        viewModel.financeActionTrigger.collect { action ->
            when (action) {
                TesseraViewModel.FinanceAction.ADD_EXPENSE -> {
                    editingTransaction = null
                    defaultIsIncomeForAdd = false
                    defaultIsTransferForAdd = false
                    showAddDialog = true
                }
                TesseraViewModel.FinanceAction.ADD_INCOME -> {
                    editingTransaction = null
                    defaultIsIncomeForAdd = true
                    defaultIsTransferForAdd = false
                    showAddDialog = true
                }
            }
        }
    }

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

    if (showAddDialog) {
        AddTransactionBottomSheet(
            bankAccounts = bankAccounts,
            creditCards = creditCards,
            benefitCards = benefitCards,
            editingTransaction = editingTransaction,
            defaultIsIncome = defaultIsIncomeForAdd,
            defaultIsTransfer = defaultIsTransferForAdd,
            onDismiss = { 
                showAddDialog = false
                editingTransaction = null
            },
            onAdd = { title, value, isIncome, category, origin, isRealized, isRecurrent, interval, dueDate, isInstallment, installmentsCount ->
                if (isInstallment) {
                    viewModel.addInstallmentTransaction(title, value, isIncome, category, origin, isRealized, installmentsCount, dueDate)
                } else {
                    viewModel.addTransaction(title, "", value, isIncome, category, origin, isRealized, isRecurrent, interval, dueDate)
                }
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
            },
            onTransfer = { from, to, value, date ->
                viewModel.addTransaction("Transferência para $to", "", value, false, "Transferência", from, true, false, "Mensal", date)
                viewModel.addTransaction("Transferência de $from", "", value, true, "Transferência", to, true, false, "Mensal", date)
            }
        )
    }

    if (showManageDialog) {
        ManageAccountsAndCardsBottomSheet(
            bankAccounts = bankAccounts,
            creditCards = creditCards,
            benefitCards = benefitCards,
            viewModel = viewModel,
            onDismiss = { showManageDialog = false }
        )
    }

    editingLongClickAccount?.let { account ->
        EditBankAccountDialog(
            account = account,
            viewModel = viewModel,
            onDismiss = { editingLongClickAccount = null }
        )
    }

    if (editingLongClickCreditCard != null || editingLongClickBenefitCard != null) {
        ManageAccountsAndCardsBottomSheet(
            bankAccounts = bankAccounts,
            creditCards = creditCards,
            benefitCards = benefitCards,
            viewModel = viewModel,
            initialEditingCreditCard = editingLongClickCreditCard,
            initialEditingBenefitCard = editingLongClickBenefitCard,
            onDismiss = { 
                editingLongClickCreditCard = null
                editingLongClickBenefitCard = null
            }
        )
    }

    if (showAdjustBalanceDialog) {
        AdjustBalancesDialog(
            bankAccounts = bankAccounts,
            viewModel = viewModel,
            onDismiss = { showAdjustBalanceDialog = false }
        )
    }

    val currentMonthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val currentMonthTransactions = remember(allTransactions, currentMonthStart) {
        allTransactions.filter { it.timestamp >= currentMonthStart }
    }

    val salaryValue = remember(currentMonthTransactions, creditCards, benefitCards) {
        val incomeSum = currentMonthTransactions.filter { tx -> 
            tx.isIncome && 
            tx.category != "Transferência" &&
            creditCards.none { card -> card.name == tx.accountOrCardName } &&
            benefitCards.none { card -> card.name == tx.accountOrCardName }
        }.sumOf { it.value }
        if (incomeSum > 0.0) incomeSum else 0.0
    }

    val committedValue = remember(currentMonthTransactions, creditCards, benefitCards) {
        val expenseSum = currentMonthTransactions.filter { tx ->
            !tx.isIncome &&
            tx.category != "Transferência" &&
            creditCards.none { card -> card.name == tx.accountOrCardName } &&
            benefitCards.none { card -> card.name == tx.accountOrCardName }
        }.sumOf { it.value }
        if (expenseSum > 0.0) expenseSum else 0.0
    }

    val freeValue = checkingBalance + if (sumInvestmentsToSpendable) (savingsBalance + investmentBalance) else 0.0

    val scrollState = rememberScrollState()
    val isCompact = scrollState.value > 150
    val normalAlpha by animateFloatAsState(targetValue = if (isCompact) 0f else 1f, animationSpec = tween(250), label = "normalAlpha")
    val compactAlpha by animateFloatAsState(targetValue = if (isCompact) 1f else 0f, animationSpec = tween(250), label = "compactAlpha")
    val accentColor = Color(0xFF71D7CD)

    Scaffold(
        containerColor = Color(0xFF070909),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(72.dp))


            Spacer(modifier = Modifier.height(16.dp))

            val displayValue = remember(selectedFilterName, bankAccounts, creditCards, checkingBalance, savingsBalance, investmentBalance, sumInvestmentsToBalance) {
                val activeCard = creditCards.find { it.name == selectedFilterName }
                val activeAccount = bankAccounts.find { it.name == selectedFilterName }
                when {
                    activeCard != null -> activeCard.usedLimit
                    activeAccount != null -> activeAccount.balance
                    else -> checkingBalance + if (sumInvestmentsToBalance) (savingsBalance + investmentBalance) else 0.0
                }
            }

            // Hero spendable budget card ("Quanto posso gastar")
            val filterName = selectedFilterName
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(PremiumGlassModifier)
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = when {
                            filterName != null -> "LIMITE/SALDO ATUAL • ${filterName.uppercase()}"
                            else -> "DISPONÍVEL PARA GASTAR"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF71D7CD),
                        letterSpacing = 1.5.sp
                    )

                    val mainValue = if (filterName != null) displayValue else freeValue
                    val isNegative = mainValue < 0.0

                    Text(
                        text = if (isPrivacyModeEnabled) "R$ ••••••" else String.format(Locale("pt", "BR"), "R$ %,.2f", mainValue),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        color = if (isNegative && !isPrivacyModeEnabled) Color(0xFFEF4444) else Color(0xFFDFE3E2)
                    )

                    if (filterName == null) {
                        // Breakdown of salary vs committed
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val ratio = if (salaryValue > 0.0) (committedValue / salaryValue).toFloat().coerceIn(0f, 1f) else 0f
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Orçamento Comprometido", fontSize = 12.sp, color = Color(0x99BDC9C6))
                                Text(
                                    text = if (isPrivacyModeEnabled) "•••" else "${(ratio * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1AFFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF71D7CD),
                                                    Color(0xFF3B82F6),
                                                    Color(0xFFEF4444)
                                                )
                                            )
                                        )
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isPrivacyModeEnabled) "Receitas: R$ ••••••" else String.format(Locale("pt", "BR"), "Receitas: R$ %,.2f", salaryValue),
                                    fontSize = 11.sp,
                                    color = Color(0xFF81C784)
                                )
                                Text(
                                    text = if (isPrivacyModeEnabled) "Comprometido: R$ ••••••" else String.format(Locale("pt", "BR"), "Comprometido: R$ %,.2f", committedValue),
                                    fontSize = 11.sp,
                                    color = Color(0xFFEF5350)
                                )
                            }
                        }
                    } else {
                        // Detailed view of selected filter (card or account details)
                        val activeCard = creditCards.find { it.name == selectedFilterName }
                        if (activeCard != null) {
                            Text(
                                text = if (isPrivacyModeEnabled) "Limite disponível: R$ ••••••" else String.format(Locale("pt", "BR"), "Limite disponível: R$ %,.2f", activeCard.limit - activeCard.usedLimit),
                                fontSize = 13.sp,
                                color = Color(0xFFBDC9C6)
                            )
                        } else {
                            val activeAccount = bankAccounts.find { it.name == selectedFilterName }
                            if (activeAccount != null) {
                                Text(
                                    text = "Conta tipo: ${activeAccount.type}",
                                    fontSize = 13.sp,
                                    color = Color(0xFFBDC9C6)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card do Painel de Dívidas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(PremiumGlassModifier)
                    .clickable { showDebtsPanel = true },
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AEF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Painel de Dívidas",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Acompanhe tudo que deve e precisa pagar",
                            fontSize = 11.sp,
                            color = Color(0xFFBDC9C6)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cartões e Contas (Sempre Expandidos)
            SectionHeader(
                title = "Seus Cartões",
                onAddClick = { showManageDialog = true }
            )
            CreditCardsCarousel(
                creditCards = creditCards,
                benefitCards = benefitCards,
                selectedFilterName = selectedFilterName,
                onCardClick = { cardName ->
                    if (benefitCards.any { it.name == cardName }) {
                        onNavigateToBenefitHub(cardName)
                    } else {
                        onNavigateToInvoiceHub(cardName)
                    }
                },
                onCardLongClick = { cardName ->
                    editingLongClickBenefitCard = benefitCards.find { it.name == cardName }
                    editingLongClickCreditCard = creditCards.find { it.name == cardName }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(
                title = "Suas Contas",
                onAddClick = { showManageDialog = true }
            )
            BankAccountsSection(
                bankAccounts = bankAccounts,
                selectedFilterName = selectedFilterName,
                onAccountClick = { accountName ->
                    selectedFilterName = if (selectedFilterName == accountName) null else accountName
                },
                onAccountLongClick = { accountName ->
                    editingLongClickAccount = bankAccounts.find { it.name == accountName }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botões de Ação Rápidos
            ActionButtonsRow(
                onReceiveClick = {
                    editingTransaction = null
                    defaultIsIncomeForAdd = true
                    defaultIsTransferForAdd = false
                    showAddDialog = true
                },
                onPayClick = {
                    editingTransaction = null
                    defaultIsIncomeForAdd = false
                    defaultIsTransferForAdd = false
                    showAddDialog = true
                },
                onTransferClick = {
                    editingTransaction = null
                    defaultIsIncomeForAdd = false
                    defaultIsTransferForAdd = true
                    showAddDialog = true
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Seção de Três Painéis Deslizáveis (Dívidas | Parcelados | Fixos)
            val coroutineScope = rememberCoroutineScope()
            val pagerState = rememberPagerState(pageCount = { 3 })

            Column(modifier = Modifier.fillMaxWidth()) {
                // Header das abas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val titles = listOf("🔴 Dívidas", "🔵 Parcelados", "🟢 Fixos")
                    titles.forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF71D7CD).copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (isSelected) Color(0xFF71D7CD) else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color(0x99FFFFFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 380.dp, max = 520.dp)
                ) { page ->
                    when (page) {
                        0 -> {
                            // Quadro 1: Dívidas (Com indicação visual de arrastar para o lado)
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x2271D7CD))
                                        .border(1.dp, Color(0xFF71D7CD).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.SwipeLeft,
                                            contentDescription = null,
                                            tint = Color(0xFF71D7CD),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Arraste para o lado para ver Parcelados e Fixos ➔",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                DebtsScreen(viewModel = viewModel, isEmbedded = true)
                            }
                        }
                        1 -> {
                            // Quadro 2: Parcelados
                            val installmentTxs = remember(filteredTransactions) {
                                filteredTransactions.filter { tx ->
                                    !tx.isIncome && (tx.subtitle.contains("Parcela") || tx.title.contains("/") || (!tx.isRecurrent && tx.dueDate > 0))
                                }
                            }
                            val totalInstallmentValue = remember(installmentTxs) { installmentTxs.sumOf { it.value } }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = PremiumGlassModifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("DESPESAS PARCELADAS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A90E2), letterSpacing = 1.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = String.format(Locale("pt", "BR"), "R$ %,.2f", totalInstallmentValue),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text("${installmentTxs.size} parcelas ativas este mês", fontSize = 12.sp, color = Color(0x99FFFFFF))
                                    }
                                }

                                 if (installmentTxs.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                        Text("Nenhuma despesa parcelada este mês.", color = Color(0x66FFFFFF), fontSize = 13.sp)
                                    }
                                } else {
                                    installmentTxs.forEach { tx ->
                                        Box(modifier = Modifier.clickable {
                                            editingTransaction = tx
                                            showAddDialog = true
                                        }) {
                                            TransactionItem(
                                                transaction = tx,
                                                bankAccounts = bankAccounts,
                                                creditCards = creditCards
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Quadro 3: Fixos & Recorrentes
                            RecurringExpensesSection(
                                transactions = filteredTransactions,
                                viewModel = viewModel,
                                onTransactionClick = { tx ->
                                    editingTransaction = tx
                                    showAddDialog = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gastos e Despesas em Geral
            RecentTransactionsSection(
                transactions = filteredTransactions,
                bankAccounts = bankAccounts,
                creditCards = creditCards,
                onTransactionClick = { tx ->
                    editingTransaction = tx
                    showAddDialog = true
                }
            )

            Spacer(modifier = Modifier.height(120.dp))
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
                        Text(
                            text = "FINANÇAS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val filterName = selectedFilterName
                        if (filterName != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .clickable { selectedFilterName = null }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Limpar Filtro", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        IconButton(onClick = { isPrivacyModeEnabled = !isPrivacyModeEnabled }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (isPrivacyModeEnabled) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Modo Privacidade",
                                tint = Color(0xFFBDC9C6),
                                modifier = Modifier.size(20.dp)
                            )
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
                        imageVector = Icons.Outlined.Paid,
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
                        text = "FINANÇAS",
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
    }
}

@Composable
fun RaloXCard(
    salary: Double,
    committed: Double,
    free: Double,
    isPrivacyMode: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(PremiumGlassModifier)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF71D7CD),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ralo-X do Adiantamento",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDFE3E2)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1AFFFFFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Este Mês",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBDC9C6)
                    )
                }
            }
            
            Text(
                text = if (isPrivacyMode) "R$ ••••••" else String.format(Locale("pt", "BR"), "R$ %,.2f", salary),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color.White
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comprometido",
                        fontSize = 13.sp,
                        color = Color(0xFFBDC9C6)
                    )
                    Text(
                        text = if (isPrivacyMode) "R$ ••••••" else String.format(Locale("pt", "BR"), "R$ %,.2f", committed),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
                
                val ratio = if (salary > 0.0) (committed / salary).toFloat().coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF71D7CD),
                                        Color(0xFF3B82F6),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            )
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Livre para gastar",
                    fontSize = 13.sp,
                    color = Color(0xFFBDC9C6)
                )
                Text(
                    text = if (isPrivacyMode) "R$ ••••••" else String.format(Locale("pt", "BR"), "R$ %,.2f", free),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71D7CD)
                )
            }
        }
    }
}

@Composable
fun ActionButtonsRow(
    onReceiveClick: () -> Unit,
    onPayClick: () -> Unit,
    onTransferClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(
            title = "RECEBER",
            icon = Icons.Default.ArrowDownward,
            iconTint = Color(0xFF71D7CD),
            bgCircleColor = Color(0x1A71D7CD),
            rotation = 45f,
            onClick = onReceiveClick,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            title = "PAGAR",
            icon = Icons.Default.ArrowUpward,
            iconTint = Color(0xFFEF4444),
            bgCircleColor = Color(0x1AEF4444),
            rotation = 45f,
            onClick = onPayClick,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            title = "TRANSFERIR",
            icon = Icons.Default.SyncAlt,
            iconTint = Color.White,
            bgCircleColor = Color(0x1AFFFFFF),
            rotation = 0f,
            onClick = onTransferClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    bgCircleColor: Color,
    rotation: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x0CFFFFFF))
            .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
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
                    .background(bgCircleColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                        }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFBDC9C6),
                letterSpacing = 1.sp
            )
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
            text = if (isPrivacyModeEnabled) "R$ *****" else String.format(Locale("pt", "BR"), "R$ %,.2f", displayValue),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 42.sp,
            color = Color(0xFFDFE3E2)
        )

        if (activeCard != null) {
            Text(
                text = if (isPrivacyModeEnabled) "Limite disponível: R$ *****" else String.format(Locale("pt", "BR"), "Limite disponível: R$ %,.2f", activeCard.limit - activeCard.usedLimit),
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
                            text = if (isPrivacyModeEnabled) "R$ *****" else String.format(Locale("pt", "BR"), "R$ %,.2f", checkingBalance), 
                            fontSize = 11.sp, 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold
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
                            text = if (isPrivacyModeEnabled) "R$ *****" else String.format(Locale("pt", "BR"), "R$ %,.2f", savingsBalance), 
                            fontSize = 11.sp, 
                            color = Color(0xFF71D7CD), 
                            fontWeight = FontWeight.Bold
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
                            text = if (isPrivacyModeEnabled) "R$ *****" else String.format(Locale("pt", "BR"), "R$ %,.2f", investmentBalance), 
                            fontSize = 11.sp, 
                            color = Color(0xFFEAB308), 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun SectionHeader(
    title: String,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.White
        )
        IconButton(onClick = onAddClick, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Gerenciar",
                tint = Color(0xFF71D7CD)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CreditCardsCarousel(
    creditCards: List<CreditCard>,
    benefitCards: List<BenefitCard>,
    selectedFilterName: String?,
    onCardClick: (String) -> Unit,
    onCardLongClick: (String) -> Unit
) {
    if (creditCards.isEmpty() && benefitCards.isEmpty()) {
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
            benefitCards.forEach { card ->
                val isSelected = selectedFilterName == card.name
                val cardColor = parseHexColor(card.colorHex)

                Box(
                    modifier = Modifier
                        .width(280.dp)
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
                        .combinedClickable(
                            onClick = { onCardClick(card.name) },
                            onLongClick = { onCardLongClick(card.name) }
                        )
                        .padding(20.dp)
                ) {
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
                            Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Color.White)
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale("pt", "BR"), "R$ %,.2f", card.balance),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = cardColor,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column {
                                Text(
                                    text = "Saldo Atual",
                                    fontSize = 11.sp,
                                    color = Color(0xFFBDC9C6),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = card.holderName,
                                    fontSize = 10.sp,
                                    color = Color(0xFF808A89),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            Text(
                                text = "•••• ${card.numberLastFour}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDFE3E2)
                            )
                        }
                    }
                }
            }

            creditCards.forEach { card ->
                val isSelected = selectedFilterName == card.name
                val cardColor = parseHexColor(card.colorHex)
                
                Box(
                    modifier = Modifier
                        .width(280.dp)
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
                        .combinedClickable(
                            onClick = { onCardClick(card.name) },
                            onLongClick = { onCardLongClick(card.name) }
                        )
                        .padding(20.dp)
                ) {
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
                                    color = Color(0xFFBDC9C6),
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BankAccountsSection(
    bankAccounts: List<BankAccount>,
    selectedFilterName: String?,
    onAccountClick: (String) -> Unit,
    onAccountLongClick: (String) -> Unit
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
                
                Box(
                    modifier = Modifier
                        .width(195.dp)
                        .height(90.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x14FFFFFF))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            brush = if (isSelected) SolidColor(accountColor) else SolidColor(Color(0x1AFFFFFF)),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .combinedClickable(
                            onClick = { onAccountClick(account.name) },
                            onLongClick = { onAccountLongClick(account.name) }
                        )
                        .padding(12.dp)
                ) {
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
    val sortedTransactions = remember(transactions) {
        transactions.sortedByDescending { it.timestamp }
    }
    
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
            val recentList = remember(sortedTransactions) { sortedTransactions.take(15) }
            val groupedTransactions = remember(recentList) {
                recentList.groupBy { tx ->
                    val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    val today = Calendar.getInstance()
                    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    
                    when {
                        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hoje"
                        
                        cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Ontem"
                        
                        else -> {
                            val sdf = java.text.SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR"))
                            sdf.format(Date(tx.timestamp))
                        }
                    }
                }
            }
            
            groupedTransactions.forEach { (dateHeader, txList) ->
                Text(
                    text = dateHeader.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71D7CD),
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                txList.forEach { transaction ->
                    Box(modifier = Modifier.clickable { onTransactionClick(transaction) }) {
                        TransactionItem(transaction, bankAccounts, creditCards)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
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

    val isOverdue = !transaction.isRealized && transaction.dueDate > 0L && transaction.dueDate < System.currentTimeMillis()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(PremiumGlassModifier)
            .then(
                if (isOverdue) {
                    Modifier.border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(28.dp))
                } else {
                    Modifier
                }
            )
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
                val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
                val dateStr = dateFormat.format(java.util.Date(transaction.timestamp))

                Text(
                    text = "$dateStr • ${transaction.subtitle.ifEmpty { transaction.category }}",
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
                if (!transaction.isRealized) {
                    val labelText = if (isOverdue) "Atrasado" else "Pendente"
                    val labelColor = if (isOverdue) Color(0xFFEF4444) else Color(0xFFEAB308)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(labelColor.copy(alpha = 0.15f))
                            .border(0.5.dp, labelColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = labelText.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = labelColor,
                            maxLines = 1
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF71D7CD).copy(alpha = 0.15f))
                            .border(0.5.dp, Color(0xFF71D7CD).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PAGA",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF71D7CD),
                            maxLines = 1
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "$sign R$ ${String.format(Locale("pt", "BR"), "%,.2f", transaction.value)}",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = color,
            maxLines = 1
        )
    }
}

@Composable
fun OverdueAlertBanner(overdueCount: Int, onClick: () -> Unit = {}) {
    val infiniteTransition = rememberInfiniteTransition(label = "OverdueAlertGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x1AEF4444))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = glowAlpha), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x26EF4444)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "ATENÇÃO: DÉBITOS EM ATRASO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (overdueCount == 1) "Você tem 1 lançamento pendente vencido." else "Você tem $overdueCount lançamentos pendentes vencidos.",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}


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

fun showFinanceDatePicker(context: Context, initialTime: Long, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialTime }
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(selectedCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@Composable
fun RecurringExpensesSection(
    transactions: List<Transaction>,
    viewModel: TesseraViewModel,
    onTransactionClick: (Transaction) -> Unit
) {
    val recurrentExpenses = remember(transactions) {
        transactions.filter { it.isRecurrent && !it.isIncome }
    }
    
    if (recurrentExpenses.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Despesas Recorrentes",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                recurrentExpenses.forEach { tx ->
                    val isOverdue = !tx.isRealized && tx.dueDate > 0L && tx.dueDate < System.currentTimeMillis()
                    val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault()) }
                    val dueDateStr = dateFormat.format(java.util.Date(tx.dueDate))
                    
                    val borderBrush = if (tx.isRealized) {
                        SolidColor(Color(0x0CFFFFFF))
                    } else if (isOverdue) {
                        val infiniteTransition = rememberInfiniteTransition(label = "OverdueGlow")
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 0.9f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "Alpha"
                        )
                        Brush.linearGradient(listOf(Color(0xFFEF4444).copy(alpha = glowAlpha), Color(0xFFEF4444).copy(alpha = 0.2f)))
                    } else {
                        SolidColor(Color(0x1AFFFFFF))
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x0CFFFFFF))
                            .border(
                                width = if (isOverdue) 1.5.dp else 1.dp,
                                brush = borderBrush,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onTransactionClick(tx) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            tx.isRealized -> Color(0x1A81C784)
                                            isOverdue -> Color(0x26EF4444)
                                            else -> Color(0x1A71D7CD)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        tx.isRealized -> Icons.Default.Check
                                        isOverdue -> Icons.Outlined.ErrorOutline
                                        else -> Icons.Outlined.Repeat
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        tx.isRealized -> Color(0xFF81C784)
                                        isOverdue -> Color(0xFFEF4444)
                                        else -> Color(0xFF71D7CD)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = tx.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (tx.isRealized) Color(0x99FFFFFF) else Color.White
                                )
                                Text(
                                    text = if (tx.accountOrCardName.isNotEmpty()) "Origem: ${tx.accountOrCardName}" else "Sem origem",
                                    fontSize = 11.sp,
                                    color = Color(0x80BDC9C6)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                when {
                                    tx.isRealized -> {
                                        Text(
                                            text = "Paga (Vencimento: $dueDateStr)",
                                            fontSize = 9.sp,
                                            color = Color(0xFF81C784),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    isOverdue -> {
                                        val diffMs = System.currentTimeMillis() - tx.dueDate
                                        val diffDays = (diffMs / (24 * 60 * 60 * 1000L)).coerceAtLeast(1)
                                        Text(
                                            text = "ATRASADO HÁ $diffDays DIAS (Venceu em $dueDateStr)",
                                            fontSize = 9.sp,
                                            color = Color(0xFFEF4444),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "Vence em: $dueDateStr (${tx.recurrenceInterval})",
                                            fontSize = 9.sp,
                                            color = Color(0xFFBDC9C6)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = String.format(java.util.Locale("pt", "BR"), "R$ %,.2f", tx.value),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = when {
                                    tx.isRealized -> Color(0xFF81C784)
                                    isOverdue -> Color(0xFFEF4444)
                                    else -> Color.White
                                }
                            )
                            
                            if (!tx.isRealized) {
                                Button(
                                    onClick = { viewModel.realizeRecurrentTransaction(tx) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isOverdue) Color(0xFFEF4444) else Color(0xFF71D7CD),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Pagar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x1A81C784))
                                        .border(0.5.dp, Color(0xFF81C784).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Paga", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
fun EditBankAccountDialog(
    account: BankAccount,
    viewModel: TesseraViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var accountType by remember { mutableStateOf(account.type) }
    var colorHex by remember { mutableStateOf(account.colorHex) }
    val colorPalettes = listOf("#8A05BE", "#FF7A00", "#E6C619", "#1C1C1C", "#0088FF", "#71D7CD")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xED070909)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Editar Conta", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Conta", color = Color(0x99FFFFFF)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF71D7CD),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tipo de Conta", color = Color.White, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Corrente", "Poupança", "Investimento").forEach { t ->
                        val isSelected = accountType == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF71D7CD).copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.dp, if (isSelected) Color(0xFF71D7CD) else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .clickable { accountType = t }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t, color = if (isSelected) Color.White else Color(0x99FFFFFF), fontSize = 12.sp)
                        }
                    }
                }

                Text("Cor", color = Color.White, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorPalettes) { hex ->
                        val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF71D7CD) }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (colorHex == hex) 3.dp else 0.dp,
                                    color = if (colorHex == hex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        viewModel.deleteBankAccount(account)
                        onDismiss()
                    }) {
                        Text("Excluir", color = Color(0xFFEF4444))
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                viewModel.addBankAccount(name, account.balance, accountType, colorHex, account.id)
                                onDismiss()
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
