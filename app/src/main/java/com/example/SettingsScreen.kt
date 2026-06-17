package com.example

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
import androidx.compose.ui.draw.blur
import com.example.data.AppDatabase
import com.example.viewmodel.TesseraViewModel
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord as HCStepsRecord
import androidx.health.connect.client.records.WeightRecord as HCWeightRecord
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TesseraViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", android.content.Context.MODE_PRIVATE) }
    val userName = remember { sharedPrefs.getString("user_name", "Kenned") ?: "Kenned" }
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val appVersionName = packageInfo?.versionName ?: "1.0.2"
    
    var isBiometricEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_enabled", false)) }
    val backgroundUri by viewModel.homeBackgroundUri.collectAsState()
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

    val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

    val requestPermissions = rememberLauncherForActivityResult(requestPermissionActivityContract) { granted ->
        if (granted.containsAll(requiredReadPermissions) || granted.isNotEmpty()) {
            viewModel.updateHealthProfile(
                heightCm = healthProfile?.heightCm ?: 0.0,
                targetWeightKg = healthProfile?.targetWeightKg ?: 0.0,
                isHealthConnectEnabled = true
            )
            Toast.makeText(context, "Sincronização com Health Connect ativada!", Toast.LENGTH_SHORT).show()
        }
    }

    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val petEvents by viewModel.allPetEvents.collectAsState(initial = emptyList())

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bgFile = java.io.File(context.filesDir, "custom_home_background.jpg")
                    bgFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val localUri = Uri.fromFile(bgFile)
                    viewModel.updateHomeBackgroundUri(localUri.toString())
                    Toast.makeText(context, "Fundo atualizado com sucesso", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "Configurações", 
                            fontFamily = FontFamily.SansSerif, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 24.sp, 
                            color = Color.White,
                            letterSpacing = 1.sp
                        ) 
                    },
                    navigationIcon = { 
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.bounceClick { onBack() }
                        ) { 
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                "Voltar", 
                                tint = Color.White.copy(alpha = 0.8f)
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
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Olá, $userName!",
                                color = Color.White,
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

                item {
                    SectionTitle("PERSONALIZAÇÃO", SecondaryGold)
                    Column(
                        modifier = PremiumGlassModifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Plano de Fundo da Home", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        
                        // Current background preview card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = backgroundUri,
                                contentDescription = "Preview do plano de fundo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Gradient overlay for visual aesthetics and text contrast
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                        )
                                    )
                            )
                            Text(
                                text = "Fundo Atual da Home",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1.5f).height(48.dp).bounceClick {
                                    galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            ) {
                                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sua Galeria", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val defaultUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop"
                                    viewModel.updateHomeBackgroundUri(defaultUrl)
                                    Toast.makeText(context, "Fundo padrão restaurado!", Toast.LENGTH_SHORT).show()
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(48.dp).bounceClick {
                                    val defaultUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop"
                                    viewModel.updateHomeBackgroundUri(defaultUrl)
                                    Toast.makeText(context, "Fundo padrão restaurado!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Restaurar", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    SectionTitle("ESTILO DO GLASSMORPHISM", SecondaryGold)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Frosted Concept - Card superior largo
                        val isFrosted = currentGlassLevel == "Frosted"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = if (isFrosted) listOf(Color(0x52FFFFFF), Color(0x1AFFFFFF)) else listOf(Color(0x1CFFFFFF), Color(0x06FFFFFF))
                                    )
                                )
                                .border(
                                    width = if (isFrosted) 2.dp else 1.dp,
                                    brush = SolidColor(if (isFrosted) SecondaryGold else Color.White.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .clickable { viewModel.updateGlassmorphismLevel("Frosted") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Frosted",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Concept",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        // Clear e Blur - Cards inferiores lado a lado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Clear: iOS
                            val isClear = currentGlassLevel == "Clear"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = if (isClear) listOf(Color(0x24FFFFFF), Color(0x08FFFFFF)) else listOf(Color(0x0EFFFFFF), Color(0x02FFFFFF))
                                        )
                                    )
                                    .border(
                                        width = if (isClear) 2.dp else 1.dp,
                                        brush = SolidColor(if (isClear) SecondaryGold else Color.White.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                                    .clickable { viewModel.updateGlassmorphismLevel("Clear") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Clear",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "iOS",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            // Blur: One UI
                            val isBlur = currentGlassLevel == "Blur"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = if (isBlur) listOf(Color(0x3DFFFFFF), Color(0x12FFFFFF)) else listOf(Color(0x1DFFFFFF), Color(0x04FFFFFF))
                                        )
                                    )
                                    .border(
                                        width = if (isBlur) 2.dp else 1.dp,
                                        brush = SolidColor(if (isBlur) SecondaryGold else Color.White.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                                    .clickable { viewModel.updateGlassmorphismLevel("Blur") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Blur",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "One UI",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                item {
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
                                    Text("Desbloqueio Biométrico", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Exigir digital ou face ID ao abrir o app", fontSize = 12.sp, color = Color.White.copy(alpha=0.6f))
                                }
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { 
                                    isBiometricEnabled = it
                                    sharedPrefs.edit().putBoolean("biometric_enabled", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryTeal,
                                    uncheckedThumbColor = Color.White.copy(alpha=0.7f),
                                    uncheckedTrackColor = Color(0x33FFFFFF),
                                    uncheckedBorderColor = Color.Transparent
                                )
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
                                                        requestPermissions.launch(permissions)
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Saúde Connect indisponível no sistema", Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                requestPermissions.launch(permissions)
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
                                    Text("Google Health Connect", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        if (healthProfile?.isHealthConnectEnabled == true) "Sincronização ativa" else "Sincronizar passos, peso e sono",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha=0.6f)
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
                                                        requestPermissions.launch(permissions)
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Saúde Connect indisponível no sistema", Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                requestPermissions.launch(permissions)
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
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryTeal,
                                    uncheckedThumbColor = Color.White.copy(alpha=0.7f),
                                    uncheckedTrackColor = Color(0x33FFFFFF),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                item {
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
                            StatRow("Tamanho Local:", getDatabaseSizeInKB(context), Color.White)
                            StatRow("Transações Registradas:", "${transactions.size}", Color.White)
                            StatRow("Eventos de Petz:", "${petEvents.size}", Color.White)
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
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TESSERA", fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = Color.White.copy(alpha=0.3f), letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("v$appVersionName", fontSize = 11.sp, color = Color.White.copy(alpha=0.2f))
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
        Text(label, fontSize = 13.sp, color = Color.White.copy(alpha=0.6f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}


