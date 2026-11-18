package com.ignaciovinarro.stayfocused.gym.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Rutina de gimnasio (GY-03 / GY-04). `scheduleDays`: máscara de días de la semana (bit 0 = lunes …
// bit 6 = domingo; 0 = sin programar). `startMinutes`: minutos desde las 00:00 (nulo = sin hora).
@Entity(
    tableName = "workout_routine",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class WorkoutRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val scheduleDays: Int = 0,
    val startMinutes: Int? = null,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
