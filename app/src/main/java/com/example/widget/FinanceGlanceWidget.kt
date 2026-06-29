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
                        
                        val transactions = transactionsDef.await()
                        val bankAccounts = bankAccountsDef.await()
                        
                        totalIncome = transactions.filter { it.isIncome }.sumOf { it.value }
                        totalExpense = transactions.filter { !it.isIncome }.sumOf { it.value }
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

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1E252B))
            .appWidgetBackground()
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(openAppAction),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "FINANÇAS",
                style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFFF9A826), night = Color(0xFFF9A826)), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text("TESSERA", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x66FFFFFF), night = Color(0x66FFFFFF)), fontSize = 9.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text("Saldo Geral", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp))
        Text(
            text = "R$ ${String.format(Locale("pt", "BR"), "%,.2f", balance)}",
            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.height(12.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Receitas", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text("R$ ${String.format(Locale("pt", "BR"), "%,.0f", totalIncome)}", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF64FFDA), night = Color(0xFF64FFDA)), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                Text("Despesas", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text("R$ ${String.format(Locale("pt", "BR"), "%,.0f", totalExpense)}", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFFFF6B6B), night = Color(0xFFFF6B6B)), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}
