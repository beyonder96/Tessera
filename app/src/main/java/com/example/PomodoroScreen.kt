package com.example

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.util.Random
import java.util.UUID

// Local Focus Category data model
data class FocusCategory(
    val id: String,
    val name: String,
    val durationMinutes: Int,
    val colorHex: String
)

// Helper color parser
private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF71D7CD)
    }
}

// SharedPreferences loaders/savers
private fun loadFocusCategories(prefs: SharedPreferences): List<FocusCategory> {
    val serialized = prefs.getString("categories_list", null)
    if (serialized == null) {
        return listOf(
            FocusCategory("1", "Estudos", 50, "#D7B4F3"),
            FocusCategory("2", "Trabalho", 25, "#71D7CD"),
            FocusCategory("3", "Oração", 15, "#F9A826")
        )
    }
    return try {
        serialized.split("||").filter { it.isNotBlank() }.map { item ->
            val parts = item.split("::")
            FocusCategory(parts[0], parts[1], parts[2].toInt(), parts[3])
        }
    } catch (e: Exception) {
        listOf(
            FocusCategory("1", "Estudos", 50, "#D7B4F3"),
            FocusCategory("2", "Trabalho", 25, "#71D7CD"),
            FocusCategory("3", "Oração", 15, "#F9A826")
        )
    }
}

private fun saveFocusCategories(prefs: SharedPreferences, list: List<FocusCategory>) {
    val serialized = list.joinToString("||") { "${it.id}::${it.name}::${it.durationMinutes}::${it.colorHex}" }
    prefs.edit().putString("categories_list", serialized).apply()
}

// Stereo Binaural Beats Player (4Hz Theta wave for focus + rain background)
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
                        // Soft brown noise (waterfall/rain)
                        val whiteL = random.nextGaussian().toFloat() * 600f
                        val whiteR = random.nextGaussian().toFloat() * 600f
                        
                        lastLeft = (lastLeft * 0.98f) + (whiteL * 0.05f)
                        lastRight = (lastRight * 0.98f) + (whiteR * 0.05f)
                        
                        // Binaural sine hum with dynamic volume swells (6-second cycle)
                        val swell = 0.5f + 0.3f * Math.sin(2.0 * Math.PI * time / (sampleRateF * 6f)).toFloat()
                        val sineL = Math.sin(phaseLeft.toDouble()).toFloat() * 1200f * swell
                        val sineR = Math.sin(phaseRight.toDouble()).toFloat() * 1200f * swell
                        
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
fun PomodoroScreen() {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("tessera_focus_prefs", Context.MODE_PRIVATE) }
    
    // Load categories
    var categories by remember { mutableStateOf(loadFocusCategories(sharedPrefs)) }
    var selectedCategory by remember(categories) { mutableStateOf(categories.firstOrNull() ?: FocusCategory("1", "Estudos", 50, "#D7B4F3")) }
    
    var showManageDialog by remember { mutableStateOf(false) }
    
    val targetSeconds = selectedCategory.durationMinutes * 60
    var secondsLeft by remember(selectedCategory) { mutableStateOf(targetSeconds) }
    var isRunning by remember { mutableStateOf(false) }

    // Sound player
    val focusSoundPlayer = remember { FocusSoundPlayer() }
    var isSoundPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            focusSoundPlayer.stop()
        }
    }

    LaunchedEffect(isRunning, selectedCategory) {
        if (isRunning) {
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
            isRunning = false
        }
    }

    val progress = (secondsLeft.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000, easing = LinearEasing))

    val accentColor = parseHexColor(selectedCategory.colorHex)

    // Canvas rotation/glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "Glow")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "Rotation"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Pulse"
    )

    // Detect Orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // LANDSCAPE LAYOUT
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Immersive Timer Ring
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    val strokeWidth = 5.dp.toPx()
                    drawArc(
                        color = Color(0xFF111111),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = accentColor.copy(alpha = 0.15f * pulseGlow),
                        startAngle = -90f + rotationAngle,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth * 2.2f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        color = Color.White
                    )
                }
            }

            // Right: Category selection, controls, audio toggle, and manage options
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FOCUS: ${selectedCategory.name.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showManageDialog = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Gerenciar", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }

                // Audio toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141414))
                        .clickable {
                            isSoundPlaying = !isSoundPlaying
                            if (isSoundPlaying) focusSoundPlayer.start() else focusSoundPlayer.stop()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSoundPlaying) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeMute,
                        contentDescription = "Som",
                        tint = if (isSoundPlaying) accentColor else Color(0xFF555555),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSoundPlaying) "Binaural Ativo" else "Binaural Focus",
                        fontSize = 11.sp,
                        color = if (isSoundPlaying) Color.White else Color(0xFF666666)
                    )
                }

                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isRunning = false
                            secondsLeft = targetSeconds
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF111111), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { isRunning = !isRunning },
                        modifier = Modifier
                            .size(52.dp)
                            .background(accentColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Controle",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    } else {
        // PORTRAIT LAYOUT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .padding(horizontal = 20.dp)
                .padding(bottom = 120.dp), // Fix FAB overlap
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(32.dp)) // Offset to keep text centered
                    Text(
                        text = "FOCUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 3.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { showManageDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Gerenciar", tint = Color.White.copy(alpha = 0.5f))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable category row
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141414))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = selectedCategory.id == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSel) Color(0xFF222222) else Color.Transparent)
                                .clickable { 
                                    selectedCategory = cat
                                    isRunning = false
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat.name,
                                color = if (isSel) Color.White else Color(0xFF666666),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Circular Timer
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()
                    drawArc(
                        color = Color(0xFF111111),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = accentColor.copy(alpha = 0.15f * pulseGlow),
                        startAngle = -90f + rotationAngle,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth * 2.2f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 54.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Foco ativado em ${selectedCategory.name}",
                        fontSize = 11.sp,
                        color = Color(0xFF555555),
                        letterSpacing = 1.sp
                    )
                }
            }

            // Controls & Audio Option
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                // Binaural Sound Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141414))
                        .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                        .clickable {
                            isSoundPlaying = !isSoundPlaying
                            if (isSoundPlaying) focusSoundPlayer.start() else focusSoundPlayer.stop()
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSoundPlaying) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeMute,
                        contentDescription = "Som de Foco",
                        tint = if (isSoundPlaying) accentColor else Color(0xFF555555),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSoundPlaying) "Binaural 4Hz Ativado" else "Ativar Binaural Focus",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSoundPlaying) Color.White else Color(0xFF666666)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isRunning = false
                            secondsLeft = targetSeconds
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF111111), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { isRunning = !isRunning },
                        modifier = Modifier
                            .size(68.dp)
                            .background(accentColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Iniciar / Pausar",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }

    if (showManageDialog) {
        ManageFocusCategoriesDialog(
            categories = categories,
            onSave = { updatedList ->
                categories = updatedList
                saveFocusCategories(sharedPrefs, updatedList)
                // Fallback to first if selected is deleted
                if (updatedList.none { it.id == selectedCategory.id }) {
                    selectedCategory = updatedList.firstOrNull() ?: FocusCategory("1", "Estudos", 50, "#D7B4F3")
                } else {
                    selectedCategory = updatedList.find { it.id == selectedCategory.id }!!
                }
                isRunning = false
            },
            onDismiss = { showManageDialog = false }
        )
    }
}

