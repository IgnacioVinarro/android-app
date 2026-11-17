package com.ignaciovinarro.stayfocused.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ignaciovinarro.stayfocused.planner.data.SubtaskDao
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubtaskDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SubtaskDao

    @Before
    fun crearBaseEnMemoria() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.subtaskDao()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    private fun subtarea(taskId: Long = 1L, title: String = "Sub", position: Int = 0) =
        SubtaskEntity(taskId = taskId, title = title, position = position)

    @Test
    fun insertarYListarPorTarea_devuelveLasSubtareasOrdenadasPorPosicion() {
        runBlocking {
            dao.insert(subtarea(title = "Tercera", position = 2))
            dao.insert(subtarea(title = "Primera", position = 0))
            dao.insert(subtarea(title = "Segunda", position = 1))

            val obtenidas = dao.getByTask(1L)

            assertThat(obtenidas.map { it.title }).containsExactly("Primera", "Segunda", "Tercera").inOrder()
        }
    }

    @Test
    fun listarPorTarea_noTraeLasDeOtraTarea() {
        runBlocking {
            dao.insert(subtarea(taskId = 1L, title = "De la 1"))
            dao.insert(subtarea(taskId = 2L, title = "De la 2"))

            assertThat(dao.getByTask(1L).map { it.title }).containsExactly("De la 1")
        }
    }

    @Test
    fun actualizarTituloYPosicion_conservaElDoneYElUuid() {
        runBlocking {
            val id = dao.insert(subtarea(title = "Vieja", position = 0).copy(done = true))
            val uuidOriginal = dao.getByTask(1L).single().uuid

            dao.updateTitleAndPosition(id, "Nueva", 3, updatedAt = 99L)

            val actualizada = dao.getByTask(1L).single()
            assertThat(actualizada.title).isEqualTo("Nueva")
            assertThat(actualizada.position).isEqualTo(3)
            assertThat(actualizada.done).isTrue()
            assertThat(actualizada.uuid).isEqualTo(uuidOriginal)
            assertThat(actualizada.updatedAt).isEqualTo(99L)
        }
    }

    @Test
    fun borradoLogicoPorId_laSacaDelListadoPeroNoBorraLaFila() {
        runBlocking {
            val id = dao.insert(subtarea(title = "A borrar"))
            dao.insert(subtarea(title = "Queda", position = 1))

            dao.softDelete(id, updatedAt = 5L)

            assertThat(dao.getByTask(1L).map { it.title }).containsExactly("Queda")
            val cursor = database.query("SELECT status FROM subtask WHERE id = $id", null)
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(-1)
            cursor.close()
        }
    }

    @Test
    fun borradoLogicoPorTarea_soloAfectaALaTareaIndicada() {
        runBlocking {
            dao.insert(subtarea(taskId = 1L))
            dao.insert(subtarea(taskId = 2L, title = "De la 2"))

            dao.softDeleteByTask(1L, updatedAt = 5L)

            assertThat(dao.getByTask(1L)).isEmpty()
            assertThat(dao.getByTask(2L).map { it.title }).containsExactly("De la 2")
        }
    }

    @Test
    fun uuidDuplicado_noSePermite() {
        runBlocking {
            val primera = subtarea()
            dao.insert(primera)
            val resultado = runCatching { dao.insert(subtarea().copy(uuid = primera.uuid)) }

            assertThat(resultado.isFailure).isTrue()
        }
    }
}
