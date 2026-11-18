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

// Versión 6 (PL-04 / PL-05, parte 1): tabla `subtask` con las subtareas de una tarea.
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `subtask` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `uuid` TEXT NOT NULL,
                `taskId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `done` INTEGER NOT NULL,
                `position` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_subtask_uuid` ON `subtask` (`uuid`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_subtask_taskId` ON `subtask` (`taskId`)"
        )
    }
}

// Versión 7 (PL-05, parte 2): tabla `subtask_completion` con el tilde de cada subtarea por
// ocurrencia (tareas recurrentes).
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `subtask_completion` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `uuid` TEXT NOT NULL,
                `subtaskId` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `completedAt` INTEGER,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_subtask_completion_subtaskId_date` " +
                "ON `subtask_completion` (`subtaskId`, `date`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_subtask_completion_uuid` ON `subtask_completion` (`uuid`)"
        )
    }
}

// Versión 8 (Fase 2, Finanzas — ticket 0028): cuentas, categorías, transacciones, presupuestos,
// etiquetas y la unión transacción↔etiqueta.
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `account` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `initialBalance` INTEGER NOT NULL,
                `iconKey` TEXT NOT NULL,
                `colorKey` TEXT NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_account_uuid` ON `account` (`uuid`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `category` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `iconKey` TEXT NOT NULL,
                `colorKey` TEXT NOT NULL,
                `isDefault` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_category_uuid` ON `category` (`uuid`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `finance_transaction` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `accountId` INTEGER NOT NULL,
                `categoryId` INTEGER,
                `transferAccountId` INTEGER,
                `note` TEXT,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_finance_transaction_uuid` ON `finance_transaction` (`uuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transaction_accountId` ON `finance_transaction` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transaction_categoryId` ON `finance_transaction` (`categoryId`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_finance_transaction_transferAccountId` " +
                "ON `finance_transaction` (`transferAccountId`)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finance_transaction_date` ON `finance_transaction` (`date`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budget` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `categoryId` INTEGER,
                `effectiveFrom` INTEGER NOT NULL,
                `amount` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_uuid` ON `budget` (`uuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budget_categoryId` ON `budget` (`categoryId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tag` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tag_uuid` ON `tag` (`uuid`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transaction_tag` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `transactionId` INTEGER NOT NULL,
                `tagId` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transaction_tag_uuid` ON `transaction_tag` (`uuid`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_transaction_tag_transactionId_tagId` " +
                "ON `transaction_tag` (`transactionId`, `tagId`)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_tag_tagId` ON `transaction_tag` (`tagId`)")
    }
}

// Versión 9: tablas de Gimnasio (ticket 0037). Solo crea tablas nuevas; no toca las existentes.
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `muscleGroup` TEXT NOT NULL,
                `equipment` TEXT NOT NULL,
                `trackingType` TEXT NOT NULL,
                `isCustom` INTEGER NOT NULL,
                `notes` TEXT,
                `link` TEXT,
                `restSeconds` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_exercise_uuid` ON `exercise` (`uuid`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workout_routine` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `scheduleDays` INTEGER NOT NULL,
                `startMinutes` INTEGER,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_routine_uuid` ON `workout_routine` (`uuid`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `routine_exercise` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `routineId` INTEGER NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `position` INTEGER NOT NULL,
                `targetSets` INTEGER NOT NULL,
                `targetReps` INTEGER NOT NULL,
                `targetWeight` INTEGER NOT NULL,
                `supersetGroup` INTEGER,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_routine_exercise_uuid` ON `routine_exercise` (`uuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_exercise_routineId` ON `routine_exercise` (`routineId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_exercise_exerciseId` ON `routine_exercise` (`exerciseId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workout_session` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `routineId` INTEGER,
                `date` INTEGER NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `endedAt` INTEGER,
                `state` TEXT NOT NULL,
                `notes` TEXT,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_session_uuid` ON `workout_session` (`uuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_session_routineId` ON `workout_session` (`routineId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_session_date` ON `workout_session` (`date`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `session_exercise_note` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_session_exercise_note_uuid` ON `session_exercise_note` (`uuid`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_session_exercise_note_sessionId_exerciseId` ON `session_exercise_note` (`sessionId`, `exerciseId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_exercise_note_exerciseId` ON `session_exercise_note` (`exerciseId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `session_set` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `sessionId` INTEGER NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `position` INTEGER NOT NULL,
                `weight` INTEGER NOT NULL,
                `reps` INTEGER NOT NULL,
                `durationSeconds` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `completed` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_session_set_uuid` ON `session_set` (`uuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_set_sessionId` ON `session_set` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_set_exerciseId` ON `session_set` (`exerciseId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `personal_record` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `uuid` TEXT NOT NULL,
                `exerciseId` INTEGER NOT NULL,
                `kind` TEXT NOT NULL,
                `value` INTEGER NOT NULL,
                `weight` INTEGER NOT NULL,
                `sessionSetId` INTEGER NOT NULL,
                `achievedAt` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_personal_record_uuid` ON `personal_record` (`uuid`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_record_exerciseId_kind` ON `personal_record` (`exerciseId`, `kind`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_record_sessionSetId` ON `personal_record` (`sessionSetId`)")
    }
}
