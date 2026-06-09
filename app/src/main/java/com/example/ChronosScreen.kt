package com.example

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Routine
import com.example.data.RoutineStep
import com.example.ui.components.PremiumGlassModifier
import com.example.viewmodel.TesseraViewModel
import kotlinx.coroutines.delay
import java.util.Random

// Programmatic Calm Ambient Sound Player (Brown Noise + Meditative Drone)
class WhiteNoisePlayer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    fun start() {
        if (isPlaying) return
        isPlaying = true
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        try {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack?.play()
            
            Thread {
                val buffer = ShortArray(bufferSize)
                val random = Random()
                var lastValue = 0f
                var phase1 = 0f
                var phase2 = 0f
                val sampleRateF = 44100f
                val freq1 = 110f // A2 note
                val freq2 = 165f // E3 note (perfect fifth)
                val phaseIncrement1 = 2f * Math.PI.toFloat() * freq1 / sampleRateF
                val phaseIncrement2 = 2f * Math.PI.toFloat() * freq2 / sampleRateF
                
                var time = 0L
                while (isPlaying) {
                    for (i in buffer.indices) {
                        // 1. Brown noise generator (calm rain/waterfall)
                        val white = random.nextGaussian().toFloat() * 1000f
                        lastValue = (lastValue * 0.98f) + (white * 0.05f)
                        
                        // 2. Meditative drone sine waves (soft hum)
                        val swell = 0.5f + 0.3f * Math.sin(2.0 * Math.PI * time / (sampleRateF * 6f)).toFloat()
                        val sine1 = Math.sin(phase1.toDouble()).toFloat() * 1500f * swell
                        val sine2 = Math.sin(phase2.toDouble()).toFloat() * 800f * swell
                        
                        phase1 += phaseIncrement1
                        if (phase1 > 2f * Math.PI.toFloat()) phase1 -= 2f * Math.PI.toFloat()
                        
                        phase2 += phaseIncrement2
                        if (phase2 > 2f * Math.PI.toFloat()) phase2 -= 2f * Math.PI.toFloat()
                        
                        val mixed = lastValue + sine1 + sine2
                        buffer[i] = mixed.coerceIn(-32768f, 32767f).toInt().toShort()
                        time++
                    }
                    audioTrack?.write(buffer, 0, buffer.size)
                }
            }.start()
        } catch (e: Exception) {
            isPlaying = false
        }
    }

    fun stop() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null
    }
}

@Composable
fun ChronosScreen(viewModel: TesseraViewModel) {
    val routines by viewModel.allRoutines.collectAsStateWithLifecycle()
    var activeRoutine by remember { mutableStateOf<Routine?>(null) }

    AnimatedContent(
        targetState = activeRoutine,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
        },
        label = "RoutinePlayerTransition"
    ) { routine ->
        if (routine == null) {
            RoutinesListView(routines = routines, viewModel = viewModel, onStartRoutine = { activeRoutine = it })
        } else {
            RoutinePlayerView(routine = routine, viewModel = viewModel, onStopRoutine = { activeRoutine = null })
        }
    }
}

