package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

object AlarmScheduler {
    fun scheduleMedicationAlarm(context: Context, medName: String, medDosage: String, timeString: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicationReceiver::class.java).apply {
            putExtra("MED_NAME", medName)
            putExtra("MED_DOSAGE", medDosage)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val parts = timeString.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // If the time is in the past, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            // Schedule repeating alarm daily
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d("AlarmScheduler", "Alarm set for $timeString")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Missing exact alarm permission", e)
        }
    }
}
