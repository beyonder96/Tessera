package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Restaurant
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
import com.example.data.BenefitCard
import com.example.data.Transaction
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.bounceClick
import com.example.viewmodel.TesseraViewModel
import com.example.ui.theme.PrimaryTeal
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenefitHubScreen(
    cardName: String,
    viewModel: TesseraViewModel,
    onBack: () -> Unit
) {
    val benefitCards by viewModel.allBenefitCards.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val card = benefitCards.find { it.name == cardName }

    val ptBR = remember { Locale("pt", "BR") }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(ptBR) }

    if (card == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cartão de benefício não encontrado", color = Color.White)
        }
        return
    }

    val cardTransactions = allTransactions.filter { it.accountOrCardName == cardName }
    val displayTransactions = remember(cardTransactions) {
        cardTransactions.sortedByDescending { it.timestamp }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D0D))) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Gerenciamento de Benefício", 
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
                    BenefitSummaryCard(card = card, currencyFormat = currencyFormat)
                }

                item {
                    Text(
                        "Extrato do Benefício",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (displayTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp).then(PremiumGlassModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Nenhuma transação neste benefício.", color = Color.White.copy(alpha=0.5f), fontSize = 14.sp)
                        }
                    }
                } else {
                    items(displayTransactions) { tx ->
                        BenefitTransactionItem(tx, currencyFormat)
                    }
                }
            }
        }
    }
}

@Composable
fun BenefitSummaryCard(card: BenefitCard, currencyFormat: NumberFormat) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(card.colorHex))
    } catch (e: Exception) {
        PrimaryTeal
    }

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
                    Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = cardColor, modifier = Modifier.size(20.dp))
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
                Text("Saldo Disponível", fontSize = 13.sp, color = Color.White.copy(alpha=0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(currencyFormat.format(card.balance), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Titular", fontSize = 13.sp, color = Color.White.copy(alpha=0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(card.holderName, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = cardColor)
            }
        }
    }
}

@Composable
fun BenefitTransactionItem(transaction: Transaction, currencyFormat: NumberFormat) {
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
