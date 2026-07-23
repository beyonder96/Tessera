package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.BankAccount
import com.example.data.CreditCard
import com.example.data.PurchaseGoal
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UnsplashPhoto(val url: String, val avgColor: String)

fun getFallbackImages(query: String): List<UnsplashPhoto> {
    val q = query.lowercase().trim()
    val notebookImages = listOf(
        "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1496181130204-7552cc14ac1a?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1531297484001-80022131f5a1?q=80&w=400&auto=format&fit=crop"
    )
    val phoneImages = listOf(
        "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1598327105666-5b89351aff97?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1580910051074-3eb694886505?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1616348436168-de43ad0db179?q=80&w=400&auto=format&fit=crop"
    )
    val travelImages = listOf(
        "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?q=80&w=400&auto=format&fit=crop"
    )
    val carImages = listOf(
        "https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1503376780353-7e6692767b70?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1552519507-da3b142c6e3d?q=80&w=400&auto=format&fit=crop"
    )
    val houseImages = listOf(
        "https://images.unsplash.com/photo-1580587771525-78b9dba3b914?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?q=80&w=400&auto=format&fit=crop"
    )
    val generalImages = listOf(
        "https://images.unsplash.com/photo-1523275335684-37898b6baf30?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1542291026-7eec264c27ff?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1572635196237-14b3f281503f?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?q=80&w=400&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?q=80&w=400&auto=format&fit=crop"
    )

    val list = when {
        q.contains("note") || q.contains("comput") || q.contains("macbook") || q.contains("pc") || q.contains("laptop") -> notebookImages
        q.contains("celular") || q.contains("iphone") || q.contains("phone") || q.contains("smart") -> phoneImages
        q.contains("viaj") || q.contains("praia") || q.contains("trip") || q.contains("jap") || q.contains("kyoto") || q.contains("paris") -> travelImages
        q.contains("carro") || q.contains("veic") || q.contains("moto") || q.contains("car") -> carImages
        q.contains("casa") || q.contains("ape") || q.contains("apart") || q.contains("home") || q.contains("house") -> houseImages
        else -> generalImages
    }

    return list.map { UnsplashPhoto(url = it, avgColor = "#71D7CD") }
}

