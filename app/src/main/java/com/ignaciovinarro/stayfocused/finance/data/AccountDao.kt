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

    @Query("SELECT * FROM account WHERE id = :id AND status = 0")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM account WHERE status = 0 ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<AccountEntity>>
}
