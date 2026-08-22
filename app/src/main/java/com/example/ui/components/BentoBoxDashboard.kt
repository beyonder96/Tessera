package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.viewmodel.TesseraViewModel
import kotlin.math.*

data class InfinityStoneModule(
    val id: String,
    val stoneName: String,
    val moduleTitle: String,
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
    val itemsAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "bento_alpha"
    )
    val itemsScale by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "bento_scale"
    )

    // Estrutura das 9 Joias do Infinito do Tessera
    val infinityStones = remember {
        listOf(
            InfinityStoneModule(
                id = "reality",
                stoneName = "Joia da Realidade",
                moduleTitle = "Finanças",
                primaryColor = Color(0xFFEF4444),
                glowColor = Color(0xFFF87171),
                coreColor = Color(0xFFFFE4E6),
                route = "finance"
            ),
            InfinityStoneModule(
                id = "time",
                stoneName = "Joia do Tempo",
                moduleTitle = "Lista de Mercado",
                primaryColor = Color(0xFF10B981),
                glowColor = Color(0xFF34D399),
                coreColor = Color(0xFFD1FAE5),
                route = "market"
            ),
            InfinityStoneModule(
                id = "power",
                stoneName = "Joia do Poder",
                moduleTitle = "Rotinas & Hábitos",
                primaryColor = Color(0xFFA855F7),
                glowColor = Color(0xFFC084FC),
                coreColor = Color(0xFFF3E8FF),
                route = "goals"
            ),
            InfinityStoneModule(
                id = "space",
                stoneName = "Joia do Espaço",
                moduleTitle = "Transporte SP",
                primaryColor = Color(0xFF0284C7),
                glowColor = Color(0xFF38BDF8),
                coreColor = Color(0xFFE0F2FE),
                route = "transport"
            ),
            InfinityStoneModule(
                id = "mind",
                stoneName = "Joia da Mente",
                moduleTitle = "Bíblia Sagrada",
                primaryColor = Color(0xFFEAB308),
                glowColor = Color(0xFFFDE047),
                coreColor = Color(0xFFFEF9C3),
                route = "bible"
            ),
            InfinityStoneModule(
                id = "life",
                stoneName = "Joia da Vida",
                moduleTitle = "Saúde & Nutri",
                primaryColor = Color(0xFF06B6D4),
                glowColor = Color(0xFF22D3EE),
                coreColor = Color(0xFFCFFAFE),
                route = "health"
            ),
            InfinityStoneModule(
                id = "soul",
                stoneName = "Joia da Alma",
                moduleTitle = "Desejos & Metas",
                primaryColor = Color(0xFFF97316),
                glowColor = Color(0xFFFB923C),
                coreColor = Color(0xFFFFEDD5),
                route = "wishes"
            ),
            InfinityStoneModule(
                id = "companion",
                stoneName = "Joia do Afeto",
                moduleTitle = "Petz",
                primaryColor = Color(0xFFEC4899),
                glowColor = Color(0xFFF472B6),
                coreColor = Color(0xFFFCE7F3),
                route = "petz"
            ),
            InfinityStoneModule(
                id = "territory",
                stoneName = "Joia da Fundação",
                moduleTitle = "Meu Apê",
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = itemsScale
                scaleY = itemsScale
                this.alpha = itemsAlpha
            },
        contentAlignment = Alignment.Center
    ) {
        // Carrossel Horizontal das Joias Cósmicas soltas no espaço
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentPadding = PaddingValues(horizontal = 64.dp),
            pageSpacing = 0.dp
        ) { page ->
            val stone = infinityStones[page]

            // Cálculo de Parallax / Escala suave
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val scale = lerp(1.0f, 0.72f, pageOffset.coerceIn(0f, 1f))
            val alpha = lerp(1.0f, 0.22f, pageOffset.coerceIn(0f, 1f))

            InfinityStoneItem(
                stone = stone,
                time = time,
                scale = scale,
                alpha = alpha,
                isFocused = pagerState.currentPage == page,
                onClick = { onNavigate(stone.route) }
            )
        }
    }
}

// ==============================================================================
// ITEM SOLTO DA JOIA DO INFINITO (Minimalismo Puro)
// ==============================================================================
@Composable
private fun InfinityStoneItem(
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
        targetValue = if (isPressed) 0.94f else scale,
        animationSpec = tween(150),
        label = "clickScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = clickScale
                scaleY = clickScale
                this.alpha = alpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp)
    ) {
        // Joia Cósmica solta no espaço ampliada
        Box(
            modifier = Modifier.size(320.dp),
            contentAlignment = Alignment.Center
        ) {
            InfinityStoneCanvas(
                primaryColor = stone.primaryColor,
                glowColor = stone.glowColor,
                coreColor = stone.coreColor,
                time = time,
                isFocused = isFocused
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Nome místico sutil da joia
        Text(
            text = stone.stoneName.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = stone.glowColor.copy(alpha = if (isFocused) 0.95f else 0.35f),
            letterSpacing = 2.5.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Nome do módulo em destaque limpo
        Text(
            text = stone.moduleTitle,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = if (isFocused) 1.0f else 0.45f),
            textAlign = TextAlign.Center
        )
    }
}

// ==============================================================================
// CANVAS DA JOIA CÓSMICA (Física de Raios, Flutuação e Brilho Cristalino)
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
        val floatOffset = sin(time * 0.06f) * 10f
        val centerY = (size.height / 2f) + floatOffset
        val center = Offset(centerX, centerY)

        val pulse = (sin(time * 0.08f) + 1f) / 2f
        val gemRadius = size.width * 0.30f
        val haloRadius = size.width * 0.48f + (if (isFocused) pulse * 14f else 0f)

        // 1. Halo Luminoso Externo (Glow Cósmico)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = if (isFocused) 0.52f + pulse * 0.16f else 0.25f),
                    primaryColor.copy(alpha = if (isFocused) 0.32f else 0.10f),
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
            val rayLength = gemRadius * 1.75f
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
                            coreColor.copy(alpha = 0.90f + pulse * 0.10f),
                            glowColor.copy(alpha = 0.40f),
                            Color.Transparent
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY)
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round,
                    blendMode = BlendMode.SrcOver
                )
            }
        }

        // 3. Partículas Orbitais Cintilantes
        val particleCount = 8
        for (i in 0 until particleCount) {
            val pAngle = (time * 0.035f + (i * 2 * PI.toFloat() / particleCount))
            val pDistance = gemRadius * (1.25f + sin(time * 0.05f + i) * 0.25f)
            val px = center.x + cos(pAngle) * pDistance
            val py = center.y + sin(pAngle) * pDistance
            val pAlpha = (sin(time * 0.1f + i) + 1f) / 2f

            drawCircle(
                color = coreColor.copy(alpha = (pAlpha * 0.75f).coerceIn(0.15f, 0.95f)),
                radius = 3.5f,
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
