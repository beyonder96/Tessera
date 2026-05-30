package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.TesseraViewModel
import com.example.data.MarketItem
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val pendingItems by viewModel.pendingMarketItems.collectAsStateWithLifecycle()
    val boughtItems by viewModel.boughtMarketItems.collectAsStateWithLifecycle()
    
    var newItemText by remember { mutableStateOf("") }
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
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
            
            // Add Item Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(Color(0xFF171D1C))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF3D4947),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                    )
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Adicionar",
                                tint = Color(0xFFBDC9C6),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Adicionar item...",
                                color = Color(0xFFBDC9C6),
                                fontSize = 16.sp
                            )
                        }
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
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Pendente Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Pendente",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    color = Color(0xFFDFE3E2)
                )
                Text(
                    text = "3 ITENS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF71D7CD),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x08FFFFFF))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            ) {
                Column {
                    pendingItems.forEachIndexed { index, item ->
                        MarketListItem(
                            text = item.name,
                            checked = item.isChecked,
                            onClick = { viewModel.toggleMarketItemChecked(item) },
                            onMarkBought = { viewModel.markMarketItemBought(item) }
                        )
                        if (index < pendingItems.size - 1) {
                            HorizontalDivider(color = Color(0x1AFFFFFF))
                        }
                    }
                    if (pendingItems.isEmpty()) {
                        Text("Nenhum item pendente", color = Color(0xFFBDC9C6), modifier = Modifier.padding(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sugeridos Frequentes Section
            Text(
                text = "SUGERIDOS FREQUENTES",
                fontSize = 14.sp,
                color = Color(0xCCBDC9C6),
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SuggestItem(icon = Icons.Outlined.Egg, label = "Ovos")
                }
                Box(modifier = Modifier.weight(1f)) {
                    SuggestItem(icon = Icons.Outlined.BakeryDining, label = "Pão")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SuggestItem(icon = Icons.Outlined.WaterDrop, label = "Água")
                }
                Box(modifier = Modifier.weight(1f)) {
                    SuggestItem(icon = Icons.Outlined.LocalFlorist, label = "Vegetais")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Comprado Recente Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = Color(0xCCBDC9C6),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "COMPRADO RECENTE",
                    fontSize = 14.sp,
                    color = Color(0xCCBDC9C6),
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x08FFFFFF))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            ) {
                Column {
                    boughtItems.forEachIndexed { index, item ->
                        BoughtItem(text = item.name)
                        if (index < boughtItems.size - 1) {
                            HorizontalDivider(color = Color(0x1AFFFFFF))
                        }
                    }
                    if (boughtItems.isEmpty()) {
                        Text("Sem compras recentes", color = Color(0xFFBDC9C6), modifier = Modifier.padding(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun MarketListItem(text: String, checked: Boolean, onClick: () -> Unit, onMarkBought: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (checked) Color(0x1A71D7CD) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (checked) Color(0xFF71D7CD) else Color(0xFF3D4947),
                    shape = RoundedCornerShape(2.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFF71D7CD))
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = text,
            fontSize = 16.sp,
            color = if (checked) Color(0xFFBDC9C6) else Color(0xFFDFE3E2),
            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )
        
        if (checked) {
            androidx.compose.material3.IconButton(onClick = onMarkBought) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.ShoppingCartCheckout,
                    contentDescription = "Confirmar compra",
                    tint = Color(0xFF71D7CD)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = "Mais opções",
                tint = Color(0x80BDC9C6)
            )
        }
    }
}

@Composable
fun SuggestItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .clickable { }
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF71D7CD),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFBDC9C6)
            )
        }
    }
}

@Composable
fun BoughtItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF71D7CD),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color(0x80DFE3E2),
            textDecoration = TextDecoration.LineThrough
        )
    }
}
