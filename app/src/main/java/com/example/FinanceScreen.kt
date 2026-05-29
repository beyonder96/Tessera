package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(onHomeClick: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Patrimônio",
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
                    containerColor = Color(0xCC0F1414),
                    scrolledContainerColor = Color(0xCC0F1414),
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
            
            Text(
                text = "R$ 45.230,00",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                color = Color(0xFFDFE3E2)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            EvolutionSection()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AccountsSection()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            RecentTransactionsSection()
            
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun EvolutionSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Evolução",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    color = Color(0xFFDFE3E2)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Últimos 6 meses",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFBDC9C6)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Selecionar período",
                        tint = Color(0xFFBDC9C6),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Grid Lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        Divider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                    }
                }
                
                // Bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val barHeights = listOf(0.4f, 0.5f, 0.45f, 0.6f, 0.75f, 0.85f)
                    val barColors = listOf(
                        Color(0x3371D7CD),
                        Color(0x3371D7CD),
                        Color(0x3371D7CD),
                        Color(0x3371D7CD),
                        Color(0x3371D7CD),
                        Color(0x8071D7CD)
                    )
                    
                    barHeights.forEachIndexed { index, height ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(height)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColors[index])
                        )
                    }
                }
                
                // "R$45k" label for the last bar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = (180 * 0.85).dp + 8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1B2120), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "R$45k",
                            color = Color(0xFF71D7CD),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun").forEach { month ->
                    Text(
                        text = month,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0x99BDC9C6),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AccountsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Contas",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            color = Color(0xFFDFE3E2),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        AccountItem(
            icon = Icons.Outlined.AccountBalance,
            title = "Conta Corrente",
            subtitle = "Banco Principal",
            value = "R$ 12.450,00",
            iconColor = Color(0xFF71D7CD),
            iconBgColor = Color(0xFF262B2A)
        )
        
        AccountItem(
            icon = Icons.AutoMirrored.Outlined.TrendingUp,
            title = "Investimentos",
            subtitle = "Corretora Alpha",
            value = "R$ 32.780,00",
            iconColor = Color(0xFFD7BAFF),
            iconBgColor = Color(0xFF262B2A)
        )
    }
}

@Composable
fun AccountItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: String,
    iconColor: Color,
    iconBgColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        color = Color(0xFFDFE3E2)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFBDC9C6)
                    )
                }
            }
            
            Text(
                text = value,
                fontSize = 16.sp,
                color = Color(0xFFDFE3E2)
            )
        }
    }
}

@Composable
fun RecentTransactionsSection() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transações Recentes",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                color = Color(0xFFDFE3E2)
            )
            
            Text(
                text = "Ver todas",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF71D7CD)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x08FFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
        ) {
            Column {
                TransactionItem(
                    icon = Icons.Outlined.ShoppingCart,
                    title = "Supermercado",
                    subtitle = "Hoje, 14:30",
                    value = "- R$ 450,00",
                    iconColor = Color(0xFFBDC9C6),
                    iconBgColor = Color(0xFF262B2A),
                    valueColor = Color(0xFFDFE3E2)
                )
                
                Divider(color = Color(0x1A879391))
                
                TransactionItem(
                    icon = Icons.Outlined.Restaurant,
                    title = "Restaurante Lazer",
                    subtitle = "Ontem, 20:15",
                    value = "- R$ 120,50",
                    iconColor = Color(0xFFBDC9C6),
                    iconBgColor = Color(0xFF262B2A),
                    valueColor = Color(0xFFDFE3E2)
                )
                
                Divider(color = Color(0x1A879391))
                
                TransactionItem(
                    icon = Icons.Outlined.Payments,
                    title = "Salário",
                    subtitle = "10 Jun, 09:00",
                    value = "+ R$ 15.000,00",
                    iconColor = Color(0xFF71D7CD),
                    iconBgColor = Color(0x1A71D7CD),
                    valueColor = Color(0xFF71D7CD)
                )
            }
        }
    }
}

@Composable
fun TransactionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: String,
    iconColor: Color,
    iconBgColor: Color,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = Color(0xFFDFE3E2)
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFBDC9C6)
                )
            }
        }
        
        Text(
            text = value,
            fontSize = 16.sp,
            color = valueColor
        )
    }
}
