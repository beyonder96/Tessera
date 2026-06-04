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
import com.example.data.PetEvent
import kotlinx.coroutines.flow.first

class PetsGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val events = db.tesseraDao().getAllPetEvents().first()

        provideContent {
            PetsWidgetContent(context = context, events = events)
        }
    }
}

@androidx.compose.runtime.Composable
fun PetsWidgetContent(context: Context, events: List<PetEvent>) {
    val openAppAction = androidx.glance.appwidget.action.actionStartActivity(
        android.content.Intent(context, MainActivity::class.java).apply {
            putExtra("route", "petz")
        }
    )
    val completedCount = events.count { it.isCompleted }
    val totalCount = events.size
    val nextEvent = events.find { !it.isCompleted }

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
                text = "PETZ",
                style = TextStyle(
                    color = androidx.glance.color.ColorProvider(day = Color(0xFFD7B4F3), night = Color(0xFFD7B4F3)),
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
            text = "Atividades de Hoje",
            style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp)
        )
        Text(
            text = "$completedCount de $totalCount concluídas",
            style = TextStyle(
                color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(10.dp))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            if (nextEvent != null) {
                Text(
                    text = "Próxima atividade:",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp)
                )
                Text(
                    text = "${nextEvent.petName}: ${nextEvent.title} às ${nextEvent.time}",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            } else {
                Text(
                    text = "Todas as atividades concluídas!",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF71D7CD), night = Color(0xFF71D7CD)), fontSize = 13.sp)
                )
            }
        }
    }
}
