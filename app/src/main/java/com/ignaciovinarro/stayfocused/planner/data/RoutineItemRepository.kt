package com.ignaciovinarro.stayfocused.planner.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoutineItemModule {

    @Provides
    @Singleton
    fun provideRoutineItemRepository(routineItemDao: RoutineItemDao): RoutineItemRepository =
        RoutineItemRepositoryImpl(routineItemDao)
}

interface RoutineItemRepository {
    fun observeByRoutine(routineId: Long): Flow<List<RoutineItemEntity>>
    suspend fun getByRoutine(routineId: Long): List<RoutineItemEntity>
    suspend fun create(item: RoutineItemEntity): Long
    suspend fun update(item: RoutineItemEntity)
    suspend fun delete(id: Long)
}

class RoutineItemRepositoryImpl(
    private val routineItemDao: RoutineItemDao
) : RoutineItemRepository {

    override fun observeByRoutine(routineId: Long): Flow<List<RoutineItemEntity>> =
        routineItemDao.observeByRoutine(routineId)

    override suspend fun getByRoutine(routineId: Long): List<RoutineItemEntity> =
        routineItemDao.getByRoutine(routineId)

    override suspend fun create(item: RoutineItemEntity): Long = routineItemDao.insert(item)

    override suspend fun update(item: RoutineItemEntity) {
        routineItemDao.update(item.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long) {
        routineItemDao.softDelete(id, System.currentTimeMillis())
    }
}
