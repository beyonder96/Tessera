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
            NavHost(navController = navController, startDestination = "home", modifier = Modifier.fillMaxSize()) {
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
                composable("apartment") {
                    ApartmentScreen(onHomeClick = { 
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
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
                                    title = "Petz",
                                    subtitle = "Gerencie a rotina e tarefas dos seus pets",
                                    icon = Icons.Outlined.Pets,
                                    iconColor = TertiaryPurple,
                                    alpha = petzAlpha,
                                    offsetY = petzOffset,
                                    onClick = { navigateAction("petz"); isFabExpanded = false }
                                )
                                
                                val aptAlpha by animateFloatAsState(if (isFabExpanded) 1f else 0f, tween(350, delayMillis = 350))
                                val aptOffset by animateDpAsState(if (isFabExpanded) 0.dp else 40.dp, spring(dampingRatio = 0.82f, stiffness = 180f))
                                PremiumGlassCard(
                                    title = "Apartamento",
                                    subtitle = "Evolução de obra em andamento",
                                    icon = Icons.Outlined.Construction,
                                    iconColor = SecondaryGold,
                                    alpha = aptAlpha,
                                    offsetY = aptOffset,
                                    onClick = { navigateAction("apartment"); isFabExpanded = false }
                                )
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
    val petRoutines by homeViewModel.petRoutines.collectAsState()
    
    val mainViewModel: TesseraViewModel = viewModel(factory = com.example.viewmodel.TesseraViewModelFactory(com.example.data.TesseraRepository(com.example.data.AppDatabase.getDatabase(context).tesseraDao())))
    val transactions by mainViewModel.allTransactions.collectAsState()
    val realIncome = remember(transactions) { transactions.filter { it.isIncome }.sumOf { it.value } }
    val realExpense = remember(transactions) { transactions.filter { !it.isIncome }.sumOf { it.value } }
    val realBalance = realIncome - realExpense
    
    var showChatSheet by remember { mutableStateOf(false) }

    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    var backgroundUri by remember {
        mutableStateOf(
            sharedPrefs.getString("home_background_uri", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop")
            ?: "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop"
        )
    }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bgFile = java.io.File(context.filesDir, "custom_home_background.jpg")
                    bgFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val localUri = Uri.fromFile(bgFile)
                    backgroundUri = localUri.toString()
                    sharedPrefs.edit().putString("home_background_uri", localUri.toString()).apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                backgroundUri = uri.toString()
                sharedPrefs.edit().putString("home_background_uri", uri.toString()).apply()
            }
        }
    }

    val exportDatabaseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            try {
                val dbFile = context.getDatabasePath("tessera_database.db")
                if (dbFile.exists()) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        java.io.FileInputStream(dbFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Toast.makeText(context, "Backup exportado com sucesso!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Banco de dados não encontrado para exportar.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Falha ao exportar backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importDatabaseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val dbFile = context.getDatabasePath("tessera_database.db")
                    dbFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    Toast.makeText(context, "Backup restaurado! Por favor, reinicie o aplicativo.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Falha ao restaurar backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        homeViewModel.seedDatabase()
    }

    val parallaxOffset = scrollState.value * 0.2f
    val depthZoom = (1f + (scrollState.value * 0.00003f)).coerceIn(1.0f, 1.05f)
    
    val headerAlpha = (1f - (scrollState.value / 250f)).coerceIn(0f, 1f)
    val headerScale = (1f - (scrollState.value / 1200f)).coerceIn(0.85f, 1f)
    val headerTranslation = -scrollState.value * 0.15f
    
    val metricsAlpha = (1f - (scrollState.value / 400f)).coerceIn(0f, 1f)
    val metricsScale = (1f - (scrollState.value / 1500f)).coerceIn(0.9f, 1f)
    val metricsTranslation = -scrollState.value * 0.1f

    val heroAlpha = (1f - (scrollState.value / 550f)).coerceIn(0f, 1f)
    val heroScale = (1f - (scrollState.value / 2000f)).coerceIn(0.92f, 1f)
    val heroTranslation = -scrollState.value * 0.08f

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
                .fillMaxWidth()
                .height(460.dp)
                .graphicsLayer {
                    translationY = -parallaxOffset
                    scaleX = depthZoom
                    scaleY = depthZoom
                }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
                .graphicsLayer {
                    translationY = -parallaxOffset
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x00070909),
                            Color(0x22070909),
                            Color(0x88070909),
                            Color(0xFF070909)
                        )
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
                        alpha = headerAlpha
                        scaleX = headerScale
                        scaleY = headerScale
                        translationY = headerTranslation
                    }
            ) {
                TopHeader(onOpenSettings = { showSettingsDialog = true })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = metricsAlpha
                        scaleX = metricsScale
                        scaleY = metricsScale
                        translationY = metricsTranslation
                    }
            ) {
                TopMetricsRow(realBalance, realIncome, realExpense, onNavigate)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = heroAlpha
                        scaleX = heroScale
                        scaleY = heroScale
                        translationY = heroTranslation
                    }
            ) {
                HeroMetric(onNavigate)
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            MainContent(netWorth, petRoutines, mainViewModel, onNavigate)
            Spacer(modifier = Modifier.height(140.dp))
        }
    }

    if (showChatSheet) {
        TesseraChatSheet(onDismiss = { showChatSheet = false }, netWorth = netWorth, petRoutines = petRoutines)
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "Configurações",
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "PERSONALIZAÇÃO DE FUNDO",
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                    
                    Text(
                        text = "Temas Disponíveis",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(
                            Pair("Montanha", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop"),
                            Pair("Aurora", "https://images.unsplash.com/photo-1531366936337-7c912a4589a7?q=80&w=800&auto=format&fit=crop"),
                            Pair("Nebulosa", "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?q=80&w=800&auto=format&fit=crop"),
                            Pair("Veludo", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=800&auto=format&fit=crop"),
                            Pair("Estrelado", "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?q=80&w=800&auto=format&fit=crop")
                        )
                        
                        items(presets.size) { index ->
                            val (name, url) = presets[index]
                            val isSelected = backgroundUri == url
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(76.dp)
                                    .clickable {
                                        backgroundUri = url
                                        sharedPrefs.edit().putString("home_background_uri", url).apply()
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) PrimaryTeal else Color(0x33FFFFFF),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galeria", color = Color.Black, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val defaultUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop"
                                backgroundUri = defaultUrl
                                sharedPrefs.edit().putString("home_background_uri", defaultUrl).apply()
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Padrão", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "GERENCIAMENTO DE BACKUP & BANCO",
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryGold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0AFFFFFF))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Banco Local:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Conectado 🟢", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tamanho do Banco:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(getDatabaseSizeInKB(context), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Transações Salvas:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${transactions.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Rotinas de Pets:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${petRoutines.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                exportDatabaseLauncher.launch("tessera_backup.db")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar Banco de Dados", color = Color.Black, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                importDatabaseLauncher.launch(arrayOf("*/*"))
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = SecondaryGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restaurar Banco de Dados", color = SecondaryGold, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Fechar", color = PrimaryTeal)
                }
            },
            containerColor = SurfaceVariantDark,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                        .clickable {
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
                Text(
                    text = "$greeting, Kenned",
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Configurações",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun TopMetricsRow(netWorth: Double, totalIncome: Double, totalExpense: Double, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    
    // Widget 1 (Financeiro)
    var financeIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(5 * 60 * 1000L)
            financeIndex = (financeIndex + 1) % 3
        }
    }

    // Widget 2 (Saúde)
    var healthIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(5 * 60 * 1000L)
            healthIndex = (healthIndex + 1) % 2
        }
    }

    // Widget 3 (Metas)
    var selectedGoal by remember { mutableStateOf(sharedPrefs.getString("selected_goal_type", "Atividade") ?: "Atividade") }
    var showGoalDialog by remember { mutableStateOf(false) }
    
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Selecione a Meta", fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    listOf("Atividade", "Tarefas Pendentes", "Ritual Diário").forEach { goal ->
                        TextButton(onClick = { 
                            selectedGoal = goal
                            sharedPrefs.edit().putString("selected_goal_type", goal).apply()
                            showGoalDialog = false 
                        }) {
                            Text(goal, color = MaterialTheme.colorScheme.onSurface)
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
                Crossfade(targetState = financeIndex, animationSpec = tween(500)) { idx ->
                    val valIdx = when(idx) {
                        0 -> netWorth
                        1 -> totalIncome
                        else -> totalExpense
                    }
                    val labelIdx = when(idx) {
                        0 -> "SALDO"
                        1 -> "RECEITAS"
                        else -> "DESPESAS"
                    }
                    val iconIdx = when(idx) {
                        0 -> Icons.Outlined.AccountBalanceWallet
                        1 -> Icons.Outlined.ArrowUpward
                        else -> Icons.Outlined.ArrowDownward
                    }
                    val formattedIdx = if (valIdx >= 1000) "${(valIdx / 1000).toInt()}k" else valIdx.toInt().toString()
                    MetricItem(iconIdx, formattedIdx, labelIdx, onClick = { onNavigate("finance") })
                }
            }

            // Widget 2 (Saúde) with smooth crossfade rotation!
            Box(modifier = Modifier.width(76.dp)) {
                Crossfade(targetState = healthIndex, animationSpec = tween(500)) { idx ->
                    val iconIdx = if (idx == 0) Icons.Outlined.Bedtime else Icons.Outlined.MonitorWeight
                    val valIdx = if (idx == 0) "8.2h" else "75.2"
                    val labelIdx = if (idx == 0) "SONO" else "PESO"
                    val progressIdx = if (idx == 0) 0.82f else 0.75f
                    val colorIdx = if (idx == 0) PrimaryTeal else TertiaryPurple
                    MetricItemWithProgress(iconIdx, valIdx, labelIdx, colorIdx, progressIdx, onClick = { onNavigate("health") })
                }
            }

            // Widget 3 (Metas)
            Box(modifier = Modifier.width(76.dp)) {
                Crossfade(targetState = selectedGoal, animationSpec = tween(500)) { goal ->
                    val valIdx = when(goal) {
                        "Tarefas Pendentes" -> "3"
                        "Ritual Diário" -> "1"
                        else -> "72"
                    }
                    val iconIdx = when(goal) {
                        "Tarefas Pendentes" -> Icons.AutoMirrored.Outlined.Assignment
                        "Ritual Diário" -> Icons.Outlined.SelfImprovement
                        else -> Icons.AutoMirrored.Outlined.DirectionsRun
                    }
                    MetricItem(iconIdx, valIdx, if (goal == "Tarefas Pendentes") "TAREFAS" else goal.uppercase(), onClick = { showGoalDialog = true })
                }
            }

            // Widget 4 (Apartamento)
            MetricItemWithProgress(Icons.Outlined.Construction, "${(aptProgress * 100).toInt()}%", "OBRA", SecondaryGold, aptProgress, onClick = { onNavigate("apartment") })
        }
    }
}

