package com.example.ui.components
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.viewmodel.TesseraViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsCentralModal(
    viewModel: TesseraViewModel,
    onDismiss: () -> Unit,
    onAddManual: () -> Unit
) {
    val accounts by viewModel.allBankAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
    val cards by viewModel.allCreditCards.collectAsStateWithLifecycle(initialValue = emptyList())
    val coroutineScope = rememberCoroutineScope()
    
    var isAddingSubscription by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newValueStr by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf("") }
    var dueDayStr by remember { mutableStateOf("") }

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val subscriptions = remember(allTransactions) {
        allTransactions
            .filter { !it.isIncome && (it.category == "Stream" || it.category == "Assinaturas/Streams") }
            .distinctBy { it.title.lowercase().trim() }
    }
    
    val totalCost = remember(subscriptions) {
        subscriptions.sumOf { it.value }
    }
    
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 16.dp, top = 40.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAddingSubscription) "Nova Assinatura" else "Central de Assinaturas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row {
                        if (!isAddingSubscription) {
                            IconButton(onClick = { isAddingSubscription = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Adicionar Assinatura", tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                        IconButton(onClick = { 
                            if (isAddingSubscription) isAddingSubscription = false else onDismiss() 
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                
                if (isAddingSubscription) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Nome do Serviço (ex: Netflix)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF71D7CD),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = Color(0xFF71D7CD),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        
                        OutlinedTextField(
                            value = newValueStr,
                            onValueChange = { newValueStr = it },
                            label = { Text("Valor Mensal (R$)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF71D7CD),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = Color(0xFF71D7CD),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                            )
                        )

                        Text("Conta ou Cartão para Débito", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp)) {
                            items(accounts.map { it.name } + cards.map { it.name }) { name ->
                                val isSelected = name == selectedAccount
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF71D7CD).copy(alpha = 0.2f) else Color.Transparent)
                                        .border(1.dp, if (isSelected) Color(0xFF71D7CD) else Color.White.copy(alpha=0.1f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedAccount = name },
                                        colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Color(0xFF71D7CD))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name, color = Color.White)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = dueDayStr,
                            onValueChange = { dueDayStr = it },
                            label = { Text("Dia do Vencimento (1-31)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF71D7CD),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedLabelColor = Color(0xFF71D7CD),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = {
                                val value = newValueStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                                val day = dueDayStr.toIntOrNull()?.coerceIn(1, 31) ?: 1
                                val calendar = java.util.Calendar.getInstance()
                                calendar.set(java.util.Calendar.DAY_OF_MONTH, day)
                                viewModel.addTransaction(
                                    title = newTitle,
                                    subtitle = "Assinatura Automática",
                                    value = value,
                                    isIncome = false,
                                    category = "Assinaturas/Streams",
                                    accountOrCardName = selectedAccount,
                                    isRealized = false,
                                    isRecurrent = true,
                                    recurrenceInterval = "Mensal",
                                    dueDate = calendar.timeInMillis,
                                    customTimestamp = calendar.timeInMillis
                                )
                                isAddingSubscription = false
                                newTitle = ""
                                newValueStr = ""
                                dueDayStr = ""
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                        ) {
                            Text("Salvar Assinatura", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Summary Card
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth().then(PremiumGlassModifier),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("CUSTO MENSAL TOTAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE94057), letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %,.2f", totalCost),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${subscriptions.size} assinaturas ativas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (subscriptions.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("Nenhuma assinatura encontrada na categoria Stream.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                }
                            }
                        } else {
                            items(subscriptions) { tx ->
                                val title = tx.title.lowercase()
                                val (logoColor, logoText) = when {
                                    title.contains("spotify") -> Color(0xFF1DB954) to "Sp"
                                    title.contains("netflix") -> Color(0xFFE50914) to "Ne"
                                    title.contains("prime") || title.contains("amazon") -> Color(0xFF00A8E1) to "Pr"
                                    title.contains("youtube") -> Color(0xFFFF0000) to "Yt"
                                    title.contains("apple") -> Color(0xFF555555) to "Ap"
                                    title.contains("disney") -> Color(0xFF113CCF) to "Di"
                                    title.contains("hbo") || title.contains("max") -> Color(0xFF5A25D6) to "Ma"
                                    title.contains("gympass") || title.contains("wellhub") -> Color(0xFFE51D2A) to "Wh"
                                    else -> Color(0xFF71D7CD) to tx.title.take(2).capitalize(Locale.ROOT)
                                }
                                
                                val domain = when {
                                    title.contains("spotify") -> "spotify.com"
                                    title.contains("netflix") -> "netflix.com"
                                    title.contains("prime") || title.contains("amazon") -> "primevideo.com"
                                    title.contains("youtube") -> "youtube.com"
                                    title.contains("apple") -> "apple.com"
                                    title.contains("disney") -> "disneyplus.com"
                                    title.contains("hbo") || title.contains("max") -> "max.com"
                                    title.contains("gympass") || title.contains("wellhub") -> "wellhub.com"
                                    title.contains("globo") || title.contains("globoplay") -> "globo.com"
                                    else -> null
                                }
                                val logoUrl = domain?.let { "https://logo.clearbit.com/$it" }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(if (logoUrl != null) Color.White else logoColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (logoUrl != null) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(logoUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Inside,
                                                modifier = Modifier.padding(4.dp).fillMaxSize()
                                            )
                                        } else {
                                            Text(logoText, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(tx.accountOrCardName.ifBlank { "Cobrança automática" }, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                    
                                    Text(
                                        text = String.format(Locale("pt", "BR"), "R$ %,.2f", tx.value),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 16.sp,
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
}
