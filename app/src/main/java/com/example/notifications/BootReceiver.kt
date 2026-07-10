package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val meds = db.tesseraDao().getAllMedications().first()
                    for (med in meds) {
                        AlarmScheduler.scheduleMedicationAlarm(context, med.name, med.dosage, med.time)
                    }
                    val sharedPrefs = context.getSharedPreferences("tessera_prefs", Context.MODE_PRIVATE)
                    val vrResetDate = sharedPrefs.getInt("vr_reset_date", 1)
                    AlarmScheduler.scheduleVrAlarm(context, vrResetDate)
                    
                    val stepsTime = sharedPrefs.getString("steps_reminder_time", "20:00") ?: "20:00"
                    AlarmScheduler.scheduleDailyReminder(context, "STEPS", stepsTime)
                    
                    val sleepTime = sharedPrefs.getString("sleep_reminder_time", "08:00") ?: "08:00"
                    AlarmScheduler.scheduleDailyReminder(context, "SLEEP", sleepTime)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
