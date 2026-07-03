package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Transaction::class, MarketItem::class, PetEvent::class, BankAccount::class, 
        CreditCard::class, Habit::class, PurchaseGoal::class, HealthProfile::class,
        Medication::class, WeightRecord::class, SleepRecord::class, PetEntity::class,
        PetWeightHistoryEntity::class, MedicationLog::class, StepsRecord::class,
        Routine::class, RoutineStep::class
    ], 
    version = 16, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tesseraDao(): TesseraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isRealized INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isRecurrent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurrenceInterval TEXT NOT NULL DEFAULT 'Mensal'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN dueDate INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pets ADD COLUMN lastAntipulgasDate INTEGER DEFAULT null")
                db.execSQL("ALTER TABLE pets ADD COLUMN lastVermifugoDate INTEGER DEFAULT null")
                db.execSQL("ALTER TABLE pets ADD COLUMN lastConsultaDate INTEGER DEFAULT null")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE purchase_goals ADD COLUMN priorityOrder INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE purchase_goals ADD COLUMN priorityClassification TEXT NOT NULL DEFAULT 'Moderado'")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine_steps ADD COLUMN checkQuestions TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pets ADD COLUMN isAngel INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE purchase_goals ADD COLUMN isBought INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE purchase_goals ADD COLUMN buyUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE purchase_goals ADD COLUMN category TEXT NOT NULL DEFAULT 'Geral'")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE market_items ADD COLUMN inMarket INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tessera_database.db"
                )
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeAndClearInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
