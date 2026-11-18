package com.ignaciovinarro.stayfocused.gym.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Serie de una sesión (GY-05 / GY-12). `weight` en gramos enteros; `type` es el nombre de `SetType`.
// `durationSeconds` solo para ejercicios de tipo tiempo. `position`: orden dentro del ejercicio.
@Entity(
    tableName = "session_set",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"])
    ]
)
data class SessionSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int,
    val weight: Long = 0,
    val reps: Int = 0,
    val durationSeconds: Int = 0,
    val type: String,
    val completed: Boolean = false,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
