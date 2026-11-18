package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert
    suspend fun insert(budget: BudgetEntity): Long

    @Update
    suspend fun update(budget: BudgetEntity)

    // `IS` (y no `=`) para que `categoryId = null` (presupuesto total) también coincida.
    @Query(
        "SELECT * FROM budget WHERE categoryId IS :categoryId AND effectiveFrom = :effectiveFrom AND status = 0"
    )
    suspend fun findExact(categoryId: Long?, effectiveFrom: Int): BudgetEntity?

    @Query("SELECT * FROM budget WHERE status = 0 ORDER BY effectiveFrom")
    fun observeAll(): Flow<List<BudgetEntity>>
}
