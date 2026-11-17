package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskOccurrenceDao {

    @Insert
    suspend fun insert(occurrence: TaskOccurrenceEntity): Long

    @Update
    suspend fun update(occurrence: TaskOccurrenceEntity)

    @Query("UPDATE task_occurrence SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("SELECT * FROM task_occurrence WHERE taskId = :taskId AND date = :date AND status = 0")
    suspend fun getByTaskAndDate(taskId: Long, date: Long): TaskOccurrenceEntity?

    @Query("SELECT * FROM task_occurrence WHERE taskId = :taskId AND status = 0")
    suspend fun getByTask(taskId: Long): List<TaskOccurrenceEntity>

    @Query("SELECT * FROM task_occurrence WHERE status = 0 AND date BETWEEN :startDate AND :endDate")
    fun observeByDateRange(startDate: Long, endDate: Long): Flow<List<TaskOccurrenceEntity>>
}
