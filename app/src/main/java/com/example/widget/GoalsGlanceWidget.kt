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
import com.example.data.Habit
import com.example.data.Routine
import kotlinx.coroutines.flow.first

class GoalsGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val habits = db.tesseraDao().getAllHabits().first()
        val routines = db.tesseraDao().getAllRoutines().first()

        provideContent {
            GoalsWidgetContent(context = context, habits = habits, routines = routines)
        }
    }
}

@androidx.compose.runtime.Composable
fun GoalsWidgetContent(context: Context, habits: List<Habit>, routines: List<Routine>) {
    val openAppAction = androidx.glance.appwidget.action.actionStartActivity(
        android.content.Intent(context, MainActivity::class.java).apply {
            putExtra("route", "goals")
        }
    )
    val completedHabits = habits.count { it.isCompleted }
    val totalHabits = habits.size

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
                text = "METAS E HÁBITOS",
                style = TextStyle(
                    color = androidx.glance.color.ColorProvider(day = Color(0xFFF9A826), night = Color(0xFFF9A826)),
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

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            // Rituais
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Rituais Diários", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp))
                Text(
                    text = "$completedHabits de $totalHabits concluídos",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
            // Routines
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                Text("Rotinas Chronos", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp))
                Text(
                    text = "${routines.size} ativas hoje",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(12.dp))
        Text(
            text = "Deseja focar agora?",
            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 11.sp)
        )
        Text(
            text = "Toque para abrir e iniciar Pomodoro ou Chronos.",
            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x66FFFFFF), night = Color(0x66FFFFFF)), fontSize = 10.sp)
        )
    }
}