@Composable
fun MetricItem(icon: ImageVector, value: String, label: String, onClick: () -> Unit) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
data class ModuleConfig(val id: String, val name: String, var isVisible: Boolean, var order: Int)

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun MainContent(netWorth: Double, petRoutines: List<PetRoutineEntity>, mainViewModel: com.example.viewmodel.TesseraViewModel, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    var showEditSheet by remember { mutableStateOf(false) }

    val defaultModules = listOf(
        ModuleConfig("finance", "Finanças", true, 0),
        ModuleConfig("market", "Mercado", true, 1),
        ModuleConfig("pets", "Petz", true, 2),
        ModuleConfig("system", "Resumo", true, 3),
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
                    "pets" -> PetsCard(petRoutines, onNavigate)
                    "system" -> {
                        val transactions by mainViewModel.allTransactions.collectAsState()
                        val marketItems by mainViewModel.pendingMarketItems.collectAsState()
                        AISummaryWidget(
                            netWorth = netWorth,
                            transactions = transactions,
                            petRoutines = petRoutines,
                            pendingMarketCount = marketItems.size,
                            onNavigate = onNavigate
                        )
                    }
                "health" -> HealthWidget()
                "goals" -> GoalsWidget(onNavigate)
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
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    val todayDate = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    // Reset daily check
    var userFeeling by remember { mutableStateOf("") }
    var isVitaminDChecked by remember { mutableStateOf(false) }
    var isOmega3Checked by remember { mutableStateOf(false) }
    var isMelatoninChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val savedFeelingDate = sharedPrefs.getString("feeling_date", "")
        if (savedFeelingDate == todayDate) {
            userFeeling = sharedPrefs.getString("user_feeling", "") ?: ""
        } else {
            sharedPrefs.edit().putString("feeling_date", todayDate).putString("user_feeling", "").apply()
            userFeeling = ""
        }

        val savedMedsDate = sharedPrefs.getString("meds_date", "")
        if (savedMedsDate == todayDate) {
            isVitaminDChecked = sharedPrefs.getBoolean("med_vitamind_taken", false)
            isOmega3Checked = sharedPrefs.getBoolean("med_omega3_taken", false)
            isMelatoninChecked = sharedPrefs.getBoolean("med_melatonin_taken", false)
        } else {
            sharedPrefs.edit()
                .putString("meds_date", todayDate)
                .putBoolean("med_vitamind_taken", false)
                .putBoolean("med_omega3_taken", false)
                .putBoolean("med_melatonin_taken", false)
                .apply()
            isVitaminDChecked = false
            isOmega3Checked = false
            isMelatoninChecked = false
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
            title = { Text("Como você está hoje?", fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.onSurface) },
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

    Column(modifier = GlassModifier.fillMaxWidth().padding(24.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAÚDE RÁPIDA",
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left column: Medications checklist
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = "PRÓXIMAS MEDICAÇÕES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                MedItem("Vitamina D", "08:00", isVitaminDChecked) { checked ->
                    isVitaminDChecked = checked
                    sharedPrefs.edit().putBoolean("med_vitamind_taken", checked).apply()
                }
                Spacer(modifier = Modifier.height(8.dp))
                MedItem("Omega 3", "12:00", isOmega3Checked) { checked ->
                    isOmega3Checked = checked
                    sharedPrefs.edit().putBoolean("med_omega3_taken", checked).apply()
                }
                Spacer(modifier = Modifier.height(8.dp))
                MedItem("Melatonina", "21:30", isMelatoninChecked) { checked ->
                    isMelatoninChecked = checked
                    sharedPrefs.edit().putBoolean("med_melatonin_taken", checked).apply()
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right column: How I am feeling
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "COMO ESTOU ME SENTINDO",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (userFeeling.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0x0AFFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { showFeelingDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = "Adicionar Sentimento",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
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
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userFeeling.substringAfter(" "),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = feelingColor
                        )
                    }
                }
            }
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
fun GoalsWidget(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    
    val hydrationChecked = sharedPrefs.getBoolean("goal_hydration_checked", false)
    val readingChecked = sharedPrefs.getBoolean("goal_reading_checked", false)
    val mindfulnessChecked = sharedPrefs.getBoolean("goal_mindfulness_checked", true)

    val completedRituais = (if (hydrationChecked) 1 else 0) + 
                           (if (readingChecked) 1 else 0) + 
                           (if (mindfulnessChecked) 1 else 0)
    
    val completedAtividades = 1 // Passos e Calorias não concluídos, Tempo Ativo concluído (45/30)
    val totalCompleted = completedAtividades + completedRituais
    val totalGoals = 6
    val progress = totalCompleted.toFloat() / totalGoals.toFloat()

    Column(
        modifier = GlassModifier
            .fillMaxWidth()
            .clickable { onNavigate("goals") }
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "METAS DIÁRIAS",
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(Icons.Outlined.Flag, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
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
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Summaries of activities and rituals
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Atividades
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.DirectionsRun,
                    contentDescription = null,
                    tint = SecondaryGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Atividades: ",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$completedAtividades de 3 concluídas",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            // Rituais
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    text = "$completedRituais de 3 concluídos",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
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
            .clickable { onNavigate("market") }
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
fun PetsCard(routines: List<PetRoutineEntity>, onNavigate: (String) -> Unit) {
    Column(
        modifier = GlassModifier
            .fillMaxWidth()
            .clickable { onNavigate("petz") }
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
            .clickable { onNavigate("finance") }
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
                text = "R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", balance)}",
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
                    text = "R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", totalIncome)}",
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
                    text = "R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", totalExpense)}",
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
    petRoutines: List<PetRoutineEntity>,
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

            val completedPetRoutines = petRoutines.count { it.isCompleted }
            val totalPetRoutines = petRoutines.size

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
                                    append("R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", balance)}")
                                    pop()
                                } else {
                                    append("Saldo negativo de ")
                                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFE57373)))
                                    append("R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", Math.abs(balance))}")
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
                                    val petsString = if (petRoutines.isEmpty()) "Nenhum compromisso pendente hoje." else petRoutines.joinToString("; ") { "${it.petName}: ${it.task} (${if(it.isCompleted) "Concluído" else "Pendente"})" }
                                    val hiddenContext = """
                                        [Contexto] Nome: Kenned | Patrimônio: R$ ${String.format(java.util.Locale("pt", "BR"), "%.2f", netWorth)} | Pets: $petsString
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
                            text = "2. Com um gerenciador de arquivos no seu celular, copie o arquivo .bin para a pasta privada do TesseraHub:",
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
}
