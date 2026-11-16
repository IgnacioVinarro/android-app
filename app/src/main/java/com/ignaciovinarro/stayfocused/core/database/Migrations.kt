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

// Versión 3 (PL-09): rutinas. `routineId` en `task` + tablas `routine` y `routine_item`.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE task ADD COLUMN routineId INTEGER")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `routine` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `uuid` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `startMinutes` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_routine_uuid` ON `routine` (`uuid`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `routine_item` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `uuid` TEXT NOT NULL,
                `routineId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `offsetMinutes` INTEGER NOT NULL,
                `durationMinutes` INTEGER NOT NULL,
                `iconKey` TEXT NOT NULL,
                `colorKey` TEXT NOT NULL,
                `position` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_routine_item_uuid` ON `routine_item` (`uuid`)"
        )
    }
}

// Versión 4 (PL-15): overrides por ocurrencia en `task_occurrence`, para editar "solo esta"
// ocurrencia de una tarea recurrente sin tocar las demás.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE task_occurrence ADD COLUMN overrideDurationMinutes INTEGER")
        db.execSQL("ALTER TABLE task_occurrence ADD COLUMN overrideTitle TEXT")
        db.execSQL("ALTER TABLE task_occurrence ADD COLUMN overrideNotes TEXT")
        db.execSQL("ALTER TABLE task_occurrence ADD COLUMN overrideIconKey TEXT")
        db.execSQL("ALTER TABLE task_occurrence ADD COLUMN overrideColorKey TEXT")
    }
}

// Versión 5 (PL-10, parte 1): tabla `reminder` con los recordatorios de una tarea (u otro dueño).
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminder` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `uuid` TEXT NOT NULL,
                `ownerType` TEXT NOT NULL,
                `ownerId` INTEGER NOT NULL,
                `minutesBefore` INTEGER NOT NULL,
                `kind` TEXT NOT NULL,
                `soundKey` TEXT,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_reminder_uuid` ON `reminder` (`uuid`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_reminder_ownerType_ownerId` " +
                "ON `reminder` (`ownerType`, `ownerId`)"
        )
    }
}
