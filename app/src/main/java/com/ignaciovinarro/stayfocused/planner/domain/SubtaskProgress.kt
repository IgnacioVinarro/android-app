package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import java.time.LocalDate

// Una subtarea tal como se ve en un día puntual: su título y si está tildada ESE día.
data class SubtaskItem(val id: Long, val title: String, val done: Boolean)

// Función pura (PL-05): las subtareas de una tarea en un día, en orden, con su tilde. En una tarea
// simple el tilde es `subtask.done`; en una recurrente es por ocurrencia: está tildada si hay un
// `subtask_completion` de esa fecha con `completedAt`. Ignora las subtareas de otras tareas, las
// borradas y los tildes de otros días.
fun subtaskItemsFor(
    taskId: Long,
    recurring: Boolean,
    date: LocalDate,
    subtasks: List<SubtaskEntity>,
    completions: List<SubtaskCompletionEntity>
): List<SubtaskItem> {
    val epochDay = date.toEpochDay()
    val doneThatDay = completions
        .filter { it.status == 0 && it.date == epochDay && it.completedAt != null }
        .map { it.subtaskId }
        .toSet()
    return subtasks
        .filter { it.taskId == taskId && it.status == 0 }
        .sortedBy { it.position }
        .map { SubtaskItem(id = it.id, title = it.title, done = if (recurring) it.id in doneThatDay else it.done) }
}

// Progreso del día: tareas completadas + subtareas completadas, sobre el total de ambas.
data class DayProgress(val done: Int, val total: Int) {
    // De 0 a 1; con total 0 es 0 (no hay nada que mostrar, la pantalla oculta el encabezado).
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
}

// Función pura (PL-05). Cuenta solo lo que se completa desde Planificación; los bloques de otros
// módulos se completan en su propia pantalla y no entran acá (ticket 0016).
fun dayProgress(tasksDone: Int, tasksTotal: Int, subtasksDone: Int, subtasksTotal: Int): DayProgress =
    DayProgress(done = tasksDone + subtasksDone, total = tasksTotal + subtasksTotal)
