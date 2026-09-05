package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BibliaBookItem
import com.example.data.BibliaVerseItem
import com.example.data.BibliaVersionItem
import com.example.data.BibleVerseVideo
import com.example.data.BibleMedal
import com.example.data.PerseveranceStats
import com.example.data.BibleVideoRecommendation
import com.example.data.BibleVideoRecommendationService
import com.example.tts.SherpaModelStatus
import android.net.Uri
import coil.compose.AsyncImage
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryGold
import com.example.ui.components.bounceClick
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.themedCardBackground
import com.example.ui.components.themedCardBorder
import com.example.ui.components.themedSubtleBackground
import com.example.ui.components.themedSubtleBorder
import com.example.viewmodel.TesseraViewModel

// Cores Pastéis YouVersion Modernas
val PastelYellow = Color(0xFFFEF08A)
val PastelGreen = Color(0xFFA7F3D0)
val PastelBlue = Color(0xFFBAE6FD)
val PastelOrange = Color(0xFFFED7AA)
val PastelPurple = Color(0xFFE9D5FF)

val PastelHighlightColors = listOf(
    PastelColor("Amarelo", "#FEF08A", PastelYellow),
    PastelColor("Verde", "#A7F3D0", PastelGreen),
    PastelColor("Azul", "#BAE6FD", PastelBlue),
    PastelColor("Pêssego", "#FED7AA", PastelOrange),
    PastelColor("Lavanda", "#E9D5FF", PastelPurple)
)

