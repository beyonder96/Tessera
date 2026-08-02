package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.composed

// Importações da biblioteca de desfoque (Glassmorphism real)
val LocalGlassmorphismLevel = staticCompositionLocalOf { "Frosted" }

// Shared GlassModifier for premium cards (Liquid Glass - Theme Aware)
val PremiumGlassModifier: Modifier
    get() = Modifier.composed {
        val level = LocalGlassmorphismLevel.current
        val appTheme = LocalAppTheme.current
        val isDark = appTheme == "dark"
        
        val (bgColors, borderColors) = if (isDark) {
            when (level) {
                "Clear" -> listOf(Color(0x12FFFFFF), Color(0x02FFFFFF)) to listOf(Color(0x8CFFFFFF), Color(0x0AFFFFFF))
                "Blur" -> listOf(Color(0x2BFFFFFF), Color(0x08FFFFFF)) to listOf(Color(0x59FFFFFF), Color(0x08FFFFFF))
                else -> listOf(Color(0x4D000000), Color(0x33000000)) to listOf(Color(0x26FFFFFF), Color(0x05FFFFFF))
            }
        } else {
            when (level) {
                "Clear" -> listOf(Color(0xB3FFFFFF), Color(0x80F8F9FA)) to listOf(Color(0x400F172A), Color(0x1A0F172A))
                "Blur" -> listOf(Color(0xDCFFFFFF), Color(0xAAF8F9FA)) to listOf(Color(0x330F172A), Color(0x140F172A))
                else -> listOf(Color(0xC8FFFFFF), Color(0x90F8F9FA)) to listOf(Color(0x2B0F172A), Color(0x100F172A))
            }
        }
        
        val modifier = this.clip(RoundedCornerShape(28.dp))
        
        modifier
            .background(Brush.verticalGradient(colors = bgColors))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(colors = borderColors),
                shape = RoundedCornerShape(28.dp)
            )
    }

@Composable
fun OuraCircularProgress(
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 4.dp.value,
    centerContent: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            drawArc(
                color = progressColor.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        centerContent()
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}