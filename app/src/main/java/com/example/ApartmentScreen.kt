package com.example

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.bounceClick
import kotlin.math.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.draw.drawBehind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApartmentScreen(onHomeClick: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE) }
    
    var progress by remember { 
        mutableStateOf(sharedPrefs.getFloat("apartment_progress", 0f)) 
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = 90f
        ),
        label = "BuildingSpringAnimation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "OledBuildingTimeClock")
    val timeClock by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TimeAngle"
    )

    val phaseColor = when {
        progress <= 0.15f -> Color(0xFF00E5FF)
        progress < 0.95f -> Color(0xFFD4AF37) // Luxurious Gold instead of Magenta
        else -> Color(0xFF00E676)
    }

    val phaseText = when {
        progress == 0f -> "Terreno & Fundações"
        progress <= 0.15f -> "Estrutura Subterrânea"
        progress < 0.50f -> "Lajes Cilíndricas Iniciais"
        progress < 0.95f -> "Fachada & Pavimentos Altos"
        progress < 1.0f -> "Sistemas de Vidro & Revestimento"
        else -> "Obra Concluída • Masterpiece"
    }

    val animatedPhaseColor by animateColorAsState(
        targetValue = phaseColor,
        animationSpec = tween(400),
        label = "ColorPhaseTransition"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505)) // Pure pitch-black OLED background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier.bounceClick { onHomeClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Apartamento",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraLight, // ExtraLight simulated Montserrat
                    fontSize = 26.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Main Building Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(380.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val centerX = canvasWidth / 2f
                    
                    val buildingWidth = 112.dp.toPx()
                    val buildingHeight = 240.dp.toPx()
                    val base = canvasHeight - 40.dp.toPx()
                    val top = base - buildingHeight
                    val radiusX = buildingWidth / 2f
                    val radiusY = radiusX * 0.32f // Perspective ratio

                    // 1. SKY LIGHT Reflection (Luz de luar azulada e fria)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x0C00E5FF), Color.Transparent),
                            center = Offset(centerX, top - 20.dp.toPx()),
                            radius = 280.dp.toPx()
                        ),
                        radius = 280.dp.toPx(),
                        center = Offset(centerX, top - 20.dp.toPx())
                    )

                    // 2. ORBITING CATS PATH DEFINITION (Spirals around cylinder)
                    // Cat 1 (Ceramic Black Shiny)
                    val cat1Angle = timeClock * 1.3f
                    val cat1RadiusX = radiusX + 16.dp.toPx()
                    val cat1RadiusY = radiusY + 8.dp.toPx()
                    val cat1Progress = (timeClock * 0.08f) % 1.0f
                    val cat1Y = base - cat1Progress * buildingHeight + sin(timeClock * 4f) * 12.dp.toPx()
                    val cat1X = centerX + cos(cat1Angle) * cat1RadiusX
                    val cat1Z = sin(cat1Angle) // < 0 is behind, >= 0 is front

                    // Cat 2
                    val cat2Angle = timeClock * 1.3f + PI.toFloat()
                    val cat2RadiusX = radiusX + 16.dp.toPx()
                    val cat2RadiusY = radiusY + 8.dp.toPx()
                    val cat2Progress = (timeClock * 0.08f + 0.5f) % 1.0f
                    val cat2Y = base - cat2Progress * buildingHeight + sin(timeClock * 4f + PI.toFloat()) * 12.dp.toPx()
                    val cat2X = centerX + cos(cat2Angle) * cat2RadiusX
                    val cat2Z = sin(cat2Angle)

                    // Helper to draw a shiny ceramic cat
                    fun drawCeramicCat(cx: Float, cy: Float) {
                        // 1. Golden/Amber Aura
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x66FFD54F), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = 28.dp.toPx()
                            ),
                            radius = 28.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                        // 2. Black Ceramic Body
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF37474F), Color(0xFF000000)),
                                center = Offset(cx - 2.dp.toPx(), cy - 2.dp.toPx()),
                                radius = 8.dp.toPx()
                            ),
                            radius = 8.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                        // 3. Ceramic specular highlight (bright white dot)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = 1.8.dp.toPx(),
                            center = Offset(cx - 3.dp.toPx(), cy - 3.dp.toPx())
                        )
                    }

                    // 3. DRAW BEHIND CATS (Z < 0)
                    if (cat1Z < 0f) drawCeramicCat(cat1X, cat1Y)
                    if (cat2Z < 0f) drawCeramicCat(cat2X, cat2Y)

                    // 4. FOUNDATIONS (Dash Grid on floor)
                    val gridWidth = 140.dp.toPx()
                    val gridHeight = gridWidth * 0.32f
                    drawOval(
                        color = Color(0x1F00E5FF),
                        topLeft = Offset(centerX - gridWidth, base - gridHeight),
                        size = Size(gridWidth * 2, gridHeight * 2),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    )

                    // 5. SKYSCRAPER CYLINDRICAL LAYOUT (8 Floors)
                    val totalFloors = 8
                    val floorHeight = buildingHeight / totalFloors
                    
                    for (f in 0 until totalFloors) {
                        val floorBottom = base - f * floorHeight
                        val floorProgress = ((animatedProgress * totalFloors) - f).coerceIn(0f, 1f)
                        
                        if (floorProgress > 0f) {
                            // Rise from bottom with spring physics
                            val currentY = base - (base - floorBottom) * elasticOut(floorProgress)
                            
                            // Modulated Wavy Slab path
                            val numPoints = 80
                            val waveAmplitude = 2.5f.dp.toPx() * floorProgress
                            val waveFrequency = 10f
                            val slabPath = Path()
                            
                            for (i in 0..numPoints) {
                                val angle = (2f * PI.toFloat() * i) / numPoints
                                val radialMod = waveAmplitude * sin(waveFrequency * angle)
                                val rX = radiusX + radialMod
                                val rY = radiusY + radialMod * (radiusY / radiusX)
                                
                                val px = centerX + cos(angle) * rX
                                val py = currentY + sin(angle) * rY + waveAmplitude * 0.4f * cos(waveFrequency * angle)
                                
                                if (i == 0) {
                                    slabPath.moveTo(px, py)
                                } else {
                                    slabPath.lineTo(px, py)
                                }
                            }
                            slabPath.close()

                            // Windows / Core lit detection
                            val dist1 = abs(cat1Y - currentY)
                            val dist2 = abs(cat2Y - currentY)
                            val minDist = min(dist1, dist2)
                            val proximity = (1f - (minDist / (floorHeight * 1.5f))).coerceIn(0f, 1f)
                            val pulse = 0.85f + 0.15f * sin(timeClock * 12f)
                            val glowAlpha = proximity * pulse * floorProgress

                            // Core warm glowing light
                            if (glowAlpha > 0f) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFFFC107).copy(alpha = 0.7f * glowAlpha), Color.Transparent),
                                        center = Offset(centerX, currentY),
                                        radius = 28.dp.toPx()
                                    ),
                                    radius = 28.dp.toPx(),
                                    center = Offset(centerX, currentY)
                                )
                                drawCircle(
                                    color = Color(0xFFFFD54F).copy(alpha = glowAlpha),
                                    radius = 5.dp.toPx(),
                                    center = Offset(centerX, currentY)
                                )
                            }

                            // Slab Fill & Reflection (Polished White Moonlit Material)
                            val slabColor = lerpColors(Color(0xFFFFFFFF), Color(0xFFECEFF1), f / (totalFloors - 1).toFloat())
                            drawPath(
                                path = slabPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(slabColor.copy(alpha = 0.9f * floorProgress), Color(0xFFB0BEC5).copy(alpha = 0.6f * floorProgress))
                                )
                            )
                            // Moonlit bluish edge outline
                            drawPath(
                                path = slabPath,
                                color = Color(0xFFB2EBF2).copy(alpha = 0.55f * floorProgress),
                                style = Stroke(width = 1.2f.dp.toPx())
                            )

                            // Pillars connecting to floor below
                            val pillarsAngles = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f).map { it * PI.toFloat() / 180f }
                            pillarsAngles.forEach { pAngle ->
                                val radialMod = waveAmplitude * sin(waveFrequency * pAngle)
                                val rX = radiusX + radialMod
                                val rY = radiusY + radialMod * (radiusY / radiusX)
                                
                                val px = centerX + cos(pAngle) * rX
                                val pyTop = currentY + sin(pAngle) * rY + waveAmplitude * 0.4f * cos(waveFrequency * pAngle)

                                val prevFloorProgress = ((animatedProgress * totalFloors) - (f - 1)).coerceIn(0f, 1f)
                                val pyBottom = if (f == 0) {
                                    base + sin(pAngle) * radiusY
                                } else {
                                    val prevWaveAmplitude = 2.5f.dp.toPx() * prevFloorProgress
                                    val prevRadialMod = prevWaveAmplitude * sin(waveFrequency * pAngle)
                                    val prevRX = radiusX + prevRadialMod
                                    val prevRY = radiusY + prevRadialMod * (radiusY / radiusX)
                                    val prevYAnimated = base - (base - (base - (f - 1) * floorHeight)) * elasticOut(prevFloorProgress)
                                    prevYAnimated + sin(pAngle) * prevRY + prevWaveAmplitude * 0.4f * cos(waveFrequency * pAngle)
                                }

                                drawLine(
                                    color = Color(0x66B0BEC5).copy(alpha = 0.7f * floorProgress),
                                    start = Offset(px, pyTop),
                                    end = Offset(px, pyBottom),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                    }

                    // 6. DRAW FRONT CATS (Z >= 0)
                    if (cat1Z >= 0f) drawCeramicCat(cat1X, cat1Y)
                    if (cat2Z >= 0f) drawCeramicCat(cat2X, cat2Y)

                    // 7. BASE FOG FADE EFFECT
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF050505)),
                            startY = base - 30.dp.toPx(),
                            endY = base + 30.dp.toPx()
                        ),
                        topLeft = Offset(0f, base - 30.dp.toPx()),
                        size = Size(canvasWidth, 80.dp.toPx())
                    )

                    // 8. TELEMETRY LOGS (Monospace text)
                    val textPaint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        textSize = 8.sp.toPx()
                        color = android.graphics.Color.argb(85, 178, 235, 242)
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    
                    val telemetryLogs = listOf(
                        "SYS.BUILD: ${(animatedProgress * 100).toInt()}%",
                        "CYL.RAD: ${buildingWidth.toInt()}px",
                        "GEOM.Z: ${String.format(java.util.Locale.US, "%.3f", animatedProgress)}",
                        "LIGHT.FREQ: 120Hz"
                    )
                    
                    telemetryLogs.forEachIndexed { index, log ->
                        val textY = base + 34.dp.toPx() + (index * 9.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(
                            log,
                            16.dp.toPx(),
                            textY,
                            textPaint
                        )
                    }
                    
                    val rightLogs = listOf(
                        "CORE.TEMP: 28.7 C",
                        "LUX.ORBIT: ACTIVE",
                        "GLOW.CORE: ACTIVE",
                        "REFLECT.T: ${String.format(java.util.Locale.US, "%.2f", timeClock)}"
                    )
                    
                    rightLogs.forEachIndexed { index, log ->
                        val textY = base + 34.dp.toPx() + (index * 9.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(
                            log,
                            size.width - 130.dp.toPx(),
                            textY,
                            textPaint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Premium Interactive Glassmorphism Control Panel
            var componentSize by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { componentSize = it }
                    .then(PremiumGlassModifier)
                    .drawBehind {
                        // Golden metallic wire progress on bottom border
                        val strokeWidth = 2.5f.dp.toPx()
                        val progressWidth = size.width * progress
                        
                        // Base track (translucent gold)
                        drawLine(
                            color = Color(0x1AD4AF37),
                            start = Offset(0f, size.height - strokeWidth / 2f),
                            end = Offset(size.width, size.height - strokeWidth / 2f),
                            strokeWidth = strokeWidth
                        )
                        
                        // Active progress wire
                        drawLine(
                            color = Color(0xFFD4AF37),
                            start = Offset(0f, size.height - strokeWidth / 2f),
                            end = Offset(progressWidth, size.height - strokeWidth / 2f),
                            strokeWidth = strokeWidth
                        )
                        
                        // Glowing golden point on extremity
                        if (progressWidth > 0f) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFD700), Color.Transparent),
                                    center = Offset(progressWidth, size.height - strokeWidth / 2f),
                                    radius = 8.dp.toPx()
                                ),
                                radius = 8.dp.toPx(),
                                center = Offset(progressWidth, size.height - strokeWidth / 2f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.5f.dp.toPx(),
                                center = Offset(progressWidth, size.height - strokeWidth / 2f)
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                val width = componentSize.width
                                if (width > 0) {
                                    val newProgress = (offset.x / width).coerceIn(0f, 1f)
                                    progress = newProgress
                                    sharedPrefs.edit().putFloat("apartment_progress", newProgress).apply()
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, dragAmount ->
                                val width = componentSize.width
                                if (width > 0) {
                                    val newProgress = (progress + dragAmount / width).coerceIn(0f, 1f)
                                    progress = newProgress
                                    sharedPrefs.edit().putFloat("apartment_progress", newProgress).apply()
                                }
                            }
                        )
                    }
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = phaseText,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = animatedPhaseColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraLight, // Montserrat simulated extra light
                        fontSize = 52.sp,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Spring Elastic interpolation function (Overshoot) for smooth floor animation
private fun elasticOut(t: Float): Float {
    if (t <= 0f) return 0f
    if (t >= 1f) return 1f
    val p = 0.4f
    return (2.0).pow(-10.0 * t).toFloat() * sin((t - p / 4f) * (2f * PI.toFloat()) / p) + 1f
}

private fun lerpColors(start: Color, end: Color, fraction: Float): Color {
    val r = start.red + (end.red - start.red) * fraction
    val g = start.green + (end.green - start.green) * fraction
    val b = start.blue + (end.blue - start.blue) * fraction
    val a = start.alpha + (end.alpha - start.alpha) * fraction
    return Color(r, g, b, a)
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
