package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Solo existe una fila acá cuando una ocurrencia de una tarea recurrente se completa, se salta
// o se modifica (CLAUDE.md); las demás ocurrencias se calculan con `occursOn` y no tienen fila.
// Completar una ocurrencia no completa las demás: `completedAt` vive acá, no en `task`.
//
// Editar "solo esta" ocurrencia (PL-15): los campos `override*` pisan el valor de la tarea base
// solo para esta fecha puntual; nulo = sin override, se usa el valor de `task`. `overrideTitle`
// vacío (no nulo) es un título válido, por eso el override es de texto completo, no de "vacío =
// sin cambio".
@Entity(
    tableName = "task_occurrence",
    indices = [
        Index(value = ["taskId", "date"], unique = true),
        Index(value = ["uuid"], unique = true)
    ]
)
data class TaskOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val taskId: Long, // apunta al id numérico de `task`, no al uuid
    val date: Long, // epochDay de la ocurrencia puntual
    val skipped: Boolean = false,
    val overrideStartMinutes: Int? = null,
    val overrideDurationMinutes: Int? = null,
    val overrideTitle: String? = null,
    val overrideNotes: String? = null,
    val overrideIconKey: String? = null,
    val overrideColorKey: String? = null,
    val completedAt: Long? = null,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
