package com.example

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.ui.geometry.Size
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

// Helper to determine if a timestamp belongs to today in local time
private fun isSameDayLocal(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
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
        stepsRecords.filter { isSameDayLocal(it.endTime, System.currentTimeMillis()) }.sumOf { it.count }
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

    val requiredReadPermissions = setOf(
        HealthPermission.getReadPermission(HCWeightRecord::class),
        HealthPermission.getReadPermission(HCStepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class)
    )

    // Check system permissions on start. If they are already granted, enable Health Connect and trigger sync.
    LaunchedEffect(healthProfile) {
        try {
            val providerPackageName = "com.google.android.apps.healthdata"
            val availabilityStatus = HealthConnectClient.getSdkStatus(context, providerPackageName)
            if (availabilityStatus == HealthConnectClient.SDK_AVAILABLE) {
                val client = HealthConnectClient.getOrCreate(context)
                val granted = client.permissionController.getGrantedPermissions()
                // Se o usuário já concedeu as permissões de leitura essenciais, ativamos silenciosamente
                if (granted.containsAll(requiredReadPermissions)) {
                    if (healthProfile != null && healthProfile?.isHealthConnectEnabled != true) {
                        viewModel.updateHealthProfile(
                            heightCm = healthProfile?.heightCm ?: 0.0,
                            targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                            isHealthConnectEnabled = true
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

    val requestPermissions = rememberLauncherForActivityResult(requestPermissionActivityContract) { granted ->
        if (granted.containsAll(requiredReadPermissions) || granted.isNotEmpty()) {
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
                
                // Busca altura com janela ampla (5 anos) para garantir resgate do dado histórico
                val heightStart = end.minus(365 * 5, ChronoUnit.DAYS)
                val hcHeights = healthConnectManager.readHeightRecords(heightStart, end)
                val latestHeight = hcHeights.maxByOrNull { it.time }?.height?.inMeters?.times(100)
                
                viewModel.updateHealthProfile(
                    heightCm = latestHeight ?: healthProfile?.heightCm ?: 0.0,
                    targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                    isHealthConnectEnabled = true
                )
            }
        }
    }

    // Auto-sync in the background if Health Connect is already enabled (including height synchronization)
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

                // Sync height record in background as well (5 years window)
                val heightStart = end.minus(365 * 5, ChronoUnit.DAYS)
                val hcHeights = healthConnectManager.readHeightRecords(heightStart, end)
                val latestHeight = hcHeights.maxByOrNull { it.time }?.height?.inMeters?.times(100)
                if (latestHeight != null && latestHeight != healthProfile?.heightCm) {
                    viewModel.updateHealthProfile(
                        heightCm = latestHeight,
                        targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                        isHealthConnectEnabled = true
                    )
                }
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
            val hasNotificationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
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
        var recurrence by remember { mutableStateOf("DAILY") }
        var selectedDays by remember { mutableStateOf(setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)) }
        
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F1618), // Soft dark teal/slate top
                                Color(0xFF070909)  // Rich black base matching HomeScreen
                            )
                        )
                    )
            ) {
                // Ambient glows
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PrimaryTeal.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = Offset(800f, 200f),
                                radius = 1100f
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    TertiaryPurple.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(-100f, 1300f),
                                radius = 1100f
                            )
                        )
                )

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
                        contentPadding = PaddingValues(bottom = 140.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                        
                        // IMC & Weight Card
                        item {
                            AnimatedCardContainer(delayMillis = 100) {
                                BmiCard(healthProfile, weightRecords.lastOrNull())
                            }
                        }

                        // Health Connect Connect Banner (display if healthProfile is null or disabled)
                        if (healthProfile == null || healthProfile?.isHealthConnectEnabled != true) {
                            item {
                                AnimatedCardContainer(delayMillis = 150) {
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
                        }

                        // Steps Card
                        item {
                            AnimatedCardContainer(delayMillis = 200) {
                                StepsCard(todaySteps) { showStepsDialog = true }
                            }
                        }

                        // Weight Chart Card
                        item {
                            AnimatedCardContainer(delayMillis = 300) {
                                WeightChartCard(weightRecords, healthProfile?.targetWeightKg) { showWeightDialog = true }
                            }
                        }

                        // Sleep Card
                        item {
                            AnimatedCardContainer(delayMillis = 400) {
                                SleepCard(sleepRecords.firstOrNull()) { showSleepDialog = true }
                            }
                        }

                        // Medications Card
                        item {
                            AnimatedCardContainer(delayMillis = 500) {
                                Column(modifier = PremiumGlassModifier.fillMaxWidth().padding(24.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("CONTROLE DE REMÉDIOS", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f), letterSpacing = 1.sp)
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
        bmi < 24.9 -> "Peso Saudável" to PrimaryTeal
        bmi < 29.9 -> "Sobrepeso" to SecondaryGold
        else -> "Obesidade" to Color(0xFFFF5252)
    }

    val bmiPosition = ((bmi.toFloat() - 15f) / 20f).coerceIn(0f, 1f)
    val animatedPosition = remember { Animatable(0f) }
    LaunchedEffect(bmi) {
        if (bmi > 0) {
            animatedPosition.animateTo(bmiPosition, animationSpec = tween(1500, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("COMPOSIÇÃO CORPORAL", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(if (bmi > 0) String.format(Locale.getDefault(), "%.1f", bmi) else "--", fontFamily = FontFamily.Serif, fontSize = 42.sp, color = OnBackgroundDark, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("IMC", fontSize = 16.sp, color = OnBackgroundDark.copy(alpha = 0.6f), modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text("STATUS", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bmiColor.copy(alpha = 0.15f))
                            .border(0.5.dp, bmiColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(bmiStatus, color = bmiColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Histórico de peso rápido / altura
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Altura", fontSize = 11.sp, color = OnBackgroundDark.copy(alpha = 0.6f))
                    Text(if (heightM > 0) "${profile?.heightCm?.toInt()} cm" else "--", color = OnBackgroundDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Peso Registrado", fontSize = 11.sp, color = OnBackgroundDark.copy(alpha = 0.6f))
                    Text(if (weightKg > 0) "${String.format(Locale.getDefault(), "%.1f", weightKg)} kg" else "--", color = OnBackgroundDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Meta", fontSize = 11.sp, color = OnBackgroundDark.copy(alpha = 0.6f))
                    Text(if (profile?.targetWeightKg != null && profile.targetWeightKg > 0) "${profile.targetWeightKg.toInt()} kg" else "--", color = OnBackgroundDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Espectro visual do IMC
            if (bmi > 0) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    // Barra do Espectro
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF03A9F4), // Abaixo do peso
                                        Color(0xFF71D7CD), // Normal
                                        Color(0xFFF9A826), // Sobrepeso
                                        Color(0xFFFF5252)  // Obesidade
                                    )
                                )
                            )
                    )
                    
                    // Indicador móvel (agulha)
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val indicatorOffset = maxWidth * animatedPosition.value - 6.dp
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, bmiColor, CircleShape)
                        )
                    }
                }
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
    val targetSteps = 10000f
    val progress = (stepsCount.toFloat() / targetSteps).coerceIn(0f, 1f)
    
    // Animação do arco
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(stepsCount) {
        animatedProgress.animateTo(progress, animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.DirectionsWalk, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                    Text("PASSOS DIÁRIOS", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f), letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("$stepsCount", fontFamily = FontFamily.Serif, fontSize = 42.sp, color = OnBackgroundDark, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Meta de 10.000 passos", fontSize = 13.sp, color = OnBackgroundDark.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "REGISTRAR", 
                    color = PrimaryTeal, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.clickable { onRegisterClick() }
                )
            }
            
            // Oura Ring style Progress Circle
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 7.dp.toPx()
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = (size.width - strokeWidth) / 2
                    
                    // Background Ring
                    drawArc(
                        color = PrimaryTeal.copy(alpha = 0.12f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Outer glow layer 1 (Wide, low opacity)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(PrimaryTeal, Color(0xFF4D96FF), TertiaryPurple, PrimaryTeal)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress.value,
                        useCenter = false,
                        style = Stroke(width = strokeWidth + 6.dp.toPx(), cap = StrokeCap.Round),
                        alpha = 0.15f
                    )
                    
                    // Outer glow layer 2 (Medium, medium opacity)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(PrimaryTeal, Color(0xFF4D96FF), TertiaryPurple, PrimaryTeal)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress.value,
                        useCenter = false,
                        style = Stroke(width = strokeWidth + 3.dp.toPx(), cap = StrokeCap.Round),
                        alpha = 0.35f
                    )
                    
                    // Foreground Ring com degradê Oura Ring
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(PrimaryTeal, Color(0xFF4D96FF), TertiaryPurple, PrimaryTeal)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress.value,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Glowing cursor dot at the end of progress
                    if (animatedProgress.value > 0.01f) {
                        val angle = -90f + 360f * animatedProgress.value
                        val angleRad = Math.toRadians(angle.toDouble())
                        val endX = center.x + radius * Math.cos(angleRad).toFloat()
                        val endY = center.y + radius * Math.sin(angleRad).toFloat()
                        
                        // Cursor glow outer
                        drawCircle(
                            color = Color(0xFF4D96FF),
                            radius = 6.dp.toPx(),
                            center = Offset(endX, endY),
                            alpha = 0.6f
                        )
                        // Cursor core white
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(endX, endY)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackgroundDark,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "meta",
                        fontSize = 9.sp,
                        color = OnBackgroundDark.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
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

                // Área do gráfico preenchida com degradê
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PrimaryTeal.copy(alpha = 0.25f * animationProgress.value),
                            Color.Transparent
                        )
                    )
                )

                // Desenha a linha de peso
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Bedtime, null, tint = TertiaryPurple, modifier = Modifier.size(18.dp))
                    Text("SONO E DESCANSO", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f), letterSpacing = 1.sp)
                }
                Text("REGISTRAR", color = PrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onRegisterClick() })
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (latestSleep != null) {
                        val hours = latestSleep.durationHours.toInt()
                        val minutes = ((latestSleep.durationHours - hours) * 60).roundToInt()
                        Text("${hours}h ${minutes}m", fontFamily = FontFamily.Serif, fontSize = 42.sp, color = OnBackgroundDark, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Última noite sincronizada", fontSize = 13.sp, color = OnBackgroundDark.copy(alpha = 0.6f))
                    } else {
                        Text("Sem Dados", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = OnBackgroundDark.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sincronize com o Health Connect", fontSize = 13.sp, color = OnBackgroundDark.copy(alpha = 0.6f))
                    }
                }
                
                // Desenhar lua crescente estilizada no Canvas
                Canvas(modifier = Modifier.size(64.dp)) {
                    val radius = size.width / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    
                    val path = Path().apply {
                        addArc(
                            oval = androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                            startAngleDegrees = -90f,
                            sweepAngleDegrees = 180f
                        )
                        quadraticTo(
                            x1 = center.x + radius * 0.1f,
                            y1 = center.y,
                            x2 = center.x,
                            y2 = center.y - radius
                        )
                    }
                    drawPath(path = path, color = TertiaryPurple)
                    
                    drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 2.dp.toPx(), center = Offset(center.x - radius * 0.5f, center.y - radius * 0.4f))
                    drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 1.5.dp.toPx(), center = Offset(center.x + radius * 0.3f, center.y + radius * 0.5f))
                }
            }
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

@Composable
fun AnimatedCardContainer(
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + 
                androidx.compose.animation.slideInVertically(initialOffsetY = { 40 }, animationSpec = androidx.compose.animation.core.tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing)),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}
