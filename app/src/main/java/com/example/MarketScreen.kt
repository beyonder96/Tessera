package com.example

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MarketItem
import com.example.viewmodel.TesseraViewModel
import com.example.ui.components.PremiumGlassModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import java.util.Locale
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import com.example.data.BankAccount
import com.example.data.BenefitCard

fun parseDoubleSafely(input: String): Double {
    if (input.isBlank()) return 0.0
    val clean = input.trim()
    
    // If the string ends with a separator, drop it temporarily to get the numeric value
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
    var showShareDialog by remember { mutableStateOf(false) }
    var showCheckoutDebitDialog by remember { mutableStateOf(false) }
    val marketListId by viewModel.marketListId.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncManager.syncStatus.collectAsStateWithLifecycle()
    val isFirebaseConfigured by viewModel.syncManager.isConfigured.collectAsStateWithLifecycle()
    
    val selectedTab = pagerState.currentPage
    val currentTabTitle = if (selectedTab == 0) "PLANEJAMENTO" else "NO MERCADO"
    
    val cartTotal = shoppingItems.filter { it.isChecked }.sumOf { it.price * it.quantity }
    val formattedTotal = String.format(Locale("pt", "BR"), "R$ %,.2f", cartTotal)

    val planningListState = rememberLazyListState()
    val shoppingListState = rememberLazyListState()

    val isCompact by remember(selectedTab) {
        derivedStateOf {
            when (selectedTab) {
                0 -> planningListState.firstVisibleItemIndex > 0 || planningListState.firstVisibleItemScrollOffset > 100
                else -> shoppingListState.firstVisibleItemIndex > 0 || shoppingListState.firstVisibleItemScrollOffset > 100
            }
        }
    }

    val normalAlpha by animateFloatAsState(targetValue = if (isCompact) 0f else 1f, animationSpec = tween(250), label = "normalAlpha")
    val compactAlpha by animateFloatAsState(targetValue = if (isCompact) 1f else 0f, animationSpec = tween(250), label = "compactAlpha")
    val accentColor = Color(0xFF71D7CD)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFF070909),
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {},
            bottomBar = {
                // Bottom anchored elements for one-handed usage
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    }, label = "BottomBarTransition"
                ) { targetTab ->
                    if (targetTab == 0) {
                        PlanningBottomBar(viewModel)
                    } else {
                        ShoppingBottomBar(
                            cartTotal = cartTotal,
                            formattedTotal = formattedTotal,
                            onCheckout = { showCheckoutDebitDialog = true },
                            onAddClick = { showAddDialog = true }
                        )
                    }
                }
            }
        ) { innerPadding ->

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
                viewModel.checkoutCart()
            },
            onDebit = { accountName, amount ->
                showCheckoutDebitDialog = false
                viewModel.checkoutCartWithDebit(accountName, amount)
            }
        )
    }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding() // Ensures list resizes when keyboard opens
            ) {

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (page == 0) {
                        PlanningView(viewModel, pendingItems, boughtItems, planningListState)
                    } else {
                        ShoppingView(
                            pendingItems = shoppingItems,
                            listState = shoppingListState,
                            onItemToggle = { viewModel.toggleMarketItemChecked(it) },
                            onItemUpdate = { item, price, qty, unit -> 
                                viewModel.updateMarketItemDetails(item, price, qty, unit)
                            }
                        )
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = normalAlpha
                            scaleX = 0.92f + (normalAlpha * 0.08f)
                            scaleY = 0.92f + (normalAlpha * 0.08f)
                            translationY = (1f - normalAlpha) * (-50f)
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MERCADO",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 2.sp
                            )
                        }
                        
                        IconButton(
                            onClick = { showShareDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartilhar Lista",
                                tint = Color(0xFF71D7CD)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Elegant Segmented Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF141918))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(32.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabPill(
                            text = "Planejamento",
                            isSelected = selectedTab == 0,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                            modifier = Modifier.weight(1f)
                        )
                        TabPill(
                            text = "No Mercado",
                            isSelected = selectedTab == 1,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                            modifier = Modifier.weight(1f)
                        )
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
                        .clickable {
                            coroutineScope.launch {
                                if (selectedTab == 0) planningListState.animateScrollToItem(0)
                                else shoppingListState.animateScrollToItem(0)
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
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
                        text = currentTabTitle,
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

        if (showAddDialog) {
            DynamicAddMarketItemDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, price, qty, unit ->
                    viewModel.addMarketItem(name = name, price = price, quantity = qty, unit = unit, isChecked = true, inMarket = true)
                    showAddDialog = false
                }
            )
        }

        if (showShareDialog) {
            ShareMarketListDialog(
                marketListId = marketListId,
                syncStatus = syncStatus,
                isFirebaseConfigured = isFirebaseConfigured,
                onDismiss = { showShareDialog = false },
                onStartShare = { listId -> viewModel.startMarketSharing(listId) },
                onStopShare = { viewModel.stopMarketSharing() }
            )
        }
    }
}

