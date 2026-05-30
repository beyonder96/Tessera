package com.tesserahub.app.utils

import android.content.Context
import android.net.Uri
import java.io.FileInputStream

object BackupHelper {
    fun exportDatabase(context: Context, uri: Uri) {
        try {
            // O nome deve ser exatamente o definido no seu AppDatabase ("tessera_database.db")
            val dbFile = context.getDatabasePath("tessera_database.db") 
            if (dbFile.exists()) {
                FileInputStream(dbFile).use { inputStream ->
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
