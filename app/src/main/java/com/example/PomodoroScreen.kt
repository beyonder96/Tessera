package com.example

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun PomodoroScreen() {
    var selectedCategory by remember { mutableStateOf("Trabalho") } // Estudos, Trabalho, Oração
    
    // Focus settings: 25 minutes for work, 50 minutes for studies, 15 minutes for prayer
    val targetSeconds = when (selectedCategory) {
        "Estudos" -> 3000 // 50m
        "Oração" -> 900   // 15m
        else -> 1500      // 25m
    }

    var secondsLeft by remember(selectedCategory) { mutableStateOf(targetSeconds) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, selectedCategory) {
        if (isRunning) {
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
            isRunning = false
        }
    }

    val progress = (secondsLeft.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000, easing = LinearEasing))

    // Dynamic category-based premium neon color
    val accentColor by animateColorAsState(
        targetValue = when (selectedCategory) {
            "Estudos" -> Color(0xFFD7B4F3) // Neon Purple
            "Oração" -> Color(0xFFF9A826)  // Neon Gold
            else -> Color(0xFF71D7CD)      // Neon Teal
        }, label = "AccentColorTransition"
    )

    // Canvas rotation/glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "Glow")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "Rotation"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)), // True AMOLED Black
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Space / Category pills
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(
                text = "FOCUS FOCUS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 3.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141414))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Estudos", "Trabalho", "Oração").forEach { cat ->
                    val isSel = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) Color(0xFF222222) else Color.Transparent)
                            .clickable { 
                                selectedCategory = cat
                                isRunning = false
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSel) Color.White else Color(0xFF666666),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // AMOLED Canvas Timer Ring
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                
                // Background Track
                drawArc(
                    color = Color(0xFF111111),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )

                // Glowing Neon Ring with Pulse
                drawArc(
                    color = accentColor.copy(alpha = 0.15f * pulseGlow),
                    startAngle = -90f + rotationAngle,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth * 2.2f, cap = StrokeCap.Round)
                )

                // Main Progress Arc
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 54.sp,
                    color = Color.White
                )
                Text(
                    text = when (selectedCategory) {
                        "Estudos" -> "Mantenha o foco absoluto"
                        "Oração" -> "Conecte-se e respire"
                        else -> "Produza sem distrações"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF555555),
                    letterSpacing = 1.sp
                )
            }
        }

        // Timer Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 60.dp)
        ) {
            // Reset Button
            IconButton(
                onClick = {
                    isRunning = false
                    secondsLeft = targetSeconds
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF111111), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reiniciar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Play / Pause Button
            IconButton(
                onClick = { isRunning = !isRunning },
                modifier = Modifier
                    .size(68.dp)
                    .background(accentColor, CircleShape)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Iniciar / Pausar",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
