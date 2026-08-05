package com.example
import androidx.compose.material3.MaterialTheme

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
import androidx.compose.ui.geometry.Offset
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
            // Utilizamos Jsoup com um User-Agent de navegador real para evitar bloqueios
            val doc = org.jsoup.Jsoup.connect(fullUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                .timeout(8000)
                .followRedirects(true)
                .get()

            // Tenta pegar primeiro o og:image
            var imgUrl = doc.select("meta[property=og:image]").attr("content")
            
            // Fallback 1: twitter:image
            if (imgUrl.isBlank()) {
                imgUrl = doc.select("meta[name=twitter:image]").attr("content")
            }
            // Fallback 2: link rel="image_src"
            if (imgUrl.isBlank()) {
                imgUrl = doc.select("link[rel=image_src]").attr("href")
            }
            // Fallback 3: Amazon main images (landingImage / imgBlkFront)
            if (imgUrl.isBlank()) {
                imgUrl = doc.select("img#landingImage").attr("src")
            }
            if (imgUrl.isBlank()) {
                imgUrl = doc.select("img#imgBlkFront").attr("src")
            }
            // Fallback 4: Mercado Livre principal (ui-pdp-image)
            if (imgUrl.isBlank()) {
                imgUrl = doc.select("img.ui-pdp-image.ui-pdp-gallery__figure__image").attr("src")
            }
            // Fallback 5: A primeira imagem normal da página
            if (imgUrl.isBlank()) {
                val firstImg = doc.select("img").firstOrNull { it.hasAttr("src") && !it.attr("src").contains("data:image") && !it.attr("src").contains("logo") }
                imgUrl = firstImg?.attr("src") ?: ""
            }

            if (imgUrl.isNotBlank()) {
                val url = URL(fullUrl)
                if (imgUrl.startsWith("//")) {
                    "https:$imgUrl"
                } else if (imgUrl.startsWith("/")) {
                    "${url.protocol}://${url.host}$imgUrl"
                } else imgUrl
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
            contentPadding = PaddingValues(top = 130.dp, bottom = 120.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DESEJOS ATIVOS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = com.example.ui.theme.SecondaryGold
                    )
                    Text(
                        text = "${activeGoals.size} ITENS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF71717A)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
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
                        onBuy = { viewModel.buyPurchaseGoal(goal, "External") },
                        onDelete = { viewModel.deletePurchaseGoal(goal) }
                    )
                }
            }
        }
        
        // Sticky Header / Search Bar (Pílula de vidro com profundidade)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF070909).copy(alpha = 0.95f), Color.Transparent)
                    )
                )
                .padding(top = 48.dp, bottom = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x800B0E14))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Pesquisar desejos...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 14.sp) },
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
                        Icon(Icons.Default.Close, contentDescription = "Limpar", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    }
                } else {
                    Icon(Icons.Outlined.Mic, contentDescription = "Voz", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                }
            }
        }
        
        // Floating Action Button
        FloatingActionButton(
            onClick = { showAddGoalDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 24.dp),
            containerColor = com.example.ui.theme.SecondaryGold,
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
                    colorHex = "#D4B36A",
                    priorityOrder = 1,
                    priorityClassification = "Alta",
                    buyUrl = buyUrl,
                    category = "Eletrônicos"
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
fun AnimatedPurchaseGoalCard(goal: PurchaseGoal, onEdit: () -> Unit, onBuy: () -> Unit, onDelete: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    
    // Gradient Obsidian Glow (Thermal UI background)
    val cardGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFF101012),
            0.55f to Color(0xFF0F0F11),
            0.80f to Color(0xFF1A0A06),
            1.0f to Color(0xFF5E1603)
        )
    )
    
    val shape = RoundedCornerShape(32.dp)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardGradient)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
    ) {
        // Bottom warm inner glow & rim edge
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF5500).copy(alpha = 0.35f),
                        Color(0xFF882200).copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height * 1.05f),
                    radius = size.width * 0.75f
                )
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFFAA00).copy(alpha = 0.75f),
                        Color.Transparent
                    )
                ),
                start = Offset(size.width * 0.2f, size.height - 1.5f),
                end = Offset(size.width * 0.8f, size.height - 1.5f),
                strokeWidth = 3f
            )
        }

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
                    Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF18181B), Color(0xFF09090B)))))
                }
                
                // Gradient overlay fusing image into dark card background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.5f to Color(0x33000000),
                                    0.85f to Color(0xCC0F0F11),
                                    1.0f to Color(0xFF0F0F11)
                                )
                            )
                        )
                )
                
                // Floating Action Pill (Top Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onEdit() }
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(14.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "Deletar",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onDelete() }
                        )
                    }
                }
            }
            
            // Product Information
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                val categoryText = goal.category.ifBlank { "ELETRÔNICOS" }
                Text(
                    text = categoryText.uppercase(),
                    color = com.example.ui.theme.SecondaryGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = goal.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Formatted Price Tag
                val formattedVal = String.format(Locale("pt", "BR"), "%,.2f", goal.targetValue)
                val parts = formattedVal.split(",")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "R$",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 3.dp, end = 4.dp)
                    )
                    Text(
                        text = parts[0],
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (parts.size > 1) {
                        Text(
                            text = ",${parts[1]}",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Bottom Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cart Glass Button (Left)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                        .clickable { if (goal.buyUrl.isNotBlank()) uriHandler.openUri(goal.buyUrl) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.ShoppingCart,
                        contentDescription = "Comprar",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Translucent "Já comprei" Main CTA Button (Right)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                        .clickable { onBuy() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = com.example.ui.theme.SecondaryGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Já comprei",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (goal == null) "Novo Desejo" else "Editar Desejo",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
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
                label = { Text("Link da Compra", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
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
                label = { Text("Nome do Produto", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
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
                label = { Text("Valor Estimado (R$)", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF71D7CD), unfocusedBorderColor = Color(0xFF3D4947)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = Color(0xFF81928F))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val cleanTarget = target.replace(Regex("[^0-9,]"), "").replace(",", ".")
                        val targetVal = cleanTarget.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && targetVal > 0) {
                            onSave(title, targetVal, buyUrl, imgUrl)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                ) {
                    Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
