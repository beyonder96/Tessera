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
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.themedCardBackground
import com.example.ui.components.themedCardBorder
import com.example.ui.components.themedSubtleBackground
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

    var showBookPicker by remember { mutableStateOf(false) }
    var showVersionPicker by remember { mutableStateOf(false) }
    val selectedVerses = remember { mutableStateListOf<BibliaVerseItem>() }

    LaunchedEffect(Unit) {
        viewModel.loadBibleMetadata()
        viewModel.loadCurrentChapter()
    }

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
                // Top App Bar
                BibleTopBar(
                    bookName = selectedBook.name,
                    chapter = selectedChapter,
                    versionCode = selectedVersion,
                    onBackClick = onHomeClick,
                    onBookClick = { showBookPicker = true },
                    onVersionClick = { showVersionPicker = true }
                )

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
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                                ) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${selectedBook.name} $selectedChapter",
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = selectedVersion.uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }

                                    items(verses, key = { it.number }) { verse ->
                                        val isSelected = selectedVerses.any { it.number == verse.number }
                                        val highlightKey = "${selectedBook.abbrev}_${selectedChapter}_${verse.number}"
                                        val highlightColorHex = highlights[highlightKey]

                                        BibleVerseRow(
                                            verse = verse,
                                            isSelected = isSelected,
                                            highlightColorHex = highlightColorHex,
                                            onClick = {
                                                if (isSelected) {
                                                    selectedVerses.removeAll { it.number == verse.number }
                                                } else {
                                                    selectedVerses.add(verse)
                                                }
                                            }
                                        )
                                    }

                                    // Chapter Footer Navigation (Prev / Next)
                                    item {
                                        Spacer(modifier = Modifier.height(32.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = if (selectedVerses.isNotEmpty()) 100.dp else 40.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Previous Chapter
                                            OutlinedButton(
                                                onClick = {
                                                    selectedVerses.clear()
                                                    viewModel.previousChapter()
                                                },
                                                enabled = selectedChapter > 1,
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, themedCardBorder()),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.onBackground
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "Cap. Anterior", fontSize = 13.sp)
                                            }

                                            // Next Chapter
                                            Button(
                                                onClick = {
                                                    selectedVerses.clear()
                                                    viewModel.nextChapter()
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text(text = "Próximo Cap.", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color.Black
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

            // Floating Bottom Action Bar for Selected Verses (YouVersion Style)
            AnimatedVisibility(
                visible = selectedVerses.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(200)) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150)) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
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
}

// ==============================================================================
// TOP BAR
// ==============================================================================
@Composable
private fun BibleTopBar(
    bookName: String,
    chapter: Int,
    versionCode: String,
    onBackClick: () -> Unit,
    onBookClick: () -> Unit,
    onVersionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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

        // Center Pill: Book & Chapter Trigger
        Surface(
            onClick = onBookClick,
            shape = RoundedCornerShape(20.dp),
            color = themedSubtleBackground(),
            border = BorderStroke(1.dp, themedCardBorder())
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$bookName $chapter",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Right Pill: Version Trigger
        Surface(
            onClick = onVersionClick,
            shape = RoundedCornerShape(20.dp),
            color = themedSubtleBackground(),
            border = BorderStroke(1.dp, themedCardBorder())
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = versionCode.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ==============================================================================
// VERSE ROW (With Pastel Highlight Overlay)
// ==============================================================================
@Composable
private fun BibleVerseRow(
    verse: BibliaVerseItem,
    isSelected: Boolean,
    highlightColorHex: String?,
    onClick: () -> Unit
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
        highlightColorHex != null -> highlightColor
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
            lineHeight = 24.sp
        )
    }
}

// ==============================================================================
// FLOATING CONTEXTUAL BAR (YouVersion Style)
// ==============================================================================
@Composable
private fun BibleSelectionFloatingBar(
    selectedCount: Int,
    onColorSelect: (String) -> Unit,
    onClearHighlight: () -> Unit,
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
