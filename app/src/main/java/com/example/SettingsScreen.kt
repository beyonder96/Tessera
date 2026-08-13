package com.example
import androidx.compose.material3.MaterialTheme

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryGold
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.bounceClick
import com.example.ui.components.isDarkTheme
import com.example.ui.components.themedCardBackground
import com.example.ui.components.themedCardBorder
import com.example.ui.components.themedButtonBorder
import com.example.ui.components.themedTextFieldColors
import com.example.ui.components.themedSubtleBackground
import com.example.ui.components.themedSubtleBorder
import com.example.ui.components.themedDivider
import com.example.ui.components.themedOverlayBackground
import com.example.ui.components.themedSwitchColors
import com.example.ui.components.themedCheckboxColors
import androidx.compose.ui.draw.blur
import com.example.data.AppDatabase
import com.example.viewmodel.TesseraViewModel
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.WeightRecord as HCWeightRecord
import androidx.health.connect.client.records.StepsRecord as HCStepsRecord
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccessTime
import com.example.data.getMetroLineColor

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: TesseraViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    val userName = remember { sharedPrefs.getString("user_name", "Kenned") ?: "Kenned" }
    var activeCategory by remember { mutableStateOf<String?>(null) }
    val packageInfo = remember {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }
    }
    val appVersionName = packageInfo?.versionName ?: "1.0.2"
    
    var isBiometricEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_enabled", false)) }
    var sumInvestmentsToBalance by remember { mutableStateOf(sharedPrefs.getBoolean("sum_investments_to_balance", false)) }
    var sumInvestmentsToSpendable by remember { mutableStateOf(sharedPrefs.getBoolean("sum_investments_to_spendable", false)) }
    var stepsReminderTime by remember { mutableStateOf(sharedPrefs.getString("steps_reminder_time", "20:00") ?: "20:00") }
    var sleepReminderTime by remember { mutableStateOf(sharedPrefs.getString("sleep_reminder_time", "08:00") ?: "08:00") }
    val currentAppTheme by viewModel.appTheme.collectAsState()
    val currentGlassLevel by viewModel.glassmorphismLevel.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val healthProfile by viewModel.healthProfile.collectAsState(initial = null)

    val permissions = setOf(
        HealthPermission.getReadPermission(HCWeightRecord::class),
        HealthPermission.getWritePermission(HCWeightRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HCStepsRecord::class),
        HealthPermission.getWritePermission(HCStepsRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class)
    )

    val requiredReadPermissions = setOf(
        HealthPermission.getReadPermission(HCWeightRecord::class),
        HealthPermission.getReadPermission(HCStepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class)
    )

    val requestPermissionActivityContract = remember { 
        try {
            PermissionController.createRequestPermissionResultContract()
        } catch (e: Exception) {
            null
        }
    }

    val requestPermissions = requestPermissionActivityContract?.let { contract ->
        rememberLauncherForActivityResult(contract) { granted ->
            if (granted.containsAll(requiredReadPermissions) || granted.isNotEmpty()) {
                viewModel.updateHealthProfile(
                    heightCm = healthProfile?.heightCm ?: 0.0,
                    targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                    isHealthConnectEnabled = true
                )
                Toast.makeText(context, "Sincronização com Health Connect ativada!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val petEvents by viewModel.allPetEvents.collectAsState(initial = emptyList())
    val configuredFootballTeams by viewModel.configuredFootballTeams.collectAsState()
    val availableFootballTeams by viewModel.availableFootballTeams.collectAsState()

    val exportDatabaseLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            try {
                // Fechar banco temporariamente para realizar checkpoint completo do WAL para o arquivo principal
                AppDatabase.closeAndClearInstance()
                val dbFile = context.getDatabasePath("tessera_database.db")
                if (dbFile.exists()) {
                    dbFile.inputStream().use { inputStream ->
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Toast.makeText(context, "Backup exportado com sucesso!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Falha ao exportar backup", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importDatabaseLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                // Fechar conexão ativa para evitar conflitos/travamento do SQLite
                AppDatabase.closeAndClearInstance()
                
                // Excluir arquivos temporários do modo WAL para evitar corrupção e inconsistência
                val walFile = context.getDatabasePath("tessera_database.db-wal")
                val shmFile = context.getDatabasePath("tessera_database.db-shm")
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val dbFile = context.getDatabasePath("tessera_database.db")
                    dbFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    Toast.makeText(context, "Backup restaurado! Por favor, reinicie o aplicativo.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Falha ao restaurar backup", Toast.LENGTH_LONG).show()
            }
        }
    }
    var showResetFinancesDialog by remember { mutableStateOf(false) }

    if (showResetFinancesDialog) {
        AlertDialog(
            onDismissRequest = { showResetFinancesDialog = false },
            title = { Text("Zerar Finanças", color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("Isso apagará todas as transações, cartões de crédito e contas bancárias. Tem certeza?", color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f)) },
            containerColor = Color(0xFF1E1E1E),
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllFinances()
                    showResetFinancesDialog = false
                    Toast.makeText(context, "Finanças zeradas com sucesso!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Zerar", color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetFinancesDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.7f))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = when (activeCategory) {
                                "personalizacao" -> "Aparência e Temas"
                                "privacidade" -> "Segurança e Privacidade"
                                "financas" -> "Finanças"
                                "metro" -> "Transporte e Trânsito"
                                "futebol" -> "Esportes e Futebol"
                                "dados" -> "Dados e Backup"
                                else -> "Configurações"
                            },
                            fontFamily = FontFamily.SansSerif, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 22.sp, 
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 0.5.sp
                        ) 
                    },
                    navigationIcon = { 
                        val handleBack = {
                            if (activeCategory != null) {
                                activeCategory = null
                            } else {
                                onBack()
                            }
                        }
                        IconButton(
                            onClick = handleBack,
                            modifier = Modifier.bounceClick { handleBack() }
                        ) { 
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                "Voltar", 
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                            ) 
                        } 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp)
            ) {
                // User Profile Header Card
                item {
                    if (activeCategory == null) {
                        val profileUriStr = remember { sharedPrefs.getString("user_profile_uri", null) }
                        val profileUri = remember(profileUriStr) { profileUriStr?.let { Uri.parse(it) } }

                        Row(
                            modifier = PremiumGlassModifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (profileUri != null) {
                                AsyncImage(
                                    model = profileUri,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, SecondaryGold, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(themedSubtleBackground())
                                        .border(1.dp, themedSubtleBorder(), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Olá, $userName!",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Membro Tessera Premium",
                                    color = SecondaryGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                // Main Categories Menu
                item {
                    if (activeCategory == null) {
                        Card(
                            modifier = PremiumGlassModifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeCategory = "personalizacao" }
                                        .padding(vertical = 16.dp, horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(SecondaryGold.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Palette, null, tint = SecondaryGold, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Aparência e Temas", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Plano de fundo e estilo glassmorphism", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)))
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeCategory = "privacidade" }
                                        .padding(vertical = 16.dp, horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryTeal.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Fingerprint, null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Segurança e Privacidade", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Biometria, lembretes e Health Connect", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeCategory = "financas" }
                                        .padding(vertical = 16.dp, horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF71D7CD).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.AccountBalanceWallet, null, tint = Color(0xFF71D7CD), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Finanças", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Configurações de saldos e investimentos", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeCategory = "metro" }
                                        .padding(vertical = 16.dp, horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4FC3F7).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.DirectionsTransit, null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Transporte e Trânsito", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Horários e linhas do metrô e trem", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeCategory = "futebol" }
                                        .padding(vertical = 16.dp, horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF69F0AE).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.SportsSoccer, null, tint = Color(0xFF69F0AE), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Esportes e Futebol", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Times e seleções monitorados", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeCategory = "dados" }
                                        .padding(vertical = 16.dp, horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE57373).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Outlined.Storage, null, tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Dados e Backup", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Importar, exportar e zerar dados", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    if (activeCategory == "financas") {
                        SectionTitle("SALDOS E INVESTIMENTOS", Color(0xFF71D7CD))
                        Column(
                            modifier = PremiumGlassModifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        sumInvestmentsToBalance = !sumInvestmentsToBalance
                                        sharedPrefs.edit().putBoolean("sum_investments_to_balance", sumInvestmentsToBalance).apply()
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF71D7CD).copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = Color(0xFF71D7CD), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Somar investimentos ao saldo", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Incluir valor no saldo total exibido", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                                    }
                                }
                                Switch(
                                    checked = sumInvestmentsToBalance,
                                    onCheckedChange = { 
                                        sumInvestmentsToBalance = it
                                        sharedPrefs.edit().putBoolean("sum_investments_to_balance", it).apply()
                                    },
                                    colors = themedSwitchColors()
                                )
                            }
                            
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0AFFFFFF)))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        sumInvestmentsToSpendable = !sumInvestmentsToSpendable
                                        sharedPrefs.edit().putBoolean("sum_investments_to_spendable", sumInvestmentsToSpendable).apply()
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF71D7CD).copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Savings, contentDescription = null, tint = Color(0xFF71D7CD), modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Somar ao disponível p/ gastar", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Investimentos contam como dinheiro livre", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                                    }
                                }
                                Switch(
                                    checked = sumInvestmentsToSpendable,
                                    onCheckedChange = { 
                                        sumInvestmentsToSpendable = it
                                        sharedPrefs.edit().putBoolean("sum_investments_to_spendable", it).apply()
                                    },
                                    colors = themedSwitchColors()
                                )
                            }
                        }
                    }
                }

                item {
                    if (activeCategory == "personalizacao") {
                        val isDark = currentAppTheme == "dark"
                        val textColor = MaterialTheme.colorScheme.onBackground
                        val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        val accentColor = if (isDark) SecondaryGold else PrimaryTeal

                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            SectionTitle("TEMA DO APLICATIVO", accentColor)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // OLED Card
                                val isOled = currentAppTheme == "dark"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFF09090B))
                                        .border(
                                            width = if (isOled) 2.dp else 1.dp,
                                            color = if (isOled) SecondaryGold else Color(0xFF27272A),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .clickable { viewModel.updateAppTheme("dark") }
                                        .padding(16.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(SecondaryGold.copy(alpha = 0.2f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "OLED",
                                                    color = SecondaryGold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            if (isOled) {
                                                Icon(
                                                    Icons.Outlined.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SecondaryGold,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        
                                        // Mini UI Mockup
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF18181B))
                                                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(12.dp))
                                                .padding(8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(SecondaryGold))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(modifier = Modifier.width(50.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)))
                                            }
                                        }
                                        
                                        Column {
                                            Text(
                                                text = "Preto Profundo",
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Economia de bateria",
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                // LIGHT Card
                                val isLight = currentAppTheme == "light"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFFFFFFFF))
                                        .border(
                                            width = if (isLight) 2.dp else 1.dp,
                                            color = if (isLight) Color(0xFF0D9488) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .clickable { viewModel.updateAppTheme("light") }
                                        .padding(16.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF0D9488).copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "LIGHT",
                                                    color = Color(0xFF0D9488),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            }
                                            if (isLight) {
                                                Icon(
                                                    Icons.Outlined.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0D9488),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        
                                        // Mini UI Mockup Light
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                                .padding(8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(0xFF0D9488)))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(modifier = Modifier.width(50.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF0F172A)))
                                            }
                                        }
                                        
                                        Column {
                                            Text(
                                                text = "Branco Total",
                                                color = Color(0xFF0F172A),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Alta legibilidade",
                                                color = Color(0xFF64748B),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Glassmorphism Section
                            SectionTitle("ESTILO DO GLASSMORPHISM", accentColor)
                            
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val glassStyles = listOf(
                                    Triple("Frosted", "Conceito iOS / macOS", "Equilíbrio perfeito entre opacidade e desfoque aveludado."),
                                    Triple("Clear", "Visão Cristalina", "Superfície translúcida de alto brilho com bordas nítidas."),
                                    Triple("Blur", "Suavidade One UI", "Fundo ambiente com desfoque profundo e visual suave.")
                                )
                                
                                glassStyles.forEach { (style, subtitle, desc) ->
                                    val isSelected = currentGlassLevel == style
                                    val cardBg = if (isDark) {
                                        if (isSelected) Color(0x33FFFFFF) else Color(0x12FFFFFF)
                                    } else {
                                        if (isSelected) Color(0xE6FFFFFF) else Color(0xB3F8F9FA)
                                    }
                                    val cardBorder = if (isSelected) {
                                        accentColor
                                    } else if (isDark) {
                                        themedButtonBorder()
                                    } else {
                                        Color(0xFFCBD5E1)
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(cardBg)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = cardBorder,
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .clickable { viewModel.updateGlassmorphismLevel(style) }
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) accentColor.copy(alpha = 0.2f) else subtextColor.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (isSelected) accentColor else subtextColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = style,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textColor
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(accentColor.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = subtitle,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = accentColor
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = desc,
                                                    fontSize = 12.sp,
                                                    color = subtextColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Live Preview Component
                            SectionTitle("PRÉ-VISUALIZAÇÃO EM TEMPO REAL", accentColor)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(PremiumGlassModifier)
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Cartão Demonstrativo",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                            Text(
                                                text = "Veja como os elementos respondem ao seu tema",
                                                fontSize = 12.sp,
                                                color = subtextColor
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(accentColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.Palette,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(subtextColor.copy(alpha = 0.15f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.75f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(accentColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    if (activeCategory == "privacidade") {
                        SectionTitle("PRIVACIDADE E SEGURANÇA", PrimaryTeal)
                    Column(
                        modifier = PremiumGlassModifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick {
                                    isBiometricEnabled = !isBiometricEnabled
                                    sharedPrefs.edit().putBoolean("biometric_enabled", isBiometricEnabled).apply()
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryTeal.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Desbloqueio Biométrico", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Exigir digital ou face ID ao abrir o app", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                                }
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { 
                                    isBiometricEnabled = it
                                    sharedPrefs.edit().putBoolean("biometric_enabled", it).apply()
                                },
                                colors = themedSwitchColors()
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0AFFFFFF)))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick {
                                    val isEnabled = healthProfile?.isHealthConnectEnabled != true
                                    if (isEnabled) {
                                        coroutineScope.launch {
                                            try {
                                                val providerPackageName = "com.google.android.apps.healthdata"
                                                val availabilityStatus = HealthConnectClient.getSdkStatus(context, providerPackageName)
                                                if (availabilityStatus == HealthConnectClient.SDK_AVAILABLE) {
                                                    val client = HealthConnectClient.getOrCreate(context)
                                                    val granted = client.permissionController.getGrantedPermissions()
                                                    if (granted.containsAll(requiredReadPermissions)) {
                                                        viewModel.updateHealthProfile(
                                                            heightCm = healthProfile?.heightCm ?: 0.0,
                                                            targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                                                            isHealthConnectEnabled = true
                                                        )
                                                        Toast.makeText(context, "Sincronização ativada!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        requestPermissions?.launch(permissions)
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Saúde Connect indisponível no sistema", Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                requestPermissions?.launch(permissions)
                                            }
                                        }
                                    } else {
                                        viewModel.updateHealthProfile(
                                            heightCm = healthProfile?.heightCm ?: 0.0,
                                            targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                                            isHealthConnectEnabled = false
                                        )
                                        Toast.makeText(context, "Sincronização desativada", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryTeal.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Sync, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Google Health Connect", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        if (healthProfile?.isHealthConnectEnabled == true) "Sincronização ativa" else "Sincronizar passos, peso e sono",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f)
                                    )
                                }
                            }
                            Switch(
                                checked = healthProfile?.isHealthConnectEnabled == true,
                                onCheckedChange = { isEnabled ->
                                    if (isEnabled) {
                                        coroutineScope.launch {
                                            try {
                                                val providerPackageName = "com.google.android.apps.healthdata"
                                                val availabilityStatus = HealthConnectClient.getSdkStatus(context, providerPackageName)
                                                if (availabilityStatus == HealthConnectClient.SDK_AVAILABLE) {
                                                    val client = HealthConnectClient.getOrCreate(context)
                                                    val granted = client.permissionController.getGrantedPermissions()
                                                    if (granted.containsAll(requiredReadPermissions)) {
                                                        viewModel.updateHealthProfile(
                                                            heightCm = healthProfile?.heightCm ?: 0.0,
                                                            targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                                                            isHealthConnectEnabled = true
                                                        )
                                                        Toast.makeText(context, "Sincronização ativada!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        requestPermissions?.launch(permissions)
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Saúde Connect indisponível no sistema", Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                requestPermissions?.launch(permissions)
                                            }
                                        }
                                    } else {
                                        viewModel.updateHealthProfile(
                                            heightCm = healthProfile?.heightCm ?: 0.0,
                                            targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                                            isHealthConnectEnabled = false
                                        )
                                        Toast.makeText(context, "Sincronização desativada", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = themedSwitchColors()
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0AFFFFFF)))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick {
                                    val parts = stepsReminderTime.split(":")
                                    val h = parts[0].toIntOrNull() ?: 20
                                    val m = parts[1].toIntOrNull() ?: 0
                                    android.app.TimePickerDialog(context, { _, hourOfDay, minute ->
                                        val formatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                                        stepsReminderTime = formatted
                                        sharedPrefs.edit().putString("steps_reminder_time", formatted).apply()
                                        com.example.notifications.AlarmScheduler.scheduleDailyReminder(context, "STEPS", formatted)
                                        Toast.makeText(context, "Lembrete de passos configurado", Toast.LENGTH_SHORT).show()
                                    }, h, m, true).show()
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryTeal.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.DirectionsWalk, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Lembrete de Passos", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Notificar diariamente às $stepsReminderTime", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0AFFFFFF)))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick {
                                    val parts = sleepReminderTime.split(":")
                                    val h = parts[0].toIntOrNull() ?: 8
                                    val m = parts[1].toIntOrNull() ?: 0
                                    android.app.TimePickerDialog(context, { _, hourOfDay, minute ->
                                        val formatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                                        sleepReminderTime = formatted
                                        sharedPrefs.edit().putString("sleep_reminder_time", formatted).apply()
                                        com.example.notifications.AlarmScheduler.scheduleDailyReminder(context, "SLEEP", formatted)
                                        Toast.makeText(context, "Lembrete de sono configurado", Toast.LENGTH_SHORT).show()
                                    }, h, m, true).show()
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(PrimaryTeal.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Lembrete de Sono", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Notificar diariamente às $sleepReminderTime", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f))
                                }
                            }
                        }
                    }
                    }
                }

                item {
                    if (activeCategory == "metro") {
                        SectionTitle("STATUS METROFERROVIÁRIO", Color(0xFF4FC3F7))
                    
                    val showTimeDialog = remember { mutableStateOf(false) }
                    val alertTimes = remember { 
                        mutableStateOf(
                            sharedPrefs.getStringSet("metro_alert_times", emptySet())
                                ?.toList()
                                ?.sorted() 
                                ?: emptyList()
                        ) 
                    }
                    val selectedLines = remember { 
                        mutableStateOf(
                            sharedPrefs.getStringSet("metro_monitored_lines", emptySet()) 
                                ?: emptySet()
                        ) 
                    }

                    val concessionarias by viewModel.metroConcessionarias.collectAsState()
                    val isLoadingConfig by viewModel.isLoadingMetroConfig.collectAsState()
                    val metroError by viewModel.metroError.collectAsState()

                    LaunchedEffect(Unit) {
                        viewModel.fetchMetroConcessionarias()
                    }

                    Column(
                        modifier = PremiumGlassModifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Horários de Alerta",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "O pop-up de status do metrô e trem será exibido na tela inicial nos horários configurados.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            
                            if (alertTimes.value.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(themedSubtleBackground(), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nenhum horário programado.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    alertTimes.value.forEach { time ->
                                        Row(
                                            modifier = Modifier
                                                .background(themedSubtleBackground(), RoundedCornerShape(16.dp))
                                                .border(1.dp, themedSubtleBorder(), RoundedCornerShape(16.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = time, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remover",
                                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        val updated = alertTimes.value.toMutableList().apply { remove(time) }
                                                        alertTimes.value = updated.sorted()
                                                        sharedPrefs.edit().putStringSet("metro_alert_times", alertTimes.value.toSet()).apply()
                                                        com.example.notifications.AlarmScheduler.cancelDailyReminder(context, "METRO_$time")
                                                        Toast.makeText(context, "Horário removido!", Toast.LENGTH_SHORT).show()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { showTimeDialog.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7).copy(alpha = 0.2f), contentColor = Color(0xFF4FC3F7)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .border(1.dp, Color(0xFF4FC3F7).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .bounceClick { showTimeDialog.value = true }
                            ) {
                                Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Adicionar Horário", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x0AFFFFFF)))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Linhas a Monitorar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Selecione as linhas que deseja acompanhar.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )

                            if (isLoadingConfig) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF4FC3F7))
                                }
                            } else if (metroError != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = metroError ?: "Erro desconhecido",
                                        color = Color(0xFFE57373),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.fetchMetroConcessionarias() },
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4FC3F7)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Tentar Novamente", color = Color(0xFF4FC3F7), fontSize = 12.sp)
                                    }
                                }
                            } else {
                                concessionarias.forEach { empresa ->
                                    val linhas = empresa.linhas ?: emptyList()
                                    if (linhas.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = empresa.nome.uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                                letterSpacing = 1.sp,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                            linhas.forEach { linha ->
                                                val lineKey = "${empresa.id}_${linha.codigo}"
                                                val isChecked = selectedLines.value.contains(lineKey)
                                                val lineColor = getMetroLineColor(linha.nome, linha.codigo)

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable {
                                                            val updated = selectedLines.value.toMutableSet()
                                                            if (isChecked) updated.remove(lineKey) else updated.add(lineKey)
                                                            selectedLines.value = updated
                                                            sharedPrefs.edit().putStringSet("metro_monitored_lines", updated).apply()
                                                        }
                                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(lineColor)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = linha.nome,
                                                        color = MaterialTheme.colorScheme.onBackground,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            val updated = selectedLines.value.toMutableSet()
                                                            if (checked) updated.add(lineKey) else updated.remove(lineKey)
                                                            selectedLines.value = updated
                                                            sharedPrefs.edit().putStringSet("metro_monitored_lines", updated).apply()
                                                        },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = Color(0xFF4FC3F7),
                                                            uncheckedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                                            checkmarkColor = Color.Black
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showTimeDialog.value) {
                        var tempHour by remember { mutableStateOf(8) }
                        var tempMinute by remember { mutableStateOf(0) }

                        Dialog(onDismissRequest = { showTimeDialog.value = false }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(themedOverlayBackground())
                                    .border(1.dp, themedButtonBorder(), RoundedCornerShape(28.dp))
                                    .padding(24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    Text(
                                        text = "Adicionar Horário",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            IconButton(onClick = { tempHour = if (tempHour < 23) tempHour + 1 else 0 }) {
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Mais", tint = MaterialTheme.colorScheme.onBackground)
                                            }
                                            Text(
                                                text = String.format("%02d", tempHour),
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(onClick = { tempHour = if (tempHour > 0) tempHour - 1 else 23 }) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Menos", tint = MaterialTheme.colorScheme.onBackground)
                                            }
                                        }

                                        Text(
                                            text = ":",
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            IconButton(onClick = { tempMinute = if (tempMinute < 59) tempMinute + 1 else 0 }) {
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Mais", tint = MaterialTheme.colorScheme.onBackground)
                                            }
                                            Text(
                                                text = String.format("%02d", tempMinute),
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(onClick = { tempMinute = if (tempMinute > 0) tempMinute - 1 else 59 }) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Menos", tint = MaterialTheme.colorScheme.onBackground)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showTimeDialog.value = false },
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, themedButtonBorder()),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancelar", color = MaterialTheme.colorScheme.onBackground)
                                        }
                                        Button(
                                            onClick = {
                                                val formattedTime = String.format("%02d:%02d", tempHour, tempMinute)
                                                if (!alertTimes.value.contains(formattedTime)) {
                                                    val updated = alertTimes.value.toMutableList().apply { add(formattedTime) }
                                                    alertTimes.value = updated.sorted()
                                                    sharedPrefs.edit().putStringSet("metro_alert_times", alertTimes.value.toSet()).apply()
                                                    com.example.notifications.AlarmScheduler.scheduleDailyReminder(context, "METRO_$formattedTime", formattedTime)
                                                    Toast.makeText(context, "Horário adicionado!", Toast.LENGTH_SHORT).show()
                                                }
                                                showTimeDialog.value = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4FC3F7)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Confirmar", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }

                item {
                    if (activeCategory == "futebol") {
                        SectionTitle("FUTEBOL E SELEÇÕES", Color(0xFF69F0AE))
                    
                    val showTeamDialog = remember { mutableStateOf(false) }
                    
                    Column(
                        modifier = PremiumGlassModifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Times e Seleções Monitorados",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "O painel de esportes exibirá o último e o próximo placar para os times abaixo.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            
                            if (configuredFootballTeams.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(themedSubtleBackground(), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nenhum time configurado.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    configuredFootballTeams.forEach { team ->
                                        Row(
                                            modifier = Modifier
                                                .background(themedSubtleBackground(), RoundedCornerShape(16.dp))
                                                .border(1.dp, themedSubtleBorder(), RoundedCornerShape(16.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = team, color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remover",
                                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        viewModel.removeFootballTeam(team)
                                                        Toast.makeText(context, "$team removido!", Toast.LENGTH_SHORT).show()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { showTeamDialog.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE).copy(alpha = 0.2f), contentColor = Color(0xFF69F0AE)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .border(1.dp, Color(0xFF69F0AE).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .bounceClick { showTeamDialog.value = true }
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Adicionar Time/Seleção", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (showTeamDialog.value) {
                        var tempTeamName by remember { mutableStateOf("") }

                        Dialog(onDismissRequest = { showTeamDialog.value = false }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(themedOverlayBackground())
                                    .border(1.dp, themedButtonBorder(), RoundedCornerShape(28.dp))
                                    .padding(24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    Text(
                                        text = "Adicionar Time",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    OutlinedTextField(
                                        value = tempTeamName,
                                        onValueChange = { tempTeamName = it },
                                        label = { Text("Nome do time ou seleção", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = themedTextFieldColors(),
                                        singleLine = true
                                    )

                                    val filteredTeams = availableFootballTeams.filter { it.contains(tempTeamName, ignoreCase = true) }.take(5)
                                    if (tempTeamName.isNotBlank() && filteredTeams.isNotEmpty() && tempTeamName != filteredTeams.firstOrNull()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(themedSubtleBackground())
                                        ) {
                                            filteredTeams.forEach { teamName ->
                                                Text(
                                                    text = teamName,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { tempTeamName = teamName }
                                                        .padding(vertical = 12.dp, horizontal = 16.dp)
                                                )
                                                if (teamName != filteredTeams.last()) {
                                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(themedDivider()))
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showTeamDialog.value = false },
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, themedButtonBorder()),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancelar", color = MaterialTheme.colorScheme.onBackground)
                                        }
                                        Button(
                                            onClick = {
                                                if (tempTeamName.isNotBlank() && !configuredFootballTeams.contains(tempTeamName.trim())) {
                                                    viewModel.addFootballTeam(tempTeamName.trim())
                                                    Toast.makeText(context, "${tempTeamName.trim()} adicionado!", Toast.LENGTH_SHORT).show()
                                                }
                                                showTeamDialog.value = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Confirmar", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }


                item {
                    if (activeCategory == "dados") {
                        SectionTitle("DADOS E BACKUP", Color(0xFFE57373))
                    Column(
                        modifier = PremiumGlassModifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // DB Stats
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatRow("Status do Banco:", "Conectado", PrimaryTeal)
                            StatRow("Tamanho Local:", getDatabaseSizeInKB(context), MaterialTheme.colorScheme.onBackground)
                            StatRow("Transações Registradas:", "${transactions.size}", MaterialTheme.colorScheme.onBackground)
                            StatRow("Eventos de Petz:", "${petEvents.size}", MaterialTheme.colorScheme.onBackground)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AFFFFFF)))
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.seedDemoData()
                                Toast.makeText(context, "Dados de demonstração carregados com sucesso!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).bounceClick {
                                viewModel.seedDemoData()
                                Toast.makeText(context, "Dados de demonstração carregados com sucesso!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importar Dados de Demonstração", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { exportDatabaseLauncher.launch("tessera_backup.db") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373), contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).bounceClick {
                                exportDatabaseLauncher.launch("tessera_backup.db")
                            }
                        ) {
                            Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar Nuvem / Armazenamento", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { importDatabaseLauncher.launch(arrayOf("*/*")) },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373).copy(alpha=0.4f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).bounceClick {
                                importDatabaseLauncher.launch(arrayOf("*/*"))
                            }
                        ) {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restaurar Arquivo de Backup", color = Color(0xFFE57373), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { showResetFinancesDialog = true },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373).copy(alpha=0.4f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp).bounceClick {
                                showResetFinancesDialog = true
                            }
                        ) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Zerar Finanças", color = Color(0xFFE57373), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    }
                }
                
                item {
                    if (activeCategory == null) {
                        Spacer(modifier = Modifier.height(40.dp))
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TESSERA", fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.3f), letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("v$appVersionName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.2f))
                    }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, color: Color) {
    Text(
        text = title,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
    )
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
