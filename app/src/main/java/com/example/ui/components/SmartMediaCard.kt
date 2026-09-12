package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.PrimaryTeal
import com.example.viewmodel.TesseraViewModel
import kotlinx.coroutines.delay

private fun formatMediaDuration(timeMs: Long): String {
    if (timeMs <= 0) return "00:00"
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
}

@Composable
private fun MiniAudioEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "miniAudioEq")
    val bar1Height by transition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by transition.animateFloat(
        initialValue = 11f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by transition.animateFloat(
        initialValue = 5f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val b1 = if (isPlaying) bar1Height.dp else 4.dp
        val b2 = if (isPlaying) bar2Height.dp else 6.dp
        val b3 = if (isPlaying) bar3Height.dp else 3.dp

        Box(modifier = Modifier.width(2.dp).height(b1).clip(CircleShape).background(PrimaryTeal))
        Box(modifier = Modifier.width(2.dp).height(b2).clip(CircleShape).background(PrimaryTeal))
        Box(modifier = Modifier.width(2.dp).height(b3).clip(CircleShape).background(PrimaryTeal))
    }
}

@Composable
fun SmartMediaCard(
    viewModel: TesseraViewModel,
    onOpenDeepDive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeMedia by viewModel.activeMediaState.collectAsState()
    var isPermissionGranted by remember { mutableStateOf(viewModel.isMediaListenerPermissionGranted(context)) }

    // Revalida a permissão ao recompor
    LaunchedEffect(Unit) {
        isPermissionGranted = viewModel.isMediaListenerPermissionGranted(context)
    }

    // 1. Estado de Desconexão / Permissão Pendente
    if (!isPermissionGranted) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(themedCardBackground())
                .border(1.dp, themedCardBorder(), RoundedCornerShape(20.dp))
                .clickable { viewModel.openMediaListenerSettings(context) }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryTeal.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Conectar Player Global",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Toque para habilitar leitura, controle e letras do Spotify/Tidal.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        lineHeight = 15.sp
                    )
                }

                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = "Conectar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        return
    }

    // 2. Estado Vazio / Sem Música Ativa
    if (activeMedia == null || activeMedia!!.title.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(themedCardBackground())
                .border(1.dp, themedCardBorder(), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Player em Espera",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Inicie uma música para sincronizar letras e controles no Tessera.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
        return
    }

    // 3. Estado Ativo de Reprodução
    val media = activeMedia!!

    var livePositionMs by remember(media.currentPositionMs, media.isPlaying, media.lastPositionUpdateTime) {
        mutableLongStateOf(media.getCalculatedPositionMs())
    }

    LaunchedEffect(media.isPlaying, media.currentPositionMs, media.lastPositionUpdateTime, media.durationMs) {
        if (media.isPlaying) {
            while (true) {
                livePositionMs = media.getCalculatedPositionMs()
                delay(250L)
            }
        } else {
            livePositionMs = media.currentPositionMs
        }
    }

    val isPlaying = media.isPlaying
    val progress = if (media.durationMs > 0) {
        (livePositionMs.toFloat() / media.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Linha Superior: Capa + Metadados + Botão Dossiê/Letras
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Área Clicável para Abrir o Dossiê/Letras
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenDeepDive)
                ) {
                    // Artwork / Thumbnail
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(themedSubtleBackground())
                            .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (media.artworkBitmap != null) {
                            Image(
                                bitmap = media.artworkBitmap.asImageBitmap(),
                                contentDescription = media.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (media.artworkUri != null) {
                            AsyncImage(
                                model = media.artworkUri,
                                contentDescription = media.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Outlined.MusicNote,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Metadados
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Badge da Plataforma e Mini Equalizador
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryTeal.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = media.appDisplayName.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = PrimaryTeal
                                )
                            }

                            if (isPlaying) {
                                MiniAudioEqualizer(isPlaying = true)
                            }
                        }

                        Text(
                            text = media.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (media.album.isNotBlank()) "${media.artist} • ${media.album}" else media.artist,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Acesso Rápido ao Painel de Letras / Ficha Técnica
                IconButton(
                    onClick = onOpenDeepDive,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Icon(
                        Icons.Outlined.GraphicEq,
                        contentDescription = "Letras e Dossiê",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Barra de Progresso com Minutagem Dinâmica
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(PrimaryTeal)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMediaDuration(livePositionMs),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                    Text(
                        text = formatMediaDuration(media.durationMs),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }

            // Controles Táteis Minimalistas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.skipMediaPrevious() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = { viewModel.toggleMediaPlayPause() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PrimaryTeal)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                        tint = Color(0xFF090A0F),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = { viewModel.skipMediaNext() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Próxima",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
