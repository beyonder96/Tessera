package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MedicationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("MED_NAME") ?: "Remédio"
        val medDosage = intent.getStringExtra("MED_DOSAGE") ?: ""
        
        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.showNotification(
            context = context,
            title = "Hora do Remédio: $medName",
            message = "Não se esqueça de tomar $medName ($medDosage).",
            notificationId = medName.hashCode()
        )
    }
}
