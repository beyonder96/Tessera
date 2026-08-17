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
    val isDay = weatherState?.isDay ?: true

    // Determine display header text
    val headerText = remember(rawDesc, isDay) {
        if (!isDay && (rawDesc.contains("Limpo", ignoreCase = true) || rawDesc.contains("Noite", ignoreCase = true))) {
            "NOITE ESTRELADA"
        } else if (isDay && (rawDesc.contains("Sun", ignoreCase = true) || rawDesc.contains("Clear", ignoreCase = true) || rawDesc.contains("Limpo", ignoreCase = true))) {
            "CÉU LIMPO"
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

    // Thermal UI - Gradient for the temperature numbers (adaptado dia / noite)
    val tempBrush = remember(isDay) {
        if (isDay) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFFF5E00),
                    Color(0xFFCC1100)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFF38BDF8),
                    Color(0xFF6366F1)
                )
            )
        }
    }

    val glowRadialInner = if (isDay) Color(0xFFFF5E00) else Color(0xFF38BDF8)
    val glowRadialOuter = if (isDay) Color(0xFF990000) else Color(0xFF1E1B4B)
    val glowLine = if (isDay) Color(0xFFFF3300) else Color(0xFF818CF8)

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
            .thermalCard(cornerRadius = 28.dp, elevation = 20.dp)
    ) {

        // Native Particle Weather Animation
        WeatherParticleEffects(
            description = rawDesc,
            isDay = isDay,
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


        }
    }
}
