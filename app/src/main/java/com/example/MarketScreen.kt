package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.BankAccount
import com.example.data.BenefitCard
import com.example.data.MarketItem
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.bounceClick
import com.example.ui.components.isDarkTheme
import com.example.ui.components.themedCardBackground
import com.example.ui.components.themedCardBorder
import com.example.ui.components.themedButtonBorder
import com.example.ui.components.themedTextFieldColors
import com.example.ui.components.themedSubtleBackground
import com.example.ui.components.themedSubtleBorder
import com.example.ui.components.themedDivider
import com.example.ui.components.themedOverlayBackground
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryGold
import com.example.viewmodel.TesseraViewModel
import kotlinx.coroutines.launch
import java.util.Locale

fun parseDoubleSafely(input: String): Double {
    if (input.isBlank()) return 0.0
    val clean = input.trim()
    val hasTrailingSeparator = clean.endsWith(".") || clean.endsWith(",")
    val normalizedClean = if (hasTrailingSeparator) clean.dropLast(1) else clean
    val lastComma = normalizedClean.lastIndexOf(',')
    val lastPoint = normalizedClean.lastIndexOf('.')
    return try {
        if (lastComma > lastPoint) {
            val normalized = normalizedClean.replace(".", "").replace(',', '.')
            normalized.toDoubleOrNull() ?: 0.0
        } else if (lastPoint > lastComma) {
            val normalized = normalizedClean.replace(",", "")
            normalized.toDoubleOrNull() ?: 0.0
        } else {
            normalizedClean.toDoubleOrNull() ?: 0.0
        }
    } catch (e: Exception) {
        0.0
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val pendingItems by viewModel.pendingMarketItems.collectAsStateWithLifecycle()
    val shoppingItems by viewModel.shoppingMarketItems.collectAsStateWithLifecycle()
    val boughtItems by viewModel.boughtMarketItems.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var showCheckoutDebitDialog by remember { mutableStateOf(false) }
    var showShareOnlineModal by remember { mutableStateOf(false) }
    val syncStatus by viewModel.supabaseMarketSync.syncStatus.collectAsStateWithLifecycle()

    val selectedTab = pagerState.currentPage
    val cartTotal = shoppingItems.filter { it.isChecked }.sumOf { it.price * it.quantity }
    val formattedTotal = String.format(Locale("pt", "BR"), "R$ %,.2f", cartTotal)
    val context = LocalContext.current
    var pendingCheckoutAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    val pdfLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val itemsToPrint = shoppingItems.filter { it.isChecked }
            coroutineScope.launch {
                try {
                    com.example.utils.PdfUtils.generateMarketPdf(context, uri, itemsToPrint, cartTotal)
                    android.widget.Toast.makeText(context, "Resumo PDF salvo com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                } catch(e: Exception) {
                    android.widget.Toast.makeText(context, "Erro ao salvar PDF", android.widget.Toast.LENGTH_SHORT).show()
                }
                pendingCheckoutAction?.invoke()
                pendingCheckoutAction = null
            }
        } else {
            pendingCheckoutAction?.invoke()
            pendingCheckoutAction = null
        }
    }

    val planningListState = rememberLazyListState()
    val shoppingListState = rememberLazyListState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            MarketHeader(
                onBack = onHomeClick,
                selectedTab = selectedTab,
                onTabSelect = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                },
                totalItemsCount = pendingItems.size + shoppingItems.size,
                cartCheckedCount = shoppingItems.count { it.isChecked },
                onShareClick = { showShareOnlineModal = true }
            )
        },
        bottomBar = {
            MarketBottomDock(
                selectedTab = selectedTab,
                cartTotal = cartTotal,
                formattedTotal = formattedTotal,
                onAddClick = { showAddDialog = true },
                onCheckoutClick = { showCheckoutDebitDialog = true },
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Tab Page Contents
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    PlanningView(
                        viewModel = viewModel,
                        pendingItems = pendingItems,
                        boughtItems = boughtItems,
                        listState = planningListState,
                        onAddClick = { showAddDialog = true }
                    )
                } else {
                    ShoppingView(
                        shoppingItems = shoppingItems,
                        listState = shoppingListState,
                        onItemToggle = { viewModel.toggleMarketItemChecked(it) },
                        onItemUpdate = { item, price, qty, unit ->
                            viewModel.updateMarketItemDetails(item, price, qty, unit)
                        },
                        onItemDelete = { viewModel.deleteMarketItem(it) }
                    )
                }
            }
        }

        // Add Item Dialog (Garante que se adiciona com inMarket correspondente à aba ativa)
        if (showAddDialog) {
            AddMarketItemDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, category, quantity, unit, price ->
                    viewModel.addMarketItem(
                        name = name,
                        category = category,
                        price = price,
                        quantity = quantity,
                        unit = unit,
                        inMarket = (selectedTab == 1)
                    )
                    showAddDialog = false
                }
            )
        }

        // Checkout Debit Dialog
        if (showCheckoutDebitDialog) {
            val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
            val benefitCards by viewModel.allBenefitCards.collectAsStateWithLifecycle()
            CheckoutDebitDialog(
                totalAmount = cartTotal,
                formattedTotal = formattedTotal,
                bankAccounts = bankAccounts,
                benefitCards = benefitCards,
                onDismiss = { showCheckoutDebitDialog = false },
                onSkip = {
                    showCheckoutDebitDialog = false
                    pendingCheckoutAction = { viewModel.checkoutCart() }
                    pdfLauncher.launch("Resumo_Mercado_${System.currentTimeMillis()}.pdf")
                },
                onDebit = { accountName, amount ->
                    showCheckoutDebitDialog = false
                    pendingCheckoutAction = { viewModel.checkoutCartWithDebit(accountName, amount) }
                    pdfLauncher.launch("Resumo_Mercado_${System.currentTimeMillis()}.pdf")
                }
            )
        }

        // Share Online Modal
        if (showShareOnlineModal) {
            com.example.ui.components.ShareOnlineModal(
                title = "Lista de Mercado Online",
                subtitle = "Compartilhe o link para acompanhar ou marcar itens em tempo real no navegador.",
                shareUrl = viewModel.supabaseMarketSync.getShareUrl(),
                isSyncing = syncStatus == com.example.data.supabase.SupabaseMarketSyncManager.SyncStatus.SYNCING,
                onDismiss = { showShareOnlineModal = false },
                onRegenerateLink = { viewModel.supabaseMarketSync.generateNewShareId() }
            )
        }
    }
}

