package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SubtaskDao {

    @Insert
    suspend fun insert(subtask: SubtaskEntity): Long

    @Query("SELECT * FROM subtask WHERE taskId = :taskId AND status = 0 ORDER BY position")
    suspend fun getByTask(taskId: Long): List<SubtaskEntity>

    // Editar una subtarea que ya existía cambia solo título y posición: `done` y `uuid` se conservan.
    @Query(
        "UPDATE subtask SET title = :title, position = :position, updatedAt = :updatedAt " +
            "WHERE id = :id AND status = 0"
    )
    suspend fun updateTitleAndPosition(id: Long, title: String, position: Int, updatedAt: Long)

    @Query("UPDATE subtask SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("UPDATE subtask SET status = -1, updatedAt = :updatedAt WHERE taskId = :taskId AND status = 0")
    suspend fun softDeleteByTask(taskId: Long, updatedAt: Long)
}
