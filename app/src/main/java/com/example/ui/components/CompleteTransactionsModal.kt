package com.example.ui.components

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Transaction
import com.example.utils.CategoryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CompleteTransactionsModal(
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
    onTransactionClick: (Transaction) -> Unit = {},
    onBulkUpdateCategory: (List<Transaction>, String) -> Unit = { _, _ -> },
    onBulkDelete: (List<Transaction>) -> Unit = {},
    onAutoCategorize: suspend (List<Transaction>) -> Int = { 0 },
    initialFilterUnclassified: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allCategories = remember(context) { CategoryUtils.getAllCategories(context) }

    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
    
    val groupedByMonth = remember(transactions) {
        transactions.groupBy { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.toSortedMap(reverseOrder())
    }

    val monthsList = groupedByMonth.keys.toList()
    val pagerState = rememberPagerState(pageCount = { if (monthsList.isEmpty()) 1 else monthsList.size })

    // Estado do modo de lote e filtros
    var isBatchMode by remember { mutableStateOf(initialFilterUnclassified) }
    var selectedFilterCategory by remember { mutableStateOf<String?>(if (initialFilterUnclassified) "Sem Categoria" else null) }
    val selectedTransactionIds = remember { mutableStateListOf<Int>() }

    var showBulkCategoryDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isAutoCategorizing by remember { mutableStateOf(false) }

    var pendingExportTransactions by remember { mutableStateOf<List<Transaction>?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                pendingExportTransactions?.let { txs ->
                    val success = generatePdf(context, it, txs, monthsList.getOrNull(pagerState.currentPage) ?: System.currentTimeMillis())
                    if (success) Toast.makeText(context, "PDF salvo com sucesso!", Toast.LENGTH_LONG).show()
                    else Toast.makeText(context, "Erro ao salvar PDF.", Toast.LENGTH_LONG).show()
                }
                pendingExportTransactions = null
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                pendingExportTransactions?.let { txs ->
                    val success = generateCsv(context, it, txs)
                    if (success) Toast.makeText(context, "CSV salvo com sucesso!", Toast.LENGTH_LONG).show()
                    else Toast.makeText(context, "Erro ao salvar CSV.", Toast.LENGTH_LONG).show()
                }
                pendingExportTransactions = null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Superior
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 16.dp, top = 40.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Extrato Completo",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (isBatchMode) {
                                Text(
                                    text = "${selectedTransactionIds.size} selecionados",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Botão Auto-Categorizar
                            IconButton(
                                onClick = {
                                    if (isAutoCategorizing) return@IconButton
                                    isAutoCategorizing = true
                                    coroutineScope.launch {
                                        val count = onAutoCategorize(transactions)
                                        isAutoCategorizing = false
                                        if (count > 0) {
                                            Toast.makeText(
                                                context,
                                                "$count lançamentos auto-categorizados!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Nenhuma pendência identificada para auto-categorização.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (isAutoCategorizing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = "Auto-Categorizar",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Botão Modo Lote / Selecionar
                            TextButton(
                                onClick = {
                                    isBatchMode = !isBatchMode
                                    if (!isBatchMode) {
                                        selectedTransactionIds.clear()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isBatchMode) "Concluir" else "Selecionar",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBatchMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Fechar
                            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    if (monthsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "Nenhuma transação encontrada.",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f)
                        ) { page ->
                            val currentMonthKey = monthsList[page]
                            val monthTransactions = groupedByMonth[currentMonthKey] ?: emptyList()
                            val formattedMonthName = monthFormat.format(Date(currentMonthKey)).replaceFirstChar { it.uppercase() }

                            // Quantidade de transações sem categoria no mês
                            val unclassifiedCount = remember(monthTransactions) {
                                monthTransactions.count {
                                    it.category.isBlank() || it.category.equals("Outros", ignoreCase = true) || it.category.equals("Sem Categoria", ignoreCase = true)
                                }
                            }

                            // Transações filtradas
                            val filteredMonthTransactions = remember(monthTransactions, selectedFilterCategory) {
                                when (selectedFilterCategory) {
                                    null -> monthTransactions
                                    "Sem Categoria" -> monthTransactions.filter {
                                        it.category.isBlank() || it.category.equals("Outros", ignoreCase = true) || it.category.equals("Sem Categoria", ignoreCase = true)
                                    }
                                    else -> monthTransactions.filter { it.category.equals(selectedFilterCategory, ignoreCase = true) }
                                }
                            }

                            val dayFormat = SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR"))
                            val groupedByDay = remember(filteredMonthTransactions) {
                                filteredMonthTransactions.groupBy {
                                    val cal = Calendar.getInstance()
                                    cal.timeInMillis = it.timestamp
                                    cal.set(Calendar.HOUR_OF_DAY, 0)
                                    cal.set(Calendar.MINUTE, 0)
                                    cal.set(Calendar.SECOND, 0)
                                    cal.set(Calendar.MILLISECOND, 0)
                                    cal.timeInMillis
                                }.toSortedMap(reverseOrder())
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp)
                            ) {
                                // Título do mês
                                Text(
                                    text = formattedMonthName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF71D7CD),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                // Barra de Filtros Rápidos (Chips)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                ) {
                                    item {
                                        FilterChip(
                                            selected = selectedFilterCategory == null,
                                            onClick = { selectedFilterCategory = null },
                                            label = { Text("Todas (${monthTransactions.size})", fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                selectedLabelColor = MaterialTheme.colorScheme.primary
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = selectedFilterCategory == null,
                                                borderColor = themedCardBorder(),
                                                selectedBorderColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }

                                    if (unclassifiedCount > 0) {
                                        item {
                                            FilterChip(
                                                selected = selectedFilterCategory == "Sem Categoria",
                                                onClick = {
                                                    selectedFilterCategory = if (selectedFilterCategory == "Sem Categoria") null else "Sem Categoria"
                                                },
                                                label = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFFF59E0B))
                                                        )
                                                        Text("Sem Categoria ($unclassifiedCount)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                    selectedLabelColor = Color(0xFFF59E0B)
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = selectedFilterCategory == "Sem Categoria",
                                                    borderColor = Color(0xFFF59E0B).copy(alpha = 0.5f),
                                                    selectedBorderColor = Color(0xFFF59E0B)
                                                )
                                            )
                                        }
                                    }

                                    // Outras categorias presentes no mês
                                    val distinctCategories = monthTransactions
                                        .map { it.category }
                                        .filter { it.isNotBlank() && !it.equals("Outros", ignoreCase = true) && !it.equals("Sem Categoria", ignoreCase = true) }
                                        .distinct()
                                        .sorted()

                                    items(distinctCategories) { catName ->
                                        val countInMonth = monthTransactions.count { it.category == catName }
                                        FilterChip(
                                            selected = selectedFilterCategory == catName,
                                            onClick = {
                                                selectedFilterCategory = if (selectedFilterCategory == catName) null else catName
                                            },
                                            label = { Text("$catName ($countInMonth)", fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                selectedLabelColor = MaterialTheme.colorScheme.primary
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = selectedFilterCategory == catName,
                                                borderColor = themedCardBorder(),
                                                selectedBorderColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                }

                                // Barra de Exportação e Controle de Seleção
                                if (!isBatchMode) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { 
                                                pendingExportTransactions = filteredMonthTransactions
                                                pdfLauncher.launch("extrato_${formattedMonthName.replace(" ", "_").lowercase()}.pdf")
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, themedSubtleBorder()),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Exportar PDF",
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 11.sp
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                pendingExportTransactions = filteredMonthTransactions
                                                csvLauncher.launch("extrato_${formattedMonthName.replace(" ", "_").lowercase()}.csv")
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, themedSubtleBorder()),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.TableView,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Exportar CSV",
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                } else {
                                    // Toolbar do Modo Lote
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${filteredMonthTransactions.size} LANÇAMENTOS LISTADOS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 1.sp
                                        )

                                        val allCurrentSelected = filteredMonthTransactions.isNotEmpty() &&
                                                filteredMonthTransactions.all { selectedTransactionIds.contains(it.id) }

                                        TextButton(
                                            onClick = {
                                                if (allCurrentSelected) {
                                                    filteredMonthTransactions.forEach { selectedTransactionIds.remove(it.id) }
                                                } else {
                                                    filteredMonthTransactions.forEach {
                                                        if (!selectedTransactionIds.contains(it.id)) {
                                                            selectedTransactionIds.add(it.id)
                                                        }
                                                    }
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (allCurrentSelected) "Desmarcar Todos" else "Marcar Todos",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Lista de Lançamentos
                                if (filteredMonthTransactions.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (selectedFilterCategory != null) "Nenhum lançamento nesta categoria." else "Nenhuma transação neste mês.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(bottom = if (isBatchMode && selectedTransactionIds.isNotEmpty()) 100.dp else 40.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        groupedByDay.forEach { (dayKey, dailyTxs) ->
                                            item {
                                                Text(
                                                    text = dayFormat.format(Date(dayKey)),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                    modifier = Modifier.padding(bottom = 2.dp, top = 6.dp)
                                                )
                                            }

                                            items(dailyTxs, key = { it.id }) { tx ->
                                                val isSelected = selectedTransactionIds.contains(tx.id)
                                                val isUnclassified = tx.category.isBlank() || tx.category.equals("Outros", ignoreCase = true) || tx.category.equals("Sem Categoria", ignoreCase = true)

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else themedSubtleBackground())
                                                        .border(
                                                            width = if (isSelected) 1.dp else 0.5.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else themedCardBorder(),
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable {
                                                            if (isBatchMode) {
                                                                if (isSelected) {
                                                                    selectedTransactionIds.remove(tx.id)
                                                                } else {
                                                                    selectedTransactionIds.add(tx.id)
                                                                }
                                                            } else {
                                                                onTransactionClick(tx)
                                                            }
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        if (isBatchMode) {
                                                            Checkbox(
                                                                checked = isSelected,
                                                                onCheckedChange = { checked ->
                                                                    if (checked) selectedTransactionIds.add(tx.id)
                                                                    else selectedTransactionIds.remove(tx.id)
                                                                },
                                                                colors = CheckboxDefaults.colors(
                                                                    checkedColor = MaterialTheme.colorScheme.primary,
                                                                    checkmarkColor = Color.Black
                                                                ),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(32.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = CategoryUtils.getCategoryIcon(tx.category),
                                                                    contentDescription = null,
                                                                    tint = if (isUnclassified) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }

                                                        Column {
                                                            Text(
                                                                text = tx.title,
                                                                color = MaterialTheme.colorScheme.onBackground,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                if (isUnclassified) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(4.dp),
                                                                        color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                                    ) {
                                                                        Text(
                                                                            text = "Sem categoria",
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color(0xFFF59E0B),
                                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                } else {
                                                                    Text(
                                                                        text = tx.category,
                                                                        fontSize = 10.sp,
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                }

                                                                if (tx.accountOrCardName.isNotBlank()) {
                                                                    Text(
                                                                        text = "• ${tx.accountOrCardName}",
                                                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                                        fontSize = 10.sp,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Text(
                                                        text = String.format(
                                                            Locale("pt", "BR"),
                                                            if (tx.isIncome) "+R$ %,.2f" else "-R$ %,.2f",
                                                            tx.value
                                                        ),
                                                        color = if (tx.isIncome) Color(0xFF81C784) else Color(0xFFEF5350),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Paginação de Meses
                        if (monthsList.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(monthsList.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) Color(0xFF71D7CD) else themedSubtleBorder()
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(if (pagerState.currentPage == iteration) 8.dp else 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Barra Flutuante Inferior de Ações em Lote (Categorizar / Excluir)
                AnimatedVisibility(
                    visible = isBatchMode && selectedTransactionIds.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    val selectedTxs = transactions.filter { selectedTransactionIds.contains(it.id) }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedTxs.size} selecionados",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Excluir em Lote
                                OutlinedButton(
                                    onClick = { showBulkDeleteConfirmDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Excluir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                // Categorizar em Lote
                                Button(
                                    onClick = { showBulkCategoryDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Outlined.Category, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Categorizar", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Seleção de Categoria em Lote
    if (showBulkCategoryDialog) {
        val selectedTxs = transactions.filter { selectedTransactionIds.contains(it.id) }
        CategorySelectionDialog(
            title = "Categorizar em Lote",
            subtitle = "Aplicar para ${selectedTxs.size} lançamentos selecionados",
            currentCategory = "",
            categories = allCategories,
            onSelectCategory = { newCat ->
                onBulkUpdateCategory(selectedTxs, newCat)
                selectedTransactionIds.clear()
                showBulkCategoryDialog = false
                Toast.makeText(context, "${selectedTxs.size} lançamentos atualizados para $newCat!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showBulkCategoryDialog = false }
        )
    }

    // Diálogo de Confirmação de Exclusão em Lote
    if (showBulkDeleteConfirmDialog) {
        val selectedTxs = transactions.filter { selectedTransactionIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Excluir ${selectedTxs.size} lançamentos?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Os lançamentos selecionados serão removidos permanentemente e os saldos das respectivas contas e faturas serão estornados.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBulkDelete(selectedTxs)
                        selectedTransactionIds.clear()
                        showBulkDeleteConfirmDialog = false
                        Toast.makeText(context, "${selectedTxs.size} lançamentos excluídos!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirmDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        )
    }
}

private suspend fun generateCsv(context: Context, uri: Uri, transactions: List<Transaction>): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val writer = outputStream.writer()
                writer.write("Data,Titulo,Subtitulo,Categoria,Conta/Cartao,Valor,Tipo\n")
                
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
                
                transactions.sortedBy { it.timestamp }.forEach { tx ->
                    val date = dateFormat.format(Date(tx.timestamp))
                    val title = tx.title.replace(",", " ")
                    val subtitle = tx.subtitle.replace(",", " ")
                    val category = tx.category.replace(",", " ")
                    val account = tx.accountOrCardName.replace(",", " ")
                    val value = String.format(Locale("pt", "BR"), "%.2f", tx.value)
                    val type = if (tx.isIncome) "Receita" else "Despesa"
                    
                    writer.write("$date,$title,$subtitle,$category,$account,$value,$type\n")
                }
                writer.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

private suspend fun generatePdf(context: Context, uri: Uri, transactions: List<Transaction>, monthKey: Long): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            
            val titlePaint = Paint().apply {
                textSize = 18f
                isFakeBoldText = true
            }
            val headerPaint = Paint().apply {
                textSize = 12f
                isFakeBoldText = true
            }
            val textPaint = Paint().apply {
                textSize = 10f
            }
            
            val monthName = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")).format(Date(monthKey)).replaceFirstChar { it.uppercase() }
            
            var yPosition = 50f
            val margin = 50f
            
            canvas.drawText("Extrato Tessera - $monthName", margin, yPosition, titlePaint)
            yPosition += 40f
            
            canvas.drawText("Data", margin, yPosition, headerPaint)
            canvas.drawText("Título", margin + 80f, yPosition, headerPaint)
            canvas.drawText("Categoria", margin + 250f, yPosition, headerPaint)
            canvas.drawText("Conta/Cartão", margin + 350f, yPosition, headerPaint)
            canvas.drawText("Valor", margin + 450f, yPosition, headerPaint)
            
            yPosition += 20f
            canvas.drawLine(margin, yPosition, 595f - margin, yPosition, textPaint)
            yPosition += 20f
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            
            for (tx in transactions.sortedBy { it.timestamp }) {
                if (yPosition > 800f) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }
                
                val dateStr = dateFormat.format(Date(tx.timestamp))
                val valStr = String.format(Locale("pt", "BR"), if (tx.isIncome) "+R$ %,.2f" else "-R$ %,.2f", tx.value)
                
                canvas.drawText(dateStr, margin, yPosition, textPaint)
                canvas.drawText(if (tx.title.length > 25) tx.title.substring(0, 22) + "..." else tx.title, margin + 80f, yPosition, textPaint)
                canvas.drawText(if (tx.category.length > 20) tx.category.substring(0, 18) + "..." else tx.category, margin + 250f, yPosition, textPaint)
                canvas.drawText(if (tx.accountOrCardName.length > 15) tx.accountOrCardName.substring(0, 12) + "..." else tx.accountOrCardName, margin + 350f, yPosition, textPaint)
                canvas.drawText(valStr, margin + 450f, yPosition, textPaint)
                
                yPosition += 20f
            }
            
            document.finishPage(page)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
