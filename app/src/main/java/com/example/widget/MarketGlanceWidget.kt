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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.MarketItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class MarketGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val items = withContext(Dispatchers.IO) { db.tesseraDao().getPendingMarketItems().first() }

        provideContent {
            MarketWidgetContent(context = context, items = items)
        }
    }
}

@androidx.compose.runtime.Composable
fun MarketWidgetContent(context: Context, items: List<MarketItem>) {
    val openAppAction = androidx.glance.appwidget.action.actionStartActivity(
        android.content.Intent(context, MainActivity::class.java).apply {
            putExtra("route", "market")
        }
    )

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
                text = "MERCADO",
                style = TextStyle(
                    color = androidx.glance.color.ColorProvider(day = Color(0xFF4D96FF), night = Color(0xFF4D96FF)),
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
            text = "Lista de Compras",
            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp)
        )
        Text(
            text = "${items.size} itens pendentes",
            style = TextStyle(
                color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(10.dp))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            if (items.isEmpty()) {
                Text(
                    text = "Tudo comprado!",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF71D7CD), night = Color(0xFF71D7CD)), fontSize = 13.sp)
                )
            } else {
                items.take(2).forEach { item ->
                    val qtyText = if (item.unit == "Kg") "${item.quantity} Kg" else "${item.quantity.toInt()} un"
                    Text(
                        text = "• ${item.name} ($qtyText)",
                        style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xE6FFFFFF), night = Color(0xE6FFFFFF)), fontSize = 12.sp)
                    )
                }
                if (items.size > 2) {
                    Text(
                        text = "+ ${items.size - 2} outros itens...",
                        style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x80FFFFFF), night = Color(0x80FFFFFF)), fontSize = 11.sp)
                    )
                }
            }
        }
    }
}
