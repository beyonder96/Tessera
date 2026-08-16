package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("REMINDER_TYPE") ?: return

        NotificationHelper.createNotificationChannel(context)

        val title: String
        val message: String
        val notificationId: Int
        
        val sharedPrefs = context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)

        when (type) {
            "VR_RESET" -> {
                title = "Vale Refeição"
                message = "Chegou o dia de atualizar seu saldo do Vale Refeição no Tessera!"
                notificationId = 9990
                val vrResetDate = sharedPrefs.getInt("vr_reset_date", 1)
                AlarmScheduler.scheduleVrAlarm(context, vrResetDate)
            }
            "STEPS" -> {
                title = "Lembrete de Passos"
                message = "Já registrou seus passos hoje?"
                notificationId = 9991
                // Reschedule for next day using saved time
                val timeString = sharedPrefs.getString("steps_reminder_time", "20:00") ?: "20:00"
                AlarmScheduler.scheduleDailyReminder(context, "STEPS", timeString)
            }
            "SLEEP" -> {
                title = "Lembrete de Sono"
                message = "Bom dia! Como foi sua noite de sono?"
                notificationId = 9992
                // Reschedule for next day using saved time
                val timeString = sharedPrefs.getString("sleep_reminder_time", "08:00") ?: "08:00"
                AlarmScheduler.scheduleDailyReminder(context, "SLEEP", timeString)
            }
            "METRO" -> {
                title = "Status do Metrô e Trem"
                message = "Verifique o status das suas linhas monitoradas no Tessera."
                notificationId = 9993
                val timeString = intent.getStringExtra("EXTRA_TIME") ?: "00:00"
                AlarmScheduler.scheduleDailyReminder(context, "METRO_$timeString", timeString)
                intent.putExtra("REMINDER_TYPE", "METRO_$timeString") // so we don't fall into default
            }
            else -> {
                if (type.startsWith("METRO_")) {
                    title = "Status do Metrô e Trem"
                    message = "Verifique o status das suas linhas monitoradas no Tessera."
                    notificationId = type.hashCode()
                    val timeString = type.removePrefix("METRO_")
                    AlarmScheduler.scheduleDailyReminder(context, type, timeString)
                } else {
                    return
                }
            }
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(context)) {
            val serviceIntent = Intent(context, GlobalOverlayService::class.java).apply {
                putExtra("REMINDER_TYPE", if (type.startsWith("METRO_")) "METRO" else type)
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            NotificationHelper.showBasicNotification(
                context = context,
                title = title,
                message = message,
                notificationId = notificationId
            )
        }
    }
}
