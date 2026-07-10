package com.example.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.MedicationLog
import com.example.widget.DailyGlanceWidget
import com.example.widget.HealthGlanceWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class MedicationReceiver : BroadcastReceiver() {

    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val medName = intent.getStringExtra("MED_NAME") ?: "Remédio"
        val medDosage = intent.getStringExtra("MED_DOSAGE") ?: ""
        
        if (action == "ACTION_MARK_TAKEN") {
            val notificationId = intent.getIntExtra("NOTIFICATION_ID", -1)
            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                val meds = db.tesseraDao().getAllMedications().first()
                val matchingMed = meds.find { it.name == medName }
                if (matchingMed != null) {
                    val start = getStartOfToday()
                    val end = getEndOfToday()
                    val logs = db.tesseraDao().getLogsForMedication(matchingMed.id, start, end).first()
                    if (logs.isEmpty()) {
                        db.tesseraDao().insertMedicationLog(
                            MedicationLog(medicationId = matchingMed.id, takenTimestamp = System.currentTimeMillis())
                        )
                    }
                    try {
                        HealthGlanceWidget().updateAll(context)
                        DailyGlanceWidget().updateAll(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
            }
            return
        }

        // Caso padrão: disparar a notificação
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
                    val serviceIntent = Intent(context, GlobalMedicationService::class.java).apply {
                        putExtra("medicationId", matchingMed.id)
                        putExtra("medicationName", medName)
                        putExtra("medicationDosage", medDosage)
                        putExtra("medicationTime", matchingMed.time)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                
                // Reagenda o alarme exato para o próximo dia
                AlarmScheduler.scheduleMedicationAlarm(
                    context = context,
                    medName = matchingMed.name,
                    medDosage = matchingMed.dosage,
                    timeString = matchingMed.time
                )
                
                if (matchingMed.isTaken) {
                    db.tesseraDao().updateMedication(matchingMed.copy(isTaken = false))
                    try {
                        HealthGlanceWidget().updateAll(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                val serviceIntent = Intent(context, GlobalMedicationService::class.java).apply {
                    putExtra("medicationName", medName)
                    putExtra("medicationDosage", medDosage)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
