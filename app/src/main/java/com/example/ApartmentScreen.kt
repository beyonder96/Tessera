package com.example

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.ui.graphics.graphicsLayer
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

    val scrollState = rememberScrollState()
    val isCompact = scrollState.value > 150
    val normalAlpha by animateFloatAsState(targetValue = if (isCompact) 0f else 1f, animationSpec = tween(250), label = "normalAlpha")
    val compactAlpha by animateFloatAsState(targetValue = if (isCompact) 1f else 0f, animationSpec = tween(250), label = "compactAlpha")
    val accentColor = Color(0xFFD4AF37) // Luxurious Gold

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Removed hardcoded background so it uses global background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            // Main Building Canvas
            Box(
                modifier = Modifier
                    .height(380.dp)
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
                    
                    val buildingWidth = 130.dp.toPx()
                    val buildingHeight = 260.dp.toPx()
                    val baseY = canvasHeight - 50.dp.toPx()
                    val radiusX = buildingWidth / 2f
                    
                    // 1. SKY LIGHT Reflection (Luz de luar azulada ao fundo)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x0A00E5FF), Color.Transparent),
                            center = Offset(centerX, baseY - buildingHeight),
                            radius = 260.dp.toPx()
                        ),
                        radius = 260.dp.toPx(),
                        center = Offset(centerX, baseY - buildingHeight)
                    )

                    // 2. FUNDAÇÕES E GRADE DE SOLO
                    val gridWidth = 120.dp.toPx()
                    val gridHeight = 30.dp.toPx()
                    drawOval(
                        color = Color(0x2200E5FF),
                        topLeft = Offset(centerX - gridWidth, baseY - gridHeight),
                        size = Size(gridWidth * 2, gridHeight * 2),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    )

                    // 3. ESTRUTURA DO PRÉDIO (8 Andares)
                    val totalFloors = 8
                    val floorHeight = buildingHeight / totalFloors
                    
                    for (f in 0 until totalFloors) {
                        val floorProgress = ((animatedProgress * totalFloors) - f).coerceIn(0f, 1f)
                        
                        if (floorProgress > 0f) {
                            val floorBottom = baseY - f * floorHeight
                            val floorHeightCurrent = floorHeight * floorProgress
                            val floorTop = floorBottom - floorHeightCurrent
                            
                            // Corpo de concreto do andar (Gradiente escuro polido)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF1E252B), Color(0xFF101418), Color(0xFF1E252B))
                                ),
                                topLeft = Offset(centerX - radiusX, floorTop),
                                size = Size(buildingWidth, floorHeightCurrent)
                            )
                            
                            // Borda/Contorno lateral do prédio
                            drawLine(
                                color = Color.White.copy(alpha = 0.12f),
                                start = Offset(centerX - radiusX, floorBottom),
                                end = Offset(centerX - radiusX, floorTop),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.12f),
                                start = Offset(centerX + radiusX, floorBottom),
                                end = Offset(centerX + radiusX, floorTop),
                                strokeWidth = 1.dp.toPx()
                            )

                            // Linha horizontal de laje de concreto (Teto do andar)
                            drawLine(
                                color = Color(0xFFB0BEC5).copy(alpha = 0.35f * floorProgress),
                                start = Offset(centerX - radiusX, floorTop),
                                end = Offset(centerX + radiusX, floorTop),
                                strokeWidth = 1.5f.dp.toPx()
                            )
                            
                            // 4. JANELAS DO ANDAR (3 por andar)
                            val numWindows = 3
                            val windowWidth = 22.dp.toPx()
                            val windowHeight = 16.dp.toPx()
                            val totalWindowsWidth = numWindows * windowWidth
                            val windowSpacing = (buildingWidth - totalWindowsWidth) / (numWindows + 1)
                            val windowY = floorTop + (floorHeight - windowHeight) / 2f
                            
                            if (floorProgress > 0.85f) {
                                for (w in 0 until numWindows) {
                                    val winX = centerX - radiusX + windowSpacing + w * (windowWidth + windowSpacing)
                                    // Padrão pseudo-aleatório para janelas acesas
                                    val isLit = (f + w) % 3 == 0 || (f * 2 + w) % 5 == 0
                                    
                                    if (isLit) {
                                        // Brilho quente da janela (luz derramada)
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(Color(0xFFFFD54F).copy(alpha = 0.35f), Color.Transparent),
                                                center = Offset(winX + windowWidth / 2f, windowY + windowHeight / 2f),
                                                radius = windowWidth * 1.3f
                                            ),
                                            radius = windowWidth * 1.3f,
                                            center = Offset(winX + windowWidth / 2f, windowY + windowHeight / 2f)
                                        )
                                        // Janela acesa
                                        drawRect(
                                            color = Color(0xFFFFF176),
                                            topLeft = Offset(winX, windowY),
                                            size = Size(windowWidth, windowHeight)
                                        )
                                        // Detalhe de esquadria/vidro interno
                                        drawLine(
                                            color = Color(0xFFD4AF37).copy(alpha = 0.3f),
                                            start = Offset(winX + windowWidth / 2f, windowY),
                                            end = Offset(winX + windowWidth / 2f, windowY + windowHeight),
                                            strokeWidth = 1f
                                        )
                                    } else {
                                        // Janela apagada (reflexo azul noturno escuro)
                                        drawRect(
                                            color = Color(0xFF1C2833),
                                            topLeft = Offset(winX, windowY),
                                            size = Size(windowWidth, windowHeight)
                                        )
                                    }
                                }
                            }
                            
                            // 5. ANDAIMES DE OBRA NO ANDAR EM CONSTRUÇÃO ACTIVE
                            if (floorProgress < 1.0f) {
                                val scaffoldHeight = 15.dp.toPx()
                                val numScaffolds = 4
                                val scWidth = buildingWidth / numScaffolds
                                
                                for (s in 0 until numScaffolds) {
                                    val scLeft = centerX - radiusX + s * scWidth
                                    val scRight = scLeft + scWidth
                                    
                                    // Estrutura em "X" do andaime
                                    drawLine(
                                        color = Color(0xAA00E5FF),
                                        start = Offset(scLeft, floorTop),
                                        end = Offset(scRight, floorTop - scaffoldHeight),
                                        strokeWidth = 1f
                                    )
                                    drawLine(
                                        color = Color(0xAA00E5FF),
                                        start = Offset(scRight, floorTop),
                                        end = Offset(scLeft, floorTop - scaffoldHeight),
                                        strokeWidth = 1f
                                    )
                                    // Vigas horizontais e verticais do andaime
                                    drawLine(
                                        color = Color(0xAA00E5FF),
                                        start = Offset(scLeft, floorTop - scaffoldHeight),
                                        end = Offset(scRight, floorTop - scaffoldHeight),
                                        strokeWidth = 1f
                                    )
                                    drawLine(
                                        color = Color(0xAA00E5FF),
                                        start = Offset(scLeft, floorTop),
                                        end = Offset(scLeft, floorTop - scaffoldHeight),
                                        strokeWidth = 1f
                                    )
                                }
                            }
                        }
                    }

                    // 6. ANTENA E BALIZA VERMELHA NO TOPO (Acima de 95% do progresso total)
                    if (animatedProgress > 0.95f) {
                        val buildingTop = baseY - buildingHeight
                        val antennaHeight = 35.dp.toPx()
                        
                        // Base da antena
                        drawRect(
                            color = Color(0xFFB0BEC5),
                            topLeft = Offset(centerX - 6f, buildingTop - 2.dp.toPx()),
                            size = Size(12f, 2.dp.toPx())
                        )
                        // Mastro metálico
                        drawLine(
                            color = Color(0xFFB0BEC5),
                            start = Offset(centerX, buildingTop),
                            end = Offset(centerX, buildingTop - antennaHeight),
                            strokeWidth = 2.dp.toPx()
                        )
                        // Baliza piscante vermelha de sinalização
                        val beaconPulse = 0.3f + 0.7f * abs(sin(timeClock * 6f))
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.Red.copy(alpha = beaconPulse), Color.Transparent),
                                center = Offset(centerX, buildingTop - antennaHeight),
                                radius = 10.dp.toPx()
                            ),
                            radius = 10.dp.toPx(),
                            center = Offset(centerX, buildingTop - antennaHeight)
                        )
                        drawCircle(
                            color = Color.Red,
                            radius = 3.dp.toPx(),
                            center = Offset(centerX, buildingTop - antennaHeight)
                        )
                    }

                    // 7. NEBLINA NA BASE (Efeito de escala/profundidade)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF050505)),
                            startY = baseY - 20.dp.toPx(),
                            endY = baseY + 40.dp.toPx()
                        ),
                        topLeft = Offset(0f, baseY - 20.dp.toPx()),
                        size = Size(canvasWidth, 60.dp.toPx())
                    )

                    // 8. LOGS DE TELEMETRIA DO APÊ (Estilo futurista e limpo)
                    val textPaint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        textSize = 8.sp.toPx()
                        color = android.graphics.Color.argb(85, 178, 235, 242)
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    
                    val activeFloors = (animatedProgress * totalFloors).toInt().coerceIn(0, totalFloors)
                    val leftLogs = listOf(
                        "SYS.APE_BUILD: ${(animatedProgress * 100).toInt()}%",
                        "STRUCT.FLOORS: $activeFloors / $totalFloors",
                        "GRID.COORDS: Z-${String.format(java.util.Locale.US, "%.3f", animatedProgress)}",
                        "FREQ.STRUCT: 60Hz"
                    )
                    
                    leftLogs.forEachIndexed { index, log ->
                        val textY = baseY + 34.dp.toPx() + (index * 9.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(
                            log,
                            16.dp.toPx(),
                            textY,
                            textPaint
                        )
                    }
                    
                    val rightLogs = listOf(
                        "STRUCT.STATUS: ${if (animatedProgress >= 1f) "CONCLUDED" else "IN_PROGRESS"}",
                        "WINDOW.GRID: ACTIVE",
                        "BEACON.LIGHT: ACTIVE",
                        "CLOCK.TICK: ${String.format(java.util.Locale.US, "%.2f", timeClock)}"
                    )
                    
                    rightLogs.forEachIndexed { index, log ->
                        val textY = baseY + 34.dp.toPx() + (index * 9.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(
                            log,
                            size.width - 140.dp.toPx(),
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
            Spacer(modifier = Modifier.height(100.dp)) // Spacer to clear the bottom navigation bar
        }

        // Floating overlay top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // 1. Barra Normal
            if (normalAlpha > 0.05f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = normalAlpha
                            scaleX = 0.92f + (normalAlpha * 0.08f)
                            scaleY = 0.92f + (normalAlpha * 0.08f)
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onHomeClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = "Home",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MEU APÊ",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            // 2. Barra Compacta
            if (compactAlpha > 0.05f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = compactAlpha
                            translationY = (1f - compactAlpha) * (-20f)
                        }
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Apartment,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                    val shimmerOffset by infiniteTransition.animateFloat(
                        initialValue = -400f,
                        targetValue = 400f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "shimmerOffset"
                    )
                    
                    val nameGlowBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            accentColor,
                            Color.White,
                            accentColor,
                            Color.White
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 150f, 150f)
                    )
                    
                    Text(
                        text = "MEU APÊ",
                        style = TextStyle(
                            brush = nameGlowBrush,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Serif
                        )
                    )
                }
            }
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
