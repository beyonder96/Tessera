package com.example
import androidx.compose.material3.MaterialTheme

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
                    style = androidx.compose.ui.text.TextStyle(brush = lavaBrush),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-1).sp,
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
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma linha selecionada.\nConfigure as linhas de metrô e trem a monitorar nas Configurações.",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            todasLinhas.forEach { (empresa, line) ->
                                val color = getMetroLineColor(line.nome, line.codigo)
                                val statusTxt = line.status?.situacao ?: "Desconhecido"
                                val isNormal = line.status?.operacaoNormal == true && 
                                    (statusTxt.equals("Operação Normal", ignoreCase = true) || statusTxt.equals("Normal", ignoreCase = true))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(86.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                ) {
                                    // Animated Train Background
                                    AnimatedMetroTrain(
                                        lineColor = color.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxSize().padding(top = 20.dp)
                                    )
                                    
                                    // Color Band
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .fillMaxHeight()
                                            .background(color)
                                            .align(Alignment.CenterStart)
                                    )
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 20.dp, end = 16.dp),
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
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(line.nome, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
                        Text("Escolher Linha", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isLoadingBus && savedBusLines.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = busAccentColor)
                    }
                } else if (busError != null && savedBusLines.isEmpty()) {
                    Text(text = busError ?: "Erro desconhecido", color = Color(0xFFE57373), modifier = Modifier.padding(16.dp))
                } else if (savedBusLines.isEmpty()) {
                    Text("Nenhuma linha salva no momento.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        savedBusLines.forEach { bus ->
                            val busColor = getBusLineColor(bus.lineNumber)
                            val isOffline = bus.estimatedArrivalText.contains("Sem") || bus.estimatedArrivalText.contains("Offline")
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = 0.02f))
                                    // Minimalist border with gradient
                                    .border(
                                        width = 0.5.dp, 
                                        brush = Brush.verticalGradient(
                                            colors = listOf(busColor.copy(alpha = 0.4f), Color.Transparent)
                                        ), 
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .padding(20.dp)
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
                                                    .background(Brush.linearGradient(listOf(busColor.copy(alpha = 0.3f), busColor.copy(alpha = 0.1f))))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(bus.lineNumber, color = busColor, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = bus.destination, 
                                                    color = MaterialTheme.colorScheme.onBackground, 
                                                    fontWeight = FontWeight.Bold, 
                                                    fontSize = 18.sp,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Via ${bus.stopName}", 
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), 
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        
                                        IconButton(
                                            onClick = { viewModel.removeBusLine(bus.lineCode) },
                                            modifier = Modifier.size(32.dp).padding(start = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remover Linha",
                                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    // Arrival section with Pulsing Text and Progress Line
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        // Simple Progress Bar indicating time (fake visual progress)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 24.dp, bottom = 4.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color.White.copy(alpha = 0.1f))
                                        ) {
                                            if (!isOffline) {
                                                val infiniteTransition = rememberInfiniteTransition(label = "bus_progress")
                                                val progress by infiniteTransition.animateFloat(
                                                    initialValue = 0f,
                                                    targetValue = 1f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(2000, easing = FastOutSlowInEasing),
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
                                        
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "CHEGA EM", 
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), 
                                                fontSize = 10.sp, 
                                                fontWeight = FontWeight.Bold, 
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
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
        Dialog(onDismissRequest = { 
            showSearchDialog = false 
            searchQuery = ""
            viewModel.searchBusLines("")
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.7f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF121212).copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Buscar Linha de Ônibus",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.searchBusLines(it)
                        },
                        placeholder = { Text("Ex: 8000, 715M, Lapa...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = busAccentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = busAccentColor
                        )
                    )

                    if (isSearchingBus) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = busAccentColor)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (busSearchResults.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (searchQuery.isEmpty()) "Digite para buscar linhas..." else "Nenhuma linha encontrada.",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            } else {
                                items(busSearchResults) { result ->
                                    val dest = if (result.sl == 1) result.ts else result.tp
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.03f))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.saveBusLine(result.cl, result.lt, dest)
                                                showSearchDialog = false
                                                searchQuery = ""
                                                viewModel.searchBusLines("")
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
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
                                                    Text(result.lt, color = busAccentColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
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
                                                text = if (result.sl == 1) "Sentido Ida" else "Sentido Volta",
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Adicionar",
                                            tint = busAccentColor,
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar", color = MaterialTheme.colorScheme.onBackground)
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
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "train_move"
    )

    val sysOnBackground = MaterialTheme.colorScheme.onBackground
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val trainWidth = w * 0.45f
        val trainHeight = h * 0.25f
        val carWidth = trainWidth * 0.45f
        val gap = trainWidth * 0.05f
        
        val startX = w * translationX
        val yPos = h * 0.8f
        
        // draw track line
        drawLine(
            color = sysOnBackground.copy(alpha = 0.05f),
            start = Offset(0f, yPos),
            end = Offset(w, yPos),
            strokeWidth = 1.dp.toPx()
        )
        
        // Train moving
        translate(left = startX) {
            // First car
            drawRoundRect(
                color = lineColor,
                topLeft = Offset(0f, yPos - trainHeight),
                size = androidx.compose.ui.geometry.Size(carWidth, trainHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            // Second car
            drawRoundRect(
                color = lineColor,
                topLeft = Offset(carWidth + gap, yPos - trainHeight),
                size = androidx.compose.ui.geometry.Size(carWidth, trainHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            // Windows
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.3f),
                topLeft = Offset(carWidth * 0.2f, yPos - trainHeight + 2.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(carWidth * 0.6f, trainHeight * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.3f),
                topLeft = Offset(carWidth + gap + carWidth * 0.2f, yPos - trainHeight + 2.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(carWidth * 0.6f, trainHeight * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
        }
    }
}

@Composable
fun PulsingArrivalText(text: String, isOffline: Boolean, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Text(
        text = text,
        color = if (isOffline) Color(0xFFFF6B6B) else accentColor.copy(alpha = alpha),
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.SansSerif,
        fontSize = 18.sp,
        style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
                color = if (isOffline) Color(0xFFFF6B6B).copy(alpha = 0.5f) else accentColor.copy(alpha = 0.5f),
                blurRadius = if (isOffline) 0f else 8f * alpha
            )
        )
    )
}
