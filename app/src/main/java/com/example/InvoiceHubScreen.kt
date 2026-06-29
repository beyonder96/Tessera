package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CreditCard
import com.example.data.Transaction
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.bounceClick
import com.example.viewmodel.TesseraViewModel
import com.example.ui.theme.PrimaryTeal
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceHubScreen(
    cardName: String,
    viewModel: TesseraViewModel,
    onBack: () -> Unit
) {
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val card = creditCards.find { it.name == cardName }

    val ptBR = remember { Locale("pt", "BR") }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(ptBR) }

    if (card == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cartão não encontrado", color = Color.White)
        }
        return
    }

    // A fatura é calculada pelas transações com o nome do cartão
    val cardTransactions = allTransactions.filter { it.accountOrCardName == cardName }
    
    var showPayInvoiceDialog by remember { mutableStateOf(false) }

    if (showPayInvoiceDialog) {
        AlertDialog(
            onDismissRequest = { showPayInvoiceDialog = false },
            title = { Text("Pagar Fatura", color = Color.White) },
            text = { Text("Deseja zerar a fatura atual de ${currencyFormat.format(card.usedLimit)}?", color = Color.White.copy(alpha=0.7f)) },
            containerColor = Color(0xFF1E1E1E),
            confirmButton = {
                TextButton(onClick = {
                    viewModel.payInvoice(card.id)
                    showPayInvoiceDialog = false
                }) {
                    Text("Pagar", color = PrimaryTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayInvoiceDialog = false }) {
                    Text("Cancelar", color = Color.White.copy(alpha=0.7f))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Gerenciamento de Fatura", 
                            fontFamily = FontFamily.SansSerif, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 20.sp, 
                            color = Color.White
                        ) 
                    },
                    navigationIcon = { 
                        IconButton(onClick = onBack, modifier = Modifier.bounceClick { onBack() }) { 
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White.copy(alpha = 0.8f)) 
                        } 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp)
            ) {
                item {
                    InvoiceSummaryCard(card = card, currencyFormat = currencyFormat)
                }

                item {
                    Button(
                        onClick = { showPayInvoiceDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).bounceClick {
                            showPayInvoiceDialog = true
                        }
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pagar Fatura", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Text(
                        "Transações do Cartão",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (cardTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp).then(PremiumGlassModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhuma transação neste cartão.", color = Color.White.copy(alpha=0.5f), fontSize = 14.sp)
                        }
                    }
                } else {
                    items(cardTransactions) { tx ->
                        CardTransactionItem(tx, currencyFormat)
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceSummaryCard(card: CreditCard, currencyFormat: NumberFormat) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(card.colorHex))
    } catch (e: Exception) {
        PrimaryTeal
    }

    val availableLimit = card.limit - card.usedLimit
    val usagePercentage = if (card.limit > 0) (card.usedLimit / card.limit).toFloat().coerceIn(0f, 1f) else 0f

    Column(
        modifier = PremiumGlassModifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(cardColor.copy(alpha=0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = cardColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(card.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Terminado em ${card.numberLastFour}", fontSize = 12.sp, color = Color.White.copy(alpha=0.6f))
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Fatura Atual", fontSize = 13.sp, color = Color.White.copy(alpha=0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(currencyFormat.format(card.usedLimit), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Limite Disponível", fontSize = 13.sp, color = Color.White.copy(alpha=0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(currencyFormat.format(availableLimit), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = cardColor)
            }
        }

        // Progress bar
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(
                progress = { usagePercentage },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = cardColor,
                trackColor = Color(0x33FFFFFF)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Usado: ${(usagePercentage * 100).toInt()}%", fontSize = 11.sp, color = Color.White.copy(alpha=0.5f))
                Text("Total: ${currencyFormat.format(card.limit)}", fontSize = 11.sp, color = Color.White.copy(alpha=0.5f))
            }
        }
    }
}

@Composable
fun CardTransactionItem(transaction: Transaction, currencyFormat: NumberFormat) {
    val isIncome = transaction.isIncome
    val amountColor = if (isIncome) Color(0xFF81C784) else Color.White

    Row(
        modifier = PremiumGlassModifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x0AFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Payment,
                    contentDescription = null,
                    tint = Color.White.copy(alpha=0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = transaction.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.category,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha=0.5f)
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isIncome) "+" else "-"} ${currencyFormat.format(transaction.value)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
