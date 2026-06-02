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
import com.example.data.Medication
import com.example.data.WeightRecord
import kotlinx.coroutines.flow.first

class HealthGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val medications = db.tesseraDao().getAllMedications().first()
        val weightRecords = db.tesseraDao().getAllWeightRecords().first()

        provideContent {
            HealthWidgetContent(context = context, medications = medications, weightRecords = weightRecords)
        }
    }
}

@androidx.compose.runtime.Composable
fun HealthWidgetContent(context: Context, medications: List<Medication>, weightRecords: List<WeightRecord>) {
    val openAppAction = androidx.glance.appwidget.action.actionStartActivity(
        android.content.Intent(context, MainActivity::class.java)
    )
    val latestWeight = weightRecords.lastOrNull()?.weightKg
    val pendingMeds = medications.filter { !it.isTaken }

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
            text = "TESSERA SAÚDE",
            style = TextStyle(
                color = androidx.glance.color.ColorProvider(day = Color(0xFFD4AF37), night = Color(0xFFD4AF37)),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(16.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Peso Atual", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 11.sp))
                Text(
                    text = if (latestWeight != null) "${String.format("%.1f", latestWeight)} kg" else "--", 
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
            }
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                Text("Remédios", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 11.sp))
                Text(
                    text = "${pendingMeds.size} pendentes", 
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFFE57373), night = Color(0xFFE57373)), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
