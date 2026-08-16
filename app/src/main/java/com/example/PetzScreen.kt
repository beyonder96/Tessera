package com.example
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.*

import com.example.utils.toDoubleClean
import com.example.utils.toDoubleCleanOrZero
import android.util.Log
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.PetEntity
import com.example.data.PetEvent
import com.example.data.PetSex
import com.example.data.PetWeightHistoryEntity
import com.example.ui.components.bounceClick
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.TertiaryPurple
import com.example.viewmodel.PetViewModel
import com.example.viewmodel.TesseraViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Helper function to open native DatePickerDialog
fun showDatePicker(context: Context, initialTime: Long, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialTime }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(selectedCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

// Helper function to calculate age based on birth date
fun calculateAge(birthDate: Long): String {
    val birthCal = Calendar.getInstance().apply { timeInMillis = birthDate }
    val now = Calendar.getInstance()
    var age = now.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
    if (now.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    
    val totalMonths = (now.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)) * 12 +
            (now.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH)) +
            (if (now.get(Calendar.DAY_OF_MONTH) < birthCal.get(Calendar.DAY_OF_MONTH)) -1 else 0)

    return when {
        age <= 0 || totalMonths < 12 -> {
            when {
                totalMonths <= 0 -> "Recém-nascido"
                totalMonths == 1 -> "1 mês"
                else -> "$totalMonths meses"
            }
        }
        age == 1 -> "1 ano"
        else -> "$age anos"
    }
}

