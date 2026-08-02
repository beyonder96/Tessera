package com.example

import android.content.Context
import java.io.File

object CrashReporter {
    fun setup(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val file = File(context.getExternalFilesDir(null), "tessera_crash_log.txt")
                val log = "Crash in thread ${thread.name}:\n${throwable.stackTraceToString()}\n\n"
                file.appendText(log)
            } catch (e: Exception) {
                // Ignore
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun getCrashLog(context: Context): String? {
        val file = File(context.getExternalFilesDir(null), "tessera_crash_log.txt")
        return if (file.exists()) file.readText() else null
    }

    fun clearCrashLog(context: Context) {
        val file = File(context.getExternalFilesDir(null), "tessera_crash_log.txt")
        if (file.exists()) file.delete()
    }
}
