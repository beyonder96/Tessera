package com.example

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.TesseraViewModel
import com.example.data.PetEvent
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.TertiaryPurple
import com.example.ui.components.PremiumGlassModifier

val GlassCardModifier = PremiumGlassModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetzScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE) }
    
    val petEvents by viewModel.allPetEvents.collectAsStateWithLifecycle()
    var selectedPet by remember { mutableStateOf("Marie") }

    // Dynamic state for Marie
    var marieBreed by remember { mutableStateOf(sharedPrefs.getString("marie_breed", "Golden Retriever") ?: "Golden Retriever") }
    var marieAge by remember { mutableStateOf(sharedPrefs.getString("marie_age", "4 anos") ?: "4 anos") }
    var mariePhoto by remember { mutableStateOf(sharedPrefs.getString("marie_photo", "https://lh3.googleusercontent.com/aida-public/AB6AXuC-nfJPLwsDCoZAPRnyoFfm-kb7-YGKFlZERj6GnvfsPRWF04QUeCIX1WhZHhCQLUF4_4wKhJZZ_Pjz7Q86FxU0IpCdNNwQFjU5MHMRrs5lQl4cD1DJTeYqV574VjOoD3xOAusiBniyTZI0VWBYGbhi0NUc57PSZP_6rU7yVmXK85XXkeVqYgYA6Z_-kIeU4PINEX9lZBUfcgobmRvse9pFNN-27sq-IuJzPyavZxsCKJk7pXdnHy5vLrP8xPsnWkGmCE1VhtBiXRw") ?: "") }

    // Dynamic state for Churchill
    var churchillBreed by remember { mutableStateOf(sharedPrefs.getString("churchill_breed", "Buldogue Francês") ?: "Buldogue Francês") }
    var churchillAge by remember { mutableStateOf(sharedPrefs.getString("churchill_age", "2 anos") ?: "2 anos") }
    var churchillPhoto by remember { mutableStateOf(sharedPrefs.getString("churchill_photo", "https://lh3.googleusercontent.com/aida-public/AB6AXuBwE9mkw-3Q01XMMJNCBsgQYL4vceyVCaIpNVZLlNpqFxq56lIYShGa2Y2Ayd2cWilSsA1Sh7N8EhEeP0UmPiTX1Jxrt5v-bwMd7go8hp_GMPk-ujDr-jURbRlfoI92fsudTavmulIvwmwVFRX5oy5pq4tLAm0ouBfSkwAy2knOwtJPymqKdo2ZhqgGc_eH8IPceKSvI0ugGLLmnBGc5BIGL9mwFb4JUYULZY9PQ4BuBWZGmIU3n7lN0G86yPzXd3Zi58hh3NsMgjw") ?: "") }

    // Dynamic Health States (reactive to selectedPet)
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

    // Dialog control states
    var petToEdit by remember { mutableStateOf<String?>(null) }
    var showAddRoutineDialog by remember { mutableStateOf(false) }
    var healthCardToEdit by remember { mutableStateOf<String?>(null) } // "vaccine", "weight", "appointment", "notes"

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Marie & Churchill",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp,
                        color = Color(0xFFDFE3E2)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onHomeClick) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "Home",
                            tint = Color(0xFFBDC9C6)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile Cards
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PetProfileCard(
                    name = "Marie",
                    breed = marieBreed,
                    age = marieAge,
                    imageUrl = mariePhoto,
                    primaryColor = TertiaryPurple,
                    isSelected = selectedPet == "Marie",
                    onClick = { selectedPet = "Marie" },
                    onEditClick = { petToEdit = "Marie" }
                )
                PetProfileCard(
                    name = "Churchill",
                    breed = churchillBreed,
                    age = churchillAge,
                    imageUrl = churchillPhoto,
                    primaryColor = PrimaryTeal,
                    isSelected = selectedPet == "Churchill",
                    onClick = { selectedPet = "Churchill" },
                    onEditClick = { petToEdit = "Churchill" }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Rotina Diária
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rotina Diária",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    color = Color(0xFFDFE3E2)
                )
                
                IconButton(
                    onClick = { showAddRoutineDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x0AFFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nova Rotina", tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                }
            }
            HorizontalDivider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
            
            // Filtered routines
            val filteredEvents = petEvents.filter { it.petName == selectedPet }
            Timeline(petEvents = filteredEvents, viewModel = viewModel)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Registros de Saúde
            Text(
                text = "Registro de Saúde",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = Color(0xFFDFE3E2)
            )
            HorizontalDivider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
            
            val themeColor = if (selectedPet == "Marie") TertiaryPurple else PrimaryTeal
            val activeVaccine = if (selectedPet == "Marie") marieVaccine else churchillVaccine
            val activeVaccineDate = if (selectedPet == "Marie") marieVaccineDate else churchillVaccineDate
            val activeWeight = if (selectedPet == "Marie") marieWeight else churchillWeight
            val activeAppointment = if (selectedPet == "Marie") marieAppointment else churchillAppointment
            val activeAppointmentDate = if (selectedPet == "Marie") churchillAppointmentDate else churchillAppointmentDate
            val activeNotes = if (selectedPet == "Marie") marieNotes else churchillNotes

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        HealthRecordCard(
                            icon = Icons.Outlined.Vaccines,
                            label = activeVaccine,
                            title = "Última Vacina",
                            value = activeVaccineDate,
                            color = themeColor,
                            onClick = { healthCardToEdit = "vaccine" }
                        )
                    }
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        HealthRecordCard(
                            icon = Icons.Outlined.Scale,
                            label = selectedPet,
                            title = "Peso Registrado",
                            value = activeWeight,
                            unit = "kg",
                            color = themeColor,
                            onClick = { healthCardToEdit = "weight" }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        HealthRecordCard(
                            icon = Icons.Outlined.CalendarMonth,
                            label = activeAppointment,
                            title = "Consulta Vet",
                            value = activeAppointmentDate,
                            color = themeColor,
                            onClick = { healthCardToEdit = "appointment" }
                        )
                    }
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        HealthRecordCard(
                            icon = Icons.Outlined.Assignment,
                            label = "Observações",
                            title = "Dieta / Alergias",
                            value = activeNotes,
                            color = themeColor,
                            onClick = { healthCardToEdit = "notes" }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(140.dp))
        }
    }

    // Dialogs implementation
    if (petToEdit != null) {
        val name = petToEdit!!
        val breed = if (name == "Marie") marieBreed else churchillBreed
        val age = if (name == "Marie") marieAge else churchillAge
        val photo = if (name == "Marie") mariePhoto else churchillPhoto
        val pColor = if (name == "Marie") TertiaryPurple else PrimaryTeal

        EditPetDialog(
            petName = name,
            currentBreed = breed,
            currentAge = age,
            currentPhoto = photo,
            primaryColor = pColor,
            onDismiss = { petToEdit = null },
            onConfirm = { newBreed, newAge, newPhoto ->
                if (name == "Marie") {
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
        val currentTitle = when (category) {
            "vaccine" -> "Última Vacina"
            "weight" -> "Peso Registrado (kg)"
            "appointment" -> "Consulta Veterinária"
            else -> "Dieta / Alergias"
        }
        val currentLabelVal = when (category) {
            "vaccine" -> if (selectedPet == "Marie") marieVaccine else churchillVaccine
            "weight" -> selectedPet
            "appointment" -> if (selectedPet == "Marie") marieAppointment else churchillAppointment
            else -> "Observações"
        }
        val currentValueVal = when (category) {
            "vaccine" -> if (selectedPet == "Marie") marieVaccineDate else churchillVaccineDate
            "weight" -> if (selectedPet == "Marie") marieWeight else churchillWeight
            "appointment" -> if (selectedPet == "Marie") marieAppointmentDate else churchillAppointmentDate
            else -> if (selectedPet == "Marie") marieNotes else churchillNotes
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
                if (selectedPet == "Marie") {
                    when (category) {
                        "vaccine" -> {
                            marieVaccine = newLabel
                            marieVaccineDate = newValue
                            sharedPrefs.edit().putString("marie_vaccine_name", newLabel).putString("marie_vaccine_date", newValue).apply()
                        }
                        "weight" -> {
                            marieWeight = newValue
                            sharedPrefs.edit().putString("marie_weight", newValue).apply()
                        }
                        "appointment" -> {
                            marieAppointment = newLabel
                            marieAppointmentDate = newValue
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
                            churchillVaccine = newLabel
                            churchillVaccineDate = newValue
                            sharedPrefs.edit().putString("churchill_vaccine_name", newLabel).putString("churchill_vaccine_date", newValue).apply()
                        }
                        "weight" -> {
                            churchillWeight = newValue
                            sharedPrefs.edit().putString("churchill_weight", newValue).apply()
                        }
                        "appointment" -> {
                            churchillAppointment = newLabel
                            churchillAppointmentDate = newValue
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
fun PetProfileCard(
    name: String, 
    breed: String, 
    age: String, 
    imageUrl: String, 
    primaryColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x08FFFFFF))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) primaryColor else Color(0x1AFFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0x0DFFFFFF))
                            .border(0.5.dp, Color(0x33FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Editar",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(0.5.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = breed, color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = age, color = Color(0xFFBDC9C6), fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, primaryColor, CircleShape)
            )
        }
    }
}

@Composable
fun Timeline(petEvents: List<PetEvent>, viewModel: TesseraViewModel) {
    Column(modifier = Modifier.padding(start = 8.dp)) {
        if (petEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x05FFFFFF))
                    .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sem rotinas agendadas hoje.\nUse o botão (+) acima para registrar uma rotina!",
                    color = Color(0xFFBDC9C6),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        } else {
            petEvents.forEachIndexed { index, event ->
                val icon = when {
                    event.title.contains("Passeio", ignoreCase = true) -> Icons.AutoMirrored.Outlined.DirectionsWalk
                    event.title.contains("Alimenta", ignoreCase = true) || event.title.contains("Ração", ignoreCase = true) || event.title.contains("Comida", ignoreCase = true) -> Icons.Outlined.Restaurant
                    event.title.contains("Medicamento", ignoreCase = true) || event.title.contains("Remédio", ignoreCase = true) || event.title.contains("Vacina", ignoreCase = true) -> Icons.Outlined.Medication
                    else -> Icons.Outlined.Pets
                }
                val color = when {
                    event.isCompleted -> Color(0xFF71D7CD)
                    event.petName == "Churchill" -> Color(0xFFD7BAFF)
                    else -> Color(0xFFDFE3E2)
                }
                
                TimelineItem(
                    isFirst = index == 0,
                    isLast = index == petEvents.size - 1,
                    isCompleted = event.isCompleted,
                    icon = icon,
                    iconColor = color,
                    title = event.title,
                    subtitle = "${event.time} - ${if (event.isCompleted) "Concluído" else "Agendado"}",
                    opacity = if (!event.isCompleted && !event.isNext) 0.7f else 1f,
                    onClick = { viewModel.togglePetEventCompleted(event) },
                    onDelete = { viewModel.deletePetEvent(event) }
                )
            }
        }
    }
}

@Composable
fun TimelineItem(
    isFirst: Boolean,
    isLast: Boolean,
    isCompleted: Boolean,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    opacity: Float,
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Line and dot
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .padding(top = 24.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x6671D7CD), Color(0x33D7BAFF), Color.Transparent)
                            )
                        )
                )
            }
            
            // Dot
            val dotColor = if (isCompleted) Color(0xFF71D7CD) else Color(0xFF1B2120)
            val borderColor = if (isCompleted) Color(0xFF0F1414) else Color(0x80879391)
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(2.dp, borderColor, CircleShape)
                    .clickable(onClick = onClick)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Content Card
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x08FFFFFF).copy(alpha = 0.06f * opacity))
                .border(1.dp, Color(0x1AFFFFFF).copy(alpha = 0.1f * opacity), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B2120).copy(alpha = opacity))
                            .border(0.5.dp, Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon, 
                            contentDescription = null, 
                            tint = iconColor.copy(alpha = opacity),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    Column {
                        Text(
                            text = title, 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.SemiBold, 
                            color = Color(0xFFDFE3E2).copy(alpha = opacity)
                        )
                        Text(
                            text = subtitle, 
                            fontSize = 12.sp, 
                            color = Color(0xB3BDC9C6).copy(alpha = opacity)
                        )
                    }
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Excluir",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HealthRecordCard(
    icon: ImageVector,
    label: String,
    title: String,
    value: String,
    unit: String = "",
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x06FFFFFF))
            .border(1.dp, Color(0x13FFFFFF), RoundedCornerShape(20.dp))
            .then(GlassCardModifier)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, color.copy(alpha = 0.12f)),
                        startY = 60f
                    )
                )
        )
        
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f))
                        .border(1.dp, color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = label, 
                    color = color, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            
            Column {
                Text(text = title, color = Color(0xFFBDC9C6), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value, 
                        color = Color.White, 
                        fontSize = 24.sp, 
                        fontFamily = FontFamily.Serif, 
                        fontWeight = FontWeight.SemiBold, 
                        modifier = Modifier.alignByBaseline()
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unit, 
                            color = Color(0xFFBDC9C6), 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.SemiBold, 
                            modifier = Modifier.alignByBaseline()
                        )
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

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photoString = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Editar Perfil de $petName",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
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
                // Photo
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(0x0DFFFFFF))
                        .border(1.5.dp, primaryColor, CircleShape)
                        .clickable { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoString.isNotEmpty()) {
                        AsyncImage(
                            model = photoString,
                            contentDescription = petName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Outlined.Pets, contentDescription = "Add Photo", tint = Color.Gray, modifier = Modifier.size(36.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x4D000000)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = "ALTERAR",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                // Breed
                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Raça / Tipo") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = Color(0x99FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Age
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Idade") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = Color(0x99FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(breed, age, photoString) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black)
            ) {
                Text("Confirmar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF0F1413),
        shape = RoundedCornerShape(24.dp)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nova Rotina para $selectedPet",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome da Atividade") },
                    placeholder = { Text("ex: Passeio no Parque, Ração Tarde") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = Color(0x99FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Horário") },
                    placeholder = { Text("ex: 15:30, 08:00") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryTeal,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = Color(0x99FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotEmpty() && time.isNotEmpty()) onConfirm(title, time) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black)
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF0F1413),
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
    onConfirm: (newLabel: String, newValue: String) -> Unit
) {
    var label by remember { mutableStateOf(currentLabel) }
    var value by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Registrar - $title",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedLabelColor = PrimaryTeal,
                            unfocusedLabelColor = Color(0x99FFFFFF)
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
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedLabelColor = PrimaryTeal,
                        unfocusedLabelColor = Color(0x99FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label, value) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black)
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White.copy(alpha = 0.6f))
            }
        },
        containerColor = Color(0xFF0F1413),
        shape = RoundedCornerShape(24.dp)
    )
}
