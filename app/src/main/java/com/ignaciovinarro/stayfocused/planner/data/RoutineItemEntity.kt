package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// `offsetMinutes` es relativo a `routine.startMinutes` (puede ser negativo, si el ítem va antes
// de la hora ancla). `position` define el orden en la lista del editor (no se puede arrastrar
// para reordenar todavía, ver ticket 0010).
@Entity(
    tableName = "routine_item",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class RoutineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val routineId: Long, // apunta al id numérico de `routine`
    val title: String,
    val offsetMinutes: Int,
    val durationMinutes: Int,
    val iconKey: String,
    val colorKey: String,
    val position: Int,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
