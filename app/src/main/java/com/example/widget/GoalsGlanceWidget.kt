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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class GoalsGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var completedHabits = 0
        var totalHabits = 0
        var routinesCount = 0

        try {
            withTimeoutOrNull(2000) {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    coroutineScope {
                        val habitsDef = async { db.tesseraDao().getAllHabits().first() }
                        val routinesDef = async { db.tesseraDao().getAllRoutines().first() }
                        
                        val habits = habitsDef.await()
                        val routines = routinesDef.await()

                        completedHabits = habits.count { it.isCompleted }
                        totalHabits = habits.size
                        routinesCount = routines.size
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        provideContent {
            GoalsWidgetContent(context, completedHabits, totalHabits, routinesCount)
        }
    }
}

@androidx.compose.runtime.Composable
fun GoalsWidgetContent(context: Context, completedHabits: Int, totalHabits: Int, routinesCount: Int) {
    val openAppAction = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            putExtra("route", "goals")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    )

    val bgProvider = ColorProvider(day = Color(0xF8FFFFFF), night = Color(0xF0121316))
    val textPrimary = ColorProvider(day = Color(0xFF0F172A), night = Color(0xFFF8FAFC))
    val textSecondary = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))
    val accentPurple = ColorProvider(day = Color(0xFF7C3AED), night = Color(0xFFA78BFA))
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
                text = "METAS & FOCO",
                style = TextStyle(color = accentPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold),
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
                    text = "Rituais Diários",
                    style = TextStyle(color = textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "$completedHabits de $totalHabits concluídos",
                    style = TextStyle(color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }

            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                Text(
                    text = "Rotinas Chronos",
                    style = TextStyle(color = textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "$routinesCount ativas",
                    style = TextStyle(color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
