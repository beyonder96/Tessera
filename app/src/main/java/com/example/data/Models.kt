package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subtitle: String,
    val value: Double,
    val isIncome: Boolean,
    val timestamp: Long,
    val category: String,
    val accountOrCardName: String = "",
    val isRealized: Boolean = true,
    val isRecurrent: Boolean = false,
    val recurrenceInterval: String = "Mensal", // "Mensal", "Semanal", "Anual"
    val dueDate: Long = 0L
)

@Entity(tableName = "market_items")
data class MarketItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isChecked: Boolean,
    val isBought: Boolean,
    val orderIndex: Int,
    val quantity: Double = 1.0,
    val unit: String = "un",
    val price: Double = 0.0,
    val category: String = "Geral",
    val inMarket: Boolean = false
)

@Entity(tableName = "pet_events")
data class PetEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val petName: String,
    val title: String,
    val time: String,
    val isCompleted: Boolean,
    val isNext: Boolean
)

@Entity(tableName = "bank_accounts")
data class BankAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double,
    val type: String, // e.g. "Corrente", "Poupança", "Investimento"
    val colorHex: String
)

@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val limit: Double,
    val usedLimit: Double,
    val numberLastFour: String,
    val colorHex: String,
    val holderName: String
)

@Entity(tableName = "benefit_cards")
data class BenefitCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double,
    val numberLastFour: String,
    val colorHex: String,
    val holderName: String
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCompleted: Boolean,
    val streak: Int,
    val iconName: String, // Store icon name e.g. "WaterDrop"
    val colorHex: String,
    val orderIndex: Int
)

@Entity(tableName = "purchase_goals")
data class PurchaseGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetValue: Double,
    val currentValue: Double,
    val imageUrl: String,
    val deadlineTimestamp: Long,
    val colorHex: String,
    val priorityOrder: Int = 1,
    val priorityClassification: String = "Moderado",
    val isBought: Boolean = false,
    val buyUrl: String = "",
    val category: String = "Geral"
)

@Entity(tableName = "health_profile")
data class HealthProfile(
    @PrimaryKey val id: Int = 1, // Single row profile
    val heightCm: Double = 0.0,
    val targetWeightKg: Double = 0.0,
    val isHealthConnectEnabled: Boolean = false
)

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val time: String,
    val isTaken: Boolean,
    val dosage: String,
    val colorHex: String,
    val recurrence: String = "DAILY" // "DAILY" (Diário) or "ALTERNATE" (Dias alternados)
)

@Entity(tableName = "weight_records")
data class WeightRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weightKg: Double,
    val timestamp: Long,
    val source: String // e.g., "manual", "Health Connect"
)

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val durationHours: Double,
    val source: String // e.g., "manual", "Health Connect"
)

enum class PetSex {
    MACHO, FEMEA
}

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val breed: String,
    val birthDate: Long, // Epoch timestamp (ms)
    val photoUri: String,
    val rga: String,
    val microchip: String,
    val sex: PetSex,
    val isCastrated: Boolean,
    val lastV4VaccineDate: Long?, // Epoch timestamp (ms)
    val lastRaivaVaccineDate: Long?, // Epoch timestamp (ms)
    val lastAntipulgasDate: Long? = null, // Epoch timestamp (ms)
    val lastVermifugoDate: Long? = null, // Epoch timestamp (ms)
    val lastConsultaDate: Long? = null, // Epoch timestamp (ms)
    val notes: String,
    val isAngel: Boolean = false
)

@Entity(
    tableName = "pet_weight_history",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["petId"])]
)
data class PetWeightHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val petId: Int,
    val date: Long, // Epoch timestamp (ms)
    val weight: Double
)

@Entity(
    tableName = "medication_logs",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicationId"])]
)
data class MedicationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val takenTimestamp: Long
)

@Entity(tableName = "steps_records")
data class StepsRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val count: Long,
    val startTime: Long,
    val endTime: Long,
    val source: String // e.g. "manual", "Health Connect"
)

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconName: String = "Spa"
)

@Entity(
    tableName = "routine_steps",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["routineId"])]
)
data class RoutineStep(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routineId: Int,
    val title: String,
    val durationSeconds: Int,
    val iconName: String,
    val orderIndex: Int,
    val checkQuestions: String = ""
)
