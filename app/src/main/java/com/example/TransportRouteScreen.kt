package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RouteSession
import com.example.data.TimelineNode
import com.example.viewmodel.TesseraViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportRouteScreen(
    viewModel: TesseraViewModel,
    routeSession: RouteSession,
    userLat: Double,
    userLng: Double,
    onBack: () -> Unit
) {
    val themeColor = remember(routeSession.corTema) {
        try {
            Color(android.graphics.Color.parseColor(routeSession.corTema))
        } catch (e: Exception) {
            Color(0xFF4FC3F7)
        }
    }

    val glassBorder = Color.White.copy(alpha = 0.08f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070909))
    ) {
        if (routeSession.tipoModal == "ONIBUS") {
            // MAPA DE ÔNIBUS (50% superior) E INFO (50% inferior)
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f)
                ) {
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(userLat, userLng), 14.5f)
                    }

                    // Polling / vehicle changes will trigger position resets or camera moves
                    LaunchedEffect(routeSession.mapaVeiculos) {
                        routeSession.mapaVeiculos?.firstOrNull()?.let { veic ->
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLng(LatLng((userLat + veic.latitude) / 2, (userLng + veic.longitude) / 2))
                            )
                        }
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = false), // Handled by manual marker
                        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
                    ) {
                        // User position Marker
                        Marker(
                            state = MarkerState(position = LatLng(userLat, userLng)),
                            title = "Você está aqui",
                            snippet = "Sua localização"
                        )

                        // Bus vehicle Marker
                        routeSession.mapaVeiculos?.forEach { veic ->
                            Marker(
                                state = MarkerState(position = LatLng(veic.latitude, veic.longitude)),
                                title = "Ônibus ${routeSession.linhaCodigo}",
                                snippet = "Prefixo: ${veic.prefixo}"
                            )
                        }

                        // Boarding point marker (150m from user simulated)
                        Marker(
                            state = MarkerState(position = LatLng(userLat + 0.001, userLng + 0.001)),
                            title = "Ponto de Embarque",
                            snippet = "Linha: ${routeSession.linhaCodigo}"
                        )
                    }

                    // Floating Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .border(1.dp, glassBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                }

                // BOTTOM SHEET FIXO (Foco em dados da linha e ETA)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(Color(0xFF0F1212))
                        .border(1.dp, glassBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Linha e Destino Final
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(themeColor, CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = routeSession.linhaCodigo,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = routeSession.destinoFinal,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Sentido Único",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Close Route Session Button
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                    .border(1.dp, glassBorder, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                            }
                        }

                        // Ações Premium (End, Save, Share)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ActionButton(label = "Encerrar", icon = Icons.Default.Stop, containerColor = Color(0xFFE57373).copy(alpha = 0.15f), textColor = Color(0xFFE57373), borderColor = Color(0xFFE57373).copy(alpha = 0.3f), onClick = onBack)
                            ActionButton(label = "Salvar", icon = Icons.Default.BookmarkBorder, containerColor = Color.White.copy(alpha = 0.05f), textColor = Color.White, borderColor = glassBorder, onClick = {})
                            ActionButton(label = "Enviar", icon = Icons.Outlined.Share, containerColor = Color.White.copy(alpha = 0.05f), textColor = Color.White, borderColor = glassBorder, onClick = {})
                        }

                        // ETA e mini timeline
                        Text(
                            text = "PRÓXIMAS PARADAS",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(routeSession.passosTimeline ?: emptyList()) { idx, step ->
                                MiniTimelineRow(step = step, isLast = idx == (routeSession.passosTimeline?.size ?: 0) - 1, themeColor = themeColor)
                            }
                        }
                    }
                }
            }
        } else {
            // TELA CHEIA TIMELINE VERTICAL (METRÔ/TREM)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header Fixo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .border(1.dp, glassBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = routeSession.destinoFinal,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Roteiro Ativo",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .border(1.dp, glassBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                }

                // Ações Premium (End, Save, Share)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    ActionButton(label = "Encerrar", icon = Icons.Default.Stop, containerColor = Color(0xFFE57373).copy(alpha = 0.15f), textColor = Color(0xFFE57373), borderColor = Color(0xFFE57373).copy(alpha = 0.3f), onClick = onBack)
                    ActionButton(label = "Salvar", icon = Icons.Default.BookmarkBorder, containerColor = Color.White.copy(alpha = 0.05f), textColor = Color.White, borderColor = glassBorder, onClick = {})
                    ActionButton(label = "Enviar", icon = Icons.Outlined.Share, containerColor = Color.White.copy(alpha = 0.05f), textColor = Color.White, borderColor = glassBorder, onClick = {})
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline contínua vertical
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    itemsIndexed(routeSession.passosTimeline ?: emptyList()) { idx, step ->
                        TimelineVerticalRow(
                            step = step,
                            isLast = idx == (routeSession.passosTimeline?.size ?: 0) - 1,
                            themeColor = themeColor,
                            lineBadge = routeSession.linhaCodigo
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    textColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MiniTimelineRow(
    step: TimelineNode,
    isLast: Boolean,
    themeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .border(2.dp, if (step.status == "atual") themeColor else Color.White.copy(alpha = 0.4f), CircleShape)
                    .background(if (step.status == "atual") Color.White else Color.Transparent, CircleShape)
            )

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(step.nome, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                step.mensagem?.let { msg ->
                    Text(msg, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                }
            }
            Text(step.horarioPrevisto, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

@Composable
fun TimelineVerticalRow(
    step: TimelineNode,
    isLast: Boolean,
    themeColor: Color,
    lineBadge: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Glowing pulsing node for current station
            if (step.status == "atual") {
                val infiniteTransition = rememberInfiniteTransition(label = "glowPulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 12.dp.value,
                    targetValue = 24.dp.value,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(scale.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .border(2.dp, Color.White, CircleShape)
                            .background(themeColor, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (step.status == "passou") Color.White.copy(alpha = 0.3f) else Color.White, CircleShape)
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(60.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (step.status == "passou") {
                                    listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.3f))
                                } else {
                                    listOf(themeColor, themeColor)
                                }
                            )
                        )
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.nome,
                    color = if (step.status == "passou") Color.White.copy(alpha = 0.4f) else Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (step.status == "atual") FontWeight.Bold else FontWeight.Medium
                )
                step.mensagem?.let { msg ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = msg,
                        color = if (step.status == "atual") Color(0xFF4FC3F7) else Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }

                // Connections/Transfer details
                step.baldeacaoLinhasecores?.let { transfers ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        transfers.forEach { transfer ->
                            val parts = transfer.split("|")
                            val name = parts.getOrNull(0) ?: ""
                            val colorStr = parts.getOrNull(1) ?: "#808080"
                            val c = try {
                                Color(android.graphics.Color.parseColor(colorStr))
                            } catch (e: Exception) {
                                Color.Gray
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(c.copy(alpha = 0.15f))
                                    .border(1.dp, c.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = name,
                                    color = c,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = step.horarioPrevisto,
                color = if (step.status == "passou") Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
        }
    }
}
