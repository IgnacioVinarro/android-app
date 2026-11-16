package com.ignaciovinarro.stayfocused.planner.data

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
    fun observeAll(): Flow<List<RoutineEntity>>
    suspend fun getById(id: Long): RoutineEntity?
    suspend fun create(routine: RoutineEntity): Long
    suspend fun update(routine: RoutineEntity)
    suspend fun delete(id: Long)
}

class RoutineRepositoryImpl(
    private val routineDao: RoutineDao
) : RoutineRepository {

    override fun observeAll(): Flow<List<RoutineEntity>> = routineDao.observeAll()

    override suspend fun getById(id: Long): RoutineEntity? = routineDao.getById(id)

    override suspend fun create(routine: RoutineEntity): Long = routineDao.insert(routine)

    override suspend fun update(routine: RoutineEntity) {
        routineDao.update(routine.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long) {
        routineDao.softDelete(id, System.currentTimeMillis())
    }
}