data class PastelColor(
    val name: String,
    val hex: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleScreen(
    viewModel: TesseraViewModel,
    onHomeClick: () -> Unit
) {
    val context = LocalContext.current
    val chapterState by viewModel.chapterUiState.collectAsStateWithLifecycle()
    val selectedBook by viewModel.selectedBibleBook.collectAsStateWithLifecycle()
    val selectedChapter by viewModel.selectedBibleChapter.collectAsStateWithLifecycle()
    val selectedVersion by viewModel.selectedBibleVersion.collectAsStateWithLifecycle()
    val versions by viewModel.bibleVersions.collectAsStateWithLifecycle()
    val books by viewModel.bibleBooks.collectAsStateWithLifecycle()
    val highlights by viewModel.verseHighlights.collectAsStateWithLifecycle()
    val targetScrollVerse by viewModel.targetScrollVerse.collectAsStateWithLifecycle()

    // Novos estados: Vídeos, Perseverança, Medalhas e Áudio
    val currentChapterVideos by viewModel.currentChapterVideos.collectAsStateWithLifecycle()
    val perseveranceStats by viewModel.perseveranceStats.collectAsStateWithLifecycle()
    val bibleMedals by viewModel.bibleMedals.collectAsStateWithLifecycle()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
    val activeTtsVerse by viewModel.activeTtsVerse.collectAsStateWithLifecycle()
    val sherpaModelStatus by viewModel.sherpaModelStatus.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    var showBookPicker by remember { mutableStateOf(false) }
    var showVersionPicker by remember { mutableStateOf(false) }
    var showPerseveranceModal by remember { mutableStateOf(false) }
    var viewingVideosVerse by remember { mutableStateOf<BibliaVerseItem?>(null) }
    val selectedVerses = remember { mutableStateListOf<BibliaVerseItem>() }

    LaunchedEffect(Unit) {
        viewModel.loadBibleMetadata()
        viewModel.loadCurrentChapter()
    }

    LaunchedEffect(chapterState, targetScrollVerse) {
        if (chapterState is TesseraViewModel.ChapterUiState.Success && targetScrollVerse != null) {
            val verses = (chapterState as TesseraViewModel.ChapterUiState.Success).chapterData.verses
            val target = targetScrollVerse
            if (target != null && verses.isNotEmpty()) {
                val targetIndex = verses.indexOfFirst { it.number == target }
                if (targetIndex >= 0) {
                    kotlinx.coroutines.delay(120)
                    listState.animateScrollToItem(targetIndex + 1)
                }
            }
        }
    }

    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBarTotalHeight = navBarBottomInset + 84.dp
    val listBottomPadding = navBarTotalHeight + 80.dp

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar com proteção de status bar / notch / câmera
                BibleTopBar(
                    bookName = selectedBook.name,
                    chapter = selectedChapter,
                    versionCode = selectedVersion,
                    perseveranceStats = perseveranceStats,
                    isAudioPlaying = isAudioPlaying,
                    sherpaModelStatus = sherpaModelStatus,
                    onBackClick = onHomeClick,
                    onBookClick = { showBookPicker = true },
                    onVersionClick = { showVersionPicker = true },
                    onPerseveranceClick = { showPerseveranceModal = true },
                    onAudioClick = {
                        val verses = (chapterState as? TesseraViewModel.ChapterUiState.Success)?.chapterData?.verses ?: emptyList()
                        viewModel.togglePlayAudio(verses)
                    }
                )

                // Banner de status e progresso do download do modelo neural Sherpa-ONNX (21 MB)
                AnimatedVisibility(
                    visible = sherpaModelStatus is SherpaModelStatus.Downloading,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    val percent = (sherpaModelStatus as? SherpaModelStatus.Downloading)?.progressPercent ?: 0
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryTeal.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { percent / 100f },
                                    modifier = Modifier.size(16.dp),
                                    color = PrimaryTeal,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Baixando voz neural pt-BR • $percent%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "21 MB",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Chapter Reading Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (val state = chapterState) {
                        is TesseraViewModel.ChapterUiState.Loading -> {
                            BibleSkeletonLoading()
                        }
                        is TesseraViewModel.ChapterUiState.Error -> {
                            BibleErrorState(
                                message = state.message,
                                onRetry = { viewModel.loadCurrentChapter() }
                            )
                        }
                        is TesseraViewModel.ChapterUiState.Success -> {
                            val verses = state.chapterData.verses
                            if (verses.isEmpty()) {
                                BibleEmptyState()
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 20.dp,
                                        end = 20.dp,
                                        top = 8.dp,
                                        bottom = listBottomPadding
                                    )
                                ) {
                                    // Header Editorial Estilo YouVersion
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp, bottom = 28.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = selectedBook.name,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$selectedChapter",
                                                fontSize = 54.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                lineHeight = 58.sp
                                            )
                                        }
                                    }

                                    items(verses, key = { it.number }) { verse ->
                                        val isSelected = selectedVerses.any { it.number == verse.number }
                                        val isTargetVerse = targetScrollVerse == verse.number
                                        val highlightKey = "${selectedBook.abbrev}_${selectedChapter}_${verse.number}"
                                        val highlightColorHex = highlights[highlightKey]
                                        val hasVideos = currentChapterVideos.any { it.verseNumber == verse.number }
                                        val isTtsActive = activeTtsVerse == verse.number

                                        BibleVerseRow(
                                            verse = verse,
                                            isSelected = isSelected,
                                            isTargetVerse = isTargetVerse,
                                            isTtsActive = isTtsActive,
                                            highlightColorHex = highlightColorHex,
                                            hasVideos = hasVideos,
                                            onClick = {
                                                viewModel.clearTargetScrollVerse()
                                                if (isSelected) {
                                                    selectedVerses.removeAll { it.number == verse.number }
                                                } else {
                                                    selectedVerses.add(verse)
                                                }
                                            },
                                            onVideoClick = {
                                                viewingVideosVerse = verse
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Bottom Reader Bar (Estilo YouVersion) - visível quando não houver versículos selecionados
            AnimatedVisibility(
                visible = selectedVerses.isEmpty(),
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(180)) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150)) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBarTotalHeight + 6.dp)
            ) {
                val verses = (chapterState as? TesseraViewModel.ChapterUiState.Success)?.chapterData?.verses ?: emptyList()
                BibleBottomReaderBar(
                    bookName = selectedBook.name,
                    chapter = selectedChapter,
                    canGoBack = selectedChapter > 1,
                    isAudioPlaying = isAudioPlaying,
                    sherpaModelStatus = sherpaModelStatus,
                    onPreviousChapter = {
                        selectedVerses.clear()
                        viewModel.previousChapter()
                    },
                    onNextChapter = {
                        selectedVerses.clear()
                        viewModel.nextChapter()
                    },
                    onBookPickerClick = { showBookPicker = true },
                    onPlayAudioClick = {
                        viewModel.togglePlayAudio(verses)
                    }
                )
            }

            // Floating Bottom Action Bar for Selected Verses
            AnimatedVisibility(
                visible = selectedVerses.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(200)) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150)) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = navBarTotalHeight + 8.dp)
                    .padding(horizontal = 16.dp)
            ) {
                BibleSelectionFloatingBar(
                    selectedCount = selectedVerses.size,
                    onColorSelect = { hex ->
                        selectedVerses.forEach { v ->
                            viewModel.setVerseHighlight(selectedBook.abbrev, selectedChapter, v.number, hex)
                        }
                        selectedVerses.clear()
                    },
                    onClearHighlight = {
                        selectedVerses.forEach { v ->
                            viewModel.removeVerseHighlight(selectedBook.abbrev, selectedChapter, v.number)
                        }
                        selectedVerses.clear()
                    },
                    onConnectVideo = {
                        if (selectedVerses.isNotEmpty()) {
                            viewingVideosVerse = selectedVerses.first()
                            selectedVerses.clear()
                        }
                    },
                    onCopy = {
                        val sorted = selectedVerses.sortedBy { it.number }
                        val text = formatVerseQuote(selectedBook.name, selectedChapter, sorted, selectedVersion)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Versículo Bíblico", text))
                        Toast.makeText(context, "Copiado com sucesso!", Toast.LENGTH_SHORT).show()
                        selectedVerses.clear()
                    },
                    onShare = {
                        val sorted = selectedVerses.sortedBy { it.number }
                        val text = formatVerseQuote(selectedBook.name, selectedChapter, sorted, selectedVersion)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartilhar Versículo"))
                        selectedVerses.clear()
                    },
                    onClose = { selectedVerses.clear() }
                )
            }
        }
    }

    // Modal: Book & Chapter Picker
    if (showBookPicker) {
        BibleBookChapterPickerModal(
            books = books,
            currentBook = selectedBook,
            currentChapter = selectedChapter,
            onSelect = { book, chapter ->
                viewModel.selectBookAndChapter(book, chapter)
                showBookPicker = false
            },
            onDismiss = { showBookPicker = false }
        )
    }

    // Modal: Version Picker
    if (showVersionPicker) {
        BibleVersionPickerModal(
            versions = versions,
            currentVersionCode = selectedVersion,
            onSelect = { code ->
                viewModel.selectBibleVersion(code)
                showVersionPicker = false
            },
            onDismiss = { showVersionPicker = false }
        )
    }

    // Modal: Estudos e Vídeos Recomendados para o Versículo
    viewingVideosVerse?.let { verse ->
        val savedVideosForVerse = currentChapterVideos.filter { it.verseNumber == verse.number }
        val recommendations = viewModel.getRecommendedVideosForVerse(
            bookAbbrev = selectedBook.abbrev,
            bookName = selectedBook.name,
            chapter = selectedChapter,
            verseNumber = verse.number,
            verseText = verse.text
        )
        RecommendedVerseVideosBottomSheet(
            bookAbbrev = selectedBook.abbrev,
            bookName = selectedBook.name,
            chapter = selectedChapter,
            verse = verse,
            savedVideos = savedVideosForVerse,
            recommendations = recommendations,
            onSaveRecommendation = { rec ->
                viewModel.saveRecommendedVideo(
                    bookAbbrev = selectedBook.abbrev,
                    bookName = selectedBook.name,
                    chapter = selectedChapter,
                    verseNumber = verse.number,
                    recommendation = rec
                )
            },
            onDeleteSavedVideo = { video ->
                viewModel.deleteVerseVideo(video)
            },
            onDismiss = { viewingVideosVerse = null }
        )
    }

    // Modal: Perseverança e Medalhas
    if (showPerseveranceModal) {
        PerseveranceMedalsModal(
            stats = perseveranceStats,
            medals = bibleMedals,
            onDismiss = { showPerseveranceModal = false }
        )
    }
}

