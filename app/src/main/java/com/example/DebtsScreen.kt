package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Debt
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.bounceClick
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryGold
import com.example.viewmodel.TesseraViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    viewModel: TesseraViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val debts by viewModel.allDebts.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<Debt?>(null) }
    var showPayDialog by remember { mutableStateOf<Debt?>(null) }
    
    // Summary Calculations
    val totalOwed = remember(debts) { debts.sumOf { it.value } }
    val totalPaid = remember(debts) {
        debts.sumOf { 
            val installmentVal = it.value / it.installmentsTotal
            installmentVal * it.installmentsPaid
        }
    }
    val remainingToPay = totalOwed - totalPaid
    val overallProgress = if (totalOwed > 0.0) (totalPaid / totalOwed).toFloat() else 0f

    val activeDebts = remember(debts) { debts.filter { !it.isPaid } }
    val paidDebts = remember(debts) { debts.filter { it.isPaid } }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Painel de Dívidas",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.bounceClick { onBack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingDebt = null
                            showAddDialog = true
                        },
                        modifier = Modifier.bounceClick {
                            editingDebt = null
                            showAddDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Nova Dívida",
                            tint = SecondaryGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 60.dp, top = 8.dp)
        ) {
            // Summary Card
            item {
                Card(
                    modifier = PremiumGlassModifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "RESUMO DE DÍVIDAS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryGold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Devido", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %,.2f", totalOwed),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Restante", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %,.2f", remainingToPay),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Progresso de Quitação", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", overallProgress * 100),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = PrimaryTeal,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            // Active Debts Section
            if (activeDebts.isNotEmpty()) {
                item {
                    Text(
                        text = "ATIVAS (${activeDebts.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                }

                items(activeDebts) { debt ->
                    val installmentValue = debt.value / debt.installmentsTotal
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val dueDateStr = sdf.format(Date(debt.dueDate))

                    Card(
                        modifier = PremiumGlassModifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = debt.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (debt.creditorName.isNotBlank()) {
                                        Text(
                                            text = "Credor: ${debt.creditorName}",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %,.2f", debt.value),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Vencimento: $dueDateStr",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }

                                Text(
                                    text = "Parcela ${debt.installmentsPaid}/${debt.installmentsTotal}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SecondaryGold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            val progress = debt.installmentsPaid.toFloat() / debt.installmentsTotal.toFloat()
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = SecondaryGold,
                                trackColor = Color.White.copy(alpha = 0.05f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showPayDialog = debt },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(38.dp)
                                        .bounceClick { showPayDialog = debt }
                                ) {
                                    Text(
                                        text = "Pagar Parcela (${String.format(Locale("pt", "BR"), "R$ %,.2f", installmentValue)})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        editingDebt = debt
                                        showAddDialog = true
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .bounceClick {
                                            editingDebt = debt
                                            showAddDialog = true
                                        },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Editar",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.deleteDebt(debt)
                                        Toast.makeText(context, "Dívida excluída", Toast.LENGTH_SHORT).show()
                                    },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .bounceClick {
                                            viewModel.deleteDebt(debt)
                                            Toast.makeText(context, "Dívida excluída", Toast.LENGTH_SHORT).show()
                                        },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Excluir",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Paid Debts Section
            if (paidDebts.isNotEmpty()) {
                item {
                    Text(
                        text = "QUITADAS (${paidDebts.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.3f),
                        letterSpacing = 1.sp
                    )
                }

                items(paidDebts) { debt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = debt.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.5f),
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                                Text(
                                    text = "Quitada em ${debt.installmentsTotal} parcelas",
                                    fontSize = 11.sp,
                                    color = PrimaryTeal
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = String.format(Locale("pt", "BR"), "R$ %,.2f", debt.value),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (activeDebts.isEmpty() && paidDebts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.ThumbUp,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Sem dívidas registradas. Muito bem!",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Dialog
    if (showAddDialog) {
        var title by remember { mutableStateOf(editingDebt?.title ?: "") }
        var description by remember { mutableStateOf(editingDebt?.description ?: "") }
        var creditor by remember { mutableStateOf(editingDebt?.creditorName ?: "") }
        var totalValueStr by remember { mutableStateOf(editingDebt?.value?.toString() ?: "") }
        var totalInstallmentsStr by remember { mutableStateOf(editingDebt?.installmentsTotal?.toString() ?: "1") }
        var paidInstallmentsStr by remember { mutableStateOf(editingDebt?.installmentsPaid?.toString() ?: "0") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF161A19).copy(alpha = 0.98f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text(
                            text = if (editingDebt == null) "Adicionar Dívida" else "Editar Dívida",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Nome / Título") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SecondaryGold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Descrição") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SecondaryGold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = creditor,
                            onValueChange = { creditor = it },
                            label = { Text("Credor") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SecondaryGold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = totalValueStr,
                            onValueChange = { totalValueStr = it },
                            label = { Text("Valor Total Devido") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SecondaryGold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = totalInstallmentsStr,
                                onValueChange = { totalInstallmentsStr = it },
                                label = { Text("Parcelas Totais") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = SecondaryGold
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = paidInstallmentsStr,
                                onValueChange = { paidInstallmentsStr = it },
                                label = { Text("Pagas") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = SecondaryGold
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    val finalVal = totalValueStr.toDoubleOrNull() ?: 0.0
                                    val totInst = totalInstallmentsStr.toIntOrNull() ?: 1
                                    val paidInst = paidInstallmentsStr.toIntOrNull() ?: 0
                                    
                                    if (title.isBlank() || finalVal <= 0.0) {
                                        Toast.makeText(context, "Preencha o título e o valor corretamente", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val isFullyPaid = paidInst >= totInst
                                    
                                    val newOrUpdated = editingDebt?.copy(
                                        title = title,
                                        description = description,
                                        value = finalVal,
                                        creditorName = creditor,
                                        installmentsTotal = totInst,
                                        installmentsPaid = paidInst,
                                        isPaid = isFullyPaid
                                    ) ?: Debt(
                                        title = title,
                                        description = description,
                                        value = finalVal,
                                        dueDate = System.currentTimeMillis() + 86400000 * 30, // Default 30 days due
                                        creditorName = creditor,
                                        installmentsTotal = totInst,
                                        installmentsPaid = paidInst,
                                        isPaid = isFullyPaid
                                    )

                                    viewModel.insertDebt(newOrUpdated)
                                    showAddDialog = false
                                    Toast.makeText(context, "Salvo com sucesso!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Pay Installment Account Selector Dialog
    if (showPayDialog != null) {
        val targetDebt = showPayDialog!!
        
        Dialog(onDismissRequest = { showPayDialog = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF161A19))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pagar Parcela de Dívida",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Selecione a conta de origem para o pagamento da parcela (${String.format(Locale("pt", "BR"), "R$ %,.2f", targetDebt.value / targetDebt.installmentsTotal)}) de \"${targetDebt.title}\":",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    if (bankAccounts.isEmpty()) {
                        Text(
                            text = "Nenhuma conta cadastrada para pagamento.",
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                        ) {
                            items(bankAccounts) { account ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .clickable {
                                            viewModel.payDebtInstallment(targetDebt, account.name)
                                            showPayDialog = null
                                            Toast.makeText(context, "Parcela paga e registrada!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = account.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = String.format(Locale("pt", "BR"), "R$ %,.2f", account.balance),
                                        color = SecondaryGold,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showPayDialog = null },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text("Cancelar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
