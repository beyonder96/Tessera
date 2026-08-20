package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.wger.WgerExercise
import com.example.ui.components.themedCardBackground
import com.example.ui.components.themedCardBorder
import com.example.ui.components.themedSubtleBackground
import com.example.ui.components.themedSubtleBorder
import com.example.ui.theme.PrimaryTeal
import com.example.viewmodel.TesseraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymScreen(
    viewModel: TesseraViewModel,
    modifier: Modifier = Modifier
) {
    val exercises by viewModel.wgerExercises.collectAsState()
    val selectedCategory by viewModel.selectedWgerCategory.collectAsState()
    val searchQuery by viewModel.wgerSearchQuery.collectAsState()
    val isLoading by viewModel.isLoadingWgerExercises.collectAsState()
    val error by viewModel.wgerExerciseError.collectAsState()

    var selectedExerciseForDetail by remember { mutableStateOf<WgerExercise?>(null) }

    LaunchedEffect(Unit) {
        if (exercises.isEmpty()) {
            viewModel.fetchWgerExercises(forceRefresh = false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // HEADER PRINCIPAL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Academia",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PrimaryTeal.copy(alpha = 0.12f))
                            .border(0.5.dp, PrimaryTeal.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "WGER OPEN API",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                }
                Text(
                    text = "${exercises.size} exercícios catalogados",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            IconButton(
                onClick = { viewModel.fetchWgerExercises(forceRefresh = true) },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(themedSubtleBackground())
                    .border(0.5.dp, themedSubtleBorder(), CircleShape)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Atualizar Catálogo",
                    tint = PrimaryTeal,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // CAMPO DE BUSCA
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setWgerSearchQuery(it) },
            placeholder = {
                Text(
                    text = "Buscar exercício, músculo ou equipamento...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setWgerSearchQuery("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Limpar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryTeal,
                unfocusedBorderColor = themedSubtleBorder(),
                focusedContainerColor = themedSubtleBackground(),
                unfocusedContainerColor = themedSubtleBackground()
            )
        )

        // CHIPS DE GRUPOS MUSCULARES
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.wgerCategoryFilters.forEach { filter ->
                val isSelected = filter.id == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryTeal.copy(alpha = 0.15f) else themedSubtleBackground())
                        .border(
                            width = 0.5.dp,
                            color = if (isSelected) PrimaryTeal else themedSubtleBorder(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.setWgerCategory(filter.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // FEED DE EXERCÍCIOS
        if (isLoading && exercises.isEmpty()) {
            // SKELETON LOADING STATE
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(themedSubtleBackground())
                            .border(1.dp, themedSubtleBorder(), RoundedCornerShape(14.dp))
                    )
                }
            }
        } else if (error != null && exercises.isEmpty()) {
            // ERROR STATE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(themedCardBackground())
                    .border(1.dp, themedCardBorder(), RoundedCornerShape(14.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = error ?: "Erro ao carregar catálogo.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.fetchWgerExercises(forceRefresh = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tentar Novamente", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else if (exercises.isEmpty()) {
            // EMPTY STATE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Nenhum exercício encontrado",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tente buscar com outro termo ou selecione 'Todos'.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // LISTA DE EXERCÍCIOS
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseCardItem(
                        exercise = exercise,
                        onClick = { selectedExerciseForDetail = exercise }
                    )
                }
            }
        }
    }

    // MODAL DE DETALHES DO EXERCÍCIO
    selectedExerciseForDetail?.let { exercise ->
        ExerciseDetailBottomSheet(
            exercise = exercise,
            onDismiss = { selectedExerciseForDetail = null }
        )
    }
}

@Composable
private fun ExerciseCardItem(
    exercise: WgerExercise,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail da execução
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(themedSubtleBackground())
                .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (exercise.mainImageUrl != null) {
                AsyncImage(
                    model = exercise.mainImageUrl,
                    contentDescription = exercise.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Informações Textuais
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = exercise.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PrimaryTeal.copy(alpha = 0.12f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = exercise.categoryName.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Músculos e Equipamento
            val infoSnippet = buildString {
                if (exercise.primaryMuscles.isNotEmpty()) {
                    append(exercise.primaryMuscles.joinToString(", "))
                }
                if (exercise.equipment.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(exercise.equipment.joinToString(", "))
                }
            }

            Text(
                text = infoSnippet.ifBlank { "Musculação e Hipertrofia" },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDetailBottomSheet(
    exercise: WgerExercise,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IMAGEM EM DESTAQUE
            if (exercise.mainImageUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(themedSubtleBackground())
                        .border(1.dp, themedSubtleBorder(), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = exercise.mainImageUrl,
                        contentDescription = exercise.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // TÍTULO E GRUPO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(PrimaryTeal.copy(alpha = 0.15f))
                        .border(0.5.dp, PrimaryTeal.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = exercise.categoryName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                }
            }

            // TAGS DE MÚSCULOS E EQUIPAMENTOS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (exercise.primaryMuscles.isNotEmpty()) {
                    Text(
                        text = "MÚSCULOS PRINCIPAIS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        exercise.primaryMuscles.forEach { muscle ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(themedSubtleBackground())
                                    .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(muscle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }

                if (exercise.equipment.isNotEmpty()) {
                    Text(
                        text = "EQUIPAMENTO NECESSÁRIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        exercise.equipment.forEach { eq ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(themedSubtleBackground())
                                    .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(eq, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            }

            // INSTRUÇÕES DE EXECUÇÃO
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "INSTRUÇÕES E POSTURA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal,
                    letterSpacing = 1.sp
                )
                Text(
                    text = exercise.description,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
                )
            }
        }
    }
}
