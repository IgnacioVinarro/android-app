package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReminderDao {

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Query(
        "SELECT * FROM reminder WHERE ownerType = :ownerType AND ownerId = :ownerId AND status = 0 " +
            "ORDER BY minutesBefore"
    )
    suspend fun getByOwner(ownerType: String, ownerId: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminder WHERE id = :id AND status = 0")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("UPDATE reminder SET status = -1, updatedAt = :updatedAt WHERE id = :id AND status = 0")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query(
        "UPDATE reminder SET status = -1, updatedAt = :updatedAt " +
            "WHERE ownerType = :ownerType AND ownerId = :ownerId AND status = 0"
    )
    suspend fun softDeleteByOwner(ownerType: String, ownerId: Long, updatedAt: Long)
}
