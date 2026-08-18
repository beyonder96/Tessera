@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example
import androidx.compose.material3.MaterialTheme

import android.util.Log
import com.example.ui.components.MetricItem
import com.example.ui.components.MetricItemWithProgress
import com.example.ui.components.MetricItemWithNeonPulse
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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.acos
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
import androidx.compose.ui.draw.drawBehind
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
import com.example.data.PetEntity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.outlined.DirectionsTransit
import com.example.data.getMetroLineColor
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
import kotlinx.coroutines.flow.first
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.OuraCircularProgress
import com.example.ui.components.LocalGlassmorphismLevel
import com.example.ui.components.isDarkTheme
import com.example.ui.components.themedCardBackground
import com.example.ui.components.themedCardBorder
import com.example.ui.components.themedOverlayBackground
import com.example.ui.components.themedOverlayBorderColors
import com.example.ui.components.themedSubtleBackground
import com.example.ui.components.themedSubtleBorder
import com.example.ui.components.themedDivider
import com.example.ui.components.themedButtonBorder
import com.example.ui.components.themedScrim
import com.example.ui.components.themedHeaderBackground
import com.example.ui.components.themedNavBarBackground
import com.example.ui.components.themedNavBarBorder
import com.example.ui.components.themedInactiveIcon
import com.example.ui.components.themedTextFieldColors
import com.example.ui.components.themedSwitchColors
import com.example.ui.components.themedCheckboxColors
import com.example.ui.components.themedImageGradientOverlay
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalConfiguration

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

import androidx.glance.appwidget.updateAll
import com.example.widget.DailyGlanceWidget
import com.example.widget.FinanceGlanceWidget
import com.example.widget.GoalsGlanceWidget
import com.example.widget.HealthGlanceWidget
import com.example.widget.MarketGlanceWidget
import com.example.widget.PetsGlanceWidget

import android.content.Intent
class MainActivity : FragmentActivity() {
    var sharedListId: String? by mutableStateOf(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
        handleAppActions(intent)
    }

