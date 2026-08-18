package com.example
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.*

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import com.example.data.getMetroLineColor
import com.example.viewmodel.TesseraViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun getBusLineColor(lineNumber: String): Color {
    if (lineNumber.isEmpty()) return Color(0xFFFFA726) // default Orange
    return when(lineNumber.first()) {
        '1' -> Color(0xFF81C784) // Light Green
        '2' -> Color(0xFF1E88E5) // Dark Blue
        '3' -> Color(0xFFFFD54F) // Yellow
        '4' -> Color(0xFFE53935) // Red
        '5' -> Color(0xFF388E3C) // Dark Green
        '6' -> Color(0xFF4FC3F7) // Light Blue
        '7' -> Color(0xFF880E4F) // Burgundy
        '8' -> Color(0xFFFF9800) // Orange
        '9' -> Color(0xFF9E9E9E) // Gray
        else -> Color(0xFFFFA726)
    }
}

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

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    val selectedLinesKeys = remember {
        sharedPrefs.getStringSet("metro_monitored_lines", emptySet()) ?: emptySet()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            viewModel.fetchRealLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (!hasFineLocation || !hasCoarseLocation) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            viewModel.fetchRealLocation(context)
        }

        if (metroStatus.isEmpty()) viewModel.fetchMetroStatus()
        viewModel.fetchBusPredictions()
    }

    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val busSearchResults by viewModel.busSearchResults.collectAsState()
    val isSearchingBus by viewModel.isSearchingBus.collectAsState()

    val lavaBrush = com.example.ui.components.rememberLavaBrush()
    val scrollState = androidx.compose.foundation.rememberScrollState()
    val accentColor = Color(0xFF4FC3F7) // SPTrans/Metro Cyan
    val busAccentColor = remember(savedBusLines) {
        if (savedBusLines.isNotEmpty()) {
            getBusLineColor(savedBusLines.first().lineNumber)
        } else {
            Color(0xFFFFA726)
        }
    }

    val bgThemeColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = bgThemeColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Hero Image with Fade to Background
            val scrollOffset = scrollState.value
            val baseBlur = (scrollOffset * 0.04f).coerceIn(0f, 16f)
            val blurRadius = baseBlur.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1543085542-a72eb3cc4cfa?q=80&w=800&auto=format&fit=crop",
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
                                colors = listOf(
                                    Color.Transparent,
                                    bgThemeColor.copy(alpha = 0.6f),
                                    bgThemeColor
                                ),
                                startY = 160f
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
                    style = androidx.compose.ui.text.TextStyle(brush = lavaBrush),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Status e Previsões em Tempo Real",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    // Filter the subway/train lines using the monitored set
                    val todasLinhas = remember(metroStatus, selectedLinesKeys) {
                        val list = mutableListOf<Pair<com.example.data.MetroEmpresaStatus, com.example.data.MetroLinhaStatus>>()
                        metroStatus.forEach { empresa ->
                            empresa.linhas?.forEach { linha ->
                                val lineKey = "${empresa.id}_${linha.codigo}"
                                if (selectedLinesKeys.contains(lineKey)) {
                                    list.add(empresa to linha)
                                }
                            }
                        }
                        list
                    }

                    if (todasLinhas.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(themedCardBackground())
                                .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.DirectionsTransit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Nenhuma linha selecionada",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Configure as linhas de metrô e trem a monitorar nas Configurações.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            todasLinhas.forEach { (empresa, line) ->
                                val color = getMetroLineColor(line.nome, line.codigo)
                                val statusTxt = line.status?.situacao ?: "Operação Normal"
                                val isNormal = line.status?.operacaoNormal == true && 
                                    (statusTxt.contains("Normal", ignoreCase = true))
                                val isSlow = statusTxt.contains("Reduzida", ignoreCase = true) || statusTxt.contains("Lentidão", ignoreCase = true)
                                val statusColor = when {
                                    isNormal -> Color(0xFF10B981)
                                    isSlow -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(96.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(themedCardBackground())
                                        .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
                                ) {
                                    // Animated Metro/CPTM Train Background
                                    AnimatedMetroTrain(
                                        lineColor = color,
                                        modifier = Modifier.fillMaxSize().padding(top = 28.dp)
                                    )
                                    
                                    // Subtle Left Accent Bar
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .fillMaxHeight()
                                            .background(color)
                                            .align(Alignment.CenterStart)
                                    )
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            // Line Code Badge
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(color.copy(alpha = 0.16f))
                                                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = line.codigo,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = line.nome,
                                                        color = MaterialTheme.colorScheme.onBackground,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 15.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (empresa.nome.contains("cptm", ignoreCase = true)) "CPTM" else "METRÔ",
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(statusColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = statusTxt,
                                                        color = statusColor,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    if (!line.status?.atualizadoHa.isNullOrBlank()) {
                                                        Text(
                                                            text = " • ${line.status?.atualizadoHa}",
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Status Icon
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(statusColor.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isNormal) {
                                                Icon(Icons.Outlined.CheckCircle, contentDescription = "Normal", tint = statusColor, modifier = Modifier.size(16.dp))
                                            } else {
                                                Icon(Icons.Outlined.Warning, contentDescription = "Alerta", tint = statusColor, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // SPTrans Section with Search & Remove controls
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MEUS ÔNIBUS",
                        fontFamily = FontFamily.SansSerif,
                        color = busAccentColor,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TextButton(
                        onClick = { showSearchDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = busAccentColor)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buscar Linha", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (isLoadingBus && savedBusLines.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = busAccentColor)
                    }
                } else if (busError != null && savedBusLines.isEmpty()) {
                    Text(text = busError ?: "Erro desconhecido", color = Color(0xFFE57373), modifier = Modifier.padding(16.dp))
                } else if (savedBusLines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(themedCardBackground())
                            .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsBus,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhuma linha adicionada",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toque em 'Buscar Linha' acima para monitorar ônibus em tempo real.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        savedBusLines.forEach { bus ->
                            val busColor = getBusLineColor(bus.lineNumber)
                            val isOffline = bus.estimatedArrivalText.contains("Sem") || bus.estimatedArrivalText.contains("Offline")
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(themedCardBackground())
                                    .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
                                    .padding(18.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            // Modern Bus Badge
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(busColor.copy(alpha = 0.16f))
                                                    .border(1.dp, busColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(bus.lineNumber, color = busColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = bus.destination, 
                                                    color = MaterialTheme.colorScheme.onBackground, 
                                                    fontWeight = FontWeight.SemiBold, 
                                                    fontSize = 16.sp,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Via ${bus.stopName}", 
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        
                                        IconButton(
                                            onClick = { viewModel.removeBusLine(bus.lineCode) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remover Linha",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(18.dp))
                                    
                                    // Arrival section with Pulsing Text and Progress Line
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Simple Progress Bar indicating time
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 20.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(themedSubtleBackground())
                                        ) {
                                            if (!isOffline) {
                                                val infiniteTransition = rememberInfiniteTransition(label = "bus_progress")
                                                val progress by infiniteTransition.animateFloat(
                                                    initialValue = 0f,
                                                    targetValue = 1f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(2400, easing = FastOutSlowInEasing),
                                                        repeatMode = RepeatMode.Restart
                                                    ),
                                                    label = "bus_progress_anim"
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(progress)
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                colors = listOf(Color.Transparent, busColor)
                                                            )
                                                        )
                                                )
                                            }
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "PREVISÃO: ", 
                                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                                fontSize = 11.sp, 
                                                fontWeight = FontWeight.Medium, 
                                                letterSpacing = 0.5.sp
                                            )
                                            PulsingArrivalText(text = bus.estimatedArrivalText, isOffline = isOffline, accentColor = busColor)
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
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.onBackground)
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
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = MaterialTheme.colorScheme.onBackground)
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
                            text = "TRANSPORTE SP",
                            style = androidx.compose.ui.text.TextStyle(
                                brush = nameGlowBrush,
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

    // Bus Search Dialog
    if (showSearchDialog) {
        var selectedFilter by remember { mutableStateOf("TODAS") } // "TODAS", "IDA", "VOLTA", "NOTURNAS"

        Dialog(onDismissRequest = { 
            showSearchDialog = false 
            searchQuery = ""
            viewModel.searchBusLines("")
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.75f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(themedOverlayBackground())
                    .border(1.dp, themedCardBorder(), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Buscar Linha de Ônibus",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = {
                                showSearchDialog = false
                                searchQuery = ""
                                viewModel.searchBusLines("")
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Fechar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.searchBusLines(it)
                        },
                        placeholder = { Text("Número ou destino (ex: 8000, Lapa)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = themedOutlinedTextFieldColors()
                    )

                    // Filter Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filterOptions = listOf("TODAS" to "Todas", "IDA" to "Ida", "VOLTA" to "Volta", "NOTURNAS" to "Noturnas")
                        filterOptions.forEach { (key, label) ->
                            val isSelected = selectedFilter == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) busAccentColor.copy(alpha = 0.18f) else themedSubtleBackground()
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) busAccentColor.copy(alpha = 0.6f) else themedSubtleBorder(),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedFilter = key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) busAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (isSearchingBus) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = busAccentColor, modifier = Modifier.size(32.dp))
                        }
                    } else {
                        val filteredResults = remember(busSearchResults, selectedFilter) {
                            when (selectedFilter) {
                                "IDA" -> busSearchResults.filter { it.sl == 1 }
                                "VOLTA" -> busSearchResults.filter { it.sl == 2 }
                                "NOTURNAS" -> busSearchResults.filter { it.lt.startsWith("N", ignoreCase = true) }
                                else -> busSearchResults
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (filteredResults.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (searchQuery.isEmpty()) "Digite o número ou nome da linha..." else "Nenhuma linha encontrada para o filtro.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(filteredResults) { result ->
                                    val dest = if (result.sl == 1) result.ts else result.tp
                                    val lineColor = getBusLineColor(result.lt)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(themedSubtleBackground())
                                            .border(1.dp, themedSubtleBorder(), RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.saveBusLine(result.cl, result.lt, dest)
                                                showSearchDialog = false
                                                searchQuery = ""
                                                viewModel.searchBusLines("")
                                            }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(lineColor.copy(alpha = 0.16f))
                                                        .border(0.5.dp, lineColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(result.lt, color = lineColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = dest,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (result.sl == 1) "Sentido Ida (Terminal Secundário)" else "Sentido Volta (Terminal Principal)",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Adicionar",
                                            tint = lineColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            showSearchDialog = false
                            searchQuery = ""
                            viewModel.searchBusLines("")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themedSubtleBackground(),
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        border = BorderStroke(1.dp, themedButtonBorder()),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar", color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedMetroTrain(lineColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "train")
    val translationX by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "train_move"
    )
    val lightPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "light_pulse"
    )

    val sysOnBackground = MaterialTheme.colorScheme.onBackground
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val trainWidth = (w * 0.42f).coerceAtMost(260.dp.toPx())
        val trainHeight = 16.dp.toPx()
        val carCount = 3
        val gap = 4.dp.toPx()
        val carWidth = (trainWidth - (gap * (carCount - 1))) / carCount
        
        val startX = w * translationX
        val trackY = h * 0.72f
        
        // Track line
        drawLine(
            color = sysOnBackground.copy(alpha = 0.08f),
            start = Offset(0f, trackY),
            end = Offset(w, trackY),
            strokeWidth = 1.5.dp.toPx()
        )
        
        // Station nodes along track
        val nodeCount = 5
        for (i in 0 until nodeCount) {
            val nodeX = w * ((i + 0.5f) / nodeCount)
            drawCircle(
                color = sysOnBackground.copy(alpha = 0.12f),
                radius = 2.dp.toPx(),
                center = Offset(nodeX, trackY)
            )
        }
        
        // Train moving along track
        translate(left = startX) {
            // Front Headlight beam
            val frontX = trainWidth
            val beamLength = 40.dp.toPx()
            drawOval(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFFF9C4).copy(alpha = 0.45f * lightPulse), Color.Transparent),
                    startX = frontX,
                    endX = frontX + beamLength
                ),
                topLeft = Offset(frontX, trackY - trainHeight * 0.8f),
                size = androidx.compose.ui.geometry.Size(beamLength, trainHeight * 0.7f)
            )

            // Draw each car
            for (carIndex in 0 until carCount) {
                val carX = carIndex * (carWidth + gap)
                val isFrontCar = carIndex == carCount - 1
                val isRearCar = carIndex == 0
                
                // Car Body
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.35f),
                            lineColor.copy(alpha = 0.18f)
                        ),
                        startY = trackY - trainHeight,
                        endY = trackY
                    ),
                    topLeft = Offset(carX, trackY - trainHeight),
                    size = androidx.compose.ui.geometry.Size(carWidth, trainHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(if (isFrontCar) 5.dp.toPx() else 2.5.dp.toPx())
                )
                
                // Roof Accent Line
                drawLine(
                    color = lineColor.copy(alpha = 0.7f),
                    start = Offset(carX + 2.dp.toPx(), trackY - trainHeight + 1.dp.toPx()),
                    end = Offset(carX + carWidth - 2.dp.toPx(), trackY - trainHeight + 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )

                // Window Bands
                val windowY = trackY - trainHeight + 3.dp.toPx()
                val windowH = trainHeight * 0.42f
                val windowW = carWidth * 0.22f
                
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(carX + carWidth * 0.2f, windowY),
                    size = androidx.compose.ui.geometry.Size(windowW, windowH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
                )
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(carX + carWidth * 0.55f, windowY),
                    size = androidx.compose.ui.geometry.Size(windowW, windowH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
                )

                // Front Windshield / Headlight on Lead Car
                if (isFrontCar) {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(carX + carWidth * 0.78f, windowY),
                        size = androidx.compose.ui.geometry.Size(carWidth * 0.18f, windowH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                    )
                    // Headlight LED
                    drawCircle(
                        color = Color(0xFFFFF9C4),
                        radius = 1.5.dp.toPx(),
                        center = Offset(carX + carWidth - 1.dp.toPx(), trackY - trainHeight * 0.35f)
                    )
                }

                // Rear Taillight on Rear Car
                if (isRearCar) {
                    drawCircle(
                        color = Color(0xFFFF5252).copy(alpha = 0.85f),
                        radius = 1.2.dp.toPx(),
                        center = Offset(carX + 1.dp.toPx(), trackY - trainHeight * 0.35f)
                    )
                }
            }
        }
    }
}

@Composable
fun PulsingArrivalText(text: String, isOffline: Boolean, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Text(
        text = text,
        color = if (isOffline) Color(0xFFEF4444) else accentColor.copy(alpha = alpha),
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp
    )
}
