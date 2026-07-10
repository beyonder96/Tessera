package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.bounceClick

@Composable
fun OuraMetricItem(
    iconType: String,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0x0CFFFFFF))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon at the top
                when (iconType) {
                    "readiness" -> {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    "sleep" -> {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(0f, h)
                                lineTo(w, h)
                                lineTo(w, h * 0.3f)
                                lineTo(w * 0.75f, h * 0.6f)
                                lineTo(w * 0.5f, h * 0.1f)
                                lineTo(w * 0.25f, h * 0.6f)
                                lineTo(0f, h * 0.3f)
                                close()
                            }
                            drawPath(path, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                    "activity" -> {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    "heart" -> {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Value in the middle
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Label underneath the circle
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun MetricItem(icon: ImageVector, value: String, label: String, onClick: () -> Unit) {
    val displayFontSize = if (value.length > 5) 12.sp else if (value.length > 4) 14.sp else 16.sp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x24FFFFFF),
                            Color(0x05FFFFFF)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x40FFFFFF),
                            Color(0x0AFFFFFF)
                        )
                    ),
                    shape = CircleShape
                )
                .bounceClick { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = displayFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(label, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
fun MetricItemWithProgress(icon: ImageVector, value: String, label: String, progressColor: Color, progress: Float, onClick: () -> Unit) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing), label = ""
    )
    val displayFontSize = if (value.length > 5) 11.sp else if (value.length > 4) 13.sp else 15.sp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x24FFFFFF),
                            Color(0x05FFFFFF)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x40FFFFFF),
                            Color(0x0AFFFFFF)
                        )
                    ),
                    shape = CircleShape
                )
                .bounceClick { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                drawArc(
                    color = progressColor.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                if (icon == Icons.Outlined.Bedtime) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w * 0.1f, h * 0.85f)
                            lineTo(w * 0.9f, h * 0.85f)
                            lineTo(w * 0.9f, h * 0.45f)
                            lineTo(w * 0.7f, h * 0.65f)
                            lineTo(w * 0.5f, h * 0.25f)
                            lineTo(w * 0.3f, h * 0.65f)
                            lineTo(w * 0.1f, h * 0.45f)
                            close()
                        }
                        drawPath(path, color = Color.White.copy(alpha = 0.8f))
                        drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 1.dp.toPx(), center = Offset(w * 0.1f, h * 0.45f))
                        drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 1.dp.toPx(), center = Offset(w * 0.5f, h * 0.25f))
                        drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 1.dp.toPx(), center = Offset(w * 0.9f, h * 0.45f))
                    }
                } else {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = displayFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(label, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
fun MetricItemWithNeonPulse(
    icon: ImageVector,
    value: String,
    label: String,
    glowColor: Color = Color(0xFF71D7CD),
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NeonPulse")
    val pulseGlowVal by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlow"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x24FFFFFF),
                            Color(0x05FFFFFF)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            glowColor.copy(alpha = pulseGlowVal),
                            glowColor.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                )
                .bounceClick { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = glowColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(label, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.5f))
    }
}
