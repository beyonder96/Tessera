package com.tessera.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet_routines")
data class PetRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val petName: String, // "Marie" ou "Churchill"
    val task: String,
    val time: String, // ex: "04:10 AM"
    val isCompleted: Boolean = false
)
