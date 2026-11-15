package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// startMinutes nulo = la tarea está en la bandeja (Inbox), sin hora asignada todavía.
// Sin routineId ni subtareas: esos llegan en sus propios pasos (PL-09, PL-05).
//
// Recurrencia (PL-08): la regla vive acá, en la tarea; las ocurrencias se calculan con la función
// pura `occursOn` (planner/domain), no se guardan. `recurrenceType` nulo = tarea no recurrente.
// `recurrenceInterval` lo usa "cada N días"; `recurrenceDays` es la máscara de días de la semana
// para "días específicos"; `recurrenceEnd` es el fin opcional (epochDay, inclusive).
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
    val recurrenceType: String? = null,
    val recurrenceInterval: Int? = null,
    val recurrenceDays: Int? = null,
    val recurrenceEnd: Long? = null,
    val status: Int = 0,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
