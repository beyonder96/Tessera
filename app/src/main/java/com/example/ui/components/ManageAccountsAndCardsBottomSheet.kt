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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BankAccount
import com.example.data.BenefitCard
import com.example.data.CreditCard
import com.example.viewmodel.TesseraViewModel
import java.util.*

val manageColorPalettes = listOf("#8A05BE", "#FF7A00", "#E6C619", "#1C1C1C", "#0088FF", "#71D7CD", "#E94057", "#417505")

fun parseManageHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF71D7CD)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsAndCardsBottomSheet(
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    benefitCards: List<BenefitCard>,
    viewModel: TesseraViewModel,
    initialEditingAccount: BankAccount? = null,
    initialEditingCreditCard: CreditCard? = null,
    initialEditingBenefitCard: BenefitCard? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Tabs: 0 -> Contas, 1 -> Crédito, 2 -> Benefícios
    var selectedTab by remember { mutableStateOf(
        when {
            initialEditingAccount != null -> 0
            initialEditingCreditCard != null -> 1
            initialEditingBenefitCard != null -> 2
            else -> 0
        }
    ) }
    var showForm by remember { mutableStateOf(initialEditingAccount != null || initialEditingCreditCard != null || initialEditingBenefitCard != null) }    // Form inputs
    var name by remember { mutableStateOf("") }
    var limitOrBalance by remember { mutableStateOf("") }
    var cardUsedLimit by remember { mutableStateOf("") }
    var lastFour by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("KENNETH S. O.") }
    var accountType by remember { mutableStateOf("Corrente") }
    var selectedColor by remember { mutableStateOf(manageColorPalettes.first()) }

    // Track items being edited
    var editingAccount by remember { mutableStateOf<BankAccount?>(initialEditingAccount) }
    var editingCard by remember { mutableStateOf<CreditCard?>(initialEditingCreditCard) }
    var editingBenefit by remember { mutableStateOf<BenefitCard?>(initialEditingBenefitCard) }
    LaunchedEffect(initialEditingAccount, initialEditingCreditCard, initialEditingBenefitCard) {
        if (initialEditingAccount != null) {
            name = initialEditingAccount.name
            limitOrBalance = initialEditingAccount.balance.toString()
            accountType = initialEditingAccount.type
            selectedColor = initialEditingAccount.colorHex
        } else if (initialEditingCreditCard != null) {
            name = initialEditingCreditCard.name
            limitOrBalance = initialEditingCreditCard.limit.toString()
            cardUsedLimit = initialEditingCreditCard.usedLimit.toString()
            lastFour = initialEditingCreditCard.numberLastFour
            holder = initialEditingCreditCard.holderName
            selectedColor = initialEditingCreditCard.colorHex
        } else if (initialEditingBenefitCard != null) {
            name = initialEditingBenefitCard.name
            limitOrBalance = initialEditingBenefitCard.balance.toString()
            lastFour = initialEditingBenefitCard.numberLastFour
            holder = initialEditingBenefitCard.holderName
            selectedColor = initialEditingBenefitCard.colorHex
        }
    }



    fun resetForm() {
        name = ""
        limitOrBalance = ""
        cardUsedLimit = ""
        lastFour = ""
        holder = "KENNETH S. O."
        accountType = "Corrente"
        selectedColor = manageColorPalettes.first()
        editingAccount = null
        editingCard = null
        editingBenefit = null
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
                    text = if (showForm) {
                        when {
                            editingAccount != null -> "Editar Conta"
                            editingCard != null -> "Editar Cartão de Crédito"
                            editingBenefit != null -> "Editar Cartão de Benefício"
                            else -> "Novo(a) Cadastro"
                        }
                    } else "Gerenciar Finanças",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                IconButton(onClick = { if (showForm) { showForm = false; resetForm() } else onDismiss() }, modifier = Modifier.size(24.dp)) {
                    Icon(if (showForm) Icons.Default.Close else Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                }
            }

            if (!showForm) {
                // Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(4.dp)
                ) {
                    val tabs = listOf("Contas", "Crédito", "Benefícios")
                    tabs.forEachIndexed { index, title ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTab == index) Color(0xFF71D7CD).copy(alpha = 0.15f) else Color.Transparent)
                                .border(1.dp, if (selectedTab == index) Color(0xFF71D7CD) else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(title, color = if (selectedTab == index) Color.White else Color(0x99FFFFFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // List View
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        0 -> { // Contas
                            if (bankAccounts.isEmpty()) {
                                Text("Nenhuma conta cadastrada.", color = Color(0x66FFFFFF), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                            }
                            bankAccounts.forEach { account ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.size(12.dp).background(parseManageHexColor(account.colorHex), CircleShape))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(account.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(account.type, color = Color(0x80BDC9C6), fontSize = 11.sp)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(String.format(Locale("pt", "BR"), "R$ %,.2f", account.balance), color = Color(0xFF71D7CD), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        IconButton(
                                            onClick = {
                                                editingAccount = account
                                                name = account.name
                                                limitOrBalance = account.balance.toString()
                                                accountType = account.type
                                                selectedColor = account.colorHex
                                                showForm = true
                                            }, 
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF71D7CD), modifier = Modifier.size(15.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { viewModel.deleteBankAccount(account) }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Crédito
                            if (creditCards.isEmpty()) {
                                Text("Nenhum cartão de crédito cadastrado.", color = Color(0x66FFFFFF), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                            }
                            creditCards.forEach { card ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.size(12.dp).background(parseManageHexColor(card.colorHex), CircleShape))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(card.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Final: ${card.numberLastFour}", color = Color(0x80BDC9C6), fontSize = 11.sp)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(String.format(Locale("pt", "BR"), "R$ %,.2f", card.limit), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        IconButton(
                                            onClick = {
                                                editingCard = card
                                                name = card.name
                                                limitOrBalance = card.limit.toString()
                                                cardUsedLimit = card.usedLimit.toString()
                                                lastFour = card.numberLastFour
                                                holder = card.holderName
                                                selectedColor = card.colorHex
                                                showForm = true
                                            }, 
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF71D7CD), modifier = Modifier.size(15.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { viewModel.deleteCreditCard(card) }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                        2 -> { // Benefícios
                            if (benefitCards.isEmpty()) {
                                Text("Nenhum cartão de benefício cadastrado.", color = Color(0x66FFFFFF), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                            }
                            benefitCards.forEach { card ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.size(12.dp).background(parseManageHexColor(card.colorHex), CircleShape))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(card.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Final: ${card.numberLastFour}", color = Color(0x80BDC9C6), fontSize = 11.sp)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(String.format(Locale("pt", "BR"), "R$ %,.2f", card.balance), color = Color(0xFFE94057), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        IconButton(
                                            onClick = {
                                                editingBenefit = card
                                                name = card.name
                                                limitOrBalance = card.balance.toString()
                                                lastFour = card.numberLastFour
                                                holder = card.holderName
                                                selectedColor = card.colorHex
                                                showForm = true
                                            }, 
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF71D7CD), modifier = Modifier.size(15.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { viewModel.deleteBenefitCard(card) }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { 
                        showForm = true
                        resetForm()
                        if (selectedTab == 1) cardUsedLimit = "0"
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Adicionar Novo(a)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

            } else {
                // Creation/Edition Form
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome (ex: Nubank, VR)", color = Color(0x99FFFFFF)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF71D7CD),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedTab == 0) {
                    // Bank Account Specifics
                    Text("TIPO DE CONTA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x14FFFFFF)).padding(4.dp)
                    ) {
                        listOf("Corrente", "Poupança", "Investimento").forEach { type ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (accountType == type) Color(0xFF4A90E2).copy(alpha = 0.2f) else Color.Transparent)
                                    .border(1.dp, if (accountType == type) Color(0xFF4A90E2) else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable { accountType = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(type, color = if (accountType == type) Color.White else Color(0x99FFFFFF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Card Specifics
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = lastFour,
                            onValueChange = { if (it.length <= 4) lastFour = it },
                            label = { Text("Final", color = Color(0x99FFFFFF)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x33FFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = holder,
                            onValueChange = { holder = it },
                            label = { Text("Titular", color = Color(0x99FFFFFF)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x33FFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(2f)
                        )
                    }
                }

                OutlinedTextField(
                    value = limitOrBalance,
                    onValueChange = { limitOrBalance = it },
                    label = { Text(if (selectedTab == 1) "Limite Total (R$)" else "Saldo (R$)", color = Color(0x99FFFFFF)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF71D7CD),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedTab == 1) {
                    OutlinedTextField(
                        value = cardUsedLimit,
                        onValueChange = { cardUsedLimit = it },
                        label = { Text("Limite Utilizado (R$)", color = Color(0x99FFFFFF)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF71D7CD),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("COR DE IDENTIFICAÇÃO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF), letterSpacing = 1.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(manageColorPalettes) { hex ->
                        val color = parseManageHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(3.dp, if (selectedColor == hex) Color.White else Color.Transparent, CircleShape)
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                val canSave = name.isNotBlank() && limitOrBalance.toDoubleOrNull() != null
                Button(
                    onClick = {
                        val value = limitOrBalance.toDoubleOrNull() ?: 0.0
                        when (selectedTab) {
                            0 -> {
                                viewModel.addBankAccount(name, value, accountType, selectedColor, editingAccount?.id ?: 0)
                            }
                            1 -> {
                                val used = cardUsedLimit.toDoubleOrNull() ?: 0.0
                                viewModel.addCreditCard(name, value, used, lastFour, selectedColor, holder, editingCard?.id ?: 0)
                            }
                            2 -> {
                                viewModel.addBenefitCard(name, value, lastFour, selectedColor, holder, editingBenefit?.id ?: 0)
                            }
                        }
                        showForm = false
                        resetForm()
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD), disabledContainerColor = Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
