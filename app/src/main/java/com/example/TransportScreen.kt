package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsTransit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.getMetroLineColor
import com.example.ui.components.*
import com.example.ui.theme.PrimaryTeal
import com.example.viewmodel.TesseraViewModel

enum class TransportTab(val title: String) {
    ALL("Geral"),
    METRO("Metrô & Trem"),
    BUS("Ônibus")
}

fun getBusLineColor(lineNumber: String): Color {
    if (lineNumber.isEmpty()) return Color(0xFFFFA726)
    return when (lineNumber.first()) {
        '1' -> Color(0xFF81C784)
        '2' -> Color(0xFF1E88E5)
        '3' -> Color(0xFFFFD54F)
        '4' -> Color(0xFFE53935)
        '5' -> Color(0xFF388E3C)
        '6' -> Color(0xFF4FC3F7)
        '7' -> Color(0xFF880E4F)
        '8' -> Color(0xFFFF9800)
        '9' -> Color(0xFF9E9E9E)
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
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE) }
    val selectedLinesKeys = remember {
        sharedPrefs.getStringSet("metro_monitored_lines", emptySet()) ?: emptySet()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            viewModel.fetchRealLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine || !hasCoarse) {
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
    var selectedTab by remember { mutableStateOf(TransportTab.ALL) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Smooth infinite rotation when refreshing
    val refreshTransition = rememberInfiniteTransition(label = "refreshAnim")
    val refreshAngle by refreshTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refreshRotation"
    )

    val monitoredMetroLines = remember(metroStatus, selectedLinesKeys) {
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Clean & Streamlined Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(themedSubtleBackground())
                                .border(0.5.dp, themedSubtleBorder(), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Voltar",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Transporte",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "São Paulo • Tempo Real",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                isRefreshing = true
                                viewModel.fetchMetroStatus(forceRefresh = true)
                                viewModel.fetchBusPredictions()
                                isRefreshing = false
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(themedSubtleBackground())
                                .border(0.5.dp, themedSubtleBorder(), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Atualizar",
                                tint = PrimaryTeal,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(if (isLoadingMetro || isLoadingBus) refreshAngle else 0f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
        ) {
            // Segmented Sub-Tab Filter
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(themedSubtleBackground())
                        .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransportTab.values().forEach { tab ->
                        val isSelected = tab == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) themedCardBackground() else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) themedSubtleBorder() else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // METRÔ & CPTM Section
            if (selectedTab == TransportTab.ALL || selectedTab == TransportTab.METRO) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "METRÔ & TREM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = PrimaryTeal
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryTeal.copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "ARTESP / CPTM",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryTeal
                                )
                            }
                        }

                        if (monitoredMetroLines.isNotEmpty()) {
                            val normalCount = monitoredMetroLines.count { 
                                it.second.status?.operacaoNormal == true && (it.second.status?.situacao ?: "").contains("Normal", ignoreCase = true)
                            }
                            Text(
                                text = "$normalCount/${monitoredMetroLines.size} normais",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (isLoadingMetro && metroStatus.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryTeal)
                        }
                    }
                } else if (metroError != null && metroStatus.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(themedCardBackground())
                                .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = "Status temporariamente indisponível",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = metroError ?: "Verifique sua conexão à internet.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Button(
                                    onClick = { viewModel.fetchMetroStatus(forceRefresh = true) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text("Tentar Novamente", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                }
                            }
                        }
                    }
                } else if (monitoredMetroLines.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(themedCardBackground())
                                .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.DirectionsTransit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhuma linha monitorada",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Configure as linhas de metrô nas Configurações do app.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(monitoredMetroLines) { (empresa, line) ->
                        MetroLineCompactCard(empresa = empresa, line = line)
                    }
                }
            }

            // ÔNIBUS SPTRANS Section
            if (selectedTab == TransportTab.ALL || selectedTab == TransportTab.BUS) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ÔNIBUS SPTRANS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFFFFA726)
                        )

                        TextButton(
                            onClick = { showSearchDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFFFA726))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buscar Linha", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFA726))
                        }
                    }
                }

                if (isLoadingBus && savedBusLines.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFFFA726))
                        }
                    }
                } else if (busError != null && savedBusLines.isEmpty()) {
                    item {
                        Text(text = busError ?: "Erro ao carregar ônibus", color = Color(0xFFEF5350), fontSize = 12.sp)
                    }
                } else if (savedBusLines.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(themedCardBackground())
                                .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.DirectionsBus,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhum ônibus adicionado",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Toque em 'Buscar Linha' acima para monitorar chegadas em tempo real.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(savedBusLines) { bus ->
                        BusLineCompactCard(
                            bus = bus,
                            onDelete = { viewModel.removeBusLine(bus.lineCode) }
                        )
                    }
                }
            }
        }
    }

    // Bus Search Dialog
    if (showSearchDialog) {
        BusSearchDialog(
            viewModel = viewModel,
            onDismiss = { showSearchDialog = false }
        )
    }
}

