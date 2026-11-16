package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import java.time.LocalDate

// Alcance elegido al editar o borrar una ocurrencia de una tarea recurrente (PL-15).
enum class EditScope { ONLY_THIS, THIS_AND_FOLLOWING, ALL }

// Campos editables de una tarea (PL-15): no incluye el patrón de recurrencia en sí (tipo,
// intervalo, días, fin), que no se puede editar desde "esta y siguientes" en este paso.
data class TaskEditFields(
    val title: String,
    val notes: String?,
    val startMinutes: Int,
    val durationMinutes: Int,
    val iconKey: String,
    val colorKey: String
)

// "Todas" (PL-15): pisa los campos editables sobre la tarea, afecta todas las ocurrencias
// pasadas y futuras. Función pura: no toca la base, eso lo hace el repositorio.
fun applyEditToAll(original: TaskEntity, edit: TaskEditFields): TaskEntity = original.copy(
    title = edit.title,
    notes = edit.notes,
    startMinutes = edit.startMinutes,
    durationMinutes = edit.durationMinutes,
    iconKey = edit.iconKey,
    colorKey = edit.colorKey
)

// "Esta y siguientes" (PL-15, CLAUDE.md: "Editar 'esta y siguientes' divide la regla en dos
// tareas"): la original recorta su fin al día anterior a `fromDate` y se arma una tarea nueva con
// los campos editados que arranca en `fromDate` y continúa el mismo patrón de recurrencia. Si
// `fromDate` es la primera ocurrencia (no hay ninguna anterior), dividir no tiene sentido: se
// devuelve el mismo resultado que `applyEditToAll` y ninguna tarea nueva.
fun applyEditFromDate(original: TaskEntity, edit: TaskEditFields, fromDate: LocalDate): Pair<TaskEntity, TaskEntity?> {
    val startDate = LocalDate.ofEpochDay(original.date)
    if (!fromDate.isAfter(startDate)) {
        return applyEditToAll(original, edit) to null
    }

    val truncatedOriginal = original.copy(recurrenceEnd = fromDate.minusDays(1).toEpochDay())
    val newTask = TaskEntity(
        title = edit.title,
        notes = edit.notes,
        date = fromDate.toEpochDay(),
        startMinutes = edit.startMinutes,
        durationMinutes = edit.durationMinutes,
        iconKey = edit.iconKey,
        colorKey = edit.colorKey,
        recurrenceType = original.recurrenceType,
        recurrenceInterval = original.recurrenceInterval,
        recurrenceDays = original.recurrenceDays,
        recurrenceEnd = original.recurrenceEnd,
        routineId = original.routineId
    )
    return truncatedOriginal to newTask
}

// Borrar "esta y siguientes": recorta el fin de la recurrencia, sin crear ninguna tarea nueva.
// Si `fromDate` es la primera ocurrencia, el llamador debe borrar la tarea entera (equivale a
// "todas"); esta función asume que ya se descartó ese caso.
fun truncateRecurrenceFromDate(original: TaskEntity, fromDate: LocalDate): TaskEntity =
    original.copy(recurrenceEnd = fromDate.minusDays(1).toEpochDay())
