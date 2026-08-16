package com.example
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.*

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.PremiumGlassModifier
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.viewmodel.TesseraViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Data class para eventos do calendário
data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean,
    val calendarColor: Int,
    val calendarName: String?,
    val location: String?
)

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
    val thermalBrush = Brush.linearGradient(listOf(Color(0xFFec4899), Color(0xFFf97316)))

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
            containerColor = MaterialTheme.colorScheme.background,
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
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0x801E1E1E))
                        .border(1.dp, Color.White.copy(alpha=0.08f), RoundedCornerShape(32.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Lembretes", "Rotinas").forEachIndexed { index, title ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .then(if (isSelected) Modifier.background(thermalBrush) else Modifier.background(Color.Transparent))
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
                                color = if (isSelected) Color.White else Color.White.copy(alpha=0.6f),
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
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 2.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {}
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
                            else -> Icons.Outlined.CalendarMonth
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
                            Color.White, accentColor, Color.White, accentColor, Color.White
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

// ======================== LEMBRETES TAB — Google Calendar ========================

fun loadCalendarEvents(context: Context, daysAhead: Int = 7): List<CalendarEvent> {
    val events = mutableListOf<CalendarEvent>()
    
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        return events
    }

    try {
        val now = Calendar.getInstance()
        val startMillis = now.timeInMillis
        val endCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, daysAhead) }
        val endMillis = endCal.timeInMillis

        // Query Instances for recurring events support
        val instancesUri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startMillis.toString())
            .appendPath(endMillis.toString())
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.EVENT_LOCATION
        )

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        context.contentResolver.query(instancesUri, projection, null, null, sortOrder)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val descIdx = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
            val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
            val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            val colorIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_COLOR)
            val calNameIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
            val locationIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

            while (cursor.moveToNext()) {
                val title = if (titleIdx >= 0) cursor.getString(titleIdx) else null
                if (title.isNullOrBlank()) continue

                events.add(
                    CalendarEvent(
                        id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L,
                        title = title,
                        description = if (descIdx >= 0) cursor.getString(descIdx) else null,
                        startTime = if (beginIdx >= 0) cursor.getLong(beginIdx) else 0L,
                        endTime = if (endIdx >= 0) cursor.getLong(endIdx) else 0L,
                        allDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false,
                        calendarColor = if (colorIdx >= 0) cursor.getInt(colorIdx) else 0xFF4285F4.toInt(),
                        calendarName = if (calNameIdx >= 0) cursor.getString(calNameIdx) else null,
                        location = if (locationIdx >= 0) cursor.getString(locationIdx) else null
                    )
                )
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("CalendarEvents", "Error loading calendar events", e)
    }

    return events
}

@Composable
fun LembretesTab(viewModel: TesseraViewModel, listState: LazyListState) {
    val context = LocalContext.current
    
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            events = loadCalendarEvents(context)
        }
    }

    // Load events when permission is available
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            events = loadCalendarEvents(context)
        }
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale("pt", "BR")) }
    val dayFormat = remember { SimpleDateFormat("EEE, dd MMM", Locale("pt", "BR")) }

    // Group events by day
    val groupedEvents = remember(events) {
        events.groupBy { event ->
            val cal = Calendar.getInstance().apply { timeInMillis = event.startTime }
            val today = Calendar.getInstance()
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

            when {
                cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Hoje"
                cal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> "Amanhã"
                else -> dayFormat.format(Date(event.startTime)).replaceFirstChar { it.uppercase() }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        if (!hasPermission) {
            // Permission request card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(themedCardBackground())
                        .border(1.dp, Color(0xFFF9A826).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF9A826).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFFF9A826),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "Conectar ao Google Calendar",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Permita o acesso ao calendário para ver seus compromissos e lembretes aqui",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF9A826),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Permitir Acesso", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        } else if (events.isEmpty()) {
            // Empty state
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(themedCardBackground())
                        .border(1.dp, themedCardBorder(), RoundedCornerShape(24.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EventAvailable,
                            contentDescription = null,
                            tint = Color(0xFF71D7CD),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Agenda livre!",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Nenhum evento nos próximos 7 dias",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            // Render grouped events
            groupedEvents.forEach { (dayLabel, dayEvents) ->
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (dayLabel == "Hoje") Color(0xFFF9A826)
                                    else if (dayLabel == "Amanhã") Color(0xFF71D7CD)
                                    else Color(0xFF81928F)
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = dayLabel.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = if (dayLabel == "Hoje") Color(0xFFF9A826)
                                    else if (dayLabel == "Amanhã") Color(0xFF71D7CD)
                                    else Color(0xFF81928F)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${dayEvents.size} evento${if (dayEvents.size > 1) "s" else ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(dayEvents, key = { "${it.id}_${it.startTime}" }) { event ->
                    CalendarEventCard(event = event, dateFormat = dateFormat)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun CalendarEventCard(event: CalendarEvent, dateFormat: SimpleDateFormat) {
    val eventColor = Color(event.calendarColor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(themedCardBackground())
            .border(1.dp, themedCardBorder(), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Color indicator line
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(eventColor)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Title
            Text(
                text = event.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (event.allDay) "Dia inteiro"
                           else "${dateFormat.format(Date(event.startTime))} — ${dateFormat.format(Date(event.endTime))}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            // Location
            if (!event.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.location,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Calendar name
            if (!event.calendarName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(eventColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.calendarName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
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
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
