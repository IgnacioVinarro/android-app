package com.ignaciovinarro.stayfocused.gym.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoutineModule {

    @Provides
    @Singleton
    fun provideRoutineRepository(routineDao: RoutineDao): RoutineRepository =
        RoutineRepositoryImpl(routineDao)
}

interface RoutineRepository {
    fun observeAll(): Flow<List<WorkoutRoutineEntity>>
    fun observeItems(routineId: Long): Flow<List<RoutineExerciseEntity>>
    suspend fun getById(id: Long): WorkoutRoutineEntity?
    suspend fun getItems(routineId: Long): List<RoutineExerciseEntity>

    // Crea la rutina con sus ejercicios; `position` se reasigna según el orden de la lista.
    suspend fun create(routine: WorkoutRoutineEntity, items: List<RoutineExerciseEntity>): Long

    // Actualiza la rutina y reemplaza todos sus ejercicios por `items`.
    suspend fun update(routine: WorkoutRoutineEntity, items: List<RoutineExerciseEntity>)

    // Borrado lógico de la rutina y sus ejercicios. El historial de sesiones no se toca.
    suspend fun delete(id: Long)
}

class RoutineRepositoryImpl(
    private val routineDao: RoutineDao
) : RoutineRepository {

    override fun observeAll(): Flow<List<WorkoutRoutineEntity>> = routineDao.observeAll()

    override fun observeItems(routineId: Long): Flow<List<RoutineExerciseEntity>> =
        routineDao.observeItems(routineId)

    override suspend fun getById(id: Long): WorkoutRoutineEntity? = routineDao.getById(id)

    override suspend fun getItems(routineId: Long): List<RoutineExerciseEntity> =
        routineDao.getItems(routineId)

    // Son dos escrituras seguidas (no una sola transacción de Room): si falla la segunda queda la rutina
    // sin ejercicios, que es un estado válido y editable.
    override suspend fun create(routine: WorkoutRoutineEntity, items: List<RoutineExerciseEntity>): Long {
        val id = routineDao.insert(routine)
        routineDao.replaceItems(id, items, System.currentTimeMillis())
        return id
    }

    override suspend fun update(routine: WorkoutRoutineEntity, items: List<RoutineExerciseEntity>) {
        val now = System.currentTimeMillis()
        routineDao.update(routine.copy(updatedAt = now))
        routineDao.replaceItems(routine.id, items, now)
    }

    override suspend fun delete(id: Long) {
        routineDao.softDelete(id, System.currentTimeMillis())
    }
}
