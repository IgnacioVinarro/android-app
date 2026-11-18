package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TagDao {

    @Insert
    abstract suspend fun insert(tag: TagEntity): Long

    @Update
    abstract suspend fun update(tag: TagEntity)

    @Query("SELECT * FROM tag WHERE id = :id AND status = 0")
    abstract suspend fun getById(id: Long): TagEntity?

    @Query("SELECT * FROM tag WHERE status = 0 ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<TagEntity>>

    @Query("UPDATE tag SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    protected abstract suspend fun softDeleteRow(id: Long, updatedAt: Long)

    @Query("UPDATE transaction_tag SET status = -1, updatedAt = :updatedAt WHERE tagId = :id AND status = 0")
    protected abstract suspend fun softDeleteLinks(id: Long, updatedAt: Long)

    // Borra la etiqueta y la quita de todas las transacciones, en una sola transacción.
    @Transaction
    open suspend fun softDelete(id: Long, updatedAt: Long) {
        softDeleteLinks(id, updatedAt)
        softDeleteRow(id, updatedAt)
    }
}