fun searchUnsplashImagesApi(query: String): List<UnsplashPhoto> {
    val apiKey = com.example.BuildConfig.WISHES_API_KEY
    if (query.isBlank()) {
        return getFallbackImages("")
    }

    try {
        val urlStr = "https://api.unsplash.com/search/photos?query=${java.net.URLEncoder.encode(query, "UTF-8")}&per_page=30&client_id=$apiKey"
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept-Version", "v1")
        if (apiKey.isNotBlank() && apiKey != "your_pexels_api_key_here") {
            conn.setRequestProperty("Authorization", "Client-ID $apiKey")
        }
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        val responseCode = conn.responseCode
        if (responseCode == 200) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val resultsArray = json.optJSONArray("results") ?: json.optJSONArray("photos")
            val results = mutableListOf<UnsplashPhoto>()
            if (resultsArray != null) {
                for (i in 0 until resultsArray.length()) {
                    val photoObj = resultsArray.getJSONObject(i)
                    val urlsObj = photoObj.optJSONObject("urls")
                    val regularUrl = urlsObj?.optString("regular") ?: urlsObj?.optString("small") ?: photoObj.optString("url", "")
                    val avgColor = photoObj.optString("color", "#71D7CD")
                    if (regularUrl.isNotBlank()) {
                        results.add(UnsplashPhoto(url = regularUrl, avgColor = avgColor))
                    }
                }
            }
            if (results.isNotEmpty()) {
                return results
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return getFallbackImages(query)
}

fun fetchOgImageFromUrl(urlStr: String): String? {
    if (urlStr.isBlank()) return null
    val fullUrl = if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
        "https://$urlStr"
    } else urlStr

    return try {
        val url = URL(fullUrl)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        conn.connectTimeout = 8000
        conn.readTimeout = 8000

        if (conn.responseCode == 200) {
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            
            val ogPattern = Regex("""<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val ogPatternAlt = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']""", RegexOption.IGNORE_CASE)
            val twitterPattern = Regex("""<meta[^>]+name=["']twitter:image["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val itempropPattern = Regex("""<meta[^>]+itemprop=["']image["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

            val match = ogPattern.find(html) ?: ogPatternAlt.find(html) ?: twitterPattern.find(html) ?: itempropPattern.find(html)
            
            val imgUrl = match?.groupValues?.get(1)
            if (!imgUrl.isNullOrBlank()) {
                if (imgUrl.startsWith("//")) {
                    "https:$imgUrl"
                } else if (imgUrl.startsWith("/")) {
                    val host = "${url.protocol}://${url.host}"
                    "$host$imgUrl"
                } else imgUrl
            } else null
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnsplashImageSearchDialog(
    onDismiss: () -> Unit,
    onImageSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<UnsplashPhoto>>(emptyList()) }
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        photos = getFallbackImages("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131817),
        title = {
            Column {
                Text(
                    text = "Consultar Imagem no Unsplash",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pesquise fotos de alta resolução sem sair do app",
                    color = Color(0xFF81928F),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Digite o termo de busca (ex: Carro, Casa)...", color = Color(0xFF5E6D6A)) },
                    trailingIcon = {
                        IconButton(onClick = {
                            isLoading = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val results = searchUnsplashImagesApi(query)
                                photos = results
                                isLoading = false
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = Color.White)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF71D7CD),
                        unfocusedBorderColor = Color(0xFF3D4947)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color(0xFF71D7CD))
                    } else if (photos.isEmpty()) {
                        Text("Nenhuma imagem encontrada.", color = Color(0xFF5E6D6A), fontSize = 14.sp)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(photos) { photo ->
                                val isSelected = selectedPhotoUrl == photo.url
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF71D7CD) else Color(0x1AFFFFFF),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedPhotoUrl = photo.url }
                                ) {
                                    AsyncImage(
                                        model = photo.url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(20.dp)
                                                .background(Color(0xFF71D7CD), CircleShape)
                                                .border(1.dp, Color.Black, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selecionado",
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
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedPhotoUrl?.let {
                        onImageSelected(it)
                        onDismiss()
                    }
                },
                enabled = selectedPhotoUrl != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF71D7CD),
                    disabledContainerColor = Color(0x3371D7CD)
                )
            ) {
                Text("Selecionar Esta Imagem", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF81928F))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishesScreen(onHomeClick: () -> Unit, viewModel: TesseraViewModel) {
    val purchaseGoals by viewModel.allPurchaseGoals.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.allBankAccounts.collectAsStateWithLifecycle()
    val creditCards by viewModel.allCreditCards.collectAsStateWithLifecycle()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<PurchaseGoal?>(null) }
    var isCardView by remember { mutableStateOf(true) }

    val activeGoals = remember(purchaseGoals) { purchaseGoals.filter { !it.isBought } }
    val boughtGoals = remember(purchaseGoals) { purchaseGoals.filter { it.isBought } }

    val totalOpenWishes = activeGoals.size
    val categoryCounts = remember(activeGoals) {
        activeGoals.groupBy { it.category }.mapValues { it.value.size }
    }

    val scrollState = rememberLazyListState()
    val isCompact by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 100
        }
    }

    val normalAlpha by animateFloatAsState(targetValue = if (isCompact) 0f else 1f, animationSpec = tween(250), label = "normalAlpha")
    val compactAlpha by animateFloatAsState(targetValue = if (isCompact) 1f else 0f, animationSpec = tween(250), label = "compactAlpha")
    val accentColor = Color(0xFF71D7CD)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFF070909),
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {}
        ) { innerPadding ->
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item { Spacer(modifier = Modifier.height(72.dp)) }

            // Painel de Métricas do Topo
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .then(PremiumGlassModifier)
                        .border(1.dp, Color(0x15FFFFFF), RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DESEJOS EM ABERTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFF81928F)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$totalOpenWishes",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Serif,
                            color = Color.White
                        )

                        if (categoryCounts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0x15FFFFFF), thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                            ) {
                                categoryCounts.forEach { (cat, count) ->
                                    val color = when (cat) {
                                        "Eletrônicos" -> Color(0xFF71D7CD)
                                        "Moda" -> Color(0xFFD7B4F3)
                                        "Casa" -> Color(0xFF34C759)
                                        "Viagem" -> Color(0xFFF9A826)
                                        "Lazer" -> Color(0xFF007AFF)
                                        else -> Color(0xFFBDC9C6)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(color.copy(alpha = 0.1f))
                                            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(color, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "$cat: $count",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Alternância de Visualização
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visualização",
                        fontSize = 13.sp,
                        color = Color(0xFF81928F),
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x10FFFFFF))
                            .padding(3.dp)
                    ) {
                        IconButton(
                            onClick = { isCardView = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isCardView) Color(0x15FFFFFF) else Color.Transparent, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Modo Cards",
                                tint = if (isCardView) Color(0xFF71D7CD) else Color(0xFF81928F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { isCardView = false },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (!isCardView) Color(0x15FFFFFF) else Color.Transparent, RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Modo Lista",
                                tint = if (!isCardView) Color(0xFF71D7CD) else Color(0xFF81928F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Seção: Ativos
            item {
                SectionHeaderWishes("DESEJOS ATIVOS", Icons.Outlined.StarBorder, Color(0xFFF9A826))
            }

            if (activeGoals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum desejo ativo cadastrado.\nToque no '+' para planejar sua próxima conquista!",
                            color = Color(0xFF5E6D6A),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                if (isCardView) {
                    items(activeGoals, key = { "active_${it.id}" }) { goal ->
                        PurchaseGoalPremiumCard(
                            goal = goal,
                            bankAccounts = bankAccounts,
                            creditCards = creditCards,
                            onAddFunds = { amount, origin ->
                                viewModel.addFundsToPurchaseGoal(goal, amount, origin)
                            },
                            onBuy = { origin ->
                                viewModel.buyPurchaseGoal(goal, origin)
                            },
                            onEditClick = { goalToEdit = goal }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                } else {
                    items(activeGoals, key = { "active_list_${it.id}" }) { goal ->
                        PurchaseGoalListRow(
                            goal = goal,
                            bankAccounts = bankAccounts,
                            creditCards = creditCards,
                            onAddFunds = { amount, origin ->
                                viewModel.addFundsToPurchaseGoal(goal, amount, origin)
                            },
                            onBuy = { origin ->
                                viewModel.buyPurchaseGoal(goal, origin)
                            },
                            onEditClick = { goalToEdit = goal }
                        )
                    }
                }
            }

            // Seção: Realizados
            if (boughtGoals.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item {
                    SectionHeaderWishes("DESEJOS REALIZADOS", Icons.Outlined.CheckCircle, Color(0xFF71D7CD))
                }

                items(boughtGoals, key = { "bought_${it.id}" }) { goal ->
                    BoughtGoalCard(goal = goal, onDelete = { viewModel.deletePurchaseGoal(goal) })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        // Floating overlay top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // 1. Barra Normal
            if (normalAlpha > 0.05f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = normalAlpha
                            scaleX = 0.92f + (normalAlpha * 0.08f)
                            scaleY = 0.92f + (normalAlpha * 0.08f)
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DESEJOS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showAddGoalDialog = true }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Adicionar Desejo",
                                tint = Color(0xFFBDC9C6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Barra Compacta
            if (compactAlpha > 0.05f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = compactAlpha
                            translationY = (1f - compactAlpha) * (-20f)
                        }
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                    val shimmerOffset by infiniteTransition.animateFloat(
                        initialValue = -400f,
                        targetValue = 400f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "shimmerOffset"
                    )
                    
                    val nameGlowBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            accentColor,
                            Color.White,
                            accentColor,
                            Color.White
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 150f, 150f)
                    )
                    
                    Text(
                        text = "DESEJOS",
                        style = TextStyle(
                            brush = nameGlowBrush,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Serif
                        )
                    )
                }
            }
        }
        } // closes Box from line 417
    } // closes Scaffold

    if (showAddGoalDialog) {
        AddPurchaseGoalDialogWishes(
            onDismiss = { showAddGoalDialog = false },
            onSave = { title, target, url, buyUrl, category, color, priorityOrder, priorityClassification ->
                viewModel.addPurchaseGoal(
                    title = title,
                    target = target,
                    current = 0.0,
                    imageUrl = url,
                    colorHex = color,
                    priorityOrder = priorityOrder,
                    priorityClassification = priorityClassification,
                    buyUrl = buyUrl,
                    category = category
                )
                showAddGoalDialog = false
            }
        )
    }

    if (goalToEdit != null) {
        EditPurchaseGoalDialogWishes(
            goal = goalToEdit!!,
            onDismiss = { goalToEdit = null },
            onSave = { updatedGoal ->
                viewModel.updatePurchaseGoal(updatedGoal)
                goalToEdit = null
            },
            onDelete = { goalToDelete ->
                viewModel.deletePurchaseGoal(goalToDelete)
                goalToEdit = null
            }
        )
    }
}

@Composable
fun SectionHeaderWishes(title: String, icon: ImageVector, tintColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = tintColor
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PurchaseGoalPremiumCard(
    goal: PurchaseGoal,
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    onAddFunds: (Double, String) -> Unit,
    onBuy: (String) -> Unit,
    onEditClick: () -> Unit
) {
    val progress = (goal.currentValue / goal.targetValue).coerceIn(0.0, 1.0)
    val color = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Color(0xFFF9A826) }

    var showAddFundsSection by remember { mutableStateOf(false) }
    var showBuySection by remember { mutableStateOf(false) }
    var fundsAmount by remember { mutableStateOf("") }

    val origins = remember(bankAccounts, creditCards) {
        bankAccounts.map { it.name } + creditCards.map { it.name }
    }
    var selectedOrigin by remember { mutableStateOf(origins.firstOrNull() ?: "") }
    var showOriginDropdown by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(PremiumGlassModifier)
            .border(1.dp, Color(0x2BFFFFFF), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box {
            // Imagem do Produto
            AsyncImage(
                model = goal.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            // Gradiente Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF070909))))
            )

            // Editar
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(36.dp)
                    .background(Color(0x66000000), CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Editar Meta",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Badge Porcentagem
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x66000000))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "${(progress * 100).roundToInt()}%",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = goal.title,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Progresso e Detalhes
        Column(modifier = Modifier.padding(20.dp)) {
            val formattedCurrent = String.format(Locale("pt", "BR"), "R$ %,.2f", goal.currentValue)
            val formattedTarget = String.format(Locale("pt", "BR"), "R$ %,.2f", goal.targetValue)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(formattedCurrent, fontSize = 22.sp, color = Color(0xFFDFE3E2), fontWeight = FontWeight.Bold)
                Text("de $formattedTarget", fontSize = 14.sp, color = Color(0xFF81928F))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de Progresso Animada
            val animatedProgress by animateFloatAsState(
                targetValue = progress.toFloat(),
                animationSpec = tween(1500, easing = FastOutSlowInEasing),
                label = "WishProgress"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x1AFFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(color.copy(alpha = 0.7f), color)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Categoria & Prioridade (Sem Prazos)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFF81928F),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = goal.category,
                        fontSize = 12.sp,
                        color = Color(0xFF81928F)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeColor = when (goal.priorityClassification) {
                        "Urgente" -> Color(0xFFEF4444)
                        "Moderado" -> Color(0xFFF9A826)
                        else -> Color(0xFF71D7CD)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .border(0.5.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = goal.priorityClassification.uppercase(),
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fluxo de Integração Financeira: Aporte de Saldo
            AnimatedContent(
                targetState = showAddFundsSection to showBuySection,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "FinancialWishesIntegration"
            ) { (isAddingFunds, isBuying) ->
                when {
                    isAddingFunds -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
                                .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text("Aportar Financeiro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = fundsAmount,
                                    onValueChange = { fundsAmount = it },
                                    label = { Text("Valor (R$)", color = Color(0xFF81928F)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1.3f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = color,
                                        unfocusedBorderColor = Color(0xFF3D4947),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Origem de débito
                                Box(modifier = Modifier.weight(1.7f)) {
                                    OutlinedTextField(
                                        value = selectedOrigin,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Debitar de", color = Color(0xFF81928F)) },
                                        trailingIcon = {
                                            IconButton(onClick = { showOriginDropdown = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF3D4947),
                                            unfocusedBorderColor = Color(0xFF3D4947),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    
                                    DropdownMenu(
                                        expanded = showOriginDropdown,
                                        onDismissRequest = { showOriginDropdown = false },
                                        modifier = Modifier.background(Color(0xFF131817))
                                    ) {
                                        origins.forEach { origin ->
                                            DropdownMenuItem(
                                                text = { Text(origin, color = Color.White) },
                                                onClick = {
                                                    selectedOrigin = origin
                                                    showOriginDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showAddFundsSection = false }) {
                                    Text("Cancelar", color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val added = fundsAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
                                        if (added > 0.0 && selectedOrigin.isNotEmpty()) {
                                            onAddFunds(added, selectedOrigin)
                                            showAddFundsSection = false
                                            fundsAmount = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = color)
                                ) {
                                    Text("Confirmar Aporte", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    isBuying -> {
                        val remainingValue = (goal.targetValue - goal.currentValue).coerceAtLeast(0.0)
                        val formattedRemaining = String.format(Locale("pt", "BR"), "R$ %,.2f", remainingValue)
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
                                .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text("Realizar Compra", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            if (remainingValue > 0.0) {
                                Text(
                                    "Falta $formattedRemaining para atingir o valor alvo. De qual conta deseja pagar este saldo restante?",
                                    color = Color(0xFFBDC9C6),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedOrigin,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Pagar saldo restante com", color = Color(0xFF81928F)) },
                                        trailingIcon = {
                                            IconButton(onClick = { showOriginDropdown = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF3D4947),
                                            unfocusedBorderColor = Color(0xFF3D4947),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    
                                    DropdownMenu(
                                        expanded = showOriginDropdown,
                                        onDismissRequest = { showOriginDropdown = false },
                                        modifier = Modifier.background(Color(0xFF131817))
                                    ) {
                                        origins.forEach { origin ->
                                            DropdownMenuItem(
                                                text = { Text(origin, color = Color.White) },
                                                onClick = {
                                                    selectedOrigin = origin
                                                    showOriginDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "Desejo totalmente financiado! Confirme a compra para marcar como realizado.",
                                    color = Color(0xFF71D7CD),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showBuySection = false }) {
                                    Text("Cancelar", color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onBuy(selectedOrigin)
                                        showBuySection = false
                                        if (goal.buyUrl.isNotBlank()) {
                                            try {
                                                val fullUrl = if (!goal.buyUrl.startsWith("http://") && !goal.buyUrl.startsWith("https://")) {
                                                    "https://${goal.buyUrl}"
                                                } else goal.buyUrl
                                                uriHandler.openUri(fullUrl)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                                ) {
                                    Text("Efetivar Compra 🛍️", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Botão Aportar
                            OutlinedButton(
                                onClick = { showAddFundsSection = true },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, color),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
                            ) {
                                Icon(Icons.Outlined.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aportar", fontWeight = FontWeight.Bold)
                            }

                            // Botão Comprar / Concluir (Abre URL externa e abre aba de débito financeiro)
                            Button(
                                onClick = {
                                    try {
                                        val finalUrl = if (goal.buyUrl.startsWith("http://") || goal.buyUrl.startsWith("https://")) {
                                            goal.buyUrl
                                        } else {
                                            "https://$goal.buyUrl"
                                        }
                                        uriHandler.openUri(finalUrl)
                                    } catch (e: Exception) {
                                        uriHandler.openUri("https://www.google.com/search?q=${goal.title}")
                                    }
                                    showBuySection = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (progress >= 1.0) Color(0xFF71D7CD) else Color(0x33FFFFFF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = if (progress >= 1.0) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (progress >= 1.0) "Comprar!" else "Comprar",
                                    color = if (progress >= 1.0) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PurchaseGoalListRow(
    goal: PurchaseGoal,
    bankAccounts: List<BankAccount>,
    creditCards: List<CreditCard>,
    onAddFunds: (Double, String) -> Unit,
    onBuy: (String) -> Unit,
    onEditClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val progress = (goal.currentValue / goal.targetValue).coerceIn(0.0, 1.0)

    val priorityColor = when (goal.priorityClassification) {
        "Urgente" -> Color(0xFFEF4444)
        "Moderado" -> Color(0xFFF9A826)
        else -> Color(0xFF71D7CD)
    }

    val colorHex = try { Color(android.graphics.Color.parseColor(goal.colorHex)) } catch (e: Exception) { Color(0xFFF9A826) }

    var showAddFundsSection by remember { mutableStateOf(false) }
    var showBuySection by remember { mutableStateOf(false) }
    var fundsAmount by remember { mutableStateOf("") }

    val origins = remember(bankAccounts, creditCards) {
        bankAccounts.map { it.name } + creditCards.map { it.name }
    }
    var selectedOrigin by remember { mutableStateOf(origins.firstOrNull() ?: "") }
    var showOriginDropdown by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x08FFFFFF))
            .border(0.5.dp, Color(0x10FFFFFF), RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = goal.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = priorityColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Colapsar" else "Expandir",
                tint = Color(0xFF81928F)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = goal.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Categoria: ${goal.category}",
                                fontSize = 12.sp,
                                color = Color(0xFF81928F)
                            )
                            Text(
                                text = "${(progress * 100).roundToInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorHex
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val formattedCurrent = String.format(Locale("pt", "BR"), "R$ %,.2f", goal.currentValue)
                        val formattedTarget = String.format(Locale("pt", "BR"), "R$ %,.2f", goal.targetValue)
                        Text(
                            text = "$formattedCurrent / $formattedTarget",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x10FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.toFloat())
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colorHex)
                    )
                }

                AnimatedContent(
                    targetState = showAddFundsSection to showBuySection,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ListFinancialWishes"
                ) { (isAdding, isBuying) ->
                    when {
                        isAdding -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x08FFFFFF), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text("Aportar Financeiro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = fundsAmount,
                                        onValueChange = { fundsAmount = it },
                                        label = { Text("Valor (R$)", fontSize = 12.sp, color = Color(0xFF81928F)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1.2f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = colorHex
                                        ),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1.8f)) {
                                        OutlinedTextField(
                                            value = selectedOrigin,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Debitar de", fontSize = 12.sp, color = Color(0xFF81928F)) },
                                            trailingIcon = {
                                                IconButton(onClick = { showOriginDropdown = true }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        DropdownMenu(
                                            expanded = showOriginDropdown,
                                            onDismissRequest = { showOriginDropdown = false },
                                            modifier = Modifier.background(Color(0xFF131817))
                                        ) {
                                            origins.forEach { origin ->
                                                DropdownMenuItem(
                                                    text = { Text(origin, color = Color.White) },
                                                    onClick = {
                                                        selectedOrigin = origin
                                                        showOriginDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { showAddFundsSection = false }) {
                                        Text("Cancelar", color = Color.Gray, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            val added = fundsAmount.replace(",", ".").toDoubleOrNull() ?: 0.0
                                            if (added > 0.0 && selectedOrigin.isNotEmpty()) {
                                                onAddFunds(added, selectedOrigin)
                                                showAddFundsSection = false
                                                fundsAmount = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = colorHex)
                                    ) {
                                        Text("Confirmar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        isBuying -> {
                            val remainingValue = (goal.targetValue - goal.currentValue).coerceAtLeast(0.0)
                            val formattedRemaining = String.format(Locale("pt", "BR"), "R$ %,.2f", remainingValue)
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x08FFFFFF), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text("Realizar Compra", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                if (remainingValue > 0.0) {
                                    Text("Falta $formattedRemaining. De qual conta debitar?", color = Color(0xFFBDC9C6), fontSize = 12.sp)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = selectedOrigin,
                                            onValueChange = {},
                                            readOnly = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            trailingIcon = {
                                                IconButton(onClick = { showOriginDropdown = true }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                                }
                                            }
                                        )
                                        DropdownMenu(
                                            expanded = showOriginDropdown,
                                            onDismissRequest = { showOriginDropdown = false },
                                            modifier = Modifier.background(Color(0xFF131817))
                                        ) {
                                            origins.forEach { origin ->
                                                DropdownMenuItem(
                                                    text = { Text(origin, color = Color.White) },
                                                    onClick = {
                                                        selectedOrigin = origin
                                                        showOriginDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text("Totalmente financiado! Confirme para concluir.", color = Color(0xFF71D7CD), fontSize = 12.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { showBuySection = false }) {
                                        Text("Cancelar", color = Color.Gray, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            onBuy(selectedOrigin)
                                            showBuySection = false
                                            if (goal.buyUrl.isNotBlank()) {
                                                try {
                                                    val fullUrl = if (!goal.buyUrl.startsWith("http://") && !goal.buyUrl.startsWith("https://")) {
                                                        "https://${goal.buyUrl}"
                                                    } else goal.buyUrl
                                                    uriHandler.openUri(fullUrl)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF71D7CD))
                                    ) {
                                        Text("Confirmar Compra", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = onEditClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0x10FFFFFF), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Editar", tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                OutlinedButton(
                                    onClick = { showAddFundsSection = true },
                                    modifier = Modifier.weight(1f),
                                    border = BorderStroke(1.dp, colorHex),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorHex),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Aportar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val finalUrl = if (goal.buyUrl.startsWith("http://") || goal.buyUrl.startsWith("https://")) {
                                                goal.buyUrl
                                            } else {
                                                "https://$goal.buyUrl"
                                            }
                                            uriHandler.openUri(finalUrl)
                                        } catch (e: Exception) {
                                            uriHandler.openUri("https://www.google.com/search?q=${goal.title}")
                                        }
                                        showBuySection = true
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (progress >= 1.0) Color(0xFF71D7CD) else Color(0x22FFFFFF)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (progress >= 1.0) "Comprar! 🛍️" else "Comprar",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (progress >= 1.0) Color.Black else Color.White
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

@Composable
fun BoughtGoalCard(goal: PurchaseGoal, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(0.5.dp, Color(0xFF71D7CD).copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = goal.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF71D7CD).copy(alpha = 0.15f))
                            .border(0.5.dp, Color(0xFF71D7CD).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CONQUISTADO 🌟",
                            color = Color(0xFF71D7CD),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale("pt", "BR"), "R$ %,.2f", goal.targetValue),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Outlined.Delete, contentDescription = "Deletar Conquista", tint = Color.Gray)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = Color(0xFF141918),
            title = { Text("Excluir Conquista?", color = Color.White) },
            text = { Text("Isso removerá este item de desejo realizado da lista. Esta ação não afetará suas transações financeiras passadas.", color = Color(0xFFBDC9C6)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Remover", color = Color(0xFFFF5252))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseGoalDialogWishes(
    onDismiss: () -> Unit,
    onSave: (title: String, target: Double, url: String, buyUrl: String, category: String, color: String, priorityOrder: Int, priorityClassification: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var buyUrl by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Eletrônicos") }
    var priorityOrder by remember { mutableStateOf("1") }
    var selectedClassification by remember { mutableStateOf("Moderado") }

    var showPexelsDialog by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    val categories = listOf("Eletrônicos", "Moda", "Casa", "Viagem", "Lazer", "Outros")

    val isValid = title.isNotBlank() && target.toDoubleOrNull() != null && buyUrl.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141918),
        title = { Text("Novo Desejo", color = Color(0xFFDFE3E2)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preview Imagem / Chamada Pexels
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x10FFFFFF))
                        .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { showPexelsDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (url.isBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color(0xFF71D7CD),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Pesquisar Imagem no Unsplash", color = Color(0xFF81928F), fontSize = 12.sp)
                        }
                    } else {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .background(Color(0xAA000000), CircleShape)
                                .border(1.dp, Color(0x33FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Trocar imagem",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("O que você deseja comprar?", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947), focusedBorderColor = Color(0xFF71D7CD)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target, onValueChange = { target = it }, label = { Text("Valor Alvo (R$)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947), focusedBorderColor = Color(0xFF71D7CD)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = buyUrl, onValueChange = { buyUrl = it }, label = { Text("Link de Compra (Obrigatório)", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947), focusedBorderColor = Color(0xFF71D7CD)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (buyUrl.isNotBlank()) {
                    val coroutineScope = rememberCoroutineScope()
                    var isExtractingImage by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1571D7CD))
                            .border(1.dp, Color(0xFF71D7CD).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable(enabled = !isExtractingImage) {
                                isExtractingImage = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    val extracted = fetchOgImageFromUrl(buyUrl)
                                    if (!extracted.isNullOrBlank()) {
                                        url = extracted
                                    }
                                    isExtractingImage = false
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isExtractingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF71D7CD), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extraindo foto do produto...", color = Color(0xFF71D7CD), fontSize = 12.sp)
                        } else {
                            Icon(Icons.Outlined.Link, contentDescription = null, tint = Color(0xFF71D7CD), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Extrair Foto Real do Link do Produto 🔗", color = Color(0xFF71D7CD), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Dropdown de Categoria
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria", color = Color(0xFF81928F)) },
                        trailingIcon = {
                            IconButton(onClick = { showCategoryDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947), focusedBorderColor = Color(0xFF71D7CD)),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                        modifier = Modifier.background(Color(0xFF131817))
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = Color.White) },
                                onClick = {
                                    category = cat
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = priorityOrder, onValueChange = { priorityOrder = it.filter { c -> c.isDigit() } }, label = { Text("Ordem de Prioridade (Numérica)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947), focusedBorderColor = Color(0xFF71D7CD)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Classificação de Prioridade", color = Color(0xFF81928F), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Leve", "Moderado", "Urgente").forEach { level ->
                            val isSelected = selectedClassification == level
                            val chipBg = when (level) {
                                "Urgente" -> if (isSelected) Color(0xFFEF4444) else Color(0x1AEF4444)
                                "Moderado" -> if (isSelected) Color(0xFFF9A826) else Color(0x1AF9A826)
                                else -> if (isSelected) Color(0xFF71D7CD) else Color(0x1A71D7CD)
                            }
                            val chipTextColor = if (isSelected) Color.Black else when (level) {
                                "Urgente" -> Color(0xFFEF4444)
                                "Moderado" -> Color(0xFFF9A826)
                                else -> Color(0xFF71D7CD)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(1.dp, if (isSelected) Color.White else chipTextColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { selectedClassification = level }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(level, color = chipTextColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val t = target.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val pOrd = priorityOrder.toIntOrNull() ?: 1
                    val defaultUrl = if (url.isBlank()) "https://images.unsplash.com/photo-1555626906-fcf10d6851b4?q=80&w=800&auto=format&fit=crop" else url
                    val colorHex = when (selectedClassification) {
                        "Urgente" -> "#EF4444"
                        "Moderado" -> "#F9A826"
                        else -> "#71D7CD"
                    }
                    onSave(title, t, defaultUrl, buyUrl, category, colorHex, pOrd, selectedClassification)
                },
                enabled = isValid
            ) {
                Text("Salvar", color = if (isValid) Color(0xFFF9A826) else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF81928F)) }
        }
    )

    if (showPexelsDialog) {
        UnsplashImageSearchDialog(
            onDismiss = { showPexelsDialog = false },
            onImageSelected = { selectedUrl ->
                url = selectedUrl
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPurchaseGoalDialogWishes(
    goal: PurchaseGoal,
    onDismiss: () -> Unit,
    onSave: (PurchaseGoal) -> Unit,
    onDelete: (PurchaseGoal) -> Unit
) {
    var title by remember { mutableStateOf(goal.title) }
    var target by remember { mutableStateOf(goal.targetValue.toString()) }
    var current by remember { mutableStateOf(goal.currentValue.toString()) }
    var url by remember { mutableStateOf(goal.imageUrl) }
    var buyUrl by remember { mutableStateOf(goal.buyUrl) }
    var category by remember { mutableStateOf(goal.category) }
    var priorityOrder by remember { mutableStateOf(goal.priorityOrder.toString()) }
    var selectedClassification by remember { mutableStateOf(goal.priorityClassification) }

    var showPexelsDialog by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    val categories = listOf("Eletrônicos", "Moda", "Casa", "Viagem", "Lazer", "Outros")

    val isValid = title.isNotBlank() && target.toDoubleOrNull() != null && buyUrl.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141918),
        title = { Text("Editar Desejo", color = Color(0xFFDFE3E2)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preview Imagem / Chamada Pexels
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x10FFFFFF))
                        .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { showPexelsDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (url.isBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color(0xFF71D7CD),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Pesquisar Imagem no Unsplash", color = Color(0xFF81928F), fontSize = 12.sp)
                        }
                    } else {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(28.dp)
                                .background(Color(0xAA000000), CircleShape)
                                .border(1.dp, Color(0x33FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Trocar imagem",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("O que você deseja comprar?", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947), focusedBorderColor = Color(0xFF71D7CD)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target, onValueChange = { target = it }, label = { Text("Valor Alvo (R$)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947), focusedBorderColor = Color(0xFF71D7CD)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = current, onValueChange = { current = it }, label = { Text("Valor Atual Salvo (R$)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947), focusedBorderColor = Color(0xFF71D7CD)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = buyUrl, onValueChange = { buyUrl = it }, label = { Text("Link de Compra (Obrigatório)", color = Color(0xFF81928F)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947), focusedBorderColor = Color(0xFF71D7CD)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (buyUrl.isNotBlank()) {
                    val coroutineScope = rememberCoroutineScope()
                    var isExtractingImage by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1571D7CD))
                            .border(1.dp, Color(0xFF71D7CD).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable(enabled = !isExtractingImage) {
                                isExtractingImage = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    val extracted = fetchOgImageFromUrl(buyUrl)
                                    if (!extracted.isNullOrBlank()) {
                                        url = extracted
                                    }
                                    isExtractingImage = false
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isExtractingImage) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF71D7CD), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extraindo foto do produto...", color = Color(0xFF71D7CD), fontSize = 12.sp)
                        } else {
                            Icon(Icons.Outlined.Link, contentDescription = null, tint = Color(0xFF71D7CD), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Extrair Foto Real do Link do Produto 🔗", color = Color(0xFF71D7CD), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Dropdown de Categoria
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria", color = Color(0xFF81928F)) },
                        trailingIcon = {
                            IconButton(onClick = { showCategoryDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947), focusedBorderColor = Color(0xFF71D7CD)),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                        modifier = Modifier.background(Color(0xFF131817))
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = Color.White) },
                                onClick = {
                                    category = cat
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = priorityOrder, onValueChange = { priorityOrder = it.filter { c -> c.isDigit() } }, label = { Text("Ordem de Prioridade (Numérica)", color = Color(0xFF81928F)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, unfocusedBorderColor = Color(0xFF3D4947), focusedBorderColor = Color(0xFF71D7CD)), singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text("Classificação de Prioridade", color = Color(0xFF81928F), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Leve", "Moderado", "Urgente").forEach { level ->
                            val isSelected = selectedClassification == level
                            val chipBg = when (level) {
                                "Urgente" -> if (isSelected) Color(0xFFEF4444) else Color(0x1AEF4444)
                                "Moderado" -> if (isSelected) Color(0xFFF9A826) else Color(0x1AF9A826)
                                else -> if (isSelected) Color(0xFF71D7CD) else Color(0x1A71D7CD)
                            }
                            val chipTextColor = if (isSelected) Color.Black else when (level) {
                                "Urgente" -> Color(0xFFEF4444)
                                "Moderado" -> Color(0xFFF9A826)
                                else -> Color(0xFF71D7CD)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .border(1.dp, if (isSelected) Color.White else chipTextColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { selectedClassification = level }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(level, color = chipTextColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onDelete(goal) }) {
                    Text("Excluir", color = Color(0xFFFF5252))
                }
                TextButton(
                    onClick = {
                        val t = target.replace(",", ".").toDoubleOrNull() ?: goal.targetValue
                        val c = current.replace(",", ".").toDoubleOrNull() ?: goal.currentValue
                        val pOrd = priorityOrder.toIntOrNull() ?: goal.priorityOrder
                        val colorHex = when (selectedClassification) {
                            "Urgente" -> "#EF4444"
                            "Moderado" -> "#F9A826"
                            else -> "#71D7CD"
                        }
                        onSave(
                            goal.copy(
                                title = title,
                                targetValue = t,
                                currentValue = c,
                                imageUrl = url,
                                buyUrl = buyUrl,
                                category = category,
                                priorityOrder = pOrd,
                                priorityClassification = selectedClassification,
                                colorHex = colorHex
                            )
                        )
                    },
                    enabled = isValid
                ) {
                    Text("Salvar", color = if (isValid) Color(0xFFF9A826) else Color.Gray)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF81928F)) }
        }
    )

    if (showPexelsDialog) {
        UnsplashImageSearchDialog(
            onDismiss = { showPexelsDialog = false },
            onImageSelected = { selectedUrl ->
                url = selectedUrl
            }
        )
    }
}
