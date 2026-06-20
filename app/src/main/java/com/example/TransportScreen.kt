package com.example

import androidx.compose.animation.*
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transporte SP", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.fetchMetroStatus()
                        viewModel.fetchBusPredictions()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Seção 1 - Status do Metrô/CPTM
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DirectionsTransit, contentDescription = null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Status do Metrô e CPTM",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingMetro && metroStatus.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF4FC3F7))
                    }
                } else if (metroError != null && metroStatus.isEmpty()) {
                    Text(text = metroError ?: "Erro desconhecido", color = Color(0xFFE57373), modifier = Modifier.padding(16.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val todasLinhas = metroStatus.flatMap { it.linhas ?: emptyList() }
                        todasLinhas.forEach { line ->
                            val color = getMetroLineColor(line.nome, line.codigo)
                            val isNormal = line.status?.operacaoNormal == true
                            val statusTxt = line.status?.situacao ?: "Desconhecido"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = line.codigo,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(line.nome, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                            Text(statusTxt, color = if (isNormal) Color(0xFF81C784) else Color(0xFFE57373), fontSize = 12.sp)
                                        }
                                    }
                                    if (!isNormal) {
                                        Icon(Icons.Outlined.Warning, contentDescription = "Alerta", tint = Color(0xFFE57373))
                                    } else {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Normal", tint = Color(0xFF81C784), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Seção 2 - Meus Ônibus (SPTrans)
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DirectionsBus, contentDescription = null, tint = Color(0xFFFFA726), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Meus Ônibus",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingBus && savedBusLines.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFFA726))
                    }
                } else if (busError != null && savedBusLines.isEmpty()) {
                    Text(text = busError ?: "Erro desconhecido", color = Color(0xFFE57373), modifier = Modifier.padding(16.dp))
                } else if (savedBusLines.isEmpty()) {
                    Text("Nenhuma linha salva no momento.", color = Color.White.copy(alpha = 0.6f))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        savedBusLines.forEach { bus ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFFFA726).copy(alpha = 0.2f))
                                                        .border(1.dp, Color(0xFFFFA726), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(bus.lineNumber, color = Color(0xFFFFA726), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(bus.destination, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Ponto: ${bus.stopName}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Chega em", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                            Text(
                                                text = bus.estimatedArrivalText,
                                                color = if (bus.estimatedArrivalText.contains("Sem") || bus.estimatedArrivalText.contains("Offline")) Color(0xFFE57373) else Color(0xFF4FC3F7),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = { /* TODO: Expandir itinerário */ },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                                    ) {
                                        Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Ver Itinerário")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