// Helper function to format RGA (e.g. 2.394.541)
fun formatRga(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(7)
    val len = digits.length
    return when {
        len <= 3 -> digits
        len <= 6 -> "${digits.substring(0, len - 3)}.${digits.substring(len - 3)}"
        else -> "${digits.substring(0, 1)}.${digits.substring(1, 4)}.${digits.substring(4)}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetzScreen(
    onHomeClick: () -> Unit,
    viewModel: TesseraViewModel,
    petViewModel: PetViewModel
) {
    val context = LocalContext.current
    val pets by petViewModel.allPets.collectAsStateWithLifecycle()
    val petEvents by viewModel.allPetEvents.collectAsStateWithLifecycle()

    var selectedPetName by remember { mutableStateOf("Marie") }

    // Make sure we have a pet selected, defaulting to first if list is not empty
    val activePet = pets.find { it.name == selectedPetName } ?: pets.firstOrNull()
    if (activePet != null && selectedPetName != activePet.name) {
        selectedPetName = activePet.name
    }

    var petToEdit by remember { mutableStateOf<PetEntity?>(null) }
    var showAddRoutineDialog by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<PetEvent?>(null) }
    var healthCardToEdit by remember { mutableStateOf<String?>(null) }

    var showAddPetDialog by remember { mutableStateOf(false) }
    var isCreatingPet by remember { mutableStateOf(false) }
    var creationProgress by remember { mutableStateOf(0f) }
    var newPetNameForAnim by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(isCreatingPet) {
        if (isCreatingPet) {
            creationProgress = 0f
            val totalSteps = 100
            for (step in 1..totalSteps) {
                kotlinx.coroutines.delay(18)
                creationProgress = step.toFloat() / totalSteps
            }
            creationProgress = 1f
            kotlinx.coroutines.delay(800)
            isCreatingPet = false
        }
    }

    // Fetch weight history for this pet at the top level
    val weightHistory by if (activePet != null) {
        petViewModel.getWeightHistory(activePet.id)
            .collectAsStateWithLifecycle(initialValue = emptyList())
    } else {
        remember { mutableStateOf(emptyList<PetWeightHistoryEntity>()) }
    }
    val latestWeight = weightHistory.lastOrNull()?.weight?.toString() ?: "0.0"

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        if (activePet == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryTeal)
            }
        } else {
            val isMarie = activePet.name == "Marie"
            val accentColor = if (isMarie) TertiaryPurple else PrimaryTeal
            val ageStr = calculateAge(activePet.birthDate)

            // Convert file:// string to File for Coil to avoid permission caching issues
            val imageModel: Any = if (activePet.photoUri.startsWith("file://")) {
                File(Uri.parse(activePet.photoUri).path ?: "")
            } else {
                activePet.photoUri
            }

            var totalDrag by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pets, selectedPetName) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = {
                                if (totalDrag < -100f) { // Swipe esquerda -> próximo pet
                                    val idx = pets.indexOfFirst { it.name == selectedPetName }
                                    if (idx != -1 && pets.size > 1) {
                                        val nextIdx = (idx + 1) % pets.size
                                        selectedPetName = pets[nextIdx].name
                                    }
                                } else if (totalDrag > 100f) { // Swipe direita -> pet anterior
                                    val idx = pets.indexOfFirst { it.name == selectedPetName }
                                    if (idx != -1 && pets.size > 1) {
                                        val prevIdx = (idx - 1 + pets.size) % pets.size
                                        selectedPetName = pets[prevIdx].name
                                    }
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount
                            }
                        )
                    }
            ) {
                Crossfade(targetState = activePet, label = "PetzCrossfade", animationSpec = tween(700)) { currentPet ->
                val scrollState = rememberScrollState()
                val currentPetAgeStr = calculateAge(currentPet.birthDate)
                val currentPetImageModel: Any = if (currentPet.photoUri.startsWith("file://")) {
                    File(Uri.parse(currentPet.photoUri).path ?: "")
                } else {
                    currentPet.photoUri
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Hero Image with Fade to Black
                    val scrollOffset = scrollState.value
                    val baseBlur = (scrollOffset * 0.04f).coerceIn(0f, 16f)
                    val blurRadius = (if (isCreatingPet) baseBlur + 16f else baseBlur).dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                    ) {
                        key(currentPet.photoUri) {
                            AsyncImage(
                                model = currentPetImageModel,
                                contentDescription = currentPet.name,
                                contentScale = ContentScale.Crop,
                                colorFilter = if (currentPet.isAngel) {
                                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                                } else null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(blurRadius)
                            )
                        }
                        if (currentPet.isAngel) {
                            val auraTransition = rememberInfiniteTransition(label = "aura")
                            val auraPulse by auraTransition.animateFloat(
                                initialValue = 0.35f,
                                targetValue = 0.8f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(3000, easing = EaseInOutSine),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "auraPulse"
                            )
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFD700).copy(alpha = 0.3f * auraPulse),
                                            Color(0xFFFFD700).copy(alpha = 0.06f * auraPulse),
                                            Color.Transparent
                                        ),
                                        center = center,
                                        radius = size.width * 0.7f * auraPulse
                                    )
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f), Color.Black),
                                        startY = 200f
                                    )
                                )
                        )
                    }

                    // Main Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(scrollState)
                            .padding(top = 300.dp, start = 24.dp, end = 24.dp)
                    ) {
                        // Pet Profile Card (Glassmorphic Header Info)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = "${currentPet.breed.uppercase()}  •  ${currentPetAgeStr.uppercase()}",
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (currentPet.isAngel) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                            .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "👼 ANJO",
                                            color = Color(0xFFFFF7C2),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }

                            // Badges Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sex Badge
                                val sexColor = if (currentPet.sex == PetSex.MACHO) PrimaryTeal else TertiaryPurple
                                val sexText = if (currentPet.sex == PetSex.MACHO) "MACHO" else "FÊMEA"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(sexColor.copy(alpha = 0.15f))
                                        .border(0.5.dp, sexColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = sexText,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                // Castrated Badge
                                val castratedText = if (currentPet.isCastrated) "CASTRADO" else "NÃO CASTRADO"
                                val castratedColor = if (currentPet.isCastrated) accentColor else Color.Gray
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(castratedColor.copy(alpha = 0.12f))
                                        .border(0.5.dp, castratedColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = castratedText,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            if (currentPet.rga.isNotEmpty() || currentPet.microchip.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (currentPet.rga.isNotEmpty()) {
                                        Text(
                                            text = "RGA: ${formatRga(currentPet.rga)}",
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.DarkGray,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .border(0.5.dp, Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    if (currentPet.microchip.isNotEmpty()) {
                                        Text(
                                            text = "CHIP: ${currentPet.microchip}",
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.DarkGray,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .border(0.5.dp, Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Health Dashboard
                        Text(
                            text = "SAÚDE & VACINAS",
                            fontFamily = FontFamily.SansSerif,
                            color = accentColor,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val isV4Expired = petViewModel.isVaccineExpired(currentPet.lastV4VaccineDate)
                        val v4FormattedDate = currentPet.lastV4VaccineDate?.let {
                            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(it))
                        } ?: "Pendente"

                        val isRaivaExpired = petViewModel.isVaccineExpired(currentPet.lastRaivaVaccineDate)
                        val raivaFormattedDate = currentPet.lastRaivaVaccineDate?.let {
                            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(it))
                        } ?: "Pendente"

                        val isAntipulgasExpired = petViewModel.isAntipulgasExpired(currentPet.lastAntipulgasDate)
                        val antipulgasFormattedDate = currentPet.lastAntipulgasDate?.let {
                            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(it))
                        } ?: "Pendente"

                        val isVermifugoExpired = petViewModel.isVermifugoExpired(currentPet.lastVermifugoDate)
                        val vermifugoFormattedDate = currentPet.lastVermifugoDate?.let {
                            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(it))
                        } ?: "Pendente"

                        val isConsultaExpired = petViewModel.isConsultaExpired(currentPet.lastConsultaDate)
                        val consultaFormattedDate = currentPet.lastConsultaDate?.let {
                            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(it))
                        } ?: "Pendente"

                        PetHealthDashboard(
                            latestWeight = latestWeight,
                            isV4Expired = isV4Expired,
                            v4FormattedDate = v4FormattedDate,
                            isRaivaExpired = isRaivaExpired,
                            raivaFormattedDate = raivaFormattedDate,
                            isAntipulgasExpired = isAntipulgasExpired,
                            antipulgasFormattedDate = antipulgasFormattedDate,
                            isVermifugoExpired = isVermifugoExpired,
                            vermifugoFormattedDate = vermifugoFormattedDate,
                            isConsultaExpired = isConsultaExpired,
                            consultaFormattedDate = consultaFormattedDate,
                            notes = currentPet.notes,
                            accentColor = accentColor,
                            onWeightClick = { healthCardToEdit = "weight" },
                            onV4Click = {
                                val initialDate = currentPet.lastV4VaccineDate ?: System.currentTimeMillis()
                                showDatePicker(context, initialDate) { selectedDate ->
                                    petViewModel.insertPet(currentPet.copy(lastV4VaccineDate = selectedDate))
                                }
                            },
                            onRaivaClick = {
                                val initialDate = currentPet.lastRaivaVaccineDate ?: System.currentTimeMillis()
                                showDatePicker(context, initialDate) { selectedDate ->
                                    petViewModel.insertPet(currentPet.copy(lastRaivaVaccineDate = selectedDate))
                                }
                            },
                            onAntipulgasClick = {
                                val initialDate = currentPet.lastAntipulgasDate ?: System.currentTimeMillis()
                                showDatePicker(context, initialDate) { selectedDate ->
                                    petViewModel.insertPet(currentPet.copy(lastAntipulgasDate = selectedDate))
                                }
                            },
                            onVermifugoClick = {
                                val initialDate = currentPet.lastVermifugoDate ?: System.currentTimeMillis()
                                showDatePicker(context, initialDate) { selectedDate ->
                                    petViewModel.insertPet(currentPet.copy(lastVermifugoDate = selectedDate))
                                }
                            },
                            onConsultaClick = {
                                val initialDate = currentPet.lastConsultaDate ?: System.currentTimeMillis()
                                showDatePicker(context, initialDate) { selectedDate ->
                                    petViewModel.insertPet(currentPet.copy(lastConsultaDate = selectedDate))
                                }
                            },
                            onNotesClick = { healthCardToEdit = "notes" }
                        )

                        // WEIGHT HISTORY LINE CHART
                        Spacer(modifier = Modifier.height(24.dp))
                        PetWeightEvolutionChart(
                            weightHistory = weightHistory,
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        if (currentPet.isAngel) {
                            Text(
                                text = "MEMÓRIA ETERNA",
                                fontFamily = FontFamily.SansSerif,
                                color = accentColor,
                                fontSize = 12.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.02f))
                                    .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "👼",
                                        fontSize = 32.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "${currentPet.name} brilha agora como uma estrela eterna. Agradecemos por cada momento de amor e carinho compartilhados.",
                                        color = Color(0xFFFFF7C2),
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Light,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ROTINA DIÁRIA",
                                    fontFamily = FontFamily.SansSerif,
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { showAddRoutineDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", tint = accentColor)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val filteredEvents = petEvents.filter { it.petName == currentPet.name }
                            OuraTimeline(filteredEvents, accentColor, viewModel) { event ->
                                routineToEdit = event
                            }
                        }

                        Spacer(modifier = Modifier.height(120.dp))
                    }

                    // ------------------- TOP BAR & COLLAPSE LOGIC -------------------
                    val isCompact = scrollState.value > 180
                    val normalAlpha by animateFloatAsState(targetValue = if (isCompact) 0f else 1f, animationSpec = tween(250), label = "normalAlpha")
                    val compactAlpha by animateFloatAsState(targetValue = if (isCompact) 1f else 0f, animationSpec = tween(250), label = "compactAlpha")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                    ) {
                        // 1. Barra Normal (Seletor completo e botoes)
                        if (normalAlpha > 0.05f) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = normalAlpha
                                        scaleX = 0.92f + (normalAlpha * 0.08f)
                                        scaleY = 0.92f + (normalAlpha * 0.08f)
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.size(40.dp))

                                LiquidTabSelector(
                                    pets = pets,
                                    selectedPetName = selectedPetName,
                                    onPetSelected = { selectedPetName = it },
                                    accentColor = accentColor
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { petToEdit = currentPet },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onBackground)
                                    }

                                    IconButton(
                                        onClick = { showAddPetDialog = true },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Pet", tint = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        }

                        // 2. Barra Compacta (Foto e Nome brilhante centralizado)
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
                                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                key(currentPet.photoUri) {
                                    AsyncImage(
                                        model = currentPetImageModel,
                                        contentDescription = currentPet.name,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = if (currentPet.isAngel) {
                                            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                                        } else null,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, accentColor, CircleShape)
                                    )
                                }

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
                                        accentColor,
                                        Color.White,
                                        accentColor,
                                        Color.White
                                    ),
                                    start = Offset(shimmerOffset, 0f),
                                    end = Offset(shimmerOffset + 150f, 150f)
                                )

                                Text(
                                    text = currentPet.name,
                                    style = TextStyle(
                                        brush = nameGlowBrush,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Serif
                                    )
                                )
                            }
                        }
                    }

                    // Se for anjo, renderiza petalas caindo por cima de tudo
                    if (currentPet.isAngel) {
                        FallingPetalsAnimation()
                    }

                    // Animacao frontal de criacao
                    if (isCreatingPet) {
                        CreationOverlay(
                            progress = creationProgress,
                            petName = newPetNameForAnim,
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }
    }

    // Edit Pet Dialog
    if (petToEdit != null) {
        val activePetData = petToEdit!!
        val isM = activePetData.name == "Marie"
        val pColor = if (isM) TertiaryPurple else PrimaryTeal

        EditPetDialog(
            pet = activePetData,
            primaryColor = pColor,
            petViewModel = petViewModel,
            onDismiss = { petToEdit = null },
            onConfirm = { updatedPet ->
                petViewModel.insertPet(updatedPet)
                petToEdit = null
            },
            onDeleteClick = {
                showDeleteConfirmation = true
            }
        )
    }

    // Delete Pet Confirmation Dialog
    if (showDeleteConfirmation && activePet != null) {
        DeletePetConfirmationDialog(
            petName = activePet.name,
            accentColor = if (activePet.name == "Marie") TertiaryPurple else PrimaryTeal,
            onDismiss = { showDeleteConfirmation = false },
            onConfirmDelete = {
                petViewModel.deletePet(activePet)
                showDeleteConfirmation = false
                petToEdit = null
            },
            onConfirmAngel = {
                petViewModel.insertPet(activePet.copy(isAngel = true))
                showDeleteConfirmation = false
                petToEdit = null
            }
        )
    }

    // Add Pet Dialog
    if (showAddPetDialog) {
        val pColor = if (selectedPetName == "Marie") TertiaryPurple else PrimaryTeal
        AddPetDialog(
            primaryColor = pColor,
            petViewModel = petViewModel,
            onDismiss = { showAddPetDialog = false },
            onConfirm = { newPet, initialWeight ->
                showAddPetDialog = false
                newPetNameForAnim = newPet.name
                isCreatingPet = true
                petViewModel.insertPetWithInitialWeight(newPet, initialWeight) {
                    selectedPetName = newPet.name
                }
            }
        )
    }

    // Add Routine Dialog
    if (showAddRoutineDialog) {
        AddRoutineDialog(
            selectedPet = selectedPetName,
            onDismiss = { showAddRoutineDialog = false },
            onConfirm = { title, time ->
                viewModel.addPetEvent(selectedPetName, title, time)
                showAddRoutineDialog = false
            }
        )
    }

    // Edit Routine Dialog
    if (routineToEdit != null) {
        val eventToEdit = routineToEdit!!
        EditRoutineDialog(
            event = eventToEdit,
            onDismiss = { routineToEdit = null },
            onConfirm = { updatedTitle, updatedTime ->
                viewModel.updatePetEvent(eventToEdit.copy(title = updatedTitle, time = updatedTime))
                routineToEdit = null
            },
            onDelete = {
                viewModel.deletePetEvent(eventToEdit)
                routineToEdit = null
            }
        )
    }

    // Health Card Dialogs / Date Picker
    if (healthCardToEdit != null && activePet != null) {
        val category = healthCardToEdit!!
        val isM = activePet.name == "Marie"

        when (category) {
            "weight" -> {
                EditHealthCardDialog(
                    title = "Registrar Peso (kg)",
                    labelPlaceholder = "",
                    valuePlaceholder = "Ex: 28.5",
                    currentLabel = "",
                    currentValue = latestWeight,
                    hideLabelInput = true,
                    onDismiss = { healthCardToEdit = null },
                    onConfirm = { _, newValue ->
                        val weightVal = newValue.toDoubleClean()
                        if (weightVal != null) {
                            petViewModel.addWeightHistoryRecord(activePet.id, System.currentTimeMillis(), weightVal)
                        }
                        healthCardToEdit = null
                    }
                )
            }
            "notes" -> {
                EditHealthCardDialog(
                    title = "Notas / Alergias",
                    labelPlaceholder = "",
                    valuePlaceholder = "Ex: Nenhuma Alergia",
                    currentLabel = "",
                    currentValue = activePet.notes,
                    hideLabelInput = true,
                    onDismiss = { healthCardToEdit = null },
                    onConfirm = { _, newValue ->
                        petViewModel.insertPet(activePet.copy(notes = newValue))
                        healthCardToEdit = null
                    }
                )
            }
        }
    }
}

