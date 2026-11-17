package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import java.time.LocalDate
import java.time.LocalDateTime

// Cuántos días hacia adelante se busca una ocurrencia que todavía no pasó. Con una tarea recurrente
// que se saltó o completó durante más de un año seguido no se programa nada hasta que se reprograme
// de nuevo (cada vez que se abre la app, ver CLAUDE.md).
const val REMINDER_SEARCH_DAYS = 366L

// Cuándo suena un aviso (`triggerAt`) y de qué día es la ocurrencia a la que avisa (`occurrenceDate`;
// distinto del día de `triggerAt` cuando el aviso es "1 día antes").
data class ReminderTrigger(val triggerAt: LocalDateTime, val occurrenceDate: LocalDate)

// Función pura (PL-10): dice cuándo suena el próximo aviso de una tarea, o null si no hay ninguno.
// Solo se calcula la PRÓXIMA ocurrencia (CLAUDE.md): al dispararse, se vuelve a llamar para la siguiente.
// No usa zonas horarias ni clases de Android: trabaja con la hora "de pared" (LocalDateTime), igual
// que la tarea, que guarda la hora como minutos desde las 00:00. Convertir a milisegundos del sistema
// es trabajo de quien llama (AlarmScheduler).
//
// `occurrences` son las filas de `task_occurrence` de ESTA tarea (saltadas, completadas o con otra hora).
// Los recordatorios no cambian por ocurrencia, pero sí la hora de inicio si la ocurrencia la modificó.
fun nextReminder(
    task: TaskEntity,
    minutesBefore: Int,
    occurrences: List<TaskOccurrenceEntity>,
    now: LocalDateTime
): ReminderTrigger? {
    if (task.status != 0) return null
    val baseStart = task.startMinutes ?: return null // sin hora no hay recordatorios

    fun triggerFor(date: LocalDate, startMinutes: Int): LocalDateTime =
        date.atStartOfDay().plusMinutes((startMinutes - minutesBefore).toLong())

    if (task.recurrenceType == null) {
        if (task.completedAt != null) return null
        val trigger = triggerFor(LocalDate.ofEpochDay(task.date), baseStart)
        return trigger.takeIf { it.isAfter(now) }?.let { ReminderTrigger(it, LocalDate.ofEpochDay(task.date)) }
    }

    // Una ocurrencia del día D nunca avisa antes que su propia hora menos `minutesBefore`, y como
    // minutesBefore >= 0 y la hora < 24 h, los disparos crecen día a día: el primero que quede en el
    // futuro es el próximo. Se empieza hoy porque una ocurrencia anterior a hoy ya pasó.
    val byDate = occurrences.associateBy { it.date }
    var date = now.toLocalDate()
    val last = date.plusDays(REMINDER_SEARCH_DAYS)
    while (!date.isAfter(last)) {
        if (occursOn(task, date)) {
            val occurrence = byDate[date.toEpochDay()]
            val usable = occurrence == null || (!occurrence.skipped && occurrence.completedAt == null)
            if (usable) {
                val start = occurrence?.overrideStartMinutes ?: baseStart
                val trigger = triggerFor(date, start)
                if (trigger.isAfter(now)) return ReminderTrigger(trigger, date)
            }
        }
        date = date.plusDays(1)
    }
    return null
}
