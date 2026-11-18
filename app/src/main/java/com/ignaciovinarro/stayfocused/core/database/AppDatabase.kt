package com.ignaciovinarro.stayfocused.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ignaciovinarro.stayfocused.core.backup.BackupDao
import com.ignaciovinarro.stayfocused.finance.data.AccountDao
import com.ignaciovinarro.stayfocused.finance.data.AccountEntity
import com.ignaciovinarro.stayfocused.finance.data.BudgetDao
import com.ignaciovinarro.stayfocused.finance.data.BudgetEntity
import com.ignaciovinarro.stayfocused.finance.data.CategoryDao
import com.ignaciovinarro.stayfocused.finance.data.CategoryEntity
import com.ignaciovinarro.stayfocused.finance.data.TagDao
import com.ignaciovinarro.stayfocused.finance.data.TagEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionDao
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionTagDao
import com.ignaciovinarro.stayfocused.finance.data.TransactionTagEntity
import com.ignaciovinarro.stayfocused.gym.data.ExerciseDao
import com.ignaciovinarro.stayfocused.gym.data.ExerciseEntity
import com.ignaciovinarro.stayfocused.gym.data.PersonalRecordEntity
import com.ignaciovinarro.stayfocused.gym.data.RoutineExerciseEntity
import com.ignaciovinarro.stayfocused.gym.data.SessionDao
import com.ignaciovinarro.stayfocused.gym.data.SessionExerciseNoteEntity
import com.ignaciovinarro.stayfocused.gym.data.SessionSetEntity
import com.ignaciovinarro.stayfocused.gym.data.WorkoutRoutineDao
import com.ignaciovinarro.stayfocused.gym.data.WorkoutRoutineEntity
import com.ignaciovinarro.stayfocused.gym.data.WorkoutSessionEntity
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
        SubtaskCompletionEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        TagEntity::class,
        TransactionTagEntity::class,
        ExerciseEntity::class,
        WorkoutRoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseNoteEntity::class,
        SessionSetEntity::class,
        PersonalRecordEntity::class
    ],
    version = 9,
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
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun tagDao(): TagDao
    abstract fun transactionTagDao(): TransactionTagDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutRoutineDao(): WorkoutRoutineDao
    abstract fun sessionDao(): SessionDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
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

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideTransactionTagDao(database: AppDatabase): TransactionTagDao = database.transactionTagDao()

    @Provides
    fun provideExerciseDao(database: AppDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideWorkoutRoutineDao(database: AppDatabase): WorkoutRoutineDao = database.workoutRoutineDao()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()
}
