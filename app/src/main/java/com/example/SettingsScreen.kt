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
    
    var isBiometricEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_enabled", false)) }
    var backgroundUri by remember {
        mutableStateOf(
            sharedPrefs.getString("home_background_uri", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop")
            ?: "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop"
        )
    }

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
                    backgroundUri = localUri.toString()
                    sharedPrefs.edit().putString("home_background_uri", localUri.toString()).apply()
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

    Scaffold(
        containerColor = Color(0xFF040505),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Configurações", 
                        fontFamily = FontFamily.Serif, 
                        fontWeight = FontWeight.SemiBold, 
                        fontSize = 28.sp, 
                        color = Color.White 
                    ) 
                },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Color.White.copy(alpha = 0.7f)) 
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
            item {
                SectionTitle("PERSONALIZAÇÃO", SecondaryGold)
                Column(
                    modifier = PremiumGlassModifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Plano de Fundo da Home", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                    .width(80.dp)
                                    .clickable {
                                        backgroundUri = url
                                        sharedPrefs.edit().putString("home_background_uri", url).apply()
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) SecondaryGold else Color(0x33FFFFFF),
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) SecondaryGold else Color.White.copy(alpha=0.6f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
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
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sua Galeria", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val defaultUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800&auto=format&fit=crop"
                                backgroundUri = defaultUrl
                                sharedPrefs.edit().putString("home_background_uri", defaultUrl).apply()
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Restaurar", color = Color.White, fontSize = 13.sp)
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
                        modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar Dados de Demonstração", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { exportDatabaseLauncher.launch("tessera_backup.db") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373), contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Nuvem / Armazenamento", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { importDatabaseLauncher.launch(arrayOf("*/*")) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE57373).copy(alpha=0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
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
                    Text("TESSERA", fontFamily = FontFamily.Serif, fontSize = 14.sp, color = Color.White.copy(alpha=0.3f), letterSpacing = 3.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("v1.0.0 Alpha", fontSize = 11.sp, color = Color.White.copy(alpha=0.2f))
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


