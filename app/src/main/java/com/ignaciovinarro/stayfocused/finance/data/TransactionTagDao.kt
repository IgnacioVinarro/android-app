package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ignaciovinarro.stayfocused.finance.domain.planTagLinkChanges
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TransactionTagDao {

    @Insert
    protected abstract suspend fun insert(link: TransactionTagEntity): Long

    @Update
    protected abstract suspend fun update(link: TransactionTagEntity)

    // Incluye las borradas: hace falta verlas para reactivarlas en vez de chocar con el índice único.
    @Query("SELECT * FROM transaction_tag WHERE transactionId = :transactionId")
    abstract suspend fun getAllForTransaction(transactionId: Long): List<TransactionTagEntity>

    @Query("SELECT * FROM transaction_tag WHERE status = 0")
    abstract fun observeActive(): Flow<List<TransactionTagEntity>>

    // Deja la transacción con exactamente las etiquetas `tagIds`.
    @Transaction
    open suspend fun replaceTags(transactionId: Long, tagIds: Set<Long>, now: Long) {
        val changes = planTagLinkChanges(getAllForTransaction(transactionId), transactionId, tagIds, now)
        changes.toUpdate.forEach { update(it) }
        changes.toInsert.forEach { insert(it) }
    }
}
