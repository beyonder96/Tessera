package com.example

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.TesseraRepository
import com.example.viewmodel.TesseraViewModel
import com.example.viewmodel.TesseraViewModelFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.tesserahub.app.data.local.AppDatabase as TesseraDatabase
import com.tesserahub.app.ui.viewmodel.HomeViewModel
import com.tesserahub.app.data.local.entity.PetRoutineEntity
import com.tesserahub.app.utils.BackupHelper
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.OuraCircularProgress
import androidx.compose.ui.draw.alpha

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TesseraHubApp()
            }
        }
    }
}

val GlassModifier = PremiumGlassModifier

@Composable
fun TesseraHubApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { TesseraRepository(database.tesseraDao()) }
    val viewModel: TesseraViewModel = viewModel(factory = TesseraViewModelFactory(repository))
    
    LaunchedEffect(Unit) {
        viewModel.initializeDataIfNeeded()
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val navigateAction: (String) -> Unit = { route ->
        if (route == "home") {
            navController.popBackStack(navController.graph.findStartDestination().id, inclusive = false)
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    var isFabExpanded by remember { mutableStateOf(false) }
    var fabHoveredItem by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151E20), // Dark teal/slate top
                        Color(0xFF070909)  // Pitch black bottom
                    )
                )
            )
        ) {
            NavHost(navController = navController, startDestination = "home", modifier = Modifier.fillMaxSize().padding(bottom = 120.dp)) {
                composable("home") {
                    HomeScreen(onNavigate = navigateAction)
                }
                composable("finance") {
                    FinanceScreen(onHomeClick = { 
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, viewModel = viewModel)
                }
                composable("health") {
                    HealthScreen(onHomeClick = { 
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
                }
                composable("goals") {
                    GoalsScreen(onHomeClick = { 
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
                }
                composable("market") {
                    MarketScreen(onHomeClick = { 
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, viewModel = viewModel)
                }
                composable("petz") {
                    PetzScreen(onHomeClick = { 
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, viewModel = viewModel)
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = innerPadding.calculateBottomPadding())) {
                BottomNavBar(
                    isExpanded = isFabExpanded,
                    onExpandedChange = { isFabExpanded = it },
                    onHoveredItemChange = { fabHoveredItem = it },
                    currentRoute = currentRoute,
                    onNavigate = navigateAction
                )
            }

            // Overlay for FAB with elegant animations
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xD9070909)) // Premium dark semi-translucent overlay
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { isFabExpanded = false }
                ) {
                    val bottomOffset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = bottomOffset),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val titleAlpha by animateFloatAsState(if (isFabExpanded) 1f else 0f, tween(300))
                            Text(
                                text = "O que você deseja ver?",
                                color = Color.White.copy(alpha = titleAlpha),
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Selecione um painel para navegar",
                                color = Color.White.copy(alpha = titleAlpha * 0.5f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(36.dp))
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val healthAlpha by animateFloatAsState(if (isFabExpanded) 1f else 0f, tween(350, delayMillis = 50))
                                val healthOffset by animateDpAsState(if (isFabExpanded) 0.dp else 40.dp, spring(dampingRatio = 0.82f, stiffness = 180f))
                                PremiumGlassCard(
                                    title = "Saúde & Corpo",
                                    subtitle = "Monitore seu sono, prontidão e batimentos",
                                    icon = Icons.Outlined.MonitorHeart,
                                    iconColor = PrimaryTeal,
                                    alpha = healthAlpha,
                                    offsetY = healthOffset,
                                    onClick = { navigateAction("health"); isFabExpanded = false }
                                )
                                
                                val goalsAlpha by animateFloatAsState(if (isFabExpanded) 1f else 0f, tween(350, delayMillis = 150))
                                val goalsOffset by animateDpAsState(if (isFabExpanded) 0.dp else 40.dp, spring(dampingRatio = 0.82f, stiffness = 180f))
                                PremiumGlassCard(
                                    title = "Metas & Hábitos",
                                    subtitle = "Acompanhe seus objetivos diários",
                                    icon = Icons.Outlined.Flag,
                                    iconColor = Color(0xFFF9A826), // Gold
                                    alpha = goalsAlpha,
                                    offsetY = goalsOffset,
                                    onClick = { navigateAction("goals"); isFabExpanded = false }
                                )
                                
                                val petzAlpha by animateFloatAsState(if (isFabExpanded) 1f else 0f, tween(350, delayMillis = 250))
                                val petzOffset by animateDpAsState(if (isFabExpanded) 0.dp else 40.dp, spring(dampingRatio = 0.82f, stiffness = 180f))
                                PremiumGlassCard(
                                    title = "Marie & Churchill",
                                    subtitle = "Gerencie a rotina e tarefas dos seus pets",
                                    icon = Icons.Outlined.Pets,
                                    iconColor = TertiaryPurple,
                                    alpha = petzAlpha,
                                    offsetY = petzOffset,
                                    onClick = { navigateAction("petz"); isFabExpanded = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val tesseraDb = remember { TesseraDatabase.getDatabase(context) }
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(tesseraDb.tesseraDao())
    )

    val netWorth by homeViewModel.totalNetWorth.collectAsState()
    val petRoutines by homeViewModel.petRoutines.collectAsState()
    var showChatSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        homeViewModel.seedDatabase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070909)) // Seamless solid black background
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x00070909), // 0%
                                Color(0x99070909), // 60%
                                Color(0xFF070909),  // 100%
                                Color(0xFF070909)   // Extra padding
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(modifier = Modifier.height(24.dp))
                TopHeader()
                Spacer(modifier = Modifier.height(32.dp))
                TopMetricsRow(netWorth, onNavigate)
                Spacer(modifier = Modifier.height(48.dp))
                HeroMetric(onNavigate)
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
        
        MainContent(netWorth, petRoutines, onChatClick = { showChatSheet = true })
        Spacer(modifier = Modifier.height(140.dp))
    }

    if (showChatSheet) {
        TesseraChatSheet(onDismiss = { showChatSheet = false }, netWorth = netWorth, petRoutines = petRoutines)
    }
}

@Composable
fun TopHeader() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    var profileUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        val savedUriStr = sharedPrefs.getString("user_profile_uri", null)
        if (savedUriStr != null) {
            try {
                profileUri = Uri.parse(savedUriStr)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            profileUri = uri
            sharedPrefs.edit().putString("user_profile_uri", uri.toString()).apply()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        IconButton(
            onClick = { 
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            if (profileUri != null) {
                AsyncImage(
                    model = profileUri,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                )
            } else {
                Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Text(
            text = "Bom dia, Kenned",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun TopMetricsRow(netWorth: Double, onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val formattedWorth = if (netWorth >= 1000) "${(netWorth / 1000).toInt()}k" else netWorth.toInt().toString()
        MetricItem(Icons.Outlined.AccountBalanceWallet, formattedWorth, "PATRIMÔNIO", onClick = { onNavigate("finance") })
        MetricItemWithProgress(Icons.Outlined.Bedtime, "82", "SAÚDE", PrimaryTeal, 0.82f, onClick = { onNavigate("health") })
        MetricItem(Icons.Outlined.FavoriteBorder, "72", "FREQUÊNCIA", onClick = { onNavigate("health") })
        MetricItemWithProgress(Icons.Outlined.MonitorWeight, "78", "CORPO", TertiaryPurple, 0.78f, onClick = { onNavigate("health") })
    }
}

@Composable
fun MetricItem(icon: ImageVector, value: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0x0AFFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(value, fontFamily = FontFamily.Serif, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(label, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MetricItemWithProgress(icon: ImageVector, value: String, label: String, progressColor: Color, progress: Float, onClick: () -> Unit) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0x0AFFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                drawArc(
                    color = progressColor.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(value, fontFamily = FontFamily.Serif, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(label, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HeroMetric(onNavigate: (String) -> Unit) {
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    
    // Determine dynamic content based on time of day
    val isMorning = hour in 5..11
    val isAfternoon = hour in 12..17
    
    val title = if (isMorning) "PRONTIDÃO" else if (isAfternoon) "ATIVIDADE" else "RELAXAMENTO"
    val value = if (isMorning) "85" else if (isAfternoon) "6.4k" else "1h"
    val subtitle = if (isMorning) "Bom dia" else if (isAfternoon) "Progresso da tarde" else "Boa noite"
    val subtext = if (isMorning) "O seu corpo recuperou bem durante a noite.\nPronto para o dia!" 
                  else if (isAfternoon) "Você está no caminho certo para atingir a meta diária." 
                  else "Hora de desacelerar. Prepare-se para uma boa noite de sono."
    val icon = if (isMorning) Icons.Outlined.WbSunny else if (isAfternoon) Icons.Outlined.DirectionsWalk else Icons.Outlined.Bedtime
    val progressValue = if (isMorning) 0.85f else if (isAfternoon) 0.64f else 0.3f
    val progressColor = if (isMorning) Color(0xFFF9A826) else if (isAfternoon) PrimaryTeal else TertiaryPurple

    // Start-up animation for the arc
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationStarted = true }
    
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationStarted) progressValue else 0f,
        animationSpec = tween(durationMillis = 1500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onNavigate("goals") }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val topOfArcY = 180.dp - (maxWidth / 2)

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.dp.toPx()
                val startAngle = 155f
                val sweepAngle = 230f
                val arcRectSize = Size(size.width, size.width)
                val topLeft = Offset(0f, size.height - size.width / 2)

                // Background arc
                drawArc(
                    color = Color(0x33DFE3E2),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = arcRectSize,
                    topLeft = topLeft
                )
                // Progress arc
                drawArc(
                    color = progressColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = arcRectSize,
                    topLeft = topLeft
                )

                val centerVector = Offset(size.width / 2, size.height)
                val radius = size.width / 2

                // Dots
                val angles = listOf(155f, 212.5f, 270f, 327.5f, 25f)
                for (angle in angles) {
                    val angleRad = Math.toRadians(angle.toDouble())
                    val x = centerVector.x + radius * Math.cos(angleRad).toFloat()
                    val y = centerVector.y + radius * Math.sin(angleRad).toFloat()
                    drawCircle(color = Color(0xFFDFE3E2), radius = 2.dp.toPx(), center = Offset(x, y))
                }
            }

            // Top center icon in thin circle
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = topOfArcY - 12.dp) // carefully centered on the arc
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(12.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = value,
                    fontFamily = FontFamily.Serif,
                    fontSize = 62.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 72.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = subtitle,
            fontFamily = FontFamily.Serif,
            fontSize = 30.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = subtext,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
data class ModuleConfig(val id: String, val name: String, var isVisible: Boolean, var order: Int)

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun MainContent(netWorth: Double, petRoutines: List<PetRoutineEntity>, onChatClick: () -> Unit = {}) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    var showEditSheet by remember { mutableStateOf(false) }

    val defaultModules = listOf(
        ModuleConfig("finance", "Finanças", true, 0),
        ModuleConfig("market", "Mercado", true, 1),
        ModuleConfig("pets", "Pets", true, 2),
        ModuleConfig("system", "Tessera AI / Sistema", true, 3),
        ModuleConfig("health", "Saúde Rápida", false, 4),
        ModuleConfig("goals", "Metas Diárias", false, 5)
    )

    var modules by remember {
        mutableStateOf(
            run {
                val savedString = sharedPrefs.getString("modules_order", null)
                if (savedString != null) {
                    try {
                        val parsed = savedString.split("|").mapNotNull { part ->
                            val parts = part.split(",")
                            if (parts.size == 4) {
                                ModuleConfig(parts[0], parts[1], parts[2].toBoolean(), parts[3].toInt())
                            } else null
                        }
                        if (parsed.isNotEmpty()) parsed.sortedBy { it.order } else defaultModules
                    } catch (e: Exception) {
                        defaultModules
                    }
                } else {
                    defaultModules
                }
            }
        )
    }

    fun saveModules(newModules: List<ModuleConfig>) {
        modules = newModules
        val str = newModules.joinToString("|") { "${it.id},${it.name},${it.isVisible},${it.order}" }
        sharedPrefs.edit().putString("modules_order", str).apply()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        modules.filter { it.isVisible }.sortedBy { it.order }.forEach { module ->
            when (module.id) {
                "finance" -> FinanceCard(netWorth)
                "market" -> MarketCard()
                "pets" -> PetsCard(petRoutines)
                "system" -> SystemCard(onChatClick = onChatClick)
                "health" -> HealthWidget()
                "goals" -> GoalsWidget()
            }
        }
        
        OutlinedButton(
            onClick = { showEditSheet = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Editar Módulos")
        }
    }

    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            containerColor = Color(0xFF131817)
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("Editar Módulos", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.height(24.dp))
                
                modules.sortedBy { it.order }.forEachIndexed { index, module ->
                    ModuleToggleWithOrder(
                        name = module.name,
                        isVisible = module.isVisible,
                        isFirst = index == 0,
                        isLast = index == modules.size - 1,
                        onToggle = { isChecked ->
                            val newModules = modules.map { if (it.id == module.id) it.copy(isVisible = isChecked) else it }
                            saveModules(newModules)
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val newModules = modules.toMutableList()
                                val temp = newModules[index]
                                newModules[index] = newModules[index - 1]
                                newModules[index - 1] = temp
                                newModules.forEachIndexed { i, m -> m.order = i }
                                saveModules(newModules)
                            }
                        },
                        onMoveDown = {
                            if (index < modules.size - 1) {
                                val newModules = modules.toMutableList()
                                val temp = newModules[index]
                                newModules[index] = newModules[index + 1]
                                newModules[index + 1] = temp
                                newModules.forEachIndexed { i, m -> m.order = i }
                                saveModules(newModules)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ModuleToggleWithOrder(
    name: String, 
    isVisible: Boolean, 
    isFirst: Boolean,
    isLast: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Column {
                IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir", tint = if (isFirst) Color.Gray else Color.White)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer", tint = if (isLast) Color.Gray else Color.White)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, fontSize = 16.sp, color = Color.White)
        }
        Switch(
            checked = isVisible,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryTeal, checkedTrackColor = PrimaryTeal.copy(alpha = 0.5f))
        )
    }
}

@Composable
fun HealthWidget() {
    Column(modifier = GlassModifier.fillMaxWidth().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "SAÚDE RÁPIDA",
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(Icons.Outlined.MonitorHeart, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Peso Atual", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("75.2 kg", fontSize = 24.sp, fontFamily = FontFamily.Serif, color = Color.White)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Sentimento", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Energizado", fontSize = 16.sp, color = PrimaryTeal, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun GoalsWidget() {
    Column(modifier = GlassModifier.fillMaxWidth().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "METAS DIÁRIAS",
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(Icons.Outlined.Flag, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("3 de 5 concluídas hoje", fontSize = 14.sp, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { 0.6f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = PrimaryTeal,
            trackColor = Color.DarkGray
        )
    }
}

@Composable
fun FinanceCard(netWorth: Double) {
    Column(
        modifier = GlassModifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "PATRIMÔNIO TOTAL",
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            val wholePart = netWorth.toLong()
            Text(
                text = "R$ ${String.format("%,d", wholePart).replace(',', '.')}",
                fontFamily = FontFamily.Serif,
                fontSize = 32.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            val decimalPart = ((netWorth - wholePart) * 100).toInt()
            Text(
                text = ",${String.format("%02d", decimalPart)}",
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Bar Chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val bars = listOf(0.2f, 0.3f, 0.45f, 0.35f, 0.5f, 0.75f)
            val isPrimaryList = listOf(false, false, true, false, false, true)
            
            bars.forEachIndexed { index, height ->
                val barColor = when {
                    isPrimaryList[index] && index == 2 -> Color(0xFF4B7770) // darker teal
                    isPrimaryList[index] -> PrimaryTeal
                    else -> Color(0xFF38403F) // dark grey
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .fillMaxHeight(height)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(barColor)
                )
            }
        }
        
        // Base line
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x33FFFFFF)))
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Abr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            Text("Mai", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            Text("Jun", fontSize = 11.sp, color = PrimaryTeal)
        }
    }
}

@Composable
fun MarketCard() {
    Column(
        modifier = GlassModifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MERCADO",
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Outlined.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
        Spacer(modifier = Modifier.height(20.dp))
        
        MarketItem(text = "Café", isChecked = true)
        Spacer(modifier = Modifier.height(16.dp))
        MarketItem(text = "Maçãs", isChecked = false)
        Spacer(modifier = Modifier.height(16.dp))
        MarketItem(text = "Leite de Aveia", isChecked = false)
    }
}

@Composable
fun MarketItem(text: String, isChecked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isChecked) PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
            fontSize = 16.sp
        )
    }
}

@Composable
fun PetsCard(routines: List<PetRoutineEntity>) {
    Column(
        modifier = GlassModifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MARIE & CHURCHILL",
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Outlined.Pets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
        Spacer(modifier = Modifier.height(20.dp))

        // Vacinas Section
        Text(
            text = "PRÓXIMAS VACINAS",
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryTeal,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        PetMedicalEvent(
            petName = "Marie",
            title = "Vacina Antirrábica",
            detail = "Importada V10 + Raiva",
            date = "12/Jun",
            status = "Agendada",
            statusColor = PrimaryTeal
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0x0DFFFFFF)))
        Spacer(modifier = Modifier.height(16.dp))

        // Consultas Section
        Text(
            text = "CONSULTAS AGENDADAS",
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
            color = TertiaryPurple,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        PetMedicalEvent(
            petName = "Churchill",
            title = "Check-up Geral",
            detail = "Dra. Ana Silva - Clinic Vet",
            date = "24/Jun",
            status = "Confirmada",
            statusColor = TertiaryPurple
        )
    }
}

@Composable
fun PetMedicalEvent(
    petName: String,
    title: String,
    detail: String,
    date: String,
    status: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Pet Name Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = petName.uppercase(),
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.08f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = status.uppercase(),
                    color = statusColor.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}


@Composable
fun SystemCard(onChatClick: () -> Unit = {}) {
    var isThinking by remember { mutableStateOf(false) }
    
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        )
    )

    Row(
        modifier = GlassModifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x1A85D6C5)) // PrimaryTeal with alpha
                    .border(1.dp, Color(0x3385D6C5).copy(alpha = if (isThinking) pulseAlpha else 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome, 
                    contentDescription = null, 
                    tint = PrimaryTeal.copy(alpha = if (isThinking) pulseAlpha else 1f), 
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Tessera AI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = if (isThinking) "Processando..." else "Modelo local pronto", 
                    fontSize = 13.sp, 
                    color = PrimaryTeal.copy(alpha = if (isThinking) pulseAlpha else 0.8f)
                )
            }
        }
        
        Button(
            onClick = { onChatClick() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x0DFFFFFF), contentColor = MaterialTheme.colorScheme.onSurface),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            elevation = null,
            modifier = Modifier.border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Chat", fontSize = 13.sp)
        }
    }
}

@Composable
fun BottomNavBar(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onHoveredItemChange: (String?) -> Unit,
    currentRoute: String = "home",
    onNavigate: (String) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Tabs Capsule
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .then(GlassModifier)
                    .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(34.dp))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(Icons.Outlined.LightMode, "Hoje", currentRoute == "home") { onNavigate("home") }
                NavItem(Icons.Outlined.AccountBalanceWallet, "Finanças", currentRoute == "finance") { onNavigate("finance") }
                NavItem(Icons.Outlined.Storefront, "Mercado", currentRoute == "market") { onNavigate("market") }
            }

            // Detached Action (+) Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .then(GlassModifier)
                    .background(if (isExpanded) PrimaryTeal else Color.Transparent, CircleShape)
                    .border(1.dp, if (isExpanded) PrimaryTeal.copy(alpha = 0.5f) else Color(0x2BFFFFFF), CircleShape)
                    .clickable { onExpandedChange(!isExpanded) },
                contentAlignment = Alignment.Center
            ) {
                val iconRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 45f else 0f,
                    animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Add", 
                    tint = if (isExpanded) Color.Black else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(iconRotation)
                )
            }
        }
    }
}

