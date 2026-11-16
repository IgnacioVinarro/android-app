package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Insert
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Query("UPDATE routine SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("SELECT * FROM routine WHERE id = :id AND status = 0")
    suspend fun getById(id: Long): RoutineEntity?

    @Query("SELECT * FROM routine WHERE status = 0 ORDER BY name")
    fun observeAll(): Flow<List<RoutineEntity>>
}