@Composable
private fun MetroLineCompactCard(
    empresa: com.example.data.MetroEmpresaStatus,
    line: com.example.data.MetroLinhaStatus
) {
    val color = getMetroLineColor(line.nome, line.codigo)
    val statusTxt = line.status?.situacao ?: "Operação Normal"
    val isNormal = line.status?.operacaoNormal == true && (statusTxt.contains("Normal", ignoreCase = true))
    val isSlow = statusTxt.contains("Reduzida", ignoreCase = true) || statusTxt.contains("Lentidão", ignoreCase = true)
    val statusColor = when {
        isNormal -> Color(0xFF10B981)
        isSlow -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    val cleanStatus = when {
        isNormal -> "Normal"
        isSlow -> "Velocidade Reduzida"
        else -> line.status?.classificacao?.ifBlank { statusTxt } ?: statusTxt
    }

    val updatedTime = com.example.data.formatMetroTime(line.status?.atualizadoEm, line.status?.atualizadoHa)
    val isCptm = empresa.nome.contains("cptm", ignoreCase = true)
    val operadoraLabel = if (isCptm) "CPTM" else "Metrô"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Linha Única Horizontal Minimalista
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Esquerda: Badge do Número com Cor + Operadora (sem nome redundante da linha)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Badge número da linha
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.20f))
                            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = line.codigo,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Operadora
                    Text(
                        text = operadoraLabel,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Direita: Status (ponto luminoso + texto) + Horário (HH:mm)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Ponto + Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = cleanStatus,
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Horário (ex: 11:38)
                    Text(
                        text = updatedTime,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Se houver ocorrência ou lentidão, exibe o detalhamento explicativo
            if (!isNormal && statusTxt.isNotBlank() && !statusTxt.equals("Operação Normal", ignoreCase = true)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.08f))
                        .border(0.5.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusTxt,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp
                    )
                }
            }

            // Animação de Trem na parte inferior do card
            AnimatedMetroTrain(
                lineColor = color,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
        }
    }
}

