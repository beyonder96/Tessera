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
import androidx.glance.layout.width
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
        var nextMedName = ""
        var nextMedTime = ""

        try {
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
            HealthWidgetContent(context, nextMedName, nextMedTime, latestWeight, todaySteps)
        }
    }
}

@androidx.compose.runtime.Composable
fun HealthWidgetContent(context: Context, nextMedName: String, nextMedTime: String, latestWeight: Double, todaySteps: Long) {
    val openAppAction = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            putExtra("route", "health")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    )

    val bgProvider = ColorProvider(day = Color(0xF8FFFFFF), night = Color(0xF0121316))
    val textPrimary = ColorProvider(day = Color(0xFF0F172A), night = Color(0xFFF8FAFC))
    val textSecondary = ColorProvider(day = Color(0xFF64748B), night = Color(0xFF94A3B8))
    val accentEmerald = ColorProvider(day = Color(0xFF059669), night = Color(0xFF10B981))
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
                text = "SAÚDE",
                style = TextStyle(color = accentEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold),
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
            // Passos
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Passos Hoje",
                    style = TextStyle(color = textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    text = "$todaySteps",
                    style = TextStyle(color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }

            // Remédio
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Próximo Remédio",
                    style = TextStyle(color = textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    text = if (nextMedName.isNotEmpty()) "$nextMedName ($nextMedTime)" else "Em dia ✓",
                    style = TextStyle(color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }

            // Peso
            Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                Text(
                    text = "Peso",
                    style = TextStyle(color = textSecondary, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1f kg", latestWeight),
                    style = TextStyle(color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
