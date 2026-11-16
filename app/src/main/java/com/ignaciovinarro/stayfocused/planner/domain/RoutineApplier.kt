package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.core.timeline.clampStartMinutes
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import java.time.LocalDate

// Aplicar una rutina a un día (PL-09) crea tareas normales enlazadas por `routineId`; editar la
// rutina después no cambia estas tareas (CLAUDE.md). `offsetMinutes` es relativo a la hora ancla
// de la rutina (`routine.startMinutes`); el resultado se enclava dentro del día (0–1439).
fun applyRoutine(routine: RoutineEntity, items: List<RoutineItemEntity>, date: LocalDate): List<TaskEntity> {
    val epochDay = date.toEpochDay()
    return items.map { item ->
        val startMinutes = clampStartMinutes(routine.startMinutes + item.offsetMinutes, item.durationMinutes)
        TaskEntity(
            title = item.title,
            date = epochDay,
            startMinutes = startMinutes,
            durationMinutes = item.durationMinutes,
            iconKey = item.iconKey,
            colorKey = item.colorKey,
            routineId = routine.id
        )
    }
}