@Composable
fun TabPill(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFF2A3634) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color(0xFFDFE3E2) else Color(0xFF81928F),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun PlanningView(viewModel: TesseraViewModel, pendingItems: List<MarketItem>, boughtItems: List<MarketItem>, listState: LazyListState) {
    var newItemText by remember { mutableStateOf("") }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { Spacer(modifier = Modifier.height(130.dp)) } // Spacer for floating top bar
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "Lista Atual",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = Color(0xFFDFE3E2),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (pendingItems.isEmpty()) {
            item {
                Text("Sua lista está vazia.", color = Color(0xFF5E6D6A), modifier = Modifier.padding(bottom = 24.dp))
            }
        } else {
            items(pendingItems, key = { it.id }) { item ->
                PlanningListItem(
                    item = item,
                    onDelete = { viewModel.deleteMarketItem(item) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Histórico Recente",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = Color(0xFFDFE3E2)
                )
                if (boughtItems.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearCompletedMarketItems() }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Limpar Histórico", tint = Color.Gray)
                    }
                }
            }
        }
        
        if (boughtItems.isEmpty()) {
            item {
                Text("Nenhum item comprado recentemente.", color = Color(0xFF5E6D6A))
            }
        } else {
            items(boughtItems, key = { it.id }) { item ->
                BoughtItem(text = item.name)
            }
        }
    }
}

@Composable
fun PlanningListItem(item: MarketItem, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1312))
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF71D7CD)))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = item.name, color = Color(0xFFBDC9C6), fontSize = 16.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Gray)
        }
    }
}

@Composable
fun BoughtItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x08FFFFFF))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF3D4947), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, fontSize = 16.sp, color = Color(0xFF5E6D6A), textDecoration = TextDecoration.LineThrough)
    }
}

@Composable
fun ShoppingView(
    pendingItems: List<MarketItem>, 
    listState: LazyListState,
    onItemToggle: (MarketItem) -> Unit,
    onItemUpdate: (MarketItem, Double, Double, String) -> Unit
) {
    val inCart = pendingItems.filter { it.isChecked }
    val toPick = pendingItems.filter { !it.isChecked }

    var expandedItemId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 180.dp) // Grande espaçamento pra não sumir nada no rodapé
    ) {
        item { Spacer(modifier = Modifier.height(130.dp)) } // Spacer for floating top bar
        
        item { Spacer(modifier = Modifier.height(8.dp)) }
        
        if (toPick.isNotEmpty()) {
            item {
                Text(
                    text = "A PEGAR (${toPick.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF81928F),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                )
            }
            items(toPick, key = { it.id }) { item ->
                ShoppingListItem(
                    item = item, 
                    isExpanded = expandedItemId == item.id,
                    onClick = { expandedItemId = if (expandedItemId == item.id) null else item.id },
                    onToggle = { onItemToggle(item) },
                    onUpdate = { price, qty, unit -> onItemUpdate(item, price, qty, unit) }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        if (inCart.isNotEmpty()) {
            item {
                Text(
                    text = "NO CARRINHO (${inCart.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71D7CD),
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp, top = 16.dp)
                )
            }
            items(inCart, key = { it.id }) { item ->
                ShoppingListItem(
                    item = item, 
                    isExpanded = expandedItemId == item.id,
                    onClick = { expandedItemId = if (expandedItemId == item.id) null else item.id },
                    onToggle = { onItemToggle(item) },
                    onUpdate = { price, qty, unit -> onItemUpdate(item, price, qty, unit) }
                )
            }
        }
        
        if (pendingItems.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum item na lista. Adicione no Planejamento.", color = Color(0xFF5E6D6A), textAlign = TextAlign.Center, modifier = Modifier.padding(40.dp))
                }
            }
        }
    }
}