@Composable
fun OuraStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String = "",
    unit: String = "",
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Serif
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = " $unit",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                color = if (subtitle == "Reset / Pendente" || subtitle == "Expirada") Color(0xFFFF5252) else Color(0xFF888888),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun OuraTimeline(
    petEvents: List<PetEvent>,
    accentColor: Color,
    viewModel: TesseraViewModel,
    onEventClick: (PetEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (petEvents.isEmpty()) {
            Text(
                text = "Nenhuma rotina agendada.",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            petEvents.forEachIndexed { index, event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onEventClick(event) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (event.isCompleted) 0.dp else 1.dp,
                                color = if (event.isCompleted) Color.Transparent else Color.DarkGray,
                                shape = CircleShape
                            )
                            .background(if (event.isCompleted) accentColor else Color.Transparent)
                            .clickable { viewModel.togglePetEventCompleted(event) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (event.isCompleted) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            color = if (event.isCompleted) Color.Gray else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = event.time,
                            color = Color.DarkGray,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = { viewModel.deletePetEvent(event) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
fun EditRoutineDialog(
    event: PetEvent,
    onDismiss: () -> Unit,
    onConfirm: (title: String, time: String) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(event.title) }
    var time by remember { mutableStateOf(event.time) }
    var showTimePicker by remember { mutableStateOf(false) }
    val accentColor = if (event.petName == "Marie") TertiaryPurple else PrimaryTeal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Rotina",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Atividade") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                ) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Horário") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Select Time",
                                tint = accentColor
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.White,
                            disabledBorderColor = Color(0xFF333333),
                            disabledLabelColor = Color.Gray,
                            disabledLeadingIconColor = Color.Gray,
                            disabledTrailingIconColor = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, time) },
                enabled = title.trim().isNotEmpty() && time.isNotEmpty()
            ) {
                Text("SALVAR", color = if (title.trim().isNotEmpty() && time.isNotEmpty()) accentColor else Color.Gray, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDelete) {
                    Text("EXCLUIR", color = Color(0xFFFF5252), letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("CANCELAR", color = Color.Gray, letterSpacing = 1.sp)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(16.dp)
    )

    if (showTimePicker) {
        ClockTimePickerDialog(
            initialTime = time,
            accentColor = accentColor,
            onDismiss = { showTimePicker = false },
            onConfirm = { selectedTime ->
                time = selectedTime
                showTimePicker = false
            }
        )
    }
}

@Composable
fun EditPetDialog(
    pet: PetEntity,
    primaryColor: Color,
    petViewModel: PetViewModel,
    onDismiss: () -> Unit,
    onConfirm: (updatedPet: PetEntity) -> Unit,
    onDeleteClick: () -> Unit
) {
    var name by remember { mutableStateOf(pet.name) }
    var breed by remember { mutableStateOf(pet.breed) }
    var birthDate by remember { mutableStateOf(pet.birthDate) }
    var photoString by remember { mutableStateOf(pet.photoUri) }
    var rga by remember { mutableStateOf(pet.rga) }
    var microchip by remember { mutableStateOf(pet.microchip) }
    var sex by remember { mutableStateOf(pet.sex) }
    var isCastrated by remember { mutableStateOf(pet.isCastrated) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val fileName = "pet_photo_${name.lowercase()}_${System.currentTimeMillis()}.jpg"
                    val profileFile = java.io.File(context.filesDir, fileName)
                    profileFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val localUri = Uri.fromFile(profileFile)
                    photoString = localUri.toString()
                }
            } catch (e: Exception) {
                Log.e("PetzScreen", "Erro ao salvar foto do pet", e)
            }
        }
    }

    val rgaError = remember(rga) { rga.isNotEmpty() && !petViewModel.validateRga(rga) }
    val microchipError = remember(microchip) { microchip.isNotEmpty() && !petViewModel.validateMicrochip(microchip) }

    val canSave = name.isNotEmpty() && breed.isNotEmpty() &&
            petViewModel.validateRga(rga) && petViewModel.validateMicrochip(microchip)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar ${pet.name}",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val imageModel: Any = if (photoString.startsWith("file://")) {
                    File(Uri.parse(photoString).path ?: "")
                } else {
                    photoString
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoString.isNotEmpty()) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Outlined.Pets, contentDescription = "Add Photo", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    colors = themedOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Raça") },
                    colors = themedOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Picker for Birthdate
                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                val birthDateFormatted = dateFormat.format(Date(birthDate))

                OutlinedTextField(
                    value = birthDateFormatted,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data de Nascimento") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = primaryColor,
                            modifier = Modifier.clickable {
                                showDatePicker(context, birthDate) { selectedDate ->
                                    birthDate = selectedDate
                                }
                            }
                        )
                    },
                    colors = themedOutlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDatePicker(context, birthDate) { selectedDate ->
                                birthDate = selectedDate
                            }
                        }
                )

                // Sex selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sexo",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sex == PetSex.MACHO) PrimaryTeal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .border(
                                    width = 1.dp,
                                    color = if (sex == PetSex.MACHO) PrimaryTeal else themedCardBorder(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { sex = PetSex.MACHO }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MACHO", color = if (sex == PetSex.MACHO) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sex == PetSex.FEMEA) TertiaryPurple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .border(
                                    width = 1.dp,
                                    color = if (sex == PetSex.FEMEA) TertiaryPurple else themedCardBorder(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { sex = PetSex.FEMEA }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("FÊMEA", color = if (sex == PetSex.FEMEA) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Castrated selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Castrado",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCastrated) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .border(
                                    width = 1.dp,
                                    color = if (isCastrated) primaryColor else themedCardBorder(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { isCastrated = true }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sim", color = if (isCastrated) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isCastrated) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .border(
                                    width = 1.dp,
                                    color = if (!isCastrated) primaryColor else themedCardBorder(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { isCastrated = false }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Não", color = if (!isCastrated) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // RGA
                OutlinedTextField(
                    value = formatRga(rga),
                    onValueChange = { input ->
                        val cleaned = input.filter { it.isDigit() }.take(7)
                        rga = cleaned
                    },
                    label = { Text("RGA") },
                    isError = rgaError,
                    supportingText = {
                        if (rgaError) {
                            Text("Deve ter exatamente 7 dígitos numéricos.", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedLabelColor = primaryColor,
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Microchip
                OutlinedTextField(
                    value = microchip,
                    onValueChange = { microchip = it },
                    label = { Text("Microchip (ISO)") },
                    isError = microchipError,
                    supportingText = {
                        if (microchipError) {
                            Text("Deve ter exatamente 15 dígitos numéricos.", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedLabelColor = primaryColor,
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canSave) {
                        onConfirm(
                            pet.copy(
                                name = name,
                                breed = breed,
                                birthDate = birthDate,
                                photoUri = photoString,
                                rga = rga,
                                microchip = microchip,
                                sex = sex,
                                isCastrated = isCastrated
                            )
                        )
                    }
                },
                enabled = canSave
            ) {
                Text("SALVAR", color = if (canSave) primaryColor else Color.Gray, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDeleteClick) {
                    Text("EXCLUIR", color = Color(0xFFFF5252), letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("CANCELAR", color = Color.Gray, letterSpacing = 1.sp)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AddRoutineDialog(
    selectedPet: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, time: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("08:00") }
    var showTimePicker by remember { mutableStateOf(false) }
    val accentColor = if (selectedPet == "Marie") TertiaryPurple else PrimaryTeal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nova Rotina",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Atividade") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                ) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Horário") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Select Time",
                                tint = accentColor
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.White,
                            disabledBorderColor = Color(0xFF333333),
                            disabledLabelColor = Color.Gray,
                            disabledLeadingIconColor = Color.Gray,
                            disabledTrailingIconColor = accentColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, time) },
                enabled = title.trim().isNotEmpty() && time.isNotEmpty()
            ) {
                Text("ADICIONAR", color = if (title.trim().isNotEmpty() && time.isNotEmpty()) accentColor else Color.Gray, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Gray, letterSpacing = 1.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(16.dp)
    )

    if (showTimePicker) {
        ClockTimePickerDialog(
            initialTime = time,
            accentColor = accentColor,
            onDismiss = { showTimePicker = false },
            onConfirm = { selectedTime ->
                time = selectedTime
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockTimePickerDialog(
    initialTime: String = "08:00",
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialParts = initialTime.split(":")
    val initialHour = initialParts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = initialParts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
                    onConfirm(formattedTime)
                }
            ) {
                Text("OK", color = accentColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Gray)
            }
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = Color(0xFF151515),
                        clockDialSelectedContentColor = Color.Black,
                        clockDialUnselectedContentColor = Color.Gray,
                        selectorColor = accentColor,
                        periodSelectorBorderColor = Color.Gray,
                        periodSelectorSelectedContainerColor = accentColor.copy(alpha = 0.2f),
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = Color.White,
                        periodSelectorUnselectedContentColor = Color.Gray,
                        timeSelectorSelectedContainerColor = accentColor.copy(alpha = 0.2f),
                        timeSelectorUnselectedContainerColor = Color(0xFF151515),
                        timeSelectorSelectedContentColor = Color.White,
                        timeSelectorUnselectedContentColor = Color.Gray
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun EditHealthCardDialog(
    title: String,
    labelPlaceholder: String,
    valuePlaceholder: String,
    currentLabel: String,
    currentValue: String,
    hideLabelInput: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var label by remember { mutableStateOf(currentLabel) }
    var value by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!hideLabelInput) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(labelPlaceholder) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedLabelColor = PrimaryTeal,
                            unfocusedLabelColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(valuePlaceholder) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(label, value) }) {
                Text("SALVAR", color = PrimaryTeal, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Gray, letterSpacing = 1.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun PetWeightEvolutionChart(
    weightHistory: List<PetWeightHistoryEntity>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (weightHistory.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(themedCardBackground(), RoundedCornerShape(16.dp))
                .border(0.5.dp, themedCardBorder(), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Nenhum dado de peso disponível.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        return
    }

    val sortedHistory = remember(weightHistory) { weightHistory.sortedBy { it.date } }
    val weights = sortedHistory.map { it.weight }
    val maxWeight = (weights.maxOrNull() ?: 10.0) * 1.1
    val minWeight = (weights.minOrNull() ?: 0.0) * 0.9
    val weightRange = if (maxWeight == minWeight) 1.0 else maxWeight - minWeight

    val dates = sortedHistory.map { it.date }
    val maxDate = dates.maxOrNull() ?: 1L
    val minDate = dates.minOrNull() ?: 0L
    val dateRange = if (maxDate == minDate) 1L else maxDate - minDate

    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(weightHistory) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "EVOLUÇÃO DO PESO",
            fontFamily = FontFamily.SansSerif,
            color = accentColor,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val sysOnBackground = MaterialTheme.colorScheme.onBackground
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height
            val paddingLeft = 40f
            val paddingBottom = 40f
            val chartWidth = width - paddingLeft
            val chartHeight = height - paddingBottom

            // Draw Y Grid lines & labels
            val gridLines = 3
            for (i in 0..gridLines) {
                val ratio = i.toFloat() / gridLines
                val yVal = minWeight + ratio * weightRange
                val yPos = chartHeight - (ratio * chartHeight)

                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.3f),
                    start = Offset(paddingLeft, yPos),
                    end = Offset(width, yPos),
                    strokeWidth = 1f
                )

                drawContext.canvas.nativeCanvas.drawText(
                    String.format(Locale.US, "%.1f", yVal),
                    0f,
                    yPos + 8f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 24f
                        isAntiAlias = true
                    }
                )
            }

            // Map points to canvas coordinates
            val points = sortedHistory.mapIndexed { index, record ->
                val xPos = if (sortedHistory.size > 1) {
                    paddingLeft + ((record.date - minDate).toFloat() / dateRange) * (chartWidth - 40f)
                } else {
                    paddingLeft + chartWidth / 2f
                }
                val yRatio = (record.weight - minWeight) / weightRange
                val yPos = chartHeight - (yRatio.toFloat() * chartHeight * animationProgress.value)
                Offset(xPos, yPos)
            }

            // Draw fill area
            if (points.size > 1) {
                val fillPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points.first().x, chartHeight)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, chartHeight)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = chartHeight
                    )
                )
            }

            // Draw connecting lines
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = accentColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }

            // Draw data points & X-axis labels
            points.forEachIndexed { index, point ->
                drawCircle(
                    color = accentColor,
                    radius = 8f,
                    center = point
                )
                drawCircle(
                    color = sysOnBackground,
                    radius = 4f,
                    center = point
                )

                // Label X
                if (index == 0 || index == points.size - 1 || points.size <= 4) {
                    val dateStr = dateFormat.format(Date(sortedHistory[index].date))
                    drawContext.canvas.nativeCanvas.drawText(
                        dateStr,
                        point.x - 24f,
                        height - 8f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 22f
                            isAntiAlias = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HealthDashboardRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isWarning: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isWarning) Color(0xFFFF5252) else accentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                color = Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                color = if (isWarning) Color(0xFFFF5252) else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.DarkGray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun PetHealthDashboard(
    latestWeight: String,
    isV4Expired: Boolean,
    v4FormattedDate: String,
    isRaivaExpired: Boolean,
    raivaFormattedDate: String,
    isAntipulgasExpired: Boolean,
    antipulgasFormattedDate: String,
    isVermifugoExpired: Boolean,
    vermifugoFormattedDate: String,
    isConsultaExpired: Boolean,
    consultaFormattedDate: String,
    notes: String,
    accentColor: Color,
    onWeightClick: () -> Unit,
    onV4Click: () -> Unit,
    onRaivaClick: () -> Unit,
    onAntipulgasClick: () -> Unit,
    onVermifugoClick: () -> Unit,
    onConsultaClick: () -> Unit,
    onNotesClick: () -> Unit
) {
    var healthScore = 100
    if (isV4Expired) healthScore -= 15
    if (isRaivaExpired) healthScore -= 15
    if (isAntipulgasExpired) healthScore -= 15
    if (isVermifugoExpired) healthScore -= 15
    if (isConsultaExpired) healthScore -= 15
    val weightDouble = latestWeight.toDoubleCleanOrZero()
    if (weightDouble <= 0.0) healthScore -= 10
    healthScore = healthScore.coerceIn(10, 100)

    val targetSweepAngle = (healthScore / 100f) * 360f
    val animatedSweepAngle by animateFloatAsState(
        targetValue = targetSweepAngle,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "HealthScoreAnimation"
    )

    val (statusLabel, statusColor) = when {
        healthScore >= 90 -> "EXCELENTE" to PrimaryTeal
        healthScore >= 70 -> "BOM" to accentColor
        healthScore >= 50 -> "ATENÇÃO" to Color(0xFFFFB300)
        else -> "CRÍTICO" to Color(0xFFFF5252)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0F0F0F))
            .border(0.5.dp, Color(0xFF222222), RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = themedDivider()
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(statusColor, statusColor.copy(alpha = 0.4f), statusColor),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedSweepAngle,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$healthScore",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "VITALIDADE",
                        color = Color.Gray,
                        fontSize = 8.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusLabel,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HealthDashboardRow(
                icon = Icons.Default.MonitorWeight,
                label = "Peso",
                value = if (weightDouble <= 0.0) "Registrar" else "$latestWeight kg",
                isWarning = weightDouble <= 0.0,
                accentColor = accentColor,
                onClick = onWeightClick
            )
            HealthDashboardRow(
                icon = Icons.Default.Vaccines,
                label = "Vacina V4",
                value = if (isV4Expired) "Expirada" else v4FormattedDate,
                isWarning = isV4Expired,
                accentColor = accentColor,
                onClick = onV4Click
            )
            HealthDashboardRow(
                icon = Icons.Default.Vaccines,
                label = "Antirrábica",
                value = if (isRaivaExpired) "Expirada" else raivaFormattedDate,
                isWarning = isRaivaExpired,
                accentColor = accentColor,
                onClick = onRaivaClick
            )
            HealthDashboardRow(
                icon = Icons.Default.Shield,
                label = "Antipulgas",
                value = if (isAntipulgasExpired) "Expirado" else antipulgasFormattedDate,
                isWarning = isAntipulgasExpired,
                accentColor = accentColor,
                onClick = onAntipulgasClick
            )
            HealthDashboardRow(
                icon = Icons.Default.Medication,
                label = "Vermífugo",
                value = if (isVermifugoExpired) "Expirado" else vermifugoFormattedDate,
                isWarning = isVermifugoExpired,
                accentColor = accentColor,
                onClick = onVermifugoClick
            )
            HealthDashboardRow(
                icon = Icons.Default.MedicalServices,
                label = "Consulta",
                value = if (isConsultaExpired) "Expirada" else consultaFormattedDate,
                isWarning = isConsultaExpired,
                accentColor = accentColor,
                onClick = onConsultaClick
            )
            HealthDashboardRow(
                icon = Icons.Default.Description,
                label = "Alergias / Notas",
                value = if (notes.isNotEmpty()) notes else "Nenhuma",
                isWarning = false,
                accentColor = accentColor,
                onClick = onNotesClick
            )
        }
    }
}

// ---------------- NEW COMPOSABLES & ELASTIC LIQUID STUFF ----------------

@Composable
fun LiquidTabSelector(
    pets: List<PetEntity>,
    selectedPetName: String,
    onPetSelected: (String) -> Unit,
    accentColor: Color
) {
    val tabCoords = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val selectedIndex = remember(pets, selectedPetName) {
        pets.indexOfFirst { it.name == selectedPetName }.coerceAtLeast(0)
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val defaultWidth = with(density) { 80.dp.toPx() }
    val defaultX = 0f

    val targetX = tabCoords[selectedIndex]?.first ?: defaultX
    val targetW = tabCoords[selectedIndex]?.second ?: defaultWidth

    // Animação com mola macia e bouncy para movimento líquido
    val animX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = 200f
        ),
        label = "liquidX"
    )

    val animW by animateFloatAsState(
        targetValue = targetW,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 200f
        ),
        label = "liquidW"
    )

    // Esticamento líquido com base na distância restante
    val distance = targetX - animX
    val stretch = (distance * 0.22f).coerceIn(-40f, 40f)

    Box(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(30.dp))
            .padding(4.dp)
    ) {
        val indicatorWidthDp = with(density) { (animW + kotlin.math.abs(stretch)).toDp() }
        val indicatorOffsetDp = with(density) { (animX + if (stretch < 0f) stretch else 0f).toDp() }

        Box(
            modifier = Modifier
                .offset(x = indicatorOffsetDp, y = 0.dp)
                .width(indicatorWidthDp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            pets.forEachIndexed { index, petItem ->
                val isSelected = petItem.name == selectedPetName
                Text(
                    text = petItem.name,
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            val parent = coords.parentLayoutCoordinates
                            if (parent != null) {
                                val position = coords.positionInParent()
                                tabCoords[index] = Pair(position.x, coords.size.width.toFloat())
                            }
                        }
                        .clip(RoundedCornerShape(26.dp))
                        .clickable { onPetSelected(petItem.name) }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    color = if (isSelected) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

data class Petal(
    var x: Float,
    var y: Float,
    var size: Float,
    var speed: Float,
    var wind: Float,
    var angle: Float,
    var angleSpeed: Float,
    var phase: Float
)

fun createRandomPetal(initialY: Boolean): Petal {
    val random = java.util.Random()
    val initialYVal = if (initialY) random.nextFloat() * 2000f else -50f
    return Petal(
        x = random.nextFloat() * 1080f,
        y = initialYVal,
        size = 12f + random.nextFloat() * 16f,
        speed = 1.5f + random.nextFloat() * 3.5f,
        wind = -0.8f + random.nextFloat() * 1.6f,
        angle = random.nextFloat() * 360f,
        angleSpeed = -2f + random.nextFloat() * 4f,
        phase = random.nextFloat() * 10f
    )
}

@Composable
fun FallingPetalsAnimation() {
    val petals = remember {
        MutableList(15) { createRandomPetal(initialY = true) }
    }
    var lastTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastTime != 0L) {
                    val deltaMillis = (frameTimeNanos - lastTime) / 1_000_000f
                    val speedFactor = (deltaMillis / 16.67f).coerceIn(0.1f, 3.0f)
                    petals.forEachIndexed { i, petal ->
                        petal.y += petal.speed * speedFactor
                        petal.angle += petal.angleSpeed * speedFactor
                        petal.x += (petal.wind + (kotlin.math.sin(petal.y * 0.008 + petal.phase) * 0.6f).toFloat()) * speedFactor

                        if (petal.y > 2200f || petal.x < -100f || petal.x > 1200f) {
                            petals[i] = createRandomPetal(initialY = false)
                        }
                    }
                }
                lastTime = frameTimeNanos
            }
        }
    }

    val sharedPath = remember { androidx.compose.ui.graphics.Path() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val tick = lastTime
        val width = size.width
        petals.forEach { petal ->
            val drawX = (petal.x % width + width) % width
            val drawY = petal.y

            drawContext.canvas.save()
            drawContext.canvas.translate(drawX, drawY)
            drawContext.canvas.rotate(petal.angle)

            sharedPath.reset()
            sharedPath.moveTo(0f, -petal.size)
            sharedPath.quadraticTo(petal.size * 0.6f, -petal.size * 0.2f, 0f, petal.size)
            sharedPath.quadraticTo(-petal.size * 0.6f, -petal.size * 0.2f, 0f, -petal.size)
            sharedPath.close()

            drawPath(
                path = sharedPath,
                color = Color(0xFFFFC0CB).copy(alpha = 0.5f) // Rosa claro translúcido
            )
            drawContext.canvas.restore()
        }
    }
}

@Composable
fun DeletePetConfirmationDialog(
    petName: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
    onConfirmAngel: () -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Excluir Perfil de $petName",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Lamentamos que precise remover o perfil. Qual o motivo da remoção?",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                
                val reasons = listOf(
                    "Doação ou Novo Lar",
                    "Falecimento (Luto)",
                    "Outros Motivos"
                )
                
                reasons.forEach { reason ->
                    val isSelected = selectedReason == reason
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .border(1.dp, if (isSelected) accentColor else themedCardBorder(), RoundedCornerShape(8.dp))
                            .clickable { selectedReason = reason }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reason,
                            color = if (isSelected) Color.White else Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedReason == "Falecimento (Luto)") {
                        onConfirmAngel()
                    } else {
                        onConfirmDelete()
                    }
                },
                enabled = selectedReason.isNotEmpty()
            ) {
                Text(
                    text = "CONFIRMAR",
                    color = if (selectedReason.isNotEmpty()) accentColor else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Gray)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetDialog(
    primaryColor: Color,
    petViewModel: PetViewModel,
    onDismiss: () -> Unit,
    onConfirm: (newPet: PetEntity, initialWeight: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var photoString by remember { mutableStateOf("") }
    var rga by remember { mutableStateOf("") }
    var microchip by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(PetSex.MACHO) }
    var isCastrated by remember { mutableStateOf(false) }
    var initialWeightStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val fileName = "pet_photo_${name.lowercase().ifEmpty { "new" }}_${System.currentTimeMillis()}.jpg"
                    val profileFile = java.io.File(context.filesDir, fileName)
                    profileFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val localUri = Uri.fromFile(profileFile)
                    photoString = localUri.toString()
                }
            } catch (e: Exception) {
                Log.e("PetzScreen", "Erro ao salvar foto do pet", e)
            }
        }
    }

    val rgaError = remember(rga) { rga.isNotEmpty() && !petViewModel.validateRga(rga) }
    val microchipError = remember(microchip) { microchip.isNotEmpty() && !petViewModel.validateMicrochip(microchip) }
    val initialWeight = initialWeightStr.toDoubleCleanOrZero()
    val canSave = name.trim().isNotEmpty() && breed.trim().isNotEmpty() &&
            (rga.isEmpty() || petViewModel.validateRga(rga)) &&
            (microchip.isEmpty() || petViewModel.validateMicrochip(microchip))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Adicionar Novo Pet",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val imageModel: Any = if (photoString.startsWith("file://")) {
                    File(Uri.parse(photoString).path ?: "")
                } else {
                    photoString
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF111111))
                        .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoString.isNotEmpty()) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Outlined.Pets, contentDescription = "Add Photo", tint = Color.DarkGray, modifier = Modifier.size(36.dp))
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    colors = themedOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Raça") },
                    colors = themedOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                val birthDateFormatted = dateFormat.format(Date(birthDate))

                OutlinedTextField(
                    value = birthDateFormatted,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data de Nascimento") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = primaryColor,
                            modifier = Modifier.clickable {
                                showDatePicker(context, birthDate) { selectedDate ->
                                    birthDate = selectedDate
                                }
                            }
                        )
                    },
                    colors = themedOutlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDatePicker(context, birthDate) { selectedDate ->
                                birthDate = selectedDate
                            }
                        }
                )

                OutlinedTextField(
                    value = initialWeightStr,
                    onValueChange = { initialWeightStr = it },
                    label = { Text("Peso Inicial (kg)") },
                    colors = themedOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sexo",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sex == PetSex.MACHO) PrimaryTeal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .border(
                                    width = 1.dp,
                                    color = if (sex == PetSex.MACHO) PrimaryTeal else themedCardBorder(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { sex = PetSex.MACHO }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MACHO", color = if (sex == PetSex.MACHO) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sex == PetSex.FEMEA) TertiaryPurple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .border(
                                    width = 1.dp,
                                    color = if (sex == PetSex.FEMEA) TertiaryPurple else themedCardBorder(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { sex = PetSex.FEMEA }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("FÊMEA", color = if (sex == PetSex.FEMEA) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Castrado",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCastrated) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .border(
                                    width = 1.dp,
                                    color = if (isCastrated) primaryColor else themedCardBorder(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { isCastrated = true }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sim", color = if (isCastrated) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isCastrated) primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .border(
                                    width = 1.dp,
                                    color = if (!isCastrated) primaryColor else themedCardBorder(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { isCastrated = false }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Não", color = if (!isCastrated) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = formatRga(rga),
                    onValueChange = { input: String ->
                        val cleaned = input.filter { it.isDigit() }.take(7)
                        rga = cleaned
                    },
                    label = { Text("RGA (Opcional)") },
                    isError = rgaError,
                    supportingText = if (rgaError) {
                        { Text("Deve ter exatamente 7 dígitos numéricos.", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    colors = themedOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = microchip,
                    onValueChange = { microchip = it },
                    label = { Text("Microchip (ISO, Opcional)") },
                    isError = microchipError,
                    supportingText = {
                        if (microchipError) {
                            Text("Deve ter exatamente 15 dígitos numéricos.", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = themedOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Alergias") },
                    colors = themedOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (canSave) {
                        val newPet = PetEntity(
                            name = name,
                            breed = breed,
                            birthDate = birthDate,
                            photoUri = photoString,
                            rga = rga,
                            microchip = microchip,
                            sex = sex,
                            isCastrated = isCastrated,
                            notes = notes,
                            lastV4VaccineDate = null,
                            lastRaivaVaccineDate = null,
                            isAngel = false
                        )
                        onConfirm(newPet, initialWeight)
                    }
                },
                enabled = canSave
            ) {
                Text("CRIAR", color = if (canSave) primaryColor else Color.Gray, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Gray, letterSpacing = 1.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(16.dp)
    )
}

data class GlowParticle(
    val angle: Double,
    val distance: Float,
    val size: Float,
    val color: Color,
    val speed: Float
)

@Composable
fun CreationOverlay(
    progress: Float,
    petName: String,
    accentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "creationGlow")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val particles = remember {
        List(25) {
            val angle = Math.random() * 2 * Math.PI
            val distance = 120f + (Math.random() * 80f).toFloat()
            val size = 4f + (Math.random() * 8f).toFloat()
            val speed = 0.5f + (Math.random() * 1.5f).toFloat()
            val color = if (Math.random() > 0.5) accentColor else Color.White
            GlowParticle(angle, distance, size, color, speed)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = this.center
                    
                    rotate(rotation) {
                        drawArc(
                            color = accentColor.copy(alpha = 0.15f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 16f, cap = StrokeCap.Round)
                        )
                        
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(accentColor, Color.White, accentColor.copy(alpha = 0.2f), accentColor),
                                center = center
                            ),
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(width = 8f, cap = StrokeCap.Round)
                        )
                    }

                    particles.forEach { p ->
                        val currentAngle = p.angle + (rotation * Math.PI / 180f) * p.speed
                        val currentDistance = p.distance * (1f - progress * 0.7f)
                        
                        val x = center.x + (currentDistance * kotlin.math.cos(currentAngle)).toFloat()
                        val y = center.y + (currentDistance * kotlin.math.sin(currentAngle)).toFloat()
                        
                        val finalX = if (progress >= 1f) {
                            center.x + ((p.distance * 1.5f) * kotlin.math.cos(p.angle)).toFloat()
                        } else x

                        val finalY = if (progress >= 1f) {
                            center.y + ((p.distance * 1.5f) * kotlin.math.sin(p.angle)).toFloat()
                        } else y

                        val alphaValue = if (progress >= 1f) {
                            (1.2f - progress).coerceIn(0f, 1f)
                        } else 0.8f

                        drawCircle(
                            color = p.color.copy(alpha = alphaValue),
                            radius = if (progress >= 1f) p.size * 0.5f else p.size,
                            center = Offset(finalX, finalY)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(themedOverlayBackground())
                        .border(1.dp, themedCardBorder(), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.3f + progress * 0.7f),
                        modifier = Modifier.size((48f + progress * 12f).dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "${(progress * 100).toInt()}%",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Serif
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val statusMsg = when {
                progress < 0.35f -> "Iniciando perfil de $petName..."
                progress < 0.70f -> "Configurando ambiente e vacinas..."
                progress < 0.98f -> "Finalizando cadastro do pet..."
                else -> "Perfil construído! ✨"
            }
            
            Text(
                text = statusMsg.uppercase(),
                color = accentColor,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
