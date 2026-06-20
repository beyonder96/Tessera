package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.getMetroLineColor
import com.example.viewmodel.TesseraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportScreen(
    onNavigateBack: () -> Unit,
    viewModel: TesseraViewModel
) {
    val metroStatus by viewModel.metroStatus.collectAsState()
    val isLoadingMetro by viewModel.isLoadingMetroStatus.collectAsState()
    val metroError by viewModel.metroError.collectAsState()

    val savedBusLines by viewModel.savedBusLines.collectAsState()
    val isLoadingBus by viewModel.isLoadingBus.collectAsState()
    val busError by viewModel.busError.collectAsState()

    LaunchedEffect(Unit) {
        if (metroStatus.isEmpty()) viewModel.fetchMetroStatus()
        if (savedBusLines.isEmpty()) viewModel.fetchBusPredictions()
    }

    val scrollState = androidx.compose.foundation.rememberScrollState()
    val accentColor = Color(0xFF4FC3F7) // SPTrans/Metro Cyan
    val busAccentColor = Color(0xFFFFA726)

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Hero Image with Fade to Black
            val scrollOffset = scrollState.value
            val baseBlur = (scrollOffset * 0.04f).coerceIn(0f, 16f)
            val blurRadius = baseBlur.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.widget_background), // Using existing background
                    contentDescription = "SP City",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blurRadius)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f), Color.Black),
                                startY = 200f
                            )
                        )
                )
            }

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(top = 220.dp, start = 24.dp, end = 24.dp)
            ) {
                // Header Title inside scroll
                Text(
                    text = "TRANSPORTE SP",
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Status e Previsões em Tempo Real",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Metrô & CPTM Section
                Text(
                    text = "METRÔ & CPTM",
                    fontFamily = FontFamily.SansSerif,
                    color = accentColor,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isLoadingMetro && metroStatus.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor)
                    }
                } else if (metroError != null && metroStatus.isEmpty()) {
                    Text(text = metroError ?: "Erro desconhecido", color = Color(0xFFE57373), modifier = Modifier.padding(16.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val todasLinhas = metroStatus.flatMap { it.linhas ?: emptyList() }
                        todasLinhas.forEach { line ->
                            val color = getMetroLineColor(line.nome, line.codigo)
                            val isNormal = line.status?.operacaoNormal == true
                            val statusTxt = line.status?.situacao ?: "Desconhecido"
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color.copy(alpha = 0.2f))
                                                .border(1.dp, color, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = line.codigo,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(line.nome, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(statusTxt, color = if (isNormal) Color(0xFF81C784) else Color(0xFFFF6B6B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    if (!isNormal) {
                                        Icon(Icons.Outlined.Warning, contentDescription = "Alerta", tint = Color(0xFFFF6B6B))
                                    } else {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Normal", tint = Color(0xFF81C784), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // SPTrans Section
                Text(
                    text = "MEUS ÔNIBUS",
                    fontFamily = FontFamily.SansSerif,
                    color = busAccentColor,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isLoadingBus && savedBusLines.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = busAccentColor)
                    }
                } else if (busError != null && savedBusLines.isEmpty()) {
                    Text(text = busError ?: "Erro desconhecido", color = Color(0xFFE57373), modifier = Modifier.padding(16.dp))
                } else if (savedBusLines.isEmpty()) {
                    Text("Nenhuma linha salva no momento.", color = Color.White.copy(alpha = 0.5f))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        savedBusLines.forEach { bus ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(0.5.dp, busAccentColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(busAccentColor.copy(alpha = 0.15f))
                                                        .border(0.5.dp, busAccentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(bus.lineNumber, color = busAccentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(bus.destination, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Ponto: ${bus.stopName}", 
                                                color = Color.White.copy(alpha = 0.6f), 
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                                            Text("CHEGA EM", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                            val isOffline = bus.estimatedArrivalText.contains("Sem") || bus.estimatedArrivalText.contains("Offline")
                                            Text(
                                                text = bus.estimatedArrivalText,
                                                color = if (isOffline) Color(0xFFFF6B6B) else accentColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }

            // Top Bar & Collapse Logic
            val isCompact = scrollState.value > 120
            val normalAlphaState = animateFloatAsState(targetValue = if (isCompact) 0f else 1f, animationSpec = tween(250), label = "normalAlpha")
            val compactAlphaState = animateFloatAsState(targetValue = if (isCompact) 1f else 0f, animationSpec = tween(250), label = "compactAlpha")
            
            val normalAlpha = normalAlphaState.value
            val compactAlpha = compactAlphaState.value

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 16.dp, end = 16.dp)
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
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                        }

                        IconButton(
                            onClick = { 
                                viewModel.fetchMetroStatus()
                                viewModel.fetchBusPredictions()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Color.White)
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
                        Icon(Icons.Outlined.DirectionsTransit, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TRANSPORTE SP",
                            style = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
