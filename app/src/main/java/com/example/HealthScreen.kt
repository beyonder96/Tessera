package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.ui.components.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(onHomeClick: () -> Unit) {
    // States
    var weight by remember { mutableStateOf(78.2) }
    var showWeightDialog by remember { mutableStateOf(false) }
    
    // Medicines list
    data class Medicine(val id: Int, val name: String, val dosage: String, val time: String, val taken: Boolean)
    var medicines by remember { mutableStateOf(listOf(
        Medicine(1, "Vitamina D", "1 cápsula - 2000 UI", "08:00", true),
        Medicine(2, "Ômega 3", "2 cápsulas - 1000mg", "12:00", false),
        Medicine(3, "Melatonina", "3mg - Sublingual", "21:30", false)
    )) }
    var showAddMedDialog by remember { mutableStateOf(false) }

    // Weight Registration Dialog
    if (showWeightDialog) {
        var tempWeight by remember { mutableStateOf(weight.toString()) }
        Dialog(onDismissRequest = { showWeightDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF070909))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text("REGISTRAR PESO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("DIGITE SEU PESO ATUAL (KG)", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = tempWeight,
                        onValueChange = { tempWeight = it },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF131817),
                            unfocusedContainerColor = Color(0xFF131817),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showWeightDialog = false }) {
                            Text("CANCELAR", color = Color(0xFF879391), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val w = tempWeight.replace(',', '.').toDoubleOrNull()
                                if (w != null && w > 0) {
                                    weight = w
                                }
                                showWeightDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black)
                        ) {
                            Text("SALVAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Add Medicine Dialog
    if (showAddMedDialog) {
        var medName by remember { mutableStateOf("") }
        var medDosage by remember { mutableStateOf("") }
        var medTime by remember { mutableStateOf("") }
        
        Dialog(onDismissRequest = { showAddMedDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF070909))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text("NOVO MEDICAMENTO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("NOME", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = medName,
                        onValueChange = { medName = it },
                        placeholder = { Text("Ex: Vitamina C", color = Color(0xFF55605E)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF131817),
                            unfocusedContainerColor = Color(0xFF131817),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("DOSAGEM / DESCRIÇÃO", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = medDosage,
                        onValueChange = { medDosage = it },
                        placeholder = { Text("Ex: 1 cápsula - 500mg", color = Color(0xFF55605E)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF131817),
                            unfocusedContainerColor = Color(0xFF131817),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("HORÁRIO", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = medTime,
                        onValueChange = { medTime = it },
                        placeholder = { Text("Ex: 08:00", color = Color(0xFF55605E)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF131817),
                            unfocusedContainerColor = Color(0xFF131817),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddMedDialog = false }) {
                            Text("CANCELAR", color = Color(0xFF879391), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (medName.isNotBlank() && medTime.isNotBlank()) {
                                    val newId = (medicines.maxOfOrNull { it.id } ?: 0) + 1
                                    medicines = medicines + Medicine(newId, medName, medDosage, medTime, false)
                                }
                                showAddMedDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black)
                        ) {
                            Text("ADICIONAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saúde",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        color = OnBackgroundDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onHomeClick) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "Home",
                            tint = OnBackgroundDark.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Circular Prontidão Score
            OuraCircularProgress(
                progress = 0.82f,
                progressColor = PrimaryTeal,
                modifier = Modifier.size(200.dp),
                strokeWidth = 10f
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "82",
                        fontFamily = FontFamily.Serif,
                        fontSize = 72.sp,
                        color = OnBackgroundDark,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 72.sp
                    )
                    Text(
                        text = "PRONTIDÃO",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnBackgroundDark.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Cardiovascular Health
            Column(
                modifier = PremiumGlassModifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                SectionHeader("SAÚDE CARDIOVASCULAR", Icons.Outlined.FavoriteBorder)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("65", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = OnBackgroundDark)
                            Text(" bpm", fontSize = 14.sp, color = OnBackgroundDark.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text("Frequência Média", fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                    }
                    
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("42", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = OnBackgroundDark)
                            Text(" ms", fontSize = 14.sp, color = OnBackgroundDark.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text("Variabilidade\n(HRV)", fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.7f), lineHeight = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Simple placeholder for the wave chart
                Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.BottomCenter) {
                     Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TertiaryPurple.copy(alpha = 0.5f)))
                     // Just an illustrative curvy line
                     Box(modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(0.3f).height(2.dp).background(TertiaryPurple).align(Alignment.BottomStart))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sleep
            Column(
                modifier = PremiumGlassModifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                SectionHeader("SONO", Icons.Outlined.Bedtime)
                
                Text("8h 16m", fontFamily = FontFamily.Serif, fontSize = 36.sp, color = OnBackgroundDark)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Sleep stages bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Acordado", fontSize = 10.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                    Text("REM", fontSize = 10.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                    Text("Leve", fontSize = 10.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                    Text("Profundo", fontSize = 10.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                    Box(modifier = Modifier.weight(0.1f).fillMaxHeight().background(SecondaryGold))
                    Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(TertiaryPurple))
                    Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(PrimaryTeal.copy(alpha = 0.5f)))
                    Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(PrimaryTeal))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Body Metrics (INCORPORATING WEIGHT REGISTRATION)
            Column(
                modifier = PremiumGlassModifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MÉTRICAS CORPORAIS",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnBackgroundDark.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "REGISTRAR",
                        color = PrimaryTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.clickable { showWeightDialog = true }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Peso Atual", fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(String.format(Locale.US, "%.1f", weight), fontFamily = FontFamily.Serif, fontSize = 28.sp, color = OnBackgroundDark)
                            Text(" kg", fontSize = 14.sp, color = OnBackgroundDark.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                    
                    Column {
                        Text("Variação Temp.", fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("+1.2", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = SecondaryGold)
                            Text(" °C", fontSize = 14.sp, color = OnBackgroundDark.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // CONTROLE DE MEDICAMENTOS (NEW PREMIUM COMPONENT)
            Column(
                modifier = PremiumGlassModifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONTROLE DE REMÉDIOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnBackgroundDark.copy(alpha = 0.7f)
                    )
                    IconButton(
                        onClick = { showAddMedDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar remédio",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                Spacer(modifier = Modifier.height(16.dp))
                
                if (medicines.isEmpty()) {
                    Text("Nenhum remédio registrado hoje", color = OnBackgroundDark.copy(alpha = 0.5f), fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        medicines.forEach { med ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        medicines = medicines.map {
                                            if (it.id == med.id) it.copy(taken = !it.taken) else it
                                        }
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (med.taken) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (med.taken) PrimaryTeal else OnBackgroundDark.copy(alpha = 0.4f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = med.name,
                                            color = if (med.taken) OnBackgroundDark.copy(alpha = 0.6f) else OnBackgroundDark,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            textDecoration = if (med.taken) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = med.dosage,
                                            color = OnBackgroundDark.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccessTime,
                                        contentDescription = null,
                                        tint = OnBackgroundDark.copy(alpha = 0.4f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = med.time,
                                        color = if (med.taken) OnBackgroundDark.copy(alpha = 0.5f) else PrimaryTeal,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
