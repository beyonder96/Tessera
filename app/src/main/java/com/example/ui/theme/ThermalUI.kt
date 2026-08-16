package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val DeepBlack = Color(0xFF090A0F)
val GlassTopDark = Color(0x661E1E28) // rgba(30, 30, 40, 0.4)
val GlassBottomDark = Color(0xCC0F0F16) // rgba(15, 15, 22, 0.8)
val ThermalBorderGlassDark = Color(0x26FFFFFF) // rgba(255, 255, 255, 0.15)
val HighlightTopDark = Color(0x26FFFFFF) // Brilho interno branco
val ThermalGlowDark = Color(0x26FF4600) // Brilho interno térmico (laranja/vermelho)

val GlassTopLight = Color(0xFFFFFFFF)
val GlassBottomLight = Color(0xFFF1F5F9)
val ThermalBorderGlassLight = Color(0xFFE2E8F0)
val HighlightTopLight = Color(0x80FFFFFF)
val ThermalGlowLight = Color(0x080F172A)

val ThermalGradientBrush = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFEC4899), // Pink
        Color(0xFFF97316)  // Orange
    )
)

/**
 * Modifier customizado para aplicar o "Thermal UI" em containers e widgets, sensível ao tema.
 * No modo escuro aplica glow térmico e vidro escuro; no modo claro aplica acabamento de camadas neutras claras e sombra suave.
 */
fun Modifier.thermalCard(
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 12.dp
): Modifier = composed {
    val isDark = LocalAppTheme.current == "dark"
    
    val bgBrush = if (isDark) {
        Brush.verticalGradient(listOf(GlassTopDark, GlassBottomDark))
    } else {
        Brush.verticalGradient(listOf(GlassTopLight, GlassBottomLight))
    }
    
    val borderColor = if (isDark) ThermalBorderGlassDark else ThermalBorderGlassLight
    val spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0x180F172A)
    val ambientColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color(0x0C0F172A)
    val highlightColor = if (isDark) HighlightTopDark else HighlightTopLight
    val glowColor = if (isDark) ThermalGlowDark else ThermalGlowLight

    this
        .shadow(
            elevation = if (isDark) elevation else (elevation / 2).coerceAtLeast(4.dp),
            shape = RoundedCornerShape(cornerRadius),
            spotColor = spotColor,
            ambientColor = ambientColor
        )
        .background(
            brush = bgBrush,
            shape = RoundedCornerShape(cornerRadius)
        )
        .border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(cornerRadius)
        )
        .drawWithCache {
            onDrawWithContent {
                drawContent() // Desenha o conteúdo original do Card
                
                // Brilho no topo (Inset shadow simulado)
                drawLine(
                    color = highlightColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
                
                // Brilho na base vazando de baixo para cima
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, glowColor),
                        startY = size.height * 0.4f,
                        endY = size.height
                    ),
                    blendMode = BlendMode.SrcOver
                )
            }
        }
}
