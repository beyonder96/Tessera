package com.tessera.app.utils

import android.content.Context
import android.net.Uri
import com.example.data.AppDatabase
import java.io.FileInputStream

object BackupHelper {
    fun exportDatabase(context: Context, uri: Uri) {
        try {
            // Fechar instância para forçar flush e checkpoint do WAL no arquivo principal
            AppDatabase.closeAndClearInstance()
            
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
