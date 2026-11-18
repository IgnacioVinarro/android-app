package com.ignaciovinarro.stayfocused.finance.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TransactionModule {

    @Provides
    @Singleton
    fun provideTransactionRepository(
        transactionDao: TransactionDao,
        transactionTagDao: TransactionTagDao
    ): TransactionRepository = TransactionRepositoryImpl(transactionDao, transactionTagDao)
}

interface TransactionRepository {
    fun observeAll(): Flow<List<TransactionEntity>>

    // Rango de epochDay, ambos extremos incluidos.
    fun observeBetween(fromDay: Long, toDay: Long): Flow<List<TransactionEntity>>
    suspend fun getById(id: Long): TransactionEntity?

    // Crea la transacción y le asigna las etiquetas. No valida: las reglas viven en
    // `validateTransaction` y las aplica quien llama.
    // Son dos escrituras seguidas (no una sola transacción de Room): si falla la segunda queda la
    // transacción sin etiquetas, que es un estado válido.
    suspend fun create(transaction: TransactionEntity, tagIds: Set<Long> = emptySet()): Long
    suspend fun update(transaction: TransactionEntity, tagIds: Set<Long>)
    suspend fun delete(id: Long)
}

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val transactionTagDao: TransactionTagDao
) : TransactionRepository {

    override fun observeAll(): Flow<List<TransactionEntity>> = transactionDao.observeAll()

    override fun observeBetween(fromDay: Long, toDay: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeBetween(fromDay, toDay)

    override suspend fun getById(id: Long): TransactionEntity? = transactionDao.getById(id)

    override suspend fun create(transaction: TransactionEntity, tagIds: Set<Long>): Long {
        val id = transactionDao.insert(transaction)
        if (tagIds.isNotEmpty()) transactionTagDao.replaceTags(id, tagIds, System.currentTimeMillis())
        return id
    }

    override suspend fun update(transaction: TransactionEntity, tagIds: Set<Long>) {
        val now = System.currentTimeMillis()
        transactionDao.update(transaction.copy(updatedAt = now))
        transactionTagDao.replaceTags(transaction.id, tagIds, now)
    }

    override suspend fun delete(id: Long) {
        transactionDao.softDelete(id, System.currentTimeMillis())
    }
}
