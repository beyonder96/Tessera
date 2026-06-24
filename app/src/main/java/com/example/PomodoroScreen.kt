package com.example

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.theme.PrimaryTeal
import kotlinx.coroutines.delay
import java.util.Random
import kotlin.math.roundToInt

// Focus Mode Types
enum class FocusMode(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FOCUS_TIMER("Focus Timer", Icons.Outlined.CenterFocusStrong),
    QUICK_NAP("Quick Nap", Icons.Outlined.DoNotDisturbOn),
    BREATHING("Breathing", Icons.Outlined.Eco)
}

// Binaural beats sound player (4Hz Theta wave for focus)
class FocusSoundPlayer {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    fun start() {
        if (isPlaying) return
        isPlaying = true
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        try {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack?.play()
            
            Thread {
                val buffer = ShortArray(bufferSize)
                val random = Random()
                var lastLeft = 0f
                var lastRight = 0f
                
                var phaseLeft = 0f
                var phaseRight = 0f
                val sampleRateF = 44100f
                
                // Theta wave binaural difference: 100Hz in left ear, 104Hz in right ear -> 4Hz difference
                val freqLeft = 100f
                val freqRight = 104f
                val phaseIncLeft = 2f * Math.PI.toFloat() * freqLeft / sampleRateF
                val phaseIncRight = 2f * Math.PI.toFloat() * freqRight / sampleRateF
                
                var time = 0L
                while (isPlaying) {
                    for (i in 0 until buffer.size step 2) {
                        // Soft brown noise simulating serene ocean waves
                        val whiteL = random.nextGaussian().toFloat() * 550f
                        val whiteR = random.nextGaussian().toFloat() * 550f
                        
                        lastLeft = (lastLeft * 0.98f) + (whiteL * 0.05f)
                        lastRight = (lastRight * 0.98f) + (whiteR * 0.05f)
                        
                        // Binaural sine hum with dynamic volume swells (6-second cycle)
                        val swell = 0.5f + 0.3f * Math.sin(2.0 * Math.PI * time / (sampleRateF * 6f)).toFloat()
                        val sineL = Math.sin(phaseLeft.toDouble()).toFloat() * 1100f * swell
                        val sineR = Math.sin(phaseRight.toDouble()).toFloat() * 1100f * swell
                        
                        phaseLeft += phaseIncLeft
                        if (phaseLeft > 2f * Math.PI.toFloat()) phaseLeft -= 2f * Math.PI.toFloat()
                        
                        phaseRight += phaseIncRight
                        if (phaseRight > 2f * Math.PI.toFloat()) phaseRight -= 2f * Math.PI.toFloat()
                        
                        val mixedL = lastLeft + sineL
                        val mixedR = lastRight + sineR
                        
                        if (i < buffer.size) {
                            buffer[i] = mixedL.coerceIn(-32768f, 32767f).toInt().toShort()
                        }
                        if (i + 1 < buffer.size) {
                            buffer[i + 1] = mixedR.coerceIn(-32768f, 32767f).toInt().toShort()
                        }
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
fun PomodoroScreen(scrollState: ScrollState = rememberScrollState()) {
    var selectedMode by remember { mutableStateOf(FocusMode.FOCUS_TIMER) }
    
    // Duration ranges: Focus (1..120 min), Nap (5..60 min), Breathing (1..15 min)
    val durationRange = when (selectedMode) {
        FocusMode.FOCUS_TIMER -> 1f..120f
        FocusMode.QUICK_NAP -> 5f..60f
        FocusMode.BREATHING -> 1f..15f
    }
    
    // Default durations
    var focusDuration by remember { mutableStateOf(25) }
    var napDuration by remember { mutableStateOf(20) }
    var breathingDuration by remember { mutableStateOf(3) }
    
    val currentDuration = when (selectedMode) {
        FocusMode.FOCUS_TIMER -> focusDuration
        FocusMode.QUICK_NAP -> napDuration
        FocusMode.BREATHING -> breathingDuration
    }
    
    val updateDuration: (Int) -> Unit = { value ->
        when (selectedMode) {
            FocusMode.FOCUS_TIMER -> focusDuration = value
            FocusMode.QUICK_NAP -> napDuration = value
            FocusMode.BREATHING -> breathingDuration = value
        }
    }
    
    var isRunning by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(0) }
    
    var selectedSoundscape by remember { mutableStateOf("Ocean") }
    var focusModeType by remember { mutableStateOf("Goal Timer") }
    
    var showSoundscapeDialog by remember { mutableStateOf(false) }
    
    val focusSoundPlayer = remember { FocusSoundPlayer() }
    
    DisposableEffect(Unit) {
        onDispose {
            focusSoundPlayer.stop()
        }
    }
 
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = 120.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Header Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Estatísticas",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
            Text(
                text = "Focus",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configurações",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // 2. "Now for you" Carousel
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "✨ Now for you",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FocusMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .height(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color(0x3DFFFFFF) else Color(0x0CFFFFFF))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) Color(0xFF8AB4F8) else Color(0x1AFFFFFF),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable {
                                selectedMode = mode
                                isRunning = false
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = mode.icon,
                                contentDescription = mode.title,
                                tint = if (isSelected) Color(0xFF8AB4F8) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = mode.title,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Selection Minutes Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$currentDuration min",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                fontSize = 44.sp,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 4. TimeRuler Scale
            TimeRuler(
                value = currentDuration,
                onValueChange = updateDuration,
                range = durationRange,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${selectedMode.title.substringBefore(" ")} >",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Soundscape / Mode Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .then(PremiumGlassModifier)
                .background(Color(0x05FFFFFF))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Soundscape Selection
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showSoundscapeDialog = true }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ocean_focus_background),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Column {
                        Text(
                            text = selectedSoundscape,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Soundscape",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )

                // Right Column: Mode Type Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            focusModeType = if (focusModeType == "Goal Timer") "Stopwatch" else "Goal Timer"
                        }
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (selectedMode == FocusMode.BREATHING) "Deep Breath" else focusModeType,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (selectedMode == FocusMode.BREATHING) "Breathing exercise" else "Focus mode",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Start Button
        Button(
            onClick = {
                secondsLeft = currentDuration * 60
                isRunning = true
                if (selectedSoundscape == "Ocean") {
                    focusSoundPlayer.start()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD0E1FD), // Light blue-purple
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = "Start",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }

    // Soundscape Selector Dialog
    if (showSoundscapeDialog) {
        AlertDialog(
            onDismissRequest = { showSoundscapeDialog = false },
            containerColor = Color(0xFF141918),
            title = { Text("Select Soundscape", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Ocean", "Rain", "Silence").forEach { sound ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedSoundscape == sound) Color(0x1AFFFFFF) else Color.Transparent)
                                .clickable {
                                    selectedSoundscape = sound
                                    showSoundscapeDialog = false
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sound, color = Color.White, fontSize = 15.sp)
                            if (selectedSoundscape == sound) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryTeal)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 7. Active Focus Mode Fullscreen Dialog
    if (isRunning) {
        ActiveFocusDialog(
            mode = selectedMode,
            secondsLeft = secondsLeft,
            soundscape = selectedSoundscape,
            onTick = { secondsLeft-- },
            onStop = {
                isRunning = false
                focusSoundPlayer.stop()
            }
        )
    }
}

// Custom TimeRuler Scale Component
@Composable
fun TimeRuler(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
            val width = size.width
            val height = size.height
            val numTicks = 25
            val spacing = width / (numTicks - 1)
            
            for (i in 0 until numTicks) {
                val x = i * spacing
                val isCenter = i == numTicks / 2
                val tickHeight = if (isCenter) height * 0.85f else height * 0.45f
                val tickAlpha = if (isCenter) 1f else 0.22f
                val tickColor = if (isCenter) Color(0xFF4285F4) else Color.White
                
                drawLine(
                    color = tickColor.copy(alpha = tickAlpha),
                    start = Offset(x, (height - tickHeight) / 2),
                    end = Offset(x, (height + tickHeight) / 2),
                    strokeWidth = if (isCenter) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Active Focus Dialog (Immersive fullscreen)
@Composable
fun ActiveFocusDialog(
    mode: FocusMode,
    secondsLeft: Int,
    soundscape: String,
    onTick: () -> Unit,
    onStop: () -> Unit
) {
    var isMinimalView by remember { mutableStateOf(false) }
    
    // Countdown coroutine
    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000L)
            onTick()
        } else {
            onStop()
        }
    }

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isMinimalView = !isMinimalView
                }
        ) {
            // Background Image (Serene Ocean sunset)
            Image(
                painter = painterResource(id = R.drawable.ocean_focus_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark vignette overlay for depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f)
                            )
                        )
                    )
            )

            if (!isMinimalView) {
                // ACTIVE COMMON VIEW
                
                // Top Header Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = soundscape,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Configuração do Foco",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Breathing mode guided circle animation (Middle of screen)
                if (mode == FocusMode.BREATHING) {
                    val breathingAnim = rememberInfiniteTransition(label = "BreathingCycle")
                    val scale by breathingAnim.animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(4000, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "Scale"
                    )
                    
                    // Guided Text based on scale size
                    val phaseText = when {
                        scale > 1.15f -> "Segure..."
                        scale < 0.85f -> "Segure..."
                        scale > 1.0f -> "Expire..."
                        else -> "Inspire..."
                    }

                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                        }
                        Text(
                            text = phaseText,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Bottom Left: Timer Display
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = timeString,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 44.sp,
                        color = Color.White
                    )
                    Text(
                        text = "${mode.title.substringBefore(" ")} >",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Bottom Right: Stop Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable { onStop() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Square,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // IMMERSIVE MINIMALIST VIEW
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = timeString,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        fontSize = 68.sp,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.6f))
                        )
                        Text(
                            text = mode.title.substringBefore(" "),
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
