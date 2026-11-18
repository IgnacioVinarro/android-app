package com.ignaciovinarro.stayfocused.gym.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Ejercicio (GY-01 / GY-02). `muscleGroup`, `equipment` y `trackingType` son nombres de enum.
// - `restSeconds`: descanso por defecto entre series (GY-07).
// - `link`: enlace o imagen opcional. `isCustom`: false = de la biblioteca precargada.
@Entity(
    tableName = "exercise",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val trackingType: String,
    val isCustom: Boolean = true,
    val notes: String? = null,
    val link: String? = null,
    val restSeconds: Int = 90,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
