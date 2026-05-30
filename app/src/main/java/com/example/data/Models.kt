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
    val category: String
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
