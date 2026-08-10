package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class WeatherEffectType {
    SUN, RAIN, THUNDER, CLOUDS, SNOW, WIND, NONE
}

@Composable
fun WeatherParticleEffects(
    description: String,
    modifier: Modifier = Modifier
) {
    val effectType = remember(description) {
        val lower = description.lowercase()
        when {
            lower.contains("thunder") || lower.contains("storm") -> WeatherEffectType.THUNDER
            lower.contains("rain") || lower.contains("drizzle") -> WeatherEffectType.RAIN
            lower.contains("snow") || lower.contains("ice") -> WeatherEffectType.SNOW
            lower.contains("wind") || lower.contains("breeze") -> WeatherEffectType.WIND
            lower.contains("cloud") || lower.contains("fog") || lower.contains("haze") || lower.contains("mist") -> WeatherEffectType.CLOUDS
            lower.contains("clear") || lower.contains("sun") || lower.contains("summer") -> WeatherEffectType.SUN
            else -> WeatherEffectType.NONE
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "weatherAnim")
    
    // Global time state for continuous rendering
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        when (effectType) {
            WeatherEffectType.SUN -> drawAuraSolar(time)
            WeatherEffectType.RAIN -> drawGlassRain(time)
            WeatherEffectType.THUNDER -> drawCinematicLightning(time)
            WeatherEffectType.CLOUDS -> drawVolumetricFog(time)
            WeatherEffectType.SNOW -> drawIceDust(time)
            WeatherEffectType.WIND -> drawKineticWaves(time)
            WeatherEffectType.NONE -> { /* No effect */ }
        }
    }
}

// 1. Ensolarado (Aura Solar)
private fun DrawScope.drawAuraSolar(time: Float) {
    val pulse = (sin(time * 0.05f) + 1f) / 2f
    val glowRadius = size.width * 0.4f + (pulse * size.width * 0.1f)
    val coreRadius = size.width * 0.15f
    val centerOffset = Offset(size.width * 0.5f, size.height * 0.4f)
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFF9900).copy(alpha = 0.3f),
                Color(0xFFFF3300).copy(alpha = 0.1f),
                Color.Transparent
            ),
            center = centerOffset,
            radius = glowRadius
        ),
        center = centerOffset,
        radius = glowRadius,
        blendMode = BlendMode.Screen
    )

    drawCircle(
        color = Color(0xFFFFCC00).copy(alpha = 0.8f + (pulse * 0.2f)),
        center = centerOffset,
        radius = coreRadius,
        blendMode = BlendMode.Screen
    )
}

// 2. Chuva (Fios de Vidro)
private fun DrawScope.drawGlassRain(time: Float) {
    val dropCount = 40
    val angle = PI / 4 // 45 degrees
    val dropSpeed = 15f
    
    for (i in 0 until dropCount) {
        val seed = i * 137.5f
        val startX = (seed * 11) % (size.width * 2) - size.width
        val startY = ((seed * 17) + time * dropSpeed * (1f + (i%5)*0.2f)) % size.height
        val length = 40f + (i % 10) * 5f
        
        val endX = startX + length * cos(angle).toFloat()
        val endY = startY + length * sin(angle).toFloat()
        val alpha = (1f - (startY / size.height)).coerceIn(0f, 0.8f)
        
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF88CCFF).copy(alpha = 0f),
                    Color(0xFFFFFFFF).copy(alpha = alpha),
                    Color(0xFF88CCFF).copy(alpha = 0f)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen
        )
    }
}

// 3. Tempestade / Raios
private fun DrawScope.drawCinematicLightning(time: Float) {
    val fastTime = time * 20f
    val flashVal = sin(fastTime) * cos(fastTime * 2.3f) * sin(fastTime * 1.7f)
    
    if (flashVal > 0.85f) {
        val intensity = (flashVal - 0.85f) / 0.15f
        
        drawRect(
            color = Color(0xFF66AAFF).copy(alpha = intensity * 0.2f),
            size = size,
            blendMode = BlendMode.Screen
        )
        
        val path = Path()
        var currentX = size.width * (0.3f + 0.4f * (fastTime % 1f))
        var currentY = 0f
        path.moveTo(currentX, currentY)
        
        while (currentY < size.height) {
            currentY += size.height / 5f
            currentX += (Random(currentY.toInt()).nextFloat() - 0.5f) * size.width * 0.3f
            path.lineTo(currentX, currentY)
        }
        
        drawPath(
            path = path,
            color = Color(0xFFFFFFFF).copy(alpha = intensity),
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
            blendMode = BlendMode.Screen
        )
        drawPath(
            path = path,
            color = Color(0xFF8833FF).copy(alpha = intensity * 0.5f),
            style = Stroke(width = 20f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
            blendMode = BlendMode.Screen
        )
    }
}

// 4. Nublado (Neblina Volumétrica)
private fun DrawScope.drawVolumetricFog(time: Float) {
    val cloudCount = 4
    val speed = 0.5f
    
    for (i in 0 until cloudCount) {
        val scale = 1f + (i * 0.3f)
        val cloudRadius = size.width * 0.6f * scale
        
        val moveX = (time * speed * (if(i%2==0) 1f else 0.7f) + (i * size.width * 0.5f)) % (size.width * 2) - size.width * 0.5f
        val moveY = size.height * 0.1f + (i * size.height * 0.2f)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF444455).copy(alpha = 0.3f),
                    Color(0xFF883311).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(moveX, moveY),
                radius = cloudRadius
            ),
            center = Offset(moveX, moveY),
            radius = cloudRadius,
            blendMode = BlendMode.Screen
        )
    }
}

// 5. Neve (Poeira de Gelo)
private fun DrawScope.drawIceDust(time: Float) {
    val particleCount = 60
    val speed = 2f
    
    for (i in 0 until particleCount) {
        val seed = i * 997f
        val startX = ((seed * 13) + sin(time * 0.1f + i) * 50f) % size.width
        val startY = ((seed * 19) + time * speed * (0.5f + (i%5)*0.2f)) % size.height
        
        val meltAlpha = (1f - (startY / size.height)).coerceIn(0f, 1f)
        val radius = 2f + (i % 4)
        
        drawCircle(
            color = Color(0xFFBBFFFF).copy(alpha = meltAlpha * 0.7f),
            center = Offset(startX, startY),
            radius = radius,
            blendMode = BlendMode.Screen
        )
    }
}

// 6. Vento / Ventania
private fun DrawScope.drawKineticWaves(time: Float) {
    val waveCount = 5
    val speed = 10f
    
    for (i in 0 until waveCount) {
        val startY = size.height * (0.2f + i * 0.15f)
        val waveOffset = (time * speed * (1f + i*0.2f)) % (size.width * 2)
        
        val path = Path()
        path.moveTo(waveOffset - size.width, startY)
        
        var x = waveOffset - size.width
        while(x < waveOffset) {
            val y = startY + sin(x * 0.02f + time * 0.1f) * 20f
            path.lineTo(x, y)
            x += 20f
        }
        
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFFFFFF).copy(alpha = 0.2f),
                    Color(0xFFFFFFFF).copy(alpha = 0.5f),
                    Color.Transparent
                ),
                start = Offset(waveOffset - size.width, startY),
                end = Offset(waveOffset, startY)
            ),
            start = Offset(waveOffset - size.width, startY),
            end = Offset(waveOffset, startY),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
            blendMode = BlendMode.Screen
        )
    }
}
