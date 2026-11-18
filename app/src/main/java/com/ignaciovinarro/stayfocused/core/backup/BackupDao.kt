package com.ignaciovinarro.stayfocused.core.backup

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ignaciovinarro.stayfocused.planner.data.ReminderEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity

// Único DAO del respaldo: lee TODAS las filas de todas las tablas (sin filtrar por status, a propósito:
// el borrado lógico también viaja) y reemplaza todo en una transacción. Es una clase abstracta porque
// Room solo permite @Transaction con cuerpo en clases abstractas, no en interfaces.
@Dao
abstract class BackupDao {

    @Query("SELECT * FROM task ORDER BY id")
    abstract suspend fun allTasks(): List<TaskEntity>

    @Query("SELECT * FROM task_occurrence ORDER BY id")
    abstract suspend fun allOccurrences(): List<TaskOccurrenceEntity>

    @Query("SELECT * FROM routine ORDER BY id")
    abstract suspend fun allRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routine_item ORDER BY id")
    abstract suspend fun allRoutineItems(): List<RoutineItemEntity>

    @Query("SELECT * FROM reminder ORDER BY id")
    abstract suspend fun allReminders(): List<ReminderEntity>

    @Query("SELECT * FROM subtask ORDER BY id")
    abstract suspend fun allSubtasks(): List<SubtaskEntity>

    @Query("SELECT * FROM subtask_completion ORDER BY id")
    abstract suspend fun allSubtaskCompletions(): List<SubtaskCompletionEntity>

    // Lectura consistente: las 7 tablas dentro de una misma transacción (sin cambios a mitad de camino).
    @Transaction
    open suspend fun readAll(): BackupData = BackupData(
        tasks = allTasks(),
        occurrences = allOccurrences(),
        routines = allRoutines(),
        routineItems = allRoutineItems(),
        reminders = allReminders(),
        subtasks = allSubtasks(),
        subtaskCompletions = allSubtaskCompletions()
    )

    // Excepción autorizada a "nunca DELETE físico" (ticket 0026): reemplazar todo exige vaciar las tablas.
    // Va dentro de la misma transacción que las inserciones: si algo falla, Room revierte y no se pierde nada.
    // No se usa clearAllTables() porque Room no lo permite dentro de una transacción.
    @Transaction
    open suspend fun replaceAll(data: BackupData) {
        deleteSubtaskCompletions()
        deleteSubtasks()
        deleteReminders()
        deleteRoutineItems()
        deleteRoutines()
        deleteOccurrences()
        deleteTasks()
        // REPLACE por si el archivo trae filas repetidas; los id del archivo se conservan para que las
        // referencias entre tablas (taskId, routineId...) sigan apuntando bien.
        insertTasks(data.tasks)
        insertOccurrences(data.occurrences)
        insertRoutines(data.routines)
        insertRoutineItems(data.routineItems)
        insertReminders(data.reminders)
        insertSubtasks(data.subtasks)
        insertSubtaskCompletions(data.subtaskCompletions)
    }

    @Query("DELETE FROM subtask_completion")
    protected abstract suspend fun deleteSubtaskCompletions()

    @Query("DELETE FROM subtask")
    protected abstract suspend fun deleteSubtasks()

    @Query("DELETE FROM reminder")
    protected abstract suspend fun deleteReminders()

    @Query("DELETE FROM routine_item")
    protected abstract suspend fun deleteRoutineItems()

    @Query("DELETE FROM routine")
    protected abstract suspend fun deleteRoutines()

    @Query("DELETE FROM task_occurrence")
    protected abstract suspend fun deleteOccurrences()

    @Query("DELETE FROM task")
    protected abstract suspend fun deleteTasks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertTasks(items: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertOccurrences(items: List<TaskOccurrenceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertRoutines(items: List<RoutineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertRoutineItems(items: List<RoutineItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertReminders(items: List<ReminderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSubtasks(items: List<SubtaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSubtaskCompletions(items: List<SubtaskCompletionEntity>)
}
