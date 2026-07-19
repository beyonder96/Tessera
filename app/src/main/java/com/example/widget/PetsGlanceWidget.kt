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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class PetsGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var completedCount = 0
        var totalCount = 0
        var nextEventText = ""
        
        try {
            withTimeoutOrNull(2000) {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    val events = db.tesseraDao().getAllPetEvents().first()
                    completedCount = events.count { it.isCompleted }
                    totalCount = events.size
                    val nextEvent = events.find { !it.isCompleted }
                    if (nextEvent != null) {
                        nextEventText = "${nextEvent.petName}: ${nextEvent.title} às ${nextEvent.time}"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        provideContent {
            PetsWidgetContent(context, completedCount, totalCount, nextEventText)
        }
    }
}

@androidx.compose.runtime.Composable
fun PetsWidgetContent(context: Context, completedCount: Int, totalCount: Int, nextEventText: String) {
    val openAppAction = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            putExtra("route", "petz")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .appWidgetBackground()
            .cornerRadius(32.dp)
            .padding(16.dp)
            .clickable(openAppAction),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "PETZ",
                style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFFD7B4F3), night = Color(0xFFD7B4F3)), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text("TESSERA", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x66FFFFFF), night = Color(0x66FFFFFF)), fontSize = 9.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = GlanceModifier.height(8.dp))

        Text("Atividades de Hoje", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp))
        Text(
            text = "$completedCount de $totalCount concluídas",
            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = GlanceModifier.height(10.dp))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            if (nextEventText.isNotEmpty()) {
                Text("Próxima atividade:", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text(nextEventText, style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            } else {
                Text("Todas as atividades concluídas!", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF64FFDA), night = Color(0xFF64FFDA)), fontSize = 13.sp))
            }
        }
    }
}
