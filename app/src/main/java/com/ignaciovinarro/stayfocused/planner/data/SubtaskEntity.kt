package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Subtarea de una tarea (PL-04 / PL-05). `position` define el orden en la lista (0, 1, 2…).
//
// `done` guarda el tilde de las subtareas de tareas SIMPLES. En una tarea recurrente las subtareas
// son una plantilla que pertenece a la tarea y el tilde de cada una vive por ocurrencia (ticket
// 0016), así que ahí `done` se ignora.
@Entity(
    tableName = "subtask",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["taskId"])
    ]
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val taskId: Long, // apunta al id numérico de `task`, no al uuid
    val title: String,
    val done: Boolean = false,
    val position: Int,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
