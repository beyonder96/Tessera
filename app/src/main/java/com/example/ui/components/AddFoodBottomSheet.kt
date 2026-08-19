package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.MealType
import com.example.data.nutrition.*
import com.example.ui.theme.PrimaryTeal
import com.example.viewmodel.TesseraViewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AddFoodTab(val title: String) {
    OPEN_FOOD("Open Food Facts"),
    NLP_TEXT("Texto Livre (Edamam)"),
    MANUAL("Manual")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodBottomSheet(
    selectedMealType: MealType,
    selectedDate: String,
    viewModel: TesseraViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(AddFoodTab.OPEN_FOOD) }

    // State for Open Food Facts
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<OpenFoodProduct>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val scannerOptions = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE
            )
            .enableAutoZoom()
            .build()
    }

    val scanner = remember {
        GmsBarcodeScanning.getClient(context, scannerOptions)
    }

    fun startLiveBarcodeScan() {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (!rawValue.isNullOrBlank()) {
                    searchQuery = rawValue
                    isSearching = true
                    searchError = null
                    coroutineScope.launch {
                        try {
                            val response = OpenFoodFactsApiClient.service.getProductByBarcode(rawValue.trim())
                            if (response.product != null) {
                                searchResults = listOf(response.product)
                            } else {
                                searchResults = emptyList()
                                searchError = "Nenhum produto encontrado para o código $rawValue."
                            }
                        } catch (e: Exception) {
                            searchError = "Erro ao buscar produto escaneado."
                        } finally {
                            isSearching = false
                        }
                    }
                }
            }
            .addOnCanceledListener {
                // Scan cancelado pelo usuário
            }
            .addOnFailureListener { e ->
                searchError = "Não foi possível abrir o scanner de câmera."
            }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startLiveBarcodeScan()
        } else {
            searchError = "Permissão de câmera necessária para escanear código de barras."
        }
    }

    // State for Natural Language / Edamam
    var nlpText by remember { mutableStateOf("") }
    var isAnalyzingNlp by remember { mutableStateOf(false) }
    var nlpResult by remember { mutableStateOf<NutritionAnalysisResult?>(null) }
    var nlpError by remember { mutableStateOf<String?>(null) }

    // State for Manual Entry
    var manualName by remember { mutableStateOf("") }
    var manualPortion by remember { mutableStateOf("1 porção") }
    var manualCalories by remember { mutableStateOf("") }
    var manualProtein by remember { mutableStateOf("") }
    var manualCarbs by remember { mutableStateOf("") }
    var manualFat by remember { mutableStateOf("") }
    var manualFiber by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = themedCardBackground(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Adicionar a ${selectedMealType.title}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Selecione o modo de registro nutricional",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(themedSubtleBackground())
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AddFoodTab.values().forEach { tab ->
                    val isSelected = tab == currentTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) themedCardBackground() else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) themedSubtleBorder() else Color.Transparent,
                                shape = RoundedCornerShape(9.dp)
                            )
                            .clickable { currentTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (currentTab) {
                AddFoodTab.OPEN_FOOD -> {
                    // Open Food Facts Search & Barcode Live Scanner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                                searchJob?.cancel()
                                if (query.isNotBlank()) {
                                    searchJob = coroutineScope.launch {
                                        delay(450)
                                        isSearching = true
                                        searchError = null
                                        try {
                                            // Se for código numérico (código de barras), busca direta por barcode
                                            if (query.trim().matches("""^\d{6,14}$""".toRegex())) {
                                                val response = OpenFoodFactsApiClient.service.getProductByBarcode(query.trim())
                                                if (response.product != null) {
                                                    searchResults = listOf(response.product)
                                                } else {
                                                    searchResults = emptyList()
                                                    searchError = "Nenhum produto encontrado para este código de barras."
                                                }
                                            } else {
                                                val response = OpenFoodFactsApiClient.service.searchProducts(terms = query.trim())
                                                searchResults = response.products ?: emptyList()
                                                if (searchResults.isEmpty()) {
                                                    searchError = "Nenhum produto encontrado para \"$query\"."
                                                }
                                            }
                                        } catch (e: Exception) {
                                            searchError = "Erro ao buscar produtos. Verifique sua conexão."
                                        } finally {
                                            isSearching = false
                                        }
                                    }
                                } else {
                                    searchResults = emptyList()
                                    isSearching = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Buscar produto ou código...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryTeal) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        searchResults = emptyList()
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            colors = themedTextFieldColors(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (hasCamera) {
                                    startLiveBarcodeScan()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PrimaryTeal.copy(alpha = 0.15f))
                                .border(1.dp, PrimaryTeal.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = "Escanear Código com Câmera",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = PrimaryTeal)
                        }
                    } else if (searchError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(searchError ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (searchResults.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { product ->
                                OpenFoodProductCard(
                                    product = product,
                                    onAdd = {
                                        viewModel.addMealRecord(
                                            mealType = selectedMealType.name,
                                            name = "${product.displayName} (${product.displayBrand})",
                                            calories = product.effectiveCalories,
                                            protein = product.effectiveProtein,
                                            carbs = product.effectiveCarbs,
                                            fat = product.effectiveFat,
                                            fiber = product.effectiveFiber,
                                            portion = product.effectivePortion,
                                            imageUrl = product.bestImage,
                                            barcode = product.code,
                                            date = selectedDate
                                        )
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    } else {
                        // Empty / Initial prompt
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.QrCodeScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Busque por produtos brasileiros ou digite o código de barras",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                AddFoodTab.NLP_TEXT -> {
                    // Natural language text input (Edamam + Intelligent Analysis)
                    Text(
                        text = "Descreva a refeição em texto livre:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = nlpText,
                        onValueChange = { 
                            nlpText = it 
                            nlpResult = null
                            nlpError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        placeholder = { 
                            Text(
                                "Ex: 1 xícara de arroz branco, 1 concha de feijão carioca, 150g de peito de frango grelhado e salada",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            ) 
                        },
                        colors = themedTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (nlpText.isNotBlank()) {
                                coroutineScope.launch {
                                    isAnalyzingNlp = true
                                    nlpError = null
                                    try {
                                        val result = NutritionDishAnalyzer.analyzeText(nlpText)
                                        nlpResult = result
                                    } catch (e: Exception) {
                                        nlpError = "Não foi possível analisar o prato. Tente novamente."
                                    } finally {
                                        isAnalyzingNlp = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = nlpText.isNotBlank() && !isAnalyzingNlp,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAnalyzingNlp) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ANALISAR PRATO", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    if (nlpResult != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        val res = nlpResult!!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(themedSubtleBackground())
                                .border(1.dp, themedSubtleBorder(), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = res.dishName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${res.calories.toInt()} kcal",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryTeal
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MacroPill("Prot", "${res.protein.toInt()}g", Color(0xFF64B5F6))
                                    MacroPill("Carb", "${res.carbs.toInt()}g", Color(0xFFFFB74D))
                                    MacroPill("Gord", "${res.fat.toInt()}g", Color(0xFFE57373))
                                    MacroPill("Fibra", "${res.fiber.toInt()}g", Color(0xFF81C784))
                                }

                                Button(
                                    onClick = {
                                        viewModel.addMealRecord(
                                            mealType = selectedMealType.name,
                                            name = res.dishName,
                                            calories = res.calories,
                                            protein = res.protein,
                                            carbs = res.carbs,
                                            fat = res.fat,
                                            fiber = res.fiber,
                                            portion = res.portion,
                                            date = selectedDate
                                        )
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CONFIRMAR & ADICIONAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                AddFoodTab.MANUAL -> {
                    // Manual entry fields
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = manualName,
                                onValueChange = { manualName = it },
                                label = { Text("Nome do Alimento", fontSize = 11.sp) },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                                colors = themedTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = manualPortion,
                                onValueChange = { manualPortion = it },
                                label = { Text("Porção", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = themedTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualCalories,
                                onValueChange = { manualCalories = it },
                                label = { Text("Kcal", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = themedTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = manualProtein,
                                onValueChange = { manualProtein = it },
                                label = { Text("Prot (g)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = themedTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = manualCarbs,
                                onValueChange = { manualCarbs = it },
                                label = { Text("Carb (g)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = themedTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = manualFat,
                                onValueChange = { manualFat = it },
                                label = { Text("Gord (g)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = themedTextFieldColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val cal = manualCalories.toDoubleOrNull() ?: 0.0
                                val prot = manualProtein.toDoubleOrNull() ?: 0.0
                                val carbs = manualCarbs.toDoubleOrNull() ?: 0.0
                                val fat = manualFat.toDoubleOrNull() ?: 0.0
                                val fib = manualFiber.toDoubleOrNull() ?: 0.0

                                if (manualName.isNotBlank() && cal > 0) {
                                    viewModel.addMealRecord(
                                        mealType = selectedMealType.name,
                                        name = manualName.trim(),
                                        calories = cal,
                                        protein = prot,
                                        carbs = carbs,
                                        fat = fat,
                                        fiber = fib,
                                        portion = manualPortion.trim(),
                                        date = selectedDate
                                    )
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = manualName.isNotBlank() && (manualCalories.toDoubleOrNull() ?: 0.0) > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SALVAR NO DIÁRIO", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenFoodProductCard(
    product: OpenFoodProduct,
    onAdd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(themedSubtleBackground())
            .border(1.dp, themedSubtleBorder(), RoundedCornerShape(14.dp))
            .clickable { onAdd() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product image
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x0DFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                if (product.bestImage != null) {
                    AsyncImage(
                        model = product.bestImage,
                        contentDescription = product.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Outlined.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.displayBrand,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${product.effectiveCalories.toInt()} kcal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${product.effectivePortion}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PrimaryTeal.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = PrimaryTeal, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MacroPill(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}
