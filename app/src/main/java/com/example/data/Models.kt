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
    val orderIndex: Int
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