@Composable
fun ShoppingListItem(
    item: MarketItem, 
    isExpanded: Boolean, 
    onClick: () -> Unit, 
    onToggle: () -> Unit,
    onUpdate: (price: Double, qty: Double, unit: String) -> Unit
) {
    val isChecked = item.isChecked
    val itemTotal = item.price * item.quantity
    val formattedTotal = String.format(Locale("pt", "BR"), "R$ %,.2f", itemTotal)
    val formattedPrice = String.format(Locale("pt", "BR"), "R$ %,.2f", item.price)
    
    val qtyFormat = if (item.quantity % 1.0 == 0.0) {
        String.format(Locale("pt", "BR"), "%.0f", item.quantity)
    } else {
        String.format(Locale("pt", "BR"), "%.3f", item.quantity)
    }

    var tempPrice by remember(item.id) { mutableStateOf(if (item.price > 0) String.format(Locale("pt", "BR"), "%.2f", item.price) else "") }
    var tempQty by remember(item.id) { mutableStateOf(String.format(Locale("pt", "BR"), if(item.quantity % 1.0 == 0.0) "%.0f" else "%.3f", item.quantity)) }
    var tempUnit by remember(item.id) { mutableStateOf(item.unit) }

    val updateValues = {
        val p = parseDoubleSafely(tempPrice)
        val q = if (tempUnit == "kg") {
            parseDoubleSafely(tempQty)
        } else {
            parseDoubleSafely(tempQty).toInt().toDouble()
        }
        onUpdate(p, q, tempUnit)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isChecked) Color(0xFF141918) else Color(0xFF1A2120))
            .border(
                width = 1.dp, 
                color = if (isChecked) Color(0x3371D7CD) else Color(0x1AFFFFFF), 
                shape = RoundedCornerShape(24.dp)
            )
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // ONE-HANDED UX: Tapping ANYWHERE on the row toggles the item, except the edit button
                .clickable { onToggle() }
                .padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isChecked) Color(0xFF71D7CD) else Color(0xFF0F1312))
                    .border(1.dp, if (isChecked) Color(0xFF71D7CD) else Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isChecked,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 18.sp,
                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Bold,
                    color = if (isChecked) Color(0xFF5E6D6A) else Color.White,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
                if (item.price > 0 && !isExpanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$qtyFormat ${item.unit} × $formattedPrice  =  $formattedTotal",
                        fontSize = 13.sp,
                        color = if (isChecked) Color(0xFF3D4947) else Color(0xFF71D7CD),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Dedicated button to expand for Price/Qty editing
            IconButton(
                onClick = { onClick() },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isExpanded) Color(0xFF71D7CD) else Color(0x1AFFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Outlined.Edit,
                    contentDescription = "Editar",
                    tint = if (isExpanded) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Animated Expansion Card Inline
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x0AFFFFFF))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempPrice,
                        onValueChange = { input ->
                            val sanitized = if (input.startsWith("0") && input.length > 1 && input[1].isDigit()) {
                                input.substring(1)
                            } else {
                                input
                            }
                            tempPrice = sanitized
                            updateValues()
                        },
                        label = { Text("Preço Unitário / por Kg", color = Color(0xFF5E6D6A), fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF71D7CD),
                            unfocusedBorderColor = Color(0xFF3D4947),
                            focusedTextColor = Color(0xFFDFE3E2),
                            unfocusedTextColor = Color(0xFFDFE3E2),
                            cursorColor = Color(0xFF71D7CD),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = tempQty,
                        onValueChange = { input ->
                            val sanitized = if (input.startsWith("0") && input.length > 1 && input[1].isDigit()) {
                                input.substring(1)
                            } else {
                                input
                            }
                            tempQty = sanitized
                            updateValues()
                        },
                        label = { Text(if (tempUnit == "kg") "Peso (Kg)" else "Quantidade", color = Color(0xFF5E6D6A), fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF71D7CD),
                            unfocusedBorderColor = Color(0xFF3D4947),
                            focusedTextColor = Color(0xFFDFE3E2),
                            unfocusedTextColor = Color(0xFFDFE3E2),
                            cursorColor = Color(0xFF71D7CD),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Premium Segmented Switch for Unit Selection
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
                            .background(if (tempUnit == "un") Color(0xFF71D7CD).copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (tempUnit == "un") Color(0xFF71D7CD) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable {
                                tempUnit = "un"
                                updateValues()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Unidade", color = if (tempUnit == "un") Color.White else Color(0x66FFFFFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (tempUnit == "kg") Color(0xFF71D7CD).copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (tempUnit == "kg") Color(0xFF71D7CD) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable {
                                tempUnit = "kg"
                                updateValues()
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Kg", color = if (tempUnit == "kg") Color.White else Color(0x66FFFFFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total do item", color = Color(0xFF81928F), fontSize = 14.sp)
                    Text(formattedTotal, color = Color(0xFF71D7CD), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun UnitButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF2A3634) else Color(0x1AFFFFFF))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isSelected) Color.White else Color(0xFF5E6D6A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PlanningBottomBar(viewModel: TesseraViewModel) {
    var newItemText by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFA070909), Color(0xFF070909))))
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 100.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .then(PremiumGlassModifier)
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (newItemText.isNotBlank()) {
                        viewModel.addMarketItem(newItemText)
                        newItemText = ""
                    }
                }),
                placeholder = { Text("Adicionar item...", color = Color(0xFF5E6D6A), fontSize = 16.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Color(0xFF71D7CD)
                ),
                modifier = Modifier.weight(1f)
            )
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF71D7CD))
                    .clickable {
                        if (newItemText.isNotBlank()) {
                            viewModel.addMarketItem(newItemText)
                            newItemText = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.Black)
            }
        }
    }
}

