package com.example

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Partículas configuration
private data class Particle(
    val id: Int,
    var x: Float,
    var y: Float,
    val size: Float,
    val speedY: Float,
    val swaySpeed: Float,
    val swayAmount: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApartmentScreen(onHomeClick: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE) }
    
    var progress by remember { mutableStateOf(sharedPrefs.getFloat("apartment_progress", 0f)) }
    var isPlaying by remember { mutableStateOf(false) }
    var isDay by remember { mutableStateOf(true) }

    // Motor da Construção
    LaunchedEffect(isPlaying) {
        while (isPlaying && progress < 1f) {
            delay(50)
            progress = (progress + 0.01f).coerceAtMost(1f)
            sharedPrefs.edit().putFloat("apartment_progress", progress).apply()
            if (progress >= 1f) isPlaying = false
        }
    }

    // Cores de Dia / Noite
    val bgTop by animateColorAsState(if (isDay) Color(0xFF7DD3FC) else Color(0xFF0F172A), tween(1000))
    val bgBottom by animateColorAsState(if (isDay) Color(0xFFE0F2FE) else Color(0xFF1E1B4B), tween(1000))
    val textColor = if (isDay) Color(0xFF1E293B) else Color.White
    val textSubColor = if (isDay) Color(0xFF475569) else Color(0xFF94A3B8)
    val glassBg = if (isDay) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
    val glassBorder = if (isDay) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)

    var showDateDialog by remember { mutableStateOf(false) }
    var expectedDate by remember { mutableStateOf(sharedPrefs.getString("apartment_date", "Dez 2026") ?: "Dez 2026") }
    var tempDate by remember { mutableStateOf(expectedDate) }
    
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
    ) {
        // Partículas Imersivas
        ParticlesLayer(isDay = isDay)

        // Astro Animatrônico
        AnimatronicAstro(isDay = isDay)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header Premium
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONSTRUTOR",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSubColor,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Glassmorphic",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // O Efeito Glassmorphic (Prédio SVG)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(glassBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(32.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                GlassmorphicBuilding(progress = progress, isDay = isDay)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Toggle Dia/Noite Movido para cá
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                IconButton(
                    onClick = { isDay = !isDay },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(glassBg)
                        .border(1.dp, glassBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isDay) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Toggle Day/Night",
                        tint = if (isDay) Color(0xFFF59E0B) else Color(0xFF93C5FD)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de Progresso Interativa (Slider)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Início", color = textSubColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Concluído", color = if (isDay) Color(0xFF0284C7) else Color(0xFF22D3EE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    var componentSize by remember { mutableStateOf(IntSize.Zero) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.1f))
                            .onSizeChanged { componentSize = it }
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val newP = (offset.x / componentSize.width).coerceIn(0f, 1f)
                                    progress = newP
                                    sharedPrefs.edit().putFloat("apartment_progress", progress).apply()
                                    isPlaying = false
                                }
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    val newP = (progress + dragAmount / componentSize.width).coerceIn(0f, 1f)
                                    progress = newP
                                    sharedPrefs.edit().putFloat("apartment_progress", progress).apply()
                                    isPlaying = false
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1))))
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                if (progress >= 1f) progress = 0f
                                isPlaying = !isPlaying 
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isDay) Color(0xFF0EA5E9) else Color(0xFF06B6D4))
                        ) {
                            Icon(
                                imageVector = if (progress >= 1f) Icons.Outlined.Refresh else if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor,
                            letterSpacing = (-2).sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Detalhes da Obra
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "DETALHES DA OBRA",
                        fontSize = 11.sp,
                        color = textSubColor,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.clickable { 
                            tempDate = expectedDate
                            showDateDialog = true 
                        }) {
                            Text("Previsão", color = textSubColor, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(expectedDate, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = textSubColor, modifier = Modifier.size(12.dp))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Status", color = textSubColor, fontSize = 13.sp)
                            Text(if (progress >= 1f) "Entregue" else "Em Andamento", color = if (progress >= 1f) Color(0xFF10B981) else Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Confetes no topo (Celebração)
        if (progress >= 1f) {
            ConfettiEffect()
        }
    }

    if (showDateDialog) {
        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            title = { Text("Previsão de Conclusão") },
            text = {
                OutlinedTextField(
                    value = tempDate,
                    onValueChange = { tempDate = it },
                    label = { Text("Data (ex: Dez 2026)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    expectedDate = tempDate
                    sharedPrefs.edit().putString("apartment_date", tempDate).apply()
                    showDateDialog = false
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun GlassmorphicBuilding(progress: Float, isDay: Boolean) {
    // Cálculos Matemáticos
    val maxH = 280f
    val foundationHeight = (progress * 2f).coerceAtMost(0.1f) * maxH
    val coreHeight = ((progress - 0.1f) * 4f).coerceIn(0f, 0.5f) * maxH
    val glassHeight = ((progress - 0.35f) * 3.5f).coerceIn(0f, 0.65f) * maxH
    val antennaHeight = ((progress - 0.8f) * 2f).coerceIn(0f, 0.2f) * maxH

    val lightColor1 = if (isDay) Color(0xFF38BDF8) else Color(0xFF818CF8)
    val lightColor2 = if (isDay) Color(0xFFFBBF24) else Color(0xFFC084FC)

    Box(modifier = Modifier.fillMaxSize()) {
        // Orbes em Blur no fundo
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .size(160.dp)
                .blur(40.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(lightColor1.copy(alpha = 0.6f + (progress * 0.4f)), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 30.dp, y = 20.dp)
                .size(120.dp)
                .blur(30.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(lightColor2.copy(alpha = 0.4f + (progress * 0.3f)), CircleShape)
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val groundY = size.height - 20.dp.toPx()

            // 1. Antena
            if (antennaHeight > 0) {
                val antH = antennaHeight
                val antY = groundY - foundationHeight - coreHeight - antH
                drawRect(
                    color = Color(0xFF94A3B8),
                    topLeft = Offset(cx - 2.dp.toPx(), antY),
                    size = Size(4.dp.toPx(), antH)
                )
                if (progress >= 0.95f) {
                    drawCircle(
                        color = Color.Red,
                        radius = 4.dp.toPx(),
                        center = Offset(cx, antY)
                    )
                }
            }

            // 2. Núcleo (Core)
            if (coreHeight > 0) {
                val coreW = 60.dp.toPx()
                val coreY = groundY - foundationHeight - coreHeight
                val coreColorTop = if (isDay) Color(0xFF64748B) else Color(0xFF334155)
                val coreColorBot = if (isDay) Color(0xFF334155) else Color(0xFF0F172A)
                drawRect(
                    brush = Brush.verticalGradient(listOf(coreColorTop, coreColorBot), startY = coreY, endY = coreY + coreHeight),
                    topLeft = Offset(cx - coreW / 2, coreY),
                    size = Size(coreW, coreHeight)
                )
                // Linhas do core
                val steps = 10
                for (i in 0..steps) {
                    val yLine = coreY + (coreHeight / steps) * i
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(cx - coreW / 2, yLine),
                        end = Offset(cx + coreW / 2, yLine),
                        strokeWidth = 2f
                    )
                }
            }

            // 3. Fachada de Vidro
            if (glassHeight > 0) {
                val glassW = 140.dp.toPx()
                val glassY = groundY - foundationHeight - glassHeight
                
                // Asas laterais
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.1f),
                    topLeft = Offset(cx - glassW/2 - 16.dp.toPx(), glassY + glassHeight * 0.1f),
                    size = Size(20.dp.toPx(), glassHeight * 0.9f),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 2f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.1f),
                    topLeft = Offset(cx + glassW/2 - 4.dp.toPx(), glassY + glassHeight * 0.1f),
                    size = Size(20.dp.toPx(), glassHeight * 0.9f),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 2f)
                )

                // Bloco principal de vidro
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.1f)),
                        startY = glassY, endY = glassY + glassHeight
                    ),
                    topLeft = Offset(cx - glassW / 2, glassY),
                    size = Size(glassW, glassHeight),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.4f), // Borda brilhante
                    topLeft = Offset(cx - glassW / 2, glassY),
                    size = Size(glassW, glassHeight),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 4f)
                )
                
                // Reflexo de topo
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(cx - glassW/2 + 4.dp.toPx(), glassY + 4.dp.toPx()),
                    end = Offset(cx + glassW/2 - 4.dp.toPx(), glassY + 4.dp.toPx()),
                    strokeWidth = 6f
                )

                // Grid do vidro
                val vSteps = 6
                for (i in 1 until vSteps) {
                    val xLine = cx - glassW/2 + (glassW / vSteps) * i
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(xLine, glassY),
                        end = Offset(xLine, glassY + glassHeight),
                        strokeWidth = 2f
                    )
                }
                val hSteps = 10
                for (i in 1 until hSteps) {
                    val yLine = glassY + (glassHeight / hSteps) * i
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(cx - glassW/2, yLine),
                        end = Offset(cx + glassW/2, yLine),
                        strokeWidth = 2f
                    )
                }
            }

            // 4. Fundação
            if (foundationHeight > 0) {
                val foundW = 180.dp.toPx()
                val foundY = groundY - foundationHeight
                val foundColor = if (isDay) Color(0xFF475569) else Color(0xFF1E293B)
                drawRoundRect(
                    color = foundColor,
                    topLeft = Offset(cx - foundW / 2, foundY),
                    size = Size(foundW, foundationHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(cx - foundW / 2, foundY),
                    size = Size(foundW, foundationHeight),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 2f)
                )
            }
        }

        // Textos Flutuantes Indicadores
        AnimatedVisibility(
            visible = progress > 0.05f,
            enter = slideInHorizontally(initialOffsetX = { -50 }) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-10).dp, y = (-20).dp)
        ) {
            Text("Fundação", color = if (isDay) Color(0xFF475569) else Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        AnimatedVisibility(
            visible = progress > 0.40f,
            enter = slideInHorizontally(initialOffsetX = { -50 }) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-10).dp, y = (-150).dp)
        ) {
            Text("Fachada de Vidro", color = if (isDay) Color(0xFF0EA5E9) else Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun AnimatronicAstro(isDay: Boolean) {
    val sunOffsetY by animateFloatAsState(targetValue = if (isDay) 0f else 150f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 80f))
    val moonOffsetY by animateFloatAsState(targetValue = if (!isDay) 0f else -150f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 80f))
    val sunAlpha by animateFloatAsState(targetValue = if (isDay) 1f else 0f)
    val moonAlpha by animateFloatAsState(targetValue = if (!isDay) 1f else 0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 40.dp, end = 40.dp)
    ) {
        // Sun
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = sunOffsetY.dp)
                .size(80.dp)
                .graphicsLayer { alpha = sunAlpha }
                .blur(10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(Brush.radialGradient(listOf(Color(0xFFFBBF24), Color.Transparent)), CircleShape)
        ) {
            Box(modifier = Modifier.align(Alignment.Center).size(60.dp).background(Color(0xFFFBBF24), CircleShape))
        }

        // Moon
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = moonOffsetY.dp)
                .size(70.dp)
                .graphicsLayer { alpha = moonAlpha }
                .background(Color(0xFFE2E8F0), CircleShape)
        ) {
            Box(modifier = Modifier.offset(x = 10.dp, y = 10.dp).size(16.dp).background(Color.Black.copy(alpha = 0.1f), CircleShape))
            Box(modifier = Modifier.offset(x = 40.dp, y = 30.dp).size(24.dp).background(Color.Black.copy(alpha = 0.1f), CircleShape))
            Box(modifier = Modifier.offset(x = 20.dp, y = 45.dp).size(10.dp).background(Color.Black.copy(alpha = 0.1f), CircleShape))
        }
    }
}

