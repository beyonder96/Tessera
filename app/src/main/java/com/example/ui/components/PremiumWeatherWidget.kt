package com.example.ui.components
import androidx.compose.material3.MaterialTheme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.viewmodel.TesseraViewModel
import kotlin.math.cos
import kotlin.math.sin
import com.example.ui.theme.thermalCard

@Composable
fun PremiumWeatherWidget(weatherState: TesseraViewModel.WeatherInfo?) {
    val temp = weatherState?.temp?.toInt() ?: 23
    val rawDesc = weatherState?.description ?: "Parcialmente Nublado"
    val city = weatherState?.city ?: "São Paulo"

    // Determine display header text
    val headerText = remember(rawDesc) {
        if (rawDesc.contains("Sun", ignoreCase = true) || rawDesc.contains("Clear", ignoreCase = true) || rawDesc.contains("Summer", ignoreCase = true)) {
            "SUMMER"
        } else {
            rawDesc.uppercase()
        }
    }

    // Thermal UI - Card background gradient
    val cardGradient = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF141416),
                Color(0xFF050506)
            )
        )
    }

    // Thermal UI - Gradient for the temperature numbers
    val tempBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFF5E00),
                Color(0xFFCC1100)
            )
        )
    }

    val glowRadialInner = Color(0xFFFF5E00)
    val glowRadialOuter = Color(0xFF990000)
    val glowLine = Color(0xFFFF3300)

    // Animated pointer needle angle (mapping temp 0..50°C to -105°..+105°)
    val targetAngle = remember(temp) {
        val clampedTemp = temp.coerceIn(0, 50)
        -105f + (clampedTemp / 50f) * 210f
    }
    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "needleAngle"
    )

    val shape = RoundedCornerShape(56.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .thermalCard(cornerRadius = 56.dp, elevation = 16.dp)
    ) {
        // Warm bottom radial & rim glow overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowRadialInner.copy(alpha = 0.35f),
                        glowRadialOuter.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.9f),
                    radius = size.width * 0.5f
                )
            )

            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        glowLine.copy(alpha = 0.75f),
                        Color.Transparent
                    )
                ),
                start = Offset(size.width * 0.2f, size.height - 1.5f),
                end = Offset(size.width * 0.8f, size.height - 1.5f),
                strokeWidth = 3f
            )
        }
        
        // Native Particle Weather Animation
        WeatherParticleEffects(
            description = rawDesc,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Text Area (Condition, City, Temp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = headerText,
                    color = Color(0xFFA1A1AA),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp
                )

                if (city.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = city.uppercase(),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(contentAlignment = Alignment.TopEnd) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 14.dp)
                    ) {
                        Text(
                            text = temp.toString(),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            style = TextStyle(
                                brush = tempBrush,
                                letterSpacing = (-0.04).em
                            )
                        )
                    }

                    // Geometric Degree Symbol
                    Box(
                        modifier = Modifier
                            .offset(x = (2).dp, y = (10).dp)
                            .size(12.dp)
                            .border(2.dp, Color(0xFF9CA3AF), CircleShape)
                    )
                }
            }

            // Right Dial Gauge Display
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val radius = 45.dp.toPx()
                    val numTicks = 31
                    val startAngle = -110f
                    val endAngle = 110f

                    for (i in 0 until numTicks) {
                        val angleDeg = startAngle + (i * (endAngle - startAngle) / (numTicks - 1))
                        val angleRad = Math.toRadians((angleDeg - 90).toDouble())

                        val innerR = radius - 6.dp.toPx()
                        val outerR = radius

                        val startX = centerX + innerR * cos(angleRad).toFloat()
                        val startY = centerY + innerR * sin(angleRad).toFloat()
                        val endX = centerX + outerR * cos(angleRad).toFloat()
                        val endY = centerY + outerR * sin(angleRad).toFloat()

                        drawLine(
                            color = Color(0xFF52525B),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Needle Pointer
                    val needleRad = Math.toRadians((animatedAngle - 90).toDouble())
                    val needleLength = 48.dp.toPx()
                    val needleStartR = 12.dp.toPx()

                    val nStartX = centerX + needleStartR * cos(needleRad).toFloat()
                    val nStartY = centerY + needleStartR * sin(needleRad).toFloat()
                    val nEndX = centerX + needleLength * cos(needleRad).toFloat()
                    val nEndY = centerY + needleLength * sin(needleRad).toFloat()

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color(0xFFE4E4E7)),
                            start = Offset(nStartX, nStartY),
                            end = Offset(nEndX, nEndY)
                        ),
                        start = Offset(nStartX, nStartY),
                        end = Offset(nEndX, nEndY),
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
