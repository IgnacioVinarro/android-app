package com.ignaciovinarro.stayfocused.gym.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Clase abstracta (no interfaz) porque Room solo permite @Transaction con cuerpo en clases abstractas.
@Dao
abstract class WorkoutRoutineDao {

    @Insert
    abstract suspend fun insert(routine: WorkoutRoutineEntity): Long

    @Update
    abstract suspend fun update(routine: WorkoutRoutineEntity)

    @Query("SELECT * FROM workout_routine WHERE id = :id AND status = 0")
    abstract suspend fun getById(id: Long): WorkoutRoutineEntity?

    @Query("SELECT * FROM workout_routine WHERE status = 0 ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<WorkoutRoutineEntity>>

    @Insert
    abstract suspend fun insertItems(items: List<RoutineExerciseEntity>)

    @Query("SELECT * FROM routine_exercise WHERE routineId = :routineId AND status = 0 ORDER BY position")
    abstract fun observeItems(routineId: Long): Flow<List<RoutineExerciseEntity>>

    @Query("SELECT * FROM routine_exercise WHERE routineId = :routineId AND status = 0 ORDER BY position")
    abstract suspend fun getItems(routineId: Long): List<RoutineExerciseEntity>

    @Query("UPDATE routine_exercise SET status = -1, updatedAt = :updatedAt WHERE routineId = :routineId AND status = 0")
    protected abstract suspend fun softDeleteItems(routineId: Long, updatedAt: Long)

    @Query("UPDATE workout_routine SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    protected abstract suspend fun softDeleteRow(id: Long, updatedAt: Long)

    // Reemplaza los ejercicios de la rutina por `items` (con `position` 0, 1, 2…) en una sola transacción.
    @Transaction
    open suspend fun replaceItems(routineId: Long, items: List<RoutineExerciseEntity>, now: Long) {
        softDeleteItems(routineId, now)
        insertItems(
            items.mapIndexed { index, item ->
                item.copy(id = 0, routineId = routineId, position = index, updatedAt = now)
            }
        )
    }

    // Borra la rutina y sus ejercicios (lógico) en una sola transacción. Las sesiones ya hechas
    // conservan su `routineId`: el historial no se toca.
    @Transaction
    open suspend fun softDelete(id: Long, updatedAt: Long) {
        softDeleteItems(id, updatedAt)
        softDeleteRow(id, updatedAt)
    }
}
