package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.sp
import com.example.viewmodel.TesseraViewModel

@Composable
fun PremiumWeatherWidget(weatherState: TesseraViewModel.WeatherInfo?) {
    val temp = weatherState?.temp?.toInt() ?: 18
    val desc = weatherState?.description ?: "Clear Sky"

    val isDay = desc.contains("Clear") || desc.contains("Sun") || desc.contains("Cloud")
    val gradientColors = if (isDay) {
        listOf(Color(0xFF4FC3F7).copy(alpha = 0.2f), Color(0xFF29B6F6).copy(alpha = 0.05f))
    } else {
        listOf(Color(0xFF7E57C2).copy(alpha = 0.2f), Color(0xFF5E35B1).copy(alpha = 0.05f))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "WeatherAnim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        // Decorative Canvas Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f * pulseAlpha),
                radius = size.height * 1.5f,
                center = Offset(size.width, 0f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CLIMA ATUAL",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$temp°",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = desc.uppercase(),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = weatherState?.city ?: "São Paulo, SP",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                val isRainy = desc.contains("Rain", ignoreCase = true) || desc.contains("Drizzle", ignoreCase = true) || desc.contains("Shower", ignoreCase = true)
                val isCloudy = desc.contains("Cloud", ignoreCase = true) || desc.contains("Overcast", ignoreCase = true)
                val isNight = desc.contains("Night", ignoreCase = true) || (!desc.contains("Sun", ignoreCase = true) && !desc.contains("Clear", ignoreCase = true) && !isCloudy && !isRainy)

                when {
                    isRainy -> AnimatedRain(Modifier.size(64.dp))
                    isCloudy -> AnimatedClouds(Modifier.size(64.dp))
                    isNight -> AnimatedMoon(Modifier.size(64.dp))
                    else -> AnimatedSun(Modifier.size(64.dp))
                }
            }
        }
    }
}

@Composable
fun AnimatedSun(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun_pulse"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_alpha"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_rotation"
    )

    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotation }) {
        // Heat waves
        drawCircle(
            color = Color(0xFFFFD54F).copy(alpha = alpha),
            radius = (size.minDimension / 2.2f) * scale
        )
        // Core sun
        drawCircle(
            color = Color(0xFFFFCA28),
            radius = size.minDimension / 2.5f
        )
        // Sun rays
        for (i in 0 until 8) {
            val angle = i * 45f
            val rad = Math.toRadians(angle.toDouble())
            val innerRadius = size.minDimension / 2.2f
            val outerRadius = size.minDimension / 1.7f
            drawLine(
                color = Color(0xFFFFCA28),
                start = Offset(
                    (center.x + innerRadius * Math.cos(rad)).toFloat(),
                    (center.y + innerRadius * Math.sin(rad)).toFloat()
                ),
                end = Offset(
                    (center.x + outerRadius * Math.cos(rad)).toFloat(),
                    (center.y + outerRadius * Math.sin(rad)).toFloat()
                ),
                strokeWidth = size.minDimension / 15f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
fun AnimatedMoon(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "moon")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moon_glow"
    )

    Canvas(modifier = modifier) {
        // Glow
        drawCircle(
            color = Color(0xFFE0E0E0).copy(alpha = glowAlpha),
            radius = size.minDimension / 2 * 0.9f
        )
        // Moon body
        drawCircle(
            color = Color(0xFFF5F5F5),
            radius = size.minDimension / 2 * 0.7f
        )
        // Craters
        drawCircle(
            color = Color(0x1A000000), 
            radius = size.minDimension / 8f,
            center = Offset(size.width * 0.6f, size.height * 0.4f)
        )
        drawCircle(
            color = Color(0x11000000),
            radius = size.minDimension / 10f,
            center = Offset(size.width * 0.4f, size.height * 0.65f)
        )
        drawCircle(
            color = Color(0x15000000),
            radius = size.minDimension / 12f,
            center = Offset(size.width * 0.65f, size.height * 0.65f)
        )
    }
}

@Composable
fun AnimatedRain(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val dropY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_drop"
    )

    Canvas(modifier = modifier) {
        val cloudColor = Color(0xFF90A4AE)
        
        // Draw cloud
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(size.width * 0.15f, size.height * 0.2f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.35f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.175f)
        )
        drawCircle(
            color = cloudColor,
            radius = size.width * 0.2f,
            center = Offset(size.width * 0.4f, size.height * 0.25f)
        )
        drawCircle(
            color = cloudColor,
            radius = size.width * 0.15f,
            center = Offset(size.width * 0.65f, size.height * 0.3f)
        )
        
        // Draw rain drops
        val dropHeight = size.height * 0.15f
        val dropWidth = size.width * 0.04f
        val startY = size.height * 0.4f
        val distance = size.height * 0.5f
        
        // Drop 1
        val y1 = startY + ((dropY + 0.0f) % 1f) * distance
        drawRoundRect(
            color = Color(0xFF4FC3F7),
            topLeft = Offset(size.width * 0.3f, y1),
            size = androidx.compose.ui.geometry.Size(dropWidth, dropHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(dropWidth)
        )
        // Drop 2
        val y2 = startY + ((dropY + 0.5f) % 1f) * distance
        drawRoundRect(
            color = Color(0xFF4FC3F7),
            topLeft = Offset(size.width * 0.5f, y2),
            size = androidx.compose.ui.geometry.Size(dropWidth, dropHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(dropWidth)
        )
        // Drop 3
        val y3 = startY + ((dropY + 0.2f) % 1f) * distance
        drawRoundRect(
            color = Color(0xFF4FC3F7),
            topLeft = Offset(size.width * 0.7f, y3),
            size = androidx.compose.ui.geometry.Size(dropWidth, dropHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(dropWidth)
        )
    }
}

@Composable
fun AnimatedClouds(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -0.05f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_drift"
    )

    Canvas(modifier = modifier) {
        val cloudColor = Color(0xFFF5F5F5)
        val cloudColorDark = Color(0xFFB0BEC5)

        // Back cloud
        translate(left = -offsetX * size.width) {
            drawRoundRect(
                color = cloudColorDark,
                topLeft = Offset(size.width * 0.25f, size.height * 0.15f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.6f, size.height * 0.3f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.15f)
            )
            drawCircle(color = cloudColorDark, radius = size.width * 0.2f, center = Offset(size.width * 0.45f, size.height * 0.2f))
            drawCircle(color = cloudColorDark, radius = size.width * 0.15f, center = Offset(size.width * 0.7f, size.height * 0.25f))
        }

        // Front cloud
        translate(left = offsetX * size.width) {
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(size.width * 0.1f, size.height * 0.4f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.7f, size.height * 0.35f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.175f)
            )
            drawCircle(color = cloudColor, radius = size.width * 0.25f, center = Offset(size.width * 0.35f, size.height * 0.45f))
            drawCircle(color = cloudColor, radius = size.width * 0.2f, center = Offset(size.width * 0.65f, size.height * 0.5f))
        }
    }
}
