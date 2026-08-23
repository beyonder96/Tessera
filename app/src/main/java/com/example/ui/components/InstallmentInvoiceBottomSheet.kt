package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BankAccount
import com.example.data.CreditCard
import com.example.ui.theme.PrimaryTeal
import com.example.utils.toDoubleCleanOrZero
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentInvoiceBottomSheet(
    card: CreditCard,
    bankAccounts: List<BankAccount>,
    currencyFormat: NumberFormat,
    onConfirm: (
        downPayment: Double,
        debitAccountName: String?,
        installmentsCount: Int,
        installmentAmount: Double,
        totalWithInterest: Double,
        firstDueDate: Long
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cardColor = remember(card.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(card.colorHex))
        } catch (e: Exception) {
            PrimaryTeal
        }
    }

    val totalInvoice = card.usedLimit
    var downPaymentText by remember { mutableStateOf("") }
    val downPayment = downPaymentText.toDoubleCleanOrZero().coerceAtMost(max(0.0, totalInvoice - 1.0))
    val remainingBalance = max(0.0, totalInvoice - downPayment)

    val availableInstallments = listOf(2, 3, 4, 5, 6, 8, 10, 12, 18, 24)
    var selectedInstallments by remember { mutableIntStateOf(3) }

    // Modo de cálculo: "installment" (digita valor da parcela) ou "rate" (digita taxa % mensal)
    var calculationMode by remember { mutableStateOf("installment") }
    var customInstallmentText by remember { mutableStateOf("") }
    var interestRateText by remember { mutableStateOf("2.5") }

    // Contas bancárias disponíveis para débito da entrada
    val checkingAccounts = remember(bankAccounts) { bankAccounts.filter { it.type == "Corrente" || it.type == "Carteira" } }
    var selectedDebitAccount by remember { mutableStateOf(checkingAccounts.firstOrNull()?.name) }

    // Data da 1ª parcela (padrão: 30 dias à frente / próximo mês)
    val defaultFirstDueDate = remember {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
        }.timeInMillis
    }
    var firstDueDate by remember { mutableLongStateOf(defaultFirstDueDate) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }

    // Cálculos de Parcela e Juros
    val installmentAmount: Double
    val totalWithInterest: Double
    val totalInterest: Double

    if (calculationMode == "installment") {
        val userTypedInstallment = customInstallmentText.toDoubleCleanOrZero()
        val defaultInstallment = if (selectedInstallments > 0) remainingBalance / selectedInstallments else 0.0
        installmentAmount = if (userTypedInstallment > 0.0) userTypedInstallment else defaultInstallment
        totalWithInterest = installmentAmount * selectedInstallments
        totalInterest = max(0.0, totalWithInterest - remainingBalance)
    } else {
        val monthlyRate = interestRateText.toDoubleCleanOrZero() / 100.0
        val baseInstallment = if (selectedInstallments > 0) remainingBalance / selectedInstallments else 0.0
        val computedInstallment = if (monthlyRate > 0.0 && selectedInstallments > 0) {
            baseInstallment * (1.0 + (monthlyRate * selectedInstallments / 2.0))
        } else {
            baseInstallment
        }
        installmentAmount = computedInstallment
        totalWithInterest = installmentAmount * selectedInstallments
        totalInterest = max(0.0, totalWithInterest - remainingBalance)
    }

    val interestPercentage = if (remainingBalance > 0.0) (totalInterest / remainingBalance) * 100.0 else 0.0
    val isValid = totalInvoice > 0.0 && installmentAmount > 0.0 && selectedInstallments >= 2

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = themedOverlayBackground(),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(cardColor.copy(alpha = 0.15f))
                            .border(1.dp, cardColor.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FormatListNumbered,
                            contentDescription = null,
                            tint = cardColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Parcelar Fatura",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${card.name} • Final ${card.numberLastFour}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Card Fatura Atual
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
                border = androidx.compose.foundation.BorderStroke(1.dp, themedCardBorder())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VALOR DA FATURA ATUAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormat.format(totalInvoice),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Em Aberto",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Entrada (Opcional)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Valor de Entrada (Opcional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

                OutlinedTextField(
                    value = downPaymentText,
                    onValueChange = { downPaymentText = it },
                    placeholder = { Text("R$ 0,00", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = themedTextFieldColors(),
                    leadingIcon = {
                        Text(
                            "R$",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                )

                if (downPayment > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Debitar entrada da conta:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(checkingAccounts) { acc ->
                            val isSelected = selectedDebitAccount == acc.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryTeal.copy(alpha = 0.18f) else themedCardBackground())
                                    .border(
                                        1.dp,
                                        if (isSelected) PrimaryTeal else themedCardBorder(),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedDebitAccount = acc.name }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalance,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = acc.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Seleção de Parcelas
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quantidade de Parcelas",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Saldo: ${currencyFormat.format(remainingBalance)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableInstallments) { count ->
                        val isSelected = selectedInstallments == count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryTeal else themedCardBackground())
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryTeal else themedCardBorder(),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedInstallments = count
                                    customInstallmentText = ""
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "${count}x",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }

            // Modo de Configuração de Valor e Juros
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (calculationMode == "installment") themedCardBackground() else Color.Transparent)
                            .clickable { calculationMode = "installment" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Valor da Parcela (R$)",
                            fontSize = 12.sp,
                            fontWeight = if (calculationMode == "installment") FontWeight.Bold else FontWeight.Normal,
                            color = if (calculationMode == "installment") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (calculationMode == "rate") themedCardBackground() else Color.Transparent)
                            .clickable { calculationMode = "rate" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Taxa de Juros (% a.m.)",
                            fontSize = 12.sp,
                            fontWeight = if (calculationMode == "rate") FontWeight.Bold else FontWeight.Normal,
                            color = if (calculationMode == "rate") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                if (calculationMode == "installment") {
                    val placeholderInstallment = if (selectedInstallments > 0) remainingBalance / selectedInstallments else 0.0
                    OutlinedTextField(
                        value = customInstallmentText,
                        onValueChange = { customInstallmentText = it },
                        placeholder = { Text(currencyFormat.format(placeholderInstallment), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = themedTextFieldColors(),
                        leadingIcon = {
                            Text(
                                "R$",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    )
                } else {
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = { interestRateText = it },
                        placeholder = { Text("Ex: 2.5", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = themedTextFieldColors(),
                        trailingIcon = {
                            Text(
                                "% a.m.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    )
                }
            }

            // Resumo do Parcelamento
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
                border = androidx.compose.foundation.BorderStroke(1.dp, themedCardBorder())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "RESUMO DO PARCELAMENTO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Valor de cada parcela",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${selectedInstallments}x de ${currencyFormat.format(installmentAmount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }

                    if (downPayment > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Entrada à vista",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Text(
                                text = currencyFormat.format(downPayment),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Juros do parcelamento",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (totalInterest > 0.01) "+ ${currencyFormat.format(totalInterest)} (${String.format(Locale("pt", "BR"), "%.1f", interestPercentage)}%)" else "Sem juros adicionais",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (totalInterest > 0.01) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(themedCardBorder())
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total a pagar (com juros)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = currencyFormat.format(totalWithInterest + downPayment),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1º Vencimento",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            text = dateFormatter.format(Date(firstDueDate)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Botão Confirmar
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(
                            downPayment,
                            selectedDebitAccount,
                            selectedInstallments,
                            installmentAmount,
                            totalWithInterest,
                            firstDueDate
                        )
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal,
                    contentColor = Color.Black,
                    disabledContainerColor = PrimaryTeal.copy(alpha = 0.3f),
                    disabledContentColor = Color.Black.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirmar Parcelamento",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