@Composable
fun PopupOverlayItem(name: String, icon: ImageVector, offsetX: Dp, offsetY: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(56.dp)
            .background(Color(0xCC1E2322), CircleShape)
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = name, tint = PrimaryTeal, modifier = Modifier.size(24.dp))
            Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isActive: Boolean, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(48.dp, 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}


@Composable
fun PremiumGlassCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    alpha: Float,
    offsetY: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset(y = offsetY)
            .alpha(alpha)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(GlassModifier)
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f))
                    .border(1.dp, iconColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TesseraChatSheet(onDismiss: () -> Unit, netWorth: Double, petRoutines: List<PetRoutineEntity>) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    
    // Model for chat messages
    data class ChatMessage(val id: String, val text: String, val isUser: Boolean)
    var messages by remember { mutableStateOf(listOf(ChatMessage("msg_0", "Olá! Como posso ajudar você hoje?", false))) }
    var isThinking by remember { mutableStateOf(false) }

    // Try to load model
    val modelPath = "/storage/emulated/0/Download/gemma-2b-it-cpu-int4.bin"
    val llmManager = remember { 
        try {
            LocalLLMManager(context).apply { startInference(modelPath) } 
        } catch (e: Exception) {
            null
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xCC070909), // More translucent for glass effect
        modifier = Modifier.fillMaxHeight(0.95f) // Taller sheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .imePadding()
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryTeal.copy(alpha=0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Tessera AI", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("Assistente Pessoal", fontSize = 12.sp, color = PrimaryTeal)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(listOf(Color.Transparent, Color(0x33FFFFFF), Color.Transparent))
            ))
            Spacer(modifier = Modifier.height(16.dp))

            // Chat Messages
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages.size, key = { messages[it].id }) { index ->
                    val message = messages[index]
                    AnimatedVisibility(
                        visible = true,
                        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .clip(RoundedCornerShape(
                                        topStart = 20.dp, 
                                        topEnd = 20.dp, 
                                        bottomStart = if (message.isUser) 20.dp else 4.dp, 
                                        bottomEnd = if (message.isUser) 4.dp else 20.dp
                                    ))
                                    .then(
                                        if (message.isUser) {
                                            Modifier.background(Brush.linearGradient(listOf(PrimaryTeal, Color(0xFF0A84FF))))
                                        } else {
                                            Modifier
                                                .background(Color(0x1AFFFFFF))
                                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(
                                                    topStart = 20.dp, topEnd = 20.dp, 
                                                    bottomStart = 4.dp, bottomEnd = 20.dp
                                                ))
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = message.text, 
                                    color = if (message.isUser) Color.Black else Color.White, 
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
                
                if (isThinking) {
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + scaleIn(initialScale = 0.8f)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                                        .background(Color(0x1AFFFFFF))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val pulseAnim = androidx.compose.animation.core.rememberInfiniteTransition()
                                        val alpha by pulseAnim.animateFloat(
                                            initialValue = 0.3f, targetValue = 1f,
                                            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                                animation = tween(600, easing = androidx.compose.animation.core.FastOutLinearInEasing),
                                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                            )
                                        )
                                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = PrimaryTeal.copy(alpha = alpha), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Processando...", color = PrimaryTeal.copy(alpha = alpha), fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                    .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (prompt.isEmpty()) {
                        Text("Mensagem...", color = Color(0x80FFFFFF), fontSize = 15.sp)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryTeal)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (prompt.isNotBlank() && !isThinking) PrimaryTeal else Color(0x33FFFFFF))
                        .clickable(enabled = prompt.isNotBlank() && !isThinking) {
                            val userText = prompt
                            prompt = ""
                            val msgId = System.currentTimeMillis().toString()
                            messages = messages + ChatMessage(msgId, userText, true)
                            isThinking = true
                            
                            coroutineScope.launch {
                                if (llmManager != null) {
                                    val petsString = if (petRoutines.isEmpty()) "Sem tarefas de pets hoje." else petRoutines.joinToString("; ") { "${it.petName}: ${it.task} (${if(it.isCompleted) "Concluído" else "Pendente"})" }
                                    val hiddenContext = """
                                        Você é a Tessera AI, assistente do app Tessera Hub.
                                        - Nome: Kenned
                                        - Patrimônio: R$ ${String.format("%.2f", netWorth)}
                                        - Status dos Pets: $petsString
                                        Pergunta do usuário: "$userText"
                                    """.trimIndent()
                                    val response = llmManager.generateResponse(hiddenContext)
                                    messages = messages + ChatMessage("resp_$msgId", response, false)
                                } else {
                                    kotlinx.coroutines.delay(1500)
                                    messages = messages + ChatMessage("resp_$msgId", "A IA local não pôde ser iniciada.", false)
                                }
                                isThinking = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send, 
                        contentDescription = "Enviar", 
                        tint = if (prompt.isNotBlank() && !isThinking) Color.Black else Color.White.copy(alpha=0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
