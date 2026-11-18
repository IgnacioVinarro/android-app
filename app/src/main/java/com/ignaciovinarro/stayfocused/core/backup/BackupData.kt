package com.ignaciovinarro.stayfocused.core.backup

import com.ignaciovinarro.stayfocused.planner.data.ReminderEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity

// Versión del formato del archivo de respaldo (no es la versión de la base de datos). Se sube cuando
// cambia la forma del JSON; la importación valida y convierte versiones anteriores (CLAUDE.md).
const val BACKUP_FORMAT_VERSION = 1

// Foto completa de los datos: todas las filas de todas las tablas, incluidas las borradas lógicamente
// (status = -1), para que el borrado también viaje con el respaldo.
data class BackupData(
    val tasks: List<TaskEntity> = emptyList(),
    val occurrences: List<TaskOccurrenceEntity> = emptyList(),
    val routines: List<RoutineEntity> = emptyList(),
    val routineItems: List<RoutineItemEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val subtasks: List<SubtaskEntity> = emptyList(),
    val subtaskCompletions: List<SubtaskCompletionEntity> = emptyList()
)

// El archivo no se puede importar: no es JSON válido, le falta algo o es de una versión que esta app no conoce.
class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)