@Composable
fun MarketHeader(
    onBack: () -> Unit,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    totalItemsCount: Int,
    cartCheckedCount: Int,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                val thermalBrush = remember { Brush.linearGradient(listOf(Color(0xFFec4899), Color(0xFFf97316))) }
                Column {
                    Text(
                        text = "MERCADO INTELIGENTE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(brush = thermalBrush),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Lista & Carrinho",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Share Online Button
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartilhar Online",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Header Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$cartCheckedCount / $totalItemsCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Pill Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .padding(4.dp)
        ) {
            val tabs = listOf("PLANEJAMENTO", "NO MERCADO")
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) PrimaryTeal else Color.Transparent)
                        .clickable { onTabSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}



@Composable
fun PlanningView(
    viewModel: TesseraViewModel,
    pendingItems: List<MarketItem>,
    boughtItems: List<MarketItem>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onAddClick: () -> Unit
) {
    if (pendingItems.isEmpty() && boughtItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sua lista de planejamento está vazia",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                val thermalBrush = remember { Brush.linearGradient(listOf(Color(0xFFec4899), Color(0xFFf97316))) }
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(52.dp)
                        .background(thermalBrush, RoundedCornerShape(20.dp))
                        .bounceClick { onAddClick() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar Item", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (pendingItems.isNotEmpty()) {
                item {
                    Text(
                        text = "ITENS PARA COMPRAR (${pendingItems.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(pendingItems, key = { it.id }) { item ->
                    MarketItemCard(
                        item = item,
                        onToggle = { viewModel.toggleMarketItemChecked(item) },
                        onDelete = { viewModel.deleteMarketItem(item) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    val thermalBrush = remember { Brush.linearGradient(listOf(Color(0xFFec4899), Color(0xFFf97316))) }
                    Button(
                        onClick = onAddClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(thermalBrush, RoundedCornerShape(20.dp))
                            .bounceClick { onAddClick() }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar Item", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            if (boughtItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "HISTÓRICO RECENTE (${boughtItems.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(boughtItems, key = { it.id }) { item ->
                    MarketItemCard(
                        item = item,
                        isBoughtMode = true,
                        onToggle = { viewModel.toggleMarketItemChecked(item) },
                        onDelete = { viewModel.deleteMarketItem(item) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ShoppingView(
    shoppingItems: List<MarketItem>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onItemToggle: (MarketItem) -> Unit,
    onItemUpdate: (MarketItem, Double, Double, String) -> Unit,
    onItemDelete: (MarketItem) -> Unit
) {
    if (shoppingItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nenhum item marcado para o mercado hoje",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shoppingItems, key = { it.id }) { item ->
                ShoppingItemInteractiveCard(
                    item = item,
                    onToggle = { onItemToggle(item) },
                    onUpdate = { price, qty, unit -> onItemUpdate(item, price, qty, unit) },
                    onDelete = { onItemDelete(item) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun MarketItemCard(
    item: MarketItem,
    isBoughtMode: Boolean = false,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = PremiumGlassModifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onToggle() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Checkbox Circle
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isChecked || isBoughtMode) PrimaryTeal
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (item.isChecked || isBoughtMode) PrimaryTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isChecked || isBoughtMode) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = item.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isBoughtMode) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isBoughtMode) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.quantity.toInt()} ${item.unit} • ${item.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Excluir",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ShoppingItemInteractiveCard(
    item: MarketItem,
    onToggle: () -> Unit,
    onUpdate: (Double, Double, String) -> Unit,
    onDelete: () -> Unit
) {
    var priceText by remember(item.price) { mutableStateOf(if (item.price > 0) item.price.toString() else "") }
    var qty by remember(item.quantity) { mutableStateOf(item.quantity) }

    Box(
        modifier = PremiumGlassModifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (item.isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (item.isChecked) PrimaryTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            .clickable { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isChecked) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = item.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Text(
                            text = item.category,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                }
            }

            // Price & Quantity controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quantity Counter (- / +)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (qty > 1) {
                                qty -= 1.0
                                onUpdate(parseDoubleSafely(priceText), qty, item.unit)
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                    }

                    Text(
                        text = "${qty.toInt()} ${item.unit}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            qty += 1.0
                            onUpdate(parseDoubleSafely(priceText), qty, item.unit)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                    }
                }

                // Price Input Field
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input ->
                        priceText = input
                        onUpdate(parseDoubleSafely(input), qty, item.unit)
                    },
                    placeholder = { Text("R$ 0,00", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier
                        .width(110.dp)
                        .height(44.dp),
                    textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
fun MarketBottomDock(
    selectedTab: Int,
    cartTotal: Double,
    formattedTotal: String,
    onAddClick: () -> Unit,
    onCheckoutClick: () -> Unit,
    viewModel: TesseraViewModel
) {
    val lavaBrush = com.example.ui.components.rememberLavaBrush()
    val thermalBrush = Brush.linearGradient(listOf(Color(0xFFec4899), Color(0xFFf97316)))
    
    val shoppingItems by viewModel.shoppingMarketItems.collectAsStateWithLifecycle()
    val hasItems = shoppingItems.isNotEmpty()
    
    val animatedBottomPadding by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (hasItems) 110.dp else 300.dp,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 250, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "dock_animation"
    )

    if (selectedTab == 1) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = animatedBottomPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(themedOverlayBackground())
                .border(1.dp, themedSubtleBorder(), RoundedCornerShape(32.dp))
                .padding(16.dp)
        ) {
            // Shopping Tab Action Bar with Centered Cart Total & Checkout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Spacer left to help center the total
                Spacer(modifier = Modifier.weight(1f))

                // Centered Total
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL NO CARRINHO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = formattedTotal,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryGold
                    )
                }

                // Spacer middle
                Spacer(modifier = Modifier.weight(1f))
                
                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(thermalBrush)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar Item", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    Button(
                        onClick = onCheckoutClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                        contentPadding = PaddingValues(),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .background(thermalBrush, CircleShape)
                            .bounceClick { onCheckoutClick() }
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = "Finalizar", modifier = Modifier.size(20.dp), tint = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMarketItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Hortifrúti") }
    var quantityText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "NOVO ITEM DE MERCADO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal,
                    letterSpacing = 1.5.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do produto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    shape = RoundedCornerShape(14.dp)
                )

                // Category selector
                Column {
                    Text("Categoria", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Alimentação", "Limpeza", "Higiene", "Outros")) { cat ->
                            val isSelected = category == cat
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Qtd.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Preço (R$)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val qty = parseDoubleSafely(quantityText).coerceAtLeast(1.0)
                                val price = parseDoubleSafely(priceText)
                                onConfirm(name, category, qty, "un", price)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Adicionar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDebitDialog(
    totalAmount: Double,
    formattedTotal: String,
    bankAccounts: List<BankAccount>,
    benefitCards: List<BenefitCard>,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onDebit: (String, Double) -> Unit
) {
    var selectedAccountName by remember {
        mutableStateOf(
            benefitCards.firstOrNull()?.name
                ?: bankAccounts.firstOrNull()?.name
                ?: "Dinheiro / Outro"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "DEBITAR COMPRA DO MERCADO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryGold,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = "Total das Compras: $formattedTotal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Selecione o cartão de benefício ou conta para registrar o débito:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(benefitCards) { card ->
                        val isSelected = selectedAccountName == card.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) SecondaryGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                .border(1.dp, if (isSelected) SecondaryGold else Color.Transparent, RoundedCornerShape(14.dp))
                                .clickable { selectedAccountName = card.name }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(card.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(String.format(Locale("pt", "BR"), "R$ %,.2f", card.balance), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }

                    items(bankAccounts) { account ->
                        val isSelected = selectedAccountName == account.name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) PrimaryTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                                .border(1.dp, if (isSelected) PrimaryTeal else Color.Transparent, RoundedCornerShape(14.dp))
                                .clickable { selectedAccountName = account.name }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(account.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(String.format(Locale("pt", "BR"), "R$ %,.2f", account.balance), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Apenas Finalizar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Button(
                        onClick = { onDebit(selectedAccountName, totalAmount) },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Confirmar Débito", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
