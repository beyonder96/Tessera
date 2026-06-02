package com.example

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.TesseraViewModel
import com.example.data.PetEvent
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.TertiaryPurple
import com.example.ui.components.bounceClick
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetzScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE) }
    
    val petEvents by viewModel.allPetEvents.collectAsStateWithLifecycle()
    var selectedPet by remember { mutableStateOf("Marie") }

    // State for Marie
    var marieBreed by remember { mutableStateOf(sharedPrefs.getString("marie_breed", "Golden Retriever") ?: "Golden Retriever") }
    var marieAge by remember { mutableStateOf(sharedPrefs.getString("marie_age", "4 anos") ?: "4 anos") }
    var mariePhoto by remember { mutableStateOf(sharedPrefs.getString("marie_photo", "https://lh3.googleusercontent.com/aida-public/AB6AXuC-nfJPLwsDCoZAPRnyoFfm-kb7-YGKFlZERj6GnvfsPRWF04QUeCIX1WhZHhCQLUF4_4wKhJZZ_Pjz7Q86FxU0IpCdNNwQFjU5MHMRrs5lQl4cD1DJTeYqV574VjOoD3xOAusiBniyTZI0VWBYGbhi0NUc57PSZP_6rU7yVmXK85XXkeVqYgYA6Z_-kIeU4PINEX9lZBUfcgobmRvse9pFNN-27sq-IuJzPyavZxsCKJk7pXdnHy5vLrP8xPsnWkGmCE1VhtBiXRw") ?: "") }

    // State for Churchill
    var churchillBreed by remember { mutableStateOf(sharedPrefs.getString("churchill_breed", "Buldogue Francês") ?: "Buldogue Francês") }
    var churchillAge by remember { mutableStateOf(sharedPrefs.getString("churchill_age", "2 anos") ?: "2 anos") }
    var churchillPhoto by remember { mutableStateOf(sharedPrefs.getString("churchill_photo", "https://lh3.googleusercontent.com/aida-public/AB6AXuBwE9mkw-3Q01XMMJNCBsgQYL4vceyVCaIpNVZLlNpqFxq56lIYShGa2Y2Ayd2cWilSsA1Sh7N8EhEeP0UmPiTX1Jxrt5v-bwMd7go8hp_GMPk-ujDr-jURbRlfoI92fsudTavmulIvwmwVFRX5oy5pq4tLAm0ouBfSkwAy2knOwtJPymqKdo2ZhqgGc_eH8IPceKSvI0ugGLLmnBGc5BIGL9mwFb4JUYULZY9PQ4BuBWZGmIU3n7lN0G86yPzXd3Zi58hh3NsMgjw") ?: "") }

    // Health States
    var marieVaccine by remember { mutableStateOf(sharedPrefs.getString("marie_vaccine_name", "V8") ?: "V8") }
    var marieVaccineDate by remember { mutableStateOf(sharedPrefs.getString("marie_vaccine_date", "12/05") ?: "12/05") }
    var marieWeight by remember { mutableStateOf(sharedPrefs.getString("marie_weight", "28.5") ?: "28.5") }
    var marieAppointment by remember { mutableStateOf(sharedPrefs.getString("marie_appointment", "Check-up Geral") ?: "Check-up Geral") }
    var marieAppointmentDate by remember { mutableStateOf(sharedPrefs.getString("marie_appointment_date", "12/Jun") ?: "12/Jun") }
    var marieNotes by remember { mutableStateOf(sharedPrefs.getString("marie_notes", "Nenhuma Alergia") ?: "Nenhuma Alergia") }

    var churchillVaccine by remember { mutableStateOf(sharedPrefs.getString("churchill_vaccine_name", "Antirrábica") ?: "Antirrábica") }
    var churchillVaccineDate by remember { mutableStateOf(sharedPrefs.getString("churchill_vaccine_date", "24/05") ?: "24/05") }
    var churchillWeight by remember { mutableStateOf(sharedPrefs.getString("churchill_weight", "12.2") ?: "12.2") }
    var churchillAppointment by remember { mutableStateOf(sharedPrefs.getString("churchill_appointment", "Consulta Oftalmo") ?: "Consulta Oftalmo") }
    var churchillAppointmentDate by remember { mutableStateOf(sharedPrefs.getString("churchill_appointment_date", "24/Jun") ?: "24/Jun") }
    var churchillNotes by remember { mutableStateOf(sharedPrefs.getString("churchill_notes", "Dieta de Controle") ?: "Dieta de Controle") }

    var petToEdit by remember { mutableStateOf<String?>(null) }
    var showAddRoutineDialog by remember { mutableStateOf(false) }
    var healthCardToEdit by remember { mutableStateOf<String?>(null) } 

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Crossfade(targetState = selectedPet, label = "PetzCrossfade", animationSpec = tween(700)) { pet ->
            val isMarie = pet == "Marie"
            val breed = if (isMarie) marieBreed else churchillBreed
            val age = if (isMarie) marieAge else churchillAge
            val photo = if (isMarie) mariePhoto else churchillPhoto
            val accentColor = if (isMarie) TertiaryPurple else PrimaryTeal
            val activeVaccine = if (isMarie) marieVaccine else churchillVaccine
            val activeVaccineDate = if (isMarie) marieVaccineDate else churchillVaccineDate
            val activeWeight = if (isMarie) marieWeight else churchillWeight
            val activeAppointment = if (isMarie) marieAppointment else churchillAppointment
            val activeAppointmentDate = if (isMarie) marieAppointmentDate else churchillAppointmentDate
            val activeNotes = if (isMarie) marieNotes else churchillNotes
            
            // Convert file:// string to File for Coil to avoid permission caching issues
            val imageModel: Any = if (photo.startsWith("file://")) File(Uri.parse(photo).path ?: "") else photo

            Box(modifier = Modifier.fillMaxSize()) {
                // Hero Image with Fade to Black
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                ) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = pet,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
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
                        Text(
                            text = "Marie",
                            modifier = Modifier
                                .clip(RoundedCornerShape(26.dp))
                                .background(if (isMarie) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedPet = "Marie" }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            color = if (isMarie) Color.White else Color.Gray,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Churchill",
                            modifier = Modifier
                                .clip(RoundedCornerShape(26.dp))
                                .background(if (!isMarie) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedPet = "Churchill" }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            color = if (!isMarie) Color.White else Color.Gray,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                    
                    IconButton(
                        onClick = { petToEdit = pet },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                }

                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 300.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text(
                        text = pet,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Light,
                        fontSize = 48.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "$breed  •  $age",
                        fontFamily = FontFamily.SansSerif,
                        color = Color.Gray,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Readiness / Oura Style Health Ring
                    Text(
                        text = "HEALTH",
                        fontFamily = FontFamily.SansSerif,
                        color = accentColor,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OuraStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Peso",
                            value = activeWeight,
                            unit = "kg",
                            accentColor = accentColor,
                            onClick = { healthCardToEdit = "weight" }
                        )
                        OuraStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Vacina",
                            value = activeVaccineDate,
                            subtitle = activeVaccine,
                            accentColor = accentColor,
                            onClick = { healthCardToEdit = "vaccine" }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OuraStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Consulta",
                            value = activeAppointmentDate,
                            subtitle = activeAppointment,
                            accentColor = accentColor,
                            onClick = { healthCardToEdit = "appointment" }
                        )
                        OuraStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Alergias",
                            value = "Ver",
                            subtitle = activeNotes,
                            accentColor = accentColor,
                            onClick = { healthCardToEdit = "notes" }
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY's ROUTINE",
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

                    val filteredEvents = petEvents.filter { it.petName == pet }
                    OuraTimeline(filteredEvents, accentColor, viewModel)

                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }

    // Dialogs remain functional
    if (petToEdit != null) {
        val name = petToEdit!!
        val isM = name == "Marie"
        val breed = if (isM) marieBreed else churchillBreed
        val age = if (isM) marieAge else churchillAge
        val photo = if (isM) mariePhoto else churchillPhoto
        val pColor = if (isM) TertiaryPurple else PrimaryTeal

        EditPetDialog(
            petName = name,
            currentBreed = breed,
            currentAge = age,
            currentPhoto = photo,
            primaryColor = pColor,
            onDismiss = { petToEdit = null },
            onConfirm = { newBreed, newAge, newPhoto ->
                if (isM) {
                    marieBreed = newBreed
                    marieAge = newAge
                    mariePhoto = newPhoto
                    sharedPrefs.edit()
                        .putString("marie_breed", newBreed)
                        .putString("marie_age", newAge)
                        .putString("marie_photo", newPhoto)
                        .apply()
                } else {
                    churchillBreed = newBreed
                    churchillAge = newAge
                    churchillPhoto = newPhoto
                    sharedPrefs.edit()
                        .putString("churchill_breed", newBreed)
                        .putString("churchill_age", newAge)
                        .putString("churchill_photo", newPhoto)
                        .apply()
                }
                petToEdit = null
            }
        )
    }

    if (showAddRoutineDialog) {
        AddRoutineDialog(
            selectedPet = selectedPet,
            onDismiss = { showAddRoutineDialog = false },
            onConfirm = { title, time ->
                viewModel.addPetEvent(selectedPet, title, time)
                showAddRoutineDialog = false
            }
        )
    }

    if (healthCardToEdit != null) {
        val category = healthCardToEdit!!
        val isM = selectedPet == "Marie"
        
        val currentTitle = when (category) {
            "vaccine" -> "Última Vacina"
            "weight" -> "Peso Registrado (kg)"
            "appointment" -> "Consulta Veterinária"
            else -> "Dieta / Alergias"
        }
        val currentLabelVal = when (category) {
            "vaccine" -> if (isM) marieVaccine else churchillVaccine
            "weight" -> selectedPet
            "appointment" -> if (isM) marieAppointment else churchillAppointment
            else -> "Observações"
        }
        val currentValueVal = when (category) {
            "vaccine" -> if (isM) marieVaccineDate else churchillVaccineDate
            "weight" -> if (isM) marieWeight else churchillWeight
            "appointment" -> if (isM) marieAppointmentDate else churchillAppointmentDate
            else -> if (isM) marieNotes else churchillNotes
        }

        EditHealthCardDialog(
            title = currentTitle,
            labelPlaceholder = when (category) {
                "vaccine" -> "Nome da Vacina (ex: V10)"
                "weight" -> "Pet"
                "appointment" -> "Motivo (ex: Check-up)"
                else -> "Título"
            },
            valuePlaceholder = when (category) {
                "vaccine" -> "Data da Dose (ex: 12/Jun)"
                "weight" -> "Peso em kg (ex: 28.5)"
                "appointment" -> "Data (ex: 24/Jun)"
                else -> "Notas (ex: Alergia a Frango)"
            },
            currentLabel = currentLabelVal,
            currentValue = currentValueVal,
            hideLabelInput = category == "weight" || category == "notes",
            onDismiss = { healthCardToEdit = null },
            onConfirm = { newLabel, newValue ->
                if (isM) {
                    when (category) {
                        "vaccine" -> {
                            marieVaccine = newLabel; marieVaccineDate = newValue
                            sharedPrefs.edit().putString("marie_vaccine_name", newLabel).putString("marie_vaccine_date", newValue).apply()
                        }
                        "weight" -> {
                            marieWeight = newValue
                            sharedPrefs.edit().putString("marie_weight", newValue).apply()
                        }
                        "appointment" -> {
                            marieAppointment = newLabel; marieAppointmentDate = newValue
                            sharedPrefs.edit().putString("marie_appointment", newLabel).putString("marie_appointment_date", newValue).apply()
                        }
                        "notes" -> {
                            marieNotes = newValue
                            sharedPrefs.edit().putString("marie_notes", newValue).apply()
                        }
                    }
                } else {
                    when (category) {
                        "vaccine" -> {
                            churchillVaccine = newLabel; churchillVaccineDate = newValue
                            sharedPrefs.edit().putString("churchill_vaccine_name", newLabel).putString("churchill_vaccine_date", newValue).apply()
                        }
                        "weight" -> {
                            churchillWeight = newValue
                            sharedPrefs.edit().putString("churchill_weight", newValue).apply()
                        }
                        "appointment" -> {
                            churchillAppointment = newLabel; churchillAppointmentDate = newValue
                            sharedPrefs.edit().putString("churchill_appointment", newLabel).putString("churchill_appointment_date", newValue).apply()
                        }
                        "notes" -> {
                            churchillNotes = newValue
                            sharedPrefs.edit().putString("churchill_notes", newValue).apply()
                        }
                    }
                }
                healthCardToEdit = null
            }
        )
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
                color = Color(0xFF888888),
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
                text = "No routines scheduled.",
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
    petName: String,
    currentBreed: String,
    currentAge: String,
    currentPhoto: String,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (breed: String, age: String, photo: String) -> Unit
) {
    var breed by remember { mutableStateOf(currentBreed) }
    var age by remember { mutableStateOf(currentAge) }
    var photoString by remember { mutableStateOf(currentPhoto) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val fileName = "pet_photo_${petName.lowercase()}_${System.currentTimeMillis()}.jpg"
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit $petName",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val imageModel: Any = if (photoString.startsWith("file://")) File(Uri.parse(photoString).path ?: "") else photoString
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
                            contentDescription = petName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Outlined.Pets, contentDescription = "Add Photo", tint = Color.DarkGray, modifier = Modifier.size(36.dp))
                    }
                }

                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Breed") },
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
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
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
            TextButton(onClick = { onConfirm(breed, age, photoString) }) {
                Text("SAVE", color = primaryColor, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray, letterSpacing = 1.sp)
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
    var time by remember { mutableStateOf("") }
    val accentColor = if (selectedPet == "Marie") TertiaryPurple else PrimaryTeal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Routine",
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
                    label = { Text("Activity") },
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

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time") },
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
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, time) }) {
                Text("ADD", color = accentColor, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray, letterSpacing = 1.sp)
            }
        },
        containerColor = Color(0xFF0A0A0A),
        shape = RoundedCornerShape(16.dp)
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
                Text("SAVE", color = PrimaryTeal, letterSpacing = 1.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.Gray, letterSpacing = 1.sp)
            }
        },
        containerColor = Color(0xFF0A0A0A),
        shape = RoundedCornerShape(16.dp)
    )
}
