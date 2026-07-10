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
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = 1f + (pulseAlpha * 0.1f)
                        scaleY = 1f + (pulseAlpha * 0.1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (desc.contains("Rain")) Icons.Outlined.Air else Icons.Outlined.WbSunny,
                    contentDescription = null,
                    tint = if (isDay) Color(0xFFFFB74D) else Color(0xFFD7B4F3),
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