@Composable
fun RoutinesListView(
    routines: List<Routine>,
    viewModel: TesseraViewModel,
    onStartRoutine: (Routine) -> Unit
) {
    var showAddRoutineDialog by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<Routine?>(null) }
    var routineToEditSteps by remember { mutableStateOf<List<RoutineStep>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chronos - Seus Rituais",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = Color.White
            )
            IconButton(onClick = { showAddRoutineDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Gerenciar", tint = Color(0xFF71D7CD))
            }
        }
        Text(
            text = "Rotinas sequenciais cronometradas para impulsionar seu dia.",
            fontSize = 13.sp,
            color = Color(0xFF81928F),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 140.dp), // Fix FAB overlap
            modifier = Modifier.fillMaxSize()
        ) {
            items(routines, key = { it.id }) { routine ->
                val steps by viewModel.getStepsForRoutine(routine.id).collectAsStateWithLifecycle(initialValue = emptyList())
                val totalDuration = steps.sumOf { it.durationSeconds }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .then(PremiumGlassModifier)
                        .border(0.5.dp, Color(0x20FFFFFF), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x0AFFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getChronosIcon(routine.iconName),
                                        contentDescription = null,
                                        tint = Color(0xFF71D7CD),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = routine.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${steps.size} passos • ${totalDuration / 60} min e ${totalDuration % 60}s",
                                        fontSize = 12.sp,
                                        color = Color(0xFF81928F)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        routineToEdit = routine
                                        routineToEditSteps = steps
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF71D7CD), modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteRoutine(routine) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                                Button(
                                    onClick = { onStartRoutine(routine) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Iniciar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        if (steps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0x0CFFFFFF))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Steps summary list
                            steps.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF71D7CD).copy(alpha = 0.6f))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = step.title,
                                        fontSize = 13.sp,
                                        color = Color(0xFFBDC9C6),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${step.durationSeconds}s",
                                        fontSize = 12.sp,
                                        color = Color(0xFF81928F)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRoutineDialog) {
        ManageRoutineDialog(
            routine = null,
            initialSteps = emptyList(),
            onSave = { name, icon, stepsList ->
                viewModel.saveRoutineWithSteps(Routine(name = name, iconName = icon), stepsList)
            },
            onDismiss = { showAddRoutineDialog = false }
        )
    }

    if (routineToEdit != null) {
        ManageRoutineDialog(
            routine = routineToEdit,
            initialSteps = routineToEditSteps,
            onSave = { name, icon, stepsList ->
                viewModel.saveRoutineWithSteps(Routine(id = routineToEdit!!.id, name = name, iconName = icon), stepsList)
            },
            onDismiss = { routineToEdit = null }
        )
    }
}

