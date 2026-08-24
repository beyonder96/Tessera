package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BankAccount
import com.example.data.BenefitCard
import com.example.data.CreditCard
import com.example.data.Transaction
import com.example.utils.CategoryUtils
import com.example.utils.toDoubleCleanOrZero
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    benefitCards: List<BenefitCard>,
    editingTransaction: Transaction?,
    defaultIsIncome: Boolean = false,
    defaultIsTransfer: Boolean = false,
    onDismiss: () -> Unit,
    onAdd: (String, Double, Boolean, String, String, Boolean, Boolean, String, Long, Boolean, Int) -> Unit,
    onUpdate: (Transaction, Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onTransfer: (from: String, to: String, value: Double, date: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Type: 0 for Despesa, 1 for Receita, 2 for Transferência
    var transactionType by remember { 
        mutableStateOf(if (defaultIsTransfer) 2 else if (defaultIsIncome) 1 else 0) 
    }

    LaunchedEffect(editingTransaction) {
        if (editingTransaction != null) {
            transactionType = if (editingTransaction.isIncome) 1 else 0
        }
    }

    val isIncome = transactionType == 1
    val isTransfer = transactionType == 2

    var title by remember { mutableStateOf(editingTransaction?.title ?: "") }
    var valueStr by remember { mutableStateOf(editingTransaction?.value?.toString() ?: "") }
    var category by remember { mutableStateOf(editingTransaction?.category ?: "Alimentação") }
    
    var isRealized by remember { mutableStateOf(editingTransaction?.isRealized ?: true) }
    var isRecurrent by remember { mutableStateOf(editingTransaction?.isRecurrent ?: false) }
    var isInstallment by remember { mutableStateOf(false) }
    var installmentsCountStr by remember { mutableStateOf("2") }
    var recurrenceInterval by remember { mutableStateOf(editingTransaction?.recurrenceInterval ?: "Mensal") }
    var dueDate by remember { mutableStateOf(editingTransaction?.dueDate ?: System.currentTimeMillis()) }
    
    // Origin for Expense/Income
    val origins = remember(bankAccounts, creditCards, benefitCards) {
        bankAccounts.map { it.name } + creditCards.map { it.name } + benefitCards.map { it.name }
    }
    var selectedOrigin by remember { 
        mutableStateOf(editingTransaction?.accountOrCardName ?: origins.firstOrNull() ?: "") 
    }

    // Origins and Destinations for Transfer
    var fromAccount by remember { mutableStateOf(bankAccounts.firstOrNull()?.name) }
    var toAccount by remember { mutableStateOf(bankAccounts.getOrNull(1)?.name) }
    
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE) }
    var customCategories by remember {
        val catsSet = sharedPrefs.getStringSet("custom_categories", emptySet()) ?: emptySet()
        val parsed = catsSet.map {
            val parts = it.split("|")
            if (parts.size == 2) {
                val icon = when (parts[1]) {
                    "Home" -> Icons.Outlined.Home
                    "Pets" -> Icons.Outlined.Pets
                    "DirectionsCar" -> Icons.Outlined.DirectionsCar
                    "ShoppingBag" -> Icons.Outlined.ShoppingBag
                    "Flight" -> Icons.Outlined.Flight
                    "School" -> Icons.Outlined.School
                    "FitnessCenter" -> Icons.Outlined.FitnessCenter
                    "LocalDining" -> Icons.Outlined.LocalDining
                    else -> Icons.Outlined.Label
                }
                parts[0] to icon
            } else {
                it to Icons.Outlined.Label
            }
        }.sortedBy { it.first }
        mutableStateOf(parsed)
    }
    
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    
    val allCategories: List<Pair<String, ImageVector>> = remember(context, customCategories) {
        CategoryUtils.getAllCategories(context).map { it.name to it.icon }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themedOverlayBackground(),
        dragHandle = { BottomSheetDefaults.DragHandle(color = themedDivider()) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction != null) "Editar Lançamento" else "Nova Transação",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }

            // Despesa/Receita/Transferência Toggle Switch
            if (editingTransaction == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(themedSubtleBackground())
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (transactionType == 0) Color(0xFFEF4444).copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (transactionType == 0) Color(0xFFEF4444) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { transactionType = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Despesa", color = if (transactionType == 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (transactionType == 1) Color(0xFF71D7CD).copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (transactionType == 1) Color(0xFF71D7CD) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { transactionType = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Receita", color = if (transactionType == 1) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (transactionType == 2) Color(0xFF4A90E2).copy(alpha = 0.2f) else Color.Transparent)
                            .border(1.dp, if (transactionType == 2) Color(0xFF4A90E2) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { transactionType = 2 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Transferir", color = if (transactionType == 2) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Value input
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "VALOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tint = when (transactionType) {
                        0 -> MaterialTheme.colorScheme.onBackground
                        1 -> Color(0xFF71D7CD)
                        else -> Color(0xFF4A90E2)
                    }
                    Text(
                        text = "R$ ",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = tint
                    )
                    BasicTextField(
                        value = valueStr,
                        onValueChange = { valueStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Start
                        ),
                        cursorBrush = SolidColor(tint),
                        modifier = Modifier.width(180.dp)
                    )
                }
                Box(modifier = Modifier.width(220.dp).height(1.dp).background(themedDivider()))
            }

            if (isTransfer) {
                // Origem Selector (Bank Accounts Only)
                Column {
                    Text("CONTA DE ORIGEM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bankAccounts) { acc ->
                            val isSelected = fromAccount == acc.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF4A90E2).copy(alpha = 0.2f) else themedSubtleBackground())
                                    .border(1.dp, if (isSelected) Color(0xFF4A90E2) else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { fromAccount = acc.name }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(acc.name, color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }
                }
                // Destino Selector (Bank Accounts Only)
                Column {
                    Text("CONTA DE DESTINO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bankAccounts.filter { it.name != fromAccount }) { acc ->
                            val isSelected = toAccount == acc.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF4A90E2).copy(alpha = 0.2f) else themedSubtleBackground())
                                    .border(1.dp, if (isSelected) Color(0xFF4A90E2) else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { toAccount = acc.name }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(acc.name, color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }
                }
            } else {
                // Origin selector for Income/Expense
                Column {
                    Text("DEBITAR/CREDITAR EM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (origins.isEmpty()) {
                        Text("Cadastre uma conta ou cartão primeiro.", color = Color(0xFFEF4444), fontSize = 12.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(origins) { originName ->
                                val isSelected = selectedOrigin == originName
                                val originColor = bankAccounts.find { it.name == originName }?.colorHex
                                    ?: creditCards.find { it.name == originName }?.colorHex
                                    ?: benefitCards.find { it.name == originName }?.colorHex
                                    ?: "#71D7CD"
                                val c = try { Color(android.graphics.Color.parseColor(originColor)) } catch(e:Exception){ MaterialTheme.colorScheme.onBackground }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) c.copy(alpha = 0.2f) else themedSubtleBackground())
                                        .border(1.dp, if (isSelected) c else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { selectedOrigin = originName }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(originName, color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Title Input
            Column {
                Text("TÍTULO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = themedSubtleBackground(),
                        unfocusedContainerColor = themedSubtleBackground(),
                        focusedBorderColor = if (isIncome) Color(0xFF71D7CD) else Color(0xFFEF4444),
                        unfocusedBorderColor = themedSubtleBorder(),
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Category Selector
            Column {
                Text("CATEGORIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allCategories) { (catName, icon) ->
                        val isSelected = category == catName
                        val selColor = if (isIncome) Color(0xFF71D7CD) else Color(0xFFEF4444)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) selColor.copy(alpha = 0.2f) else themedSubtleBackground())
                                .border(1.dp, if (isSelected) selColor else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { category = catName }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(icon, contentDescription = null, tint = if (isSelected) selColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Text(catName, color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(themedSubtleBackground())
                                .border(1.dp, Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { showNewCategoryDialog = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Text("Nova", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Status Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(themedSubtleBackground())
                    .clickable { isRealized = !isRealized }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (isIncome) "Recebido" else "Pago", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                Switch(
                    checked = isRealized,
                    onCheckedChange = { isRealized = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onBackground, checkedTrackColor = if(isIncome) Color(0xFF71D7CD) else Color(0xFFEF4444))
                )
            }

            // Data de Vencimento
            val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
            var showDatePicker by remember { mutableStateOf(false) }

            Column {
                Text("DATA DE VENCIMENTO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(themedSubtleBackground())
                        .border(1.dp, themedSubtleBorder(), RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (dueDate > 0L) dateFormat.format(Date(dueDate)) else "Hoje",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp
                        )
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "Selecionar Data",
                            tint = Color(0xFF71D7CD),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = if (dueDate > 0L) dueDate else System.currentTimeMillis())
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { dueDate = it }
                            showDatePicker = false
                        }) {
                            Text("OK", color = Color(0xFF71D7CD))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Opção de Conta Recorrente / Fixa
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(themedSubtleBackground())
                    .clickable { isRecurrent = !isRecurrent }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Conta Recorrente / Fixa Mensal?", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                    Text("Repete todos os meses automaticamente", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Switch(
                    checked = isRecurrent,
                    onCheckedChange = { isRecurrent = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onBackground, checkedTrackColor = Color(0xFF71D7CD))
                )
            }

            if (isRecurrent) {
                Column {
                    Text("FREQUÊNCIA DA RECORRÊNCIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Mensal", "Semanal", "Anual").forEach { interval ->
                            val isSelected = recurrenceInterval == interval
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF71D7CD).copy(alpha = 0.2f) else themedSubtleBackground())
                                    .border(1.dp, if (isSelected) Color(0xFF71D7CD) else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { recurrenceInterval = interval }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(interval, color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            if (!isIncome && !isTransfer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(themedSubtleBackground())
                        .clickable { isInstallment = !isInstallment }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Parcelar Lançamento?", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                    Switch(
                        checked = isInstallment,
                        onCheckedChange = { isInstallment = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onBackground, checkedTrackColor = Color(0xFF4A90E2))
                    )
                }
                
                if (isInstallment) {
                    Column {
                        Text("QUANTIDADE DE PARCELAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = installmentsCountStr,
                            onValueChange = { installmentsCountStr = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = themedSubtleBackground(),
                                unfocusedContainerColor = themedSubtleBackground(),
                                focusedBorderColor = Color(0xFF4A90E2),
                                unfocusedBorderColor = themedSubtleBorder(),
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (editingTransaction != null) {
                    Button(
                        onClick = { onDelete(editingTransaction) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AEF4444)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Excluir", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
                
                val parsedValue = valueStr.toDoubleCleanOrZero()
                val canSave = if (isTransfer) {
                    fromAccount != null && toAccount != null && parsedValue > 0.0
                } else {
                    title.isNotBlank() && parsedValue > 0.0 && selectedOrigin.isNotBlank()
                }

                val actionButtonColor = if (isTransfer) Color(0xFF4A90E2) else if (isIncome) Color(0xFF71D7CD) else MaterialTheme.colorScheme.primary
                val actionTextColor = if (isTransfer || isIncome) Color.Black else MaterialTheme.colorScheme.onPrimary

                Button(
                    onClick = {
                        val v = parsedValue
                        if (isTransfer) {
                            onTransfer(fromAccount!!, toAccount!!, v, dueDate)
                            onDismiss()
                        } else {
                            if (editingTransaction != null) {
                                val updated = editingTransaction.copy(
                                    title = title,
                                    value = v,
                                    isIncome = isIncome,
                                    category = category,
                                    accountOrCardName = selectedOrigin,
                                    isRealized = isRealized,
                                    isRecurrent = isRecurrent,
                                    recurrenceInterval = recurrenceInterval,
                                    dueDate = dueDate
                                )
                                onUpdate(editingTransaction, updated)
                            } else {
                                val installs = installmentsCountStr.toIntOrNull() ?: 1
                                onAdd(title, v, isIncome, category, selectedOrigin, isRealized, isRecurrent, recurrenceInterval, dueDate, isInstallment, installs)
                            }
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(if (editingTransaction != null) 1f else 2f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actionButtonColor,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (editingTransaction != null) "Salvar Alterações" else "Confirmar", color = actionTextColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showNewCategoryDialog) {
        NewCategoryDialog(
            onDismiss = { showNewCategoryDialog = false },
            onSave = { newName, newIcon ->
                val newString = "$newName|$newIcon"
                val oldSet = sharedPrefs.getStringSet("custom_categories", emptySet()) ?: emptySet()
                val newSet = oldSet.toMutableSet().apply { add(newString) }
                sharedPrefs.edit().putStringSet("custom_categories", newSet).apply()
                
                val iconVec = when (newIcon) {
                    "Home" -> Icons.Outlined.Home
                    "Pets" -> Icons.Outlined.Pets
                    "DirectionsCar" -> Icons.Outlined.DirectionsCar
                    "ShoppingBag" -> Icons.Outlined.ShoppingBag
                    "Flight" -> Icons.Outlined.Flight
                    "School" -> Icons.Outlined.School
                    "FitnessCenter" -> Icons.Outlined.FitnessCenter
                    "LocalDining" -> Icons.Outlined.LocalDining
                    else -> Icons.Outlined.Label
                }
                customCategories = (customCategories + (newName to iconVec)).sortedBy { it.first }
                category = newName
                showNewCategoryDialog = false
            }
        )
    }
}

@Composable
fun NewCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("Label") }
    
    val icons = listOf(
        "Label" to Icons.Outlined.Label,
        "Home" to Icons.Outlined.Home,
        "Pets" to Icons.Outlined.Pets,
        "DirectionsCar" to Icons.Outlined.DirectionsCar,
        "ShoppingBag" to Icons.Outlined.ShoppingBag,
        "Flight" to Icons.Outlined.Flight,
        "School" to Icons.Outlined.School,
        "FitnessCenter" to Icons.Outlined.FitnessCenter,
        "LocalDining" to Icons.Outlined.LocalDining
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Categoria", color = MaterialTheme.colorScheme.onBackground) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    colors = themedOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(icons) { (iconName, iconVector) ->
                        val isSel = selectedIcon == iconName
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFF71D7CD).copy(alpha = 0.2f) else themedSubtleBackground())
                                .border(1.dp, if (isSel) Color(0xFF71D7CD) else themedSubtleBorder(), RoundedCornerShape(8.dp))
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(iconVector, contentDescription = null, tint = if (isSel) Color(0xFF71D7CD) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name, selectedIcon) }) {
                Text("Salvar", color = Color(0xFF71D7CD))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onBackground)
            }
        }
    )
}
