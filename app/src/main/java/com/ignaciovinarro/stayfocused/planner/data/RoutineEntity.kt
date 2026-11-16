package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// `startMinutes` es la hora ancla de la rutina (PL-09): los ítems guardan un offset relativo a
// esta hora, así que moverla corre todos los ítems juntos la próxima vez que se aplique.
@Entity(tableName = "routine", indices = [Index(value = ["uuid"], unique = true)])
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val startMinutes: Int,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
