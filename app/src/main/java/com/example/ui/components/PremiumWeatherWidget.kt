package com.example.ui.components

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

@Composable
fun PremiumWeatherWidget(weatherState: TesseraViewModel.WeatherInfo?) {
    val temp = weatherState?.temp?.toInt() ?: 38
    val rawDesc = weatherState?.description ?: "Summer"
    val city = weatherState?.city

    // Determine display header text
    val headerText = remember(rawDesc) {
        if (rawDesc.contains("Sun", ignoreCase = true) || rawDesc.contains("Clear", ignoreCase = true) || rawDesc.contains("Summer", ignoreCase = true)) {
            "SUMMER"
        } else {
            rawDesc.uppercase()
        }
    }

    // Card background gradient matching glass-card reference
    val cardGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFF101012),
            0.50f to Color(0xFF0F0F11),
            0.75f to Color(0xFF1A0A06),
            1.0f to Color(0xFF5E1603)
        )
    )

    // Gradient for the temperature numbers perfectly matching bottom heat glow
    val tempBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.35f to Color(0xFFE4E4E7),
            0.75f to Color(0xFFDE4C4C),
            1.0f to Color(0xFFF97316)
        )
    )

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

    val shape = RoundedCornerShape(44.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .clip(shape)
            .background(cardGradient)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape),
        contentAlignment = Alignment.TopCenter
    ) {
        // Warm bottom radial & rim glow overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Warm inner glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF5500).copy(alpha = 0.45f),
                        Color(0xFF992200).copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 1.05f),
                    radius = size.width * 0.75f
                )
            )

            // Bright amber rim highlight at bottom edge
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFFAA00).copy(alpha = 0.85f),
                        Color.Transparent
                    )
                ),
                start = Offset(size.width * 0.2f, size.height - 1.5f),
                end = Offset(size.width * 0.8f, size.height - 1.5f),
                strokeWidth = 3f
            )
        }

        // Header Text & Location
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = headerText,
                color = Color(0xFFA1A1AA),
                fontSize = 15.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                letterSpacing = 5.sp
            )

            if (!city.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = city.uppercase(),
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 2.sp
                )
            }
        }

        // Central Dial Arc & Needle Gauge
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerX = size.width / 2f
            val centerY = size.height * 0.52f
            val radius = 105.dp.toPx()
            val numTicks = 45
            val startAngle = -105f
            val endAngle = 105f

            // Draw procedural tick marks
            for (i in 0 until numTicks) {
                val angleDeg = startAngle + (i * (endAngle - startAngle) / (numTicks - 1))
                val angleRad = Math.toRadians((angleDeg - 90).toDouble())

                val innerR = radius - 9.dp.toPx()
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

            // Draw Needle Pointer
            val needleRad = Math.toRadians((animatedAngle - 90).toDouble())
            val needleLength = 112.dp.toPx()
            val needleStartR = 30.dp.toPx()

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

        // Temperature Display Container (Bottom positioned)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            val tempStr = temp.toString()
            val firstDigit = if (tempStr.length > 1) tempStr.substring(0, tempStr.length - 1) else ""
            val secondDigit = if (tempStr.length > 1) tempStr.substring(tempStr.length - 1) else tempStr

            Box(contentAlignment = Alignment.TopEnd) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(end = 18.dp)
                ) {
                    if (firstDigit.isNotEmpty()) {
                        Text(
                            text = firstDigit,
                            fontSize = 135.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            style = TextStyle(
                                brush = tempBrush,
                                letterSpacing = (-0.05).em
                            )
                        )
                    }
                    Text(
                        text = secondDigit,
                        fontSize = 135.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.offset(x = if (firstDigit.isNotEmpty()) (-22).dp else 0.dp),
                        style = TextStyle(
                            brush = tempBrush,
                            letterSpacing = (-0.05).em
                        )
                    )
                }

                // Geometric Degree Symbol
                Box(
                    modifier = Modifier
                        .offset(x = (4).dp, y = (18).dp)
                        .size(18.dp)
                        .border(2.5.dp, Color(0xFF9CA3AF), CircleShape)
                )
            }
        }
    }
}
