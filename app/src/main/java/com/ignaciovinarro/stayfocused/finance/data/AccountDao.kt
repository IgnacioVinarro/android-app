package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("UPDATE account SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    suspend fun softDelete(id: Long, updatedAt: Long)

    // Transacciones vivas de la cuenta, como origen/titular o como destino de una transferencia.
    @Query(
        "SELECT COUNT(*) FROM finance_transaction " +
            "WHERE status = 0 AND (accountId = :accountId OR transferAccountId = :accountId)"
    )
    suspend fun countTransactions(accountId: Long): Int

    @Query("SELECT * FROM account WHERE id = :id AND status = 0")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM account WHERE status = 0 ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<AccountEntity>>
}
