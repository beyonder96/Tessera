package com.tesserahub.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "finances")
data class FinanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Double,
    val type: String, // "INCOME" ou "EXPENSE"
    val timestamp: Long
)
