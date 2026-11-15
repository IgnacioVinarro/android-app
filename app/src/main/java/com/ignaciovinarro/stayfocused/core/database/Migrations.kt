package com.ignaciovinarro.stayfocused.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Versión 2 (PL-08, parte 1): recurrencia en `task` + tabla `task_occurrence`.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE task ADD COLUMN recurrenceType TEXT")
        db.execSQL("ALTER TABLE task ADD COLUMN recurrenceInterval INTEGER")
        db.execSQL("ALTER TABLE task ADD COLUMN recurrenceDays INTEGER")
        db.execSQL("ALTER TABLE task ADD COLUMN recurrenceEnd INTEGER")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_occurrence` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `uuid` TEXT NOT NULL,
                `taskId` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `skipped` INTEGER NOT NULL,
                `overrideStartMinutes` INTEGER,
                `completedAt` INTEGER,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_task_occurrence_taskId_date` " +
                "ON `task_occurrence` (`taskId`, `date`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_task_occurrence_uuid` " +
                "ON `task_occurrence` (`uuid`)"
        )
    }
}
