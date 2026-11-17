package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskCompletionDao {

    @Insert
    suspend fun insert(completion: SubtaskCompletionEntity): Long

    @Update
    suspend fun update(completion: SubtaskCompletionEntity)

    @Query("SELECT * FROM subtask_completion WHERE subtaskId = :subtaskId AND date = :date AND status = 0")
    suspend fun getBySubtaskAndDate(subtaskId: Long, date: Long): SubtaskCompletionEntity?

    @Query("SELECT * FROM subtask_completion WHERE date = :date AND status = 0")
    fun observeByDate(date: Long): Flow<List<SubtaskCompletionEntity>>
}
