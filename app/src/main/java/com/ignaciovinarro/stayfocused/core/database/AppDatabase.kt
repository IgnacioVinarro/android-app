package com.ignaciovinarro.stayfocused.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ignaciovinarro.stayfocused.core.backup.BackupDao
import com.ignaciovinarro.stayfocused.planner.data.ReminderDao
import com.ignaciovinarro.stayfocused.planner.data.ReminderEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionDao
import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskDao
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineDao
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemDao
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskDao
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceDao
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "stayfocused.db"

@Database(
    entities = [
        TaskEntity::class,
        TaskOccurrenceEntity::class,
        RoutineEntity::class,
        RoutineItemEntity::class,
        ReminderEntity::class,
        SubtaskEntity::class,
        SubtaskCompletionEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskOccurrenceDao(): TaskOccurrenceDao
    abstract fun routineDao(): RoutineDao
    abstract fun routineItemDao(): RoutineItemDao
    abstract fun reminderDao(): ReminderDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun subtaskCompletionDao(): SubtaskCompletionDao
    abstract fun backupDao(): BackupDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTaskOccurrenceDao(database: AppDatabase): TaskOccurrenceDao = database.taskOccurrenceDao()

    @Provides
    fun provideRoutineDao(database: AppDatabase): RoutineDao = database.routineDao()

    @Provides
    fun provideRoutineItemDao(database: AppDatabase): RoutineItemDao = database.routineItemDao()

    @Provides
    fun provideReminderDao(database: AppDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideSubtaskDao(database: AppDatabase): SubtaskDao = database.subtaskDao()

    @Provides
    fun provideSubtaskCompletionDao(database: AppDatabase): SubtaskCompletionDao = database.subtaskCompletionDao()

    @Provides
    fun provideBackupDao(database: AppDatabase): BackupDao = database.backupDao()
}
