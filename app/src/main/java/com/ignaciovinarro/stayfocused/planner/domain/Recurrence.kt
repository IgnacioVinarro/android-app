package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RecurrenceType { DAILY, WEEKLY, SPECIFIC_DAYS, MONTHLY, EVERY_N_DAYS }

// Máscara de días de la semana para "días específicos": bit (DayOfWeek.value - 1),
// lunes = bit 0 ... domingo = bit 6.
fun dayOfWeekBit(day: DayOfWeek): Int = 1 shl (day.value - 1)

// Función pura (PL-08): dice si una tarea recurrente tiene ocurrencia en esa fecha. La regla vive
// en la tarea; las ocurrencias no se guardan, se calculan acá. No depende de Room ni de ninguna
// clase de Android, solo de los campos de recurrencia y de java.time.
fun occursOn(task: TaskEntity, date: LocalDate): Boolean {
    val type = task.recurrenceType?.let { runCatching { RecurrenceType.valueOf(it) }.getOrNull() }
        ?: return false
    val startDate = LocalDate.ofEpochDay(task.date)
    if (date.isBefore(startDate)) return false
    task.recurrenceEnd?.let { end -> if (date.toEpochDay() > end) return false }

    return when (type) {
        RecurrenceType.DAILY -> true
        RecurrenceType.WEEKLY -> date.dayOfWeek == startDate.dayOfWeek
        RecurrenceType.SPECIFIC_DAYS -> {
            val mask = task.recurrenceDays ?: 0
            (mask and dayOfWeekBit(date.dayOfWeek)) != 0
        }
        RecurrenceType.MONTHLY -> date.dayOfMonth == startDate.dayOfMonth
        RecurrenceType.EVERY_N_DAYS -> {
            val interval = (task.recurrenceInterval ?: 1).coerceAtLeast(1)
            ChronoUnit.DAYS.between(startDate, date) % interval == 0L
        }
    }
}
