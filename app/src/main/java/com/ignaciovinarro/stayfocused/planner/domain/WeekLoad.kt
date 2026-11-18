package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

const val DAYS_IN_WEEK = 7

// Carga de un día (PL-12): cuántas tareas tiene y cuántos minutos planificados. `fraction` es de 0 a 1
// respecto del día más cargado de la semana, para dibujar la barra.
data class DayLoad(
    val date: LocalDate,
    val taskCount: Int,
    val plannedMinutes: Int,
    val fraction: Float
)

// La semana empieza el lunes.
fun weekStartOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

// Función pura (PL-12). Para cada uno de los 7 días desde `weekStart`:
// - tarea simple: cuenta si su fecha es ese día;
// - tarea recurrente: cuenta si `occursOn` dice que ocurre y la ocurrencia no está saltada.
// Los minutos suman la duración solo de lo que tiene hora (una tarea de la bandeja cuenta en la cantidad
// pero no en los minutos). Una ocurrencia modificada usa su hora y duración propias.
fun buildWeekLoad(
    weekStart: LocalDate,
    tasks: List<TaskEntity>,
    occurrences: List<TaskOccurrenceEntity>
): List<DayLoad> {
    val activeTasks = tasks.filter { it.status == 0 }
    val occurrenceByKey = occurrences.filter { it.status == 0 }.associateBy { it.taskId to it.date }

    val raw = (0 until DAYS_IN_WEEK).map { offset ->
        val date = weekStart.plusDays(offset.toLong())
        val epochDay = date.toEpochDay()
        var count = 0
        var minutes = 0
        for (task in activeTasks) {
            if (task.recurrenceType == null) {
                if (task.date != epochDay) continue
                count++
                if (task.startMinutes != null) minutes += task.durationMinutes
            } else {
                if (!occursOn(task, date)) continue
                val occurrence = occurrenceByKey[task.id to epochDay]
                if (occurrence?.skipped == true) continue
                count++
                val start = occurrence?.overrideStartMinutes ?: task.startMinutes
                if (start != null) minutes += occurrence?.overrideDurationMinutes ?: task.durationMinutes
            }
        }
        Triple(date, count, minutes)
    }

    val maxMinutes = raw.maxOf { it.third }
    return raw.map { (date, count, minutes) ->
        DayLoad(
            date = date,
            taskCount = count,
            plannedMinutes = minutes,
            fraction = if (maxMinutes == 0) 0f else minutes.toFloat() / maxMinutes
        )
    }
}
