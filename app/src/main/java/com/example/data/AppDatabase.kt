package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Transaction::class, MarketItem::class, PetEvent::class, BankAccount::class, 
        CreditCard::class, Habit::class, PurchaseGoal::class, HealthProfile::class,
        Medication::class, WeightRecord::class, SleepRecord::class
    ], 
    version = 5, 
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
                    "tessera_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
