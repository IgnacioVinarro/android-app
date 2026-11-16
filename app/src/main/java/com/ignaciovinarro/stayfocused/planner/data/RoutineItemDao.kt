package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineItemDao {

    @Insert
    suspend fun insert(item: RoutineItemEntity): Long

    @Update
    suspend fun update(item: RoutineItemEntity)

    @Query("UPDATE routine_item SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("SELECT * FROM routine_item WHERE routineId = :routineId AND status = 0 ORDER BY position")
    fun observeByRoutine(routineId: Long): Flow<List<RoutineItemEntity>>

    @Query("SELECT * FROM routine_item WHERE routineId = :routineId AND status = 0 ORDER BY position")
    suspend fun getByRoutine(routineId: Long): List<RoutineItemEntity>
}
