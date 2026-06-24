package com.example

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
    val boughtItems by viewModel.boughtMarketItems.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Planejamento, 1 = Modo Compras
    
    val cartTotal = pendingItems.filter { it.isChecked }.sumOf { it.price * it.quantity }
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
                        ShoppingBottomBar(cartTotal, formattedTotal, onCheckout = { viewModel.checkoutCart() })
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding() // Ensures list resizes when keyboard opens
            ) {
                Spacer(modifier = Modifier.height(72.dp))

                // Elegant Segmented Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF141918))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(32.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabPill(
                        text = "Planejamento",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabPill(
                        text = "No Mercado",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    }, label = "TabTransition"
                ) { targetTab ->
                    if (targetTab == 0) {
                        PlanningView(viewModel, pendingItems, boughtItems, planningListState)
                    } else {
                        ShoppingView(
                            pendingItems = pendingItems,
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
                            text = "MERCADO",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                    
                    // Checkout button removed from top bar, it is now in the bottom bar for one-handed use
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
                        text = "MERCADO",
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
                PlanningListItem(item = item)
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        item {
            Text(
                text = "Histórico Recente",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = Color(0xFFDFE3E2),
                modifier = Modifier.padding(bottom = 16.dp)
            )
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
fun PlanningListItem(item: MarketItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1312))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF71D7CD)))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = item.name, color = Color(0xFFBDC9C6), fontSize = 16.sp, modifier = Modifier.weight(1f))
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
            .padding(horizontal = 24.dp, vertical = 16.dp)
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
fun ShoppingBottomBar(cartTotal: Double, formattedTotal: String, onCheckout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFA070909), Color(0xFF070909))))
            .padding(horizontal = 24.dp, vertical = 16.dp)
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
