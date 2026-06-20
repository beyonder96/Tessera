package com.example.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import java.util.Locale

class DailyGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var transactions = emptyList<com.example.data.Transaction>()
        var stepsRecords = emptyList<com.example.data.StepsRecord>()
        var petEvents = emptyList<com.example.data.PetEvent>()
        var marketItems = emptyList<com.example.data.MarketItem>()
        
        try {
            val db = AppDatabase.getDatabase(context)
            transactions = withContext(Dispatchers.IO) { db.tesseraDao().getAllTransactions().first() }
            stepsRecords = withContext(Dispatchers.IO) { db.tesseraDao().getAllStepsRecords().first() }
            petEvents = withContext(Dispatchers.IO) { db.tesseraDao().getAllPetEvents().first() }
            marketItems = withContext(Dispatchers.IO) { db.tesseraDao().getPendingMarketItems().first() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Balance
        val totalIncome = transactions.filter { it.isIncome }.sumOf { it.value }
        val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.value }
        val balance = totalIncome - totalExpense

        // Steps
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        val todaySteps = stepsRecords.filter { it.startTime >= todayStart && it.endTime <= todayEnd }.sumOf { it.count }

        // Pet events
        val completedPetEvents = petEvents.count { it.isCompleted }
        val totalPetEvents = petEvents.size

        // Market pending count
        val pendingMarketCount = marketItems.size

        provideContent {
            DailyWidgetContent(
                context = context,
                balance = balance,
                steps = todaySteps,
                petCompleted = completedPetEvents,
                petTotal = totalPetEvents,
                marketPending = pendingMarketCount
            )
        }
    }
}

@androidx.compose.runtime.Composable
fun DailyWidgetContent(
    context: Context,
    balance: Double,
    steps: Long,
    petCompleted: Int,
    petTotal: Int,
    marketPending: Int
) {
    val openAppAction = androidx.glance.appwidget.action.actionStartActivity(
        android.content.Intent(context, MainActivity::class.java).apply {
            putExtra("route", "daily")
        }
    )

    val calendar = Calendar.getInstance()
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))
    val weekDayStr = when (dayOfWeek) {
        Calendar.SUNDAY -> "Domingo"
        Calendar.MONDAY -> "Segunda-feira"
        Calendar.TUESDAY -> "Terça-feira"
        Calendar.WEDNESDAY -> "Quarta-feira"
        Calendar.THURSDAY -> "Quinta-feira"
        Calendar.FRIDAY -> "Sexta-feira"
        else -> "Sábado"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(16.dp)
            .clickable(openAppAction),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DAILY BRIEF",
                style = TextStyle(
                    color = androidx.glance.color.ColorProvider(day = Color(0xFF71D7CD), night = Color(0xFF71D7CD)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "TESSERA",
                style = TextStyle(
                    color = androidx.glance.color.ColorProvider(day = Color(0x66FFFFFF), night = Color(0x66FFFFFF)),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(8.dp))

        Text(
            text = "$weekDayStr, $dayOfMonth de $monthName".uppercase(),
            style = TextStyle(
                color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "Seu dia em resumo",
            style = TextStyle(
                color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Balance / Finances
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(text = "Saldo", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text(text = "R$ ${String.format(Locale("pt", "BR"), "%,.0f", balance)}", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            // Health steps
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(text = "Passos", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text(text = "$steps", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            // Pets events
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(text = "Petz", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text(text = "$petCompleted/$petTotal", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            // Market pending
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(text = "Mercado", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text(text = "$marketPending itens", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}
