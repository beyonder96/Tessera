package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.data.AppDatabase

import kotlinx.coroutines.flow.first
import java.util.Calendar

import androidx.glance.appwidget.updateAll
import com.example.widget.HealthGlanceWidget

class MedicationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("MED_NAME") ?: "Remédio"
        val medDosage = intent.getStringExtra("MED_DOSAGE") ?: ""
        val dosageText = if (medDosage.isNotBlank()) " ($medDosage)" else ""
        val messageText = "Não se esqueça de tomar $medName$dosageText."
        
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val meds = db.tesseraDao().getAllMedications().first()
            val matchingMed = meds.find { it.name == medName }
            if (matchingMed != null) {
                val todayDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val shouldShow = when (matchingMed.recurrence) {
                    "DAILY" -> true
                    "ALTERNATE" -> {
                        val daysSinceEpoch = (System.currentTimeMillis() / (24 * 60 * 60 * 1000)).toInt()
                        daysSinceEpoch % 2 == 0
                    }
                    else -> {
                        val activeDays = matchingMed.recurrence.split(",").mapNotNull { it.toIntOrNull() }
                        activeDays.isEmpty() || todayDayOfWeek in activeDays
                    }
                }
                
                if (shouldShow) {
                    NotificationHelper.createNotificationChannel(context)
                    NotificationHelper.showNotification(
                        context = context,
                        title = "Hora do Remédio: $medName",
                        message = messageText,
                        notificationId = medName.hashCode()
                    )
                }
                
                if (matchingMed.isTaken) {
                    db.tesseraDao().updateMedication(matchingMed.copy(isTaken = false))
                    try {
                        HealthGlanceWidget().updateAll(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                NotificationHelper.createNotificationChannel(context)
                NotificationHelper.showNotification(
                    context = context,
                    title = "Hora do Remédio: $medName",
                    message = messageText,
                    notificationId = medName.hashCode()
                )
            }
        }
    }
}
