package com.ignaciovinarro.stayfocused.gym.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExerciseModule {

    @Provides
    @Singleton
    fun provideExerciseRepository(exerciseDao: ExerciseDao): ExerciseRepository =
        ExerciseRepositoryImpl(exerciseDao)
}

interface ExerciseRepository {
    fun observeAll(): Flow<List<ExerciseEntity>>
    suspend fun getById(id: Long): ExerciseEntity?
    suspend fun create(exercise: ExerciseEntity): Long
    suspend fun update(exercise: ExerciseEntity)

    // Borrado lógico. Las series ya registradas conservan su `exerciseId` (el historial no se pierde).
    suspend fun delete(id: Long)

    // Inserta la biblioteca precargada solo si la tabla nunca tuvo filas (ni siquiera borradas).
    suspend fun seedDefaults(defaults: List<ExerciseEntity>)
}

class ExerciseRepositoryImpl(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    override fun observeAll(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()

    override suspend fun getById(id: Long): ExerciseEntity? = exerciseDao.getById(id)

    override suspend fun create(exercise: ExerciseEntity): Long = exerciseDao.insert(exercise)

    override suspend fun update(exercise: ExerciseEntity) {
        exerciseDao.update(exercise.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long) {
        exerciseDao.softDelete(id, System.currentTimeMillis())
    }

    override suspend fun seedDefaults(defaults: List<ExerciseEntity>) {
        if (exerciseDao.countAllRows() == 0) exerciseDao.insertAll(defaults)
    }
}
