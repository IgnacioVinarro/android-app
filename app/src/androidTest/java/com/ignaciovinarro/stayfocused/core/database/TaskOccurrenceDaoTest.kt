package com.ignaciovinarro.stayfocused.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceDao
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskOccurrenceDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: TaskOccurrenceDao

    @Before
    fun crearBaseEnMemoria() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.taskOccurrenceDao()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    private fun ocurrencia(
        taskId: Long = 1L,
        date: Long = 19000L,
        completedAt: Long? = null
    ) = TaskOccurrenceEntity(taskId = taskId, date = date, completedAt = completedAt)

    @Test
    fun insertarYObtenerPorTareaYFecha_devuelveLaMismaOcurrencia() {
        runBlocking {
            dao.insert(ocurrencia(taskId = 1L, date = 19000L, completedAt = 123L))

            val obtenida = dao.getByTaskAndDate(1L, 19000L)

            assertThat(obtenida).isNotNull()
            assertThat(obtenida?.completedAt).isEqualTo(123L)
        }
    }

    @Test
    fun unicaPorTareaYFecha_insertarDuplicadaFalla() {
        runBlocking {
            dao.insert(ocurrencia(taskId = 1L, date = 19000L))

            var fallo = false
            try {
                dao.insert(ocurrencia(taskId = 1L, date = 19000L))
            } catch (e: Exception) {
                fallo = true
            }

            assertThat(fallo).isTrue()
        }
    }

    @Test
    fun observeByDateRange_soloDevuelveLasDelRangoYNoBorradas() {
        runBlocking {
            dao.insert(ocurrencia(taskId = 1L, date = 19000L))
            dao.insert(ocurrencia(taskId = 2L, date = 19005L))
            dao.insert(ocurrencia(taskId = 3L, date = 19020L))
            val borradaId = dao.insert(ocurrencia(taskId = 4L, date = 19002L))
            dao.softDelete(borradaId, System.currentTimeMillis())

            val delRango = dao.observeByDateRange(19000L, 19010L).first()

            assertThat(delRango.map { it.taskId }).containsExactly(1L, 2L)
        }
    }

    @Test
    fun softDelete_marcaStatusBorradoYNoApareceEnConsultas() {
        runBlocking {
            val id = dao.insert(ocurrencia())

            dao.softDelete(id, System.currentTimeMillis())

            assertThat(dao.getByTaskAndDate(1L, 19000L)).isNull()
        }
    }
}
