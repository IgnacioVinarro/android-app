package com.ignaciovinarro.stayfocused.core.timeline

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class TimelineBlock(
    val id: String, // único dentro de su módulo
    val source: String, // "planner", "gym", "nutrition", etc.
    val date: LocalDate,
    val startMinutes: Int, // de 0 a 1439
    val durationMinutes: Int,
    val title: String,
    val iconKey: String,
    val colorKey: String,
    val completed: Boolean,
    val movable: Boolean = true, // si se puede mover/redimensionar arrastrando (PL-07)
    val route: String // pantalla que se abre al tocar el bloque
)

// El núcleo solo conoce esta interfaz; cada módulo registra su propia implementación con Hilt.
interface TimelineSource {
    fun blocksFor(date: LocalDate): Flow<List<TimelineBlock>>
}
