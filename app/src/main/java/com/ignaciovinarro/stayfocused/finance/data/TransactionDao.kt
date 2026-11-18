package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TransactionDao {

    @Insert
    abstract suspend fun insert(transaction: TransactionEntity): Long

    @Update
    abstract suspend fun update(transaction: TransactionEntity)

    @Query("SELECT * FROM finance_transaction WHERE id = :id AND status = 0")
    abstract suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM finance_transaction WHERE status = 0 ORDER BY date DESC, id DESC")
    abstract fun observeAll(): Flow<List<TransactionEntity>>

    // Rango de epochDay, ambos extremos incluidos.
    @Query(
        "SELECT * FROM finance_transaction WHERE status = 0 AND date BETWEEN :fromDay AND :toDay " +
            "ORDER BY date DESC, id DESC"
    )
    abstract fun observeBetween(fromDay: Long, toDay: Long): Flow<List<TransactionEntity>>

    @Query("UPDATE finance_transaction SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    protected abstract suspend fun softDeleteRow(id: Long, updatedAt: Long)

    @Query("UPDATE transaction_tag SET status = -1, updatedAt = :updatedAt WHERE transactionId = :id AND status = 0")
    protected abstract suspend fun softDeleteTagLinks(id: Long, updatedAt: Long)

    // Borra la transacción y sus uniones con etiquetas en una sola transacción.
    @Transaction
    open suspend fun softDelete(id: Long, updatedAt: Long) {
        softDeleteTagLinks(id, updatedAt)
        softDeleteRow(id, updatedAt)
    }
}
