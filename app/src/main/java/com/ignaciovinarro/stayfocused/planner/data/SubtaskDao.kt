package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {

    @Insert
    suspend fun insert(subtask: SubtaskEntity): Long

    @Query("SELECT * FROM subtask WHERE taskId = :taskId AND status = 0 ORDER BY position")
    suspend fun getByTask(taskId: Long): List<SubtaskEntity>

    // Todas las subtareas activas: la línea de tiempo y el progreso del día las necesitan de varias
    // tareas a la vez, y son pocas (uso personal), así que no hace falta filtrar por tarea.
    @Query("SELECT * FROM subtask WHERE status = 0 ORDER BY taskId, position")
    fun observeAll(): Flow<List<SubtaskEntity>>

    // Editar una subtarea que ya existía cambia solo título y posición: `done` y `uuid` se conservan.
    @Query(
        "UPDATE subtask SET title = :title, position = :position, updatedAt = :updatedAt " +
            "WHERE id = :id AND status = 0"
    )
    suspend fun updateTitleAndPosition(id: Long, title: String, position: Int, updatedAt: Long)

    // Tilde de una subtarea de una tarea simple (las recurrentes usan `subtask_completion`).
    @Query("UPDATE subtask SET done = :done, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    suspend fun updateDone(id: Long, done: Boolean, updatedAt: Long)

    @Query("UPDATE subtask SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("UPDATE subtask SET status = -1, updatedAt = :updatedAt WHERE taskId = :taskId AND status = 0")
    suspend fun softDeleteByTask(taskId: Long, updatedAt: Long)
}
