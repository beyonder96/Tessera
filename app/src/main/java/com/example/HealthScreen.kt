package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(onHomeClick: () -> Unit) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saúde",
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Circular Prontidão Score
            OuraCircularProgress(
                progress = 0.82f,
                progressColor = PrimaryTeal,
                modifier = Modifier.size(200.dp),
                strokeWidth = 10f
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "82",
                        fontFamily = FontFamily.Serif,
                        fontSize = 72.sp,
                        color = OnBackgroundDark,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 72.sp
                    )
                    Text(
                        text = "PRONTIDÃO",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnBackgroundDark.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Cardiovascular Health
            Column(
                modifier = PremiumGlassModifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                SectionHeader("SAÚDE CARDIOVASCULAR", Icons.Outlined.FavoriteBorder)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("65", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = OnBackgroundDark)
                            Text(" bpm", fontSize = 14.sp, color = OnBackgroundDark.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text("Frequência Média", fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                    }
                    
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("42", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = OnBackgroundDark)
                            Text(" ms", fontSize = 14.sp, color = OnBackgroundDark.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text("Variabilidade\n(HRV)", fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.7f), lineHeight = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Simple placeholder for the wave chart
                Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.BottomCenter) {
                     Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(TertiaryPurple.copy(alpha = 0.5f)))
                     // Just an illustrative curvy line
                     Box(modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth(0.3f).height(2.dp).background(TertiaryPurple).align(Alignment.BottomStart))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sleep
            Column(
                modifier = PremiumGlassModifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                SectionHeader("SONO", Icons.Outlined.Bedtime)
                
                Text("8h 16m", fontFamily = FontFamily.Serif, fontSize = 36.sp, color = OnBackgroundDark)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Sleep stages bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Acordado", fontSize = 10.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                    Text("REM", fontSize = 10.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                    Text("Leve", fontSize = 10.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                    Text("Profundo", fontSize = 10.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                    Box(modifier = Modifier.weight(0.1f).fillMaxHeight().background(SecondaryGold))
                    Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(TertiaryPurple))
                    Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(PrimaryTeal.copy(alpha = 0.5f)))
                    Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(PrimaryTeal))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Body Metrics
            Column(
                modifier = PremiumGlassModifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                SectionHeader("MÉTRICAS CORPORAIS", Icons.Outlined.MonitorWeight)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Peso Atual", fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("78", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = OnBackgroundDark)
                            Text(" kg", fontSize = 14.sp, color = OnBackgroundDark.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                    
                    Column {
                        Text("Variação Temp.", fontSize = 12.sp, color = OnBackgroundDark.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("+1.2", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = SecondaryGold)
                            Text(" °C", fontSize = 14.sp, color = OnBackgroundDark.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
