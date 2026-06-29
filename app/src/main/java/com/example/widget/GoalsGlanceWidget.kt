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
                text = "METAS E HÁBITOS",
                style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFFF9A826), night = Color(0xFFF9A826)), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text("TESSERA", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x66FFFFFF), night = Color(0x66FFFFFF)), fontSize = 9.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Rituais Diários", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp))
                Text(
                    text = "$completedHabits de $totalHabits concluídos",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                Text("Rotinas Chronos", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp))
                Text(
                    text = "$routinesCount ativas hoje",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(12.dp))
        Text("Deseja focar agora?", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 11.sp))
        Text("Toque para abrir e iniciar Pomodoro ou Chronos.", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x66FFFFFF), night = Color(0x66FFFFFF)), fontSize = 10.sp))
    }
}
