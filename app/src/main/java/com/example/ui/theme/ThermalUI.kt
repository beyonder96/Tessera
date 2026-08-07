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

val DeepBlack = Color(0xFF050505)
val GlassTop = Color(0x661E1E1E) // rgba(30, 30, 30, 0.4)
val GlassBottom = Color(0xCC0F0F0F) // rgba(15, 15, 15, 0.8)
val ThermalBorderGlass = Color(0x0DFFFFFF) // rgba(255, 255, 255, 0.05)
val HighlightTop = Color(0x26FFFFFF) // Brilho interno branco
val ThermalGlow = Color(0x26FF4600) // Brilho interno térmico (laranja/vermelho)

val ThermalGradientBrush = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFEC4899), // Pink
        Color(0xFFF97316)  // Orange
    )
)

/**
 * Modifier customizado para aplicar o "Thermal UI" em containers e widgets.
 * Aplica sombra externa, fundo de vidro (gradient), borda sutil e brilhos internos (inset shadows simulados).
 */
fun Modifier.thermalCard(
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 20.dp
): Modifier = composed {
    this
        .shadow(
            elevation = elevation,
            shape = RoundedCornerShape(cornerRadius),
            spotColor = Color.Black,
            ambientColor = Color.Black
        )
        .background(
            brush = Brush.verticalGradient(listOf(GlassTop, GlassBottom)),
            shape = RoundedCornerShape(cornerRadius)
        )
        .border(
            width = 1.dp,
            color = ThermalBorderGlass,
            shape = RoundedCornerShape(cornerRadius)
        )
        .drawWithCache {
            onDrawWithContent {
                drawContent() // Desenha o conteúdo original do Card
                
                // Brilho Branco no topo (Inset shadow simulado)
                drawLine(
                    color = HighlightTop,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 3f
                )
                
                // Brilho Térmico na base vazando de baixo para cima
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, ThermalGlow),
                        startY = size.height * 0.4f, // Começa do meio para baixo
                        endY = size.height
                    ),
                    blendMode = BlendMode.SrcOver
                )
            }
        }
}
