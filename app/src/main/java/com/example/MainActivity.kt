@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.bounceClick
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.TesseraRepository
import com.example.viewmodel.TesseraViewModel
import com.example.viewmodel.TesseraViewModelFactory
import com.example.viewmodel.PetViewModel
import com.example.viewmodel.PetViewModelFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.tessera.app.data.local.AppDatabase as TesseraDatabase
import com.tessera.app.ui.viewmodel.HomeViewModel
import com.tessera.app.data.local.entity.PetRoutineEntity
import com.tessera.app.utils.BackupHelper
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

import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TesseraApp()
            }
        }
    }
}

val GlassModifier = PremiumGlassModifier

@Composable
fun TesseraApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { TesseraRepository(database.tesseraDao()) }
    val viewModel: TesseraViewModel = viewModel(factory = TesseraViewModelFactory(repository))
    val petViewModel: PetViewModel = viewModel(factory = PetViewModelFactory(repository))

    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    var isFirstTime by remember { mutableStateOf(sharedPrefs.getBoolean("first_time_user", true)) }

    if (isFirstTime) {
        OnboardingScreen(
            viewModel = viewModel,
            onCompleted = {
                isFirstTime = false
                sharedPrefs.edit().putBoolean("first_time_user", false).apply()
            }
        )
        return
    }

    var isBiometricEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_enabled", false)) }
    var isUnlocked by remember { mutableStateOf(!isBiometricEnabled) }

    if (!isUnlocked) {
        LockScreen(onUnlocked = { isUnlocked = true })
        return
    }

    LaunchedEffect(Unit) {
        viewModel.initializeDataIfNeeded()
    }

    val navController = rememberNavController()
    val activity = context as? android.app.Activity
    val startRoute = remember(activity) { activity?.intent?.getStringExtra("route") }
    LaunchedEffect(startRoute) {
        if (startRoute != null) {
            navController.navigate(startRoute) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            activity?.intent?.removeExtra("route")
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val navigateAction: (String) -> Unit = { route ->
        val targetRoute = when (route) {
            "chronos" -> {
                viewModel.selectedGoalsTab = 1
                "goals"
            }
            "focus" -> {
                viewModel.selectedGoalsTab = 2
                "goals"
            }
            else -> route
        }
        if (targetRoute == "home") {
            navController.popBackStack(navController.graph.findStartDestination().id, inclusive = false)
        } else {
            navController.navigate(targetRoute) {
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
            val contentBlur by animateDpAsState(if (isFabExpanded) 32.dp else 0.dp, tween(300))
            Box(modifier = Modifier.fillMaxSize().blur(contentBlur)) {
                NavHost(navController = navController, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                composable("home") {
                    HomeScreen(onNavigate = navigateAction)
                }
                composable("settings") {
                    SettingsScreen(viewModel = viewModel, onBack = { 
                        navController.popBackStack()
                    })
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
                    HealthScreen(viewModel = viewModel, onHomeClick = { 
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
                    }, viewModel = viewModel)
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
                    PetzScreen(
                        onHomeClick = { 
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        viewModel = viewModel,
                        petViewModel = petViewModel
                    )
                }
                composable("apartment") {
                    ApartmentScreen(onHomeClick = { 
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
                }
                composable("daily") {
                    DailyScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigate = navigateAction
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xD9070909), // Soft vignette fading in
                                Color(0xFF070909)  // Deep rich black base matching HomeScreen background
                            )
                        )
                    )
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                BottomNavBar(
                    isExpanded = isFabExpanded,
                    onExpandedChange = { isFabExpanded = it },
                    onHoveredItemChange = { fabHoveredItem = it },
                    currentRoute = currentRoute,
                    onNavigate = navigateAction
                )
            }
            } // Fecha o Box do blur

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
                        .background(Color(0x99000000)) // Mais transparência para ver o blur do fundo
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
                            
                            val itemsAlpha by animateFloatAsState(if (isFabExpanded) 1f else 0f, tween(400, delayMillis = 50))
                            val itemsOffset by animateDpAsState(if (isFabExpanded) 0.dp else 60.dp, spring(dampingRatio = 0.8f, stiffness = 150f))
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        PremiumGridTile(
                                            title = "Saúde\n& Corpo",
                                            icon = Icons.Outlined.MonitorHeart,
                                            iconColor = PrimaryTeal,
                                            alpha = itemsAlpha,
                                            offsetY = itemsOffset,
                                            onClick = { navigateAction("health"); isFabExpanded = false }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        PremiumGridTile(
                                            title = "Foco\n& Rotinas",
                                            icon = Icons.Outlined.Flag,
                                            iconColor = Color(0xFFF9A826),
                                            alpha = itemsAlpha,
                                            offsetY = itemsOffset,
                                            onClick = { navigateAction("goals"); isFabExpanded = false }
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        PremiumGridTile(
                                            title = "Meu\nApartamento",
                                            icon = Icons.Outlined.Construction,
                                            iconColor = SecondaryGold,
                                            alpha = itemsAlpha,
                                            offsetY = itemsOffset,
                                            onClick = { navigateAction("apartment"); isFabExpanded = false }
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        PremiumGridTile(
                                            title = "Meus\nPetz",
                                            icon = Icons.Outlined.Pets,
                                            iconColor = TertiaryPurple,
                                            alpha = itemsAlpha,
                                            offsetY = itemsOffset,
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
    }
}

fun getDatabaseSizeInKB(context: android.content.Context): String {
    return try {
        val dbFile = context.getDatabasePath("tessera_database.db")
        if (dbFile.exists()) {
            val bytes = dbFile.length()
            val kb = bytes / 1024.0
            String.format(java.util.Locale.US, "%.1f KB", kb)
        } else {
            "0 KB"
        }
    } catch (e: Exception) {
        "Indisponível"
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
    val totalIncome by homeViewModel.totalIncome.collectAsState()
    val totalExpense by homeViewModel.totalExpense.collectAsState()
    
    val mainViewModel: TesseraViewModel = viewModel(factory = com.example.viewmodel.TesseraViewModelFactory(com.example.data.TesseraRepository(com.example.data.AppDatabase.getDatabase(context).tesseraDao())))
    val petEvents by mainViewModel.allPetEvents.collectAsState(initial = emptyList())
    val stepsRecords by mainViewModel.allStepsRecords.collectAsState(initial = emptyList())
    val marketItems by mainViewModel.pendingMarketItems.collectAsState(initial = emptyList())
    val medications by mainViewModel.allMedications.collectAsState(initial = emptyList())
    
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    var isBiometricEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_enabled", false)) }
    
    val transactions by mainViewModel.allTransactions.collectAsState()
    val bankAccounts by mainViewModel.allBankAccounts.collectAsState()
    val realIncome = remember(transactions) { transactions.filter { it.isIncome }.sumOf { it.value } }
    val realExpense = remember(transactions) { transactions.filter { !it.isIncome }.sumOf { it.value } }
    val realBalance = realIncome - realExpense
    val realPatrimony = remember(bankAccounts) { bankAccounts.sumOf { it.balance } }
    
    var showChatSheet by remember { mutableStateOf(false) }

    var backgroundUri by remember {
        mutableStateOf(
            sharedPrefs.getString("home_background_uri", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop")
            ?: "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop"
        )
    }

    val scrollState = rememberScrollState()

    val todayStart = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayEnd = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 23)
        set(java.util.Calendar.MINUTE, 59)
        set(java.util.Calendar.SECOND, 59)
        set(java.util.Calendar.MILLISECOND, 999)
    }.timeInMillis
    val todaySteps = stepsRecords.filter { it.startTime >= todayStart && it.endTime <= todayEnd }.sumOf { it.count }

    LaunchedEffect(Unit) {
        // removed seedDatabase
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070909))
    ) {
        AsyncImage(
            model = backgroundUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0x00070909),
                            0.15f to Color(0x11070909),
                            0.45f to Color(0xAA070909),
                            0.70f to Color(0xFF070909),
                            1.0f to Color(0xFF070909)
                        )
                    )
                )
        )

        // Ambient Breathing Glow
        val calendarForTheme = java.util.Calendar.getInstance()
        val hourForTheme = calendarForTheme.get(java.util.Calendar.HOUR_OF_DAY)
        val glowColor = when (hourForTheme) {
            in 5..11 -> Color(0xFFFF8A65) // Warm peach sunrise
            in 12..17 -> Color(0xFFFFD54F) // Golden yellow sun
            in 18..22 -> Color(0xFF9575CD) // Twilight violet/purple
            else -> Color(0xFF4FC3F7) // Midnight deep blue
        }

        val infiniteTransition = rememberInfiniteTransition(label = "GlowBreathe")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.08f,
            targetValue = 0.20f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowAlpha"
        )
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(6000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowScale"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                    alpha = (1f - (scrollState.value / 300f)).coerceIn(0f, 1f)
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(200f, 0f),
                        radius = 600f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val scrollVal = scrollState.value
                        alpha = (1f - (scrollVal / 250f)).coerceIn(0f, 1f)
                        val scale = (1f - (scrollVal / 1200f)).coerceIn(0.85f, 1f)
                        scaleX = scale
                        scaleY = scale
                        translationY = -scrollVal * 0.15f
                    }
            ) {
                TopHeader(onOpenSettings = { onNavigate("settings") })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val scrollVal = scrollState.value
                        alpha = (1f - (scrollVal / 400f)).coerceIn(0f, 1f)
                        val scale = (1f - (scrollVal / 1500f)).coerceIn(0.9f, 1f)
                        scaleX = scale
                        scaleY = scale
                        translationY = -scrollVal * 0.1f
                    }
            ) {
                val habits by mainViewModel.allHabits.collectAsState(initial = emptyList<com.example.data.Habit>())
                val weightRecords by mainViewModel.allWeightRecords.collectAsState(initial = emptyList())

                val latestWeight = weightRecords.lastOrNull()?.weightKg ?: 75.2

                TopMetricsRow(
                    patrimony = realPatrimony,
                    netWorth = realBalance,
                    totalIncome = realIncome,
                    totalExpense = realExpense,
                    habits = habits,
                    latestWeight = latestWeight,
                    todaySteps = todaySteps,
                    onNavigate = onNavigate
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val scrollVal = scrollState.value
                        alpha = (1f - (scrollVal / 550f)).coerceIn(0f, 1f)
                        val scale = (1f - (scrollVal / 2000f)).coerceIn(0.92f, 1f)
                        scaleX = scale
                        scaleY = scale
                        translationY = -scrollVal * 0.08f
                    }
            ) {
                val habits by mainViewModel.allHabits.collectAsState(initial = emptyList<com.example.data.Habit>())
                HeroMetric(habits, todaySteps, onNavigate)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            DailyBriefWidget(onClick = { onNavigate("daily") })
            
            Spacer(modifier = Modifier.height(48.dp))
            MainContent(netWorth, petEvents, mainViewModel, onNavigate)
            Spacer(modifier = Modifier.height(140.dp))
        }
    }

    if (showChatSheet) {
        TesseraChatSheet(
            onDismiss = { showChatSheet = false },
            netWorth = netWorth,
            petEvents = petEvents,
            marketItems = marketItems,
            medications = medications
        )
    }
}

