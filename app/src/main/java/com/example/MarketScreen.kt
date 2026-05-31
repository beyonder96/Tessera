package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val pendingItems by viewModel.pendingMarketItems.collectAsStateWithLifecycle()
    val boughtItems by viewModel.boughtMarketItems.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Planejamento, 1 = Modo Compras
    
    val cartTotal = pendingItems.filter { it.isChecked }.sumOf { it.price * it.quantity }
    val formattedTotal = String.format(Locale("pt", "BR"), "R$ %,.2f", cartTotal)

    Scaffold(
        containerColor = Color(0xFF070909),
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            MarketTopBar(
                onHomeClick = onHomeClick, 
                selectedTab = selectedTab, 
                onTabSelected = { selectedTab = it },
                cartTotal = formattedTotal,
                onCheckout = { viewModel.checkoutCart() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }, label = "TabTransition"
            ) { targetTab ->
                if (targetTab == 0) {
                    PlanningView(viewModel, pendingItems, boughtItems)
                } else {
                    ShoppingView(
                        pendingItems = pendingItems,
                        onItemToggle = { viewModel.toggleMarketItemChecked(it) },
                        onItemUpdate = { item, price, qty, unit -> 
                            viewModel.updateMarketItemDetails(item, price, qty, unit)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketTopBar(onHomeClick: () -> Unit, selectedTab: Int, onTabSelected: (Int) -> Unit, cartTotal: String, onCheckout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF070909))
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Mercado",
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
                        contentDescription = "Voltar",
                        tint = Color(0xFFBDC9C6)
                    )
                }
            },
            actions = {
                if (selectedTab == 1) {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF71D7CD))
                            .clickable { onCheckout() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Finalizar", color = Color(0xFF070909), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            )
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141918))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabPill(
                text = "Planejamento",
                isSelected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            )
            TabPill(
                text = "No Mercado",
                isSelected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            )
        }
        
        // Ilha Dinâmica de Total
        AnimatedVisibility(visible = selectedTab == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .then(PremiumGlassModifier)
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total do Carrinho", color = Color(0xFFBDC9C6), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(cartTotal, color = Color(0xFFDFE3E2), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
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
fun PlanningView(viewModel: TesseraViewModel, pendingItems: List<MarketItem>, boughtItems: List<MarketItem>) {
    var newItemText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .then(PremiumGlassModifier)
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                TextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newItemText.isNotBlank()) {
                            viewModel.addMarketItem(newItemText)
                            newItemText = ""
                        }
                    }),
                    placeholder = {
                        Text(
                            text = "Adicionar item (Ex: Leite)...",
                            color = Color(0xFF5E6D6A),
                            fontSize = 16.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFFDFE3E2),
                        unfocusedTextColor = Color(0xFFDFE3E2)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

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
    onItemToggle: (MarketItem) -> Unit,
    onItemUpdate: (MarketItem, Double, Double, String) -> Unit
) {
    val inCart = pendingItems.filter { it.isChecked }
    val toPick = pendingItems.filter { !it.isChecked }

    var expandedItemId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
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

    var tempPrice by remember(item.price) { mutableStateOf(if (item.price > 0) String.format(Locale("pt", "BR"), "%.2f", item.price) else "") }
    var tempQty by remember(item.quantity) { mutableStateOf(String.format(Locale("pt", "BR"), if(item.quantity % 1.0 == 0.0) "%.0f" else "%.3f", item.quantity)) }
    var tempUnit by remember(item.unit) { mutableStateOf(item.unit) }

    val updateValues = {
        val p = tempPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
        val q = tempQty.replace(",", ".").toDoubleOrNull() ?: 0.0
        onUpdate(p, q, tempUnit)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(if (isChecked) PremiumGlassModifier else Modifier.background(Color(0xFF141918)))
            .border(
                width = 1.dp, 
                color = if (isChecked) Color(0x3371D7CD) else Color(0x1AFFFFFF), 
                shape = RoundedCornerShape(20.dp)
            )
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isChecked) Color(0xFF71D7CD) else Color.Transparent)
                    .border(2.dp, if (isChecked) Color(0xFF71D7CD) else Color(0xFF3D4947), CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF070909), modifier = Modifier.size(18.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 18.sp,
                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                    color = if (isChecked) Color(0xFF81928F) else Color(0xFFDFE3E2),
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
                if (item.price > 0 && !isExpanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$qtyFormat ${item.unit} × $formattedPrice",
                        fontSize = 13.sp,
                        color = Color(0xFF5E6D6A)
                    )
                }
            }
            
            if (itemTotal > 0 && !isExpanded) {
                Text(
                    text = formattedTotal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isChecked) Color(0xFFDFE3E2) else Color(0xFFBDC9C6)
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
                        onValueChange = { 
                            tempPrice = it
                            updateValues()
                        },
                        label = { Text("R$", color = Color(0xFF5E6D6A), fontSize = 12.sp) },
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
                        onValueChange = { 
                            tempQty = it
                            updateValues()
                        },
                        label = { Text("Qtd", color = Color(0xFF5E6D6A), fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.8f),
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

                    Column(
                        modifier = Modifier.weight(0.7f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        UnitButton(text = "un", isSelected = tempUnit == "un") { 
                            tempUnit = "un"
                            updateValues()
                        }
                        UnitButton(text = "kg", isSelected = tempUnit == "kg") { 
                            tempUnit = "kg"
                            updateValues()
                        }
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
