package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.media.*
import com.example.ui.theme.PrimaryTeal
import com.example.viewmodel.TesseraViewModel
import java.net.URLEncoder

enum class MusicDashboardTab(val title: String) {
    LYRICS("Letras"),
    CREDITS("Ficha Técnica"),
    VIDEOS("Audiovisual")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicContextDashboardModal(
    viewModel: TesseraViewModel,
    onDismiss: () -> Unit
) {
    val activeMedia by viewModel.activeMediaState.collectAsState()
    val dossier by viewModel.musicDossier.collectAsState()
    val isLoading by viewModel.isLoadingMusicDossier.collectAsState()

    var selectedTab by remember { mutableStateOf(MusicDashboardTab.LYRICS) }
    val listState = rememberLazyListState()

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(activeMedia?.title, activeMedia?.artist) {
        if (activeMedia != null) {
            viewModel.fetchMusicDossier(forceRefresh = false)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(themedSubtleBorder())
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        val media = activeMedia
        if (media == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma mídia em reprodução no momento.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@ModalBottomSheet
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 4.dp,
                bottom = navBarBottom + 28.dp
            )
        ) {
            // 1. HEADER SUPERIOR
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(themedSubtleBackground())
                            .border(0.5.dp, themedSubtleBorder(), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", modifier = Modifier.size(16.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PrimaryTeal.copy(alpha = 0.12f))
                            .border(0.5.dp, PrimaryTeal.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${media.appDisplayName.uppercase()} • FLUXO AO VIVO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }

                    IconButton(
                        onClick = { viewModel.fetchMusicDossier(forceRefresh = true) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(themedSubtleBackground())
                            .border(0.5.dp, themedSubtleBorder(), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 2. HERO PLAYER COMPACTO
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(themedCardBackground())
                        .border(1.dp, themedCardBorder(), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Linha Superior: Capa + Metadados
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(themedSubtleBackground())
                                .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(12.dp)),
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
                                Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(24.dp))
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = media.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (media.album.isNotBlank()) "${media.artist} • ${media.album}" else media.artist,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Scrubber / Linha do tempo
                    val progress = if (media.durationMs > 0) {
                        (media.currentPositionMs.toFloat() / media.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = progress,
                            onValueChange = { newProgress ->
                                val targetPos = (newProgress * media.durationMs).toLong()
                                viewModel.seekMediaTo(targetPos)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryTeal,
                                activeTrackColor = PrimaryTeal,
                                inactiveTrackColor = themedSubtleBorder()
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(18.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatDuration(media.currentPositionMs), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatDuration(media.durationMs), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Controles de Playback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.skipMediaPrevious() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(20.dp))
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
                                if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (media.isPlaying) "Pausar" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { viewModel.skipMediaNext() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Próxima", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // 3. BARRA DE SEGMENTOS (3 ABAS)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(themedSubtleBackground())
                        .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MusicDashboardTab.values().forEach { tab ->
                        val isSelected = tab == selectedTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) themedCardBackground() else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) themedSubtleBorder() else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (dossier?.isPodcast == true && tab == MusicDashboardTab.LYRICS) "Capítulos" else tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. CONTEÚDO MODULAR DAS ABAS
            if (isLoading && dossier == null) {
                item {
                    MusicSkeletonLoading()
                }
            } else {
                val currentDossier = dossier
                when (selectedTab) {
                    MusicDashboardTab.LYRICS -> {
                        item {
                            LyricsAndFactsSection(
                                dossier = currentDossier,
                                currentPositionMs = media.currentPositionMs,
                                onSeek = { pos -> viewModel.seekMediaTo(pos) },
                                onRetry = { viewModel.fetchMusicDossier(forceRefresh = true) }
                            )
                        }
                    }
                    MusicDashboardTab.CREDITS -> {
                        item {
                            TechnicalCreditsSection(credits = currentDossier?.technicalCredits)
                        }
                    }
                    MusicDashboardTab.VIDEOS -> {
                        if (currentDossier?.relatedVideos.isNullOrEmpty()) {
                            item {
                                EmptyVideosState()
                            }
                        } else {
                            items(currentDossier!!.relatedVideos) { video ->
                                VideoActionCard(video = video, fallbackQuery = "${media.artist} ${media.title}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsAndFactsSection(
    dossier: MusicContextDossier?,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit
) {
    if (dossier == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Letra não encontrada para esta faixa.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onRetry) {
                    Text("Tentar novamente", color = PrimaryTeal, fontSize = 12.sp)
                }
            }
        }
        return
    }

    // EDGE CASE 1: PODCAST
    if (dossier.isPodcast) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "TÓPICOS E CAPÍTULOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = PrimaryTeal
            )

            if (dossier.podcastChapters.isEmpty()) {
                Text(
                    text = "Nenhum capítulo fornecido pelo autor deste episódio.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                dossier.podcastChapters.forEach { chapter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(themedCardBackground())
                            .border(1.dp, themedCardBorder(), RoundedCornerShape(12.dp))
                            .clickable { onSeek(chapter.timestampMs) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryTeal.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(chapter.timeFormatted, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(chapter.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            if (chapter.description != null) {
                                Text(chapter.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
        return
    }

    // EDGE CASE 2: INSTRUMENTAL
    if (dossier.isInstrumental) {
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
                Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(32.dp))
                Text(
                    text = "Faixa Instrumental",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Esta composição não possui vocais ou letra catalogada.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // LETRAS REAIS (SINCRONIZADAS OU COMPLETAS)
    val lyricsInfo = dossier.lyricsInfo
    val lines = lyricsInfo?.lines ?: emptyList()

    if (lines.isEmpty()) {
        val rawLyrics = lyricsInfo?.plainLyrics
        if (rawLyrics.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Letra não encontrada para esta faixa.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onRetry) {
                        Text("Tentar novamente", color = PrimaryTeal, fontSize = 12.sp)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(themedCardBackground())
                    .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = rawLyrics,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    } else {
        // Encontra a linha ativa se a letra for sincronizada
        val isSynced = lyricsInfo?.isSynced == true
        var activeLineIndex = -1
        if (isSynced) {
            for (i in lines.indices) {
                val t = lines[i].timestampMs
                if (t != null && t <= currentPositionMs) {
                    activeLineIndex = i
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(themedCardBackground())
                .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            lines.forEachIndexed { index, line ->
                val isActive = isSynced && index == activeLineIndex
                var isExpanded by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) PrimaryTeal.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable {
                            if (line.timestampMs != null) {
                                onSeek(line.timestampMs)
                            }
                            if (line.hasAnnotation) {
                                isExpanded = !isExpanded
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = line.text,
                            fontSize = if (isActive) 14.sp else 13.sp,
                            fontWeight = if (isActive || line.hasAnnotation) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) PrimaryTeal else if (line.hasAnnotation) PrimaryTeal else MaterialTheme.colorScheme.onBackground.copy(alpha = if (isSynced && !isActive) 0.6f else 0.9f),
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )

                        if (line.hasAnnotation) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Lightbulb, contentDescription = "Fato", tint = PrimaryTeal, modifier = Modifier.size(10.dp))
                            }
                        }
                    }

                    if (isExpanded && line.annotationText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(themedSubtleBackground())
                                .border(BorderStroke(0.5.dp, PrimaryTeal.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(12.dp))
                                Text(
                                    text = line.annotationText,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Fonte: Lrclib / Catálogo Editorial",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun TechnicalCreditsSection(credits: TrackTechnicalCredits?) {
    if (credits == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Ficha técnica indisponível.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "CRÉDITOS DE GRAVAÇÃO (MUSICBRAINZ)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = PrimaryTeal
        )

        CreditItem(label = "Compositores", value = credits.composers.joinToString(", "))
        CreditItem(label = "Produção Musical", value = credits.producers.joinToString(", "))
        if (credits.recordLabel != null) CreditItem(label = "Gravadora / Selo", value = credits.recordLabel)
        if (credits.studio != null) CreditItem(label = "Estúdio", value = credits.studio)
        if (credits.releaseDate != null) CreditItem(label = "Data de Lançamento", value = credits.releaseDate)
        if (credits.isrc != null) CreditItem(label = "ISRC", value = credits.isrc)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (credits.bpm != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(themedSubtleBackground())
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BPM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${credits.bpm}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                    }
                }
            }

            if (credits.key != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(themedSubtleBackground())
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(credits.key, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun VideoActionCard(video: TrackVideoMedia, fallbackQuery: String) {
    val context = LocalContext.current
    val query = video.youtubeQuery.ifBlank { fallbackQuery }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(12.dp))
            .clickable {
                try {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded"))
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryTeal.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.SmartDisplay, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PrimaryTeal.copy(alpha = 0.12f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(video.category.displayName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(video.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(video.channelTitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }

        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Abrir no YouTube", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun EmptyVideosState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Nenhum vídeo adicional disponível.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MusicSkeletonLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(color = PrimaryTeal, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text("Buscando letra e créditos oficiais...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(themedSubtleBorder())
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(themedSubtleBorder())
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(themedSubtleBorder())
        )
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
