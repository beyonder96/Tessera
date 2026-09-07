package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "medication_alarm_channel_v4"
    private const val CHANNEL_NAME = "Lembrete Crítico de Medicamento"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificações para lembretes de medicamentos"
                enableVibration(true)
                setShowBadge(true)
                setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                setSound(soundUri, audioAttributes)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_MEDICATION_ALARM"
            putExtra("MED_NAME", title.removePrefix("Hora do Remédio: "))
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Botão de ação rápida na notificação "Marcar como tomado"
        val markTakenIntent = Intent(context, MedicationReceiver::class.java).apply {
            action = "ACTION_MARK_TAKEN"
            val medName = title.removePrefix("Hora do Remédio: ")
            putExtra("MED_NAME", medName)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Marcar como tomado", markTakenPendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    fun showBasicNotification(context: Context, title: String, message: String, notificationId: Int) {
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    const val CHANNEL_TASKS_NOTICES_ID = "tessera_tasks_notices_priority_v1"
    private const val CHANNEL_TASKS_NOTICES_NAME = "Avisos e Tarefas Compartilhadas"

    fun createTasksNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()

            val channel = NotificationChannel(
                CHANNEL_TASKS_NOTICES_ID,
                CHANNEL_TASKS_NOTICES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações prioritárias para avisos e tarefas enviadas da Web"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 200, 350)
                setShowBadge(true)
                setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC)
                setSound(soundUri, audioAttributes)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskNoticeNotification(
        context: Context,
        title: String,
        description: String?,
        timeOrDate: String?,
        taskId: String,
        notificationId: Int = taskId.hashCode()
    ) {
        createTasksNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_OPEN_ZENITH_TASKS"
            putExtra("TARGET_TAB", 2)
            putExtra("TASK_ID", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val contentText = buildString {
            if (!description.isNullOrBlank()) {
                append(description)
            }
            if (!timeOrDate.isNullOrBlank()) {
                if (isNotEmpty()) append(" • ")
                append(timeOrDate)
            }
            if (isEmpty()) {
                append("Novo aviso recebido da Web aguardando sua confirmação.")
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_TASKS_NOTICES_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔔 $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVibrate(longArrayOf(0, 350, 200, 350))
            .setSound(soundUri)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
