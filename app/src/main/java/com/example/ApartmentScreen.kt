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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApartmentScreen(onHomeClick: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE) }
    
    var progress by remember { 
        mutableStateOf(sharedPrefs.getFloat("apartment_progress", 0f)) 
    }

    // Apple-style spring physics for smooth building construction/deconstruction transitions
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = 0.68f, // Responsive, bouncy feel
            stiffness = 90f      // Smooth, natural speed deceleration
        ),
        label = "BuildingSpringAnimation"
    )

    // Infinite clock transition to animate lighting waves, reflections and particles without overhead
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

    // Determine current construction phase color and label reatively
    val phaseColor = when {
        progress <= 0.15f -> Color(0xFF00E5FF) // Cyberpunk Cyan (Foundation)
        progress < 0.95f -> Color(0xFFFF007F)  // Vivid Magenta (Structure)
        else -> Color(0xFF00E676)              // Emerald Green (Completion)
    }

    val phaseText = when {
        progress == 0f -> "Terreno & Fundações"
        progress <= 0.15f -> "Estrutura Subterrânea"
        progress < 0.50f -> "Pórticos & Vigas Iniciais"
        progress < 0.95f -> "Fachada & Pavimentos Altos"
        progress < 1.0f -> "Sistemas de Vidro & Revestimento"
        else -> "Obra Concluída • Masterpiece"
    }

    // Smooth transition of state colors
    val animatedPhaseColor by animateColorAsState(
        targetValue = phaseColor,
        animationSpec = tween(400),
        label = "ColorPhaseTransition"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Pure pitch-black OLED background
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
                    fontWeight = FontWeight.Light,
                    fontSize = 26.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Main Building Canvas (Takes upper central area)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(340.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val centerX = canvasWidth / 2f
                    
                    // Skyscraper physical layout bounds
                    val buildingWidth = 84.dp.toPx()
                    val buildingHeight = 240.dp.toPx()
                    val left = centerX - buildingWidth / 2f
                    val right = centerX + buildingWidth / 2f
                    val base = canvasHeight - 20.dp.toPx()
                    val top = base - buildingHeight
                    
                    // 1. ISOMETRIC CYBER BLUEPRINT GRID (Ground Perspective)
                    val gridWidth = 160.dp.toPx()
                    val gridHeight = 35.dp.toPx()

                    // Radial glow floor reflection
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x3300E5FF),
                                Color.Transparent
                            ),
                            center = Offset(centerX, base),
                            radius = gridWidth
                        ),
                        topLeft = Offset(centerX - gridWidth, base - gridHeight),
                        size = Size(gridWidth * 2, gridHeight * 2)
                    )

                    // Ground isometric wireframe lines
                    val numPerspectiveLines = 8
                    for (i in 0..numPerspectiveLines) {
                        val angle = PI.toFloat() * (i / numPerspectiveLines.toFloat())
                        val endX = centerX + cos(angle) * gridWidth
                        val endY = base + sin(angle) * gridHeight
                        drawLine(
                            color = Color(0x2200E5FF),
                            start = Offset(centerX, base),
                            end = Offset(endX, endY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Concentric rings
                    for (r in listOf(0.4f, 0.7f, 1.0f)) {
                        drawOval(
                            color = Color(0x2B00E5FF),
                            topLeft = Offset(centerX - gridWidth * r, base - gridHeight * r),
                            size = Size(gridWidth * 2 * r, gridHeight * 2 * r),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // 2. UNDERGROUND BLUEPRINT FOUNDATIONS (0%)
                    val foundationCyan = Color(0xFF00E5FF)
                    val pilesCount = 4
                    for (i in 0 until pilesCount) {
                        val pileX = left + (buildingWidth / (pilesCount - 1)) * i
                        drawLine(
                            color = foundationCyan.copy(alpha = 0.4f + 0.4f * sin(timeClock * 2f + i).absoluteValue),
                            start = Offset(pileX, base),
                            end = Offset(pileX, base + 24.dp.toPx()),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    }

                    // 3. MORPHING STRUCTURAL FRAMES (1% to 99%)
                    val totalFloors = 8
                    val floorHeight = buildingHeight / totalFloors
                    
                    for (f in 0 until totalFloors) {
                        val floorBottom = base - f * floorHeight
                        val floorProgress = ((animatedProgress * totalFloors) - f).coerceIn(0f, 1f)
                        
                        if (floorProgress > 0f) {
                            val currentFloorTop = floorBottom - (floorHeight * floorProgress)
                            
                            // Dynamic color morphing gradients for structures rising
                            val structColorStart = Color(0xFFD500F9) // Deep Violet
                            val structColorEnd = Color(0xFFFF007F)   // Emissive Magenta
                            
                            // Shift colors smoothly per floor level
                            val floorAccentColor = lerpColors(
                                structColorStart, 
                                structColorEnd, 
                                f / (totalFloors - 1).toFloat()
                            )
                            
                            // 3 Pillars: Left, Center, Right
                            val pillarsX = listOf(left, centerX, right)
                            pillarsX.forEach { px ->
                                drawLine(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(floorAccentColor, floorAccentColor.copy(alpha = 0.2f))
                                    ),
                                    start = Offset(px, floorBottom),
                                    end = Offset(px, currentFloorTop),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                            
                            // Structural cross-bracing (Architectural Wireframe Grid)
                            if (floorProgress > 0.4f) {
                                drawLine(
                                    color = floorAccentColor.copy(alpha = 0.15f),
                                    start = Offset(left, floorBottom),
                                    end = Offset(right, currentFloorTop),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = floorAccentColor.copy(alpha = 0.15f),
                                    start = Offset(right, floorBottom),
                                    end = Offset(left, currentFloorTop),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            
                            // Floor slabs
                            drawLine(
                                color = floorAccentColor,
                                start = Offset(left, currentFloorTop),
                                end = Offset(right, currentFloorTop),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }

                    // 4. LUXURY GLASS SKIN & REFLECTIONS (Materializing as progress increases)
                    if (animatedProgress > 0.15f) {
                        val glassAlpha = ((animatedProgress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                        
                        val glassBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x1C00E5FF), // Cyber Cyan reflection tint
                                Color(0x5503080A)  // Deep luxury dark grey
                            ),
                            startY = base - (buildingHeight * animatedProgress),
                            endY = base
                        )
                        
                        val currentGlassHeight = buildingHeight * animatedProgress
                        val currentGlassTop = base - currentGlassHeight

                        // Glass Pane Fill
                        drawRect(
                            brush = glassBrush,
                            topLeft = Offset(left, currentGlassTop),
                            size = Size(buildingWidth, currentGlassHeight),
                            alpha = glassAlpha
                        )

                        // Outer glass silhouette borders
                        drawRect(
                            color = Color(0xFF00E5FF).copy(alpha = 0.35f * glassAlpha),
                            topLeft = Offset(left, currentGlassTop),
                            size = Size(buildingWidth, currentGlassHeight),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Vertical divisions / Glass panels
                        val numPanels = 4
                        for (v in 1 until numPanels) {
                            val panelX = left + (buildingWidth / numPanels) * v
                            drawLine(
                                color = Color(0x3B00E5FF).copy(alpha = 0.25f * glassAlpha),
                                start = Offset(panelX, currentGlassTop),
                                end = Offset(panelX, base),
                                strokeWidth = 0.5.dp.toPx()
                            )
                        }

                        // Premium Diagonal Glass Light Reflections
                        val sweepStart = left + (timeClock * 15f) % (buildingWidth * 2f) - buildingWidth
                        val reflectionPath = Path().apply {
                            moveTo(sweepStart, base)
                            lineTo(sweepStart + 25.dp.toPx(), base)
                            lineTo(sweepStart - 15.dp.toPx(), currentGlassTop)
                            lineTo(sweepStart - 40.dp.toPx(), currentGlassTop)
                            close()
                        }
                        
                        drawPath(
                            path = reflectionPath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0f),
                                    Color.White.copy(alpha = 0.12f * glassAlpha),
                                    Color.White.copy(alpha = 0f)
                                ),
                                start = Offset(sweepStart, base),
                                end = Offset(sweepStart - 15.dp.toPx(), currentGlassTop)
                            )
                        )

                        // 5. MASTERPIECE LANDSCAPING & INTERIOR LIGHTING (95% to 100% completion)
                        if (animatedProgress > 0.92f) {
                            val completionFactor = ((animatedProgress - 0.92f) / 0.08f).coerceIn(0f, 1f)
                            
                            // Golden Amber warm light windows pulsing inside
                            val floorsToLight = 6
                            val windowsPerFloor = 3
                            for (fl in 0 until floorsToLight) {
                                val floorY = base - fl * floorHeight - floorHeight / 2f
                                for (w in 0 until windowsPerFloor) {
                                    val winX = left + (buildingWidth / (windowsPerFloor + 1)) * (w + 1)
                                    val pulseValue = sin(timeClock * 3.5f + fl * 2.5f + w).absoluteValue
                                    val amberAlpha = (0.25f + 0.75f * pulseValue) * completionFactor
                                    
                                    drawRect(
                                        color = Color(0xFFFFC107).copy(alpha = amberAlpha),
                                        topLeft = Offset(winX - 5.dp.toPx(), floorY - 3.dp.toPx()),
                                        size = Size(10.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                            }

                            // Emerald Green Luxury Hanging Gardens on Roof / Terrace
                            val roofY = top
                            // Base bushes
                            drawArc(
                                color = Color(0xFF00E676).copy(alpha = completionFactor),
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = true,
                                topLeft = Offset(centerX - 24.dp.toPx(), roofY - 9.dp.toPx()),
                                size = Size(48.dp.toPx(), 18.dp.toPx())
                            )
                            // Elegant leaves particles
                            for (leaf in -3..3) {
                                val leafX = centerX + leaf * 7.dp.toPx()
                                val waveOffset = sin(timeClock * 2f + leaf).absoluteValue * 3.dp.toPx()
                                val leafY = roofY - 7.dp.toPx() + waveOffset
                                drawCircle(
                                    color = Color(0xFF00C853).copy(alpha = completionFactor),
                                    radius = 4.dp.toPx(),
                                    center = Offset(leafX, leafY)
                                )
                            }
                        }
                    }

                    // 6. KINETIC ACTIVE LINE & NEON PARTICLES CLOUD
                    if (animatedProgress > 0f && animatedProgress < 1.0f) {
                        val currentLineHeight = buildingHeight * animatedProgress
                        val lineY = base - currentLineHeight

                        // Responsive color tracking the current phase color
                        val neonAccentColor = when {
                            animatedProgress <= 0.15f -> Color(0xFF00E5FF)
                            animatedProgress < 0.95f -> Color(0xFFFF007F)
                            else -> Color(0xFF00E676)
                        }

                        // Emissive laser neon glow radial overlay
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(neonAccentColor.copy(alpha = 0.45f), Color.Transparent),
                                center = Offset(centerX, lineY),
                                radius = buildingWidth * 0.95f
                            ),
                            topLeft = Offset(centerX - buildingWidth * 0.95f, lineY - 9.dp.toPx()),
                            size = Size(buildingWidth * 1.9f, 18.dp.toPx())
                        )

                        // Laser horizontal alignment guide
                        drawLine(
                            color = neonAccentColor,
                            start = Offset(left - 12.dp.toPx(), lineY),
                            end = Offset(right + 12.dp.toPx(), lineY),
                            strokeWidth = 2.5.dp.toPx()
                        )

                        // Kinetic particle wave swirling around construction level
                        val activeParticles = 14
                        for (p in 0 until activeParticles) {
                            val phaseOffset = p * (2 * PI.toFloat() / activeParticles)
                            val particleTime = timeClock + phaseOffset
                            
                            // Spiral coordinates around building frame
                            val px = centerX + sin(particleTime) * (buildingWidth / 2f + 14.dp.toPx() * cos(particleTime * 2.5f).absoluteValue)
                            val py = lineY + cos(particleTime * 2f) * 11.dp.toPx() - (particleTime % 1f) * 6.dp.toPx()
                            
                            val pRadius = 2.dp.toPx() + 1.dp.toPx() * sin(particleTime * 4f).absoluteValue
                            val pAlpha = (0.3f + 0.7f * sin(particleTime * 3f).absoluteValue) * (1f - animatedProgress)

                            drawCircle(
                                color = neonAccentColor.copy(alpha = pAlpha),
                                radius = pRadius,
                                center = Offset(px, py)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Premium Interactive Glassmorphism Control Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(PremiumGlassModifier)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = phaseText,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = animatedPhaseColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Light,
                        fontSize = 52.sp,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // High-fidelity active slider
                    Slider(
                        value = progress,
                        onValueChange = { 
                            progress = it
                            sharedPrefs.edit().putFloat("apartment_progress", it).apply()
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = animatedPhaseColor,
                            activeTrackColor = animatedPhaseColor,
                            inactiveTrackColor = Color(0x1AFFFFFF),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Utility to linearly interpolate color vectors smoothly in Canvas drawing operations
private fun lerpColors(start: Color, end: Color, fraction: Float): Color {
    val r = start.red + (end.red - start.red) * fraction
    val g = start.green + (end.green - start.green) * fraction
    val b = start.blue + (end.blue - start.blue) * fraction
    val a = start.alpha + (end.alpha - start.alpha) * fraction
    return Color(r, g, b, a)
}
