package com.example

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PremiumGlassModifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.draw.drawBehind
import com.airbnb.lottie.compose.*

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

    val phaseColor = when {
        progress <= 0.15f -> Color(0xFF00E5FF)
        progress < 0.95f -> Color(0xFFD4AF37) // Luxurious Gold
        else -> Color(0xFF00E676) // Emerald Green
    }

    val phaseText = when {
        progress == 0f -> "Terreno & Fundações"
        progress <= 0.15f -> "Estrutura Subterrânea"
        progress < 0.50f -> "Lajes Iniciais"
        progress < 0.95f -> "Fachada & Pavimentos"
        progress < 1.0f -> "Acabamentos Finais"
        else -> "Obra Concluída"
    }

    val animatedPhaseColor by animateColorAsState(
        targetValue = phaseColor,
        animationSpec = tween(400),
        label = "ColorPhaseTransition"
    )

    val scrollState = rememberScrollState()
    val isCompact = scrollState.value > 100
    val normalAlpha by animateFloatAsState(targetValue = if (isCompact) 0f else 1f, animationSpec = tween(250), label = "normalAlpha")
    val compactAlpha by animateFloatAsState(targetValue = if (isCompact) 1f else 0f, animationSpec = tween(250), label = "compactAlpha")
    val accentColor = Color(0xFFD4AF37) // Luxurious Gold

    // Lottie Composition
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://lottie.host/17e25fb5-db8d-4e20-a681-43da2d9489d8/rV3d8aQJtG.json")
    )
    
    // We bind Lottie progress to the user's selected progress instead of looping it


    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // Main Building Visual with Glassmorphism Wrapper
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .then(PremiumGlassModifier)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (composition == null) {
                    CircularProgressIndicator(color = accentColor)
                } else {
                    LottieAnimation(
                        composition = composition,
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                if (progress >= 1.0f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Concluído",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "BEM-VINDO AO NOVO LAR",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Premium Interactive Glassmorphism Control Panel
            var componentSize by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { componentSize = it }
                    .then(PremiumGlassModifier)
                    .drawBehind {
                        val strokeWidth = 3.dp.toPx()
                        val progressWidth = size.width * progress
                        
                        drawLine(
                            color = Color(0x1AD4AF37),
                            start = Offset(0f, size.height - strokeWidth / 2f),
                            end = Offset(size.width, size.height - strokeWidth / 2f),
                            strokeWidth = strokeWidth
                        )
                        
                        drawLine(
                            color = animatedPhaseColor,
                            start = Offset(0f, size.height - strokeWidth / 2f),
                            end = Offset(progressWidth, size.height - strokeWidth / 2f),
                            strokeWidth = strokeWidth
                        )
                        
                        if (progressWidth > 0f) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(animatedPhaseColor, Color.Transparent),
                                    center = Offset(progressWidth, size.height - strokeWidth / 2f),
                                    radius = 12.dp.toPx()
                                ),
                                radius = 12.dp.toPx(),
                                center = Offset(progressWidth, size.height - strokeWidth / 2f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
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
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = phaseText.uppercase(),
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = animatedPhaseColor,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraLight,
                        fontSize = 64.sp,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Details Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(PremiumGlassModifier)
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "DETALHES DA OBRA",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Previsão", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            Text("Dez 2026", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Status", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            Text(if (progress >= 1f) "Entregue" else "Em Andamento", color = animatedPhaseColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(120.dp)) 
        }

        // Top Bar Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (normalAlpha > 0.05f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = normalAlpha
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MEU APARTAMENTO",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }
            }

            if (compactAlpha > 0.05f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = compactAlpha
                            translationY = (1f - compactAlpha) * (-20f)
                        }
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
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
                    Text(
                        text = "MEU APÊ",
                        style = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp
                        )
                    )
                }
            }
        }
    }
}
