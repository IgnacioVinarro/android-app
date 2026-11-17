package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE task SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("SELECT * FROM task WHERE id = :id AND status = 0")
    suspend fun getById(id: Long): TaskEntity?

    // Tareas atrasadas (PL-14): simples, no completadas y con fecha anterior a hoy, de la más vieja a
    // la más nueva (con hora primero dentro del mismo día). Las recurrentes quedan fuera (ticket 0017).
    @Query(
        "SELECT * FROM task WHERE status = 0 AND recurrenceType IS NULL AND completedAt IS NULL " +
            "AND date < :today ORDER BY date, startMinutes IS NULL, startMinutes"
    )
    fun observeOverdue(today: Long): Flow<List<TaskEntity>>

    // Solo las tareas con hora pueden tener recordatorios (ticket 0012).
    @Query("SELECT * FROM task WHERE status = 0 AND startMinutes IS NOT NULL")
    suspend fun getAllTimed(): List<TaskEntity>

    // Para una fecha, trae las tareas simples de ese día y, además, todas las tareas recurrentes
    // cuyo inicio ya pasó (pueden tener ocurrencia ese día; `occursOn` decide cuáles exactamente).
    @Query(
        "SELECT * FROM task WHERE status = 0 AND (" +
            "(recurrenceType IS NULL AND date = :date) OR " +
            "(recurrenceType IS NOT NULL AND date <= :date)" +
            ") ORDER BY startMinutes IS NULL, startMinutes"
    )
    fun observeByDate(date: Long): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM task WHERE status = 0 AND date BETWEEN :startDate AND :endDate " +
            "ORDER BY date, startMinutes IS NULL, startMinutes"
    )
    fun observeByDateRange(startDate: Long, endDate: Long): Flow<List<TaskEntity>>
}
