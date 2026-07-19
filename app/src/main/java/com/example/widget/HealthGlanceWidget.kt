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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HealthGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var todaySteps = 0L
        var latestWeight = 75.2
        var vibe = "Pendente"
        var nextMedName = ""
        var nextMedTime = ""

        try {
            val sharedPrefs = context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val savedFeelingDate = sharedPrefs.getString("feeling_date", "")
            if (savedFeelingDate == todayDate) {
                vibe = sharedPrefs.getString("user_feeling", "Pendente") ?: "Pendente"
            }

            withTimeoutOrNull(2000) {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(context)
                    coroutineScope {
                        val medsDef = async { db.tesseraDao().getAllMedications().first() }
                        val weightsDef = async { db.tesseraDao().getAllWeightRecords().first() }
                        val stepsDef = async { db.tesseraDao().getAllStepsRecords().first() }

                        val medications = medsDef.await()
                        val weightRecords = weightsDef.await()
                        val stepsRecords = stepsDef.await()

                        val todayStart = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val todayEnd = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        todaySteps = stepsRecords.filter { it.startTime in todayStart..todayEnd }.sumOf { it.count }
                        
                        latestWeight = weightRecords.lastOrNull()?.weightKg ?: 75.2
                        
                        val uncheckedMeds = medications.filter { !it.isTaken }
                        val nextMed = if (uncheckedMeds.isNotEmpty()) {
                            val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            val futureMeds = uncheckedMeds.filter { it.time >= nowStr }.sortedBy { it.time }
                            if (futureMeds.isNotEmpty()) futureMeds.first() else uncheckedMeds.sortedBy { it.time }.first()
                        } else null
                        
                        if (nextMed != null) {
                            nextMedName = nextMed.name
                            nextMedTime = nextMed.time
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        provideContent {
            HealthWidgetContent(context, nextMedName, nextMedTime, latestWeight, todaySteps, vibe)
        }
    }
}

@androidx.compose.runtime.Composable
fun HealthWidgetContent(context: Context, nextMedName: String, nextMedTime: String, latestWeight: Double, todaySteps: Long, vibe: String) {
    val openAppAction = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            putExtra("route", "health")
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
                text = "SAÚDE",
                style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF64FFDA), night = Color(0xFF64FFDA)), fontSize = 10.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text("TESSERA", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x66FFFFFF), night = Color(0x66FFFFFF)), fontSize = 9.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Próximo Remédio", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp))
                Text(
                    text = if (nextMedName.isNotEmpty()) "$nextMedName ($nextMedTime)" else "Nenhum pendente",
                    style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                Text("Vibe de Hoje", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF81928F), night = Color(0xFF81928F)), fontSize = 11.sp))
                Text(text = vibe, style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0xFF64FFDA), night = Color(0xFF64FFDA)), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(modifier = GlanceModifier.height(12.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text("Passos Hoje", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)), fontSize = 10.sp))
                Text(text = "$todaySteps", style = TextStyle(color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
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
