package com.tesserahub.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_metrics")
data class HealthEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bpm: Int,
    val weight: Double,
    val timestamp: Long
)
