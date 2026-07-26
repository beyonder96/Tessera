package com.example

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.getValue

import android.Manifest
import android.app.DatePickerDialog
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            Log.e("HealthScreen", "Erro ao buscar dados de saúde", e)
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

    // A sincronização automática do Health Connect agora é feita globalmente em MainActivity.kt
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

    LaunchedEffect(Unit) {
        viewModel.healthActionTrigger.collect { action ->
            when (action) {
                TesseraViewModel.HealthAction.ADD_STEPS -> {
                    showStepsDialog = true
                }
                TesseraViewModel.HealthAction.ADD_SLEEP -> {
                    showSleepDialog = true
                }
            }
        }
    }
    
    val listState = rememberLazyListState()
    val isCompact by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100
        }
    }
    val normalAlpha by animateFloatAsState(targetValue = if (isCompact) 0f else 1f, animationSpec = tween(250), label = "normalAlpha")
    val compactAlpha by animateFloatAsState(targetValue = if (isCompact) 1f else 0f, animationSpec = tween(250), label = "compactAlpha")

    val infiniteHeartTransition = rememberInfiniteTransition(label = "HeartPulse")
    val heartScale by infiniteHeartTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeartScale"
    )

    var showFullScreenAlertForMed by remember { mutableStateOf<Medication?>(null) }
    var dismissedMedicationIds by remember { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(medications) {
        val nowCal = Calendar.getInstance()
        val currentMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
        val pendingDueMed = medications.find { med ->
            if (med.isTaken || dismissedMedicationIds.contains(med.id)) return@find false
            val parts = med.time.split(":")
            if (parts.size == 2) {
                val medHour = parts[0].toIntOrNull() ?: 0
                val medMin = parts[1].toIntOrNull() ?: 0
                val medMinutes = medHour * 60 + medMin
                currentMinutes >= medMinutes
            } else false
        }
        if (pendingDueMed != null) {
            showFullScreenAlertForMed = pendingDueMed
        }
    }
    var medicationToDelete by remember { mutableStateOf<Medication?>(null) }

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
        var sleepTimeMs by remember {
            mutableStateOf(
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, 22)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            )
        }
        var wakeTimeMs by remember {
            mutableStateOf(
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 7)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            )
        }

        val sleepDateStr = remember(sleepTimeMs) {
            val cal = Calendar.getInstance().apply { timeInMillis = sleepTimeMs }
            String.format(Locale.getDefault(), "%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        }
        val sleepTimeStr = remember(sleepTimeMs) {
            val cal = Calendar.getInstance().apply { timeInMillis = sleepTimeMs }
            String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }
        val wakeDateStr = remember(wakeTimeMs) {
            val cal = Calendar.getInstance().apply { timeInMillis = wakeTimeMs }
            String.format(Locale.getDefault(), "%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        }
        val wakeTimeStr = remember(wakeTimeMs) {
            val cal = Calendar.getInstance().apply { timeInMillis = wakeTimeMs }
            String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }

        val durationMs = wakeTimeMs - sleepTimeMs
        val durationHours = durationMs.toDouble() / 3600000.0
        val isValid = durationMs > 0
        val durationText = if (isValid) {
            val hours = (durationMs / 3600000L).toInt()
            val minutes = ((durationMs % 3600000L) / 60000L).toInt()
            "Total: ${hours}h ${minutes}m"
        } else {
            "A hora de acordar deve ser após a hora de dormir."
        }

        Dialog(onDismissRequest = { showSleepDialog = false }) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF070909)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(24.dp)) {
                Column {
                    Text("REGISTRAR SONO", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text("DORMIU EM", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131817))
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = sleepTimeMs }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            cal.set(Calendar.YEAR, y)
                                            cal.set(Calendar.MONTH, m)
                                            cal.set(Calendar.DAY_OF_MONTH, d)
                                            sleepTimeMs = cal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CalendarToday, null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(sleepDateStr, color = Color.White, fontSize = 13.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131817))
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = sleepTimeMs }
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            cal.set(Calendar.HOUR_OF_DAY, h)
                                            cal.set(Calendar.MINUTE, min)
                                            sleepTimeMs = cal.timeInMillis
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AccessTime, null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(sleepTimeStr, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("ACORDOU EM", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131817))
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = wakeTimeMs }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            cal.set(Calendar.YEAR, y)
                                            cal.set(Calendar.MONTH, m)
                                            cal.set(Calendar.DAY_OF_MONTH, d)
                                            wakeTimeMs = cal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CalendarToday, null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(wakeDateStr, color = Color.White, fontSize = 13.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131817))
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = wakeTimeMs }
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            cal.set(Calendar.HOUR_OF_DAY, h)
                                            cal.set(Calendar.MINUTE, min)
                                            wakeTimeMs = cal.timeInMillis
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AccessTime, null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(wakeTimeStr, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Real-time duration panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isValid) PrimaryTeal.copy(alpha = 0.08f) else Color(0x15FF5252))
                            .border(1.dp, if (isValid) PrimaryTeal.copy(alpha = 0.2f) else Color(0x33FF5252), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = durationText,
                            color = if (isValid) Color.White else Color(0xFFFF5252),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSleepDialog = false }) { Text("CANCELAR", color = Color(0xFF879391)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isValid) {
                                    viewModel.addManualSleepRecord(sleepTimeMs, wakeTimeMs, durationHours)
                                    if (healthProfile?.isHealthConnectEnabled == true) {
                                        coroutineScope.launch {
                                            healthConnectManager.writeSleepRecord(sleepTimeMs, wakeTimeMs)
                                        }
                                    }
                                    showSleepDialog = false
                                }
                            },
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryTeal,
                                contentColor = Color.Black,
                                disabledContainerColor = Color(0xFF131817),
                                disabledContentColor = Color.Gray
                            )
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
        var stepDateMs by remember {
            mutableStateOf(Calendar.getInstance().timeInMillis)
        }
        val stepDateStr = remember(stepDateMs) {
            val cal = Calendar.getInstance().apply { timeInMillis = stepDateMs }
            String.format(Locale.getDefault(), "%02d/%02d/%04d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        }

        Dialog(onDismissRequest = { showStepsDialog = false }) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF070909)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp)).padding(24.dp)) {
                Column {
                    Text("REGISTRAR PASSOS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text("DATA", color = Color(0xFF879391), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131817))
                            .clickable {
                                val cal = Calendar.getInstance().apply { timeInMillis = stepDateMs }
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        cal.set(Calendar.YEAR, y)
                                        cal.set(Calendar.MONTH, m)
                                        cal.set(Calendar.DAY_OF_MONTH, d)
                                        stepDateMs = cal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarToday, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stepDateStr, color = Color.White, fontSize = 14.sp)
                        }
                    }

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
                                if (count != null && count > 0) {
                                    val cal = Calendar.getInstance().apply { timeInMillis = stepDateMs }
                                    cal.set(Calendar.HOUR_OF_DAY, 12)
                                    cal.set(Calendar.MINUTE, 0)
                                    cal.set(Calendar.SECOND, 0)
                                    val now = cal.timeInMillis
                                    viewModel.addManualStepsRecord(count, now - 3600000, now) // Assume 1 hora de caminhada no meio do dia
                                    if (healthProfile?.isHealthConnectEnabled == true) {
                                        coroutineScope.launch {
                                            healthConnectManager.writeStepsRecord(count, now - 3600000, now)
                                        }
                                    }
                                    showStepsDialog = false
                                }
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

    if (medicationToDelete != null) {
        AlertDialog(
            onDismissRequest = { medicationToDelete = null },
            title = { Text("Excluir Medicamento", fontFamily = FontFamily.Serif, color = OnBackgroundDark) },
            text = { Text("Deseja realmente excluir o medicamento \"${medicationToDelete?.name}\"?", color = OnBackgroundDark.copy(alpha = 0.8f)) },
            confirmButton = {
                TextButton(onClick = {
                    medicationToDelete?.let { 
                        viewModel.deleteMedication(it)
                        com.example.notifications.AlarmScheduler.cancelMedicationAlarm(context, it.name)
                    }
                    medicationToDelete = null
                }) {
                    Text("EXCLUIR", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { medicationToDelete = null }) {
                    Text("CANCELAR", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF131817),
            titleContentColor = OnBackgroundDark,
            textContentColor = OnBackgroundDark.copy(alpha = 0.8f)
        )
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
                    topBar = {}
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 96.dp, bottom = 140.dp)
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

                        // Steps Chart Card
                        item {
                            AnimatedCardContainer(delayMillis = 250) {
                                StepsChartCard(stepsRecords)
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

                        // Sleep Chart Card
                        item {
                            AnimatedCardContainer(delayMillis = 450) {
                                SleepChartCard(sleepRecords)
                            }
                        }

                        // Medications Card
                        item {
                            AnimatedCardContainer(delayMillis = 500) {
                                Column(modifier = PremiumGlassModifier.fillMaxWidth().padding(24.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("CONTROLE DE REMÉDIOS", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f), letterSpacing = 1.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            if (medications.isNotEmpty()) {
                                                Text(
                                                    text = "TESTAR POP-UP",
                                                    color = SecondaryGold,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.clickable {
                                                        showFullScreenAlertForMed = medications.firstOrNull()
                                                    }
                                                )
                                            }
                                            IconButton(onClick = { showMedicationDialog = true }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Add, "Adicionar", tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    if (medications.isEmpty()) {
                                        Text("Nenhum remédio registrado", color = OnBackgroundDark.copy(alpha = 0.5f), fontSize = 14.sp)
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            medications.forEach { med ->
                                                MedicationItem(
                                                    med = med,
                                                    onToggle = { viewModel.toggleMedicationTaken(med) },
                                                    onDeleteClick = { medicationToDelete = med }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                        
                        // Custom Floating Glass Header Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            // 1. Barra Normal
                            if (normalAlpha > 0.05f) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            alpha = normalAlpha
                                            scaleX = 0.92f + (normalAlpha * 0.08f)
                                            scaleY = 0.92f + (normalAlpha * 0.08f)
                                        },
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Saúde",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 28.sp,
                                        color = OnBackgroundDark
                                    )
                                }
                            }

                            // 2. Barra Compacta
                            if (compactAlpha > 0.05f) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .graphicsLayer {
                                            alpha = compactAlpha
                                            translationY = (1f - compactAlpha) * (-20f)
                                        }
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .border(1.dp, PrimaryTeal.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .graphicsLayer(
                                                scaleX = heartScale,
                                                scaleY = heartScale
                                            )
                                    )
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                                    val shimmerOffset by infiniteTransition.animateFloat(
                                        initialValue = -400f,
                                        targetValue = 400f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(2000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "shimmerOffset"
                                    )
                                    
                                    val nameGlowBrush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White,
                                            PrimaryTeal,
                                            Color.White,
                                            PrimaryTeal,
                                            Color.White
                                        ),
                                        start = Offset(shimmerOffset, 0f),
                                        end = Offset(shimmerOffset + 150f, 150f)
                                    )
                                    
                                    Text(
                                        text = "SAÚDE",
                                        style = TextStyle(
                                            brush = nameGlowBrush,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            letterSpacing = 2.sp,
                                            fontFamily = FontFamily.Serif
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Pop-up de Medicamento de Tela Cheia
            showFullScreenAlertForMed?.let { med ->
                MedicationFullScreenAlert(
                    med = med,
                    onDismiss = {
                        dismissedMedicationIds = dismissedMedicationIds + med.id
                        showFullScreenAlertForMed = null
                    },
                    onTaken = {
                        viewModel.toggleMedicationTaken(med)
                        showFullScreenAlertForMed = null
                    }
                )
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
    val latestWeight = records.lastOrNull()?.weightKg
    val startWeight = records.firstOrNull()?.weightKg

    Column(modifier = PremiumGlassModifier.fillMaxWidth().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("HISTÓRICO DE PESO", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f))
            Text("REGISTRAR", color = PrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onRegisterClick() })
        }
        
        // Exibição do último peso e detalhes da meta
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (latestWeight != null) String.format(Locale.getDefault(), "%.1f kg", latestWeight) else "-- kg",
                    fontFamily = FontFamily.Serif,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackgroundDark
                )
                Text(
                    text = "Último peso registrado",
                    fontSize = 11.sp,
                    color = OnBackgroundDark.copy(alpha = 0.5f)
                )
            }

            if (targetWeight != null && targetWeight > 0 && latestWeight != null) {
                val diff = targetWeight - latestWeight
                val absDiff = kotlin.math.abs(diff)
                val isLoss = diff < 0
                
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SecondaryGold.copy(alpha = 0.12f))
                            .border(0.5.dp, SecondaryGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Meta: ${String.format(Locale.getDefault(), "%.1f kg", targetWeight)}",
                            color = SecondaryGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            diff == 0.0 -> "Meta atingida!"
                            isLoss -> "Faltam ${String.format(Locale.getDefault(), "%.1f kg", absDiff)} (Perda)"
                            else -> "Faltam ${String.format(Locale.getDefault(), "%.1f kg", absDiff)} (Ganho)"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (diff == 0.0) PrimaryTeal else Color.White.copy(alpha = 0.8f)
                    )
                }
            }
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
                    points.add(Offset(x, y))
                }

                if (points.isNotEmpty()) {
                    path.moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val cx = (p1.x + p2.x) / 2f
                        path.cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
                    }
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

        // Barra de progresso linear de meta
        if (startWeight != null && targetWeight != null && targetWeight > 0 && latestWeight != null && startWeight != targetWeight) {
            Spacer(modifier = Modifier.height(20.dp))
            
            val totalDiff = targetWeight - startWeight
            val currentProgress = latestWeight - startWeight
            
            val progressFraction = if (totalDiff != 0.0) {
                (currentProgress / totalDiff).coerceIn(0.0, 1.0).toFloat()
            } else {
                1f
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inicial: ${String.format(Locale.getDefault(), "%.1f kg", startWeight)}",
                        fontSize = 10.sp,
                        color = OnBackgroundDark.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Atual: ${String.format(Locale.getDefault(), "%.1f kg", latestWeight)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                    Text(
                        text = "Meta: ${String.format(Locale.getDefault(), "%.1f kg", targetWeight)}",
                        fontSize = 10.sp,
                        color = SecondaryGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x1AFFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(PrimaryTeal, SecondaryGold)
                                )
							)
					)
				}
				
				val progressPercent = (progressFraction * 100).toInt()
				Text(
					text = "Você completou $progressPercent% do progresso em direção à sua meta.",
					fontSize = 11.sp,
					color = OnBackgroundDark.copy(alpha = 0.6f),
					modifier = Modifier.padding(top = 8.dp)
				)
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

@Composable
fun StepsChartCard(records: List<StepsRecord>) {
    val daysList = remember(records) {
        (0..6).map { daysAgo ->
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }.reversed()
    }

    val stepsData = remember(records, daysList) {
        daysList.map { dayCal ->
            val startOfDay = dayCal.timeInMillis
            val endOfDay = startOfDay + 86400000L - 1L
            val dayRecords = records.filter { it.endTime in startOfDay..endOfDay }
            
            val manualRecords = dayRecords.filter { it.source == "manual" }
            val totalSteps = if (manualRecords.isNotEmpty()) {
                manualRecords.sumOf { it.count }
            } else {
                dayRecords.sumOf { it.count }
            }
            
            val dayName = when (dayCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Dom"
                Calendar.MONDAY -> "Seg"
                Calendar.TUESDAY -> "Ter"
                Calendar.WEDNESDAY -> "Qua"
                Calendar.THURSDAY -> "Qui"
                Calendar.FRIDAY -> "Sex"
                else -> "Sáb"
            }
            dayName to totalSteps
        }
    }

    val averageSteps = remember(stepsData) {
        val activeDays = stepsData.filter { it.second > 0 }
        if (activeDays.isNotEmpty()) activeDays.map { it.second }.average() else 0.0
    }

    Column(modifier = PremiumGlassModifier.fillMaxWidth().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.DirectionsWalk, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                Text("PASSOS SEMANAIS", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f), letterSpacing = 1.sp)
            }
            
            if (averageSteps > 0) {
                Text(
                    text = "Média: ${averageSteps.toInt()} /dia",
                    color = PrimaryTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        val hasData = stepsData.any { it.second > 0 }
        if (hasData) {
            val animationProgress = remember { Animatable(0f) }
            LaunchedEffect(records) {
                animationProgress.animateTo(1f, animationSpec = tween(1500))
            }

            val maxSteps = remember(stepsData) {
                max(stepsData.maxOf { it.second }.toDouble(), 12000.0)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val sectionWidth = canvasWidth / 7
                        val barWidth = 20.dp.toPx()
                        
                        val targetY = canvasHeight - ((10000.0 / maxSteps) * canvasHeight).toFloat()
                        drawLine(
                            color = Color(0x33FFFFFF),
                            start = Offset(0f, targetY),
                            end = Offset(canvasWidth, targetY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                        
                        drawIntoCanvas { canvas ->
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#879391")
                                textSize = 9.dp.toPx()
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            canvas.nativeCanvas.drawText("Meta (10k)", 6.dp.toPx(), targetY - 4.dp.toPx(), textPaint)
                        }

                        stepsData.forEachIndexed { index, (_, count) ->
                            val centerX = index * sectionWidth + sectionWidth / 2f
                            val barHeight = ((count / maxSteps) * canvasHeight * animationProgress.value).toFloat()
                            val barTop = canvasHeight - barHeight
                            
                            if (count > 0) {
                                val reachedGoal = count >= 10000
                                val startColor = if (reachedGoal) PrimaryTeal else SecondaryGold
                                val endColor = if (reachedGoal) PrimaryTeal.copy(alpha = 0.3f) else SecondaryGold.copy(alpha = 0.3f)
                                
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(startColor, endColor)
                                    ),
                                    topLeft = Offset(centerX - barWidth / 2f, barTop),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )

                                drawIntoCanvas { canvas ->
                                    val textPaint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 9.dp.toPx()
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        typeface = android.graphics.Typeface.DEFAULT
                                    }
                                    val countFormatted = if (count >= 1000) {
                                        String.format(Locale.getDefault(), "%.1fk", count.toFloat() / 1000f)
                                    } else {
                                        count.toString()
                                    }
                                    canvas.nativeCanvas.drawText(
                                        countFormatted,
                                        centerX,
                                        barTop - 6.dp.toPx(),
                                        textPaint
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    stepsData.forEach { (dayName, _) ->
                        Text(
                            text = dayName,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = OnBackgroundDark.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum dado de passos registrado nos últimos 7 dias.",
                    color = OnBackgroundDark.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SleepChartCard(records: List<SleepRecord>) {
    val daysList = remember(records) {
        (0..6).map { daysAgo ->
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }.reversed()
    }

    val sleepData = remember(records, daysList) {
        daysList.map { dayCal ->
            val startOfDay = dayCal.timeInMillis
            val endOfDay = startOfDay + 86400000L - 1L
            val dayRecords = records.filter { it.endTime in startOfDay..endOfDay }
            
            val manualRecords = dayRecords.filter { it.source == "manual" }
            val totalHours = if (manualRecords.isNotEmpty()) {
                manualRecords.sumOf { it.durationHours }
            } else {
                dayRecords.sumOf { it.durationHours }
            }
            
            val dayName = when (dayCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Dom"
                Calendar.MONDAY -> "Seg"
                Calendar.TUESDAY -> "Ter"
                Calendar.WEDNESDAY -> "Qua"
                Calendar.THURSDAY -> "Qui"
                Calendar.FRIDAY -> "Sex"
                else -> "Sáb"
            }
            dayName to totalHours
        }
    }

    val averageHours = remember(sleepData) {
        val activeDays = sleepData.filter { it.second > 0 }
        if (activeDays.isNotEmpty()) activeDays.map { it.second }.average() else 0.0
    }

    Column(modifier = PremiumGlassModifier.fillMaxWidth().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Bedtime, null, tint = TertiaryPurple, modifier = Modifier.size(18.dp))
                Text("SONO SEMANAL", style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.7f), letterSpacing = 1.sp)
            }
            
            if (averageHours > 0) {
                val avgHoursInt = averageHours.toInt()
                val avgMins = ((averageHours - avgHoursInt) * 60).roundToInt()
                Text(
                    text = "Média: ${avgHoursInt}h ${avgMins}m/dia",
                    color = PrimaryTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        val hasData = sleepData.any { it.second > 0 }
        if (hasData) {
            val animationProgress = remember { Animatable(0f) }
            LaunchedEffect(records) {
                animationProgress.animateTo(1f, animationSpec = tween(1500))
            }

            val maxHours = remember(sleepData) {
                max(sleepData.maxOf { it.second }, 10.0)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val sectionWidth = canvasWidth / 7
                        val barWidth = 20.dp.toPx()
                        
                        val targetY = canvasHeight - ((8.0 / maxHours) * canvasHeight).toFloat()
                        drawLine(
                            color = Color(0x33FFFFFF),
                            start = Offset(0f, targetY),
                            end = Offset(canvasWidth, targetY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                        
                        drawIntoCanvas { canvas ->
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#879391")
                                textSize = 9.dp.toPx()
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            canvas.nativeCanvas.drawText("Meta (8h)", 6.dp.toPx(), targetY - 4.dp.toPx(), textPaint)
                        }

                        sleepData.forEachIndexed { index, (_, hours) ->
                            val centerX = index * sectionWidth + sectionWidth / 2f
                            val barHeight = ((hours / maxHours) * canvasHeight * animationProgress.value).toFloat()
                            val barTop = canvasHeight - barHeight
                            
                            if (hours > 0) {
                                val startColor = if (hours >= 8.0) PrimaryTeal else TertiaryPurple
                                val endColor = if (hours >= 8.0) PrimaryTeal.copy(alpha = 0.3f) else TertiaryPurple.copy(alpha = 0.3f)
                                
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(startColor, endColor)
                                    ),
                                    topLeft = Offset(centerX - barWidth / 2f, barTop),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )

                                drawIntoCanvas { canvas ->
                                    val textPaint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 10.dp.toPx()
                                        textAlign = android.graphics.Paint.Align.CENTER
                                        typeface = android.graphics.Typeface.DEFAULT
                                    }
                                    val hoursFormatted = String.format(Locale.getDefault(), "%.1f", hours).replace(",0", "")
                                    canvas.nativeCanvas.drawText(
                                        hoursFormatted,
                                        centerX,
                                        barTop - 6.dp.toPx(),
                                        textPaint
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    sleepData.forEach { (dayName, _) ->
                        Text(
                            text = dayName,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = OnBackgroundDark.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum dado de sono registrado nos últimos 7 dias.",
                    color = OnBackgroundDark.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
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
fun MedicationItem(med: Medication, onToggle: () -> Unit, onDeleteClick: () -> Unit) {
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.AccessTime, null, tint = OnBackgroundDark.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                Text(med.time, color = if (med.isTaken) OnBackgroundDark.copy(alpha = 0.5f) else PrimaryTeal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Excluir",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedCardContainer(
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!visible) {
            kotlinx.coroutines.delay(delayMillis.toLong())
            visible = true
        }
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

@Composable
fun MedicationFullScreenAlert(
    med: Medication,
    onDismiss: () -> Unit,
    onTaken: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "PulsePill")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PillScale"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070909))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrimaryTeal.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            center = Offset(200f, 300f),
                            radius = 900f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF5252).copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(800f, 1200f),
                            radius = 900f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(32.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(
                        text = "LEMBRETE DE REMÉDIO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hora de cuidar de você",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.08f))
                            .border(1.dp, PrimaryTeal.copy(alpha = 0.25f), CircleShape)
                            .graphicsLayer(
                                scaleX = pulseScale,
                                scaleY = pulseScale
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.15f))
                                .border(1.dp, PrimaryTeal.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MedicalServices,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Text(
                        text = med.name,
                        fontFamily = FontFamily.Serif,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    if (med.dosage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = med.dosage,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Agendado para às ${med.time}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onTaken,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "MARCAR COMO TOMADO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.5.sp
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "LEMBRAR MAIS TARDE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}


