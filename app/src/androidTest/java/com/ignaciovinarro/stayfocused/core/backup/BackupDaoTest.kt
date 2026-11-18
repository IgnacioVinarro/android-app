package com.ignaciovinarro.stayfocused.core.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ignaciovinarro.stayfocused.core.database.AppDatabase
import com.ignaciovinarro.stayfocused.planner.data.ReminderEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Room en memoria. Comprueba lo que las pruebas unitarias no pueden: que las consultas y la transacción
// de BackupDao funcionan de verdad contra SQLite (ticket 0026).
@RunWith(AndroidJUnit4::class)
class BackupDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: BackupDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        dao = database.backupDao()
    }

    @After
    fun tearDown() = database.close()

    private fun sample() = BackupData(
        tasks = listOf(
            TaskEntity(id = 1, uuid = "t1", title = "Activa", date = 20_000, durationMinutes = 30, iconKey = "i", colorKey = "c"),
            TaskEntity(id = 2, uuid = "t2", title = "Borrada", date = 20_000, durationMinutes = 30, iconKey = "i", colorKey = "c", status = -1)
        ),
        occurrences = listOf(TaskOccurrenceEntity(id = 1, uuid = "o1", taskId = 1, date = 20_007, skipped = true)),
        routines = listOf(RoutineEntity(id = 1, uuid = "r1", name = "Mañana", startMinutes = 420)),
        routineItems = listOf(RoutineItemEntity(id = 1, uuid = "ri1", routineId = 1, title = "Ducha", offsetMinutes = 0, durationMinutes = 15, iconKey = "i", colorKey = "c", position = 0)),
        reminders = listOf(ReminderEntity(id = 1, uuid = "rem1", ownerId = 1, minutesBefore = 10)),
        subtasks = listOf(SubtaskEntity(id = 1, uuid = "s1", taskId = 1, title = "Calentar", position = 0)),
        subtaskCompletions = listOf(SubtaskCompletionEntity(id = 1, uuid = "sc1", subtaskId = 1, date = 20_007))
    )

    @Test
    fun readAll_en_una_base_vacia_devuelve_todo_vacio() = runBlocking {
        assertThat(dao.readAll()).isEqualTo(BackupData())
    }

    @Test
    fun replaceAll_y_readAll_son_ida_y_vuelta_e_incluyen_las_filas_borradas() = runBlocking {
        dao.replaceAll(sample())

        assertThat(dao.readAll()).isEqualTo(sample())
    }

    @Test
    fun replaceAll_reemplaza_lo_que_habia_y_no_deja_restos() = runBlocking {
        dao.replaceAll(sample())
        val otro = BackupData(
            tasks = listOf(TaskEntity(id = 10, uuid = "t10", title = "Otra", date = 20_100, durationMinutes = 15, iconKey = "i", colorKey = "c"))
        )

        dao.replaceAll(otro)

        assertThat(dao.readAll()).isEqualTo(otro)
    }

    @Test
    fun replaceAll_conserva_los_id_para_que_las_referencias_sigan_validas() = runBlocking {
        dao.replaceAll(sample())

        val data = dao.readAll()
        assertThat(data.occurrences.single().taskId).isEqualTo(data.tasks.first().id)
        assertThat(data.routineItems.single().routineId).isEqualTo(data.routines.single().id)
    }

    @Test
    fun replaceAll_con_filas_repetidas_no_falla_y_queda_una() = runBlocking {
        val tarea = sample().tasks.first()

        dao.replaceAll(BackupData(tasks = listOf(tarea, tarea)))

        assertThat(dao.readAll().tasks).containsExactly(tarea)
    }
}
