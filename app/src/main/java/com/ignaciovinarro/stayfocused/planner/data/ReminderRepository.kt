package com.ignaciovinarro.stayfocused.planner.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReminderModule {

    @Provides
    @Singleton
    fun provideReminderRepository(reminderDao: ReminderDao): ReminderRepository =
        ReminderRepositoryImpl(reminderDao)
}

interface ReminderRepository {
    suspend fun getByOwner(ownerType: String, ownerId: Long): List<ReminderEntity>
    suspend fun getById(id: Long): ReminderEntity?

    // Deja al dueño con exactamente este conjunto de `minutesBefore`: borra lógicamente los que
    // sobran, inserta los que faltan y no toca los que ya estaban (conservan su `uuid`).
    suspend fun replaceForOwner(ownerType: String, ownerId: Long, minutesBefore: Collection<Int>)

    suspend fun deleteByOwner(ownerType: String, ownerId: Long)
}

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao
) : ReminderRepository {

    override suspend fun getByOwner(ownerType: String, ownerId: Long): List<ReminderEntity> =
        reminderDao.getByOwner(ownerType, ownerId)

    override suspend fun getById(id: Long): ReminderEntity? = reminderDao.getById(id)

    override suspend fun replaceForOwner(ownerType: String, ownerId: Long, minutesBefore: Collection<Int>) {
        val wanted = minutesBefore.toSet()
        val existing = reminderDao.getByOwner(ownerType, ownerId)
        val now = System.currentTimeMillis()

        existing.filter { it.minutesBefore !in wanted }.forEach { reminderDao.softDelete(it.id, now) }

        val alreadyThere = existing.map { it.minutesBefore }.toSet()
        (wanted - alreadyThere).sorted().forEach { minutes ->
            reminderDao.insert(ReminderEntity(ownerType = ownerType, ownerId = ownerId, minutesBefore = minutes))
        }
    }

    override suspend fun deleteByOwner(ownerType: String, ownerId: Long) {
        reminderDao.softDeleteByOwner(ownerType, ownerId, System.currentTimeMillis())
    }
}
