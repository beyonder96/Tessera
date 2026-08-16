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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.composed
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api

// Importações da biblioteca de desfoque (Glassmorphism real)
val LocalGlassmorphismLevel = staticCompositionLocalOf { "Frosted" }

// ── Theme-Aware Color Helpers ──

/** Whether the current theme is dark */
@Composable
fun isDarkTheme(): Boolean = LocalAppTheme.current == "dark"

/** Subtle card background */
@Composable
fun themedCardBackground(): Color = if (isDarkTheme()) Color(0xFF111218) else Color(0xFFFFFFFF)

/** Subtle card border */
@Composable
fun themedCardBorder(): Color = if (isDarkTheme()) Color(0xFF27272A) else Color(0xFFE2E8F0)

/** Overlay/dialog background */
@Composable
fun themedOverlayBackground(): Color = if (isDarkTheme()) Color(0xFF111218).copy(alpha = 0.98f) else Color(0xFFFFFFFF).copy(alpha = 0.98f)

/** Overlay/dialog border gradient colors */
@Composable
fun themedOverlayBorderColors(): List<Color> = if (isDarkTheme())
    listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))
else
    listOf(Color(0xFF0F172A).copy(alpha = 0.12f), Color(0xFF0F172A).copy(alpha = 0.04f))

/** Very subtle background */
@Composable
fun themedSubtleBackground(): Color = if (isDarkTheme()) Color.White.copy(alpha = 0.04f) else Color(0xFF0F172A).copy(alpha = 0.04f)

/** Very subtle border */
@Composable
fun themedSubtleBorder(): Color = if (isDarkTheme()) Color.White.copy(alpha = 0.08f) else Color(0xFF0F172A).copy(alpha = 0.08f)

/** Thin line divider */
@Composable
fun themedDivider(): Color = if (isDarkTheme()) Color(0x1AFFFFFF) else Color(0x140F172A)

/** Button/outlined border */
@Composable
fun themedButtonBorder(): Color = if (isDarkTheme()) Color.White.copy(alpha = 0.2f) else Color(0xFF0F172A).copy(alpha = 0.15f)

/** Full-screen scrim */
@Composable
fun themedScrim(): Color = if (isDarkTheme()) Color(0xCC000000) else Color(0x66000000)

/** Header bar background */
@Composable
fun themedHeaderBackground(): Color = if (isDarkTheme()) Color(0xCC090A0F) else Color(0xCCF8FAFC)

/** Bottom nav bar pill background */
@Composable
fun themedNavBarBackground(): Color = if (isDarkTheme()) Color(0xFF161722).copy(alpha = 0.95f) else Color(0xFFFFFFFF).copy(alpha = 0.95f)

/** Bottom nav bar pill border */
@Composable
fun themedNavBarBorder(): Color = if (isDarkTheme()) Color(0xFF27272A) else Color(0xFFE2E8F0)

/** Inactive icon tint */
@Composable
fun themedInactiveIcon(): Color = if (isDarkTheme()) Color(0xFF71717A) else Color(0xFF64748B)

/** TextField colors for forms */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun themedTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = if (isDarkTheme()) Color(0xFF131817) else Color(0xFFF1F3F5),
    unfocusedContainerColor = if (isDarkTheme()) Color(0xFF131817) else Color(0xFFF1F3F5),
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
)

/** OutlinedTextField colors for forms */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun themedOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = if (isDarkTheme()) Color.White.copy(alpha = 0.2f) else Color(0xFF0F172A).copy(alpha = 0.15f),
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary
)

/** Switch colors */
@Composable
fun themedSwitchColors(checkedTrackColor: Color = MaterialTheme.colorScheme.primary) = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = checkedTrackColor,
    uncheckedThumbColor = if (isDarkTheme()) Color.White.copy(alpha = 0.7f) else Color(0xFF94A3B8),
    uncheckedTrackColor = if (isDarkTheme()) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)
)

