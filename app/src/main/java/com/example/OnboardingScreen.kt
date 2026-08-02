package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: TesseraViewModel,
    onCompleted: () -> Unit
) {
    var stepIndex by remember { mutableStateOf(0) }
    val totalSteps = 5
    var seedDemoChecked by remember { mutableStateOf(true) }

    // Ambient Glowing Transitions
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientGlowOnboarding")
    val glowAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha1"
    )
    val glowAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha2"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        // Deep background glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Glow top right (Teal)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PrimaryTeal.copy(alpha = glowAlpha1), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                    radius = size.width * pulseScale
                ),
                radius = size.width,
                center = Offset(size.width * 0.8f, size.height * 0.2f)
            )

            // Glow bottom left (Purple)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TertiaryPurple.copy(alpha = glowAlpha2), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.8f),
                    radius = size.width * 1.2f * pulseScale
                ),
                radius = size.width * 1.2f,
                center = Offset(size.width * 0.2f, size.height * 0.8f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TESSERA",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    letterSpacing = 4.sp
                )
                
                if (stepIndex < totalSteps - 1) {
                    Text(
                        text = "PULAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier
                            .clickable {
                                stepIndex = totalSteps - 1
                            }
                            .padding(8.dp)
                    )
                }
            }

            // Main Animated Content Pager
            AnimatedContent(
                targetState = stepIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                label = "OnboardingPageTransition"
            ) { page ->
                when (page) {
                    0 -> OnboardingWelcomePage()
                    1 -> OnboardingHealthPage()
                    2 -> OnboardingFinancePage()
                    3 -> OnboardingFocusPage()
                    4 -> OnboardingFinalPage(
                        seedDemoChecked = seedDemoChecked,
                        onSeedChange = { seedDemoChecked = it }
                    )
                }
            }

            // Bottom Navigation Row
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    repeat(totalSteps) { idx ->
                        val isSelected = idx == stepIndex
                        val width by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "indicatorWidth")
                        val color by animateColorAsState(if (isSelected) PrimaryTeal else Color.White.copy(alpha = 0.2f), label = "indicatorColor")
                        
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // CTA Action Button
                Button(
                    onClick = {
                        if (stepIndex < totalSteps - 1) {
                            stepIndex++
                        } else {
                            if (seedDemoChecked) {
                                viewModel.seedDemoData()
                            }
                            onCompleted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryTeal,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (stepIndex == totalSteps - 1) "INICIAR JORNADA" else "CONTINUAR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingWelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryTeal.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
                .border(1.dp, PrimaryTeal.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Seu Espaço,\nSua Harmonia",
            fontFamily = FontFamily.Serif,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "O Tessera unifica sua saúde, rituais diários, finanças pessoais e lazer em uma única interface premium líquida e sem distrações.",
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 22.sp
        )
    }
}

@Composable
fun OnboardingHealthPage() {
    val infiniteTransition = rememberInfiniteTransition(label = "HealthOnboardingRings")
    val animatedProgress1 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StepsRing"
    )
    val animatedProgress2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SleepRing"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            // Steps progress ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 8.dp.toPx()
                // Track 1
                drawArc(
                    color = PrimaryTeal.copy(alpha = 0.1f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeW)
                )
                // Progress 1
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(PrimaryTeal, Color(0xFF4D96FF), PrimaryTeal)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress1,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }

            // Sleep progress ring (nested)
            Canvas(modifier = Modifier.size(130.dp)) {
                val strokeW = 8.dp.toPx()
                // Track 2
                drawArc(
                    color = TertiaryPurple.copy(alpha = 0.1f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeW)
                )
                // Progress 2
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(TertiaryPurple, Color(0xFFFF8A65), TertiaryPurple)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress2,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
            
            Icon(
                imageVector = Icons.Outlined.MonitorHeart,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Saúde",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sincronização premium inspirada no Oura Ring. Acompanhe passos, qualidade do sono e composição corporal em harmonia com o Google Health Connect.",
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 22.sp
        )
    }
}

@Composable
fun OnboardingFinancePage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .then(PremiumGlassModifier)
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PATRIMÔNIO LÍQUIDO", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)
                    Icon(Icons.Outlined.AccountBalanceWallet, null, tint = SecondaryGold, modifier = Modifier.size(20.dp))
                }
                Text("R$ 208.151,60", fontFamily = FontFamily.Serif, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Balanço Mensal", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("+R$ 18.049,80", fontSize = 12.sp, color = PrimaryTeal, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Cartões Ativos", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                        Text("Inter Black • Nubank", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Finanças Pessoais",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Controle patrimonial elegante. Acompanhe suas contas bancárias, faturas de cartão de crédito e transações sem poluição visual ou complexidade excessiva.",
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 22.sp
        )
    }
}

@Composable
fun OnboardingFocusPage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .then(PremiumGlassModifier)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("ROTINA MATINAL", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), letterSpacing = 1.sp)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(PrimaryTeal.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.WaterDrop, null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Beber Água (500ml)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(SecondaryGold.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Spa, null, tint = SecondaryGold, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Meditação Transcendental", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Foco & Rotinas",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Desenvolva consistência. Construa rotinas matinais/noturnas passo a passo com temporizador e monitore seus hábitos diários de forma gratificante.",
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 22.sp
        )
    }
}

@Composable
fun OnboardingFinalPage(
    seedDemoChecked: Boolean,
    onSeedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryTeal.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
                .border(1.dp, PrimaryTeal.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.DoneAll,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            text = "Tudo Pronto!",
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Você está prestes a iniciar sua nova experiência integrada. Escolha se prefere começar com dados de demonstração já preenchidos para testar tudo:",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Option checklist to seed database
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, if (seedDemoChecked) PrimaryTeal.copy(alpha = 0.4f) else Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                .background(if (seedDemoChecked) PrimaryTeal.copy(alpha = 0.05f) else Color(0x05FFFFFF))
                .clickable { onSeedChange(!seedDemoChecked) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = seedDemoChecked,
                onCheckedChange = { onSeedChange(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryTeal,
                    uncheckedColor = Color.White.copy(alpha = 0.4f),
                    checkmarkColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Carregar Dados de Demonstração",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Popula o app com pets, contas, rotinas e transações fictícias para testar.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
