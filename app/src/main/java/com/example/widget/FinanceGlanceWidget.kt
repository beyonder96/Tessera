package com.example.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Transaction
import kotlinx.coroutines.flow.first

class FinanceGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val transactions = db.tesseraDao().getAllTransactions().first()

        provideContent {
            FinanceWidgetContent(context = context, transactions = transactions)
        }
    }
}

@androidx.compose.runtime.Composable
fun FinanceWidgetContent(context: Context, transactions: List<Transaction>) {
    val openAppAction = androidx.glance.appwidget.action.actionStartActivity(
        android.content.Intent(context, MainActivity::class.java)
    )
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.value }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.value }
    val balance = totalIncome - totalExpense

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(androidx.glance.color.ColorProvider(day = Color(0xCC070909), night = Color(0xCC070909)))
            .padding(20.dp)
            .clickable(openAppAction),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "TESSERA FINANCE",
            style = TextStyle(
                color = androidx.glance.color.ColorProvider(day = Color(0xFFD4AF37), night = Color(0xFFD4AF37)),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(16.dp))

        Text(
            text = "Saldo Atual",
            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 12.sp)
        )
        Text(
            text = "R$ ${String.format("%.2f", balance)}",
            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = GlanceModifier.height(16.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Receitas", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 11.sp))
                Text("R$ ${String.format("%.2f", totalIncome)}", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF71D7CD), night = Color(0xFF71D7CD)), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                Text("Despesas", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 11.sp))
                Text("R$ ${String.format("%.2f", totalExpense)}", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFFE57373), night = Color(0xFFE57373)), fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}
