package com.example

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
import androidx.compose.animation.core.spring
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.OuraCircularProgress

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
                    HomeScreen()
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
                        .background(Color(0xE6000000)) // Deep black translucent
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { isFabExpanded = false }
                ) {
                    val bottomOffset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 40.dp
                    Box(
                        modifier = Modifier.fillMaxSize().padding(bottom = bottomOffset),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        val offsetHealth by animateDpAsState(if (isFabExpanded) (-100).dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = 200f))
                        val offsetGoals by animateDpAsState(if (isFabExpanded) (-140).dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = 150f))
                        val offsetPetz by animateDpAsState(if (isFabExpanded) (-100).dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = 200f))
                        
                        val xOffsetHealth by animateDpAsState(if (isFabExpanded) (-80).dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = 200f))
                        val xOffsetPetz by animateDpAsState(if (isFabExpanded) 80.dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = 200f))

                        val alphaItems by androidx.compose.animation.core.animateFloatAsState(if (isFabExpanded) 1f else 0f, tween(200))
                        val scaleItems by androidx.compose.animation.core.animateFloatAsState(if (isFabExpanded) 1f else 0.5f, spring(dampingRatio = 0.6f, stiffness = 200f))

                        Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(y = (-40).dp)) {
                            // Health
                            Box(
                                modifier = Modifier
                                    .offset(x = xOffsetHealth, y = offsetHealth)
                                    .scale(scaleItems)
                                    .alpha(alphaItems)
                                    .size(72.dp)
                                    .background(Color(0xFF131817), CircleShape)
                                    .border(1.dp, PrimaryTeal.copy(alpha = 0.3f), CircleShape)
                                    .clickable { navigateAction("health"); isFabExpanded = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.MonitorHeart, contentDescription = "Saúde", tint = PrimaryTeal, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Saúde", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            // Goals
                            Box(
                                modifier = Modifier
                                    .offset(x = 0.dp, y = offsetGoals)
                                    .scale(scaleItems)
                                    .alpha(alphaItems)
                                    .size(72.dp)
                                    .background(Color(0xFF131817), CircleShape)
                                    .border(1.dp, PrimaryTeal.copy(alpha = 0.3f), CircleShape)
                                    .clickable { navigateAction("goals"); isFabExpanded = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.Flag, contentDescription = "Metas", tint = PrimaryTeal, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Metas", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            // Petz
                            Box(
                                modifier = Modifier
                                    .offset(x = xOffsetPetz, y = offsetPetz)
                                    .scale(scaleItems)
                                    .alpha(alphaItems)
                                    .size(72.dp)
                                    .background(Color(0xFF131817), CircleShape)
                                    .border(1.dp, PrimaryTeal.copy(alpha = 0.3f), CircleShape)
                                    .clickable { navigateAction("petz"); isFabExpanded = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.Pets, contentDescription = "Petz", tint = PrimaryTeal, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Petz", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            Text(
                                text = "O que você deseja ver?",
                                color = Color.White.copy(alpha = alphaItems * 0.9f),
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Serif,
                                modifier = Modifier.offset(y = (-220).dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val tesseraDb = remember { TesseraDatabase.getDatabase(context) }
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(tesseraDb.tesseraDao())
    )

    val netWorth by homeViewModel.totalNetWorth.collectAsState()
    val petRoutines by homeViewModel.petRoutines.collectAsState()

    LaunchedEffect(Unit) {
        homeViewModel.seedDatabase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                TopMetricsRow(netWorth)
                Spacer(modifier = Modifier.height(48.dp))
                HeroMetric()
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
        
        MainContent(netWorth, petRoutines)
        Spacer(modifier = Modifier.height(140.dp))
    }
}

@Composable
fun TopHeader() {
    var profileUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            profileUri = uri
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
fun TopMetricsRow(netWorth: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val formattedWorth = if (netWorth >= 1000) "${(netWorth / 1000).toInt()}k" else netWorth.toInt().toString()
        MetricItem(Icons.Outlined.AccountBalanceWallet, formattedWorth, "PATRIMÔNIO")
        MetricItemWithProgress(Icons.Outlined.Bedtime, "82", "SAÚDE", PrimaryTeal, 0.82f)
        MetricItem(Icons.Outlined.FavoriteBorder, "72", "FREQUÊNCIA")
        MetricItemWithProgress(Icons.Outlined.MonitorWeight, "78", "CORPO", TertiaryPurple, 0.78f)
    }
}

@Composable
fun MetricItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0x0AFFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), CircleShape),
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
fun MetricItemWithProgress(icon: ImageVector, value: String, label: String, progressColor: Color, progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0x0AFFFFFF))
                .border(1.dp, Color(0x1AFFFFFF), CircleShape),
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
                    sweepAngle = 360f * progress,
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
fun HeroMetric() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
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
                    color = PrimaryTeal,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * 0.75f,
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

            // Top center fire icon in thin circle
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
                    Icons.Outlined.LocalFireDepartment,
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
                    text = "PASSOS",
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "5.203",
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
            text = "Progresso do dia",
            fontFamily = FontFamily.Serif,
            fontSize = 30.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "A sua meta diária está na média, quase lá!\nContinue assim.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(netWorth: Double, petRoutines: List<PetRoutineEntity>) {
    var showFinance by remember { mutableStateOf(true) }
    var showMarket by remember { mutableStateOf(true) }
    var showPets by remember { mutableStateOf(true) }
    var showSystem by remember { mutableStateOf(true) }
    var showEditSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (showFinance) FinanceCard(netWorth)
        if (showMarket) MarketCard()
        if (showPets) PetsCard(petRoutines)
        if (showSystem) SystemCard()
        
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
                ModuleToggle("Finanças", showFinance) { showFinance = it }
                ModuleToggle("Mercado", showMarket) { showMarket = it }
                ModuleToggle("Pets", showPets) { showPets = it }
                ModuleToggle("Tessera AI / Sistema", showSystem) { showSystem = it }
            }
        }
    }
}

