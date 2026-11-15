package com.ignaciovinarro.stayfocused.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ignaciovinarro.stayfocused.planner.data.TaskDao
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: TaskDao

    @Before
    fun crearBaseEnMemoria() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.taskDao()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    private fun tarea(
        title: String = "Tarea",
        date: Long = 19000L,
        startMinutes: Int? = 9 * 60
    ) = TaskEntity(
        title = title,
        date = date,
        startMinutes = startMinutes,
        durationMinutes = 30,
        iconKey = "task_default",
        colorKey = "blue"
    )

    @Test
    fun insertarYObtenerPorId_devuelveLaMismaTarea() = runBlocking {
        val id = dao.insert(tarea(title = "Comprar pan"))

        val obtenida = dao.getById(id)

        assertThat(obtenida).isNotNull()
        assertThat(obtenida?.title).isEqualTo("Comprar pan")
    }

    @Test
    fun observeByDate_soloDevuelveTareasDeEseDiaYNoBorradas() = runBlocking {
        dao.insert(tarea(title = "Hoy 1", date = 19000L))
        dao.insert(tarea(title = "Hoy 2", date = 19000L, startMinutes = 10 * 60))
        dao.insert(tarea(title = "Otro día", date = 19001L))
        val borradaId = dao.insert(tarea(title = "Borrada", date = 19000L, startMinutes = 8 * 60))
        dao.softDelete(borradaId, System.currentTimeMillis())

        val tareasDelDia = dao.observeByDate(19000L).first()

        assertThat(tareasDelDia.map { it.title }).containsExactly("Hoy 1", "Hoy 2")
    }

    @Test
    fun observeByDate_ordenaPrimeroConHoraYLuegoLaBandeja() = runBlocking {
        dao.insert(tarea(title = "Sin hora", date = 19000L, startMinutes = null))
        dao.insert(tarea(title = "A las 9", date = 19000L, startMinutes = 9 * 60))

        val tareasDelDia = dao.observeByDate(19000L).first()

        assertThat(tareasDelDia.map { it.title }).containsExactly("A las 9", "Sin hora").inOrder()
    }

    @Test
    fun softDelete_marcaStatusBorradoYNoApareceEnConsultas() = runBlocking {
        val id = dao.insert(tarea(title = "A borrar"))

        dao.softDelete(id, System.currentTimeMillis())

        assertThat(dao.getById(id)).isNull()
    }

    @Test
    fun uuidEsUnico_insertarUuidDuplicadoFalla() = runBlocking {
        val tareaOriginal = tarea(title = "Original")
        dao.insert(tareaOriginal)

        var fallo = false
        try {
            dao.insert(tareaOriginal.copy(title = "Duplicada"))
        } catch (e: Exception) {
            fallo = true
        }

        assertThat(fallo).isTrue()
    }
}
