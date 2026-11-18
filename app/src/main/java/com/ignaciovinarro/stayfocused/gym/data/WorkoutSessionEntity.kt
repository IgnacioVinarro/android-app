package com.ignaciovinarro.stayfocused.gym.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Sesión de entrenamiento (GY-05 / GY-10). `routineId` nulo = sesión libre (GY-14).
// `state` es el nombre de `SessionState` (no se llama `status` porque esa columna es el borrado lógico).
// `date`: epochDay; `startedAt`/`endedAt`: milisegundos (`endedAt` nulo = en curso).
@Entity(
    tableName = "workout_session",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["routineId"]),
        Index(value = ["date"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val routineId: Long? = null,
    val date: Long,
    val startedAt: Long,
    val endedAt: Long? = null,
    val state: String,
    val notes: String? = null,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
