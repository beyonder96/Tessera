package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.viewmodel.TesseraViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class InfinityStoneModule(
    val id: String,
    val stoneName: String,
    val moduleTitle: String,
    val subtitle: String,
    val metricHighlight: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val glowColor: Color,
    val coreColor: Color,
    val route: String
)

@Composable
fun BentoBoxDashboard(
    viewModel: TesseraViewModel,
    isExpanded: Boolean,
    onNavigate: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val itemsAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "bento_alpha"
    )
    val itemsOffset by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 20.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
        label = "bento_offset"
    )

    // Observar dados em tempo real dos módulos
    val transactions by viewModel.allTransactions.collectAsState()
    val healthProfile by viewModel.healthProfile.collectAsState()
    val mealRecords by viewModel.allMealRecords.collectAsState()
    val marketItems by viewModel.pendingMarketItems.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val purchaseGoals by viewModel.allPurchaseGoals.collectAsState()
    val petEvents by viewModel.allPetEvents.collectAsState()
    val metroStatus by viewModel.metroStatus.collectAsState()
    val busLines by viewModel.savedBusLines.collectAsState()
    val dailyVerse by viewModel.dailyVerse.collectAsState()

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayCalories = remember(mealRecords, todayStr) {
        mealRecords.filter { it.date == todayStr }.sumOf { it.calories }.toInt()
    }
    val calGoal = healthProfile?.dailyCalorieGoal?.toInt() ?: 2000

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val currentBalance = remember(transactions) {
        transactions.sumOf { if (it.isIncome) it.value else -it.value }
    }

    // Estrutura das 9 Joias do Infinito do Tessera
    val infinityStones = remember(
        transactions, currentBalance, todayCalories, calGoal,
        marketItems, habits, purchaseGoals, petEvents,
        metroStatus, busLines, dailyVerse
    ) {
        listOf(
            InfinityStoneModule(
                id = "reality",
                stoneName = "Joia da Realidade",
                moduleTitle = "Finanças",
                subtitle = "${transactions.size} transações registradas",
                metricHighlight = currencyFormatter.format(currentBalance),
                icon = Icons.Outlined.AccountBalanceWallet,
                primaryColor = Color(0xFFEF4444),
                glowColor = Color(0xFFF87171),
                coreColor = Color(0xFFFFE4E6),
                route = "finance"
            ),
            InfinityStoneModule(
                id = "time",
                stoneName = "Joia do Tempo",
                moduleTitle = "Lista de Mercado",
                subtitle = if (marketItems.isNotEmpty()) "${marketItems.size} itens na lista de compras" else "Lista vazia e pronta",
                metricHighlight = if (marketItems.isNotEmpty()) "${marketItems.size} Itens" else "Concluído",
                icon = Icons.Outlined.ShoppingCart,
                primaryColor = Color(0xFF10B981),
                glowColor = Color(0xFF34D399),
                coreColor = Color(0xFFD1FAE5),
                route = "market"
            ),
            InfinityStoneModule(
                id = "power",
                stoneName = "Joia do Poder",
                moduleTitle = "Rotinas & Hábitos",
                subtitle = if (habits.isNotEmpty()) "${habits.size} hábitos em progresso diário" else "Construa novas rotinas",
                metricHighlight = if (habits.isNotEmpty()) "${habits.size} Ativos" else "0 Hábitos",
                icon = Icons.Outlined.Flag,
                primaryColor = Color(0xFFA855F7),
                glowColor = Color(0xFFC084FC),
                coreColor = Color(0xFFF3E8FF),
                route = "goals"
            ),
            InfinityStoneModule(
                id = "space",
                stoneName = "Joia do Espaço",
                moduleTitle = "Transporte SP",
                subtitle = if (busLines.isNotEmpty()) "${busLines.size} linhas salvas • Metrô & Ônibus" else "Metrô/CPTM & Ônibus ao vivo",
                metricHighlight = "SP Ao Vivo",
                icon = Icons.Outlined.DirectionsTransit,
                primaryColor = Color(0xFF0284C7),
                glowColor = Color(0xFF38BDF8),
                coreColor = Color(0xFFE0F2FE),
                route = "transport"
            ),
            InfinityStoneModule(
                id = "mind",
                stoneName = "Joia da Mente",
                moduleTitle = "Bíblia Sagrada",
                subtitle = if (dailyVerse != null) "${dailyVerse?.book?.name} ${dailyVerse?.chapter}:${dailyVerse?.verse}" else "Leitor YouVersion & Versículos",
                metricHighlight = if (dailyVerse != null) "${dailyVerse?.book?.name ?: "Bíblia"} ${dailyVerse?.chapter ?: 1}" else "Palavra",
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                primaryColor = Color(0xFFEAB308),
                glowColor = Color(0xFFFDE047),
                coreColor = Color(0xFFFEF9C3),
                route = "bible"
            ),
            InfinityStoneModule(
                id = "life",
                stoneName = "Joia da Vida",
                moduleTitle = "Saúde & Nutri",
                subtitle = if (todayCalories > 0) "$todayCalories de $calGoal kcal meta" else "Diário alimentar e bio-métricas",
                metricHighlight = if (todayCalories > 0) "$todayCalories kcal" else "Nutrição",
                icon = Icons.Outlined.MonitorHeart,
                primaryColor = Color(0xFF06B6D4),
                glowColor = Color(0xFF22D3EE),
                coreColor = Color(0xFFCFFAFE),
                route = "health"
            ),
            InfinityStoneModule(
                id = "soul",
                stoneName = "Joia da Alma",
                moduleTitle = "Desejos & Metas",
                subtitle = if (purchaseGoals.isNotEmpty()) "${purchaseGoals.size} metas e sonhos na lista" else "Wishlist de metas",
                metricHighlight = if (purchaseGoals.isNotEmpty()) "${purchaseGoals.size} Desejos" else "Wishlist",
                icon = Icons.Outlined.BookmarkBorder,
                primaryColor = Color(0xFFF97316),
                glowColor = Color(0xFFFB923C),
                coreColor = Color(0xFFFFEDD5),
                route = "wishes"
            ),
            InfinityStoneModule(
                id = "companion",
                stoneName = "Joia do Afeto",
                moduleTitle = "Petz",
                subtitle = if (petEvents.isNotEmpty()) "${petEvents.size} cuidados e registros" else "Cuidados diários e saúde",
                metricHighlight = if (petEvents.isNotEmpty()) "${petEvents.size} Cuidados" else "Pets",
                icon = Icons.Outlined.Pets,
                primaryColor = Color(0xFFEC4899),
                glowColor = Color(0xFFF472B6),
                coreColor = Color(0xFFFCE7F3),
                route = "petz"
            ),
            InfinityStoneModule(
                id = "territory",
                stoneName = "Joia da Fundação",
                moduleTitle = "Meu Apê",
                subtitle = "Reforma, compras e custos da casa",
                metricHighlight = "Imóvel",
                icon = Icons.Outlined.Construction,
                primaryColor = Color(0xFFD97706),
                glowColor = Color(0xFFF59E0B),
                coreColor = Color(0xFFFEF3C7),
                route = "apartment"
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { infinityStones.size }, initialPage = 0)

    val infiniteTransition = rememberInfiniteTransition(label = "infinityStoneLoop")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(80000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stoneTime"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = itemsOffset)
            .alpha(itemsAlpha)
    ) {
        // Carrossel Horizontal das Joias Cósmicas
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = 16.dp
        ) { page ->
            val stone = infinityStones[page]

            // Cálculo de Parallax / Escala 3D
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val scale = lerp(1.04f, 0.82f, pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(1.0f, 0.38f, pageOffset.coerceIn(0f, 1f))

            InfinityStoneCard(
                stone = stone,
                time = time,
                scale = scale,
                alpha = alpha,
                isFocused = pagerState.currentPage == page,
                onClick = { onNavigate(stone.route) }
            )
        }

        // Mini Seletor Cósmico de Joias (9 pontos iluminados)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(themedSubtleBackground())
                .border(0.5.dp, themedSubtleBorder(), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            infinityStones.forEachIndexed { index, item ->
                val isSelected = pagerState.currentPage == index
                val dotSize by animateDpAsState(
                    targetValue = if (isSelected) 12.dp else 7.dp,
                    animationSpec = tween(200),
                    label = "dotSize"
                )

                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(if (isSelected) item.primaryColor else item.primaryColor.copy(alpha = 0.35f))
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                )
            }
        }
    }
}