    private fun handleAppActions(intent: Intent?) {
        intent?.getStringExtra("OPEN_TARGET")?.let { target ->
            AppState.pendingHealthAction = target
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporter.setup(this)
        handleDeepLink(intent)
        handleAppActions(intent)
        
        var initialSharedText: String? = null
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            initialSharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        }

        val imageLoader = coil.ImageLoader.Builder(this)
            .components {
                add(coil.decode.SvgDecoder.Factory())
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()
        setContent {
            TesseraApp()
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            val listId = uri.getQueryParameter("listId")
            if (!listId.isNullOrBlank()) {
                sharedListId = listId
            }
        }
    }
}

fun Modifier.scrollFadeInOut(): Modifier = composed {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = remember(configuration, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val fadeThreshold = remember(density) {
        with(density) { 150.dp.toPx() }
    }
    var yPosition by remember { mutableStateOf(0f) }
    var height by remember { mutableStateOf(0f) }

    this
        .onGloballyPositioned { coordinates ->
            yPosition = coordinates.positionInWindow().y
            height = coordinates.size.height.toFloat()
        }
        .graphicsLayer {
            alpha = when {
                yPosition + height < 0f || yPosition > screenHeightPx -> 0f
                yPosition < fadeThreshold -> {
                    val progress = (yPosition + height) / (fadeThreshold + height)
                    progress.coerceIn(0f, 1f)
                }
                yPosition + height > screenHeightPx - fadeThreshold -> {
                    val progress = (screenHeightPx - yPosition) / (fadeThreshold + height)
                    progress.coerceIn(0f, 1f)
                }
                else -> 1f
            }
        }
}

val GlassModifier = PremiumGlassModifier

@Composable
fun TesseraApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { TesseraRepository(database.tesseraDao()) }
    val viewModel: TesseraViewModel = viewModel(factory = TesseraViewModelFactory(repository, context))
    val petViewModel: PetViewModel = viewModel(factory = PetViewModelFactory(repository))

    val appTheme by viewModel.appTheme.collectAsState()
    val currentGlassLevel by viewModel.glassmorphismLevel.collectAsState()

    CompositionLocalProvider(
        LocalGlassmorphismLevel provides currentGlassLevel
    ) {
        MyApplicationTheme(appTheme = appTheme) {
            val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
            var isFirstTime by remember { mutableStateOf(sharedPrefs.getBoolean("first_time_user", true)) }

            if (isFirstTime) {
                OnboardingScreen(
                    viewModel = viewModel,
                    onCompleted = {
                        val editor = sharedPrefs.edit()
                        editor.putBoolean("first_time_user", false)
                        editor.apply()
                        isFirstTime = false
                    }
                )
                return@MyApplicationTheme
            }

            var isBiometricEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_enabled", false)) }
            var isUnlocked by remember { mutableStateOf(!isBiometricEnabled) }

            if (!isUnlocked) {
                LockScreen(onUnlocked = { isUnlocked = true })
                return@MyApplicationTheme
            }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Permissão concedida ou negada */ }
    )
    
    val healthProfile by viewModel.healthProfile.collectAsState(initial = null)
    val healthConnectManager = remember { com.example.health.HealthConnectManager(context) }

    LaunchedEffect(healthProfile?.isHealthConnectEnabled) {
        if (healthProfile?.isHealthConnectEnabled == true && healthConnectManager.hasAllPermissions()) {
            try {
                val end = java.time.Instant.now()
                val start = end.minus(30, java.time.temporal.ChronoUnit.DAYS)
                val hcWeights = healthConnectManager.readWeightRecords(start, end)
                val hcSleeps = healthConnectManager.readSleepRecords(start, end)
                val hcSteps = healthConnectManager.readStepsRecords(start, end)
                
                val localWeights = hcWeights.map { com.example.data.WeightRecord(weightKg = it.weight.inKilograms, timestamp = it.time.toEpochMilli(), source = "Health Connect") }
                val localSleeps = hcSleeps.map { 
                    val duration = java.time.temporal.ChronoUnit.MINUTES.between(it.startTime, it.endTime).toDouble() / 60.0
                    com.example.data.SleepRecord(startTime = it.startTime.toEpochMilli(), endTime = it.endTime.toEpochMilli(), durationHours = duration, source = "Health Connect") 
                }
                val localSteps = hcSteps.map {
                    com.example.data.StepsRecord(count = it.count, startTime = it.startTime.toEpochMilli(), endTime = it.endTime.toEpochMilli(), source = "Health Connect")
                }
                viewModel.syncHealthConnectData(localWeights, localSleeps, localSteps)

                val heightStart = end.minus(365 * 5, java.time.temporal.ChronoUnit.DAYS)
                val hcHeights = healthConnectManager.readHeightRecords(heightStart, end)
                val latestHeight = hcHeights.maxByOrNull { it.time }?.height?.inMeters?.times(100)
                if (latestHeight != null && latestHeight != healthProfile?.heightCm) {
                    viewModel.updateHealthProfile(
                        heightCm = latestHeight,
                        targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                        isHealthConnectEnabled = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initializeDataIfNeeded()
        
        // Solicita permissão de notificação no Android 13+ (API 33+) se ainda não concedida
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Re-agenda todos os alarmes de medicamentos no início do app para garantir que estão registrados no AlarmManager
        launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val meds = db.tesseraDao().getAllMedications().first()
                for (med in meds) {
                    com.example.notifications.AlarmScheduler.scheduleMedicationAlarm(context, med.name, med.dosage, med.time)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Erro ao agendar alarmes de medicamentos", e)
            }
        }
    }

    LaunchedEffect(Unit) {
        launch {
            viewModel.allTransactions.collect {
                try {
                    FinanceGlanceWidget().updateAll(context)
                    DailyGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao atualizar widgets de transações", e)
                }
            }
        }
        launch {
            viewModel.allBankAccounts.collect {
                try {
                    FinanceGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao atualizar widgets de contas", e)
                }
            }
        }
        launch {
            viewModel.pendingMarketItems.collect {
                try {
                    MarketGlanceWidget().updateAll(context)
                    DailyGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao atualizar widgets de mercado", e)
                }
            }
        }
        launch {
            viewModel.allHabits.collect {
                try {
                    GoalsGlanceWidget().updateAll(context)
                    DailyGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao atualizar widgets de hábitos", e)
                }
            }
        }
        launch {
            viewModel.allRoutines.collect {
                try {
                    GoalsGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao atualizar widgets de rotinas", e)
                }
            }
        }
        launch {
            viewModel.allPetEvents.collect {
                try {
                    PetsGlanceWidget().updateAll(context)
                    DailyGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao atualizar widgets de pets", e)
                }
            }
        }
        launch {
            viewModel.allMedications.collect {
                try {
                    HealthGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao atualizar widgets de medicação", e)
                }
            }
        }
        launch {
            viewModel.allStepsRecords.collect {
                try {
                    HealthGlanceWidget().updateAll(context)
                    DailyGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao atualizar widgets de passos", e)
                }
            }
        }
        launch {
            viewModel.allWeightRecords.collect {
                try {
                    HealthGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao atualizar widgets de peso", e)
                }
            }
        }
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
    LaunchedEffect(activity) {
        val intent = activity?.intent
        if (intent?.action == android.content.Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
            if (text != null) {
                viewModel.handleSharedText(text)
                navController.navigate("wishes") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            intent.removeExtra(android.content.Intent.EXTRA_TEXT)
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val navigateAction: (String) -> Unit = { route ->
        val targetRoute = when (route) {
            "rotinas" -> {
                viewModel.selectedGoalsTab = 1
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

    var homeScrollOffset by remember { mutableStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val contentBlur by animateDpAsState(if (isFabExpanded) 32.dp else 0.dp, tween(300))
                    
                    LaunchedEffect(AppState.pendingHealthAction) {
                        val action = AppState.pendingHealthAction
                        if (action != null) {
                            if (action == "STEPS" || action == "SLEEP" || action == "MEDICATION") {
                                navController.navigate("health")
                            } else if (action == "METRO") {
                                navController.navigate("transport")
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize().blur(contentBlur)) {
                    NavHost(navController = navController, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                    composable("home") {
                        DailyScreen(
                            viewModel = viewModel,
                            onNavigate = navigateAction,
                            petViewModel = petViewModel,
                            onBack = {  }
                        )
                    }
                composable("settings") {
                    SettingsScreen(viewModel = viewModel, onBack = { 
                        navController.popBackStack()
                    })
                }
                composable("finance") {
                    FinanceScreen(
                        onHomeClick = { 
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }, 
                        viewModel = viewModel,
                        onNavigateToInvoiceHub = { cardName ->
                            navController.navigate("invoice_hub/$cardName")
                        },
                        onNavigateToBenefitHub = { cardName ->
                            navController.navigate("benefit_hub/$cardName")
                        }
                    )
                }
                composable("invoice_hub/{cardName}") { backStackEntry ->
                    val cardName = backStackEntry.arguments?.getString("cardName") ?: ""
                    InvoiceHubScreen(
                        cardName = cardName,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("benefit_hub/{cardName}") { backStackEntry ->
                    val cardName = backStackEntry.arguments?.getString("cardName") ?: ""
                    BenefitHubScreen(
                        cardName = cardName,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
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
                    ZenithScreen(
                        onHomeClick = { 
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }, 
                        viewModel = viewModel,
                        initialPage = viewModel.selectedGoalsTab
                    )
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
                        petViewModel = petViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigate = navigateAction
                    )
                }
                composable("wishes") {
                    WishesScreen(
                        onHomeClick = { 
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        viewModel = viewModel
                    )
                }
                composable("transport") {
                    TransportScreen(
                        viewModel = viewModel,
                        onNavigateBack = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                BottomNavBar(
                    viewModel = viewModel,
                    isExpanded = isFabExpanded,
                    onExpandedChange = { isFabExpanded = it },
                    onHoveredItemChange = { fabHoveredItem = it },
                    currentRoute = currentRoute,
                    onNavigate = navigateAction,
                    onCameraClick = { }
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(GlassModifier)
                                .padding(vertical = 24.dp, horizontal = 16.dp)
                        ) {
                            val titleAlpha by animateFloatAsState(if (isFabExpanded) 1f else 0f, tween(250))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Módulos",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = titleAlpha),
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "8 Ativos",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            com.example.ui.components.BentoBoxDashboard(
                                viewModel = viewModel,
                                isExpanded = isFabExpanded,
                                onNavigate = { route -> 
                                    navigateAction(route)
                                    isFabExpanded = false
                                }
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
        Log.e("MainActivity", "Erro ao calcular tamanho do banco", e)
        "Indisponível"
    }
}

@Composable
fun DailyScreen(viewModel: TesseraViewModel, onNavigate: (String) -> Unit, onScrollChange: (Int) -> Unit) {
    val context = LocalContext.current
    val mainViewModel = viewModel
    val petViewModel: PetViewModel = viewModel(factory = com.example.viewmodel.PetViewModelFactory(com.example.data.TesseraRepository(com.example.data.AppDatabase.getDatabase(context).tesseraDao())))
    val petEvents by mainViewModel.allPetEvents.collectAsState(initial = emptyList())
    val stepsRecords by mainViewModel.allStepsRecords.collectAsState(initial = emptyList())
    val marketItems by mainViewModel.pendingMarketItems.collectAsState(initial = emptyList())
    val medications by mainViewModel.allMedications.collectAsState(initial = emptyList())
    val habits by mainViewModel.allHabits.collectAsState(initial = emptyList())
    val sleepRecords by mainViewModel.allSleepRecords.collectAsState(initial = emptyList())
    val weightRecords by mainViewModel.allWeightRecords.collectAsState(initial = emptyList())
    val latestWeight = remember(weightRecords) { weightRecords.lastOrNull()?.weightKg ?: 70.0 }
    val latestSleep = remember(sleepRecords) { sleepRecords.maxByOrNull { it.endTime }?.durationHours ?: 0.0 }
    
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    var isBiometricEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_enabled", false)) }
    
    val transactions by mainViewModel.allTransactions.collectAsState()
    val bankAccounts by mainViewModel.allBankAccounts.collectAsState()
    val benefitCards by mainViewModel.allBenefitCards.collectAsState(initial = emptyList())

    val currentMonthStart = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val currentMonthEnd = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
    val currentMonthTransactions = remember(transactions, currentMonthStart, currentMonthEnd) {
        transactions.filter { it.timestamp in currentMonthStart..currentMonthEnd }
    }

    val realIncome = remember(currentMonthTransactions, benefitCards) { 
        currentMonthTransactions.filter { tx -> 
            tx.isIncome && benefitCards.none { card -> card.name == tx.accountOrCardName } 
        }.sumOf { it.value } 
    }
    val realExpense = remember(currentMonthTransactions, benefitCards) { 
        currentMonthTransactions.filter { tx -> 
            !tx.isIncome && benefitCards.none { card -> card.name == tx.accountOrCardName } 
        }.sumOf { it.value } 
    }
    val realBalance = realIncome - realExpense
    val realPatrimony = remember(bankAccounts) { bankAccounts.sumOf { it.balance } }

    val netWorth = realBalance
    val totalIncome = realIncome
    val totalExpense = realExpense

    // Metro & Trem Alert States
    var showMetroPopup by remember { mutableStateOf(false) }
    val metroStatus by mainViewModel.metroStatus.collectAsState()
    val isLoadingMetroStatus by mainViewModel.isLoadingMetroStatus.collectAsState()
    val metroError by mainViewModel.metroError.collectAsState()

    // LaunchedEffect periódico para monitorar horários programados
    LaunchedEffect(Unit) {
        while (true) {
            val alertTimes = sharedPrefs.getStringSet("metro_alert_times", emptySet()) ?: emptySet()
            val monitoredLines = sharedPrefs.getStringSet("metro_monitored_lines", emptySet()) ?: emptySet()
            
            if (alertTimes.isNotEmpty() && monitoredLines.isNotEmpty()) {
                val calendar = java.util.Calendar.getInstance()
                val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(java.util.Calendar.MINUTE)
                val currentTimeStr = String.format(java.util.Locale.US, "%02d:%02d", currentHour, currentMinute)
                
                if (alertTimes.contains(currentTimeStr)) {
                    val todayDateStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", 
                        calendar.get(java.util.Calendar.YEAR),
                        calendar.get(java.util.Calendar.MONTH) + 1,
                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                    val triggerKey = "${todayDateStr} ${currentTimeStr}"
                    val lastTrigger = sharedPrefs.getString("metro_last_trigger", "")
                    
                    if (lastTrigger != triggerKey) {
                        sharedPrefs.edit().putString("metro_last_trigger", triggerKey).apply()
                        mainViewModel.fetchMetroStatus()
                        showMetroPopup = true
                    }
                }
            }
            kotlinx.coroutines.delay(30000L) // Verifica a cada 30 segundos
        }
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.value) {
        onScrollChange(scrollState.value)
    }

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
            .then(PremiumGlassModifier)
    ) {

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
            initialValue = 0.05f,
            targetValue = 0.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = EaseInOutSine),
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

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .graphicsLayer {
                    alpha = (1f - (scrollState.value / 300f)).coerceIn(0f, 1f)
                }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.1f),
                    radius = size.width * glowScale * 0.9f
                ),
                center = Offset(size.width * 0.8f, size.height * 0.1f),
                radius = size.width * glowScale * 0.9f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            // Espaço equivalente ao header flutuante
            Spacer(modifier = Modifier.height(110.dp))


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
                    .scrollFadeInOut()
            ) {
                TopMetricsRow(
                    patrimony = realPatrimony,
                    netWorth = netWorth,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    todaySteps = todaySteps,
                    latestWeight = latestWeight,
                    latestSleep = latestSleep,
                    onNavigate = onNavigate
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            MainContent(netWorth, petEvents, mainViewModel, petViewModel, onNavigate)
            Spacer(modifier = Modifier.height(140.dp))
        }

        // Floating Header over the content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    if (scrollState.value > 50) (if (isDarkTheme()) Color(0xCC000000) else Color(0xCCFFFFFF)) else Color.Transparent
                )
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TopHeader(
                onOpenSettings = { onNavigate("settings") },
                onOpenMetro = {
                    onNavigate("transport")
                }
            )
        }
    }



    if (showMetroPopup) {
        val monitoredLines = remember { sharedPrefs.getStringSet("metro_monitored_lines", emptySet()) ?: emptySet() }

        Dialog(onDismissRequest = { showMetroPopup = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(themedOverlayBackground())
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = themedOverlayBorderColors()
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4FC3F7).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF4FC3F7).copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsTransit,
                                contentDescription = null,
                                tint = Color(0xFF4FC3F7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Status Metroferroviário",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "Situação das linhas em tempo real",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = { showMetroPopup = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))

                    if (isLoadingMetroStatus) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF4FC3F7))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Buscando informações da ARTESP...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    } else if (metroError != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Outlined.Warning, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(36.dp))
                            Text(
                                text = metroError ?: "Não foi possível carregar as informações.",
                                color = Color(0xFFE57373),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { mainViewModel.fetchMetroStatus() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Tentar Novamente", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (monitoredLines.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nenhuma linha selecionada para monitoramento.",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Configure as linhas desejadas nas Configurações do app.",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val allEmpresas = metroStatus
                        val matchedLines = mutableListOf<Pair<String, com.example.data.MetroLinhaStatus>>()
                        
                        allEmpresas.forEach { empresa ->
                            empresa.linhas?.forEach { linha ->
                                val lineKey = "${empresa.id}_${linha.codigo}"
                                if (monitoredLines.contains(lineKey)) {
                                    matchedLines.add(empresa.nome to linha)
                                }
                            }
                        }

                        if (matchedLines.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Nenhuma linha selecionada está ativa na API.",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                matchedLines.forEach { (empresaNome, linha) ->
                                    val lineColor = getMetroLineColor(linha.nome, linha.codigo)
                                    val statusDetail = linha.status
                                    val situacao = statusDetail?.situacao ?: "Sem informações"
                                    val isNormal = statusDetail?.operacaoNormal ?: true
                                    val atualizadoHa = statusDetail?.atualizadoHa ?: ""

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(themedSubtleBackground(), RoundedCornerShape(16.dp))
                                            .border(1.dp, themedSubtleBorder(), RoundedCornerShape(16.dp))
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp, 40.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(lineColor)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Text(
                                                text = linha.nome,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = empresaNome,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isNormal) Color(0xFF34C759).copy(alpha = 0.15f)
                                                        else Color(0xFFFF9500).copy(alpha = 0.15f)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = situacao,
                                                    color = if (isNormal) Color(0xFF30D158) else Color(0xFFFF9F0A),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            if (atualizadoHa.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "há $atualizadoHa",
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(themedDivider()))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showMetroPopup = false },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, themedButtonBorder()),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Fechar", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                        }
                        
                        if (!isLoadingMetroStatus && monitoredLines.isNotEmpty()) {
                            Button(
                                onClick = { mainViewModel.fetchMetroStatus() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1.5f).height(48.dp).bounceClick { mainViewModel.fetchMetroStatus() }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Atualizar", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopHeader(onOpenSettings: () -> Unit, onOpenMetro: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    var profileUri by remember { mutableStateOf<Uri?>(null) }
    val userName = remember { sharedPrefs.getString("user_name", "Kenned") ?: "Kenned" }

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
                Log.e("MainActivity", "Erro ao salvar foto de perfil", e)
                profileUri = uri
                sharedPrefs.edit().putString("user_profile_uri", uri.toString()).apply()
            }
        }
    }

    // Thermal/Lava Lamp animation colors
    val lavaBrush = com.example.ui.components.rememberLavaBrush()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(themedHeaderBackground())
                .border(1.dp, themedSubtleBorder(), RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
        // Left side: Settings icon + TESSERA text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { onOpenSettings() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configurações",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "TESSERA",
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(brush = lavaBrush),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }

        // Right buttons (Photo upload and Metro)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    launcher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.size(36.dp)
            ) {
                if (profileUri != null) {
                    Box(contentAlignment = Alignment.Center) {
                        // Aura layer
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .blur(12.dp)
                                .background(lavaBrush, CircleShape)
                        )
                        // Profile image
                        AsyncImage(
                            model = profileUri,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Upload Foto de Perfil",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = onOpenMetro,
                modifier = Modifier.size(36.dp)
            ) {
                Box(modifier = Modifier.size(36.dp).drawBehind { drawCircle(brush = lavaBrush, radius = size.width/2, alpha = 0.4f) }, contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsTransit,
                        contentDescription = "Status do Metrô",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
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
    todaySteps: Long,
    latestWeight: Double,
    latestSleep: Double,
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
    val aptProgress = sharedPrefs.getFloat("apartment_progress", 0.75f)

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
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Widget 1 (Finanças) with smooth crossfade rotation!
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
                        0 -> String.format(java.util.Locale("pt", "BR"), "%.1fh", latestSleep)
                        1 -> String.format(java.util.Locale("pt", "BR"), "%.1f", latestWeight)
                        else -> todaySteps.toString()
                    }
                    val labelIdx = when (idx) {
                        0 -> "SONO"
                        1 -> "PESO"
                        else -> "PASSOS"
                    }
                    val progressIdx = when (idx) {
                        0 -> (latestSleep / 10.0).toFloat().coerceIn(0f, 1f)
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

            // Widget 3 (Daily Brief - Pulsing Neon)
            Box(modifier = Modifier.width(76.dp)) {
                MetricItemWithNeonPulse(
                    icon = Icons.Outlined.AutoAwesome,
                    value = "NOW",
                    label = "DAILY",
                    glowColor = Color(0xFF71D7CD),
                    onClick = { onNavigate("daily") }
                )
            }

            // Widget 4 (Apartamento)
            MetricItemWithProgress(Icons.Outlined.Construction, "${(aptProgress * 100).toInt()}%", "OBRA", SecondaryGold, aptProgress, onClick = { onNavigate("apartment") })
        }
    }
}

@Composable
fun HeroMetric(metric: com.example.viewmodel.TesseraViewModel.DynamicHeroMetric?, onNavigate: (String) -> Unit) {
    if (metric == null) return

    val progressValue = if (metric.target > 0f) (metric.value / metric.target).coerceIn(0f, 1f) else 0f
    val progressColor = try {
        Color(android.graphics.Color.parseColor(metric.colorHex))
    } catch (e: Exception) {
        PrimaryTeal
    }

    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(metric) { animationStarted = true }
    
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationStarted) progressValue else 0f,
        animationSpec = tween(durationMillis = 1500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )

    var isHeroVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200L)
        isHeroVisible = true
    }

    val iconVector = when (metric.iconName) {
        "DirectionsWalk" -> Icons.Outlined.DirectionsWalk
        "CheckCircle" -> Icons.Outlined.CheckCircle
        "Timer" -> Icons.Outlined.Timer
        "AttachMoney" -> Icons.Outlined.AttachMoney
        "LocalMall" -> Icons.Outlined.LocalMall
        "WaterDrop" -> Icons.Outlined.WaterDrop
        "Pets" -> Icons.Outlined.Pets
        "Medication" -> Icons.Outlined.Medication
        "Warning" -> Icons.Outlined.Warning
        else -> Icons.Outlined.Info
    }

    AnimatedVisibility(
        visible = isHeroVisible,
        enter = fadeIn(tween(800)) + scaleIn(initialScale = 0.95f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable {
                    val target = when (metric.name) {
                        "RITUAIS DIÁRIOS" -> "goals"
                        "PASSOS COMPLETADOS" -> "health"
                        "COMPRAS PENDENTES" -> "market"
                        "BALANÇO FINANCEIRO" -> "finance"
                        else -> "goals"
                    }
                    onNavigate(target)
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                val topOfArcY = 200.dp - (maxWidth / 2)

                val sysOnBackground = MaterialTheme.colorScheme.onBackground
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    val startAngle = 160f
                    val sweepAngle = 220f
                    val arcRectSize = Size(size.width * 0.85f, size.width * 0.85f)
                    val topLeft = Offset(size.width * 0.075f, size.height - (size.width * 0.85f) / 1.7f)

                    // Draw thin elegant background track
                    drawArc(
                        color = sysOnBackground.copy(alpha = 0.15f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = arcRectSize,
                        topLeft = topLeft
                    )

                    // Draw thick subtle glow underlay for active progress
                    drawArc(
                        color = progressColor.copy(alpha = 0.12f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth * 3f, cap = StrokeCap.Round),
                        size = arcRectSize,
                        topLeft = topLeft
                    )

                    // Draw thin bright progress track
                    drawArc(
                        color = progressColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth * 1.5f, cap = StrokeCap.Round),
                        size = arcRectSize,
                        topLeft = topLeft
                    )

                    // Draw little tick dots along the arc (5 dots like Oura Ring app)
                    val radius = (size.width * 0.85f) / 2
                    val angles = listOf(160f, 215f, 270f, 325f, 20f)
                    for (angle in angles) {
                        val angleRad = Math.toRadians(angle.toDouble())
                        val x = size.width / 2 + radius * Math.cos(angleRad).toFloat()
                        val y = size.height - (size.width * 0.85f) / 1.7f + radius + radius * Math.sin(angleRad).toFloat()
                        drawCircle(color = sysOnBackground.copy(alpha = 0.4f), radius = 2.5.dp.toPx(), center = Offset(x, y))
                    }
                }

                // Values at ends of the arc (0 and Target)
                val targetText = if (metric.target >= 1000f) "${(metric.target/1000).toInt()}k" else "${metric.target.toInt()}"
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "0",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 44.dp)
                            .graphicsLayer { rotationZ = 45f }
                    )
                    Text(
                        text = targetText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp, bottom = 44.dp)
                            .graphicsLayer { rotationZ = -45f }
                    )
                }

                // Center circular icon button and text underneath (Oura Style)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x1F000000))
                            .border(1.dp, themedButtonBorder(), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = progressColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = metric.label.uppercase(),
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (metric.value % 1f == 0f) "${metric.value.toInt()}" else "${metric.value}",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
data class ModuleConfig(val id: String, val name: String, var isVisible: Boolean, var order: Int)

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun InsightCardComponent(card: com.example.viewmodel.TesseraViewModel.InsightCard, onAction: () -> Unit, onClick: () -> Unit) {
    var isDismissed by remember { mutableStateOf(false) }
    if (isDismissed) return

    val iconVector = when (card.iconName) {
        "DirectionsWalk" -> Icons.Outlined.DirectionsWalk
        "CheckCircle" -> Icons.Outlined.CheckCircle
        "Timer" -> Icons.Outlined.Timer
        "AttachMoney" -> Icons.Outlined.AttachMoney
        "LocalMall" -> Icons.Outlined.LocalMall
        "WaterDrop" -> Icons.Outlined.WaterDrop
        "Pets" -> Icons.Outlined.Pets
        "Medication" -> Icons.Outlined.Medication
        "Warning" -> Icons.Outlined.Warning
        else -> Icons.Outlined.Info
    }

    val accentColor = when (card.category) {
        "health" -> Color(0xFF34C759)
        "finance" -> Color(0xFF007AFF)
        "market" -> Color(0xFFFF3B30)
        "pets" -> Color(0xFFFF9500)
        else -> Color(0xFF71D7CD)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(PremiumGlassModifier)
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = card.title.uppercase(),
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.description,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }
            }
            IconButton(
                onClick = { 
                    isDismissed = true 
                    onAction()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun MainContent(
    netWorth: Double,
    petEvents: List<com.example.data.PetEvent>,
    mainViewModel: com.example.viewmodel.TesseraViewModel,
    petViewModel: com.example.viewmodel.PetViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    
    val defaultModules = listOf(
        ModuleConfig("finance", "Finanças e Fluxo", true, 0),
        ModuleConfig("health", "Saúde e Energia", true, 1),
        ModuleConfig("goals", "Rotinas", true, 2),
        ModuleConfig("pets", "Meus Petz", true, 3),
        ModuleConfig("market", "Mercado e Desejos", true, 4)
    )

    var modules by remember {
        val saved = sharedPrefs.getString("home_modules_config", null)
        if (saved != null) {
            try {
                val jsonArray = org.json.JSONArray(saved)
                val parsed = mutableListOf<ModuleConfig>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    parsed.add(ModuleConfig(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        isVisible = obj.getBoolean("isVisible"),
                        order = obj.getInt("order")
                    ))
                }
                // Ensure all default modules exist
                val merged = defaultModules.map { defaultMod ->
                    parsed.find { it.id == defaultMod.id } ?: defaultMod
                }.sortedBy { it.order }
                mutableStateOf(merged)
            } catch (e: Exception) {
                mutableStateOf(defaultModules)
            }
        } else {
            mutableStateOf(defaultModules)
        }
    }

    val saveModules = { newModules: List<ModuleConfig> ->
        modules = newModules
        val jsonArray = org.json.JSONArray()
        newModules.forEach { mod ->
            val obj = org.json.JSONObject()
            obj.put("id", mod.id)
            obj.put("name", mod.name)
            obj.put("isVisible", mod.isVisible)
            obj.put("order", mod.order)
            jsonArray.put(obj)
        }
        sharedPrefs.edit().putString("home_modules_config", jsonArray.toString()).apply()
    }

    var showEditSheet by remember { mutableStateOf(false) }

    val insights by mainViewModel.aiInsights.collectAsState(initial = emptyList())
    val transactions by mainViewModel.allTransactions.collectAsState(initial = emptyList())
    val bankAccounts by mainViewModel.allBankAccounts.collectAsState(initial = emptyList())
    val benefitCards by mainViewModel.allBenefitCards.collectAsState(initial = emptyList())
    val marketItems by mainViewModel.pendingMarketItems.collectAsState(initial = emptyList())
    val medications by mainViewModel.allMedications.collectAsState(initial = emptyList())
    val habits by mainViewModel.allHabits.collectAsState(initial = emptyList())
    val purchaseGoals by mainViewModel.allPurchaseGoals.collectAsState(initial = emptyList())
    val routines by mainViewModel.allRoutines.collectAsState(initial = emptyList())
    val pets by petViewModel.allPets.collectAsState(initial = emptyList())
    val stepsRecords by mainViewModel.allStepsRecords.collectAsState(initial = emptyList())
    val weightRecords by mainViewModel.allWeightRecords.collectAsState(initial = emptyList())
    val healthProfile by mainViewModel.healthProfile.collectAsState(initial = null)


    val todayStart = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
    val todaySteps = remember(stepsRecords) {
        stepsRecords.filter { it.startTime >= todayStart && it.endTime <= todayEnd }.sumOf { it.count }
    }
    val latestWeight = remember(weightRecords) {
        weightRecords.lastOrNull()?.weightKg ?: 0.0
    }
    val bmi = remember(latestWeight, healthProfile) {
        val heightCm = healthProfile?.heightCm ?: 0.0
        if (heightCm > 0.0 && latestWeight > 0.0) {
            val heightM = heightCm / 100.0
            latestWeight / (heightM * heightM)
        } else {
            0.0
        }
    }

    val visibleModules = modules.filter { it.isVisible }
    val pagerState = rememberPagerState(pageCount = { visibleModules.size })

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 16.dp
        ) { page ->
            val mod = visibleModules[page]
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val pageOffset = (
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).absoluteValue
                        val scale = lerp(
                            start = 0.85f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                        val alpha = lerp(
                            start = 0.5f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                when (mod.id) {
                    "finance" -> HomeFinanceWidget(transactions, bankAccounts, benefitCards, onNavigate)
                    "health" -> HealthWidget(medications, { mainViewModel.toggleMedicationTaken(it) }, latestWeight, todaySteps, bmi)
                    "goals" -> GoalsWidget(habits, purchaseGoals, routines, { mainViewModel.toggleHabitCompleted(it) }, onNavigate)
                    "pets" -> PetsCard(pets, onNavigate)
                    "market" -> MarketCard(marketItems, onNavigate)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(visibleModules.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(color)
                        .size(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = { showEditSheet = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Editar Widgets", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }

    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            containerColor = MaterialTheme.colorScheme.background,
            scrimColor = Color.Black.copy(alpha = 0.7f)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = "Editar Widgets da Home",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                modules.forEachIndexed { index, mod ->
                    ModuleToggleWithOrder(
                        name = mod.name,
                        isVisible = mod.isVisible,
                        isFirst = index == 0,
                        isLast = index == modules.lastIndex,
                        onToggle = { visible ->
                            val copy = modules.toMutableList()
                            copy[index] = copy[index].copy(isVisible = visible)
                            saveModules(copy)
                        },
                        onMoveUp = {
                            val copy = modules.toMutableList()
                            val temp = copy[index]
                            copy[index] = copy[index - 1]
                            copy[index - 1] = temp
                            copy.forEachIndexed { i, m -> copy[i] = m.copy(order = i) }
                            saveModules(copy)
                        },
                        onMoveDown = {
                            val copy = modules.toMutableList()
                            val temp = copy[index]
                            copy[index] = copy[index + 1]
                            copy[index + 1] = temp
                            copy.forEachIndexed { i, m -> copy[i] = m.copy(order = i) }
                            saveModules(copy)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
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
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir", tint = if (isFirst) Color.Gray else MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descer", tint = if (isLast) Color.Gray else MaterialTheme.colorScheme.onBackground)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
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
        Triple(Icons.Outlined.FlashOn, "Energizado", PrimaryTeal),
        Triple(Icons.Outlined.Spa, "Calmo", SecondaryGold),
        Triple(Icons.Outlined.Bedtime, "Cansado", TertiaryPurple),
        Triple(Icons.Outlined.Adjust, "Focado", Color(0xFF64B5F6)), // Light Blue
        Triple(Icons.Outlined.Speed, "Estressado", Color(0xFFE57373)) // Light Red
    )

    if (showFeelingDialog) {
        AlertDialog(
            onDismissRequest = { showFeelingDialog = false },
            title = { Text("Qual é a sua vibe hoje?", fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    feelings.forEach { (icon, label, color) ->
                        TextButton(
                            onClick = {
                                userFeeling = label
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
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = color,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(label, fontSize = 16.sp, color = color, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    val matchingFeeling = feelings.firstOrNull { userFeeling.contains(it.second) }
                    val feelingColor = matchingFeeling?.third ?: PrimaryTeal
                    val feelingIcon = matchingFeeling?.first ?: Icons.Outlined.FlashOn
                    val feelingLabel = matchingFeeling?.second ?: userFeeling
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showFeelingDialog = true }
                    ) {
                        Icon(
                            imageVector = feelingIcon,
                            contentDescription = feelingLabel,
                            tint = feelingColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = feelingLabel,
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
                color = MaterialTheme.colorScheme.onBackground
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
                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground,
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
                    color = MaterialTheme.colorScheme.onBackground
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
                    color = MaterialTheme.colorScheme.onBackground
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
                                        color = MaterialTheme.colorScheme.onBackground
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
                            color = MaterialTheme.colorScheme.onBackground
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
                    color = MaterialTheme.colorScheme.onBackground
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
                        color = MaterialTheme.colorScheme.onBackground
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
                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground,
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
fun PetsCard(pets: List<com.example.data.PetEntity>, onNavigate: (String) -> Unit) {
    val now = System.currentTimeMillis()
    val dateFormat = remember { java.text.SimpleDateFormat("dd/MMM", java.util.Locale("pt", "BR")) }
    
    data class HealthEvent(
        val petName: String,
        val title: String,
        val interval: String,
        val dueDate: Long,
        val isVaccine: Boolean,
        val isExpired: Boolean
    )
    
    val allEvents = remember(pets) {
        val events = mutableListOf<HealthEvent>()
        pets.forEach { pet ->
            val v4Due = pet.lastV4VaccineDate?.let { it + 365L * 24 * 3600 * 1000 } ?: 0L
            val v4Expired = pet.lastV4VaccineDate == null || v4Due < now
            events.add(HealthEvent(pet.name, "Vacina V4", "Anual", v4Due, true, v4Expired))
            
            val raivaDue = pet.lastRaivaVaccineDate?.let { it + 365L * 24 * 3600 * 1000 } ?: 0L
            val raivaExpired = pet.lastRaivaVaccineDate == null || raivaDue < now
            events.add(HealthEvent(pet.name, "Vacina Antirrábica", "Anual", raivaDue, true, raivaExpired))
            
            val antipulgasDue = pet.lastAntipulgasDate?.let { it + 90L * 24 * 3600 * 1000 } ?: 0L
            val antipulgasExpired = pet.lastAntipulgasDate == null || antipulgasDue < now
            events.add(HealthEvent(pet.name, "Antipulgas", "A cada 3 meses", antipulgasDue, false, antipulgasExpired))
            
            val vermifugoDue = pet.lastVermifugoDate?.let { it + 180L * 24 * 3600 * 1000 } ?: 0L
            val vermifugoExpired = pet.lastVermifugoDate == null || vermifugoDue < now
            events.add(HealthEvent(pet.name, "Vermífugo", "A cada 6 meses", vermifugoDue, false, vermifugoExpired))
            
            val consultaDue = pet.lastConsultaDate?.let { it + 365L * 24 * 3600 * 1000 } ?: 0L
            val consultaExpired = pet.lastConsultaDate == null || consultaDue < now
            events.add(HealthEvent(pet.name, "Consulta de Rotina", "Anual", consultaDue, false, consultaExpired))
        }
        events
    }

    val vaccines = remember(allEvents) {
        allEvents.filter { it.isVaccine }.sortedWith(compareBy({ !it.isExpired }, { it.dueDate }))
    }
    
    val routines = remember(allEvents) {
        allEvents.filter { !it.isVaccine }.sortedWith(compareBy({ !it.isExpired }, { it.dueDate }))
    }

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
        
        if (vaccines.isEmpty()) {
            Text(
                text = "Nenhuma vacina pendente",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        } else {
            vaccines.take(2).forEachIndexed { idx, ev ->
                if (idx > 0) Spacer(modifier = Modifier.height(12.dp))
                val dateStr = if (ev.dueDate == 0L) "Pendente" else dateFormat.format(java.util.Date(ev.dueDate))
                val statusText = if (ev.isExpired) "Atrasada" else "Em dia"
                val statusColor = if (ev.isExpired) Color(0xFFEF4444) else PrimaryTeal
                
                PetMedicalEvent(
                    petName = ev.petName,
                    title = ev.title,
                    detail = ev.interval,
                    date = dateStr,
                    status = statusText,
                    statusColor = statusColor
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0x0DFFFFFF)))
        Spacer(modifier = Modifier.height(16.dp))

        // Rotinas/Consultas Section
        Text(
            text = "SAÚDE E ROTINAS",
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
            color = TertiaryPurple,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (routines.isEmpty()) {
            Text(
                text = "Nenhum compromisso pendente",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        } else {
            routines.take(2).forEachIndexed { idx, ev ->
                if (idx > 0) Spacer(modifier = Modifier.height(12.dp))
                val dateStr = if (ev.dueDate == 0L) "Pendente" else dateFormat.format(java.util.Date(ev.dueDate))
                val statusText = if (ev.isExpired) "Atrasado" else "Em dia"
                val statusColor = if (ev.isExpired) Color(0xFFEF4444) else TertiaryPurple
                
                PetMedicalEvent(
                    petName = ev.petName,
                    title = ev.title,
                    detail = ev.interval,
                    date = dateStr,
                    status = statusText,
                    statusColor = statusColor
                )
            }
        }
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
                    color = MaterialTheme.colorScheme.onBackground,
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
                color = MaterialTheme.colorScheme.onBackground,
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
fun HomeFinanceWidget(
    transactions: List<com.example.data.Transaction>,
    bankAccounts: List<com.example.data.BankAccount>,
    benefitCards: List<com.example.data.BenefitCard>,
    onNavigate: (String) -> Unit
) {
    val currentMonthStart = remember(transactions) {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val currentMonthEnd = remember(transactions) {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
    val currentMonthTransactions = remember(transactions, currentMonthStart, currentMonthEnd) {
        transactions.filter { it.timestamp in currentMonthStart..currentMonthEnd }
    }
    val totalIncome = currentMonthTransactions.filter { tx ->
        tx.isIncome && benefitCards.none { card -> card.name == tx.accountOrCardName }
    }.sumOf { it.value }
    val totalExpense = currentMonthTransactions.filter { tx ->
        !tx.isIncome && benefitCards.none { card -> card.name == tx.accountOrCardName }
    }.sumOf { it.value }
    val balance = bankAccounts.sumOf { it.balance }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    var hideValues by remember { mutableStateOf(sharedPrefs.getBoolean("hide_finance_values", true)) }

    val toggleHide = {
        val next = !hideValues
        hideValues = next
        sharedPrefs.edit().putBoolean("hide_finance_values", next).apply()
    }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { toggleHide() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (hideValues) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "Ocultar Valores",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
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
                text = if (hideValues) "R$ *****" else "R$ ${String.format(java.util.Locale("pt", "BR"), "%,.2f", balance)}",
                fontFamily = FontFamily.Serif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
                color = if (hideValues) MaterialTheme.colorScheme.onBackground else if (balance >= 0) PrimaryTeal else Color(0xFFE57373)
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
                    text = if (hideValues) "R$ ***" else "R$ ${String.format(java.util.Locale("pt", "BR"), "%,.2f", totalIncome)}",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
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
                    text = if (hideValues) "R$ ***" else "R$ ${String.format(java.util.Locale("pt", "BR"), "%,.2f", totalExpense)}",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
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
    benefitCards: List<com.example.data.BenefitCard> = emptyList(),
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
            val currentMonthStart = remember(transactions) {
                java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_MONTH, 1)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            val currentMonthEnd = remember(transactions) {
                java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                    set(java.util.Calendar.MINUTE, 59)
                    set(java.util.Calendar.SECOND, 59)
                    set(java.util.Calendar.MILLISECOND, 999)
                }.timeInMillis
            }
            val currentMonthTransactions = remember(transactions, currentMonthStart, currentMonthEnd) {
                transactions.filter { it.timestamp in currentMonthStart..currentMonthEnd }
            }
            val totalIncome = currentMonthTransactions.filter { tx ->
                tx.isIncome && benefitCards.none { card -> card.name == tx.accountOrCardName }
            }.sumOf { it.value }
            val totalExpense = currentMonthTransactions.filter { tx ->
                !tx.isIncome && benefitCards.none { card -> card.name == tx.accountOrCardName }
            }.sumOf { it.value }
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
                    color = MaterialTheme.colorScheme.onBackground
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
                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground))
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
                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground))
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
fun CircularNavButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    activeIconColor: Color = MaterialTheme.colorScheme.onPrimary,
    onClick: () -> Unit
) {
    val isDark = LocalAppTheme.current == "dark"
    val defaultActiveBg = if (isDark) Color.White else PrimaryTeal
    val defaultActiveIcon = if (isDark) Color.Black else Color.White
    
    val bgColors = if (isActive) {
        listOf(activeColor, activeColor)
    } else if (isDark) {
        listOf(Color(0x2BFFFFFF), Color(0x06FFFFFF))
    } else {
        listOf(Color(0xF0F1F5F9), Color(0xE2E2E8F0))
    }

    val borderColors = if (isActive) {
        listOf(activeColor.copy(alpha = 0.8f), activeColor.copy(alpha = 0.4f))
    } else if (isDark) {
        listOf(Color(0x59FFFFFF), Color(0x08FFFFFF))
    } else {
        listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8))
    }

    val iconTint = if (isActive) {
        activeIconColor
    } else if (isDark) {
        Color.White
    } else {
        Color(0xFF0F172A)
    }

    Box(
        modifier = Modifier.size(56.dp)
    ) {
        // Ambient shadow/glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .blur(8.dp)
                .background(
                    if (isActive) (if (isDark) defaultActiveBg else activeColor).copy(alpha = 0.3f) else if (isDark) Color(0x55000000) else Color(0x1F000000),
                    CircleShape
                )
        )

        // Actual Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.verticalGradient(colors = bgColors))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(colors = borderColors),
                    shape = CircleShape
                )
                .bounceClick { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun MinimalNavButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    activeColor: Color = Color(0xFFE85D04),
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(48.dp)
            .bounceClick { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.onBackground else themedInactiveIcon(),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else Color.Transparent)
        )
    }
}

@Composable
fun BottomNavBar(
    viewModel: TesseraViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onHoveredItemChange: (String?) -> Unit,
    currentRoute: String = "home",
    onNavigate: (String) -> Unit = {},
    onCameraClick: () -> Unit = {}
) {
    var displayedRoute by remember { mutableStateOf(currentRoute) }

    LaunchedEffect(currentRoute) {
        if (currentRoute != displayedRoute) {
            displayedRoute = currentRoute
        }
    }

    val handleTabClick: (String, Int) -> Unit = { route, _ ->
        if (route != displayedRoute) {
            onNavigate(route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .background(themedNavBarBackground(), RoundedCornerShape(40.dp))
                .border(1.dp, themedNavBarBorder(), RoundedCornerShape(40.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.wrapContentSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (displayedRoute) {
                    "finance" -> {
                        MinimalNavButton(
                            icon = Icons.Outlined.LightMode,
                            contentDescription = "Hoje",
                            isActive = false,
                            onClick = { handleTabClick("home", 0) }
                        )
                        MinimalNavButton(
                            icon = Icons.Default.ArrowDownward,
                            contentDescription = "Despesa",
                            isActive = false,
                            onClick = { viewModel.triggerFinanceAction(TesseraViewModel.FinanceAction.ADD_EXPENSE) }
                        )
                        MinimalNavButton(
                            icon = Icons.Default.ArrowUpward,
                            contentDescription = "Receita",
                            isActive = false,
                            onClick = { viewModel.triggerFinanceAction(TesseraViewModel.FinanceAction.ADD_INCOME) }
                        )
                    }
                    "health" -> {
                        MinimalNavButton(
                            icon = Icons.Outlined.LightMode,
                            contentDescription = "Hoje",
                            isActive = false,
                            onClick = { handleTabClick("home", 0) }
                        )
                        MinimalNavButton(
                            icon = Icons.Outlined.DirectionsWalk,
                            contentDescription = "Passos",
                            isActive = false,
                            onClick = { viewModel.triggerHealthAction(TesseraViewModel.HealthAction.ADD_STEPS) }
                        )
                        MinimalNavButton(
                            icon = Icons.Outlined.Bedtime,
                            contentDescription = "Sono",
                            isActive = false,
                            onClick = { viewModel.triggerHealthAction(TesseraViewModel.HealthAction.ADD_SLEEP) }
                        )
                    }
                    "wishes" -> {
                        MinimalNavButton(
                            icon = Icons.Outlined.LightMode,
                            contentDescription = "Hoje",
                            isActive = false,
                            onClick = { handleTabClick("home", 0) }
                        )
                        MinimalNavButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Adicionar Desejo",
                            isActive = false,
                            onClick = { viewModel.triggerWishesAction(TesseraViewModel.WishesAction.ADD_WISH) }
                        )
                        MinimalNavButton(
                            icon = Icons.Default.Search,
                            contentDescription = "Pesquisar",
                            isActive = false,
                            onClick = { viewModel.triggerWishesAction(TesseraViewModel.WishesAction.SEARCH_WISHES) }
                        )
                    }
                    else -> {
                        MinimalNavButton(
                            icon = Icons.Outlined.LightMode,
                            contentDescription = "Hoje",
                            isActive = displayedRoute == "home",
                            onClick = { handleTabClick("home", 0) }
                        )
                        MinimalNavButton(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Finanças",
                            isActive = displayedRoute == "finance",
                            onClick = { handleTabClick("finance", 1) }
                        )
                        MinimalNavButton(
                            icon = Icons.Outlined.Storefront,
                            contentDescription = "Mercado",
                            isActive = displayedRoute == "market",
                            onClick = { handleTabClick("market", 2) }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                MinimalNavButton(
                    icon = Icons.Default.Menu,
                    contentDescription = "Mais",
                    isActive = isExpanded,
                    activeColor = MaterialTheme.colorScheme.onBackground,
                    onClick = { onExpandedChange(!isExpanded) }
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
            Text(name, color = MaterialTheme.colorScheme.onBackground, fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
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
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
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
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
        }
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
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Wave1"
    )
    val wave2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing, delayMillis = 2000),
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
            .background(if (isDarkTheme()) Color(0xFF070909) else MaterialTheme.colorScheme.background),
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
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Sua privacidade está resguardada. Use biometria para acessar seu painel.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            contentColor = if (isDarkTheme()) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            "TENTAR NOVAMENTE",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
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
                color = MaterialTheme.colorScheme.onBackground,
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
