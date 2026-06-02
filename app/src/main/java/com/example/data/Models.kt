package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subtitle: String,
    val value: Double,
    val isIncome: Boolean,
    val timestamp: Long,
    val category: String,
    val accountOrCardName: String = ""
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
    val category: String = "Geral"
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
    val colorHex: String
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
    val colorHex: String
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
