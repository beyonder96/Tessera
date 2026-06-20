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
import com.example.data.Medication
import com.example.data.WeightRecord
import com.example.data.StepsRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import java.util.Locale

class HealthGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var medications = emptyList<com.example.data.Medication>()
        var weightRecords = emptyList<com.example.data.WeightRecord>()
        var stepsRecords = emptyList<com.example.data.StepsRecord>()
        
        try {
            val db = AppDatabase.getDatabase(context)
            medications = withContext(Dispatchers.IO) { db.tesseraDao().getAllMedications().first() }
            weightRecords = withContext(Dispatchers.IO) { db.tesseraDao().getAllWeightRecords().first() }
            stepsRecords = withContext(Dispatchers.IO) { db.tesseraDao().getAllStepsRecords().first() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Read Vibe from SharedPreferences
        val sharedPrefs = context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)
        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val savedFeelingDate = sharedPrefs.getString("feeling_date", "")
        val vibe = if (savedFeelingDate == todayDate) {
            sharedPrefs.getString("user_feeling", "Pendente") ?: "Pendente"
        } else {
            "Pendente"
        }

        // Steps today
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        val todaySteps = stepsRecords.filter { it.startTime >= todayStart && it.endTime <= todayEnd }.sumOf { it.count }

        val latestWeight = weightRecords.lastOrNull()?.weightKg ?: 75.2

        provideContent {
            HealthWidgetContent(
                context = context,
                medications = medications,
                latestWeight = latestWeight,
                todaySteps = todaySteps,
                vibe = vibe
            )
        }
    }
}

@androidx.compose.runtime.Composable
fun HealthWidgetContent(
    context: Context,
    medications: List<Medication>,
    latestWeight: Double,
    todaySteps: Long,
    vibe: String
) {
    val openAppAction = androidx.glance.appwidget.action.actionStartActivity(
        android.content.Intent(context, MainActivity::class.java).apply {
            putExtra("route", "health")
        }
    )

    // Find closest unchecked medication
    val uncheckedMeds = medications.filter { !it.isTaken }
    val nextMed = if (uncheckedMeds.isNotEmpty()) {
        val nowStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val futureMeds = uncheckedMeds.filter { it.time >= nowStr }.sortedBy { it.time }
        if (futureMeds.isNotEmpty()) {
            futureMeds.first()
        } else {
            uncheckedMeds.sortedBy { it.time }.first()
        }
    } else {
        null
    }

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
                text = "SAÚDE",
                style = TextStyle(
                    color = androidx.glance.color.ColorProvider(day = Color(0xFF71D7CD), night = Color(0xFF71D7CD)),
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
            // Next Med
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Próximo Remédio", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp))
                Text(
                    text = if (nextMed != null) "${nextMed.name} (${nextMed.time})" else "Nenhum pendente",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }
            // Vibe
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                Text("Vibe de Hoje", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp))
                Text(
                    text = vibe,
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF71D7CD), night = Color(0xFF71D7CD)), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(12.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            // Steps today
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Passos Hoje", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text(text = "$todaySteps", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            // Latest Weight
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                Text("Peso Corporal", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text(
                    text = String.format(Locale("pt", "BR"), "%.1f kg", latestWeight),
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
