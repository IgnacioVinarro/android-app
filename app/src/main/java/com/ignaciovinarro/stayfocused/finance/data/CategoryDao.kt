package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Clase abstracta (no interfaz) porque Room solo permite @Transaction con cuerpo en clases abstractas.
@Dao
abstract class CategoryDao {

    @Insert
    abstract suspend fun insert(category: CategoryEntity): Long

    @Insert
    abstract suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    abstract suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM category WHERE id = :id AND status = 0")
    abstract suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM category WHERE status = 0 ORDER BY type, name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<CategoryEntity>>

    // Cuenta TODAS las filas, también las borradas: sirve para sembrar las predefinidas una sola vez.
    @Query("SELECT COUNT(*) FROM category")
    abstract suspend fun countAllRows(): Int

    @Query("SELECT COUNT(*) FROM finance_transaction WHERE categoryId = :categoryId AND status = 0")
    abstract suspend fun countTransactions(categoryId: Long): Int

    @Query(
        "UPDATE finance_transaction SET categoryId = :toId, updatedAt = :updatedAt " +
            "WHERE categoryId = :fromId AND status = 0"
    )
    protected abstract suspend fun reassignTransactions(fromId: Long, toId: Long, updatedAt: Long)

    @Query("UPDATE budget SET status = -1, updatedAt = :updatedAt WHERE categoryId = :categoryId AND status = 0")
    protected abstract suspend fun softDeleteBudgets(categoryId: Long, updatedAt: Long)

    @Query("UPDATE category SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    protected abstract suspend fun softDeleteRow(id: Long, updatedAt: Long)

    // Borra la categoría (lógico) y, en la misma transacción, mueve sus transacciones a `reassignToId`
    // y borra sus presupuestos. Todo o nada.
    @Transaction
    open suspend fun deleteReassigning(id: Long, reassignToId: Long?, updatedAt: Long) {
        if (reassignToId != null) reassignTransactions(id, reassignToId, updatedAt)
        softDeleteBudgets(id, updatedAt)
        softDeleteRow(id, updatedAt)
    }
}