// ==============================================================================
// TOP BAR
// ==============================================================================
// ==============================================================================
// TOP BAR (YouVersion Minimalist Header)
// ==============================================================================
@Composable
private fun BibleTopBar(
    bookName: String,
    chapter: Int,
    versionCode: String,
    perseveranceStats: PerseveranceStats,
    isAudioPlaying: Boolean,
    sherpaModelStatus: SherpaModelStatus,
    onBackClick: () -> Unit,
    onBookClick: () -> Unit,
    onVersionClick: () -> Unit,
    onPerseveranceClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botão de Áudio / Leitura TTS Sherpa-ONNX
            IconButton(
                onClick = onAudioClick,
                modifier = Modifier.size(38.dp)
            ) {
                if (sherpaModelStatus is SherpaModelStatus.Downloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = PrimaryTeal,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                        contentDescription = if (isAudioPlaying) "Pausar leitura em áudio" else "Ouvir capítulo",
                        tint = if (isAudioPlaying) PrimaryTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Pill de Perseverança & Medalhas
            Surface(
                onClick = onPerseveranceClick,
                shape = RoundedCornerShape(20.dp),
                color = themedSubtleBackground(),
                border = BorderStroke(1.dp, themedCardBorder())
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "🔥", fontSize = 13.sp)
                    Text(
                        text = "${perseveranceStats.currentStreak} d",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (perseveranceStats.currentStreak > 0) Color(0xFFF97316) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Pill de Versão Bíblica
            Surface(
                onClick = onVersionClick,
                shape = RoundedCornerShape(20.dp),
                color = themedSubtleBackground(),
                border = BorderStroke(1.dp, themedCardBorder())
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = versionCode.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ==============================================================================
// VERSE ROW (Estilo YouVersion Editorial com Indicador de Vídeo)
// ==============================================================================
@Composable
private fun BibleVerseRow(
    verse: BibliaVerseItem,
    isSelected: Boolean,
    isTargetVerse: Boolean = false,
    isTtsActive: Boolean = false,
    highlightColorHex: String?,
    hasVideos: Boolean = false,
    onClick: () -> Unit,
    onVideoClick: () -> Unit
) {
    val highlightColor = remember(highlightColorHex) {
        if (highlightColorHex != null) {
            try {
                val parsed = android.graphics.Color.parseColor(highlightColorHex)
                Color(parsed).copy(alpha = 0.28f)
            } catch (e: Exception) {
                Color.Transparent
            }
        } else {
            Color.Transparent
        }
    }

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        isTtsActive -> PrimaryTeal.copy(alpha = 0.22f)
        isTargetVerse -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        highlightColorHex != null -> highlightColor
        else -> Color.Transparent
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        isTtsActive -> PrimaryTeal.copy(alpha = 0.5f)
        isTargetVerse -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected || isTargetVerse || isTtsActive) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTtsActive) PrimaryTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    ) {
                        append("${verse.number}  ")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        append(verse.text.trim())
                    }
                },
                lineHeight = 27.sp,
                modifier = Modifier.weight(1f)
            )

            if (hasVideos) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onVideoClick)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SmartDisplay,
                        contentDescription = "Vídeo conectado",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

