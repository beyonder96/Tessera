package com.example

import androidx.compose.ui.text.TextStyle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Habit
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ZenithScreen(
    onHomeClick: () -> Unit, 
    viewModel: TesseraViewModel,
    initialPage: Int = 0
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, 1),
        pageCount = { 2 }
    )

    val lembretesLazyListState = rememberLazyListState()
    val chronosLazyListState = rememberLazyListState()

    val isCompact by remember(pagerState.currentPage) {
        derivedStateOf {
            when (pagerState.currentPage) {
                1 -> chronosLazyListState.firstVisibleItemIndex > 0 || chronosLazyListState.firstVisibleItemScrollOffset > 100
                else -> lembretesLazyListState.firstVisibleItemIndex > 0 || lembretesLazyListState.firstVisibleItemScrollOffset > 100
            }
        }
    }

    val normalAlpha by animateFloatAsState(targetValue = if (isCompact) 0f else 1f, animationSpec = tween(250), label = "normalAlpha")
    val compactAlpha by animateFloatAsState(targetValue = if (isCompact) 1f else 0f, animationSpec = tween(250), label = "compactAlpha")

    val accentColor = when (pagerState.currentPage) {
        1 -> Color(0xFF71D7CD)
        else -> Color(0xFFF9A826)
    }

    val titleText = when (pagerState.currentPage) {
        1 -> "Rotinas"
        else -> "Lembretes"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFF070909),
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {}
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(modifier = Modifier.height(72.dp))

                // Internal navigation tabs — 2 tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141918))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Lembretes", "Rotinas").forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        val bgSelectedColor = when (index) {
                            1 -> Color(0xFF0F2624) // Teal dark
                            else -> Color(0xFF26200F) // Gold/Black
                        }
                        val textAccentColor = when (index) {
                            1 -> Color(0xFF71D7CD)
                            else -> Color(0xFFF9A826)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) bgSelectedColor else Color.Transparent)
                                .bounceClick { 
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) textAccentColor else Color(0xFF81928F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    // Parallax effect on page content
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val alphaValue = (1f - kotlin.math.abs(pageOffset)).coerceIn(0f, 1f)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = alphaValue
                                translationX = pageOffset * 100f
                            }
                    ) {
                        when (page) {
                            1 -> ChronosScreen(viewModel = viewModel, listState = chronosLazyListState)
                            else -> LembretesTab(viewModel = viewModel, listState = lembretesLazyListState)
                        }
                    }
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
                            text = titleText.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Espaço reservado para ações futuras
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
                        imageVector = when (pagerState.currentPage) {
                            1 -> Icons.Outlined.HourglassEmpty
                            else -> Icons.Outlined.Notifications
                        },
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
                        text = titleText.uppercase(),
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
    }
}

@Composable
fun LembretesTab(viewModel: TesseraViewModel, listState: LazyListState) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            SectionHeader("LEMBRETES", Icons.Outlined.Notifications)
        }

        // Placeholder para integração com Google Calendar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141918))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFFF9A826),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Integração com Google Calendar",
                        color = Color(0xFFBDC9C6),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Em breve você poderá sincronizar seus lembretes com o Google Calendar",
                        color = Color(0xFF81928F),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF81928F), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color(0xFF81928F)
        )
    }
}
