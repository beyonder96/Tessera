package com.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(onHomeClick: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Metas",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        color = OnBackgroundDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onHomeClick) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = "Home",
                            tint = OnBackgroundDark.copy(alpha = 0.7f)
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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            SectionHeader("ATIVIDADE", Icons.Outlined.Timeline)
            
            // Steps
            GoalProgressCard(
                title = "PASSOS",
                value = "8.5k",
                total = "/ 10k",
                icon = Icons.Outlined.DirectionsWalk,
                progress = 0.85f,
                progressColor = PrimaryTeal
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Calories
            GoalProgressCard(
                title = "CALORIAS",
                value = "420",
                total = "/ 600",
                icon = Icons.Outlined.LocalFireDepartment,
                progress = 0.7f,
                progressColor = SecondaryGold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Active Time
            GoalProgressCard(
                title = "ATIVIDADE",
                value = "45",
                total = "/ 30 m",
                icon = Icons.Outlined.Timer,
                progress = 1.0f,
                progressColor = TertiaryPurple
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            SectionHeader("PATRIMÔNIO", Icons.Outlined.AccountBalance)
            
            // Wealth Goal Image Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderGlass, RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=800&auto=format&fit=crop",
                    contentDescription = "Viagem Kyoto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x33000000), Color(0xCC000000))
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Viagem Kyoto 2025", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Rendimento ativo", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        Text("↗ +2.4%", fontSize = 12.sp, color = SecondaryGold, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text("R$ 14.500", fontFamily = FontFamily.Serif, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("/ 20.000", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0x4DFFFFFF), CircleShape)) {
                            Box(modifier = Modifier.fillMaxWidth(0.725f).height(4.dp).background(SecondaryGold, CircleShape))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            SectionHeader("RITUAIS DIÁRIOS", Icons.Outlined.Spa)
            
            Column(
                modifier = PremiumGlassModifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
                
                var hydrationChecked by remember { mutableStateOf(sharedPrefs.getBoolean("goal_hydration_checked", false)) }
                var readingChecked by remember { mutableStateOf(sharedPrefs.getBoolean("goal_reading_checked", false)) }
                var mindfulnessChecked by remember { mutableStateOf(sharedPrefs.getBoolean("goal_mindfulness_checked", true)) }
                
                RitualItem(
                    title = "Hidratação",
                    subtitle = "2.5L / 3.0L",
                    icon = Icons.Outlined.WaterDrop,
                    iconColor = PrimaryTeal,
                    isChecked = hydrationChecked,
                    onToggle = { 
                        hydrationChecked = !hydrationChecked 
                        sharedPrefs.edit().putBoolean("goal_hydration_checked", hydrationChecked).apply()
                    }
                )
                HorizontalDivider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 24.dp))
                RitualItem(
                    title = "Leitura Profunda",
                    subtitle = "0 / 30 min",
                    icon = Icons.Outlined.MenuBook,
                    iconColor = SecondaryGold,
                    isChecked = readingChecked,
                    onToggle = { 
                        readingChecked = !readingChecked 
                        sharedPrefs.edit().putBoolean("goal_reading_checked", readingChecked).apply()
                    }
                )
                HorizontalDivider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(horizontal = 24.dp))
                RitualItem(
                    title = "Mindfulness",
                    subtitle = "Concluído",
                    icon = Icons.Outlined.SelfImprovement,
                    iconColor = TertiaryPurple,
                    isChecked = mindfulnessChecked,
                    onToggle = { 
                        mindfulnessChecked = !mindfulnessChecked 
                        sharedPrefs.edit().putBoolean("goal_mindfulness_checked", mindfulnessChecked).apply()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun GoalProgressCard(
    title: String,
    value: String,
    total: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    progress: Float,
    progressColor: Color
) {
    Row(
        modifier = PremiumGlassModifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = OnBackgroundDark.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontFamily = FontFamily.Serif, fontSize = 20.sp, color = OnBackgroundDark, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(4.dp))
                Text(total, fontSize = 14.sp, color = OnBackgroundDark.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        
        OuraCircularProgress(
            progress = progress,
            progressColor = progressColor,
            modifier = Modifier.size(48.dp),
            strokeWidth = 3f
        ) {
            Icon(icon, contentDescription = null, tint = progressColor, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun RitualItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 16.sp, color = OnBackgroundDark, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.6f))
            }
        }
        
        // Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(2.dp, if (isChecked) iconColor else Color(0x33FFFFFF), CircleShape)
                .background(if (isChecked) iconColor.copy(alpha = 0.2f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(iconColor))
            }
        }
    }
}
