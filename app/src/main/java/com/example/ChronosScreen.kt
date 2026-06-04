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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Routine
import com.example.data.RoutineStep
import com.example.ui.components.PremiumGlassModifier
import com.example.viewmodel.TesseraViewModel
import kotlinx.coroutines.delay
import java.util.Random

// Programmatic White Noise Player
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
                while (isPlaying) {
                    for (i in buffer.indices) {
                        // Math-synthesized white noise
                        buffer[i] = (random.nextGaussian() * 4000).toInt().toShort()
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Chronos - Seus Rituais",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = Color.White
        )
        Text(
            text = "Rotinas sequenciais cronometradas para impulsionar seu dia.",
            fontSize = 13.sp,
            color = Color(0xFF81928F),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x0AFFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (routine.iconName) {
                                            "Spa" -> Icons.Outlined.Spa
                                            "WaterDrop" -> Icons.Outlined.WaterDrop
                                            else -> Icons.Outlined.Spa
                                        },
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

                            Button(
                                onClick = { onStartRoutine(routine) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("Iniciar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                onStopRoutine()
            }
        }
    }

    val totalDuration = currentStep.durationSeconds.toFloat().coerceAtLeast(1f)
    val progress = (secondsLeft.toFloat() / totalDuration).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000, easing = LinearEasing))

    val stepIcon = when (currentStep.iconName) {
        "WaterDrop" -> Icons.Outlined.WaterDrop
        "SelfImprovement" -> Icons.Outlined.SelfImprovement
        "Spa" -> Icons.Outlined.Spa
        "MenuBook" -> Icons.Outlined.MenuBook
        else -> Icons.Outlined.Spa
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
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
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            // White Noise Toggle
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
                    contentDescription = "Ruído Branco",
                    tint = if (isWhiteNoisePlaying) Color(0xFF71D7CD) else Color(0xFF81928F),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isWhiteNoisePlaying) "Ruído Branco Ativo" else "Ativar Ruído Branco",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isWhiteNoisePlaying) Color.White else Color(0xFF81928F)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

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
                            onStopRoutine()
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