@Composable
fun ManageFocusCategoriesDialog(
    categories: List<FocusCategory>,
    onSave: (List<FocusCategory>) -> Unit,
    onDismiss: () -> Unit
) {
    var listState by remember { mutableStateOf(categories) }
    var editingCategory by remember { mutableStateOf<FocusCategory?>(null) }
    
    // Form states
    var name by remember { mutableStateOf("") }
    var durationMins by remember { mutableStateOf("25") }
    val colorPalettes = listOf("#71D7CD", "#D7B4F3", "#F9A826", "#EF4444", "#3B82F6", "#10B981")
    var selectedColor by remember { mutableStateOf(colorPalettes.first()) }
    
    LaunchedEffect(editingCategory) {
        if (editingCategory != null) {
            name = editingCategory!!.name
            durationMins = editingCategory!!.durationMinutes.toString()
            selectedColor = editingCategory!!.colorHex
        } else {
            name = ""
            durationMins = "25"
            selectedColor = colorPalettes.first()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Gerenciar Foco",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = Color.White
                )

                if (editingCategory == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listState.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0CFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).background(parseHexColor(cat.colorHex), CircleShape))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = "${cat.name} (${cat.durationMinutes}m)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = { editingCategory = cat },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF71D7CD), modifier = Modifier.size(16.dp))
                                    }
                                    if (listState.size > 1) {
                                        IconButton(
                                            onClick = {
                                                listState = listState.filter { it.id != cat.id }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                editingCategory = FocusCategory(UUID.randomUUID().toString(), "", 25, colorPalettes.first())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Novo Canal de Foco", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome (ex: Estudos)", color = Color(0x66FFFFFF)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = durationMins,
                            onValueChange = { durationMins = it.filter { c -> c.isDigit() } },
                            label = { Text("Duração (Minutos)", color = Color(0x66FFFFFF)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0x1AFFFFFF), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Text("COR DE FOCO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0x66FFFFFF))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            colorPalettes.forEach { hex ->
                                val col = parseHexColor(hex)
                                val isSel = selectedColor == hex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(if (isSel) 2.dp else 0.dp, Color.White, CircleShape)
                                        .clickable { selectedColor = hex }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(onClick = { editingCategory = null }) {
                                Text("Voltar", color = Color.White.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    val duration = durationMins.toIntOrNull() ?: 25
                                    if (name.isNotBlank() && duration > 0) {
                                        val existingIndex = listState.indexOfFirst { it.id == editingCategory!!.id }
                                        val updatedCat = editingCategory!!.copy(name = name, durationMinutes = duration, colorHex = selectedColor)
                                        
                                        listState = if (existingIndex >= 0) {
                                            listState.toMutableList().apply { set(existingIndex, updatedCat) }
                                        } else {
                                            listState + updatedCat
                                        }
                                        editingCategory = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                            ) {
                                Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (editingCategory == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Fechar", color = Color.White.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                onSave(listState)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                        ) {
                            Text("Aplicar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
