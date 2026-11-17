package com.ignaciovinarro.stayfocused.planner.data

import com.ignaciovinarro.stayfocused.planner.domain.SubtaskDraft
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SubtaskModule {

    @Provides
    @Singleton
    fun provideSubtaskRepository(subtaskDao: SubtaskDao): SubtaskRepository = SubtaskRepositoryImpl(subtaskDao)
}

interface SubtaskRepository {
    suspend fun getByTask(taskId: Long): List<SubtaskEntity>

    // Deja a la tarea con exactamente estas subtareas, en este orden (la posición es el índice de la
    // lista). Las que tienen `id` y ya existen conservan su `uuid` y su `done` (solo cambian título y
    // posición); las que no tienen `id` se insertan; las existentes que no vienen se borran lógicamente.
    suspend fun replaceForTask(taskId: Long, subtasks: List<SubtaskDraft>)

    suspend fun deleteByTask(taskId: Long)
}

class SubtaskRepositoryImpl(
    private val subtaskDao: SubtaskDao
) : SubtaskRepository {

    override suspend fun getByTask(taskId: Long): List<SubtaskEntity> = subtaskDao.getByTask(taskId)

    override suspend fun replaceForTask(taskId: Long, subtasks: List<SubtaskDraft>) {
        val existing = subtaskDao.getByTask(taskId)
        val existingIds = existing.map { it.id }.toSet()
        val keptIds = subtasks.mapNotNull { it.id }.filter { it in existingIds }.toSet()
        val now = System.currentTimeMillis()

        existing.filter { it.id !in keptIds }.forEach { subtaskDao.softDelete(it.id, now) }

        subtasks.forEachIndexed { position, draft ->
            if (draft.id != null && draft.id in keptIds) {
                subtaskDao.updateTitleAndPosition(draft.id, draft.title, position, now)
            } else {
                subtaskDao.insert(SubtaskEntity(taskId = taskId, title = draft.title, position = position))
            }
        }
    }

    override suspend fun deleteByTask(taskId: Long) {
        subtaskDao.softDeleteByTask(taskId, System.currentTimeMillis())
    }
}
