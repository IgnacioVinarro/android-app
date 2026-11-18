package com.ignaciovinarro.stayfocused.gym.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SessionDao {

    // --- Sesiones ---

    @Insert
    abstract suspend fun insert(session: WorkoutSessionEntity): Long

    @Update
    abstract suspend fun update(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_session WHERE id = :id AND status = 0")
    abstract suspend fun getById(id: Long): WorkoutSessionEntity?

    // La sesión en curso (a lo sumo una): permite retomarla si la app se cerró.
    @Query("SELECT * FROM workout_session WHERE state = 'IN_PROGRESS' AND status = 0 ORDER BY startedAt DESC LIMIT 1")
    abstract fun observeInProgress(): Flow<WorkoutSessionEntity?>

    // Historial (GY-10): sesiones terminadas, la más reciente primero.
    @Query("SELECT * FROM workout_session WHERE state = 'COMPLETED' AND status = 0 ORDER BY startedAt DESC")
    abstract fun observeHistory(): Flow<List<WorkoutSessionEntity>>

    // Rango de epochDay, ambos extremos incluidos.
    @Query("SELECT * FROM workout_session WHERE status = 0 AND date BETWEEN :fromDay AND :toDay ORDER BY startedAt")
    abstract fun observeBetween(fromDay: Long, toDay: Long): Flow<List<WorkoutSessionEntity>>

    // --- Series ---

    @Insert
    abstract suspend fun insertSet(set: SessionSetEntity): Long

    @Update
    abstract suspend fun updateSet(set: SessionSetEntity)

    @Query("SELECT * FROM session_set WHERE sessionId = :sessionId AND status = 0 ORDER BY exerciseId, position")
    abstract fun observeSets(sessionId: Long): Flow<List<SessionSetEntity>>

    @Query("SELECT * FROM session_set WHERE sessionId = :sessionId AND status = 0 ORDER BY exerciseId, position")
    abstract suspend fun getSets(sessionId: Long): List<SessionSetEntity>

    @Query("UPDATE session_set SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    abstract suspend fun softDeleteSet(id: Long, updatedAt: Long)

    // GY-06: series de la sesión TERMINADA más reciente (distinta de `excludeSessionId`) que tuvo ese ejercicio.
    @Query(
        """
        SELECT s.* FROM session_set s
        WHERE s.status = 0 AND s.exerciseId = :exerciseId AND s.sessionId = (
            SELECT w.id FROM workout_session w
            JOIN session_set x ON x.sessionId = w.id AND x.exerciseId = :exerciseId AND x.status = 0
            WHERE w.status = 0 AND w.state = 'COMPLETED' AND w.id != :excludeSessionId
            ORDER BY w.startedAt DESC LIMIT 1
        )
        ORDER BY s.position
        """
    )
    abstract suspend fun getLastSets(exerciseId: Long, excludeSessionId: Long): List<SessionSetEntity>

    // --- Notas por ejercicio (GY-17) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertNote(note: SessionExerciseNoteEntity): Long

    @Query("SELECT * FROM session_exercise_note WHERE sessionId = :sessionId AND status = 0")
    abstract fun observeNotes(sessionId: Long): Flow<List<SessionExerciseNoteEntity>>

    @Query("SELECT * FROM session_exercise_note WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND status = 0")
    abstract suspend fun getNote(sessionId: Long, exerciseId: Long): SessionExerciseNoteEntity?

    // Última nota escrita para el ejercicio, para mostrarla al repetir la rutina.
    @Query(
        "SELECT * FROM session_exercise_note WHERE exerciseId = :exerciseId AND status = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1"
    )
    abstract suspend fun getLastNote(exerciseId: Long): SessionExerciseNoteEntity?

    // --- Récords ---

    @Insert
    abstract suspend fun insertRecords(records: List<PersonalRecordEntity>)

    @Query("SELECT * FROM personal_record WHERE exerciseId = :exerciseId AND status = 0")
    abstract suspend fun getRecords(exerciseId: Long): List<PersonalRecordEntity>

    @Query("SELECT * FROM personal_record WHERE status = 0 ORDER BY achievedAt DESC")
    abstract fun observeRecords(): Flow<List<PersonalRecordEntity>>

    // --- Borrado lógico ---

    @Query(
        "UPDATE personal_record SET status = -1, updatedAt = :updatedAt WHERE status = 0 AND sessionSetId IN " +
            "(SELECT id FROM session_set WHERE sessionId = :sessionId)"
    )
    protected abstract suspend fun softDeleteRecords(sessionId: Long, updatedAt: Long)

    @Query("UPDATE session_set SET status = -1, updatedAt = :updatedAt WHERE sessionId = :sessionId AND status = 0")
    protected abstract suspend fun softDeleteSets(sessionId: Long, updatedAt: Long)

    @Query("UPDATE session_exercise_note SET status = -1, updatedAt = :updatedAt WHERE sessionId = :sessionId AND status = 0")
    protected abstract suspend fun softDeleteNotes(sessionId: Long, updatedAt: Long)

    @Query("UPDATE workout_session SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    protected abstract suspend fun softDeleteRow(id: Long, updatedAt: Long)

    // Borra la sesión con sus series, notas y los récords que lograron (todo lógico, una transacción).
    // Los récords van ANTES que las series porque se buscan a través de las series de la sesión.
    @Transaction
    open suspend fun softDelete(id: Long, updatedAt: Long) {
        softDeleteRecords(id, updatedAt)
        softDeleteSets(id, updatedAt)
        softDeleteNotes(id, updatedAt)
        softDeleteRow(id, updatedAt)
    }
}
