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
import androidx.glance.color.ColorProvider
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
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class FinanceGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var totalIncome = 0.0
        var totalExpense = 0.0
        var balance = 0.0
        
        try {
            withTimeoutOrNull(2000) {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    coroutineScope {
                        val transactionsDef = async { db.tesseraDao().getAllTransactions().first() }
                        val bankAccountsDef = async { db.tesseraDao().getAllBankAccounts().first() }
                        val benefitCardsDef = async { db.tesseraDao().getAllBenefitCards().first() }
                        
                        val transactions = transactionsDef.await()
                        val bankAccounts = bankAccountsDef.await()
                        val benefitCards = benefitCardsDef.await()
                        
                        val currentMonthStart = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.DAY_OF_MONTH, 1)
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val currentMonthEnd = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                            set(java.util.Calendar.HOUR_OF_DAY, 23)
                            set(java.util.Calendar.MINUTE, 59)
                            set(java.util.Calendar.SECOND, 59)
                            set(java.util.Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        val currentMonthTransactions = transactions.filter { it.timestamp in currentMonthStart..currentMonthEnd }

                        totalIncome = currentMonthTransactions.filter { tx ->
                            tx.isIncome && benefitCards.none { card -> card.name == tx.accountOrCardName }
                        }.sumOf { it.value }
                        totalExpense = currentMonthTransactions.filter { tx ->
                            !tx.isIncome && benefitCards.none { card -> card.name == tx.accountOrCardName }
                        }.sumOf { it.value }
                        balance = bankAccounts.sumOf { it.balance }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        provideContent {
            FinanceWidgetContent(context, totalIncome, totalExpense, balance)
        }
    }
}

@androidx.compose.runtime.Composable
fun FinanceWidgetContent(context: Context, totalIncome: Double, totalExpense: Double, balance: Double) {
    val openAppAction = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            putExtra("route", "finance")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    )

    val bgProvider = ColorProvider(day = Color(0xF8FFFFFF), night = Color(0xF0121316))
    val textPrimary = ColorProvider(day = Color(0xFF0F172A), night = Color(0xFFF8FAFC))
    val textSecondary = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))
    val accentGold = ColorProvider(day = Color(0xFFD97706), night = Color(0xFFF59E0B))
    val incomeGreen = ColorProvider(day = Color(0xFF059669), night = Color(0xFF10B981))
    val expenseRed = ColorProvider(day = Color(0xFFDC2626), night = Color(0xFFEF4444))
    val cardSurface = ColorProvider(day = Color(0x0F0F172A), night = Color(0x18FFFFFF))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgProvider)
            .appWidgetBackground()
            .cornerRadius(18.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(openAppAction),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FINANÇAS",
                style = TextStyle(color = accentGold, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "TESSERA",
                style = TextStyle(color = textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(cardSurface)
                .cornerRadius(12.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Saldo Geral",
                    style = TextStyle(color = textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "R$ ${String.format(Locale("pt", "BR"), "%,.2f", balance)}",
                    style = TextStyle(color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+ R$ ${String.format(Locale("pt", "BR"), "%,.0f", totalIncome)}",
                        style = TextStyle(color = incomeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "- R$ ${String.format(Locale("pt", "BR"), "%,.0f", totalExpense)}",
                        style = TextStyle(color = expenseRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
