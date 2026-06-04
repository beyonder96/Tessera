package com.example

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    return when {
        age <= 0 -> {
            val months = now.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH) +
                    (if (now.get(Calendar.DAY_OF_MONTH) < birthCal.get(Calendar.DAY_OF_MONTH)) -1 else 0)
            if (months <= 0) "Recém-nascido" else "$months meses"
        }
        age == 1 -> "1 ano"
        else -> "$age anos"
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
    var healthCardToEdit by remember { mutableStateOf<String?>(null) }

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
                    val blurRadius = (scrollOffset * 0.04f).coerceIn(0f, 16f).dp

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                    ) {
                        AsyncImage(
                            model = currentPetImageModel,
                            contentDescription = currentPet.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(blurRadius)
                        )
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
                            Text(
                                text = currentPet.name,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Light,
                                fontSize = 42.sp,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "${currentPet.breed.uppercase()}  •  ${currentPetAgeStr.uppercase()}",
                                fontFamily = FontFamily.SansSerif,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

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
                                        color = Color.White,
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
                                        color = Color.White,
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
                                            text = "RGA: ${currentPet.rga}",
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

                        PetHealthDashboard(
                            latestWeight = latestWeight,
                            isV4Expired = isV4Expired,
                            v4FormattedDate = v4FormattedDate,
                            isRaivaExpired = isRaivaExpired,
                            raivaFormattedDate = raivaFormattedDate,
                            notes = currentPet.notes,
                            accentColor = accentColor,
                            onWeightClick = { healthCardToEdit = "weight" },
                            onV4Click = { healthCardToEdit = "vaccine_v4" },
                            onRaivaClick = { healthCardToEdit = "vaccine_raiva" },
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
                        OuraTimeline(filteredEvents, accentColor, viewModel)

                        Spacer(modifier = Modifier.height(120.dp))
                    }

                    // Top Navigation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onHomeClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Outlined.Home, contentDescription = "Home", tint = Color.White)
                        }

                        // Tab Selector
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(4.dp)
                        ) {
                            pets.forEach { petItem ->
                                val isSelected = petItem.name == selectedPetName
                                Text(
                                    text = petItem.name,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { selectedPetName = petItem.name }
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { petToEdit = currentPet },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color.White)
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

    // Health Card Dialogs / Date Picker
    if (healthCardToEdit != null && activePet != null) {
        val category = healthCardToEdit!!
        val isM = activePet.name == "Marie"

        when (category) {
            "vaccine_v4" -> {
                val initialDate = activePet.lastV4VaccineDate ?: System.currentTimeMillis()
                showDatePicker(context, initialDate) { selectedDate ->
                    petViewModel.insertPet(activePet.copy(lastV4VaccineDate = selectedDate))
                    healthCardToEdit = null
                }
            }
            "vaccine_raiva" -> {
                val initialDate = activePet.lastRaivaVaccineDate ?: System.currentTimeMillis()
                showDatePicker(context, initialDate) { selectedDate ->
                    petViewModel.insertPet(activePet.copy(lastRaivaVaccineDate = selectedDate))
                    healthCardToEdit = null
                }
            }
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
                        val weightVal = newValue.toDoubleOrNull()
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
            .background(Color(0xFF111111))
            .border(0.5.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
            .bounceClick(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = Color.White,
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
fun OuraTimeline(petEvents: List<PetEvent>, accentColor: Color, viewModel: TesseraViewModel) {
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
                        .bounceClick { viewModel.togglePetEventCompleted(event) }
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
                            .background(if (event.isCompleted) accentColor else Color.Transparent),
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
fun EditPetDialog(
    pet: PetEntity,
    primaryColor: Color,
    petViewModel: PetViewModel,
    onDismiss: () -> Unit,
    onConfirm: (updatedPet: PetEntity) -> Unit
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
                e.printStackTrace()
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
                color = Color.White
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

                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Raça") },
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color(0xFF333333),
                        focusedLabelColor = primaryColor,
                        unfocusedLabelColor = Color.Gray
                    ),
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
                        color = Color.Gray,
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
                                .background(if (sex == PetSex.MACHO) PrimaryTeal.copy(alpha = 0.2f) else Color(0xFF111111))
                                .border(
                                    width = 1.dp,
                                    color = if (sex == PetSex.MACHO) PrimaryTeal else Color(0xFF333333),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { sex = PetSex.MACHO }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MACHO", color = if (sex == PetSex.MACHO) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sex == PetSex.FEMEA) TertiaryPurple.copy(alpha = 0.2f) else Color(0xFF111111))
                                .border(
                                    width = 1.dp,
                                    color = if (sex == PetSex.FEMEA) TertiaryPurple else Color(0xFF333333),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { sex = PetSex.FEMEA }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("FÊMEA", color = if (sex == PetSex.FEMEA) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Castrated selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Castrado",
                        color = Color.Gray,
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
                                .background(if (isCastrated) primaryColor.copy(alpha = 0.2f) else Color(0xFF111111))
                                .border(
                                    width = 1.dp,
                                    color = if (isCastrated) primaryColor else Color(0xFF333333),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { isCastrated = true }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sim", color = if (isCastrated) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isCastrated) primaryColor.copy(alpha = 0.2f) else Color(0xFF111111))
                                .border(
                                    width = 1.dp,
                                    color = if (!isCastrated) primaryColor else Color(0xFF333333),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { isCastrated = false }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Não", color = if (!isCastrated) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // RGA
                OutlinedTextField(
                    value = rga,
                    onValueChange = { rga = it },
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
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Gray, letterSpacing = 1.sp)
            }
        },
        containerColor = Color(0xFF0A0A0A),
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
                color = Color.White
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
        containerColor = Color(0xFF0A0A0A),
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
        containerColor = Color(0xFF0C0C0C),
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
                color = Color.White
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
        containerColor = Color(0xFF0A0A0A),
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
                .background(Color(0xFF111111), RoundedCornerShape(16.dp))
                .border(0.5.dp, Color(0xFF222222), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Nenhum dado de peso disponível.", color = Color.Gray, fontSize = 14.sp)
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111111))
            .border(0.5.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
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
                val yPos = chartHeight - (yRatio.toFloat() * chartHeight)
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
                    color = Color.White,
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
    notes: String,
    accentColor: Color,
    onWeightClick: () -> Unit,
    onV4Click: () -> Unit,
    onRaivaClick: () -> Unit,
    onNotesClick: () -> Unit
) {
    var healthScore = 100
    if (isV4Expired) healthScore -= 30
    if (isRaivaExpired) healthScore -= 30
    val weightDouble = latestWeight.toDoubleOrNull() ?: 0.0
    if (weightDouble <= 0.0) healthScore -= 15
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
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    drawArc(
                        color = Color(0xFF1E1E1E),
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
                        color = Color.White,
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