// ==============================================================================
// CARD DA JOIA DO INFINITO
// ==============================================================================
@Composable
private fun InfinityStoneCard(
    stone: InfinityStoneModule,
    time: Float,
    scale: Float,
    alpha: Float,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val clickScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else scale,
        animationSpec = tween(150),
        label = "clickScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = clickScale
                scaleY = clickScale
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        stone.primaryColor.copy(alpha = if (isFocused) 0.16f else 0.05f),
                        Color(0xFF121216).copy(alpha = 0.85f),
                        Color(0xFF08080A).copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        stone.primaryColor.copy(alpha = if (isFocused) 0.80f else 0.25f),
                        stone.glowColor.copy(alpha = if (isFocused) 0.30f else 0.08f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(stone.primaryColor.copy(alpha = 0.14f))
                    .border(0.5.dp, stone.primaryColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(stone.primaryColor)
                )
                Text(
                    text = stone.stoneName.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = stone.glowColor,
                    letterSpacing = 1.2.sp
                )
            }

            // Canvas da Joia Cósmica com Raios e Flutuação
            Box(
                modifier = Modifier
                    .size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                InfinityStoneCanvas(
                    primaryColor = stone.primaryColor,
                    glowColor = stone.glowColor,
                    coreColor = stone.coreColor,
                    time = time,
                    isFocused = isFocused
                )

                // Ícone do módulo no coração da gema
                Icon(
                    imageVector = stone.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White.copy(alpha = 0.95f)
                )
            }

            // Informações do Módulo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stone.moduleTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stone.metricHighlight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = stone.glowColor
                )
                Text(
                    text = stone.subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // CTA Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = stone.primaryColor.copy(alpha = if (isFocused) 0.18f else 0.08f),
                border = BorderStroke(1.dp, stone.primaryColor.copy(alpha = if (isFocused) 0.6f else 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Acessar Módulo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFocused) stone.glowColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (isFocused) stone.glowColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==============================================================================
// CANVAS DA JOIA CÓSMICA (Física de Raios, Flutuação e Brilho)
// ==============================================================================
@Composable
private fun InfinityStoneCanvas(
    primaryColor: Color,
    glowColor: Color,
    coreColor: Color,
    time: Float,
    isFocused: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        // Levitação senoidal suave (flutuação vertical)
        val floatOffset = sin(time * 0.06f) * 6f
        val centerY = (size.height / 2f) + floatOffset
        val center = Offset(centerX, centerY)

        val pulse = (sin(time * 0.08f) + 1f) / 2f
        val gemRadius = size.width * 0.22f
        val haloRadius = size.width * 0.45f + (if (isFocused) pulse * 8f else 0f)

        // 1. Halo Luminoso Externo (Glow Cósmico)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = if (isFocused) 0.45f + pulse * 0.15f else 0.20f),
                    primaryColor.copy(alpha = if (isFocused) 0.25f else 0.08f),
                    Color.Transparent
                ),
                center = center,
                radius = haloRadius
            ),
            center = center,
            radius = haloRadius,
            blendMode = BlendMode.SrcOver
        )

        // 2. Raios Cósmicos Rotativos (Flares de Energia)
        if (isFocused) {
            val rayCount = 6
            val rayLength = gemRadius * 1.85f
            val rotAngle = (time * 0.025f) % (2 * PI.toFloat())
            for (i in 0 until rayCount) {
                val angle = rotAngle + (i * 2 * PI.toFloat() / rayCount)
                val startX = center.x + cos(angle) * (gemRadius * 0.7f)
                val startY = center.y + sin(angle) * (gemRadius * 0.7f)
                val endX = center.x + cos(angle) * rayLength
                val endY = center.y + sin(angle) * rayLength

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            coreColor.copy(alpha = 0.8f + pulse * 0.2f),
                            glowColor.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY)
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round,
                    blendMode = BlendMode.SrcOver
                )
            }
        }

        // 3. Partículas Orbitais Cintilantes
        val particleCount = 8
        for (i in 0 until particleCount) {
            val pAngle = (time * 0.035f + (i * 2 * PI.toFloat() / particleCount))
            val pDistance = gemRadius * (1.2f + sin(time * 0.05f + i) * 0.25f)
            val px = center.x + cos(pAngle) * pDistance
            val py = center.y + sin(pAngle) * pDistance
            val pAlpha = (sin(time * 0.1f + i) + 1f) / 2f

            drawCircle(
                color = coreColor.copy(alpha = (pAlpha * 0.7f).coerceIn(0.1f, 0.9f)),
                radius = 2.2f,
                center = Offset(px, py),
                blendMode = BlendMode.SrcOver
            )
        }

        // 4. Núcleo Multifacetado da Joia (Geometria Cristalina Octogonal)
        drawFacetedGem(
            center = center,
            radius = gemRadius,
            primaryColor = primaryColor,
            glowColor = glowColor,
            coreColor = coreColor
        )
    }
}

