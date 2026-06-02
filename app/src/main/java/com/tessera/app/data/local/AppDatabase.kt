package com.tessera.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tessera.app.data.local.entity.*
import com.tessera.app.data.local.dao.TesseraDao

@Database(
    entities = [
        FinanceEntity::class, 
        MarketItemEntity::class, 
        HealthEntity::class, 
        GoalEntity::class, 
        PetRoutineEntity::class
    ], 
    version = 1, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun tesseraDao(): TesseraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tessera_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