@Composable
private fun BusLineCompactCard(
    bus: com.example.data.SavedBusLine,
    onDelete: () -> Unit
) {
    val busColor = getBusLineColor(bus.lineNumber)
    val isOffline = bus.estimatedArrivalText.contains("Sem") || bus.estimatedArrivalText.contains("Offline")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(busColor.copy(alpha = 0.16f))
                            .border(1.dp, busColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(bus.lineNumber, color = busColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = bus.destination,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "Via ${bus.stopName}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remover",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Previsão & Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(themedSubtleBackground())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Próximo Ônibus",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isOffline) Color(0xFFEF5350) else PrimaryTeal)
                    )
                    Text(
                        text = bus.estimatedArrivalText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOffline) Color(0xFFEF5350) else PrimaryTeal
                    )
                }
            }

            // Animação de Ônibus na parte inferior do card
            AnimatedBusDrive(
                busColor = busColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
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
        val trainHeight = 15.dp.toPx()
        val carCount = 3
        val gap = 4.dp.toPx()
        val carWidth = (trainWidth - (gap * (carCount - 1))) / carCount
        
        val startX = w * translationX
        val trackY = h * 0.75f
        
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
            val beamLength = 36.dp.toPx()
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
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(if (isFrontCar) 4.dp.toPx() else 2.dp.toPx())
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
fun AnimatedBusDrive(busColor: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bus_drive")
    val translationX by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bus_move"
    )
    val lightPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bus_light_pulse"
    )

    val sysOnBackground = MaterialTheme.colorScheme.onBackground
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val busWidth = 72.dp.toPx()
        val busHeight = 15.dp.toPx()
        val roadY = h * 0.75f
        
        val startX = w * translationX
        
        // Road surface line
        drawLine(
            color = sysOnBackground.copy(alpha = 0.08f),
            start = Offset(0f, roadY),
            end = Offset(w, roadY),
            strokeWidth = 1.5.dp.toPx()
        )
        
        // Road dashed divider
        val dashCount = 8
        for (i in 0 until dashCount) {
            val dashX = w * (i.toFloat() / dashCount)
            drawLine(
                color = sysOnBackground.copy(alpha = 0.12f),
                start = Offset(dashX, roadY),
                end = Offset(dashX + (w / dashCount * 0.4f), roadY),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Bus moving along road
        translate(left = startX) {
            val frontX = busWidth
            val beamLength = 36.dp.toPx()
            
            // Headlight beam
            drawOval(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFFF9C4).copy(alpha = 0.45f * lightPulse), Color.Transparent),
                    startX = frontX,
                    endX = frontX + beamLength
                ),
                topLeft = Offset(frontX, roadY - busHeight * 0.8f),
                size = androidx.compose.ui.geometry.Size(beamLength, busHeight * 0.7f)
            )

            // Bus Body
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        busColor.copy(alpha = 0.45f),
                        busColor.copy(alpha = 0.25f)
                    ),
                    startY = roadY - busHeight,
                    endY = roadY
                ),
                topLeft = Offset(0f, roadY - busHeight),
                size = androidx.compose.ui.geometry.Size(busWidth, busHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
            )
            
            // Roof Accent Line
            drawLine(
                color = busColor.copy(alpha = 0.85f),
                start = Offset(2.dp.toPx(), roadY - busHeight + 1.dp.toPx()),
                end = Offset(busWidth - 2.dp.toPx(), roadY - busHeight + 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )

            // Bus Windows (3 side windows + front windshield)
            val windowY = roadY - busHeight + 3.dp.toPx()
            val windowH = busHeight * 0.44f
            val windowW = 12.dp.toPx()
            
            for (wIndex in 0..2) {
                val winX = 6.dp.toPx() + wIndex * (windowW + 4.dp.toPx())
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.4f),
                    topLeft = Offset(winX, windowY),
                    size = androidx.compose.ui.geometry.Size(windowW, windowH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
                )
            }
            
            // Front Windshield
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(busWidth - 16.dp.toPx(), windowY),
                size = androidx.compose.ui.geometry.Size(14.dp.toPx(), windowH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )

            // Wheels
            val wheelRadius = 2.5.dp.toPx()
            drawCircle(
                color = sysOnBackground.copy(alpha = 0.6f),
                radius = wheelRadius,
                center = Offset(14.dp.toPx(), roadY)
            )
            drawCircle(
                color = sysOnBackground.copy(alpha = 0.6f),
                radius = wheelRadius,
                center = Offset(busWidth - 14.dp.toPx(), roadY)
            )

            // Headlight LED
            drawCircle(
                color = Color(0xFFFFF9C4),
                radius = 1.5.dp.toPx(),
                center = Offset(busWidth - 1.dp.toPx(), roadY - busHeight * 0.35f)
            )

            // Rear Taillight
            drawCircle(
                color = Color(0xFFFF5252).copy(alpha = 0.85f),
                radius = 1.2.dp.toPx(),
                center = Offset(1.dp.toPx(), roadY - busHeight * 0.35f)
            )
        }
    }
}

@Composable
private fun BusSearchDialog(
    viewModel: TesseraViewModel,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("TODAS") }
    val busSearchResults by viewModel.busSearchResults.collectAsState()
    val isSearchingBus by viewModel.isSearchingBus.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .clip(RoundedCornerShape(20.dp))
                .background(themedCardBackground())
                .border(1.dp, themedCardBorder(), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Buscar Linha de Ônibus",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.searchBusLines(it)
                    },
                    placeholder = { Text("Número ou nome da linha...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = PrimaryTeal) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.searchBusLines("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = themedTextFieldColors()
                )

                // Filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("TODAS" to "Todas", "IDA" to "Ida", "VOLTA" to "Volta", "NOTURNAS" to "Noturnas").forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) PrimaryTeal.copy(alpha = 0.15f) else themedSubtleBackground())
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryTeal.copy(alpha = 0.4f) else themedSubtleBorder(),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedFilter = key }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (isSearchingBus) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryTeal, modifier = Modifier.size(28.dp))
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
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredResults.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (searchQuery.isEmpty()) "Digite o número da linha para buscar..." else "Nenhuma linha encontrada.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 12.sp
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
                                            onDismiss()
                                        }
                                        .padding(12.dp),
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
                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text(result.lt, color = lineColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = dest,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (result.sl == 1) "Sentido Ida" else "Sentido Volta",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Adicionar",
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
