package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BenefitCard
import com.example.data.CreditCard
import com.example.viewmodel.TesseraViewModel
import com.example.ui.theme.PrimaryTeal

val colorPalette = listOf(
    "#71D7CD", "#E94057", "#8A2BE2", "#F5A623", 
    "#4A90E2", "#50E3C2", "#B8E986", "#F8E71C", 
    "#D0021B", "#8B572A", "#417505", "#BD10E0",
    "#9013FE", "#4A4A4A", "#1E1E1E", "#000000"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCardsDialog(
    creditCards: List<CreditCard>,
    benefitCards: List<BenefitCard>,
    viewModel: TesseraViewModel,
    onDismiss: () -> Unit
) {
    var isBenefitMode by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var numberLastFour by remember { mutableStateOf("") }
    var limitOrBalance by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf(colorPalette[0]) }
    
    var editingCreditCard by remember { mutableStateOf<CreditCard?>(null) }
    var editingBenefitCard by remember { mutableStateOf<BenefitCard?>(null) }

    fun resetForm() {
        name = ""
        holderName = ""
        numberLastFour = ""
        limitOrBalance = ""
        colorHex = colorPalette[0]
        editingCreditCard = null
        editingBenefitCard = null
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xED070909)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (editingCreditCard != null || editingBenefitCard != null) "Editar Cartão" else "Novo Cartão",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                if (editingCreditCard == null && editingBenefitCard == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1AFFFFFF))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isBenefitMode) Color(0xFF71D7CD) else Color.Transparent)
                                .clickable { isBenefitMode = false; resetForm() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Crédito", color = if (!isBenefitMode) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isBenefitMode) Color(0xFFE94057) else Color.Transparent)
                                .clickable { isBenefitMode = true; resetForm() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Benefício", color = if (isBenefitMode) Color.White else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome (ex: Nubank, VR)", color = Color(0x99FFFFFF)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isBenefitMode) Color(0xFFE94057) else PrimaryTeal,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = limitOrBalance,
                    onValueChange = { limitOrBalance = it },
                    label = { Text(if (isBenefitMode) "Saldo Inicial (R$)" else "Limite Total (R$)", color = Color(0x99FFFFFF)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isBenefitMode) Color(0xFFE94057) else PrimaryTeal,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = numberLastFour,
                        onValueChange = { if (it.length <= 4) numberLastFour = it },
                        label = { Text("Últimos 4 dígitos", color = Color(0x99FFFFFF)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isBenefitMode) Color(0xFFE94057) else PrimaryTeal,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text("Nome do Titular", color = Color(0x99FFFFFF)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isBenefitMode) Color(0xFFE94057) else PrimaryTeal,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Cor do Cartão", color = Color.White, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorPalette) { hex ->
                        val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { PrimaryTeal }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (colorHex == hex) 3.dp else 0.dp,
                                    color = if (colorHex == hex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (editingCreditCard != null || editingBenefitCard != null) {
                        TextButton(onClick = {
                            editingCreditCard?.let { viewModel.deleteCreditCard(it) }
                            editingBenefitCard?.let { viewModel.deleteBenefitCard(it) }
                            resetForm()
                            onDismiss()
                        }) {
                            Text("Excluir", color = Color(0xFFEF4444))
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = Color.White.copy(alpha=0.7f))
                        }
                    }

                    Button(
                        onClick = {
                            val valDouble = limitOrBalance.replace(",", ".").toDoubleOrNull() ?: 0.0
                            if (isBenefitMode) {
                                viewModel.addBenefitCard(
                                    name = name.ifBlank { "Cartão Benefício" },
                                    balance = valDouble,
                                    numberLastFour = numberLastFour.ifBlank { "0000" },
                                    colorHex = colorHex,
                                    holderName = holderName.ifBlank { "Titular" },
                                    id = editingBenefitCard?.id ?: 0
                                )
                            } else {
                                viewModel.addCreditCard(
                                    name = name.ifBlank { "Cartão Crédito" },
                                    limit = valDouble,
                                    usedLimit = editingCreditCard?.usedLimit ?: 0.0,
                                    numberLastFour = numberLastFour.ifBlank { "0000" },
                                    colorHex = colorHex,
                                    holderName = holderName.ifBlank { "Titular" },
                                    id = editingCreditCard?.id ?: 0
                                )
                            }
                            resetForm()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isBenefitMode) Color(0xFFE94057) else PrimaryTeal)
                    ) {
                        Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
