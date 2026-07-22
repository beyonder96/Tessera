package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import android.content.Context
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
    var fromAccount by remember { mutableStateOf<String?>(null) }
    var toAccount by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE) }
    var customCategories by remember {
        val catsSet = sharedPrefs.getStringSet("custom_categories", emptySet()) ?: emptySet()
        mutableStateOf<List<String>>(catsSet.toList().sorted())
    }
    
    val defaultCategories = listOf(
        "Alimentação" to Icons.Outlined.Restaurant,
        "Transporte" to Icons.Outlined.DirectionsCar,
        "Saúde" to Icons.Outlined.MedicalServices,
        "Lazer" to Icons.Outlined.Movie,
        "Salário" to Icons.Outlined.AttachMoney,
        "Outros" to Icons.Outlined.Receipt
    )

    val allCategories: List<Pair<String, ImageVector>> = remember(customCategories) {
        defaultCategories + customCategories.map { it to Icons.Outlined.Label }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1115),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x33FFFFFF)) }
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
                    color = Color.White
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                }
            }

            // Despesa/Receita/Transferência Toggle Switch
            if (editingTransaction == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x14FFFFFF))
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
                        Text("Despesa", color = if (transactionType == 0) Color.White else Color(0x66FFFFFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        Text("Receita", color = if (transactionType == 1) Color.White else Color(0x66FFFFFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        Text("Transferir", color = if (transactionType == 2) Color.White else Color(0x66FFFFFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Value input
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "VALOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0x66FFFFFF),
                    letterSpacing = 1.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tint = when (transactionType) {
                        0 -> Color.White
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
                            color = Color.White,
                            textAlign = TextAlign.Start
                        ),
                        cursorBrush = SolidColor(tint),
                        modifier = Modifier.width(180.dp)
                    )
                }
                Box(modifier = Modifier.width(220.dp).height(1.dp).background(Color(0x33FFFFFF)))
            }

            if (isTransfer) {
                // Origem Selector (Bank Accounts Only)
                Column {
                    Text("ORIGEM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bankAccounts) { acc ->
                            val isSelected = fromAccount == acc.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF4A90E2).copy(alpha = 0.2f) else Color(0x14FFFFFF))
                                    .border(1.dp, if (isSelected) Color(0xFF4A90E2) else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { fromAccount = acc.name }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(acc.name, color = if (isSelected) Color.White else Color(0x99FFFFFF), fontSize = 14.sp)
                            }
                        }
                    }
                }
                // Destino Selector (Bank Accounts Only)
                Column {
                    Text("DESTINO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bankAccounts.filter { it.name != fromAccount }) { acc ->
                            val isSelected = toAccount == acc.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF4A90E2).copy(alpha = 0.2f) else Color(0x14FFFFFF))
                                    .border(1.dp, if (isSelected) Color(0xFF4A90E2) else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { toAccount = acc.name }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(acc.name, color = if (isSelected) Color.White else Color(0x99FFFFFF), fontSize = 14.sp)
                            }
                        }
                    }
                }
            } else {
                // Origin selector for Income/Expense
                Column {
                    Text("DEBITAR/CREDITAR EM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
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
                                val c = try { Color(android.graphics.Color.parseColor(originColor)) } catch(e:Exception){ Color.White }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) c.copy(alpha = 0.2f) else Color(0x14FFFFFF))
                                        .border(1.dp, if (isSelected) c else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { selectedOrigin = originName }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(originName, color = if (isSelected) Color.White else Color(0x99FFFFFF), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                // Title Input
                Column {
                    Text("TÍTULO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x0AFFFFFF),
                            unfocusedContainerColor = Color(0x0AFFFFFF),
                            focusedBorderColor = Color(0xFF71D7CD),
                            unfocusedBorderColor = Color(0x1AFFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Category Selector
                Column {
                    Text("CATEGORIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allCategories) { (catName, icon) ->
                            val isSelected = category == catName
                            val selColor = if (isIncome) Color(0xFF71D7CD) else Color(0xFFEF4444)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) selColor.copy(alpha = 0.2f) else Color(0x14FFFFFF))
                                    .border(1.dp, if (isSelected) selColor else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { category = catName }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(icon, contentDescription = null, tint = if (isSelected) selColor else Color(0x99FFFFFF), modifier = Modifier.size(16.dp))
                                    Text(catName, color = if (isSelected) Color.White else Color(0x99FFFFFF), fontSize = 13.sp)
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
                        .background(Color(0x14FFFFFF))
                        .clickable { isRealized = !isRealized }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (isIncome) "Recebido" else "Pago", color = Color.White, fontSize = 16.sp)
                    Switch(
                        checked = isRealized,
                        onCheckedChange = { isRealized = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = if(isIncome) Color(0xFF71D7CD) else Color(0xFFEF4444))
                    )
                }

                // Data de Vencimento
                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
                var showDatePicker by remember { mutableStateOf(false) }

                Column {
                    Text("DATA DE VENCIMENTO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0AFFFFFF))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
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
                                color = Color.White,
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
                                Text("Cancelar", color = Color.Gray)
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
                        .background(Color(0x14FFFFFF))
                        .clickable { isRecurrent = !isRecurrent }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Conta Recorrente / Fixa Mensal?", color = Color.White, fontSize = 16.sp)
                        Text("Repete todos os meses automaticamente", color = Color(0x66FFFFFF), fontSize = 11.sp)
                    }
                    Switch(
                        checked = isRecurrent,
                        onCheckedChange = { isRecurrent = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF71D7CD))
                    )
                }

                if (isRecurrent) {
                    Column {
                        Text("FREQUÊNCIA DA RECORRÊNCIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Mensal", "Semanal", "Anual").forEach { interval ->
                                val isSelected = recurrenceInterval == interval
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFF71D7CD).copy(alpha = 0.2f) else Color(0x14FFFFFF))
                                        .border(1.dp, if (isSelected) Color(0xFF71D7CD) else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { recurrenceInterval = interval }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(interval, color = if (isSelected) Color.White else Color(0x99FFFFFF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                if (!isIncome) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x14FFFFFF))
                            .clickable { isInstallment = !isInstallment }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Parcelar Lançamento?", color = Color.White, fontSize = 16.sp)
                        Switch(
                            checked = isInstallment,
                            onCheckedChange = { isInstallment = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF4A90E2))
                        )
                    }
                    
                    if (isInstallment) {
                        Column {
                            Text("QUANTIDADE DE PARCELAS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = installmentsCountStr,
                                onValueChange = { installmentsCountStr = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0x0AFFFFFF),
                                    unfocusedContainerColor = Color(0x0AFFFFFF),
                                    focusedBorderColor = Color(0xFF4A90E2),
                                    unfocusedBorderColor = Color(0x1AFFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
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
                
                val canSave = if (isTransfer) {
                    fromAccount != null && toAccount != null && valueStr.toDoubleOrNull() ?: 0.0 > 0.0
                } else {
                    title.isNotBlank() && valueStr.toDoubleOrNull() ?: 0.0 > 0.0 && selectedOrigin.isNotBlank()
                }

                Button(
                    onClick = {
                        val v = valueStr.toDoubleOrNull() ?: 0.0
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
                        containerColor = if (isTransfer) Color(0xFF4A90E2) else if (isIncome) Color(0xFF71D7CD) else Color.White,
                        disabledContainerColor = Color(0x33FFFFFF)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (editingTransaction != null) "Salvar Alterações" else "Confirmar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
