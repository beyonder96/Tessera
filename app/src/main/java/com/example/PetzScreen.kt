package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.TesseraViewModel
import com.example.data.PetEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetzScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val petEvents by viewModel.allPetEvents.collectAsStateWithLifecycle()
    
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Petz",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
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
                    breed = "Golden Retriever",
                    age = "4 anos",
                    imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuC-nfJPLwsDCoZAPRnyoFfm-kb7-YGKFlZERj6GnvfsPRWF04QUeCIX1WhZHhCQLUF4_4wKhJZZ_Pjz7Q86FxU0IpCdNNwQFjU5MHMRrs5lQl4cD1DJTeYqV574VjOoD3xOAusiBniyTZI0VWBYGbhi0NUc57PSZP_6rU7yVmXK85XXkeVqYgYA6Z_-kIeU4PINEX9lZBUfcgobmRvse9pFNN-27sq-IuJzPyavZxsCKJk7pXdnHy5vLrP8xPsnWkGmCE1VhtBiXRw",
                    primaryColor = Color(0xFFD7BAFF) // Tertiary
                )
                PetProfileCard(
                    name = "Churchill",
                    breed = "Buldogue Francês",
                    age = "2 anos",
                    imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBwE9mkw-3Q01XMMJNCBsgQYL4vceyVCaIpNVZLlNpqFxq56lIYShGa2Y2Ayd2cWilSsA1Sh7N8EhEeP0UmPiTX1Jxrt5v-bwMd7go8hp_GMPk-ujDr-jURbRlfoI92fsudTavmulIvwmwVFRX5oy5pq4tLAm0ouBfSkwAy2knOwtJPymqKdo2ZhqgGc_eH8IPceKSvI0ugGLLmnBGc5BIGL9mwFb4JUYULZY9PQ4BuBWZGmIU3n7lN0G86yPzXd3Zi58hh3NsMgjw",
                    primaryColor = Color(0xFF71D7CD) // Primary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Rotina Diária
            Text(
                text = "Rotina Diária",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = Color(0xFFDFE3E2),
                modifier = Modifier
                    .border(width = 1.dp, color = Color.Transparent) // trick for bottom border
                    .padding(bottom = 8.dp)
            )
            HorizontalDivider(color = Color(0x33879391), modifier = Modifier.padding(bottom = 24.dp))
            
            Timeline(petEvents = petEvents, viewModel = viewModel)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Registros de Saúde
            Text(
                text = "Registros de Saúde",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = Color(0xFFDFE3E2),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HorizontalDivider(color = Color(0x33879391), modifier = Modifier.padding(bottom = 24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                    HealthRecordCard(
                        icon = Icons.Outlined.Vaccines,
                        label = "V8",
                        title = "Última Vacina",
                        value = "12/05",
                        color = Color(0xFFD7BAFF)
                    )
                }
                Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                    HealthRecordCard(
                        icon = Icons.Outlined.Scale,
                        label = "Marie",
                        title = "Peso Ideal",
                        value = "28.5",
                        unit = "kg",
                        color = Color(0xFF71D7CD)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun PetProfileCard(name: String, breed: String, age: String, imageUrl: String, primaryColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.1f), Color.Transparent)
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = name,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    color = Color(0xFFDFE3E2)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(primaryColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(text = breed, color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(text = age, color = Color(0xFFBDC9C6), fontSize = 16.sp)
                }
            }
            
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0x4D879391), CircleShape)
            )
        }
    }
}

@Composable
fun Timeline(petEvents: List<PetEvent>, viewModel: TesseraViewModel) {
    Column(modifier = Modifier.padding(start = 12.dp)) {
        if (petEvents.isEmpty()) {
            Text("Sem eventos programados", color = Color(0xFFBDC9C6), modifier = Modifier.padding(vertical = 16.dp))
        }
        petEvents.forEachIndexed { index, event ->
            val icon = when {
                event.title.contains("Passeio", ignoreCase = true) -> Icons.AutoMirrored.Outlined.DirectionsWalk
                event.title.contains("Alimenta", ignoreCase = true) -> Icons.Outlined.Restaurant
                event.title.contains("Medicamento", ignoreCase = true) -> Icons.Outlined.Medication
                else -> Icons.Outlined.Pets
            }
            val color = when {
                event.isCompleted -> Color(0xFF71D7CD)
                event.petName == "Churchill" -> Color(0xFFD7BAFF)
                else -> Color(0xFFBDC9C6)
            }
            
            TimelineItem(
                isFirst = index == 0,
                isLast = index == petEvents.size - 1,
                isCompleted = event.isCompleted,
                icon = icon,
                iconColor = color,
                title = if (event.petName != "Marie") "${event.title} (${event.petName})" else event.title,
                subtitle = "${event.time} - ${if (event.isCompleted) "Concluído" else if (event.isNext) "Próximo" else "Agendado"}",
                opacity = if (!event.isCompleted && !event.isNext) 0.6f else 1f,
                onClick = { viewModel.togglePetEventCompleted(event) }
            )
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
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
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
                                colors = listOf(Color(0x8071D7CD), Color(0x33D7BAFF), Color.Transparent)
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
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x08FFFFFF).copy(alpha = 0.08f * opacity))
                .border(1.dp, Color(0x1AFFFFFF).copy(alpha = 0.1f * opacity), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1B2120).copy(alpha = opacity))
                        .border(1.dp, Color(0x4D879391).copy(alpha = opacity), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor.copy(alpha = opacity))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDFE3E2).copy(alpha = opacity))
                    Text(text = subtitle, fontSize = 16.sp, color = Color(0xB3BDC9C6).copy(alpha = opacity))
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
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x08FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, color.copy(alpha = 0.1f)),
                        startY = 50f
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
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Text(text = label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Column {
                Text(text = title, color = Color(0xFFBDC9C6), fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = value, color = Color(0xFFDFE3E2), fontSize = 28.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = unit, color = Color(0xFFBDC9C6), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
                    }
                }
            }
        }
    }
}
