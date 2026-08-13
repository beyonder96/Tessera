package com.example.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GlobalOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "global_overlay_channel",
                "Lembretes (Overlay)",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "global_overlay_channel")
            .setContentTitle("Tessera Lembretes")
            .setContentText("Aguardando confirmação do lembrete...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1991, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1991, notification)
        }

        val reminderType = intent?.getStringExtra("REMINDER_TYPE") ?: "MEDICATION"
        
        val titleText: String
        val descText: String
        var medicationId = -1

        when (reminderType) {
            "STEPS" -> {
                titleText = "Lembrete de Passos"
                descText = "Já registrou seus passos hoje?"
            }
            "SLEEP" -> {
                titleText = "Lembrete de Sono"
                descText = "Bom dia! Como foi sua noite de sono?"
            }
            "METRO" -> {
                titleText = "Status do Metrô/Trem"
                descText = "Verifique o status das suas linhas monitoradas no Tessera."
            }
            else -> {
                // Default: MEDICATION
                medicationId = intent?.getIntExtra("medicationId", -1) ?: -1
                val medicationName = intent?.getStringExtra("medicationName") ?: "Medicamento"
                val medicationDosage = intent?.getStringExtra("medicationDosage") ?: ""
                val medicationTime = intent?.getStringExtra("medicationTime") ?: ""
                titleText = "Hora do Remédio"
                descText = "$medicationName - $medicationDosage\n$medicationTime"
            }
        }

        if (overlayView == null) {
            showOverlay(reminderType, titleText, descText, medicationId)
        }
        
        return START_NOT_STICKY
    }

    private fun showOverlay(type: String, titleStr: String, descStr: String, medicationId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val context = this
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            
            // Background drawable with rounded corners
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#18181A"))
                cornerRadius = 60f
                setStroke(2, android.graphics.Color.parseColor("#33FFFFFF"))
            }
            background = bg
            setPadding(60, 60, 60, 60)
            
            val title = TextView(context).apply {
                text = titleStr
                setTextColor(android.graphics.Color.WHITE)
                textSize = 20f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
            }
            addView(title)
            
            val desc = TextView(context).apply {
                text = descStr
                setTextColor(android.graphics.Color.parseColor("#A1A1AA"))
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 30, 0, 40)
            }
            addView(desc)
            
            val btnConfirm = Button(context).apply {
                text = if (type == "MEDICATION") "MARCAR COMO TOMADO" else "REGISTRAR AGORA"
                val btnBg = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#71D7CD"))
                    cornerRadius = 30f
                }
                background = btnBg
                setTextColor(android.graphics.Color.parseColor("#131817"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setOnClickListener {
                    if (type == "MEDICATION" && medicationId != -1) {
                        markAsTaken(medicationId)
                    } else {
                        val launchIntent = Intent(context, com.example.MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("OPEN_TARGET", type)
                        }
                        context.startActivity(launchIntent)
                    }
                    removeOverlay()
                }
            }
            addView(btnConfirm)
            
            val spacer = View(context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 20
                )
            }
            addView(spacer)
            
            val btnDismiss = Button(context).apply {
                text = "LEMBRAR DEPOIS"
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setTextColor(android.graphics.Color.parseColor("#879391"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setOnClickListener {
                    removeOverlay()
                }
            }
            addView(btnDismiss)
        }
        
        try {
            val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, defaultSoundUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        overlayView = layout
        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
            stopSelf()
        }
    }

    private fun markAsTaken(medicationId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = com.example.data.AppDatabase.getDatabase(applicationContext)
            val log = com.example.data.MedicationLog(
                medicationId = medicationId,
                takenTimestamp = System.currentTimeMillis()
            )
            db.tesseraDao().insertMedicationLog(log)
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }
}