@Composable
fun ParticlesLayer(isDay: Boolean) {
    var ticks by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            ticks++
        }
    }

    val particles = remember {
        List(25) {
            Particle(
                id = it,
                x = Random.nextFloat(),
                y = Random.nextFloat() * 1000f,
                size = Random.nextFloat() * 10f + 5f,
                speedY = Random.nextFloat() * 2f + 1f,
                swaySpeed = Random.nextFloat() * 0.05f,
                swayAmount = Random.nextFloat() * 2f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val particleColor = if (isDay) Color(0xFF22C55E).copy(alpha = 0.3f) else Color(0xFF93C5FD).copy(alpha = 0.4f)

        particles.forEach { p ->
            p.y += p.speedY
            if (p.y > h + 50f) {
                p.y = -50f
                p.x = Random.nextFloat()
            }
            val currentX = (p.x * w) + sin(ticks * p.swaySpeed) * (p.swayAmount * 10f)
            
            drawCircle(
                color = particleColor,
                radius = p.size,
                center = Offset(currentX, p.y)
            )
        }
    }
}

@Composable
fun ConfettiEffect() {
    var ticks by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (ticks < 200) {
            delay(16)
            ticks++
        }
    }
    
    val confettis = remember {
        val colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFC084FC), Color(0xFFF472B6), Color(0xFFFBBF24))
        List(80) {
            object {
                var x = Random.nextFloat()
                var y = -Random.nextFloat() * 200f
                val speedX = (Random.nextFloat() - 0.5f) * 10f
                val speedY = Random.nextFloat() * 15f + 5f
                val color = colors.random()
                val size = Random.nextFloat() * 15f + 10f
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        confettis.forEach { c ->
            c.x += c.speedX / size.width
            c.y += c.speedY
            drawRect(
                color = c.color,
                topLeft = Offset(c.x * size.width, c.y),
                size = Size(c.size, c.size)
            )
        }
    }
}
