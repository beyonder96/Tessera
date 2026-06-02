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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Habit
import com.example.data.Medication
import kotlinx.coroutines.flow.first

class TesseraGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val habits = db.tesseraDao().getAllHabits().first()
        val medications = db.tesseraDao().getAllMedications().first()

        provideContent {
            WidgetContent(context = context, habits = habits, medications = medications)
        }
    }
}

@androidx.compose.runtime.Composable
fun WidgetContent(context: Context, habits: List<Habit>, medications: List<Medication>) {
    val openAppAction = androidx.glance.appwidget.action.actionStartActivity(
        android.content.Intent(context, MainActivity::class.java)
    )
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
            text = "TESSERA HUB",
            style = TextStyle(
                color = androidx.glance.color.ColorProvider(day = Color(0xFFD4AF37), night = Color(0xFFD4AF37)),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(16.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Hábitos Pendentes",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                val pendingHabits = habits.filter { !it.isCompleted }
                if (pendingHabits.isEmpty()) {
                    Text(text = "Tudo feito!", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF71D7CD), night = Color(0xFF71D7CD)), fontSize = 13.sp))
                } else {
                    pendingHabits.take(3).forEach { habit ->
                        Text(text = "• ${habit.name}", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 12.sp))
                    }
                }
            }

            Spacer(modifier = GlanceModifier.width(16.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Remédios",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                val pendingMeds = medications.filter { !it.isTaken }
                if (pendingMeds.isEmpty()) {
                    Text(text = "Nenhum pendente.", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF71D7CD), night = Color(0xFF71D7CD)), fontSize = 13.sp))
                } else {
                    pendingMeds.take(3).forEach { med ->
                        Text(text = "• ${med.name} (${med.time})", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 12.sp))
                    }
                }
            }
        }
    }
}