@Composable
fun ManageRoutineDialog(
    routine: Routine?,
    initialSteps: List<RoutineStep>,
    onSave: (String, String, List<RoutineStep>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(routine?.name ?: "") }
    val iconOptions = listOf("Spa", "WaterDrop", "SelfImprovement", "MenuBook", "FitnessCenter", "Coffee", "Bedtime", "WorkOutline", "Lightbulb", "FavoriteBorder", "MusicNote", "Brush")
    var selectedIcon by remember { mutableStateOf(routine?.iconName ?: "Spa") }
    
    var steps by remember { mutableStateOf(initialSteps) }
    
    // Step creation form states
    var stepTitle by remember { mutableStateOf("") }
    var stepDurationMins by remember { mutableStateOf("2") }
    var stepDurationSecs by remember { mutableStateOf("0") }
    var selectedStepIcon by remember { mutableStateOf("Spa") }
    
    var currentStepQuestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var newQuestionText by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(24.dp))
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (routine == null) "Nova Rotina" else "Editar Rotina",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Rotina (ex: Manhã)", color = Color(0x66FFFFFF)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Column {
                    Text("ÍCONE DA ROTINA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        iconOptions.forEach { iconName ->
                            val isSel = selectedIcon == iconName
                            val icon = getChronosIcon(iconName)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) Color(0xFF71D7CD).copy(alpha = 0.2f) else Color(0x0CFFFFFF))
                                    .border(if (isSel) 1.dp else 0.dp, Color(0xFF71D7CD), CircleShape)
                                    .clickable { selectedIcon = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = if (isSel) Color(0xFF71D7CD) else Color(0xFF81928F), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = Color(0x14FFFFFF))
                
                Text("PASSOS DA ROTINA (${steps.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF71D7CD))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = getChronosIcon(step.iconName),
                                    contentDescription = null,
                                    tint = Color(0xFF71D7CD),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(step.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("${step.durationSeconds}s", color = Color(0xFF81928F), fontSize = 11.sp)
                                    
                                    // Show checklist questions if present
                                    if (step.checkQuestions.isNotBlank()) {
                                        val questionList = step.checkQuestions.split("\n").filter { it.isNotBlank() }
                                        if (questionList.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            questionList.forEach { q ->
                                                Text(
                                                    text = "? $q",
                                                    color = Color(0xFF71D7CD),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(start = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            steps = steps.toMutableList().apply {
                                                val temp = this[index]
                                                this[index] = this[index - 1]
                                                this[index - 1] = temp
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                if (index < steps.size - 1) {
                                    IconButton(
                                        onClick = {
                                            steps = steps.toMutableList().apply {
                                                val temp = this[index]
                                                this[index] = this[index + 1]
                                                this[index + 1] = temp
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        steps = steps.filterIndexed { i, _ -> i != index }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x05FFFFFF), RoundedCornerShape(16.dp))
                        .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ADICIONAR NOVO PASSO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81928F))
                        
                        OutlinedTextField(
                            value = stepTitle,
                            onValueChange = { stepTitle = it },
                            label = { Text("Nome do Passo (ex: Meditar)", color = Color(0x66FFFFFF)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = stepDurationMins,
                                onValueChange = { stepDurationMins = it.filter { c -> c.isDigit() } },
                                label = { Text("Minutos", color = Color(0x66FFFFFF)) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = stepDurationSecs,
                                onValueChange = { stepDurationSecs = it.filter { c -> c.isDigit() } },
                                label = { Text("Segundos", color = Color(0x66FFFFFF)) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ÍCONE DO PASSO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81928F))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                iconOptions.forEach { iconName ->
                                    val isSel = selectedStepIcon == iconName
                                    val icon = getChronosIcon(iconName)
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(if (isSel) Color(0xFF71D7CD).copy(alpha = 0.2f) else Color(0x0CFFFFFF))
                                            .border(if (isSel) 1.dp else 0.dp, Color(0xFF71D7CD), CircleShape)
                                            .clickable { selectedStepIcon = iconName },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = null, tint = if (isSel) Color(0xFF71D7CD) else Color(0xFF81928F), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                        
                        // Checklist Questions Input
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("PERGUNTAS DE VERIFICAÇÃO (OPCIONAL)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81928F))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newQuestionText,
                                    onValueChange = { newQuestionText = it },
                                    label = { Text("Ex: Desligou as luzes?", color = Color(0x44FFFFFF), fontSize = 11.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF71D7CD),
                                        unfocusedBorderColor = Color(0x1AFFFFFF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        if (newQuestionText.isNotBlank()) {
                                            currentStepQuestions = currentStepQuestions + newQuestionText.trim()
                                            newQuestionText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD).copy(alpha = 0.2f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("+ Pergunta", color = Color(0xFF71D7CD), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            
                            if (currentStepQuestions.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    currentStepQuestions.forEachIndexed { qIdx, question ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0x05FFFFFF), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "- $question",
                                                color = Color(0xFFBDC9C6),
                                                fontSize = 12.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    currentStepQuestions = currentStepQuestions.filterIndexed { idx, _ -> idx != qIdx }
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remover", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    val mins = stepDurationMins.toIntOrNull() ?: 0
                                    val secs = stepDurationSecs.toIntOrNull() ?: 0
                                    val totalSecs = mins * 60 + secs
                                    if (stepTitle.isNotBlank() && totalSecs > 0) {
                                        val serializedQuestions = currentStepQuestions.joinToString("\n")
                                        steps = steps + RoutineStep(
                                            routineId = routine?.id ?: 0,
                                            title = stepTitle,
                                            durationSeconds = totalSecs,
                                            iconName = selectedStepIcon,
                                            orderIndex = steps.size,
                                            checkQuestions = serializedQuestions
                                        )
                                        stepTitle = ""
                                        stepDurationMins = "2"
                                        stepDurationSecs = "0"
                                        currentStepQuestions = emptyList()
                                        newQuestionText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                            ) {
                                Text("+ Passo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && steps.isNotEmpty()) {
                                onSave(name, selectedIcon, steps)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                    ) {
                        Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RoutinePlayerView(
    routine: Routine,
    viewModel: TesseraViewModel,
    onStopRoutine: () -> Unit
) {
    val steps by viewModel.getStepsForRoutine(routine.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var currentStepIndex by remember { mutableStateOf(0) }
    
    val noisePlayer = remember { WhiteNoisePlayer() }
    var isWhiteNoisePlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            noisePlayer.stop()
        }
    }

    if (steps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF71D7CD))
        }
        return
    }

    val currentStep = steps.getOrNull(currentStepIndex) ?: steps.first()
    var secondsLeft by remember(currentStep) { mutableStateOf(currentStep.durationSeconds) }
    var isTimerRunning by remember { mutableStateOf(true) }

    val allChecklistQuestions = remember(steps) {
        steps.flatMap { step ->
            step.checkQuestions.split("\n").filter { it.isNotBlank() }
        }
    }

    var showChecklistScreen by remember { mutableStateOf(false) }
    var checkedQuestions by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(currentStep, isTimerRunning) {
        if (isTimerRunning) {
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
            // Auto transition
            if (currentStepIndex < steps.size - 1) {
                currentStepIndex++
            } else {
                if (allChecklistQuestions.isNotEmpty()) {
                    showChecklistScreen = true
                } else {
                    viewModel.completeRoutine(routine)
                    onStopRoutine()
                }
            }
        }
    }

    val totalDuration = currentStep.durationSeconds.toFloat().coerceAtLeast(1f)
    val progress = (secondsLeft.toFloat() / totalDuration).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000, easing = LinearEasing))

    val stepIcon = getChronosIcon(currentStep.iconName)

    if (showChecklistScreen) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    text = routine.name.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71D7CD),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Verificação Final",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = Color.White
                )
                Text(
                    text = "Responda às perguntas associadas aos passos.",
                    fontSize = 13.sp,
                    color = Color(0xFF81928F),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Checklist Items Scrollable List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                allChecklistQuestions.forEach { question ->
                    val isChecked = checkedQuestions.contains(question)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x0AFFFFFF))
                            .border(0.5.dp, if (isChecked) Color(0xFF71D7CD).copy(alpha = 0.3f) else Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                            .clickable {
                                checkedQuestions = if (isChecked) {
                                    checkedQuestions - question
                                } else {
                                    checkedQuestions + question
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(2.dp, if (isChecked) Color(0xFF71D7CD) else Color(0xFF81928F), CircleShape)
                                .background(if (isChecked) Color(0xFF71D7CD) else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = question,
                            fontSize = 15.sp,
                            color = if (isChecked) Color.White else Color(0xFFBDC9C6),
                            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Bottom Buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        viewModel.completeRoutine(routine)
                        onStopRoutine()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Concluir Rotina",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(onClick = onStopRoutine) {
                    Text("Sair sem concluir", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 120.dp), // Adjust for navigation overlap
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(
                    text = routine.name.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71D7CD),
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Passo ${currentStepIndex + 1} de ${steps.size}",
                    fontSize = 13.sp,
                    color = Color(0xFF81928F)
                )
            }

            // Circular Timer Display
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color(0x0CFFFFFF),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFF71D7CD),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = stepIcon,
                        contentDescription = null,
                        tint = Color(0xFF71D7CD),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentStep.title,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Controls and Mute Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // Ambient Sound Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0AFFFFFF))
                        .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                        .clickable {
                            isWhiteNoisePlaying = !isWhiteNoisePlaying
                            if (isWhiteNoisePlaying) noisePlayer.start() else noisePlayer.stop()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isWhiteNoisePlaying) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeMute,
                        contentDescription = "Som Relaxante",
                        tint = if (isWhiteNoisePlaying) Color(0xFF71D7CD) else Color(0xFF81928F),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isWhiteNoisePlaying) "Som Relaxante Ativo" else "Ativar Som Relaxante",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWhiteNoisePlaying) Color.White else Color(0xFF81928F)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Player Commands
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel/Stop
                    IconButton(
                        onClick = onStopRoutine,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x0CFFFFFF), CircleShape)
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancelar", tint = Color.White)
                    }

                    // Play / Pause
                    IconButton(
                        onClick = { isTimerRunning = !isTimerRunning },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF71D7CD), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Controle",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Skip Step
                    IconButton(
                        onClick = {
                            if (currentStepIndex < steps.size - 1) {
                                currentStepIndex++
                            } else {
                                if (allChecklistQuestions.isNotEmpty()) {
                                    showChecklistScreen = true
                                } else {
                                    viewModel.completeRoutine(routine)
                                    onStopRoutine()
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x0CFFFFFF), CircleShape)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Pular", tint = Color.White)
                    }
                }
            }
        }
    }
}

fun getChronosIcon(name: String): ImageVector {
    return when (name) {
        "Spa" -> Icons.Outlined.Spa
        "WaterDrop" -> Icons.Outlined.WaterDrop
        "SelfImprovement" -> Icons.Outlined.SelfImprovement
        "MenuBook" -> Icons.Outlined.MenuBook
        "FitnessCenter" -> Icons.Outlined.FitnessCenter
        "Coffee" -> Icons.Outlined.Coffee
        "Bedtime" -> Icons.Outlined.Bedtime
        "WorkOutline" -> Icons.Outlined.WorkOutline
        "Lightbulb" -> Icons.Outlined.Lightbulb
        "FavoriteBorder" -> Icons.Outlined.FavoriteBorder
        "MusicNote" -> Icons.Outlined.MusicNote
        "Brush" -> Icons.Outlined.Brush
        else -> Icons.Outlined.Spa
    }
}
