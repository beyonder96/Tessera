package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.SupabaseClientProvider
import com.example.ui.components.OuraCircularProgress
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApartmentScreen(onHomeClick: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    
    var progress by remember { mutableStateOf(sharedPrefs.getFloat("apartment_progress", 0.75f)) }
    var isPlaying by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    
    var showDateDialog by remember { mutableStateOf(false) }
    var expectedDate by remember { mutableStateOf(sharedPrefs.getString("apartment_date", "Dez 2026") ?: "Dez 2026") }
    var tempDate by remember { mutableStateOf(expectedDate) }
    
    val scrollState = rememberScrollState()

    fun syncWithCloud(newProgress: Float, newDate: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("action", "sync_apartment")
                    put("progress", (newProgress * 100).toInt())
                    put("expected_date", newDate)
                }.toString()
                SupabaseClientProvider.invokeFunction("telegram-bot", payload)
            } catch (e: Exception) {
                android.util.Log.w("ApartmentScreen", "Erro ao sincronizar com nuvem: ${e.message}")
            }
        }
    }

    fun refreshFromCloud(showToast: Boolean = true) {
        scope.launch {
            isSyncing = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val payload = JSONObject().apply { put("action", "get_apartment") }.toString()
                    SupabaseClientProvider.invokeFunction("telegram-bot", payload)
                }
                if (result.isSuccess) {
                    val jsonStr = result.getOrNull() ?: ""
                    val obj = JSONObject(jsonStr)
                    if (obj.optBoolean("ok")) {
                        val data = obj.optJSONObject("data")
                        if (data != null) {
                            val remoteProg = data.optInt("progress", (progress * 100).toInt()) / 100f
                            val remoteDate = data.optString("expected_date", expectedDate)
                            progress = remoteProg
                            expectedDate = remoteDate
                            tempDate = remoteDate
                            sharedPrefs.edit()
                                .putFloat("apartment_progress", remoteProg)
                                .putString("apartment_date", remoteDate)
                                .apply()
                            if (showToast) {
                                Toast.makeText(context, "Sincronizado com o Bot: ${(remoteProg * 100).toInt()}%!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else if (showToast) {
                    Toast.makeText(context, "Não foi possível conectar com o Bot", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (showToast) {
                    Toast.makeText(context, "Erro na sincronização: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSyncing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshFromCloud(showToast = false)
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying && progress < 1f) {
            delay(100)
            progress = (progress + 0.005f).coerceAtMost(1f)
            sharedPrefs.edit().putFloat("apartment_progress", progress).apply()
            if (progress >= 1f) isPlaying = false
        }
        if (!isPlaying) {
            syncWithCloud(progress, expectedDate)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MEU APÊ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Acompanhamento",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Icon(Icons.Outlined.Apartment, contentDescription = "Home", tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Thermal UI Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(PremiumGlassModifier)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OuraCircularProgress(
                        progress = progress,
                        progressColor = SecondaryGold,
                        modifier = Modifier.size(240.dp),
                        strokeWidth = 12f
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Apartment, contentDescription = null, tint = SecondaryGold, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "CONCLUÍDO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botão de Atualizar / Sincronizar com a Nuvem e Telegram Bot
                        IconButton(
                            onClick = { refreshFromCloud(showToast = true) },
                            enabled = !isSyncing,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = SecondaryGold,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = "Atualizar status da nuvem",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }

                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isPlaying) SecondaryGold.copy(alpha = 0.2f) else PrimaryTeal.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = if (isPlaying) SecondaryGold else PrimaryTeal,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Slider(
                        value = progress,
                        onValueChange = { newProgress ->
                            progress = newProgress
                            sharedPrefs.edit().putFloat("apartment_progress", newProgress).apply()
                        },
                        onValueChangeFinished = {
                            syncWithCloud(progress, expectedDate)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = SecondaryGold,
                            activeTrackColor = SecondaryGold,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Expected Date Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                    .clickable { showDateDialog = true }
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PREVISÃO DE ENTREGA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = expectedDate,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar Previsão",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    if (showDateDialog) {
        ModalBottomSheet(
            onDismissRequest = { showDateDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "EDITAR PREVISÃO",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryGold,
                        letterSpacing = 1.5.sp
                    )

                    OutlinedTextField(
                        value = tempDate,
                        onValueChange = { tempDate = it },
                        label = { Text("Mês e Ano (ex: Dez 2026)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDateDialog = false }) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                expectedDate = tempDate
                                sharedPrefs.edit().putString("apartment_date", tempDate).apply()
                                syncWithCloud(progress, tempDate)
                                showDateDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Salvar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
