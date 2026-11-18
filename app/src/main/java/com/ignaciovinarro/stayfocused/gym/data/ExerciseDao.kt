package com.ignaciovinarro.stayfocused.gym.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercise WHERE id = :id AND status = 0")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE status = 0 ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    // Cuenta TODAS las filas, también las borradas: sirve para sembrar la biblioteca una sola vez.
    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun countAllRows(): Int

    @Query("UPDATE exercise SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    suspend fun softDelete(id: Long, updatedAt: Long)
}
