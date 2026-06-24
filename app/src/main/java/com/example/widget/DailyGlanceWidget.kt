package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.Locale

class DailyGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var balance = 0.0
        var todaySteps = 0L
        var completedPetEvents = 0
        var totalPetEvents = 0
        var pendingMarketCount = 0

        try {
            withTimeoutOrNull(2000) {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    coroutineScope {
                        val transactionsDef = async { db.tesseraDao().getAllTransactions().first() }
                        val stepsDef = async { db.tesseraDao().getAllStepsRecords().first() }
                        val petsDef = async { db.tesseraDao().getAllPetEvents().first() }
                        val marketDef = async { db.tesseraDao().getPendingMarketItems().first() }

                        val transactions = transactionsDef.await()
                        val stepsRecords = stepsDef.await()
                        val petEvents = petsDef.await()
                        val marketItems = marketDef.await()

                        balance = transactions.filter { it.isIncome }.sumOf { it.value } - transactions.filter { !it.isIncome }.sumOf { it.value }
                        
                        val todayStart = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val todayEnd = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        todaySteps = stepsRecords.filter { it.startTime in todayStart..todayEnd }.sumOf { it.count }
                        
                        completedPetEvents = petEvents.count { it.isCompleted }
                        totalPetEvents = petEvents.size
                        pendingMarketCount = marketItems.size
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        provideContent {
            DailyWidgetContent(context, balance, todaySteps, completedPetEvents, totalPetEvents, pendingMarketCount)
        }
    }
}

@androidx.compose.runtime.Composable
fun DailyWidgetContent(context: Context, balance: Double, steps: Long, petCompleted: Int, petTotal: Int, marketPending: Int) {
    val openAppAction = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            putExtra("route", "daily")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    )

    val calendar = Calendar.getInstance()
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "BR"))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF0D1517))
            .appWidgetBackground()
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(openAppAction),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "DAILY BRIEF",
                style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF71D7CD), night = Color(0xFF71D7CD)), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text("TESSERA", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x66FFFFFF), night = Color(0x66FFFFFF)), fontSize = 9.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "$dayOfMonth de $monthName".uppercase(),
            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Seu dia em resumo",
            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.height(12.dp))
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Saldo", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text("R$ ${String.format(Locale("pt", "BR"), "%,.0f", balance)}", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Passos", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text("$steps", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Petz", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text("$petCompleted/$petTotal", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Mercado", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text("$marketPending", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}
