package com.example

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord as HCStepsRecord
import androidx.health.connect.client.records.WeightRecord as HCWeightRecord
import com.example.data.HealthProfile
import com.example.data.Medication
import com.example.data.SleepRecord
import com.example.data.StepsRecord
import com.example.data.WeightRecord
import com.example.health.HealthConnectManager
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

// Helper function to get start of today
private fun getStartOfToday(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// Helper function to get end of today
private fun getEndOfToday(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

private fun parseDoubleSanitized(input: String): Double? {
    val normalized = input.replace(",", ".")
    val regex = """[+-]?([0-9]*[.])?[0-9]+""".toRegex()
    val match = regex.find(normalized)
    return match?.value?.toDoubleOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(viewModel: TesseraViewModel, onHomeClick: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val healthConnectManager = remember { HealthConnectManager(context) }

    val healthProfile by viewModel.healthProfile.collectAsState(initial = null)
    val medications by viewModel.allMedications.collectAsState(initial = emptyList())
    val weightRecords by viewModel.allWeightRecords.collectAsState(initial = emptyList())
    val sleepRecords by viewModel.allSleepRecords.collectAsState(initial = emptyList())
    val stepsRecords by viewModel.allStepsRecords.collectAsState(initial = emptyList())

    val todaySteps = remember(stepsRecords) {
        val todayStart = getStartOfToday()
        val todayEnd = getEndOfToday()
        stepsRecords.filter { it.startTime >= todayStart && it.endTime <= todayEnd }.sumOf { it.count }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(HCWeightRecord::class),
        HealthPermission.getWritePermission(HCWeightRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HCStepsRecord::class),
        HealthPermission.getWritePermission(HCStepsRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class)
    )

    val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

    val requestPermissions = rememberLauncherForActivityResult(requestPermissionActivityContract) { granted ->
        if (granted.containsAll(permissions)) {
            coroutineScope.launch {
                val end = Instant.now()
                val start = end.minus(30, ChronoUnit.DAYS)
                val hcWeights = healthConnectManager.readWeightRecords(start, end)
                val hcSleeps = healthConnectManager.readSleepRecords(start, end)
                val hcSteps = healthConnectManager.readStepsRecords(start, end)
                
                val localWeights = hcWeights.map { WeightRecord(weightKg = it.weight.inKilograms, timestamp = it.time.toEpochMilli(), source = "Health Connect") }
                val localSleeps = hcSleeps.map { 
                    val duration = ChronoUnit.MINUTES.between(it.startTime, it.endTime).toDouble() / 60.0
                    SleepRecord(startTime = it.startTime.toEpochMilli(), endTime = it.endTime.toEpochMilli(), durationHours = duration, source = "Health Connect") 
                }
                val localSteps = hcSteps.map {
                    StepsRecord(count = it.count, startTime = it.startTime.toEpochMilli(), endTime = it.endTime.toEpochMilli(), source = "Health Connect")
                }
                viewModel.syncHealthConnectData(localWeights, localSleeps, localSteps)
                
                val hcHeights = healthConnectManager.readHeightRecords(start, end)
                val latestHeight = hcHeights.maxByOrNull { it.time }?.height?.inMeters?.times(100)
                
                viewModel.updateHealthProfile(
                    heightCm = latestHeight ?: healthProfile?.heightCm ?: 0.0,
                    targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                    isHealthConnectEnabled = true
                )
            }
        }
    }

    // Auto-sync in the background if Health Connect is already enabled
    LaunchedEffect(healthProfile?.isHealthConnectEnabled) {
        if (healthProfile?.isHealthConnectEnabled == true) {
            try {
                val end = Instant.now()
                val start = end.minus(30, ChronoUnit.DAYS)
                val hcWeights = healthConnectManager.readWeightRecords(start, end)
                val hcSleeps = healthConnectManager.readSleepRecords(start, end)
                val hcSteps = healthConnectManager.readStepsRecords(start, end)
                
                val localWeights = hcWeights.map { WeightRecord(weightKg = it.weight.inKilograms, timestamp = it.time.toEpochMilli(), source = "Health Connect") }
                val localSleeps = hcSleeps.map { 
                    val duration = ChronoUnit.MINUTES.between(it.startTime, it.endTime).toDouble() / 60.0
                    SleepRecord(startTime = it.startTime.toEpochMilli(), endTime = it.endTime.toEpochMilli(), durationHours = duration, source = "Health Connect") 
                }
                val localSteps = hcSteps.map {
                    StepsRecord(count = it.count, startTime = it.startTime.toEpochMilli(), endTime = it.endTime.toEpochMilli(), source = "Health Connect")
                }
                viewModel.syncHealthConnectData(localWeights, localSleeps, localSteps)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { /* Permissão concedida ou negada */ }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showWeightDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showStepsDialog by remember { mutableStateOf(false) }
    var showMedicationDialog by remember { mutableStateOf(false) }

    // Dialog: Registrar Peso / Dados de Perfil
    if (showWeightDialog) {
        var inputWeight by remember { mutableStateOf("") }
        var inputHeight by remember { mutableStateOf(healthProfile?.heightCm?.toString() ?: "") }
        var inputTarget by remember { mutableStateOf(healthProfile?.targetWeightKg?.toString() ?: "") }
        
        Dialog(onDismissRequest = { showWeightDialog = false }) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF070909)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(24.dp)) {
                Column {
                    Text("REGISTRAR DADOS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("PESO (KG)", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TextField(value = inputWeight, onValueChange = { inputWeight = it }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF131817), unfocusedContainerColor = Color(0xFF131817), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().padding(top = 4.dp), shape = RoundedCornerShape(12.dp))
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ALTURA (CM)", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TextField(value = inputHeight, onValueChange = { inputHeight = it }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF131817), unfocusedContainerColor = Color(0xFF131817), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().padding(top = 4.dp), shape = RoundedCornerShape(12.dp))
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("META DE PESO (KG)", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TextField(value = inputTarget, onValueChange = { inputTarget = it }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF131817), unfocusedContainerColor = Color(0xFF131817), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().padding(top = 4.dp), shape = RoundedCornerShape(12.dp))

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showWeightDialog = false }) { Text("CANCELAR", color = Color(0xFF879391)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val w = parseDoubleSanitized(inputWeight)
                            val parsedHeight = parseDoubleSanitized(inputHeight)
                            val h = if (parsedHeight != null) {
                                if (parsedHeight < 3.0) parsedHeight * 100.0 else parsedHeight
                            } else {
                                healthProfile?.heightCm ?: 0.0
                            }
                            val t = parseDoubleSanitized(inputTarget) ?: healthProfile?.targetWeightKg ?: 0.0
                            if (w != null) {
                                viewModel.addManualWeightRecord(w)
                                if (healthProfile?.isHealthConnectEnabled == true) {
                                    coroutineScope.launch {
                                        healthConnectManager.writeWeightRecord(w, System.currentTimeMillis())
                                    }
                                }
                            }
                            viewModel.updateHealthProfile(heightCm = h, targetWeightKg = t, isHealthConnectEnabled = healthProfile?.isHealthConnectEnabled ?: false)
                            showWeightDialog = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black)) { Text("SALVAR") }
                    }
                }
            }
        }
    }

    // Dialog: Registrar Sono
    if (showSleepDialog) {
        var inputHours by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showSleepDialog = false }) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF070909)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(24.dp)) {
                Column {
                    Text("REGISTRAR SONO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("HORAS DE SONO", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = inputHours,
                        onValueChange = { inputHours = it },
                        placeholder = { Text("Ex: 8.0", color = Color(0xFF55605E)) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF131817), unfocusedContainerColor = Color(0xFF131817), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSleepDialog = false }) { Text("CANCELAR", color = Color(0xFF879391)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val hours = inputHours.toDoubleOrNull()
                                if (hours != null) {
                                    val now = System.currentTimeMillis()
                                    val startTime = now - (hours * 3600000).toLong()
                                    viewModel.addManualSleepRecord(startTime, now, hours)
                                    if (healthProfile?.isHealthConnectEnabled == true) {
                                        coroutineScope.launch {
                                            healthConnectManager.writeSleepRecord(startTime, now)
                                        }
                                    }
                                }
                                showSleepDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black)
                        ) {
                            Text("SALVAR")
                        }
                    }
                }
            }
        }
    }

    // Dialog: Registrar Passos
    if (showStepsDialog) {
        var inputSteps by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showStepsDialog = false }) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF070909)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(24.dp)) {
                Column {
                    Text("REGISTRAR PASSOS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("QUANTIDADE DE PASSOS", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TextField(
                        value = inputSteps,
                        onValueChange = { inputSteps = it },
                        placeholder = { Text("Ex: 8500", color = Color(0xFF55605E)) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF131817), unfocusedContainerColor = Color(0xFF131817), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showStepsDialog = false }) { Text("CANCELAR", color = Color(0xFF879391)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val count = inputSteps.toLongOrNull()
                                if (count != null) {
                                    val now = System.currentTimeMillis()
                                    viewModel.addManualStepsRecord(count, now - 3600000, now) // Assume 1 hora de caminhada
                                    if (healthProfile?.isHealthConnectEnabled == true) {
                                        coroutineScope.launch {
                                            healthConnectManager.writeStepsRecord(count, now - 3600000, now)
                                        }
                                    }
                                }
                                showStepsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black)
                        ) {
                            Text("SALVAR")
                        }
                    }
                }
            }
        }
    }

    // Dialog: Novo Medicamento
    if (showMedicationDialog) {
        var name by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("08:00") }
        var dosage by remember { mutableStateOf("") }
        var recurrence by remember { mutableStateOf("DAILY") } // "DAILY", "ALTERNATE" ou "SPECIFIC"
        var selectedDays by remember { mutableStateOf(setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)) }
        
        // Native TimePickerDialog setup
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )

        Dialog(onDismissRequest = { showMedicationDialog = false }) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF070909)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(24.dp)) {
                Column {
                    Text("NOVO MEDICAMENTO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("NOME", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TextField(value = name, onValueChange = { name = it }, placeholder = { Text("Ex: Vitamina C", color = Color(0xFF55605E)) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF131817), unfocusedContainerColor = Color(0xFF131817), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().padding(top = 4.dp), shape = RoundedCornerShape(12.dp))
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("DOSAGEM", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TextField(value = dosage, onValueChange = { dosage = it }, placeholder = { Text("Ex: 1 cápsula", color = Color(0xFF55605E)) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF131817), unfocusedContainerColor = Color(0xFF131817), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().padding(top = 4.dp), shape = RoundedCornerShape(12.dp))
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("HORÁRIO", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clickable { timePickerDialog.show() }
                    ) {
                        OutlinedTextField(
                            value = time,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.White,
                                disabledBorderColor = Color.Transparent,
                                disabledContainerColor = Color(0xFF131817)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                Icon(Icons.Outlined.AccessTime, contentDescription = "Select Time", tint = PrimaryTeal)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("RECORRÊNCIA", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (recurrence == "DAILY") PrimaryTeal.copy(alpha = 0.2f) else Color(0xFF131817))
                                .border(
                                    width = 1.dp,
                                    color = if (recurrence == "DAILY") PrimaryTeal else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { recurrence = "DAILY" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Diário", color = if (recurrence == "DAILY") Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (recurrence == "ALTERNATE") PrimaryTeal.copy(alpha = 0.2f) else Color(0xFF131817))
                                .border(
                                    width = 1.dp,
                                    color = if (recurrence == "ALTERNATE") PrimaryTeal else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { recurrence = "ALTERNATE" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Alternado", color = if (recurrence == "ALTERNATE") Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (recurrence == "SPECIFIC") PrimaryTeal.copy(alpha = 0.2f) else Color(0xFF131817))
                                .border(
                                    width = 1.dp,
                                    color = if (recurrence == "SPECIFIC") PrimaryTeal else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { recurrence = "SPECIFIC" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Personalizado", color = if (recurrence == "SPECIFIC") Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    if (recurrence == "SPECIFIC") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("DIAS DA SEMANA", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val weekDays = listOf(
                                Calendar.SUNDAY to "D",
                                Calendar.MONDAY to "S",
                                Calendar.TUESDAY to "T",
                                Calendar.WEDNESDAY to "Q",
                                Calendar.THURSDAY to "Q",
                                Calendar.FRIDAY to "S",
                                Calendar.SATURDAY to "S"
                            )
                            weekDays.forEach { (dayId, label) ->
                                val isSelected = selectedDays.contains(dayId)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) PrimaryTeal.copy(alpha = 0.2f) else Color(0xFF131817))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) PrimaryTeal else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedDays = if (isSelected) {
                                                selectedDays - dayId
                                            } else {
                                                selectedDays + dayId
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showMedicationDialog = false }) { Text("CANCELAR", color = Color(0xFF879391)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (name.isNotBlank() && time.isNotBlank()) {
                                val savedRecurrence = if (recurrence == "SPECIFIC") {
                                    selectedDays.sorted().joinToString(",")
                                } else {
                                    recurrence
                                }
                                viewModel.addMedication(name, time, dosage, "#FF4081", savedRecurrence)
                                com.example.notifications.AlarmScheduler.scheduleMedicationAlarm(context, name, dosage, time)
                            }
                            showMedicationDialog = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black)) { Text("ADICIONAR") }
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
                title = { Text("Saúde", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = OnBackgroundDark) },
                navigationIcon = { IconButton(onClick = onHomeClick) { Icon(Icons.Outlined.Home, "Home", tint = OnBackgroundDark.copy(alpha = 0.7f)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 140.dp) // Adjusted for FAB/Navigation bottom padding
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // IMC & Weight Card
            item {
                BmiCard(healthProfile, weightRecords.lastOrNull())
            }

            // Health Connect Connect Banner
            if (healthProfile?.isHealthConnectEnabled != true) {
                item {
                    HealthConnectBanner { 
                        coroutineScope.launch {
                            val providerPackageName = "com.google.android.apps.healthdata"
                            val availabilityStatus = HealthConnectClient.getSdkStatus(context, providerPackageName)
                            
                            if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE || availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                                android.widget.Toast.makeText(context, "O app 'Saúde Connect' da Google não está instalado ou precisa de atualização. Redirecionando...", android.widget.Toast.LENGTH_LONG).show()
                                val uriString = "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriString)))
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Não foi possível abrir a Play Store.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }
                            
                            try {
                                val client = HealthConnectClient.getOrCreate(context)
                                val granted = client.permissionController.getGrantedPermissions()
                                if (granted.containsAll(permissions)) {
                                    android.widget.Toast.makeText(context, "Permissões já concedidas. Habilitando sincronização...", android.widget.Toast.LENGTH_SHORT).show()
                                    viewModel.updateHealthProfile(
                                        heightCm = healthProfile?.heightCm ?: 0.0,
                                        targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                                        isHealthConnectEnabled = true
                                    )
                                } else {
                                    requestPermissions.launch(permissions)
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Erro ao abrir permissões: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }

            // Steps Card (New!)
            item {
                StepsCard(todaySteps) { showStepsDialog = true }
            }

            // Weight Chart Card
            item {
                WeightChartCard(weightRecords, healthProfile?.targetWeightKg) { showWeightDialog = true }
            }

            // Sleep Card
            item {
                SleepCard(sleepRecords.firstOrNull()) { showSleepDialog = true }
            }

            // Medications Card
            item {
                Column(modifier = PremiumGlassModifier.fillMaxWidth().padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("CONTROLE DE REMÉDIOS", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f))
                        IconButton(onClick = { showMedicationDialog = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Add, "Adicionar", tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (medications.isEmpty()) {
                        Text("Nenhum remédio registrado", color = OnBackgroundDark.copy(alpha = 0.5f), fontSize = 14.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            medications.forEach { med ->
                                MedicationItem(med) { viewModel.toggleMedicationTaken(med) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BmiCard(profile: HealthProfile?, latestWeight: WeightRecord?) {
    val heightM = (profile?.heightCm ?: 0.0) / 100.0
    val weightKg = latestWeight?.weightKg ?: 0.0
    val bmi = if (heightM > 0 && weightKg > 0) weightKg / (heightM * heightM) else 0.0

    val (bmiStatus, bmiColor) = when {
        bmi == 0.0 -> "Sem Dados" to Color.Gray
        bmi < 18.5 -> "Abaixo do Peso" to Color(0xFF03A9F4)
        bmi < 24.9 -> "Peso Normal" to PrimaryTeal
        bmi < 29.9 -> "Sobrepeso" to SecondaryGold
        else -> "Obesidade" to Color(0xFFF44336)
    }

    Column(modifier = PremiumGlassModifier.fillMaxWidth().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Seu IMC", color = OnBackgroundDark.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(if (bmi > 0) String.format("%.1f", bmi) else "--", fontFamily = FontFamily.Serif, fontSize = 36.sp, color = OnBackgroundDark)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(bmiColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(bmiStatus, color = bmiColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Peso Atual", color = OnBackgroundDark.copy(alpha = 0.7f), fontSize = 12.sp)
                Text(if (weightKg > 0) "${String.format("%.1f", weightKg)} kg" else "--", color = OnBackgroundDark, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Altura", color = OnBackgroundDark.copy(alpha = 0.7f), fontSize = 12.sp)
                Text(if (heightM > 0) "${profile?.heightCm?.toInt()} cm" else "--", color = OnBackgroundDark, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun HealthConnectBanner(onClick: () -> Unit) {
    Row(
        modifier = PremiumGlassModifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryTeal.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Sync, "Sync", tint = PrimaryTeal, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Conectar Health Connect", color = OnBackgroundDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Sincronize peso, sono e passos automaticamente", color = OnBackgroundDark.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
fun StepsCard(stepsCount: Long, onRegisterClick: () -> Unit) {
    Column(modifier = PremiumGlassModifier.fillMaxWidth().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.DirectionsWalk, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                Text("PASSOS", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f))
            }
            Text("REGISTRAR", color = PrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onRegisterClick() })
        }
        Text("$stepsCount", fontFamily = FontFamily.Serif, fontSize = 36.sp, color = OnBackgroundDark)
        Text("Passos registrados hoje", fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
    }
}

@Composable
fun WeightChartCard(records: List<WeightRecord>, targetWeight: Double?, onRegisterClick: () -> Unit) {
    Column(modifier = PremiumGlassModifier.fillMaxWidth().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("HISTÓRICO DE PESO", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f))
            Text("REGISTRAR", color = PrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onRegisterClick() })
        }
        
        if (targetWeight != null && targetWeight > 0) {
            Text("Meta: ${String.format("%.1f", targetWeight)} kg", color = SecondaryGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (records.size > 1) {
            val animationProgress = remember { Animatable(0f) }
            LaunchedEffect(records) {
                animationProgress.animateTo(1f, animationSpec = tween(1500))
            }

            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                val maxWeight = records.maxOf { it.weightKg } + 2
                val minWeight = records.minOf { it.weightKg } - 2
                val range = max(maxWeight - minWeight, 1.0)
                
                val width = size.width
                val height = size.height
                val stepX = width / (records.size - 1).coerceAtLeast(1)
                
                val path = Path()
                val points = mutableListOf<Offset>()
                
                records.forEachIndexed { index, record ->
                    val x = index * stepX
                    val normalizedY = 1f - ((record.weightKg - minWeight) / range).toFloat()
                    val y = normalizedY * height
                    val point = Offset(x, y)
                    points.add(point)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                if (targetWeight != null && targetWeight > 0) {
                    val targetY = (1f - ((targetWeight - minWeight) / range).toFloat()) * height
                    if (targetY in 0f..height) {
                        drawLine(color = SecondaryGold.copy(alpha = 0.5f), start = Offset(0f, targetY), end = Offset(width, targetY), strokeWidth = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                    }
                }

                drawPath(path = path, brush = Brush.horizontalGradient(listOf(TertiaryPurple, PrimaryTeal)), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round), alpha = animationProgress.value)
                points.forEach { point ->
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = point, alpha = animationProgress.value)
                    drawCircle(color = PrimaryTeal, radius = 2.dp.toPx(), center = point, alpha = animationProgress.value)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("Adicione mais dados para ver o gráfico.", color = OnBackgroundDark.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SleepCard(latestSleep: SleepRecord?, onRegisterClick: () -> Unit) {
    Column(modifier = PremiumGlassModifier.fillMaxWidth().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Bedtime, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                Text("SONO", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f))
            }
            Text("REGISTRAR", color = PrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onRegisterClick() })
        }
        if (latestSleep != null) {
            val hours = latestSleep.durationHours.toInt()
            val minutes = ((latestSleep.durationHours - hours) * 60).roundToInt()
            Text("${hours}h ${minutes}m", fontFamily = FontFamily.Serif, fontSize = 36.sp, color = OnBackgroundDark)
            Text("Última noite sincronizada", fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
        } else {
            Text("Sem Dados", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = OnBackgroundDark.copy(alpha = 0.5f))
            Text("Conecte ao Health Connect ou registre", fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
        }
    }
}

private fun getRecurrenceDisplayName(recurrence: String): String {
    return when (recurrence) {
        "DAILY" -> "Diário"
        "ALTERNATE" -> "Dias alternados"
        else -> {
            val days = recurrence.split(",").mapNotNull { it.toIntOrNull() }.sorted()
            if (days.isEmpty()) return "Nenhum dia"
            if (days.size == 7) return "Diário"
            val dayNames = mapOf(
                Calendar.SUNDAY to "Dom",
                Calendar.MONDAY to "Seg",
                Calendar.TUESDAY to "Ter",
                Calendar.WEDNESDAY to "Qua",
                Calendar.THURSDAY to "Qui",
                Calendar.FRIDAY to "Sex",
                Calendar.SATURDAY to "Sáb"
            )
            days.map { dayNames[it] ?: "" }.filter { it.isNotEmpty() }.joinToString(", ")
        }
    }
}

@Composable
fun MedicationItem(med: Medication, onToggle: () -> Unit) {
    val recurrenceText = getRecurrenceDisplayName(med.recurrence)
    val subtitleText = if (med.dosage.isNotBlank()) "${med.dosage} • $recurrenceText" else recurrenceText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = if (med.isTaken) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (med.isTaken) PrimaryTeal else OnBackgroundDark.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = med.name,
                    color = if (med.isTaken) OnBackgroundDark.copy(alpha = 0.6f) else OnBackgroundDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (med.isTaken) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitleText,
                    color = OnBackgroundDark.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Outlined.AccessTime, null, tint = OnBackgroundDark.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
            Text(med.time, color = if (med.isTaken) OnBackgroundDark.copy(alpha = 0.5f) else PrimaryTeal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
