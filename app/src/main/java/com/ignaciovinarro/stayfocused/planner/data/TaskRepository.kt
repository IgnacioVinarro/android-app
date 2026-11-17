package com.ignaciovinarro.stayfocused.planner.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TaskModule {

    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository = TaskRepositoryImpl(taskDao)
}

interface TaskRepository {
    fun observeByDate(date: Long): Flow<List<TaskEntity>>
    fun observeByDateRange(startDate: Long, endDate: Long): Flow<List<TaskEntity>>
    suspend fun getById(id: Long): TaskEntity?
    fun observeOverdue(today: Long): Flow<List<TaskEntity>>
    suspend fun getAllTimed(): List<TaskEntity>
    suspend fun create(task: TaskEntity): Long
    suspend fun update(task: TaskEntity)
    suspend fun delete(id: Long)
}

class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun observeByDate(date: Long): Flow<List<TaskEntity>> = taskDao.observeByDate(date)

    override fun observeByDateRange(startDate: Long, endDate: Long): Flow<List<TaskEntity>> =
        taskDao.observeByDateRange(startDate, endDate)

    override suspend fun getById(id: Long): TaskEntity? = taskDao.getById(id)

    override fun observeOverdue(today: Long): Flow<List<TaskEntity>> = taskDao.observeOverdue(today)

    override suspend fun getAllTimed(): List<TaskEntity> = taskDao.getAllTimed()

    override suspend fun create(task: TaskEntity): Long = taskDao.insert(task)

    override suspend fun update(task: TaskEntity) {
        taskDao.update(task.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long) {
        taskDao.softDelete(id, System.currentTimeMillis())
    }
}
