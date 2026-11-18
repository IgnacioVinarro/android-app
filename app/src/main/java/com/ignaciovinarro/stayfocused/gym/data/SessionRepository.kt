package com.ignaciovinarro.stayfocused.gym.data

import com.ignaciovinarro.stayfocused.gym.domain.NewRecord
import com.ignaciovinarro.stayfocused.gym.domain.SessionState
import com.ignaciovinarro.stayfocused.gym.domain.detectRecords
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {

    @Provides
    @Singleton
    fun provideSessionRepository(sessionDao: SessionDao): SessionRepository =
        SessionRepositoryImpl(sessionDao)
}

// Un récord logrado en una sesión, con el ejercicio que lo logró.
data class ExerciseRecord(val exerciseId: Long, val record: NewRecord)

interface SessionRepository {
    fun observeInProgress(): Flow<WorkoutSessionEntity?>
    fun observeHistory(): Flow<List<WorkoutSessionEntity>>

    // Rango de epochDay, ambos extremos incluidos.
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<WorkoutSessionEntity>>
    fun observeSets(sessionId: Long): Flow<List<SessionSetEntity>>
    fun observeNotes(sessionId: Long): Flow<List<SessionExerciseNoteEntity>>
    fun observeRecords(): Flow<List<PersonalRecordEntity>>
    suspend fun getById(id: Long): WorkoutSessionEntity?

    // Empieza una sesión en curso. `routineId` nulo = sesión libre (GY-14).
    suspend fun start(routineId: Long?, date: Long, startedAt: Long): Long

    // La sesión en curso se guarda serie por serie: `id == 0` inserta, otro valor actualiza.
    suspend fun saveSet(set: SessionSetEntity): Long
    suspend fun deleteSet(id: Long)

    // GY-06: series del mismo ejercicio en la última sesión terminada (vacío si no hay).
    suspend fun getLastSets(exerciseId: Long, excludeSessionId: Long): List<SessionSetEntity>

    // GY-17: nota del ejercicio en esa sesión (texto vacío la deja en blanco) y la última nota escrita.
    suspend fun saveNote(sessionId: Long, exerciseId: Long, note: String)
    suspend fun getLastNote(exerciseId: Long): SessionExerciseNoteEntity?

    // Termina la sesión en curso, detecta y guarda los récords (GY-09) y los devuelve.
    // Si la sesión no existe o no está en curso no hace nada y devuelve lista vacía.
    suspend fun complete(sessionId: Long, endedAt: Long): List<ExerciseRecord>

    // Sesión abandonada = incompleta que conserva las series (CLAUDE.md). No genera récords.
    suspend fun abandon(sessionId: Long, endedAt: Long)

    // Borrado lógico de la sesión con sus series, notas y récords.
    suspend fun delete(id: Long)
}

class SessionRepositoryImpl(
    private val sessionDao: SessionDao
) : SessionRepository {

    override fun observeInProgress(): Flow<WorkoutSessionEntity?> = sessionDao.observeInProgress()

    override fun observeHistory(): Flow<List<WorkoutSessionEntity>> = sessionDao.observeHistory()

    override fun observeBetween(fromDay: Long, toDay: Long): Flow<List<WorkoutSessionEntity>> =
        sessionDao.observeBetween(fromDay, toDay)

    override fun observeSets(sessionId: Long): Flow<List<SessionSetEntity>> = sessionDao.observeSets(sessionId)

    override fun observeNotes(sessionId: Long): Flow<List<SessionExerciseNoteEntity>> =
        sessionDao.observeNotes(sessionId)

    override fun observeRecords(): Flow<List<PersonalRecordEntity>> = sessionDao.observeRecords()

    override suspend fun getById(id: Long): WorkoutSessionEntity? = sessionDao.getById(id)

    override suspend fun start(routineId: Long?, date: Long, startedAt: Long): Long =
        sessionDao.insert(
            WorkoutSessionEntity(
                routineId = routineId,
                date = date,
                startedAt = startedAt,
                state = SessionState.IN_PROGRESS.name
            )
        )

    override suspend fun saveSet(set: SessionSetEntity): Long =
        if (set.id == 0L) {
            sessionDao.insertSet(set)
        } else {
            sessionDao.updateSet(set.copy(updatedAt = System.currentTimeMillis()))
            set.id
        }

    override suspend fun deleteSet(id: Long) {
        sessionDao.softDeleteSet(id, System.currentTimeMillis())
    }

    override suspend fun getLastSets(exerciseId: Long, excludeSessionId: Long): List<SessionSetEntity> =
        sessionDao.getLastSets(exerciseId, excludeSessionId)

    override suspend fun saveNote(sessionId: Long, exerciseId: Long, note: String) {
        val existing = sessionDao.getNote(sessionId, exerciseId)
        sessionDao.upsertNote(
            (existing ?: SessionExerciseNoteEntity(sessionId = sessionId, exerciseId = exerciseId, note = note))
                .copy(note = note, updatedAt = System.currentTimeMillis())
        )
    }

    override suspend fun getLastNote(exerciseId: Long): SessionExerciseNoteEntity? =
        sessionDao.getLastNote(exerciseId)

    override suspend fun complete(sessionId: Long, endedAt: Long): List<ExerciseRecord> {
        val session = sessionDao.getById(sessionId) ?: return emptyList()
        if (session.state != SessionState.IN_PROGRESS.name) return emptyList()

        val sets = sessionDao.getSets(sessionId)
        val found = mutableListOf<ExerciseRecord>()
        sets.groupBy { it.exerciseId }.forEach { (exerciseId, exerciseSets) ->
            // Los récords guardados son todos de sesiones anteriores: este es el único momento en que se crean.
            val bests = sessionDao.getRecords(exerciseId).toBests()
            detectRecords(exerciseSets.map { it.toSetLog() }, bests).forEach {
                found += ExerciseRecord(exerciseId, it)
            }
        }
        if (found.isNotEmpty()) {
            sessionDao.insertRecords(
                found.map {
                    PersonalRecordEntity(
                        exerciseId = it.exerciseId,
                        kind = it.record.kind.name,
                        value = it.record.value,
                        weight = it.record.weight,
                        sessionSetId = it.record.setId,
                        achievedAt = endedAt
                    )
                }
            )
        }
        sessionDao.update(
            session.copy(state = SessionState.COMPLETED.name, endedAt = endedAt, updatedAt = System.currentTimeMillis())
        )
        return found
    }

    override suspend fun abandon(sessionId: Long, endedAt: Long) {
        val session = sessionDao.getById(sessionId) ?: return
        if (session.state != SessionState.IN_PROGRESS.name) return
        sessionDao.update(
            session.copy(state = SessionState.ABANDONED.name, endedAt = endedAt, updatedAt = System.currentTimeMillis())
        )
    }

    override suspend fun delete(id: Long) {
        sessionDao.softDelete(id, System.currentTimeMillis())
    }
}
