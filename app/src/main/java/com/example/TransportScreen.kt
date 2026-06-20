package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.TransportParada
import com.example.data.TransportTimeline
import com.example.viewmodel.TesseraViewModel
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportScreen(
    viewModel: TesseraViewModel,
    onHomeClick: () -> Unit
) {
    val context = LocalContext.current
    val timelines by viewModel.transportTimelines.collectAsState()
    val isLoading by viewModel.isLoadingTransport.collectAsState()
    val error by viewModel.transportError.collectAsState()
    val locationName by viewModel.userLocationName.collectAsState()

    val activeRouteSession by viewModel.activeRouteSession.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    val metroStatus by viewModel.metroStatus.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.any { it }
    }

    // Coordenadas padrão de SP (Paulista) caso não obtenha o GPS
    var currentLatitude by remember { mutableStateOf(-23.5615) }
    var currentLongitude by remember { mutableStateOf(-46.6387) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission")
    fun requestLocationAndFetchData() {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                }
                viewModel.fetchTransportData(currentLatitude, currentLongitude)
            }.addOnFailureListener {
                viewModel.fetchTransportData(currentLatitude, currentLongitude)
            }
        } else {
            // Solicita a permissão, mas busca os dados com coordenadas padrão para não travar a tela
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            viewModel.fetchTransportData(currentLatitude, currentLongitude)
        }
    }

    // Busca inicial de dados ao entrar na tela
    LaunchedEffect(hasLocationPermission) {
        requestLocationAndFetchData()
        viewModel.fetchMetroStatus()
    }

    // REDIRECIONAMENTO DE TELA DE ROTA ATIVA (Estado B)
    if (activeRouteSession != null) {
        TransportRouteScreen(
            viewModel = viewModel,
            routeSession = activeRouteSession!!,
            userLat = currentLatitude,
            userLng = currentLongitude,
            onBack = { viewModel.clearActiveRouteSession() }
        )
        return
    }

    var selectedFilter by remember { mutableStateOf("todos") } // "todos", "onibus", "metro"

    val filteredTimelines = remember(timelines, selectedFilter) {
        when (selectedFilter) {
            "onibus" -> timelines.filter { it.tipoTransporte == "onibus" }
            "metro" -> timelines.filter { it.tipoTransporte == "metro" || it.tipoTransporte == "trem" }
            else -> timelines
        }
    }

    val glassBackground = Color(0xFF070909).copy(alpha = 0.75f)
    val glassBorder = Color.White.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070909))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // HEADER PREMIUM
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, glassBorder, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Localização Atual",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF4FC3F7),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = locationName,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = { requestLocationAndFetchData() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, glassBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = Color.White)
                }
            }

            // CAMPO DE BUSCA MINIMALISTA "Para onde vamos?"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            viewModel.updateSearchSuggestions(it)
                            isSearchFocused = true
                        },
                        placeholder = {
                            Text(
                                text = "Para onde vamos?",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    viewModel.updateSearchSuggestions("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = Color.White)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                    )

                    // Autocomplete Suggestions Dropdown
                    if (isSearchFocused && searchSuggestions.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0F1212))
                                .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                searchSuggestions.forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                searchQuery = suggestion
                                                isSearchFocused = false
                                                viewModel.selectDestination(
                                                    suggestion,
                                                    currentLatitude,
                                                    currentLongitude
                                                )
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (suggestion.contains("Ônibus") || suggestion.contains("onibus")) Icons.Outlined.DirectionsBus else Icons.Outlined.DirectionsTransit,
                                            contentDescription = null,
                                            tint = Color(0xFF4FC3F7),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = suggestion,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // GRID DE STATUS COMPACTO (METRÔ E TREM)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "STATUS GLOBAL DE LINHAS",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            val linesToDisplay = listOf(
                Pair("1", "L1-Azul"),
                Pair("2", "L2-Verde"),
                Pair("3", "L3-Vermelha"),
                Pair("4", "L4-Amarela"),
                Pair("5", "L5-Lilás"),
                Pair("15", "L15-Prata")
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                items(linesToDisplay) { line ->
                    val matchingEmpresa = metroStatus.find { emp ->
                        emp.linhas?.any { it.codigo == line.first } == true
                    }
                    val matchingLinha = matchingEmpresa?.linhas?.find { it.codigo == line.first }
                    val situacao = matchingLinha?.status?.situacao ?: "Operação Normal"
                    val isNormal = situacao.contains("Normal", ignoreCase = true)
                    
                    val (badgeColor, textColor) = when (line.first) {
                        "1" -> Color(0xFF005CA9) to Color.White
                        "2" -> Color(0xFF008940) to Color.White
                        "3" -> Color(0xFFEE3E23) to Color.White
                        "4" -> Color(0xFFFFD100) to Color.Black
                        "5" -> Color(0xFF90278E) to Color.White
                        else -> Color(0xFF97A0A6) to Color.White
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = line.second,
                                    color = textColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = situacao,
                                color = if (isNormal) Color(0xFF30D158) else Color(0xFFFF9F0A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // BOTÕES DE FILTRO
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("todos", "Todos", Icons.Default.DirectionsCar),
                    Triple("onibus", "Ônibus", Icons.Outlined.DirectionsBus),
                    Triple("metro", "Metrô/Trem", Icons.Outlined.DirectionsTransit)
                ).forEach { (id, label, icon) ->
                    val isSelected = selectedFilter == id
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color(0xFF4FC3F7).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                            .border(1.dp, if (isSelected) Color(0xFF4FC3F7).copy(alpha = 0.4f) else glassBorder, RoundedCornerShape(18.dp))
                            .clickable { selectedFilter = id },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // AVISO DE PERMISSÃO PENDENTE
            if (!hasLocationPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB74D).copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFFB74D))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Precisamos do GPS ativo",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Para rastrear ônibus e estações próximas a você.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ativar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // LISTA DE TIMELINES
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF4FC3F7))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Cruzando dados geográficos...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (filteredTimelines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nenhum transporte por perto no momento.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(filteredTimelines) { timeline ->
                        TimelineCard(timeline = timeline)
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineCard(timeline: TransportTimeline) {
    val themeColor = remember(timeline.corTema) {
        try {
            Color(android.graphics.Color.parseColor(timeline.corTema))
        } catch (e: Exception) {
            Color(0xFF4FC3F7)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Topo do Card da Linha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Badge circular com o ID da linha (L1, 809P-10, etc.)
                    Box(
                        modifier = Modifier
                            .background(themeColor, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeline.linhaIdentificador,
                            color = if (timeline.corTema.lowercase() == "#ffffd100") Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = timeline.linhaNome,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (timeline.tipoTransporte == "onibus") "Ônibus SPTrans" else "Metrô / Trem",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Badge de status da operação
                val isNormal = timeline.statusLinha.contains("Normal", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isNormal) Color(0xFF34C759).copy(alpha = 0.1f)
                            else Color(0xFFFF9500).copy(alpha = 0.1f)
                        )
                        .border(
                            1.dp,
                            if (isNormal) Color(0xFF34C759).copy(alpha = 0.2f)
                            else Color(0xFFFF9500).copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = timeline.statusLinha,
                        color = if (isNormal) Color(0xFF30D158) else Color(0xFFFF9F0A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // O CORE: DESENHO DA TIMELINE VERTICAL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp)
            ) {
                timeline.proximasParadas.forEachIndexed { index, parada ->
                    val isLast = index == timeline.proximasParadas.size - 1
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Desenho do nó (Bolinha e Linha vertical conectora)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(24.dp)
                        ) {
                            // Nó (Bolinha)
                            when (parada.status) {
                                "atual" -> {
                                    // Efeito concêntrico com animação pulsar
                                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                    val scale by infiniteTransition.animateFloat(
                                        initialValue = 8.dp.value,
                                        targetValue = 18.dp.value,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1500, easing = EaseInOutSine),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "scale"
                                    )
                                    Box(
                                        modifier = Modifier.size(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Círculo de glow pulsante
                                        Box(
                                            modifier = Modifier
                                                .size(scale.dp)
                                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                        )
                                        // Círculo borda externa
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .border(2.dp, Color.White, CircleShape)
                                                .background(themeColor, CircleShape)
                                        )
                                        // Círculo central
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }
                                "passou" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color.White.copy(alpha = 0.5f), CircleShape)
                                        )
                                    }
                                }
                                "destino" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .border(2.dp, Color.White, CircleShape)
                                                .background(Color.Transparent, CircleShape)
                                        )
                                    }
                                }
                                else -> { // "proxima"
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                                        )
                                    }
                                }
                            }

                            // Linha conectora vertical
                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(44.dp)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = if (parada.status == "passou") {
                                                    listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.3f))
                                                } else {
                                                    listOf(themeColor, themeColor)
                                                }
                                            )
                                        )
                                )
                            }
                        }

                        // Informações do ponto ao lado do nó
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (isLast) 0.dp else 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(
                                    text = parada.paradaNome,
                                    color = if (parada.status == "passou") Color.White.copy(alpha = 0.4f) else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (parada.status == "atual") FontWeight.Bold else FontWeight.Medium
                                )
                                parada.mensagem?.let { msg ->
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg,
                                        color = if (parada.status == "atual") Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            Text(
                                text = parada.horarioPrevisto,
                                color = if (parada.status == "passou") Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(0.5f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            // RODAPÉ COM INDICADOR DE CHEGADA/TEMPO RESTANTE (se aplicável ao ônibus)
            if (timeline.tipoTransporte == "onibus" && timeline.tempoRestanteTotalMin > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.02f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF4FC3F7).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = Color(0xFF4FC3F7),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${timeline.tempoRestanteTotalMin} mins restantes",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFD54F).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mais próximo", color = Color(0xFFFFD54F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
