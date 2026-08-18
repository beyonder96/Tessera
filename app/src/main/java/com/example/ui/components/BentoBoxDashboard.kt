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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel

private data class ModuleItem(
    val title: String,
    val subtitle: String,
    val route: String,
    val icon: ImageVector,
    val iconColor: Color
)

@Composable
fun BentoBoxDashboard(
    viewModel: TesseraViewModel,
    isExpanded: Boolean,
    onNavigate: (String) -> Unit
) {
    val itemsAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "bento_alpha"
    )
    val itemsOffset by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "bento_offset"
    )

    val modules = remember {
        listOf(
            ModuleItem("Finanças", "Saldo & Contas", "finance", Icons.Outlined.AccountBalanceWallet, Color(0xFF10B981)),
            ModuleItem("Saúde", "Passos & Sono", "health", Icons.Outlined.MonitorHeart, PrimaryTeal),
            ModuleItem("Transporte", "Metrô & Ônibus", "transport", Icons.Outlined.DirectionsBus, Color(0xFF38BDF8)),
            ModuleItem("Rotinas", "Hábitos & Metas", "goals", Icons.Outlined.Flag, Color(0xFFF97316)),
            ModuleItem("Mercado", "Lista de Compras", "market", Icons.Outlined.ShoppingCart, Color(0xFF34D399)),
            ModuleItem("Desejos", "Planejamento", "wishes", Icons.Outlined.BookmarkBorder, Color(0xFFF59E0B)),
            ModuleItem("Meu Apê", "Reforma & Custos", "apartment", Icons.Outlined.Construction, SecondaryGold),
            ModuleItem("Petz", "Cuidados & Vacinas", "petz", Icons.Outlined.Pets, TertiaryPurple)
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = itemsOffset)
            .alpha(itemsAlpha)
    ) {
        modules.chunked(2).forEach { rowModules ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowModules.forEach { module ->
                    Box(modifier = Modifier.weight(1f)) {
                        ModernBentoModuleTile(
                            title = module.title,
                            subtitle = module.subtitle,
                            icon = module.icon,
                            iconColor = module.iconColor,
                            onClick = { onNavigate(module.route) }
                        )
                    }
                }
                if (rowModules.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ModernBentoModuleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "pressScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.14f))
                    .border(0.5.dp, iconColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1
            )
        }
    }
}
