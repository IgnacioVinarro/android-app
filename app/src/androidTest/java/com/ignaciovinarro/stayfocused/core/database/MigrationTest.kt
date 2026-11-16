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
}