@Composable
fun ModuleToggle(name: String, isVisible: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 16.sp, color = Color.White)
        Switch(
            checked = isVisible,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryTeal, checkedTrackColor = PrimaryTeal.copy(alpha = 0.5f))
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
        
        if (routines.isEmpty()) {
            PetEvent(text = "Sem rotinas hoje", time = "--:--", color = Color.Gray, isPrimaryTime = false)
        } else {
            routines.forEachIndexed { index, routine ->
                PetEvent(
                    text = "${routine.petName}: ${routine.task}",
                    time = routine.time,
                    color = if (routine.isCompleted) PrimaryTeal else TertiaryPurple,
                    isPrimaryTime = !routine.isCompleted
                )
                if (index < routines.size - 1) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun PetEvent(text: String, time: String, color: Color, isPrimaryTime: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color)) // TODO inner shadow/blur if desired
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        }
        Text(
            text = time,
            color = if (isPrimaryTime) PrimaryTeal else MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SystemCard() {
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
            onClick = { isThinking = !isThinking },
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
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xE62A2F2E), Color(0xFA131817))
                    ),
                    shape = RoundedCornerShape(40.dp)
                )
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(40.dp))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(Icons.Outlined.LightMode, "Hoje", currentRoute == "home") { onNavigate("home") }
            NavItem(Icons.Outlined.AccountBalanceWallet, "Finanças", currentRoute == "finance") { onNavigate("finance") }
            NavItem(Icons.Outlined.Storefront, "Mercado", currentRoute == "market") { onNavigate("market") }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isExpanded) PrimaryTeal else Color(0x1AFFFFFF), CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                    .clickable { onExpandedChange(!isExpanded) },
                contentAlignment = Alignment.Center
            ) {
                val iconRotation by animateFloatAsState(if (isExpanded) 45f else 0f)
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Add", 
                    tint = if (isExpanded) Color.Black else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.scale(if (isExpanded) 1.2f else 1f).rotate(iconRotation)
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
fun AIChatTest() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var resposta by remember { mutableStateOf("A aguardar pergunta...") }
    
    // Caminho da pasta Downloads no Android (confirma se o nome do ficheiro .bin é igual a este)
    val modelPath = "/storage/emulated/0/Download/gemma-2b-it-cpu-int4.bin"
    
    val llmManager = remember { 
        LocalLLMManager(context).apply { startInference(modelPath) } 
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("IA Offline", style = MaterialTheme.typography.headlineMedium)
        
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Escreve a tua pergunta") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = {
                resposta = "A pensar..."
                coroutineScope.launch {
                    resposta = llmManager.generateResponse(prompt)
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Enviar")
        }
        
        Text(text = resposta, modifier = Modifier.padding(top = 16.dp))
    }
}