private fun DrawScope.drawFacetedGem(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    glowColor: Color,
    coreColor: Color
) {
    val facets = 8
    val outerPoints = mutableListOf<Offset>()
    for (i in 0 until facets) {
        val angle = (i * 2 * PI / facets) - (PI / 8)
        val x = center.x + cos(angle).toFloat() * radius
        val y = center.y + sin(angle).toFloat() * radius
        outerPoints.add(Offset(x, y))
    }

    // Facetas triangulares de reflexão
    for (i in 0 until facets) {
        val nextIdx = (i + 1) % facets
        val facetPath = Path().apply {
            moveTo(center.x, center.y)
            lineTo(outerPoints[i].x, outerPoints[i].y)
            lineTo(outerPoints[nextIdx].x, outerPoints[nextIdx].y)
            close()
        }

        val shadeAlpha = if (i % 2 == 0) 0.85f else 0.60f
        drawPath(
            path = facetPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    coreColor.copy(alpha = 0.9f),
                    glowColor.copy(alpha = shadeAlpha),
                    primaryColor.copy(alpha = 0.95f)
                ),
                center = center,
                radius = radius
            )
        )
    }

    // Contorno do Cristal
    val fullPath = Path().apply {
        moveTo(outerPoints[0].x, outerPoints[0].y)
        for (i in 1 until facets) {
            lineTo(outerPoints[i].x, outerPoints[i].y)
        }
        close()
    }

    drawPath(
        path = fullPath,
        color = Color.White.copy(alpha = 0.65f),
        style = Stroke(width = 1.5f)
    )

    // Brilho especular superior (reflexo de prisma)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.75f),
                Color.White.copy(alpha = 0.1f),
                Color.Transparent
            ),
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f),
            radius = radius * 0.45f
        ),
        center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f),
        radius = radius * 0.45f
    )
}
