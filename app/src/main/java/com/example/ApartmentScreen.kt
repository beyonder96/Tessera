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
                        .fillMaxWidth(0.95f)
                        .height(360.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val centerX = canvasWidth / 2f
                    
                    // Skyscraper physical layout bounds
                    val buildingWidth = 96.dp.toPx()
                    val buildingHeight = 250.dp.toPx()
                    val halfWidth = buildingWidth / 2f
                    val base = canvasHeight - 24.dp.toPx()
                    val top = base - buildingHeight
                    
                    // isometric inclination offsets
                    val isoSlopeLeft = 14.dp.toPx()
                    val isoSlopeRight = 14.dp.toPx()
                    
                    val left = centerX - halfWidth
                    val right = centerX + halfWidth
                    val currentProgressHeight = buildingHeight * animatedProgress
                    val currentLineY = base - currentProgressHeight

                    // 1. ISOMETRIC CYBER BLUEPRINT GRID (Ground Perspective)
                    val gridWidth = 180.dp.toPx()
                    val gridHeight = 40.dp.toPx()

                    // Radial glow floor reflection
                    drawOval(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x3D00E5FF), Color.Transparent),
                            center = Offset(centerX, base),
                            radius = gridWidth
                        ),
                        topLeft = Offset(centerX - gridWidth, base - gridHeight),
                        size = Size(gridWidth * 2, gridHeight * 2)
                    )

                    // Rotating Concentric rings in 3D perspective
                    val rings = listOf(0.4f, 0.7f, 1.0f)
                    rings.forEachIndexed { idx, r ->
                        val rotationDir = if (idx % 2 == 0) 1f else -1f
                        val degrees = timeClock * 35f * rotationDir
                        
                        // Simulate rotation using transform
                        drawContext.canvas.save()
                        drawContext.transform.rotate(degrees, Offset(centerX, base))
                        
                        // draw concentric ellipses with dashes
                        drawOval(
                            color = Color(0x3B00E5FF),
                            topLeft = Offset(centerX - gridWidth * r, base - gridHeight * r),
                            size = Size(gridWidth * 2 * r, gridHeight * 2 * r),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                            )
                        )
                        drawContext.canvas.restore()
                    }

                    // Perspective floor lines radiating from base center
                    val floorLines = 10
                    for (i in 0..floorLines) {
                        val angle = PI.toFloat() * (i / floorLines.toFloat())
                        val endX = centerX + cos(angle) * gridWidth
                        val endY = base + sin(angle) * gridHeight
                        drawLine(
                            color = Color(0x1F00E5FF),
                            start = Offset(centerX, base),
                            end = Offset(endX, endY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 2. GENERATE PARTICLES DATA (Cylinder Orbit 3D)
                    val activeParticles = 20
                    data class OrbitParticle(
                        val x: Float,
                        val y: Float,
                        val size: Float,
                        val alpha: Float,
                        val isBehind: Boolean
                    )
                    
                    val particlesList = List(activeParticles) { p ->
                        val phaseOffset = p * (2 * PI.toFloat() / activeParticles)
                        // swirl speed and orbit radius
                        val angle = timeClock * 1.8f + phaseOffset
                        val radius = halfWidth + 24.dp.toPx() + 8.dp.toPx() * sin(angle * 2f)
                        
                        // cylindrical projection
                        val px = centerX + cos(angle) * radius
                        val py = currentLineY - 12.dp.toPx() * sin(angle) + cos(angle * 3f) * 6.dp.toPx()
                        val depth = sin(angle) // negative depth means behind building
                        
                        val pSize = 2.dp.toPx() + 1.5.dp.toPx() * (depth + 1f)
                        val pAlpha = (0.2f + 0.8f * ((depth + 1f) / 2f)) * (1f - animatedProgress).coerceAtLeast(0.1f)
                        
                        OrbitParticle(px, py, pSize, pAlpha, depth < 0f)
                    }

                    // 3. DRAW BEHIND PARTICLES
                    particlesList.filter { it.isBehind }.forEach { p ->
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = p.alpha),
                            radius = p.size,
                            center = Offset(p.x, p.y)
                        )
                    }

                    // 4. UNDERGROUND FOUNDATIONS (0%)
                    val pilesCount = 5
                    for (i in 0 until pilesCount) {
                        val fraction = i / (pilesCount - 1).toFloat()
                        val pileX = left + buildingWidth * fraction
                        val pileSlope = lerp(-isoSlopeLeft, isoSlopeRight, fraction)
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha = 0.35f + 0.3f * sin(timeClock * 3f + i).absoluteValue),
                            start = Offset(pileX, base + pileSlope),
                            end = Offset(pileX, base + pileSlope + 22.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    }

                    // 5. MORPHING STRUCTURAL FRAMES (1% to 99%)
                    val totalFloors = 8
                    val floorHeight = buildingHeight / totalFloors
                    
                    for (f in 0 until totalFloors) {
                        val floorBottom = base - f * floorHeight
                        val floorProgress = ((animatedProgress * totalFloors) - f).coerceIn(0f, 1f)
                        
                        if (floorProgress > 0f) {
                            val currentFloorHeight = floorHeight * floorProgress
                            val currentFloorTop = floorBottom - currentFloorHeight
                            
                            // Base color transitions based on floor level
                            val baseViolet = Color(0xFFD500F9)
                            val baseMagenta = Color(0xFFFF007F)
                            val floorAccentColor = lerpColors(baseViolet, baseMagenta, f / (totalFloors - 1).toFloat())
                            
                            // Let's draw the structural pillars for left and right faces
                            val pillarsLeft = listOf(left, left + halfWidth / 2f, centerX)
                            val pillarsRight = listOf(centerX, centerX + halfWidth / 2f, right)
                            
                            // Left Face Vertical Structural Pillars
                            pillarsLeft.forEachIndexed { index, px ->
                                val ratio = (px - left) / halfWidth
                                val slopeBottom = lerp(-isoSlopeLeft, 0f, ratio)
                                val slopeTop = slopeBottom
                                drawLine(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(floorAccentColor, floorAccentColor.copy(alpha = 0.2f))
                                    ),
                                    start = Offset(px, floorBottom + slopeBottom),
                                    end = Offset(px, currentFloorTop + slopeTop),
                                    strokeWidth = 2.5.dp.toPx()
                                )
                            }
                            
                            // Right Face Vertical Structural Pillars
                            pillarsRight.forEachIndexed { index, px ->
                                val ratio = (px - centerX) / halfWidth
                                val slopeBottom = lerp(0f, -isoSlopeRight, ratio)
                                val slopeTop = slopeBottom
                                drawLine(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(floorAccentColor, floorAccentColor.copy(alpha = 0.2f))
                                    ),
                                    start = Offset(px, floorBottom + slopeBottom),
                                    end = Offset(px, currentFloorTop + slopeTop),
                                    strokeWidth = 2.5.dp.toPx()
                                )
                            }
                            
                            // Horizontal Floor Slabs (V-shape in perspective)
                            val leftSlabPath = Path().apply {
                                moveTo(left, currentFloorTop - isoSlopeLeft)
                                lineTo(centerX, currentFloorTop)
                            }
                            val rightSlabPath = Path().apply {
                                moveTo(centerX, currentFloorTop)
                                lineTo(right, currentFloorTop - isoSlopeRight)
                            }
                            drawPath(leftSlabPath, color = floorAccentColor, style = Stroke(width = 2.dp.toPx()))
                            drawPath(rightSlabPath, color = floorAccentColor, style = Stroke(width = 2.dp.toPx()))
                            
                            // Cross-bracing wireframes for high-tech skeleton detail
                            if (floorProgress > 0.4f) {
                                // Left Face X-Bracing
                                drawLine(
                                    color = floorAccentColor.copy(alpha = 0.15f),
                                    start = Offset(left, floorBottom - isoSlopeLeft),
                                    end = Offset(centerX, currentFloorTop),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = floorAccentColor.copy(alpha = 0.15f),
                                    start = Offset(centerX, floorBottom),
                                    end = Offset(left, currentFloorTop - isoSlopeLeft),
                                    strokeWidth = 1.dp.toPx()
                                )
                                // Right Face X-Bracing
                                drawLine(
                                    color = floorAccentColor.copy(alpha = 0.15f),
                                    start = Offset(centerX, floorBottom),
                                    end = Offset(right, currentFloorTop - isoSlopeRight),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = floorAccentColor.copy(alpha = 0.15f),
                                    start = Offset(right, floorBottom - isoSlopeRight),
                                    end = Offset(centerX, currentFloorTop),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                    }

                    // 6. LUXURY GLASS SKIN & REFLECTIONS (Materializing as progress increases)
                    if (animatedProgress > 0.15f) {
                        val glassAlpha = ((animatedProgress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                        val currentGlassHeight = buildingHeight * animatedProgress
                        
                        // Left Face Glass Panel (Bright, reflecting sun light)
                        val leftGlassPath = Path().apply {
                            moveTo(left, base - isoSlopeLeft)
                            lineTo(centerX, base)
                            lineTo(centerX, base - currentGlassHeight)
                            lineTo(left, base - currentGlassHeight - isoSlopeLeft)
                            close()
                        }
                        val leftGlassBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x3B00E5FF), // Cyber Cyan reflection tint
                                Color(0x7503080A)  // Deep luxury dark grey
                            ),
                            startY = base - currentGlassHeight,
                            endY = base
                        )
                        drawPath(leftGlassPath, brush = leftGlassBrush, alpha = glassAlpha)
                        
                        // Right Face Glass Panel (Slightly darker shade for 3D depth)
                        val rightGlassPath = Path().apply {
                            moveTo(centerX, base)
                            lineTo(right, base - isoSlopeRight)
                            lineTo(right, base - currentGlassHeight - isoSlopeRight)
                            lineTo(centerX, base - currentGlassHeight)
                            close()
                        }
                        val rightGlassBrush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x240091EA), // Deep Blue tint
                                Color(0x8C020507)  // Deeper background shading
                            ),
                            startY = base - currentGlassHeight,
                            endY = base
                        )
                        drawPath(rightGlassPath, brush = rightGlassBrush, alpha = glassAlpha)

                        // Outline Borders for Left & Right face
                        drawPath(
                            path = leftGlassPath,
                            color = Color(0xFF00E5FF).copy(alpha = 0.45f * glassAlpha),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawPath(
                            path = rightGlassPath,
                            color = Color(0xFF00E5FF).copy(alpha = 0.3f * glassAlpha),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Glass Panel Sub-Divisions (perspective lines)
                        val leftSubPanels = 3
                        for (v in 1 until leftSubPanels) {
                            val ratio = v / leftSubPanels.toFloat()
                            val px = lerp(left, centerX, ratio)
                            val slope = lerp(-isoSlopeLeft, 0f, ratio)
                            drawLine(
                                color = Color(0x4000E5FF).copy(alpha = 0.25f * glassAlpha),
                                start = Offset(px, base + slope),
                                end = Offset(px, base - currentGlassHeight + slope),
                                strokeWidth = 0.5.dp.toPx()
                            )
                        }
                        val rightSubPanels = 3
                        for (v in 1 until rightSubPanels) {
                            val ratio = v / rightSubPanels.toFloat()
                            val px = lerp(centerX, right, ratio)
                            val slope = lerp(0f, -isoSlopeRight, ratio)
                            drawLine(
                                color = Color(0x2D00E5FF).copy(alpha = 0.2f * glassAlpha),
                                start = Offset(px, base + slope),
                                end = Offset(px, base - currentGlassHeight + slope),
                                strokeWidth = 0.5.dp.toPx()
                            )
                        }

                        // Premium Diagonal Glass Light Reflections sweep effect
                        val sweepPercent = (timeClock * 0.15f) % 2f - 1f // from -1.0 to 1.0
                        
                        // Left Reflection sweep
                        val sweepLeftX = lerp(left - 20.dp.toPx(), centerX + 20.dp.toPx(), sweepPercent.coerceIn(0f, 1f))
                        val leftRefPath = Path().apply {
                            moveTo(sweepLeftX, base)
                            lineTo(sweepLeftX + 16.dp.toPx(), base)
                            lineTo(sweepLeftX - 16.dp.toPx(), base - currentGlassHeight)
                            lineTo(sweepLeftX - 32.dp.toPx(), base - currentGlassHeight)
                            close()
                        }
                        drawPath(
                            path = leftRefPath,
                            brush = Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.15f * glassAlpha), Color.White.copy(alpha = 0f)),
                                start = Offset(sweepLeftX, base),
                                end = Offset(sweepLeftX - 16.dp.toPx(), base - currentGlassHeight)
                            )
                        )

                        // 7. LANDSCAPING & INTERIOR LIGHTS (92% to 100%)
                        if (animatedProgress > 0.9f) {
                            val completionFactor = ((animatedProgress - 0.9f) / 0.1f).coerceIn(0f, 1f)
                            
                            // Golden Amber warm light windows pulsing inside the building
                            val litFloors = 6
                            for (fl in 0 until litFloors) {
                                val floorY = base - fl * floorHeight - floorHeight / 2f
                                
                                // Window Left
                                val winLeftX = lerp(left, centerX, 0.4f)
                                val winLeftSlope = lerp(-isoSlopeLeft, 0f, 0.4f)
                                val pulseValL = sin(timeClock * 2.8f + fl * 1.5f).absoluteValue
                                drawRect(
                                    color = Color(0xFFFFC107).copy(alpha = (0.2f + 0.8f * pulseValL) * completionFactor),
                                    topLeft = Offset(winLeftX - 6.dp.toPx(), floorY + winLeftSlope - 4.dp.toPx()),
                                    size = Size(12.dp.toPx(), 8.dp.toPx())
                                )
                                
                                // Window Right
                                val winRightX = lerp(centerX, right, 0.6f)
                                val winRightSlope = lerp(0f, -isoSlopeRight, 0.6f)
                                val pulseValR = cos(timeClock * 2.2f + fl * 2f).absoluteValue
                                drawRect(
                                    color = Color(0xFFFFD54F).copy(alpha = (0.3f + 0.7f * pulseValR) * completionFactor),
                                    topLeft = Offset(winRightX - 6.dp.toPx(), floorY + winRightSlope - 4.dp.toPx()),
                                    size = Size(12.dp.toPx(), 8.dp.toPx())
                                )
                            }

                            // Luxury Hanging Rooftop Gardens
                            val roofY = top
                            
                            // Emerald foliage base
                            drawArc(
                                color = Color(0xFF00E676).copy(alpha = completionFactor),
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = true,
                                topLeft = Offset(centerX - 22.dp.toPx(), roofY - 8.dp.toPx()),
                                size = Size(44.dp.toPx(), 16.dp.toPx())
                            )
                            // Swaying garden leaves
                            for (leaf in -4..4) {
                                val leafX = centerX + leaf * 5.dp.toPx()
                                val leafSlope = if (leaf < 0) lerp(-isoSlopeLeft, 0f, (leafX - left)/halfWidth) else lerp(0f, -isoSlopeRight, (leafX - centerX)/halfWidth)
                                val sway = sin(timeClock * 2.5f + leaf).absoluteValue * 2.dp.toPx()
                                drawCircle(
                                    color = Color(0xFF00C853).copy(alpha = completionFactor),
                                    radius = 3.dp.toPx(),
                                    center = Offset(leafX, roofY + leafSlope - 6.dp.toPx() + sway)
                                )
                            }
                            
                            // Aeronautical Beacon Light at very top
                            val beaconPulse = sin(timeClock * 6f).absoluteValue
                            drawCircle(
                                color = if (beaconPulse > 0.5f) Color.Red.copy(alpha = completionFactor) else Color(0xFF00E5FF).copy(alpha = completionFactor * 0.4f),
                                radius = 4.dp.toPx(),
                                center = Offset(centerX, roofY - 12.dp.toPx())
                            )
                            drawLine(
                                color = Color.White.copy(alpha = completionFactor * 0.3f),
                                start = Offset(centerX, roofY),
                                end = Offset(centerX, roofY - 12.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    // 8. KINETIC ACTIVE LINE, NEON GLOW & HOLOGRAPHIC CRANE
                    if (animatedProgress > 0f && animatedProgress < 1.0f) {
                        // laser alignment guide (V-shape path matching 3D projection)
                        val laserPath = Path().apply {
                            moveTo(left - 10.dp.toPx(), currentLineY - isoSlopeLeft)
                            lineTo(centerX, currentLineY)
                            lineTo(right + 10.dp.toPx(), currentLineY - isoSlopeRight)
                        }
                        
                        val neonAccent = when {
                            animatedProgress <= 0.15f -> Color(0xFF00E5FF)
                            animatedProgress < 0.95f -> Color(0xFFFF007F)
                            else -> Color(0xFF00E676)
                        }
                        
                        // laser path glow
                        drawPath(
                            path = laserPath,
                            color = neonAccent.copy(alpha = 0.3f + 0.2f * sin(timeClock * 8f).absoluteValue),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = laserPath,
                            color = neonAccent,
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Holographic Crane on construction level
                        // Moves left/right slightly
                        val craneOffset = sin(timeClock * 1.5f) * 20.dp.toPx()
                        val craneX = centerX + craneOffset
                        val craneBaseY = currentLineY
                        
                        // Crane structure path
                        val cranePath = Path().apply {
                            moveTo(craneX, craneBaseY)
                            lineTo(craneX, craneBaseY - 18.dp.toPx())
                            lineTo(craneX + 16.dp.toPx(), craneBaseY - 24.dp.toPx()) // arm
                            moveTo(craneX, craneBaseY - 18.dp.toPx())
                            lineTo(craneX - 10.dp.toPx(), craneBaseY - 20.dp.toPx()) // counterweight
                        }
                        drawPath(cranePath, color = Color(0xAA00E5FF), style = Stroke(width = 1.dp.toPx()))
                        
                        // Laser cable and source point
                        val laserEmitterX = craneX + 10.dp.toPx()
                        val laserEmitterY = craneBaseY - 21.5f.dp.toPx()
                        drawLine(
                            color = Color(0xFFFFFFFF),
                            start = Offset(laserEmitterX, laserEmitterY),
                            end = Offset(laserEmitterX, currentLineY),
                            strokeWidth = 1.dp.toPx()
                        )
                        
                        // Glow spark at construction point
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(laserEmitterX, currentLineY)
                        )
                        drawCircle(
                            color = neonAccent.copy(alpha = 0.5f),
                            radius = 8.dp.toPx(),
                            center = Offset(laserEmitterX, currentLineY)
                        )
                    }

                    // 9. DRAW FRONT PARTICLES
                    particlesList.filter { !it.isBehind }.forEach { p ->
                        drawCircle(
                            color = Color(0xFFFF007F).copy(alpha = p.alpha), // Emissive neon pink for front particles
                            radius = p.size,
                            center = Offset(p.x, p.y)
                        )
                    }

                    // 10. TELEMETRY INFO (Monospace small technical logs)
                    val textPaint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        textSize = 8.sp.toPx()
                        color = android.graphics.Color.argb(70, 0, 229, 255)
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    
                    val telemetryLogs = listOf(
                        "SYS.BUILD: ${(animatedProgress * 100).toInt()}%",
                        "STRUCT.Z: ${String.format(java.util.Locale.US, "%.3f", animatedProgress)}",
                        "VECTORS.OK: [${(centerX + halfWidth).toInt()}, ${(base - currentProgressHeight).toInt()}]",
                        "ANT.FREQ: 844.2 MHZ"
                    )
                    
                    telemetryLogs.forEachIndexed { index, log ->
                        val textY = base + 38.dp.toPx() + (index * 9.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(
                            log,
                            10.dp.toPx(),
                            textY,
                            textPaint
                        )
                    }
                    
                    // Right telemetry logs
                    val rightLogs = listOf(
                        "CORE.TEMP: 32.4 C",
                        "NET.FLOW: 1.48 GB/S",
                        "LOAD.STAT: NOMINAL",
                        "REFLECT.PHASE: ${String.format(java.util.Locale.US, "%.2f", timeClock)}"
                    )
                    
                    rightLogs.forEachIndexed { index, log ->
                        val textY = base + 38.dp.toPx() + (index * 9.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText(
                            log,
                            size.width - 120.dp.toPx(),
                            textY,
                            textPaint
                        )
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

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
