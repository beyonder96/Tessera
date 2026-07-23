package com.example

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.PurchaseGoal
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.theme.PrimaryTeal
import com.example.viewmodel.TesseraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

suspend fun fetchOgImageFromUrl(urlStr: String): String? {
    if (urlStr.isBlank()) return null
    val fullUrl = if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
        "https://$urlStr"
    } else urlStr

    return withContext(Dispatchers.IO) {
        try {
            val url = URL(fullUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                val ogPattern = Regex("""<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                val ogPatternAlt = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']""", RegexOption.IGNORE_CASE)
                val match = ogPattern.find(html) ?: ogPatternAlt.find(html)
                
                val imgUrl = match?.groupValues?.get(1)
                if (!imgUrl.isNullOrBlank()) {
                    if (imgUrl.startsWith("//")) {
                        "https:$imgUrl"
                    } else if (imgUrl.startsWith("/")) {
                        "${url.protocol}://${url.host}$imgUrl"
                    } else imgUrl
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishesScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val purchaseGoals by viewModel.allPurchaseGoals.collectAsStateWithLifecycle()
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<PurchaseGoal?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val activeGoals = remember(purchaseGoals, searchQuery) {
        purchaseGoals.filter { !it.isBought && it.title.contains(searchQuery, ignoreCase = true) }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF070909))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 130.dp, bottom = 120.dp, start = 24.dp, end = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = "DESEJOS ATIVOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF71D7CD)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (activeGoals.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nenhum desejo encontrado.\nToque no '+' para planejar algo!",
                            color = Color(0xFF5E6D6A),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(activeGoals, key = { it.id }) { goal ->
                    AnimatedPurchaseGoalCard(
                        goal = goal,
                        onEdit = { goalToEdit = goal },
                        onBuy = { viewModel.buyPurchaseGoal(goal, "External") }
                    )
                }
            }
        }
        
        // Sticky Header / Search Bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF070909).copy(alpha = 0.95f), Color.Transparent)
                    )
                )
                .padding(top = 48.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .then(PremiumGlassModifier)
                    .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(32.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Pesquisar desejos...", color = Color.White.copy(alpha = 0.5f)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar", tint = Color.White)
                    }
                }
            }
        }
        
        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddGoalDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 24.dp),
            containerColor = Color(0xFF71D7CD),
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar Desejo")
        }
    }
    
    if (showAddGoalDialog) {
        WishesDialog(
            goal = null,
            onDismiss = { showAddGoalDialog = false },
            onSave = { title, target, buyUrl, imgUrl ->
                viewModel.addPurchaseGoal(
                    title = title,
                    target = target,
                    current = 0.0,
                    imageUrl = imgUrl,
                    colorHex = "#71D7CD",
                    priorityOrder = 1,
                    priorityClassification = "Alta",
                    buyUrl = buyUrl,
                    category = "Geral"
                )
                showAddGoalDialog = false
            }
        )
    }
    
    if (goalToEdit != null) {
        WishesDialog(
            goal = goalToEdit,
            onDismiss = { goalToEdit = null },
            onSave = { title, target, buyUrl, imgUrl ->
                val updated = goalToEdit!!.copy(
                    title = title,
                    targetValue = target,
                    buyUrl = buyUrl,
                    imageUrl = imgUrl
                )
                viewModel.updatePurchaseGoal(updated)
                goalToEdit = null
            }
        )
    }
}

@Composable
fun AnimatedPurchaseGoalCard(goal: PurchaseGoal, onEdit: () -> Unit, onBuy: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ScaleAnimation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (goal.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = goal.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF2C3E50), Color(0xFF000000)))))
                }
                
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF070909))))
                )
                
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Olha esse produto que quero comprar: ${goal.title}\n\n${goal.buyUrl}")
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartilhar Desejo"))
                        },
                        modifier = Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Compartilhar", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = goal.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %,.2f", goal.targetValue),
                        color = Color(0xFF71D7CD),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (goal.buyUrl.isNotBlank()) uriHandler.openUri(goal.buyUrl) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Comprar", color = Color.White, fontSize = 14.sp)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = onBuy,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Marcar Comprado", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishesDialog(
    goal: PurchaseGoal?,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf(goal?.title ?: "") }
    var target by remember { mutableStateOf(if (goal != null) String.format(Locale.US, "%.2f", goal.targetValue) else "") }
    var buyUrl by remember { mutableStateOf(goal?.buyUrl ?: "") }
    var imgUrl by remember { mutableStateOf(goal?.imageUrl ?: "") }
    
    var isFetchingImage by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Scrape image when URL changes
    LaunchedEffect(buyUrl) {
        if (buyUrl.isNotBlank() && imgUrl.isBlank() && (buyUrl.startsWith("http"))) {
            isFetchingImage = true
            val fetchedImg = fetchOgImageFromUrl(buyUrl)
            if (fetchedImg != null) {
                imgUrl = fetchedImg
            }
            isFetchingImage = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131817),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (goal == null) "Novo Desejo" else "Editar Desejo",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (imgUrl.isNotBlank()) {
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (isFetchingImage) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF71D7CD))
                    }
                }
                
                OutlinedTextField(
                    value = buyUrl,
                    onValueChange = { buyUrl = it },
                    label = { Text("Link da Compra", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0xFF3D4947)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome do Produto", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0xFF3D4947)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Valor Estimado (R$)", color = Color.White.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0xFF3D4947)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetVal = target.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && targetVal > 0) {
                        onSave(title, targetVal, buyUrl, imgUrl)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
            ) {
                Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF81928F))
            }
        }
    )
}
