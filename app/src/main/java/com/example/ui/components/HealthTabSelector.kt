package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryTeal

enum class HealthSubTab(val title: String) {
    GENERAL("Geral"),
    NUTRI("Nutri")
}

@Composable
fun HealthTabSelector(
    selectedTab: HealthSubTab,
    onTabSelected: (HealthSubTab) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = PrimaryTeal
) {
    val tabs = HealthSubTab.values()
    val tabCoords = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    val density = LocalDensity.current
    val defaultWidth = with(density) { 72.dp.toPx() }
    val defaultX = 0f

    val targetX = tabCoords[selectedIndex]?.first ?: defaultX
    val targetW = tabCoords[selectedIndex]?.second ?: defaultWidth

    // Animação líquida com mola para transição suave idêntica ao Petz
    val animX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 240f
        ),
        label = "healthLiquidX"
    )

    val animW by animateFloatAsState(
        targetValue = targetW,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = 240f
        ),
        label = "healthLiquidW"
    )

    val distance = targetX - animX
    val stretch = (distance * 0.18f).coerceIn(-30f, 30f)

    Box(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(30.dp))
            .background(themedNavBarBackground())
            .border(0.5.dp, themedNavBarBorder(), RoundedCornerShape(30.dp))
            .padding(4.dp)
    ) {
        val indicatorWidthDp = with(density) { (animW + kotlin.math.abs(stretch)).toDp() }
        val indicatorOffsetDp = with(density) { (animX + if (stretch < 0f) stretch else 0f).toDp() }

        Box(
            modifier = Modifier
                .offset(x = indicatorOffsetDp, y = 0.dp)
                .width(indicatorWidthDp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(26.dp))
                .background(themedSubtleBackground())
                .border(
                    width = 1.dp,
                    color = themedSubtleBorder(),
                    shape = RoundedCornerShape(26.dp)
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            tabs.forEachIndexed { index, tabItem ->
                val isSelected = tabItem == selectedTab
                Text(
                    text = tabItem.title,
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            val parent = coords.parentLayoutCoordinates
                            if (parent != null) {
                                val position = coords.positionInParent()
                                tabCoords[index] = Pair(position.x, coords.size.width.toFloat())
                            }
                        }
                        .clip(RoundedCornerShape(26.dp))
                        .clickable { onTabSelected(tabItem) }
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}
