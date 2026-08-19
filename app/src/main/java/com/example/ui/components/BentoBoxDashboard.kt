package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BentoBoxDashboard(
    viewModel: TesseraViewModel,
    isExpanded: Boolean,
    onNavigate: (String) -> Unit
) {
    val itemsAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "bento_alpha"
    )
    val itemsOffset by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 20.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
        label = "bento_offset"
    )

    // Observar dados em tempo real dos módulos
    val transactions by viewModel.allTransactions.collectAsState()
    val healthProfile by viewModel.healthProfile.collectAsState()
    val mealRecords by viewModel.allMealRecords.collectAsState()
    val marketItems by viewModel.pendingMarketItems.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val purchaseGoals by viewModel.allPurchaseGoals.collectAsState()
    val petEvents by viewModel.allPetEvents.collectAsState()
    val metroStatus by viewModel.metroStatus.collectAsState()
    val busLines by viewModel.savedBusLines.collectAsState()

    // Métricas calculadas para micro-glance dos módulos
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayCalories = remember(mealRecords, todayStr) {
        mealRecords.filter { it.date == todayStr }.sumOf { it.calories }.toInt()
    }
    val calGoal = healthProfile?.dailyCalorieGoal?.toInt() ?: 2000

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val currentBalance = remember(transactions) {
        transactions.sumOf { if (it.isIncome) it.value else -it.value }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = itemsOffset)
            .alpha(itemsAlpha)
    ) {
        // ROW 1: Finanças (Hero Tile Largo com Balanço)
        BentoHeroWideTile(
            title = "Finanças",
            subtitle = "Saldo em Contas & Cartões",
            metricText = currencyFormatter.format(currentBalance),
            badgeText = "${transactions.size} transações",
            icon = Icons.Outlined.AccountBalanceWallet,
            accentColor = Color(0xFF10B981),
            onClick = { onNavigate("finance") }
        )

        // ROW 2: Saúde & Nutrição + Transporte SP (Dois Bento Tiles Principais)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Saúde & Nutri
            BentoMediumTile(
                title = "Saúde & Nutri",
                subtitle = if (todayCalories > 0) "$todayCalories / $calGoal kcal" else "Diário Nutricional",
                badgeText = "Nutri & IMC",
                icon = Icons.Outlined.MonitorHeart,
                accentColor = PrimaryTeal,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("health") }
            )

            // Transporte
            BentoMediumTile(
                title = "Transporte",
                subtitle = if (busLines.isNotEmpty()) "${busLines.size} linhas salvas" else "Metrô & Ônibus",
                badgeText = "SP Tempo Real",
                icon = Icons.Outlined.DirectionsTransit,
                accentColor = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("transport") }
            )
        }

        // ROW 3: Rotinas & Mercado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rotinas & Metas
            BentoMediumTile(
                title = "Rotinas",
                subtitle = if (habits.isNotEmpty()) "${habits.size} hábitos ativos" else "Hábitos diários",
                badgeText = "Hábitos",
                icon = Icons.Outlined.Flag,
                accentColor = Color(0xFFF97316),
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("goals") }
            )

            // Lista de Mercado
            BentoMediumTile(
                title = "Mercado",
                subtitle = if (marketItems.isNotEmpty()) "${marketItems.size} itens pendentes" else "Lista de compras",
                badgeText = "Compras",
                icon = Icons.Outlined.ShoppingCart,
                accentColor = Color(0xFF34D399),
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("market") }
            )
        }

        // ROW 4: Desejos, Petz & Meu Apê (Grid Compacto de 3 colunas)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BentoCompactTile(
                title = "Desejos",
                subtitle = "${purchaseGoals.size} metas",
                icon = Icons.Outlined.BookmarkBorder,
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("wishes") }
            )

            BentoCompactTile(
                title = "Petz",
                subtitle = if (petEvents.isNotEmpty()) "${petEvents.size} cuidados" else "Cuidados",
                icon = Icons.Outlined.Pets,
                accentColor = TertiaryPurple,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("petz") }
            )

            BentoCompactTile(
                title = "Meu Apê",
                subtitle = "Reforma",
                icon = Icons.Outlined.Construction,
                accentColor = SecondaryGold,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate("apartment") }
            )
        }
    }
}

@Composable
private fun BentoHeroWideTile(
    title: String,
    subtitle: String,
    metricText: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "heroScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = accentColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = metricText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Acessar →",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun BentoMediumTile(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "mediumScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(13.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(0.5.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(themedSubtleBackground())
                        .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BentoCompactTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "compactScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
