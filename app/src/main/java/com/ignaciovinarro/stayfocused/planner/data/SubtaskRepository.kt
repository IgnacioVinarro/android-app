package com.ignaciovinarro.stayfocused.planner.data

import com.ignaciovinarro.stayfocused.planner.domain.SubtaskDraft
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SubtaskModule {

    @Provides
    @Singleton
    fun provideSubtaskRepository(
        subtaskDao: SubtaskDao,
        subtaskCompletionDao: SubtaskCompletionDao
    ): SubtaskRepository = SubtaskRepositoryImpl(subtaskDao, subtaskCompletionDao)
}

interface SubtaskRepository {
    suspend fun getByTask(taskId: Long): List<SubtaskEntity>

    // Todas las subtareas activas y los tildes de un día (para la línea de tiempo y el progreso).
    fun observeAll(): Flow<List<SubtaskEntity>>
    fun observeCompletions(date: Long): Flow<List<SubtaskCompletionEntity>>

    // Deja a la tarea con exactamente estas subtareas, en este orden (la posición es el índice de la
    // lista). Las que tienen `id` y ya existen conservan su `uuid` y su `done` (solo cambian título y
    // posición); las que no tienen `id` se insertan; las existentes que no vienen se borran lógicamente.
    suspend fun replaceForTask(taskId: Long, subtasks: List<SubtaskDraft>)

    // Tilde de una subtarea de una tarea SIMPLE: se guarda en la propia subtarea.
    suspend fun setDone(subtaskId: Long, done: Boolean)

    // Tilde de una subtarea de una tarea RECURRENTE en una ocurrencia (epochDay): se guarda aparte,
    // para que tildarla hoy no la tilde en los demás días.
    suspend fun setDoneOn(subtaskId: Long, date: Long, done: Boolean)

    suspend fun deleteByTask(taskId: Long)
}

class SubtaskRepositoryImpl(
    private val subtaskDao: SubtaskDao,
    private val completionDao: SubtaskCompletionDao
) : SubtaskRepository {

    override suspend fun getByTask(taskId: Long): List<SubtaskEntity> = subtaskDao.getByTask(taskId)

    override fun observeAll(): Flow<List<SubtaskEntity>> = subtaskDao.observeAll()

    override fun observeCompletions(date: Long): Flow<List<SubtaskCompletionEntity>> =
        completionDao.observeByDate(date)

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

    override suspend fun setDone(subtaskId: Long, done: Boolean) {
        subtaskDao.updateDone(subtaskId, done, System.currentTimeMillis())
    }

    override suspend fun setDoneOn(subtaskId: Long, date: Long, done: Boolean) {
        val now = System.currentTimeMillis()
        val completedAt = if (done) now else null
        val existing = completionDao.getBySubtaskAndDate(subtaskId, date)
        if (existing == null) {
            // Destildar algo que nunca se tildó no necesita fila.
            if (done) completionDao.insert(SubtaskCompletionEntity(subtaskId = subtaskId, date = date, completedAt = now))
        } else {
            completionDao.update(existing.copy(completedAt = completedAt, updatedAt = now))
        }
    }

    override suspend fun deleteByTask(taskId: Long) {
        subtaskDao.softDeleteByTask(taskId, System.currentTimeMillis())
    }
}
