package com.example.ui.components
import androidx.compose.material3.MaterialTheme

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CompleteTransactionsModal(
    transactions: List<Transaction>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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

    var pendingExportFormat by remember { mutableStateOf<String?>(null) }
    var pendingExportTransactions by remember { mutableStateOf<List<Transaction>?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (pendingExportFormat == "pdf") "application/pdf" else "text/csv"
        )
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                pendingExportTransactions?.let { txs ->
                    val success = if (pendingExportFormat == "pdf") {
                        generatePdf(context, it, txs, monthsList[pagerState.currentPage])
                    } else {
                        generateCsv(context, it, txs)
                    }
                    if (success) {
                        Toast.makeText(context, "Arquivo salvo com sucesso!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Erro ao salvar arquivo.", Toast.LENGTH_LONG).show()
                    }
                }
                pendingExportFormat = null
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
            color = Color(0xFF070909)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 16.dp, top = 40.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extrato Completo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }

                if (monthsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhuma transação encontrada.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        val currentMonthKey = monthsList[page]
                        val monthTransactions = groupedByMonth[currentMonthKey] ?: emptyList()
                        val formattedMonthName = monthFormat.format(Date(currentMonthKey)).replaceFirstChar { it.uppercase() }
                        
                        val dayFormat = SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR"))
                        val groupedByDay = monthTransactions.groupBy {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = it.timestamp
                            cal.set(Calendar.HOUR_OF_DAY, 0)
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            cal.timeInMillis
                        }.toSortedMap(reverseOrder())

                        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                            Text(
                                text = formattedMonthName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF71D7CD),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { 
                                        pendingExportFormat = "pdf"
                                        pendingExportTransactions = monthTransactions
                                        createDocumentLauncher.launch("extrato_${formattedMonthName.replace(" ", "_").lowercase()}.pdf")
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Exportar PDF", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        pendingExportFormat = "csv"
                                        pendingExportTransactions = monthTransactions
                                        createDocumentLauncher.launch("extrato_${formattedMonthName.replace(" ", "_").lowercase()}.csv")
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Icon(Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Exportar CSV", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                groupedByDay.forEach { (dayKey, dailyTxs) ->
                                    item {
                                        Text(
                                            text = dayFormat.format(Date(dayKey)),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                                        )
                                    }
                                    
                                    items(dailyTxs) { tx ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tx.title,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (tx.subtitle.isNotBlank() || tx.accountOrCardName.isNotBlank()) {
                                                    val subtitle = if (tx.subtitle.isNotBlank()) tx.subtitle else tx.accountOrCardName
                                                    Text(
                                                        text = subtitle,
                                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            Text(
                                                text = String.format(Locale("pt", "BR"), if (tx.isIncome) "+R$ %,.2f" else "-R$ %,.2f", tx.value),
                                                color = if (tx.isIncome) Color(0xFF81C784) else Color(0xFFEF5350),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (monthsList.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(monthsList.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) Color(0xFF71D7CD) else Color.White.copy(alpha = 0.2f)
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
        }
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
                canvas.drawText(if (tx.category.length > 15) tx.category.substring(0, 12) + "..." else tx.category, margin + 250f, yPosition, textPaint)
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
