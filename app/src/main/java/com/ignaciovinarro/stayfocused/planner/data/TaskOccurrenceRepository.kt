package com.ignaciovinarro.stayfocused.planner.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TaskOccurrenceModule {

    @Provides
    @Singleton
    fun provideTaskOccurrenceRepository(
        taskOccurrenceDao: TaskOccurrenceDao
    ): TaskOccurrenceRepository = TaskOccurrenceRepositoryImpl(taskOccurrenceDao)
}

interface TaskOccurrenceRepository {
    fun observeByDateRange(startDate: Long, endDate: Long): Flow<List<TaskOccurrenceEntity>>
    suspend fun getByTaskAndDate(taskId: Long, date: Long): TaskOccurrenceEntity?
    suspend fun getByTask(taskId: Long): List<TaskOccurrenceEntity>
    suspend fun create(occurrence: TaskOccurrenceEntity): Long
    suspend fun update(occurrence: TaskOccurrenceEntity)
    suspend fun delete(id: Long)
}

class TaskOccurrenceRepositoryImpl(
    private val taskOccurrenceDao: TaskOccurrenceDao
) : TaskOccurrenceRepository {

    override fun observeByDateRange(startDate: Long, endDate: Long): Flow<List<TaskOccurrenceEntity>> =
        taskOccurrenceDao.observeByDateRange(startDate, endDate)

    override suspend fun getByTaskAndDate(taskId: Long, date: Long): TaskOccurrenceEntity? =
        taskOccurrenceDao.getByTaskAndDate(taskId, date)

    override suspend fun getByTask(taskId: Long): List<TaskOccurrenceEntity> =
        taskOccurrenceDao.getByTask(taskId)

    override suspend fun create(occurrence: TaskOccurrenceEntity): Long = taskOccurrenceDao.insert(occurrence)

    override suspend fun update(occurrence: TaskOccurrenceEntity) {
        taskOccurrenceDao.update(occurrence.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long) {
        taskOccurrenceDao.softDelete(id, System.currentTimeMillis())
    }
}
