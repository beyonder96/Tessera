package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.*
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
import com.example.data.BankAccount
import com.example.data.CreditCard
import com.example.utils.CategoryItem
import com.example.utils.CategoryUtils
import com.example.utils.ParsedStatementTransaction
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBankStatementBottomSheet(
    initialTransactions: List<ParsedStatementTransaction>,
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    onConfirmImport: (String, List<ParsedStatementTransaction>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allCategories = remember(context) { CategoryUtils.getAllCategories(context) }

    val transactions = remember {
        mutableStateListOf<ParsedStatementTransaction>().apply { addAll(initialTransactions) }
    }

    val allAccountNames = remember(bankAccounts, creditCards) {
        val list = mutableListOf<String>()
        bankAccounts.forEach { list.add(it.name) }
        creditCards.forEach { list.add(it.name) }
        if (list.isEmpty()) list.add("Conta Principal")
        list
    }

    var selectedAccount by remember { mutableStateOf(allAccountNames.firstOrNull() ?: "Conta Principal") }
    var showAccountDropdown by remember { mutableStateOf(false) }

    // Estado para seleção de categoria de uma transação específica ou em lote
    var transactionForCategoryChange by remember { mutableStateOf<ParsedStatementTransaction?>(null) }
    var isBulkCategoryChange by remember { mutableStateOf(false) }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    val selectedCount = transactions.count { it.isSelected }
    val totalIncome = remember(transactions) {
        transactions.filter { it.isSelected && it.isIncome }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.isSelected && !it.isIncome }.sumOf { it.amount }
    }

    // Agrupamento das despesas selecionadas por categoria para feedback de orçamento
    val categoryTotals = remember(transactions) {
        transactions.filter { it.isSelected && !it.isIncome }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Importar Extrato",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${transactions.size} lançamentos identificados",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seletor da Conta de Destino
            Text(
                text = "CONTA DE DESTINO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { showAccountDropdown = true },
                    shape = RoundedCornerShape(12.dp),
                    color = themedSubtleBackground(),
                    border = BorderStroke(1.dp, themedCardBorder()),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedAccount,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = showAccountDropdown,
                    onDismissRequest = { showAccountDropdown = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    allAccountNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name, fontWeight = if (name == selectedAccount) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                selectedAccount = name
                                showAccountDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card Resumo Financeiro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(themedSubtleBackground())
                    .border(0.5.dp, themedCardBorder(), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ENTRADAS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    Text(text = currencyFormatter.format(totalIncome), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(themedCardBorder()))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "SAÍDAS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(text = currencyFormatter.format(totalExpense), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }

            // Preview rápido de categorias
            if (categoryTotals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoryTotals) { (catName, amount) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = themedSubtleBackground(),
                            border = BorderStroke(0.5.dp, themedCardBorder())
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = CategoryUtils.getCategoryIcon(catName),
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$catName: ${currencyFormatter.format(amount)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Toolbar da Lista (Selecionar Todos / Desmarcar / Categorizar em Lote)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LANÇAMENTOS ($selectedCount)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (selectedCount > 0) {
                        TextButton(
                            onClick = { isBulkCategoryChange = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Category,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Categorizar ($selectedCount)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            val allSelected = transactions.all { it.isSelected }
                            transactions.indices.forEach { i ->
                                transactions[i] = transactions[i].copy(isSelected = !allSelected)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (transactions.all { it.isSelected }) "Desmarcar Todos" else "Marcar Todos",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Lista de Lançamentos
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum lançamento encontrado no extrato.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { item ->
                        val index = transactions.indexOfFirst { it.id == item.id }
                        ParsedTransactionRow(
                            item = item,
                            onToggleSelected = {
                                if (index != -1) {
                                    transactions[index] = transactions[index].copy(isSelected = !item.isSelected)
                                }
                            },
                            onToggleIncome = {
                                if (index != -1) {
                                    transactions[index] = transactions[index].copy(isIncome = !item.isIncome)
                                }
                            },
                            onChangeCategoryClick = {
                                transactionForCategoryChange = item
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão CTA de Confirmação
            Button(
                onClick = {
                    onConfirmImport(selectedAccount, transactions.filter { it.isSelected })
                },
                enabled = selectedCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Confirmar Importação ($selectedCount Lançamentos)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }

    // Modal para Seleção Individual de Categoria
    if (transactionForCategoryChange != null) {
        val currentTx = transactionForCategoryChange!!
        CategorySelectionDialog(
            title = "Alterar Categoria",
            subtitle = currentTx.title,
            currentCategory = currentTx.category,
            categories = allCategories,
            onSelectCategory = { newCategory ->
                val index = transactions.indexOfFirst { it.id == currentTx.id }
                if (index != -1) {
                    transactions[index] = transactions[index].copy(category = newCategory)
                }
                transactionForCategoryChange = null
            },
            onDismiss = { transactionForCategoryChange = null }
        )
    }

    // Modal para Seleção em Lote de Categoria
    if (isBulkCategoryChange) {
        CategorySelectionDialog(
            title = "Categorizar em Lote",
            subtitle = "Aplicar para $selectedCount lançamentos selecionados",
            currentCategory = "",
            categories = allCategories,
            onSelectCategory = { newCategory ->
                transactions.indices.forEach { i ->
                    if (transactions[i].isSelected) {
                        transactions[i] = transactions[i].copy(category = newCategory)
                    }
                }
                isBulkCategoryChange = false
            },
            onDismiss = { isBulkCategoryChange = false }
        )
    }
}

@Composable
private fun ParsedTransactionRow(
    item: ParsedStatementTransaction,
    onToggleSelected: () -> Unit,
    onToggleIncome: () -> Unit,
    onChangeCategoryClick: () -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val categoryIcon = CategoryUtils.getCategoryIcon(item.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (item.isSelected) themedSubtleBackground() else Color.Transparent)
            .border(
                width = 0.5.dp,
                color = if (item.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else themedCardBorder(),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onToggleSelected)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Checkbox
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (item.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(1.5.dp, if (item.isSelected) MaterialTheme.colorScheme.primary else themedCardBorder(), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (item.isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.dateFormatted,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    // Chip Clicável de Categoria
                    Surface(
                        onClick = onChangeCategoryClick,
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "${if (item.isIncome) "+" else "-"} ${currencyFormatter.format(item.amount)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.error
            )
            Surface(
                onClick = onToggleIncome,
                shape = RoundedCornerShape(4.dp),
                color = if (item.isIncome) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                border = BorderStroke(0.5.dp, if (item.isIncome) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Text(
                    text = if (item.isIncome) "Entrada ⇄" else "Saída ⇄",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isIncome) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun CategorySelectionDialog(
    title: String,
    subtitle: String,
    currentCategory: String,
    categories: List<CategoryItem>,
    onSelectCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCategories = remember(searchQuery, categories) {
        if (searchQuery.isBlank()) categories
        else categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar categoria...", fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = themedCardBorder()
                    )
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCategories) { cat ->
                        val isSelected = cat.name.equals(currentCategory, ignoreCase = true)
                        Surface(
                            onClick = { onSelectCategory(cat.name) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                            border = BorderStroke(
                                width = if (isSelected) 1.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else themedCardBorder()
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = cat.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    )
}