@Composable
fun ShoppingBottomBar(
    cartTotal: Double,
    formattedTotal: String,
    onCheckout: () -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFA070909), Color(0xFF070909))))
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 100.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF141918))
                .border(1.dp, Color(0xFF71D7CD).copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text("Total", color = Color(0xFF81928F), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(formattedTotal, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2A3634), CircleShape)
                        .border(1.dp, Color(0xFF71D7CD).copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Item",
                        tint = Color(0xFF71D7CD)
                    )
                }

                Button(
                    onClick = onCheckout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF71D7CD),
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF2A3634)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    enabled = cartTotal > 0
                ) {
                    Text("FINALIZAR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicAddMarketItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, quantity: Double, unit: String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var productName by remember { mutableStateOf("") }
    var unitType by remember { mutableStateOf("un") } // "un" or "kg"
    var quantityText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1312)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(1.dp, Color(0x3371D7CD), RoundedCornerShape(24.dp))
                .imePadding()
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "DialogStepTransition"
            ) { currentStep ->
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (currentStep) {
                        1 -> {
                            Text(
                                text = "Qual o nome do produto?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            
                            OutlinedTextField(
                                value = productName,
                                onValueChange = { productName = it },
                                placeholder = { Text("Ex: Tomate", color = Color(0xFF5E6D6A)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next,
                                    keyboardType = KeyboardType.Text
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        if (productName.isNotBlank()) {
                                            step = 2
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF71D7CD),
                                    unfocusedBorderColor = Color(0xFF3D4947),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFF71D7CD)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                shape = RoundedCornerShape(12.dp)
                            )
                            
                            // Auto-focus the field when step 1 starts
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onDismiss) {
                                    Text("Cancelar", color = Color(0xFF81928F))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { step = 2 },
                                    enabled = productName.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF71D7CD),
                                        contentColor = Color.Black,
                                        disabledContainerColor = Color(0xFF2A3634)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Avançar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        2 -> {
                            Text(
                                text = "Como é vendido?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Unit Selection
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (unitType == "un") Color(0xFF71D7CD).copy(alpha = 0.15f) else Color(0xFF141918))
                                        .border(
                                            width = 1.dp,
                                            color = if (unitType == "un") Color(0xFF71D7CD) else Color(0x33FFFFFF),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            unitType = "un"
                                            if (quantityText.isBlank()) quantityText = "1"
                                            step = 3
                                        }
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            tint = if (unitType == "un") Color(0xFF71D7CD) else Color(0xFF81928F),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Por Unidade",
                                            fontWeight = FontWeight.Bold,
                                            color = if (unitType == "un") Color.White else Color(0xFF81928F),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                
                                // Weight Selection
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (unitType == "kg") Color(0xFF71D7CD).copy(alpha = 0.15f) else Color(0xFF141918))
                                        .border(
                                            width = 1.dp,
                                            color = if (unitType == "kg") Color(0xFF71D7CD) else Color(0x33FFFFFF),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            unitType = "kg"
                                            if (quantityText.isBlank() || quantityText == "1") quantityText = "0.000"
                                            step = 3
                                        }
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (unitType == "kg") Color(0xFF71D7CD) else Color(0xFF81928F),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "A Quilo (Kg)",
                                            fontWeight = FontWeight.Bold,
                                            color = if (unitType == "kg") Color.White else Color(0xFF81928F),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                TextButton(onClick = { step = 1 }) {
                                    Text("Voltar", color = Color(0xFF81928F))
                                }
                            }
                        }
                        3 -> {
                            Text(
                                text = productName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = quantityText,
                                    onValueChange = { input ->
                                        val sanitized = if (input.startsWith("0") && input.length > 1 && input[1].isDigit()) {
                                            input.substring(1)
                                        } else {
                                            input
                                        }
                                        quantityText = sanitized
                                    },
                                    label = { Text(if (unitType == "kg") "Peso (Kg)" else "Quantidade", color = Color(0xFF81928F), fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF71D7CD),
                                        unfocusedBorderColor = Color(0xFF3D4947),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color(0xFF71D7CD)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                
                                OutlinedTextField(
                                    value = priceText,
                                    onValueChange = { input ->
                                        val sanitized = if (input.startsWith("0") && input.length > 1 && input[1].isDigit()) {
                                            input.substring(1)
                                        } else {
                                            input
                                        }
                                        priceText = sanitized
                                    },
                                    label = { Text("Preço", color = Color(0xFF81928F), fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val qty = parseDoubleSafely(quantityText)
                                            val prc = parseDoubleSafely(priceText)
                                            if (qty > 0 && prc > 0) {
                                                onConfirm(productName, prc, qty, unitType)
                                            }
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF71D7CD),
                                        unfocusedBorderColor = Color(0xFF3D4947),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = Color(0xFF71D7CD)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            
                            // Auto-focus the price field when step 3 starts
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                            
                            val parsedQty = parseDoubleSafely(quantityText)
                            val parsedPrc = parseDoubleSafely(priceText)
                            val totalVal = parsedQty * parsedPrc
                            val formattedTotal = String.format(Locale("pt", "BR"), "R$ %,.2f", totalVal)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total Estimado", color = Color(0xFF81928F), fontSize = 12.sp)
                                    Text(formattedTotal, color = Color(0xFF71D7CD), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { step = 2 }) {
                                        Text("Voltar", color = Color(0xFF81928F))
                                    }
                                    Button(
                                        onClick = {
                                            onConfirm(productName, parsedPrc, parsedQty, unitType)
                                        },
                                        enabled = parsedQty > 0 && parsedPrc > 0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF71D7CD),
                                            contentColor = Color.Black,
                                            disabledContainerColor = Color(0xFF2A3634)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Adicionar", fontWeight = FontWeight.Bold)
                                    }
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
fun ShareMarketListDialog(
    marketListId: String?,
    syncStatus: com.example.data.MarketSyncManager.SyncStatus,
    isFirebaseConfigured: Boolean,
    onDismiss: () -> Unit,
    onStartShare: (String) -> Unit,
    onStopShare: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val shareLink = "https://${com.example.BuildConfig.FIREBASE_PROJECT_ID}.web.app/?listId=${marketListId ?: ""}"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1312)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(1.dp, Color(0x3371D7CD), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sincronização em Tempo Real",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                if (!isFirebaseConfigured) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1AFF0000))
                            .border(1.dp, Color(0x4DFF0000), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "O Firebase não está configurado. Por favor, adicione as chaves FIREBASE_API_KEY, FIREBASE_PROJECT_ID e FIREBASE_APP_ID no seu arquivo .env para habilitar a sincronização em tempo real.",
                            color = Color(0xFFFF6B6B),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2A3634),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Fechar")
                    }
                } else {
                    if (marketListId != null) {
                        // Sincronização Ativa
                        val statusText = when (syncStatus) {
                            com.example.data.MarketSyncManager.SyncStatus.CONNECTING -> "Conectando..."
                            com.example.data.MarketSyncManager.SyncStatus.CONNECTED -> "Conectado"
                            com.example.data.MarketSyncManager.SyncStatus.ERROR -> "Erro de Conexão"
                            else -> "Inativo"
                        }
                        val statusColor = when (syncStatus) {
                            com.example.data.MarketSyncManager.SyncStatus.CONNECTED -> Color(0xFF71D7CD)
                            com.example.data.MarketSyncManager.SyncStatus.CONNECTING -> Color(0xFFFFD54F)
                            com.example.data.MarketSyncManager.SyncStatus.ERROR -> Color(0xFFFF5252)
                            else -> Color(0xFF81928F)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sincronização: $statusText",
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // Link read-only box
                        OutlinedTextField(
                            value = shareLink,
                            onValueChange = {},
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3D4947),
                                unfocusedBorderColor = Color(0xFF3D4947),
                                focusedTextColor = Color(0xFF81928F),
                                unfocusedTextColor = Color(0xFF81928F)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Botão de envio via Intent (WhatsApp/Share Sheet)
                        Button(
                            onClick = {
                                val shareMessage = "Lista de Mercado no Tessera 🛒\nCódigo da Lista: $marketListId\nAcesse pelo link: $shareLink\nou abra pelo app: tessera://market?listId=$marketListId"
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareMessage)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Compartilhar Lista de Mercado"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2), contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compartilhar via WhatsApp / App", fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val annotatedString = androidx.compose.ui.text.buildAnnotatedString { append(shareLink) }
                                    clipboardManager.setText(annotatedString)
                                    android.widget.Toast.makeText(context, "Link copiado!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD), contentColor = Color.Black),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Copiar Link", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onStopShare,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Parar Sinc", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Sincronização Inativa
                        var showEnterCodeField by remember { mutableStateOf(false) }
                        var enteredCode by remember { mutableStateOf("") }

                        Text(
                            text = "Compartilhe sua lista de compras em tempo real com outra pessoa ou conecte-se a uma lista existente digitando o código!",
                            color = Color(0xFFBDC9C6),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = {
                                val uniqueId = java.util.UUID.randomUUID().toString().take(8)
                                onStartShare(uniqueId)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD), contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Gerar Nova Lista Compartilhada", fontWeight = FontWeight.Bold)
                        }

                        if (!showEnterCodeField) {
                            OutlinedButton(
                                onClick = { showEnterCodeField = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF71D7CD))
                            ) {
                                Text("Entrar em Lista Existente por Código", color = Color(0xFF71D7CD), fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = enteredCode,
                                    onValueChange = { enteredCode = it.trim() },
                                    label = { Text("Código da Lista (ex: ABCD1234)", color = Color(0xFF81928F)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF71D7CD),
                                        unfocusedBorderColor = Color(0xFF3D4947)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        if (enteredCode.isNotBlank()) {
                                            onStartShare(enteredCode)
                                            onDismiss()
                                        }
                                    },
                                    enabled = enteredCode.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD), contentColor = Color.Black),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Conectar à Lista", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Voltar", color = Color(0xFF81928F))
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
    onDebit: (accountName: String, amount: Double) -> Unit
) {
    var selectedAccountName by remember { mutableStateOf<String?>(null) }
    var selectedAccountType by remember { mutableStateOf("") } // "bank" ou "benefit"
    var debitAmountText by remember { mutableStateOf(String.format(Locale("pt", "BR"), "%.2f", totalAmount)) }
    var isPartial by remember { mutableStateOf(false) }

    val selectedBalance = remember(selectedAccountName, bankAccounts, benefitCards) {
        when (selectedAccountType) {
            "bank" -> bankAccounts.find { it.name == selectedAccountName }?.balance
            "benefit" -> benefitCards.find { it.name == selectedAccountName }?.balance
            else -> null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF111514))
                .border(1.dp, Color(0xFF71D7CD).copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Finalizar Compra",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Deseja debitar o valor?",
                            color = Color(0xFF81928F),
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF71D7CD).copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = formattedTotal,
                            color = Color(0xFF71D7CD),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Divider(color = Color(0xFF2A3634), thickness = 1.dp)

                // Accounts list
                if (bankAccounts.isNotEmpty()) {
                    Text(
                        text = "CONTAS BANCÁRIAS",
                        color = Color(0xFF81928F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    bankAccounts.forEach { account ->
                        AccountOptionRow(
                            name = account.name,
                            balance = account.balance,
                            colorHex = account.colorHex,
                            isSelected = selectedAccountName == account.name && selectedAccountType == "bank",
                            onClick = {
                                selectedAccountName = account.name
                                selectedAccountType = "bank"
                            }
                        )
                    }
                }

                if (benefitCards.isNotEmpty()) {
                    Text(
                        text = "CARTÕES DE BENEFÍCIO",
                        color = Color(0xFF81928F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    benefitCards.forEach { card ->
                        AccountOptionRow(
                            name = card.name,
                            balance = card.balance,
                            colorHex = card.colorHex,
                            isSelected = selectedAccountName == card.name && selectedAccountType == "benefit",
                            onClick = {
                                selectedAccountName = card.name
                                selectedAccountType = "benefit"
                            }
                        )
                    }
                }

                // Partial debit option
                AnimatedVisibility(visible = selectedAccountName != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Divider(color = Color(0xFF2A3634), thickness = 1.dp)

                        // Toggle total vs partial
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1A2220))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Total" to false, "Parcial" to true).forEach { (label, partial) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isPartial == partial) Color(0xFF71D7CD).copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            isPartial = partial
                                            if (!partial) {
                                                debitAmountText = String.format(Locale("pt", "BR"), "%.2f", totalAmount)
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isPartial == partial) Color(0xFF71D7CD) else Color(0xFF81928F),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Amount field for partial
                        AnimatedVisibility(visible = isPartial) {
                            OutlinedTextField(
                                value = debitAmountText,
                                onValueChange = { debitAmountText = it },
                                label = { Text("Valor a debitar") },
                                prefix = { Text("R$ ", color = Color(0xFF71D7CD)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF71D7CD),
                                    unfocusedBorderColor = Color(0xFF2A3634),
                                    cursorColor = Color(0xFF71D7CD),
                                    focusedLabelColor = Color(0xFF71D7CD),
                                    unfocusedLabelColor = Color(0xFF81928F),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Balance warning
                        if (selectedBalance != null) {
                            val debitValue = parseDoubleSafely(debitAmountText)
                            if (debitValue > (selectedBalance ?: 0.0)) {
                                Text(
                                    text = "⚠️ Saldo insuficiente (${String.format(Locale("pt", "BR"), "R$ %,.2f", selectedBalance)})",
                                    color = Color(0xFFFF5252),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Skip button
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81928F)),
                        border = BorderStroke(1.dp, Color(0xFF2A3634)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text("Pular", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // Debit button
                    Button(
                        onClick = {
                            val accountName = selectedAccountName ?: return@Button
                            val debitValue = parseDoubleSafely(debitAmountText)
                            if (debitValue > 0) {
                                onDebit(accountName, debitValue)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedAccountName != null && parseDoubleSafely(debitAmountText) > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF71D7CD),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF2A3634),
                            disabledContentColor = Color(0xFF5E6D6A)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text("Debitar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountOptionRow(
    name: String,
    balance: Double,
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color(0xFF71D7CD) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFF71D7CD).copy(alpha = 0.08f) else Color(0xFF1A2220))
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) Color(0xFF71D7CD).copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = String.format(Locale("pt", "BR"), "R$ %,.2f", balance),
            color = Color(0xFF81928F),
            fontSize = 13.sp
        )
    }
}
