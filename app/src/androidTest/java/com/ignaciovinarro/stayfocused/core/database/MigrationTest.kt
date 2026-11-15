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
}