/** Checkbox colors */
@Composable
fun themedCheckboxColors() = CheckboxDefaults.colors(
    checkedColor = MaterialTheme.colorScheme.primary,
    uncheckedColor = if (isDarkTheme()) Color.White.copy(alpha = 0.4f) else Color(0xFF94A3B8),
    checkmarkColor = Color.White
)

/** Gradient overlay for images */
@Composable
fun themedImageGradientOverlay(): List<Color> = if (isDarkTheme())
    listOf(Color.Transparent, Color(0xCC000000))
else
    listOf(Color.Transparent, Color(0xCCFFFFFF))



// Shared GlassModifier for premium cards (Liquid Glass - Theme Aware)
val PremiumGlassModifier: Modifier
    get() = Modifier.composed {
        val level = LocalGlassmorphismLevel.current
        val appTheme = LocalAppTheme.current
        val isDark = appTheme == "dark"
        
        val (bgColors, borderColors) = if (isDark) {
            when (level) {
                "Clear" -> listOf(Color(0xFF141416).copy(alpha = 0.8f), Color(0xFF050506).copy(alpha = 0.9f)) to listOf(Color(0xFF333333), Color(0xFFFF5E00).copy(alpha = 0.3f))
                "Blur" -> listOf(Color(0xFF141416), Color(0xFF050506)) to listOf(Color(0xFF333333), Color(0xFFFF5E00).copy(alpha = 0.5f))
                else -> listOf(Color(0xFF1A1A1C), Color(0xFF0A0A0C)) to listOf(Color(0xFF333333), Color(0xFFFF5E00).copy(alpha = 0.4f))
            }
        } else {
            when (level) {
                "Clear" -> listOf(Color(0xB3FFFFFF), Color(0x80F8F9FA)) to listOf(Color(0x400F172A), Color(0x1A0F172A))
                "Blur" -> listOf(Color(0xDCFFFFFF), Color(0xAAF8F9FA)) to listOf(Color(0x330F172A), Color(0x140F172A))
                else -> listOf(Color(0xC8FFFFFF), Color(0x90F8F9FA)) to listOf(Color(0x2B0F172A), Color(0x100F172A))
            }
        }
        
        val modifier = this.clip(RoundedCornerShape(24.dp))
        
        modifier
            .background(Brush.verticalGradient(colors = bgColors))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(colors = borderColors),
                shape = RoundedCornerShape(24.dp)
            )
            .drawWithContent {
                drawContent()
                val glowColor = if (isDark) Color(0xFFFF5E00).copy(alpha = 0.15f) else Color(0xFF0F172A).copy(alpha = 0.04f)
                val glowBrush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(size.width / 2, size.height),
                    radius = size.width * 0.8f
                )
                // Draw bottom glow
                drawRect(
                    brush = glowBrush,
                    topLeft = Offset(0f, 0f),
                    size = size
                )
            }
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
@Composable
fun rememberLavaBrush(): Brush {
    val isDark = MaterialTheme.colorScheme.background == Color.Black || MaterialTheme.colorScheme.background == Color(0xFF000000)
    
    val infiniteTransition = rememberInfiniteTransition(label = "lavaLamp")
    
    val targetColor1 = if (isDark) Color(0xFFFF5722) else Color(0xFF00BCD4) // Thermal: Deep Orange | Frosty: Cyan
    val targetColor2 = if (isDark) Color(0xFFFFC107) else Color(0xFF81D4FA) // Thermal: Amber | Frosty: Light Blue
    
    val targetColor3 = if (isDark) Color(0xFFFF0000) else Color(0xFF03A9F4) // Thermal: Red | Frosty: Light Blue
    val targetColor4 = if (isDark) Color(0xFFFF9800) else Color(0xFF4FC3F7) // Thermal: Orange | Frosty: Blue
    
    val color1 by infiniteTransition.animateColor(
        initialValue = targetColor1,
        targetValue = targetColor2,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = targetColor3,
        targetValue = targetColor4,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color2"
    )

    return Brush.linearGradient(
        colors = listOf(color1, color2),
        start = Offset(0f, 0f),
        end = Offset(500f, 500f)
    )
}
