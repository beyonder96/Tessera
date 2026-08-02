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
import com.example.viewmodel.TesseraViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsCentralModal(
    viewModel: TesseraViewModel,
    onDismiss: () -> Unit,
    onAddManual: () -> Unit
) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    
    val subscriptions = remember(allTransactions) {
        allTransactions.filter { !it.isIncome && (it.category == "Stream" || it.category == "Assinaturas/Streams") }
    }
    
    val totalCost = remember(subscriptions) {
        subscriptions.sumOf { it.value }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF070909)
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
                        text = "Central de Assinaturas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row {
                        IconButton(onClick = onAddManual) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar Assinatura", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
                
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
                            
                            val logoUrl = when {
                                title.contains("spotify") -> "https://storage.googleapis.com/pr-newsroom-wp/1/2018/11/Spotify_Logo_RGB_Green.png"
                                title.contains("netflix") -> "https://images.ctfassets.net/y2ske730sjqp/1aONibCke6niZhgPxuiilC/2c401b05a07288746ddf3bd3943f176c/BrandAssets_Logos_01-Wordmark.jpg?w=940"
                                title.contains("prime") || title.contains("amazon") -> "https://m.media-amazon.com/images/G/01/primevideo/seo/primevideo-seo-logo.png"
                                title.contains("disney") -> "https://cnbl-cdn.bamgrid.com/assets/7ecc8bcb60ad77193058d63e321bd21cbac2fc67281dbd9927676ea4a4c83594/original"
                                else -> null
                            }

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
                                            model = logoUrl,
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