@Composable
fun TopHeader(onOpenSettings: () -> Unit) {
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
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val profileFile = java.io.File(context.filesDir, "user_profile_photo.jpg")
                    profileFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val localUri = Uri.fromFile(profileFile)
                    profileUri = localUri
                    sharedPrefs.edit().putString("user_profile_uri", localUri.toString()).apply()
                    Toast.makeText(context, "Foto de perfil salva com sucesso!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                profileUri = uri
                sharedPrefs.edit().putString("user_profile_uri", uri.toString()).apply()
            }
        }
    }
    
    var isHeaderVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isHeaderVisible = true
    }

    AnimatedVisibility(
        visible = isHeaderVisible,
        enter = fadeIn(tween(800)) + slideInVertically(initialOffsetY = { -30 }, animationSpec = tween(800, easing = FastOutSlowInEasing))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x0AFFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                        .bounceClick {
                            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileUri != null) {
                        AsyncImage(
                            model = profileUri,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                val calendar = java.util.Calendar.getInstance()
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val greeting = when (hour) {
                    in 0..11 -> "Bom dia"
                    in 12..17 -> "Boa tarde"
                    else -> "Boa noite"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$greeting, Kenned",
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Normal
                    )

                    val timeIcon = when (hour) {
                        in 5..11 -> Icons.Outlined.WbSunny
                        in 12..17 -> Icons.Outlined.LightMode
                        in 18..22 -> Icons.Outlined.Nightlight
                        else -> Icons.Outlined.Bedtime
                    }
                    val iconColor = when (hour) {
                        in 5..11 -> Color(0xFFFFB74D)
                        in 12..17 -> Color(0xFFFFD54F)
                        in 18..22 -> Color(0xFFB39DDB)
                        else -> Color(0xFF81D4FA)
                    }

                    val rotationTransition = rememberInfiniteTransition(label = "IconAnim")
                    val rotationAngle by if (hour in 5..17) {
                        rotationTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(20000, easing = LinearEasing)
                            ),
                            label = "SunRotate"
                        )
                    } else {
                        rotationTransition.animateFloat(
                            initialValue = -10f,
                            targetValue = 10f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(3000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "MoonSway"
                        )
                    }

                    Icon(
                        imageVector = timeIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                rotationZ = rotationAngle
                            }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x0AFFFFFF))
                    .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                    .bounceClick(onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Configurações",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TopMetricsRow(
    patrimony: Double,
    netWorth: Double,
    totalIncome: Double,
    totalExpense: Double,
    habits: List<com.example.data.Habit>,
    latestWeight: Double,
    todaySteps: Long,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    
    // Widget 1 (Financeiro)
    var financeIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(5 * 60 * 1000L)
            financeIndex = (financeIndex + 1) % 4
        }
    }

    // Widget 2 (Saúde)
    var healthIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(5 * 60 * 1000L)
            healthIndex = (healthIndex + 1) % 3
        }
    }

    // Widget 4 (Apartamento)
    val aptProgress = sharedPrefs.getFloat("apartment_progress", 0f)

    var isRowVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100L)
        isRowVisible = true
    }

    AnimatedVisibility(
        visible = isRowVisible,
        enter = fadeIn(tween(600)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(600, easing = FastOutSlowInEasing))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Widget 1 (Financeiro) with smooth crossfade rotation!
            Box(modifier = Modifier.width(76.dp)) {
                Crossfade(targetState = financeIndex, animationSpec = tween(500), label = "FinanceRotation") { idx ->
                    val valIdx = when(idx) {
                        0 -> patrimony
                        1 -> netWorth
                        2 -> totalIncome
                        else -> totalExpense
                    }
                    val labelIdx = when(idx) {
                        0 -> "PATRIMÔNIO"
                        1 -> "SALDO"
                        2 -> "RECEITAS"
                        else -> "DESPESAS"
                    }
                    val iconIdx = when(idx) {
                        0 -> Icons.Outlined.AccountBalance
                        1 -> Icons.Outlined.AccountBalanceWallet
                        2 -> Icons.Outlined.ArrowUpward
                        else -> Icons.Outlined.ArrowDownward
                    }
                    val formattedIdx = if (valIdx >= 1000) "${(valIdx / 1000).toInt()}k" else valIdx.toInt().toString()
                    MetricItem(iconIdx, formattedIdx, labelIdx, onClick = { onNavigate("finance") })
                }
            }

            // Widget 2 (Saúde) with smooth crossfade rotation!
            Box(modifier = Modifier.width(76.dp)) {
                Crossfade(targetState = healthIndex, animationSpec = tween(500), label = "HealthRotation") { idx ->
                    val iconIdx = when (idx) {
                        0 -> Icons.Outlined.Bedtime
                        1 -> Icons.Outlined.MonitorWeight
                        else -> Icons.Outlined.DirectionsWalk
                    }
                    val valIdx = when (idx) {
                        0 -> "8.2h"
                        1 -> String.format(java.util.Locale("pt", "BR"), "%.1f", latestWeight)
                        else -> todaySteps.toString()
                    }
                    val labelIdx = when (idx) {
                        0 -> "SONO"
                        1 -> "PESO"
                        else -> "PASSOS"
                    }
                    val progressIdx = when (idx) {
                        0 -> 0.82f
                        1 -> (latestWeight / 120.0f).toFloat().coerceIn(0f, 1f)
                        else -> (todaySteps.toFloat() / 10000f).coerceIn(0f, 1f)
                    }
                    val colorIdx = when (idx) {
                        0 -> PrimaryTeal
                        1 -> TertiaryPurple
                        else -> Color(0xFF4D96FF)
                    }
                    MetricItemWithProgress(iconIdx, valIdx, labelIdx, colorIdx, progressIdx, onClick = { onNavigate("health") })
                }
            }

            // Widget 3 (Contexto / Momento)
            Box(modifier = Modifier.width(76.dp)) {
                val calendar = java.util.Calendar.getInstance()
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val contextIcon = when (hour) {
                    in 5..11 -> Icons.Outlined.WbSunny
                    in 12..17 -> Icons.Outlined.WorkOutline
                    in 18..22 -> Icons.Outlined.Nightlight
                    else -> Icons.Outlined.Bedtime
                }
                val contextValue = when (hour) {
                    in 5..11 -> "Café"
                    in 12..17 -> "Foco"
                    in 18..22 -> "Relax"
                    else -> "Dormir"
                }
                val contextLabel = when (hour) {
                    in 5..11 -> "MATINAL"
                    in 12..17 -> "PRODUTIVO"
                    in 18..22 -> "MOMENTO"
                    else -> "CONTEXTO"
                }
                val contextProgress = when (hour) {
                    in 5..11 -> 0.25f
                    in 12..17 -> 0.60f
                    in 18..22 -> 0.85f
                    else -> 1.0f
                }
                val contextColor = when (hour) {
                    in 5..11 -> Color(0xFFF9A826)
                    in 12..17 -> Color(0xFF4D96FF)
                    in 18..22 -> Color(0xFFD7B4F3)
                    else -> Color(0xFF71D7CD)
                }
                
                MetricItemWithProgress(
                    icon = contextIcon,
                    value = contextValue,
                    label = contextLabel,
                    progressColor = contextColor,
                    progress = contextProgress,
                    onClick = { 
                        Toast.makeText(context, "Modo $contextLabel: $contextValue", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Widget 4 (Apartamento)
            MetricItemWithProgress(Icons.Outlined.Construction, "${(aptProgress * 100).toInt()}%", "OBRA", SecondaryGold, aptProgress, onClick = { onNavigate("apartment") })
        }
    }
}

@Composable
fun MetricItem(icon: ImageVector, value: String, label: String, onClick: () -> Unit) {
    val displayFontSize = if (value.length > 5) 12.sp else if (value.length > 4) 14.sp else 16.sp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x24FFFFFF), // Reflective top gloss
                            Color(0x05FFFFFF)  // Frosted clear base
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x40FFFFFF), // Bright top edge bevel shine
                            Color(0x0AFFFFFF)  // Faded bottom edge
                        )
                    ),
                    shape = CircleShape
                )
                .bounceClick { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = displayFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    val displayFontSize = if (value.length > 5) 11.sp else if (value.length > 4) 13.sp else 15.sp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x24FFFFFF),
                            Color(0x05FFFFFF)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x40FFFFFF),
                            Color(0x0AFFFFFF)
                        )
                    ),
                    shape = CircleShape
                )
                .bounceClick { onClick() },
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = displayFontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(label, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HeroMetric(habits: List<com.example.data.Habit>, todaySteps: Long = 0, onNavigate: (String) -> Unit) {
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    
    val isMorning = hour in 5..11
    val isAfternoon = hour in 12..17
    
    val completedHabits = habits.count { it.isCompleted }
    val totalHabits = habits.size
    val habitScore = if (totalHabits > 0) ((completedHabits.toFloat() / totalHabits) * 100).toInt() else 85
    val habitProgressValue = if (totalHabits > 0) completedHabits.toFloat() / totalHabits else 0.85f

    val title = if (isMorning) "PRONTIDÃO" else if (isAfternoon) "ATIVIDADE" else "RELAXAMENTO"
    val value = if (isMorning) {
        "$habitScore"
    } else if (isAfternoon) {
        if (todaySteps >= 1000) {
            String.format(java.util.Locale("pt", "BR"), "%.1fk", todaySteps.toFloat() / 1000f)
        } else {
            todaySteps.toString()
        }
    } else {
        val targetHour = 23
        val targetMinute = 0
        val currentMinutesOfDay = hour * 60 + minute
        val targetMinutesOfDay = targetHour * 60 + targetMinute
        
        if (currentMinutesOfDay >= targetMinutesOfDay || hour < 5) {
            "Dormir"
        } else {
            val startRelaxMinutesOfDay = 21 * 60
            if (currentMinutesOfDay < startRelaxMinutesOfDay) {
                val diffMin = startRelaxMinutesOfDay - currentMinutesOfDay
                if (diffMin >= 60) "${(diffMin / 60)}h" else "${diffMin}m"
            } else {
                val remainingMin = targetMinutesOfDay - currentMinutesOfDay
                if (remainingMin >= 60) {
                    val hrs = remainingMin / 60
                    val mins = remainingMin % 60
                    if (mins == 0) "${hrs}h" else "${hrs}h${mins}m"
                } else {
                    "${remainingMin}m"
                }
            }
        }
    }
    
    val subtitle = if (isMorning) "Bom dia" else if (isAfternoon) "Progresso da tarde" else "Boa noite"
    val subtext = if (isMorning) {
        "Você completou $completedHabits de $totalHabits hábitos diários!\nÓtimo progresso."
    } else if (isAfternoon) {
        if (todaySteps >= 10000) {
            "Meta de 10.000 passos alcançada! Excelente atividade hoje."
        } else {
            "Você deu $todaySteps passos hoje. Faltam ${10000 - todaySteps} para sua meta diária."
        }
    } else {
        val targetHour = 23
        val targetMinute = 0
        val currentMinutesOfDay = hour * 60 + minute
        val targetMinutesOfDay = targetHour * 60 + targetMinute
        
        if (currentMinutesOfDay >= targetMinutesOfDay || hour < 5) {
            "Já passou do seu horário de dormir recomendado. Desligue as telas e descanse."
        } else {
            val startRelaxMinutesOfDay = 21 * 60
            if (currentMinutesOfDay < startRelaxMinutesOfDay) {
                "A noite chegou. O período de relaxamento guiado começa às 21h."
            } else {
                val remainingMin = targetMinutesOfDay - currentMinutesOfDay
                "Faltam $remainingMin min para seu horário ideal de sono. Comece a desacelerar."
            }
        }
    }
    
    val icon = if (isMorning) Icons.Outlined.WbSunny else if (isAfternoon) Icons.Outlined.DirectionsWalk else Icons.Outlined.Bedtime
    val progressValue = if (isMorning) {
        habitProgressValue
    } else if (isAfternoon) {
        (todaySteps.toFloat() / 10000f).coerceIn(0f, 1f)
    } else {
        val targetHour = 23
        val targetMinute = 0
        val currentMinutesOfDay = hour * 60 + minute
        val targetMinutesOfDay = targetHour * 60 + targetMinute
        val startRelaxMinutesOfDay = 21 * 60
        
        if (currentMinutesOfDay >= targetMinutesOfDay || hour < 5) {
            1.0f
        } else if (currentMinutesOfDay < startRelaxMinutesOfDay) {
            0.0f
        } else {
            val totalRelaxPeriod = targetMinutesOfDay - startRelaxMinutesOfDay
            val elapsed = currentMinutesOfDay - startRelaxMinutesOfDay
            (elapsed.toFloat() / totalRelaxPeriod.toFloat()).coerceIn(0f, 1f)
        }
    }
    val progressColor = if (isMorning) Color(0xFFF9A826) else if (isAfternoon) PrimaryTeal else TertiaryPurple

    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationStarted = true }
    
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationStarted) progressValue else 0f,
        animationSpec = tween(durationMillis = 1500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )

    var isHeroVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200L)
        isHeroVisible = true
    }

    AnimatedVisibility(
        visible = isHeroVisible,
        enter = fadeIn(tween(800)) + scaleIn(initialScale = 0.95f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    ) {
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

                    drawArc(
                        color = Color(0x33DFE3E2),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = arcRectSize,
                        topLeft = topLeft
                    )
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

                    val angles = listOf(155f, 212.5f, 270f, 327.5f, 25f)
                    for (angle in angles) {
                        val angleRad = Math.toRadians(angle.toDouble())
                        val x = centerVector.x + radius * Math.cos(angleRad).toFloat()
                        val y = centerVector.y + radius * Math.sin(angleRad).toFloat()
                        drawCircle(color = Color(0xFFDFE3E2), radius = 2.dp.toPx(), center = Offset(x, y))
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = topOfArcY - 12.dp)
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
                    val heroFontSize = if (value.length > 5) 28.sp else if (value.length > 3) 44.sp else 62.sp
                    val heroLineHeight = if (value.length > 5) 36.sp else if (value.length > 3) 50.sp else 72.sp
                    Text(
                        text = value,
                        fontFamily = FontFamily.Serif,
                        fontSize = heroFontSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Normal,
                        lineHeight = heroLineHeight
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
}

@OptIn(ExperimentalMaterial3Api::class)
data class ModuleConfig(val id: String, val name: String, var isVisible: Boolean, var order: Int)

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun MainContent(netWorth: Double, petEvents: List<com.example.data.PetEvent>, mainViewModel: com.example.viewmodel.TesseraViewModel, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    var showEditSheet by remember { mutableStateOf(false) }

    val defaultModules = listOf(
        ModuleConfig("finance", "Finanças", true, 0),
        ModuleConfig("market", "Mercado", true, 1),
        ModuleConfig("pets", "Petz", true, 2),
        ModuleConfig("health", "Saúde", false, 3),
        ModuleConfig("goals", "Foco & Rotinas", false, 4)
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
                                if (parts[0] == "system") null
                                else {
                                    val name = if (parts[0] == "health") "Saúde" else parts[1]
                                    ModuleConfig(parts[0], name, parts[2].toBoolean(), parts[3].toInt())
                                }
                            } else null
                        }
                        if (parsed.isNotEmpty()) {
                            parsed.sortedBy { it.order }.mapIndexed { i, m -> m.copy(order = i) }
                        } else defaultModules
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
        val visibleModules = remember { modules.filter { it.isVisible }.sortedBy { it.order } }
        visibleModules.forEachIndexed { index, module ->
            var isVisible by remember { mutableStateOf(false) }
            LaunchedEffect(module.id) {
                kotlinx.coroutines.delay(index * 120L)
                isVisible = true
            }
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)) + 
                        slideInVertically(
                            initialOffsetY = { 60 },
                            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                        )
            ) {
                when (module.id) {
                    "finance" -> {
                        val transactions by mainViewModel.allTransactions.collectAsState()
                        HomeFinanceWidget(transactions, onNavigate)
                    }
                    "market" -> {
                        val marketItems by mainViewModel.pendingMarketItems.collectAsState()
                        MarketCard(marketItems, onNavigate)
                    }
                    "pets" -> PetsCard(petEvents, onNavigate)
                    "health" -> {
                        val medications by mainViewModel.allMedications.collectAsState()
                        val weightRecords by mainViewModel.allWeightRecords.collectAsState(initial = emptyList())
                        val stepsRecords by mainViewModel.allStepsRecords.collectAsState(initial = emptyList())
                        val healthProfile by mainViewModel.healthProfile.collectAsState(initial = null)

                        val latestWeight = weightRecords.lastOrNull()?.weightKg ?: 0.0
                        val heightCm = healthProfile?.heightCm ?: 0.0
                        val height = if (heightCm > 0.0) heightCm / 100.0 else 1.75
                        val bmi = if (latestWeight > 0.0 && height > 0.0) latestWeight / (height * height) else 0.0

                        val todayStart = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val todayEnd = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 23)
                            set(java.util.Calendar.MINUTE, 59)
                            set(java.util.Calendar.SECOND, 59)
                            set(java.util.Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        val todaySteps = stepsRecords.filter { it.startTime >= todayStart && it.endTime <= todayEnd }.sumOf { it.count }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick { onNavigate("health") }
                        ) {
                            HealthWidget(
                                medications = medications,
                                onToggleMedication = { med -> mainViewModel.toggleMedicationTaken(med) },
                                latestWeight = latestWeight,
                                todaySteps = todaySteps,
                                bmi = bmi
                            )
                        }
                    }
                    "goals" -> {
                        val habits by mainViewModel.allHabits.collectAsState(initial = emptyList())
                        val purchaseGoals by mainViewModel.allPurchaseGoals.collectAsState(initial = emptyList())
                        val routines by mainViewModel.allRoutines.collectAsState(initial = emptyList())
                        GoalsWidget(
                            habits = habits,
                            purchaseGoals = purchaseGoals,
                            routines = routines,
                            onToggleHabit = { habit -> mainViewModel.toggleHabitCompleted(habit) },
                            onNavigate = { route ->
                                if (route == "goals") {
                                    mainViewModel.selectedGoalsTab = 0
                                }
                                onNavigate(route)
                            }
                        )
                    }
                }
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
            Text("Editar Widgets")
        }
    }

    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            containerColor = Color(0xFF131817)
        ) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
                Text("Editar Widgets", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
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
                                val sorted = modules.sortedBy { it.order }.toMutableList()
                                val temp = sorted[index]
                                sorted[index] = sorted[index - 1]
                                sorted[index - 1] = temp
                                sorted.forEachIndexed { i, m -> m.order = i }
                                saveModules(sorted)
                            }
                        },
                        onMoveDown = {
                            if (index < modules.size - 1) {
                                val sorted = modules.sortedBy { it.order }.toMutableList()
                                val temp = sorted[index]
                                sorted[index] = sorted[index + 1]
                                sorted[index + 1] = temp
                                sorted.forEachIndexed { i, m -> m.order = i }
                                saveModules(sorted)
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
fun HealthWidget(
    medications: List<com.example.data.Medication>,
    onToggleMedication: (com.example.data.Medication) -> Unit,
    latestWeight: Double,
    todaySteps: Long,
    bmi: Double
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    val todayDate = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    var userFeeling by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val savedFeelingDate = sharedPrefs.getString("feeling_date", "")
        if (savedFeelingDate == todayDate) {
            userFeeling = sharedPrefs.getString("user_feeling", "") ?: ""
        } else {
            sharedPrefs.edit().putString("feeling_date", todayDate).putString("user_feeling", "").apply()
            userFeeling = ""
        }
    }

    var showFeelingDialog by remember { mutableStateOf(false) }

    val feelings = listOf(
        Triple("⚡", "Energizado", PrimaryTeal),
        Triple("😌", "Calmo", SecondaryGold),
        Triple("😴", "Cansado", TertiaryPurple),
        Triple("🧘", "Focado", Color(0xFF64B5F6)), // Light Blue
        Triple("😰", "Estressado", Color(0xFFE57373)) // Light Red
    )

    if (showFeelingDialog) {
        AlertDialog(
            onDismissRequest = { showFeelingDialog = false },
            title = { Text("Qual é a sua vibe hoje?", fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    feelings.forEach { (emoji, label, color) ->
                        TextButton(
                            onClick = {
                                userFeeling = "$emoji $label"
                                sharedPrefs.edit().putString("user_feeling", userFeeling).apply()
                                showFeelingDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(label, fontSize = 16.sp, color = color, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = SurfaceVariantDark,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Find closest unchecked medication
    val uncheckedMeds = medications.filter { !it.isTaken }
    val nextMed = if (uncheckedMeds.isNotEmpty()) {
        val nowStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val futureMeds = uncheckedMeds.filter { it.time >= nowStr }.sortedBy { it.time }
        if (futureMeds.isNotEmpty()) {
            futureMeds.first()
        } else {
            uncheckedMeds.sortedBy { it.time }.first()
        }
    } else {
        null
    }

    Column(modifier = GlassModifier.fillMaxWidth().padding(24.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAÚDE",
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Outlined.MonitorHeart,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left column: Next Medication
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = "PRÓXIMO MEDICAMENTO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (nextMed != null) {
                    MedItem(nextMed.name, nextMed.time, nextMed.isTaken) { _ ->
                        onToggleMedication(nextMed)
                    }
                } else {
                    Text("Nenhum pendente hoje.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right column: Vibe
            Column(
                modifier = Modifier.weight(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VIBE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (userFeeling.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x0AFFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { showFeelingDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Adicionar Vibe",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    val matchingFeeling = feelings.firstOrNull { userFeeling.contains(it.second) }
                    val feelingColor = matchingFeeling?.third ?: PrimaryTeal
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showFeelingDialog = true }
                    ) {
                        Text(
                            text = userFeeling.substringBefore(" "),
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userFeeling.substringAfter(" "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = feelingColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
        Spacer(modifier = Modifier.height(20.dp))

        // Row of Steps, IMC, and Weight using minimal icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HealthMiniCard(
                icon = Icons.Outlined.DirectionsWalk,
                label = "Passos",
                value = "$todaySteps",
                tint = Color(0xFF4D96FF),
                modifier = Modifier.weight(1f)
            )
            HealthMiniCard(
                icon = Icons.Outlined.Analytics,
                label = "IMC",
                value = if (bmi > 0.0) String.format(java.util.Locale("pt", "BR"), "%.1f", bmi) else "--",
                tint = PrimaryTeal,
                modifier = Modifier.weight(1f)
            )
            HealthMiniCard(
                icon = Icons.Outlined.MonitorWeight,
                label = "Peso",
                value = if (latestWeight > 0.0) String.format(java.util.Locale("pt", "BR"), "%.1f kg", latestWeight) else "--",
                tint = TertiaryPurple,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HealthMiniCard(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0AFFFFFF))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF81928F),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun MedItem(name: String, time: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isChecked) PrimaryTeal else Color(0x1AFFFFFF))
                .border(1.dp, if (isChecked) PrimaryTeal else Color(0x33FFFFFF), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Color.White,
                style = if (isChecked) androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle.Default
            )
            Text(
                text = time,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun GoalsWidget(
    habits: List<com.example.data.Habit>,
    purchaseGoals: List<com.example.data.PurchaseGoal>,
    routines: List<com.example.data.Routine>,
    onToggleHabit: (com.example.data.Habit) -> Unit,
    onNavigate: (String) -> Unit
) {
    val completedRituais = habits.count { it.isCompleted }
    val totalRituais = habits.size
    
    val completedMetas = purchaseGoals.count { it.currentValue >= it.targetValue }
    val totalMetas = purchaseGoals.size
    
    val totalCompleted = completedMetas + completedRituais
    val totalGoals = totalMetas + totalRituais
    val progress = if (totalGoals > 0) totalCompleted.toFloat() / totalGoals.toFloat() else 0f

    Column(
        modifier = GlassModifier
            .fillMaxWidth()
            .bounceClick { onNavigate("goals") }
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FOCO & ROTINAS",
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pomodoro Shortcut Icon Button
                IconButton(
                    onClick = { onNavigate("focus") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = "Focus Time",
                        tint = Color(0xFFD7B4F3),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Icon(Icons.Outlined.Flag, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
            }
        }
        
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$totalCompleted de $totalGoals concluídas hoje",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = PrimaryTeal,
                    trackColor = Color(0x1AFFFFFF)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Summaries of activities and rituals
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Metas de Compra (Lista de Desejos)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = SecondaryGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lista de Desejos: ",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$completedMetas de $totalMetas concluídos",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Rotinas (Chronos)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("chronos") },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Spa,
                            contentDescription = null,
                            tint = Color(0xFF71D7CD),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fluxo de Rotinas (Chronos):",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${routines.size} ativas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF71D7CD)
                    )
                }

                if (routines.isEmpty()) {
                    Text(
                        text = "Nenhuma rotina cadastrada.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 24.dp)
                    )
                } else {
                    routines.take(2).forEach { routine ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x0AFFFFFF))
                                .clickable { onNavigate("chronos") }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x1A71D7CD)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = "Iniciar",
                                        tint = Color(0xFF71D7CD),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = routine.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Toque para iniciar fluxo",
                                        fontSize = 9.sp,
                                        color = Color(0xFF81928F)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0x66FFFFFF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Rituais Header and checklist
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Spa,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rituais Diários: ",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$completedRituais de $totalRituais concluídos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Ver tudo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (habits.isEmpty()) {
                    Text(
                        text = "Nenhum ritual configurado para hoje.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                    )
                } else {
                    // Render a compact list of habits with direct interactive checkboxes
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    ) {
                        habits.forEach { habit ->
                            val icon = when (habit.iconName) {
                                "WaterDrop" -> Icons.Outlined.WaterDrop
                                "MenuBook" -> Icons.Outlined.MenuBook
                                "SelfImprovement" -> Icons.Outlined.SelfImprovement
                                else -> Icons.Outlined.TaskAlt
                            }
                            val color = try { Color(android.graphics.Color.parseColor(habit.colorHex)) } catch (e: Exception) { Color(0xFF71D7CD) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x0AFFFFFF))
                                    .border(1.dp, if (habit.isCompleted) color.copy(alpha = 0.2f) else Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                                    .clickable { onToggleHabit(habit) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (habit.isCompleted) color.copy(alpha = 0.15f) else Color(0x05FFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (habit.isCompleted) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = habit.name,
                                        fontSize = 13.sp,
                                        color = if (habit.isCompleted) Color(0xFFDFE3E2) else Color(0xFFBDC9C6),
                                        fontWeight = if (habit.isCompleted) FontWeight.Medium else FontWeight.Normal,
                                        style = if (habit.isCompleted) androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.LineThrough) else androidx.compose.ui.text.TextStyle.Default
                                    )
                                }

                                // Mini Checkbox
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, if (habit.isCompleted) color else Color(0xFF3D4947), CircleShape)
                                        .background(if (habit.isCompleted) color else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (habit.isCompleted) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
fun MarketCard(items: List<com.example.data.MarketItem>, onNavigate: (String) -> Unit) {
    val totalItems = items.size
    val completedItems = items.count { it.isChecked }
    val pendingItems = items.count { !it.isChecked }
    
    val completionProgress = if (totalItems > 0) completedItems.toFloat() / totalItems.toFloat() else 0f
    
    Column(
        modifier = GlassModifier
            .fillMaxWidth()
            .bounceClick { onNavigate("market") }
            .padding(24.dp)
    ) {
        // HEADER ROW
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryTeal.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "MERCADO",
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Completion Badge
            if (totalItems > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimaryTeal.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$completedItems/$totalItems CONCLUÍDOS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
        Spacer(modifier = Modifier.height(16.dp))
        
        if (totalItems == 0) {
            // GORGEOUS EMPTY STATE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier
                        .size(36.dp)
                        .alpha(0.8f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tudo Comprado! Despensa Cheia",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sua lista está 100% em dia. Toque para ver ou adicionar.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // LIVE PROGRESS BAR
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (pendingItems > 0) "$pendingItems itens pendentes de compra" else "Todos os itens marcados!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "${(completionProgress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { completionProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = PrimaryTeal,
                    trackColor = Color(0x1AFFFFFF)
                )
            }
            
            // REDESIGNED SHOPPING CAPSULES
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.take(3).forEach { item ->
                    MarketCardItemRow(item = item)
                }
            }
            
            // QUICK VIEW PILL FOOTER
            if (totalItems > 3) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x0AFFFFFF))
                        .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ ${totalItems - 3} mais itens na sua lista",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketCardItemRow(item: com.example.data.MarketItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x05FFFFFF))
            .border(1.dp, Color(0x12FFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.ShoppingBag,
                contentDescription = null,
                tint = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else PrimaryTeal,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.name,
                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color.White,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Icon(
            imageVector = if (item.isChecked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (item.isChecked) PrimaryTeal else Color(0x33FFFFFF),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun PetsCard(routines: List<com.example.data.PetEvent>, onNavigate: (String) -> Unit) {
    Column(
        modifier = GlassModifier
            .fillMaxWidth()
            .bounceClick { onNavigate("petz") }
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PETZ",
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
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
fun HomeFinanceWidget(transactions: List<com.example.data.Transaction>, onNavigate: (String) -> Unit) {
    val currentMonthTransactions = transactions // In a real app we'd filter by current month
    val totalIncome = currentMonthTransactions.filter { it.isIncome }.sumOf { it.value }
    val totalExpense = currentMonthTransactions.filter { !it.isIncome }.sumOf { it.value }
    val balance = totalIncome - totalExpense

    Column(
        modifier = GlassModifier
            .fillMaxWidth()
            .bounceClick { onNavigate("finance") }
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FINANÇAS E FLUXO",
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
        Spacer(modifier = Modifier.height(20.dp))

        // Balance Section
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = "Saldo Geral",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "R$ ${String.format(java.util.Locale("pt", "BR"), "%,.2f", balance)}",
                fontFamily = FontFamily.Serif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = if (balance >= 0) PrimaryTeal else Color(0xFFE57373)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Receitas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "R$ ${String.format(java.util.Locale("pt", "BR"), "%,.2f", totalIncome)}",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
            
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0x1AFFFFFF)))
            
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Despesas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "R$ ${String.format(java.util.Locale("pt", "BR"), "%,.2f", totalExpense)}",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AISummaryWidget(
    netWorth: Double,
    transactions: List<com.example.data.Transaction>,
    petEvents: List<com.example.data.PetEvent>,
    pendingMarketCount: Int,
    onNavigate: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    androidx.compose.animation.AnimatedContent(
        targetState = isExpanded,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
        }
    ) { expanded ->
        if (!expanded) {
            Row(
                modifier = GlassModifier
                    .fillMaxWidth()
                    .clickable { isExpanded = true }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = "Tessera AI",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "RESUMO TESSERA AI",
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            val currentMonthTransactions = transactions
            val totalIncome = currentMonthTransactions.filter { it.isIncome }.sumOf { it.value }
            val totalExpense = currentMonthTransactions.filter { !it.isIncome }.sumOf { it.value }
            val balance = totalIncome - totalExpense

            val completedPetRoutines = petEvents.count { it.isCompleted }
            val totalPetRoutines = petEvents.size

            Column(
                modifier = GlassModifier
                    .fillMaxWidth()
                    .clickable { isExpanded = false }
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RESUMO DO SISTEMA",
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Tessera AI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryTeal.copy(alpha = 0.8f)
                        )
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = "Recolher",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Olá Kenned! Aqui está o panorama do seu ecossistema hoje:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("💰 ", fontSize = 14.sp)
                        Text(
                            text = buildAnnotatedString {
                                append("Finanças: ")
                                if (balance >= 0) {
                                    append("Saldo positivo de ")
                                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = PrimaryTeal))
                                    append("R$ ${String.format(java.util.Locale("pt", "BR"), "%,.2f", balance)}")
                                    pop()
                                } else {
                                    append("Saldo negativo de ")
                                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFE57373)))
                                    append("R$ ${String.format(java.util.Locale("pt", "BR"), "%,.2f", Math.abs(balance))}")
                                    pop()
                                }
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Text("🐾 ", fontSize = 14.sp)
                        Text(
                            text = buildAnnotatedString {
                                append("Pets: ")
                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                                append("$completedPetRoutines de $totalPetRoutines")
                                pop()
                                append(" tarefas concluídas para Marie & Churchill.")
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Text("🛒 ", fontSize = 14.sp)
                        Text(
                            text = buildAnnotatedString {
                                append("Mercado: ")
                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                                append("$pendingMarketCount")
                                pop()
                                append(" itens pendentes na sua lista de compras.")
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
            // Main Tabs Capsule Container with ambient glow
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
            ) {
                // Ambient Glow (Soft blurred shadow for elegant contrast)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .blur(10.dp)
                        .background(Color(0x7F000000), RoundedCornerShape(34.dp))
                )
                
                // Actual Capsule
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(34.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xD9222625), // 85% opacity sleek top slate
                                    Color(0xFA121414)  // 98% opacity rich deep slate
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x40FFFFFF), // Premium glossy top highlight
                                    Color(0x10FFFFFF)  // Faint bottom edge
                                )
                            ),
                            shape = RoundedCornerShape(34.dp)
                        )
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(Icons.Outlined.LightMode, "Hoje", currentRoute == "home") { onNavigate("home") }
                    NavItem(Icons.Outlined.AccountBalanceWallet, "Finanças", currentRoute == "finance") { onNavigate("finance") }
                    NavItem(Icons.Outlined.Storefront, "Mercado", currentRoute == "market") { onNavigate("market") }
                }
            }

            // Detached Action (+) Button Container with ambient glow
            Box(
                modifier = Modifier.size(68.dp)
            ) {
                // Ambient Glow (Soft blurred shadow/glow for elegant contrast)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .blur(10.dp)
                        .background(
                            if (isExpanded) PrimaryTeal.copy(alpha = 0.4f) else Color(0x7F000000),
                            CircleShape
                        )
                )

                // Actual Button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            if (isExpanded) {
                                Brush.verticalGradient(
                                    colors = listOf(PrimaryTeal, PrimaryTeal.copy(alpha = 0.8f))
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xD9222625),
                                        Color(0xFA121414)
                                    )
                                )
                            }
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = if (isExpanded) {
                                    listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.2f))
                                } else {
                                    listOf(Color(0x40FFFFFF), Color(0x10FFFFFF))
                                }
                            ),
                            shape = CircleShape
                        )
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


@Composable
fun PremiumGridTile(
    title: String,
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
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .then(GlassModifier)
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f))
                    .border(1.dp, iconColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TesseraChatSheet(
    onDismiss: () -> Unit,
    netWorth: Double,
    petEvents: List<com.example.data.PetEvent>,
    marketItems: List<com.example.data.MarketItem>,
    medications: List<com.example.data.Medication>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    
    // Model for chat messages
    data class ChatMessage(val id: String, val text: String, val isUser: Boolean)
    var messages by remember { mutableStateOf(listOf(ChatMessage("msg_0", "Olá! Como posso ajudar você hoje?", false))) }
    var isThinking by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

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
                modifier = GlassModifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .padding(16.dp)
            ) {
                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = tween(1500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    )
                )
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x1A85D6C5))
                        .border(1.dp, Color(0x3385D6C5).copy(alpha = pulseAlpha), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome, 
                        contentDescription = null, 
                        tint = PrimaryTeal.copy(alpha = pulseAlpha), 
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tessera AI", fontFamily = FontFamily.Serif, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        val isLocalActive = llmManager?.isLocalActive == true
                        val badgeColor = if (isLocalActive) Color(0xFF34C759) else Color(0xFFFF9500)
                        val badgeText = if (isLocalActive) "Gemma 2B Local" else "Simulada (Offline)"

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { showHelpDialog = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = badgeText,
                                    fontSize = 10.sp,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = if (llmManager?.isLocalActive == true) "IA executando localmente no seu dispositivo" else "Clique no badge para ativar o Gemma 2B",
                        fontSize = 12.sp,
                        color = if (llmManager?.isLocalActive == true) PrimaryTeal.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { if (llmManager?.isLocalActive != true) showHelpDialog = true }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

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
                                    val petsString = if (petEvents.isEmpty()) "Nenhum compromisso pendente hoje." else petEvents.joinToString("; ") { "${it.petName}: ${it.title} (${if(it.isCompleted) "Concluído" else "Pendente"})" }
                                    val marketString = if (marketItems.isEmpty()) "Nenhuma compra pendente." else marketItems.joinToString("; ") { "${it.name} (${it.quantity} ${it.unit})${if (it.isChecked || it.isBought) " (Comprado)" else " (Pendente)"}" }
                                    val medsString = if (medications.isEmpty()) "Nenhum medicamento agendado." else medications.joinToString("; ") { "${it.name} (${it.dosage}) às ${it.time} - ${if (it.isTaken) "Tomado" else "Pendente"}" }

                                    val hiddenContext = """
                                        [Contexto] Nome do Usuário: Kenned
                                        [Contexto] Patrimônio consolidado: R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", netWorth)}
                                        [Contexto] Compromissos dos Pets: $petsString
                                        [Contexto] Lista de Compras (Mercado): $marketString
                                        [Contexto] Medicamentos e Remédios: $medsString
                                        
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

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Configurar Gemma 2B Local",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Para usar o modelo Gemma 2B da Google 100% offline e privado no seu dispositivo, siga os passos abaixo:",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "1. Baixe o modelo Gemma 2B para MediaPipe (.bin).",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "2. Com um gerenciador de arquivos no seu celular, copie o arquivo .bin para a pasta privada do Tessera:",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Android/data/com.example/files/",
                                fontSize = 11.sp,
                                color = PrimaryTeal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "3. Renomeie o arquivo exatamente para:",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "gemma-2b-it-cpu-int4.bin",
                                fontSize = 11.sp,
                                color = PrimaryTeal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    Text(
                        text = "Nota: Devido às regras de Scoped Storage do Android 11+, aplicativos não conseguem acessar diretamente a pasta 'Downloads' padrão do seu sistema. Por isso, a cópia manual para a pasta interna é necessária.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryTeal)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF15191A),
            textContentColor = Color.White,
            titleContentColor = Color.White
        )
    }
}

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            context as androidx.fragment.app.FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    hasError = true
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onUnlocked()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    hasError = true
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Tessera")
            .setSubtitle("Use sua biometria para acessar o app")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LockScreenRipple")
    val wave1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave1"
    )
    val wave2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing, delayMillis = 1250),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave2"
    )
    
    val ambientGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AmbientGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070909)),
        contentAlignment = Alignment.Center
    ) {
        // Soft background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryTeal.copy(alpha = ambientGlowAlpha), Color.Transparent),
                        center = Offset.Unspecified,
                        radius = 800f
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Glassmorphic Biometrics Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .then(PremiumGlassModifier)
                    .padding(vertical = 40.dp, horizontal = 24.dp)
            ) {
                // Animated Fingerprint Container with Waves
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Wave 1
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = PrimaryTeal,
                            radius = 40.dp.toPx() + (40.dp.toPx() * wave1Progress),
                            alpha = (1f - wave1Progress) * 0.25f,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                    // Wave 2
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = PrimaryTeal,
                            radius = 40.dp.toPx() + (40.dp.toPx() * wave2Progress),
                            alpha = (1f - wave2Progress) * 0.25f,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    // Pulsing Central Icon Background
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.1f))
                            .border(1.dp, PrimaryTeal.copy(alpha = 0.25f), CircleShape)
                            .clickable {
                                val executor = ContextCompat.getMainExecutor(context)
                                val biometricPrompt = BiometricPrompt(
                                    context as androidx.fragment.app.FragmentActivity,
                                    executor,
                                    object : BiometricPrompt.AuthenticationCallback() {
                                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                            super.onAuthenticationSucceeded(result)
                                            onUnlocked()
                                        }
                                        override fun onAuthenticationFailed() {
                                            super.onAuthenticationFailed()
                                            hasError = true
                                        }
                                    }
                                )
                                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle("Desbloquear Tessera")
                                    .setSubtitle("Use sua biometria para acessar o app")
                                    .setNegativeButtonText("Cancelar")
                                    .build()
                                biometricPrompt.authenticate(promptInfo)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = "Biometria",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                
                Text(
                    text = "Tessera Protegido",
                    fontFamily = FontFamily.Serif,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Sua privacidade está resguardada. Use biometria para acessar seu painel.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                if (hasError) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            val executor = ContextCompat.getMainExecutor(context)
                            val biometricPrompt = BiometricPrompt(
                                context as androidx.fragment.app.FragmentActivity,
                                executor,
                                object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        super.onAuthenticationSucceeded(result)
                                        onUnlocked()
                                    }
                                }
                            )
                            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Desbloquear Tessera")
                                .setNegativeButtonText("Cancelar")
                                .build()
                            biometricPrompt.authenticate(promptInfo)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryTeal,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            "TENTAR NOVAMENTE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyBriefWidget(onClick: () -> Unit) {
    val calendar = java.util.Calendar.getInstance()
    val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
    val dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    val monthName = calendar.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale("pt", "BR"))
    
    val weekDayStr = when (dayOfWeek) {
        java.util.Calendar.SUNDAY -> "Domingo"
        java.util.Calendar.MONDAY -> "Segunda-feira"
        java.util.Calendar.TUESDAY -> "Terça-feira"
        java.util.Calendar.WEDNESDAY -> "Quarta-feira"
        java.util.Calendar.THURSDAY -> "Quinta-feira"
        java.util.Calendar.FRIDAY -> "Sexta-feira"
        else -> "Sábado"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(GlassModifier)
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
            .bounceClick(onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DAILY BRIEF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        letterSpacing = 1.5.sp
                    )
                }
                Text(
                    text = "NOW",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0x99FFFFFF),
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = "$weekDayStr, $dayOfMonth de $monthName".uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF81928F),
                letterSpacing = 1.sp
            )
            
            Text(
                text = "Seu dia em resumo",
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                lineHeight = 22.sp
            )
            
            Text(
                text = "Confira o panorama consolidado de finanças, saúde, pets, mercado e tarefas em um só lugar.",
                fontSize = 12.sp,
                color = Color(0xFF81928F),
                lineHeight = 16.sp
            )
        }
    }
}

