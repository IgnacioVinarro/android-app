package com.ignaciovinarro.stayfocused.gym.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Nota de un ejercicio dentro de una sesión (GY-17). Una por par sesión-ejercicio.
@Entity(
    tableName = "session_exercise_note",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["sessionId", "exerciseId"], unique = true),
        Index(value = ["exerciseId"])
    ]
)
data class SessionExerciseNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val sessionId: Long,
    val exerciseId: Long,
    val note: String,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
