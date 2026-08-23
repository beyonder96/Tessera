package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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

    if (!isPermissionGranted) {
        // Convite Minimalista Contextual
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(themedSubtleBackground())
                .border(1.dp, themedSubtleBorder(), RoundedCornerShape(16.dp))
                .clickable { viewModel.openMediaListenerSettings(context) }
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryTeal.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conectar Player Global",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Toque para habilitar letras, créditos e controle do Spotify/Tidal.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        return
    }

    // Card Reativo com animação de expansão/colapso suave
    AnimatedVisibility(
        visible = activeMedia != null && activeMedia!!.title.isNotBlank(),
        enter = fadeIn(tween(180)) + expandVertically(tween(180)),
        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
    ) {
        val media = activeMedia ?: return@AnimatedVisibility

        var livePositionMs by remember(media.currentPositionMs, media.isPlaying, media.lastPositionUpdateTime) {
            mutableLongStateOf(media.getCalculatedPositionMs())
        }

        LaunchedEffect(media.isPlaying, media.currentPositionMs, media.lastPositionUpdateTime, media.durationMs) {
            if (media.isPlaying) {
                while (true) {
                    livePositionMs = media.getCalculatedPositionMs()
                    kotlinx.coroutines.delay(250L)
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
                .clip(RoundedCornerShape(16.dp))
                .background(themedCardBackground())
                .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Área Clicável para Abrir o Dashboard (Capa + Textos)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenDeepDive)
                            .padding(vertical = 2.dp)
                    ) {
                        // Artwork / Thumbnail
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(themedSubtleBackground())
                                .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(10.dp)),
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
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Metadados
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = media.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                // Badge da Plataforma
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PrimaryTeal.copy(alpha = 0.12f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = media.appDisplayName.uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryTeal
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (media.album.isNotBlank()) "${media.artist} • ${media.album}" else media.artist,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Controles Táteis Isolados
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.skipMediaPrevious() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleMediaPlayPause() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.skipMediaNext() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Próxima",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Barra Linear de Progresso (2px)
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(themedSubtleBorder())
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(PrimaryTeal)
                        )
                    }
                }
            }
        }
    }
}
