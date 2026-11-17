package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import java.time.LocalDate

// Función pura (PL-14): una tarea está atrasada si es simple (no recurrente), no está completada ni
// borrada y su fecha es anterior a hoy, con o sin hora. Una tarea de hoy cuya hora ya pasó NO está
// atrasada. Las recurrentes quedan fuera: sus ocurrencias pasadas no tienen fila (ticket 0017).
fun isOverdue(task: TaskEntity, today: LocalDate): Boolean =
    task.status == 0 &&
        task.recurrenceType == null &&
        task.completedAt == null &&
        task.date < today.toEpochDay()

// "Reprogramar": cambia el día y conserva la hora (y la duración, los recordatorios y lo demás).
// Devuelve nulo si la tarea no está atrasada o si la fecha elegida es anterior a hoy.
fun rescheduleOverdueTask(task: TaskEntity, newDate: LocalDate, today: LocalDate): TaskEntity? {
    if (!isOverdue(task, today) || newDate.isBefore(today)) return null
    return task.copy(date = newDate.toEpochDay())
}

// "A la bandeja": pasa a la bandeja de HOY, o sea, fecha de hoy y sin hora. Devuelve nulo si la tarea
// no está atrasada. Quien la guarda debe borrar también sus recordatorios: sin hora no los hay.
fun moveToInbox(task: TaskEntity, today: LocalDate): TaskEntity? {
    if (!isOverdue(task, today)) return null
    return task.copy(date = today.toEpochDay(), startMinutes = null)
}
