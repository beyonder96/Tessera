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

    val bgProvider = ColorProvider(day = Color(0xF8FFFFFF), night = Color(0xF0121316))
    val textPrimary = ColorProvider(day = Color(0xFF0F172A), night = Color(0xFFF8FAFC))
    val textSecondary = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))
    val accentPink = ColorProvider(day = Color(0xFFC026D3), night = Color(0xFFE879F9))
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
                text = "PETZ",
                style = TextStyle(color = accentPink, fontSize = 9.sp, fontWeight = FontWeight.Bold),
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
                    text = "Atividades de Hoje",
                    style = TextStyle(color = textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "$completedCount de $totalCount concluídas",
                    style = TextStyle(color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }

            if (nextEventText.isNotEmpty()) {
                Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Próxima:",
                        style = TextStyle(color = textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = nextEventText,
                        style = TextStyle(color = textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