// ==============================================================================
// BOTTOM READER BAR (Barra Flutuante YouVersion: Play + < Livro X >)
// ==============================================================================
@Composable
private fun BibleBottomReaderBar(
    bookName: String,
    chapter: Int,
    canGoBack: Boolean,
    isAudioPlaying: Boolean,
    sherpaModelStatus: SherpaModelStatus,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onBookPickerClick: () -> Unit,
    onPlayAudioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botão circular de Play / Pause
        Surface(
            onClick = onPlayAudioClick,
            shape = CircleShape,
            color = themedCardBackground(),
            border = BorderStroke(1.dp, themedCardBorder()),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (sherpaModelStatus is SherpaModelStatus.Downloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PrimaryTeal,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isAudioPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isAudioPlaying) "Pausar Leitura" else "Ouvir Capítulo",
                        tint = if (isAudioPlaying) PrimaryTeal else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Pill central de navegação rápida: < Livro X >
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = themedCardBackground(),
            border = BorderStroke(1.dp, themedCardBorder()),
            modifier = Modifier.height(46.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                IconButton(
                    onClick = onPreviousChapter,
                    enabled = canGoBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Capítulo Anterior",
                        tint = if (canGoBack) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onBookPickerClick)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$bookName $chapter",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = onNextChapter,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "Próximo Capítulo",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==============================================================================
// FLOATING CONTEXTUAL BAR (YouVersion Style com Ação de Vídeo)
// ==============================================================================
@Composable
private fun BibleSelectionFloatingBar(
    selectedCount: Int,
    onColorSelect: (String) -> Unit,
    onClearHighlight: () -> Unit,
    onConnectVideo: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(PremiumGlassModifier)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header: Selected count & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount ${if (selectedCount == 1) "versículo selecionado" else "versículos selecionados"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onConnectVideo, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Outlined.SmartDisplay, contentDescription = "Conectar Vídeo do YouTube", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Outlined.Share, contentDescription = "Compartilhar", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Pastel Color Chips + Clear Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PastelHighlightColors.forEach { item ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(item.color)
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .clickable { onColorSelect(item.hex) }
                    )
                }

                // Clear Highlight Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(themedSubtleBackground())
                        .border(1.dp, themedCardBorder(), CircleShape)
                        .clickable { onClearHighlight() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FormatColorReset,
                        contentDescription = "Limpar destaque",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==============================================================================
// MODAL: ESTUDOS E VÍDEOS RECOMENDADOS AUTOMATICAMENTE (YOUTUBE)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendedVerseVideosBottomSheet(
    bookAbbrev: String,
    bookName: String,
    chapter: Int,
    verse: BibliaVerseItem,
    savedVideos: List<BibleVerseVideo>,
    recommendations: List<BibleVideoRecommendation>,
    onSaveRecommendation: (BibleVideoRecommendation) -> Unit,
    onDeleteSavedVideo: (BibleVerseVideo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Cabeçalho Editorial
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.SmartDisplay,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Estudos em Vídeo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$bookName $chapter:${verse.number}",
                        fontSize = 13.sp,
                        color = PrimaryTeal,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Atalho de 1 toque: Pesquisa Inteligente no YouTube
                Surface(
                    onClick = {
                        val searchUrl = BibleVideoRecommendationService.buildSearchUrlForVerse(bookName, chapter, verse.number)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                        context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryTeal.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(14.dp))
                        Text("Buscar no YouTube", fontSize = 11.sp, color = PrimaryTeal, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Versículo em Citação Suave
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(themedSubtleBackground())
                    .border(1.dp, themedSubtleBorder(), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "“${verse.text.trim()}”",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Seção: Estudos Salvos pelo Usuário (se houver)
                if (savedVideos.isNotEmpty()) {
                    item {
                        Text(
                            text = "MEUS ESTUDOS SALVOS (${savedVideos.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryTeal,
                            letterSpacing = 1.sp
                        )
                    }

                    items(savedVideos, key = { "saved_${it.id}" }) { saved ->
                        SavedVideoCard(
                            video = saved,
                            onWatch = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(saved.youtubeUrl))
                                context.startActivity(intent)
                            },
                            onDelete = { onDeleteSavedVideo(saved) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Seção: Recomendações Automáticas do App
                item {
                    Text(
                        text = "ESTUDOS RECOMENDADOS AUTOMATICAMENTE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                }

                if (recommendations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma recomendação adicional para este versículo.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    items(recommendations, key = { it.id }) { rec ->
                        val isAlreadySaved = savedVideos.any { it.videoId == rec.videoId || it.title == rec.title }
                        RecommendedVideoCard(
                            recommendation = rec,
                            isSaved = isAlreadySaved,
                            onWatch = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rec.youtubeUrl))
                                context.startActivity(intent)
                            },
                            onToggleSave = {
                                if (!isAlreadySaved) {
                                    onSaveRecommendation(rec)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedVideoCard(
    recommendation: BibleVideoRecommendation,
    isSaved: Boolean,
    onWatch: () -> Unit,
    onToggleSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail com Play
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable(onClick = onWatch),
                contentAlignment = Alignment.Center
            ) {
                if (recommendation.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = recommendation.thumbnailUrl,
                        contentDescription = recommendation.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Reproduzir",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(3.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Badge de Categoria
                Text(
                    text = recommendation.categoryBadge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryTeal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = recommendation.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (recommendation.channelName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = recommendation.channelName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Botão de Favoritar / Salvar
            IconButton(
                onClick = onToggleSave,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isSaved) "Estudo Salvo" else "Salvar Estudo",
                    tint = if (isSaved) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun SavedVideoCard(
    video: BibleVerseVideo,
    onWatch: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(themedCardBackground())
            .border(1.dp, PrimaryTeal.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 62.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable(onClick = onWatch),
                contentAlignment = Alignment.Center
            ) {
                if (video.videoId.isNotBlank()) {
                    AsyncImage(
                        model = "https://img.youtube.com/vi/${video.videoId}/hqdefault.jpg",
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Reproduzir",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(3.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (video.channelName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = video.channelName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Remover dos salvos",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ==============================================================================
// MODAL: PERSEVERANÇA E MEDALHAS BÍBLICAS
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerseveranceMedalsModal(
    stats: PerseveranceStats,
    medals: List<BibleMedal>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Perseverança na Palavra",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sua jornada diária de leitura e reflexão",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Card Hero de Perseverança
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(themedCardBackground())
                        .border(1.dp, themedCardBorder(), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SEQUÊNCIA ATUAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF97316),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${stats.currentStreak}",
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 40.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (stats.currentStreak == 1) "dia" else "dias seguidos",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF97316).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🔥", fontSize = 26.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Maior Sequência", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text("${stats.longestStreak} dias", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column {
                                Text("Dias Lidos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text("${stats.totalDaysRead} dias", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column {
                                Text("Capítulos Lidos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Text("${stats.totalChaptersRead}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PrimaryTeal)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (stats.readToday) PrimaryTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (stats.readToday) "✨ Leitura de hoje registrada!" else "📖 Leia um capítulo hoje para manter o fogo aceso.",
                                    fontSize = 12.sp,
                                    color = if (stats.readToday) PrimaryTeal else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Título de Medalhas
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MEDALHAS & CONQUISTAS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    letterSpacing = 1.2.sp
                )
            }

            // Lista de Medalhas
            items(medals, key = { it.id }) { medal ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (medal.isUnlocked) PrimaryTeal.copy(alpha = 0.08f)
                            else themedCardBackground()
                        )
                        .border(
                            1.dp,
                            if (medal.isUnlocked) PrimaryTeal.copy(alpha = 0.35f) else themedCardBorder(),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (medal.isUnlocked) PrimaryTeal.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = medal.iconEmoji,
                                fontSize = 22.sp,
                                color = if (medal.isUnlocked) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = medal.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (medal.isUnlocked) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Conquistada",
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = medal.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                            if (!medal.isUnlocked && medal.targetProgress > 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { medal.currentProgress.toFloat() / medal.targetProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = PrimaryTeal,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// BOOK & CHAPTER PICKER MODAL
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BibleBookChapterPickerModal(
    books: List<BibliaBookItem>,
    currentBook: BibliaBookItem,
    currentChapter: Int,
    onSelect: (BibliaBookItem, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(if (currentBook.testament == "NT") 1 else 0) }
    var selectedBookForChapters by remember { mutableStateOf<BibliaBookItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredBooks = remember(books, selectedTab, searchQuery) {
        val testamentFilter = if (selectedTab == 0) "VT" else "NT"
        books.filter { b ->
            b.testament.equals(testamentFilter, ignoreCase = true) &&
                    (searchQuery.isBlank() || b.name.contains(searchQuery, ignoreCase = true) || b.abbrev.contains(searchQuery, ignoreCase = true))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            if (selectedBookForChapters == null) {
                // Step 1: Select Book
                Text(
                    text = "Selecionar Livro",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar livro...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Testament Tabs (VT / NT)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(themedSubtleBackground())
                        .padding(3.dp)
                ) {
                    listOf("Antigo Testamento", "Novo Testamento").forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    if (filteredBooks.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhum livro encontrado.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(filteredBooks, key = { it.id }) { b ->
                            val isCurrent = b.id == currentBook.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedBookForChapters = b }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = b.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = b.abbrev.uppercase(),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // Step 2: Select Chapter for Selected Book
                val book = selectedBookForChapters!!
                val maxCap = getEstimatedChapterCount(book.abbrev)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { selectedBookForChapters = null }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                        Text(
                            text = book.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Selecione o capítulo",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chapter Number Grid
                val chapters = (1..maxCap).toList()
                val chunked = chapters.chunked(5)

                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(chunked) { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { cap ->
                                val isCurrent = book.id == currentBook.id && cap == currentChapter
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) MaterialTheme.colorScheme.primary else themedSubtleBackground())
                                        .border(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else themedCardBorder(), RoundedCornerShape(10.dp))
                                        .clickable { onSelect(book, cap) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cap.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            // Filler boxes for incomplete rows
                            for (i in 0 until (5 - row.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// VERSION PICKER MODAL
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BibleVersionPickerModal(
    versions: List<BibliaVersionItem>,
    currentVersionCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Versão da Bíblia",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                items(versions, key = { it.code }) { ver ->
                    val isSelected = ver.code.equals(currentVersionCode, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onSelect(ver.code) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ver.code.uppercase(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (!ver.copyright.isNullOrBlank()) {
                                Text(
                                    text = ver.copyright,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// SKELETON LOADING & ERROR STATES
// ==============================================================================
@Composable
private fun BibleSkeletonLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(8) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (it % 2 == 0) 0.95f else 0.85f)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(themedSubtleBackground())
            )
        }
    }
}

@Composable
private fun BibleErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Não foi possível carregar o capítulo",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Tentar Novamente", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun BibleEmptyState() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("Nenhum versículo encontrado neste capítulo.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ==============================================================================
// HELPERS
// ==============================================================================
private fun formatVerseQuote(
    bookName: String,
    chapter: Int,
    verses: List<BibliaVerseItem>,
    version: String
): String {
    val versesText = verses.joinToString(" ") { "(${it.number}) ${it.text.trim()}" }
    val verseNumbers = if (verses.size == 1) "${verses.first().number}" else "${verses.first().number}-${verses.last().number}"
    return "«$versesText»\n— $bookName $chapter:$verseNumbers ($version)"
}

private fun getEstimatedChapterCount(abbrev: String): Int {
    return when (abbrev.lowercase()) {
        "gn" -> 50; "ex" -> 40; "lv" -> 27; "nm" -> 36; "dt" -> 34
        "js" -> 24; "jz" -> 21; "rt" -> 4; "1sm" -> 31; "2sm" -> 24
        "1rs" -> 22; "2rs" -> 25; "1cr" -> 29; "2cr" -> 36; "ed" -> 10
        "ne" -> 13; "et" -> 10; "job" -> 42; "sl" -> 150; "pv" -> 31
        "ec" -> 12; "ct" -> 8; "is" -> 66; "jr" -> 52; "lm" -> 5
        "ez" -> 48; "dn" -> 12; "os" -> 14; "jl" -> 3; "am" -> 9
        "ob" -> 1; "jn" -> 4; "mq" -> 7; "na" -> 3; "hc" -> 3
        "sf" -> 3; "ag" -> 2; "zc" -> 14; "ml" -> 4
        "mt" -> 28; "mc" -> 16; "lc" -> 24; "jo" -> 21; "atos" -> 28
        "rm" -> 16; "1co" -> 16; "2co" -> 13; "gl" -> 6; "ef" -> 6
        "fp" -> 4; "cl" -> 4; "1ts" -> 5; "2ts" -> 3; "1tm" -> 6
        "2tm" -> 4; "tt" -> 3; "fm" -> 1; "hb" -> 13; "tg" -> 5
        "1pe" -> 5; "2pe" -> 3; "1jo" -> 5; "2jo" -> 1; "3jo" -> 1
        "jd" -> 1; "ap" -> 22
        else -> 50
    }
}
