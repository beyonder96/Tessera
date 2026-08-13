package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel

@Composable
fun BentoBoxDashboard(
    viewModel: TesseraViewModel,
    isExpanded: Boolean,
    onNavigate: (String) -> Unit
) {
    val itemsAlpha by animateFloatAsState(if (isExpanded) 1f else 0f, tween(400, delayMillis = 50), label = "alpha")
    val itemsOffset by animateDpAsState(if (isExpanded) 0.dp else 60.dp, spring(dampingRatio = 0.8f, stiffness = 150f), label = "offset")

    // Coletando Live Data para a Bento Box
    val healthProfile by viewModel.healthProfile.collectAsState(initial = null)
    val routines by viewModel.allRoutines.collectAsState(initial = emptyList())
    val marketItems by viewModel.pendingMarketItems.collectAsState(initial = emptyList())
    val petEvents by viewModel.allPetEvents.collectAsState(initial = emptyList())
    val wishes by viewModel.allPurchaseGoals.collectAsState(initial = emptyList())
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    // Cálculos rápidos de dados
    val totalRoutines = routines.size
    val marketCount = marketItems.size
    val petEventsCount = petEvents.size
    val activeWishes = wishes.filter { it.currentValue < it.targetValue }.size

    val healthValue = if (healthProfile != null && healthProfile?.targetWeightKg != 0.0) "${healthProfile?.targetWeightKg} kg meta" else "Em dia"

    val balance = transactions.sumOf { if (it.isIncome) it.value else -it.value }
    val balanceStr = String.format("R$ %.2f", balance)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Linha 1: 2 blocos grandes (Saúde e Rotinas)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                BentoTile(
                    title = "Saúde",
                    subtitle = healthValue,
                    icon = Icons.Outlined.MonitorHeart,
                    iconColor = PrimaryTeal,
                    alpha = itemsAlpha,
                    offsetY = itemsOffset,
                    isLarge = true,
                    onClick = { onNavigate("health") }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                BentoTile(
                    title = "Rotinas",
                    subtitle = "$totalRoutines ativas",
                    icon = Icons.Outlined.Flag,
                    iconColor = Color(0xFFF9A826),
                    alpha = itemsAlpha,
                    offsetY = itemsOffset,
                    isLarge = true,
                    onClick = { onNavigate("goals") }
                )
            }
        }

        // Linha 2: 3 blocos pequenos (Meu Apê, Finanças, Pets)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                BentoTile(
                    title = "Apê",
                    subtitle = "$marketCount itens",
                    icon = Icons.Outlined.Construction,
                    iconColor = SecondaryGold,
                    alpha = itemsAlpha,
                    offsetY = itemsOffset,
                    isLarge = false,
                    onClick = { onNavigate("apartment") }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                BentoTile(
                    title = "Finanças",
                    subtitle = balanceStr,
                    icon = Icons.Outlined.AttachMoney,
                    iconColor = Color(0xFF10B981),
                    alpha = itemsAlpha,
                    offsetY = itemsOffset,
                    isLarge = false,
                    onClick = { onNavigate("finance") }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                BentoTile(
                    title = "Petz",
                    subtitle = "$petEventsCount eventos",
                    icon = Icons.Outlined.Pets,
                    iconColor = TertiaryPurple,
                    alpha = itemsAlpha,
                    offsetY = itemsOffset,
                    isLarge = false,
                    onClick = { onNavigate("petz") }
                )
            }
        }

        // Linha 3: 2 blocos mistos (Desejos, Transporte)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                BentoTile(
                    title = "Desejos",
                    subtitle = "$activeWishes salvos",
                    icon = Icons.Outlined.Star,
                    iconColor = Color(0xFFF9A826),
                    alpha = itemsAlpha,
                    offsetY = itemsOffset,
                    isLarge = false,
                    onClick = { onNavigate("wishes") }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                BentoTile(
                    title = "Transporte",
                    subtitle = "Ao vivo",
                    icon = Icons.Outlined.DirectionsBus,
                    iconColor = Color(0xFF4FC3F7),
                    alpha = itemsAlpha,
                    offsetY = itemsOffset,
                    isLarge = false,
                    onClick = { onNavigate("transport") }
                )
            }
        }
    }
}

@Composable
fun BentoTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    alpha: Float,
    offsetY: Dp,
    isLarge: Boolean,
    onClick: () -> Unit
) {
    val height = if (isLarge) 140.dp else 110.dp
    
    Box(
        modifier = Modifier
            .offset(y = offsetY)
            .alpha(alpha)
            .height(height)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(themedSubtleBackground())
            .border(1.dp, themedSubtleBorder(), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = if (isLarge) Arrangement.SpaceBetween else Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            if (isLarge) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f))
                            .border(1.dp, iconColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Column {
                    Text(
                        text = subtitle,
                        color = iconColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Serif
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f))
                        .border(1.dp, iconColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = iconColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1
                )
            }
        }
    }
}
