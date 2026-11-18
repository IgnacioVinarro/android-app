package com.ignaciovinarro.stayfocused.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

private const val TEST_DB = "migration-test"

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrarDe1a2_conservaLasTareasYAgregaLasColumnasYLaTablaNueva() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO task (id, uuid, title, date, startMinutes, durationMinutes, " +
                    "iconKey, colorKey, status, createdAt, updatedAt) VALUES " +
                    "(1, 'uuid-1', 'Tarea vieja', 19000, 540, 30, 'task_default', 'blue', 0, 1, 1)"
            )
            close()
        }

        val migrada = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        val tareas = migrada.query("SELECT * FROM task WHERE id = 1")
        assertThat(tareas.moveToFirst()).isTrue()
        assertThat(tareas.getString(tareas.getColumnIndexOrThrow("title"))).isEqualTo("Tarea vieja")
        assertThat(tareas.isNull(tareas.getColumnIndexOrThrow("recurrenceType"))).isTrue()
        tareas.close()

        val tablas = migrada.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'task_occurrence'"
        )
        assertThat(tablas.moveToFirst()).isTrue()
        tablas.close()
    }

    @Test
    fun migrarDe2a3_agregaRoutineIdYLasTablasDeRutina() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO task (id, uuid, title, date, startMinutes, durationMinutes, " +
                    "iconKey, colorKey, status, createdAt, updatedAt) VALUES " +
                    "(1, 'uuid-1', 'Tarea vieja', 19000, 540, 30, 'task_default', 'blue', 0, 1, 1)"
            )
            close()
        }

        val migrada = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        val tareas = migrada.query("SELECT * FROM task WHERE id = 1")
        assertThat(tareas.moveToFirst()).isTrue()
        assertThat(tareas.isNull(tareas.getColumnIndexOrThrow("routineId"))).isTrue()
        tareas.close()

        listOf("routine", "routine_item").forEach { tabla ->
            val tablas = migrada.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$tabla'"
            )
            assertThat(tablas.moveToFirst()).isTrue()
            tablas.close()
        }
    }

    @Test
    fun migrarDe3a4_conservaLasOcurrenciasYAgregaLasColumnasDeOverride() {
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                "INSERT INTO task (id, uuid, title, date, startMinutes, durationMinutes, " +
                    "iconKey, colorKey, status, createdAt, updatedAt) VALUES " +
                    "(1, 'uuid-1', 'Tarea recurrente', 19000, 540, 30, 'task_default', 'blue', 0, 1, 1)"
            )
            execSQL(
                "INSERT INTO task_occurrence (id, uuid, taskId, date, skipped, " +
                    "overrideStartMinutes, status, createdAt, updatedAt) VALUES " +
                    "(1, 'uuid-occ-1', 1, 19001, 0, 600, 0, 1, 1)"
            )
            close()
        }

        val migrada = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        val ocurrencias = migrada.query("SELECT * FROM task_occurrence WHERE id = 1")
        assertThat(ocurrencias.moveToFirst()).isTrue()
        assertThat(ocurrencias.getInt(ocurrencias.getColumnIndexOrThrow("overrideStartMinutes"))).isEqualTo(600)
        assertThat(ocurrencias.isNull(ocurrencias.getColumnIndexOrThrow("overrideTitle"))).isTrue()
        assertThat(ocurrencias.isNull(ocurrencias.getColumnIndexOrThrow("overrideDurationMinutes"))).isTrue()
        assertThat(ocurrencias.isNull(ocurrencias.getColumnIndexOrThrow("overrideNotes"))).isTrue()
        assertThat(ocurrencias.isNull(ocurrencias.getColumnIndexOrThrow("overrideIconKey"))).isTrue()
        assertThat(ocurrencias.isNull(ocurrencias.getColumnIndexOrThrow("overrideColorKey"))).isTrue()
        ocurrencias.close()
    }

    @Test
    fun migrarDe4a5_conservaLasTareasYCreaLaTablaDeRecordatorios() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                "INSERT INTO task (id, uuid, title, date, startMinutes, durationMinutes, " +
                    "iconKey, colorKey, status, createdAt, updatedAt) VALUES " +
                    "(1, 'uuid-1', 'Tarea vieja', 19000, 540, 30, 'task_default', 'blue', 0, 1, 1)"
            )
            close()
        }

        val migrada = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        val tareas = migrada.query("SELECT * FROM task WHERE id = 1")
        assertThat(tareas.moveToFirst()).isTrue()
        assertThat(tareas.getString(tareas.getColumnIndexOrThrow("title"))).isEqualTo("Tarea vieja")
        tareas.close()

        // La tabla nueva arranca vacía y acepta filas con los valores por defecto del dominio.
        migrada.execSQL(
            "INSERT INTO reminder (id, uuid, ownerType, ownerId, minutesBefore, kind, status, " +
                "createdAt, updatedAt) VALUES (1, 'uuid-rem-1', 'task', 1, 15, 'notice', 0, 1, 1)"
        )
        val recordatorios = migrada.query("SELECT * FROM reminder WHERE ownerType = 'task' AND ownerId = 1")
        assertThat(recordatorios.moveToFirst()).isTrue()
        assertThat(recordatorios.getInt(recordatorios.getColumnIndexOrThrow("minutesBefore"))).isEqualTo(15)
        assertThat(recordatorios.isNull(recordatorios.getColumnIndexOrThrow("soundKey"))).isTrue()
        recordatorios.close()
    }

    @Test
    fun migrarDe5a6_conservaLasTareasYCreaLaTablaDeSubtareas() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                "INSERT INTO task (id, uuid, title, date, startMinutes, durationMinutes, " +
                    "iconKey, colorKey, status, createdAt, updatedAt) VALUES " +
                    "(1, 'uuid-1', 'Tarea vieja', 19000, 540, 30, 'task_default', 'blue', 0, 1, 1)"
            )
            close()
        }

        val migrada = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        val tareas = migrada.query("SELECT * FROM task WHERE id = 1")
        assertThat(tareas.moveToFirst()).isTrue()
        assertThat(tareas.getString(tareas.getColumnIndexOrThrow("title"))).isEqualTo("Tarea vieja")
        tareas.close()

        // La tabla nueva arranca vacía y acepta filas con los valores por defecto del dominio.
        migrada.execSQL(
            "INSERT INTO subtask (id, uuid, taskId, title, done, position, status, createdAt, updatedAt) " +
                "VALUES (1, 'uuid-sub-1', 1, 'Comprar pan', 0, 0, 0, 1, 1)"
        )
        val subtareas = migrada.query("SELECT * FROM subtask WHERE taskId = 1")
        assertThat(subtareas.moveToFirst()).isTrue()
        assertThat(subtareas.getString(subtareas.getColumnIndexOrThrow("title"))).isEqualTo("Comprar pan")
        assertThat(subtareas.getInt(subtareas.getColumnIndexOrThrow("done"))).isEqualTo(0)
        subtareas.close()
    }

    @Test
    fun migrarDe6a7_conservaLasSubtareasYCreaLaTablaDeTildes() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                "INSERT INTO task (id, uuid, title, date, startMinutes, durationMinutes, " +
                    "iconKey, colorKey, status, createdAt, updatedAt) VALUES " +
                    "(1, 'uuid-1', 'Tarea vieja', 19000, 540, 30, 'task_default', 'blue', 0, 1, 1)"
            )
            execSQL(
                "INSERT INTO subtask (id, uuid, taskId, title, done, position, status, createdAt, updatedAt) " +
                    "VALUES (1, 'uuid-sub-1', 1, 'Comprar pan', 1, 0, 0, 1, 1)"
            )
            close()
        }

        val migrada = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        val subtareas = migrada.query("SELECT * FROM subtask WHERE id = 1")
        assertThat(subtareas.moveToFirst()).isTrue()
        assertThat(subtareas.getString(subtareas.getColumnIndexOrThrow("title"))).isEqualTo("Comprar pan")
        assertThat(subtareas.getInt(subtareas.getColumnIndexOrThrow("done"))).isEqualTo(1)
        subtareas.close()

        // La tabla nueva arranca vacía y acepta filas; completedAt puede ser nulo (tilde destildado).
        migrada.execSQL(
            "INSERT INTO subtask_completion (id, uuid, subtaskId, date, completedAt, status, createdAt, updatedAt) " +
                "VALUES (1, 'uuid-comp-1', 1, 19000, NULL, 0, 1, 1)"
        )
        val tildes = migrada.query("SELECT * FROM subtask_completion WHERE subtaskId = 1")
        assertThat(tildes.moveToFirst()).isTrue()
        assertThat(tildes.isNull(tildes.getColumnIndexOrThrow("completedAt"))).isTrue()
        tildes.close()
    }

    @Test
    fun migrarDe7a8_conservaLasTareasYCreaLasTablasDeFinanzas() {
        helper.createDatabase(TEST_DB, 7).apply {
            execSQL(
                "INSERT INTO task (id, uuid, title, date, startMinutes, durationMinutes, " +
                    "iconKey, colorKey, status, createdAt, updatedAt) VALUES " +
                    "(1, 'uuid-1', 'Tarea vieja', 19000, 540, 30, 'task_default', 'blue', 0, 1, 1)"
            )
            close()
        }

        val migrada = helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)

        val tareas = migrada.query("SELECT * FROM task WHERE id = 1")
        assertThat(tareas.moveToFirst()).isTrue()
        assertThat(tareas.getString(tareas.getColumnIndexOrThrow("title"))).isEqualTo("Tarea vieja")
        tareas.close()

        listOf("account", "category", "finance_transaction", "budget", "tag", "transaction_tag").forEach { tabla ->
            val tablas = migrada.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$tabla'"
            )
            assertThat(tablas.moveToFirst()).isTrue()
            tablas.close()
        }

        // Las tablas nuevas aceptan filas; categoryId y transferAccountId pueden ser nulos.
        migrada.execSQL(
            "INSERT INTO finance_transaction (id, uuid, type, amount, date, accountId, categoryId, " +
                "transferAccountId, note, status, createdAt, updatedAt) " +
                "VALUES (1, 'uuid-tx-1', 'TRANSFER', 1000, 19000, 1, NULL, 2, NULL, 0, 1, 1)"
        )
        val transacciones = migrada.query("SELECT * FROM finance_transaction WHERE id = 1")
        assertThat(transacciones.moveToFirst()).isTrue()
        assertThat(transacciones.isNull(transacciones.getColumnIndexOrThrow("categoryId"))).isTrue()
        assertThat(transacciones.getLong(transacciones.getColumnIndexOrThrow("amount"))).isEqualTo(1000L)
        transacciones.close()
    }
}
