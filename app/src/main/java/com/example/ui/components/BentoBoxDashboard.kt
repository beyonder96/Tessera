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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel

private data class ModuleItem(
    val title: String,
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
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val itemsOffset by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 30.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 200f),
        label = "offset"
    )

    val modules = remember {
        listOf(
            ModuleItem("Finanças", "finance", Icons.Outlined.AccountBalanceWallet, Color(0xFF10B981)),
            ModuleItem("Desejos", "wishes", Icons.Outlined.BookmarkBorder, Color(0xFFF59E0B)),
            ModuleItem("Transporte", "transport", Icons.Outlined.DirectionsBus, Color(0xFF38BDF8)),
            ModuleItem("Saúde", "health", Icons.Outlined.MonitorHeart, PrimaryTeal),
            ModuleItem("Rotinas", "goals", Icons.Outlined.Flag, Color(0xFFF97316)),
            ModuleItem("Meu Apê", "apartment", Icons.Outlined.Construction, SecondaryGold),
            ModuleItem("Petz", "petz", Icons.Outlined.Pets, TertiaryPurple),
            ModuleItem("Mercado", "market", Icons.Outlined.ShoppingCart, Color(0xFF34D399))
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
                        MinimalModuleTile(
                            title = module.title,
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
private fun MinimalModuleTile(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            maxLines = 1
        )
    }
}
