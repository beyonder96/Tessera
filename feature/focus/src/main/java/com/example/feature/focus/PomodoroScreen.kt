package com.example.feature.focus

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.feature.focus.theme.FocusTeal
import com.example.feature.focus.theme.focusCardBorder
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun PomodoroScreen(
    scrollState: ScrollState = rememberScrollState(),
    viewModel: PomodoroViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Text(
                text = "Focus",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = {}, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Configurações",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
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
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
                    val isSelected = uiState.selectedMode == mode
                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .height(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color(0x3DFFFFFF) else Color(0x0CFFFFFF))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF8AB4F8) else focusCardBorder(),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable {
                                viewModel.onSelectMode(mode)
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = mode.icon,
                                contentDescription = mode.title,
                                tint = if (isSelected) Color(0xFF8AB4F8) else MaterialTheme.colorScheme.onSurfaceVariant,
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
                text = "${uiState.currentDuration} min",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                fontSize = 44.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // 4. TimeRuler Scale
            TimeRuler(
                value = uiState.currentDuration,
                onValueChange = { viewModel.onUpdateDuration(it) },
                range = uiState.durationRange,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${uiState.selectedMode.title.substringBefore(" ")} >",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Soundscape / Mode Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
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
                        .clickable { viewModel.onShowSoundscapeDialog(true) }
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
                            text = uiState.selectedSoundscape,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Soundscape",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
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
                            viewModel.onToggleFocusModeType()
                        }
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (uiState.selectedMode == FocusMode.BREATHING) "Deep Breath" else uiState.focusModeType,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (uiState.selectedMode == FocusMode.BREATHING) "Breathing exercise" else "Focus mode",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Start Button
        Button(
            onClick = {
                viewModel.onStartTimer()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD0E1FD),
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
    if (uiState.showSoundscapeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowSoundscapeDialog(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Select Soundscape", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Ocean", "Rain", "Silence").forEach { sound ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (uiState.selectedSoundscape == sound) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable {
                                    viewModel.onSelectSoundscape(sound)
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sound, color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                            if (uiState.selectedSoundscape == sound) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = FocusTeal)
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 7. Active Focus Mode Fullscreen Dialog
    if (uiState.isRunning) {
        ActiveFocusDialog(
            mode = uiState.selectedMode,
            secondsLeft = uiState.secondsLeft,
            soundscape = uiState.selectedSoundscape,
            onTick = { viewModel.onTick() },
            onStop = {
                viewModel.onStopTimer()
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
        val tickBaseColor = MaterialTheme.colorScheme.onBackground
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
                val tickColor = if (isCenter) Color(0xFF4285F4) else tickBaseColor
                
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
                        color = MaterialTheme.colorScheme.onBackground,
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
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Breathing mode guided circle animation
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
                            color = MaterialTheme.colorScheme.onBackground,
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
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${mode.title.substringBefore(" ")} >",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // Minimalist view
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
                        color = MaterialTheme.colorScheme.onBackground
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
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
