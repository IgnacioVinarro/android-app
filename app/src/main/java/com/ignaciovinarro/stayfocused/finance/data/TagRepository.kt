package com.ignaciovinarro.stayfocused.finance.data

import com.ignaciovinarro.stayfocused.finance.domain.normalizeTagName
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TagModule {

    @Provides
    @Singleton
    fun provideTagRepository(tagDao: TagDao, transactionTagDao: TransactionTagDao): TagRepository =
        TagRepositoryImpl(tagDao, transactionTagDao)
}

interface TagRepository {
    fun observeAll(): Flow<List<TagEntity>>

    // Uniones activas transacción↔etiqueta, para mostrar las etiquetas de cada transacción.
    fun observeLinks(): Flow<List<TransactionTagEntity>>

    // No valida el nombre: eso lo hace `validateTagName` en quien llama. Acá solo se recorta.
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)

    // Borra la etiqueta (lógico) y la quita de todas las transacciones.
    suspend fun delete(id: Long)
}

class TagRepositoryImpl(
    private val tagDao: TagDao,
    private val transactionTagDao: TransactionTagDao
) : TagRepository {

    override fun observeAll(): Flow<List<TagEntity>> = tagDao.observeAll()

    override fun observeLinks(): Flow<List<TransactionTagEntity>> = transactionTagDao.observeActive()

    override suspend fun create(name: String): Long = tagDao.insert(TagEntity(name = normalizeTagName(name)))

    override suspend fun rename(id: Long, name: String) {
        val tag = tagDao.getById(id) ?: return
        tagDao.update(tag.copy(name = normalizeTagName(name), updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long) {
        tagDao.softDelete(id, System.currentTimeMillis())
    }
}
