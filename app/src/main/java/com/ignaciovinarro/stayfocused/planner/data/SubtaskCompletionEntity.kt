package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Tilde de una subtarea en una ocurrencia puntual de una tarea RECURRENTE (PL-05). Las subtareas de
// una recurrente son una plantilla que pertenece a la tarea; lo que cambia día a día es si están
// tildadas, y eso vive acá, igual que el completado de la tarea vive en `task_occurrence`.
//
// Solo hay fila si esa subtarea se tildó alguna vez ese día. Destildar pone `completedAt` en nulo en
// vez de borrar la fila, así el índice único (subtaskId, date) no estorba al volver a tildar.
// Las tareas simples no usan esta tabla: usan `subtask.done`.
@Entity(
    tableName = "subtask_completion",
    indices = [
        Index(value = ["subtaskId", "date"], unique = true),
        Index(value = ["uuid"], unique = true)
    ]
)
data class SubtaskCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val subtaskId: Long, // apunta al id numérico de `subtask`, no al uuid
    val date: Long, // epochDay de la ocurrencia
    val completedAt: Long? = null,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
