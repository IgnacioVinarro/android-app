package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// startMinutes nulo = la tarea está en la bandeja (Inbox), sin hora asignada todavía.
// Sin campos de recurrencia/routineId/subtareas: esos llegan en sus propios pasos (PL-08, PL-09, PL-05).
@Entity(tableName = "task", indices = [Index(value = ["uuid"], unique = true)])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String? = null,
    val date: Long,
    val startMinutes: Int? = null,
    val durationMinutes: Int,
    val iconKey: String,
    val colorKey: String,
    val status: Int = 0,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
