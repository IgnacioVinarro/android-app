package com.ignaciovinarro.stayfocused.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionDao
import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskDao
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubtaskCompletionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SubtaskCompletionDao
    private lateinit var subtaskDao: SubtaskDao

    @Before
    fun crearBaseEnMemoria() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.subtaskCompletionDao()
        subtaskDao = database.subtaskDao()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    private fun tilde(subtaskId: Long = 1L, date: Long = 19000L, completedAt: Long? = 10L) =
        SubtaskCompletionEntity(subtaskId = subtaskId, date = date, completedAt = completedAt)

    @Test
    fun insertarYBuscarPorSubtareaYDia_devuelveElTilde() {
        runBlocking {
            dao.insert(tilde(subtaskId = 1L, date = 19000L))

            val obtenido = dao.getBySubtaskAndDate(1L, 19000L)

            assertThat(obtenido).isNotNull()
            assertThat(obtenido!!.completedAt).isEqualTo(10L)
        }
    }

    @Test
    fun buscarPorOtroDiaOOtraSubtarea_noDevuelveNada() {
        runBlocking {
            dao.insert(tilde(subtaskId = 1L, date = 19000L))

            assertThat(dao.getBySubtaskAndDate(1L, 19001L)).isNull()
            assertThat(dao.getBySubtaskAndDate(2L, 19000L)).isNull()
        }
    }

    @Test
    fun observarPorDia_traeSoloLosTildesDeEseDia() {
        runBlocking {
            dao.insert(tilde(subtaskId = 1L, date = 19000L))
            dao.insert(tilde(subtaskId = 2L, date = 19000L))
            dao.insert(tilde(subtaskId = 1L, date = 19001L))

            val delDia = dao.observeByDate(19000L).first()

            assertThat(delDia.map { it.subtaskId }).containsExactly(1L, 2L)
        }
    }

    @Test
    fun destildar_actualizaCompletedAtSinBorrarLaFila() {
        runBlocking {
            dao.insert(tilde(subtaskId = 1L, date = 19000L))
            val original = dao.getBySubtaskAndDate(1L, 19000L)!!

            dao.update(original.copy(completedAt = null))

            val actualizado = dao.getBySubtaskAndDate(1L, 19000L)
            assertThat(actualizado).isNotNull()
            assertThat(actualizado!!.completedAt).isNull()
            assertThat(actualizado.id).isEqualTo(original.id)
        }
    }

    @Test
    fun subtareaYDiaDuplicados_noSePermiten() {
        runBlocking {
            dao.insert(tilde(subtaskId = 1L, date = 19000L))

            val resultado = runCatching { dao.insert(tilde(subtaskId = 1L, date = 19000L)) }

            assertThat(resultado.isFailure).isTrue()
        }
    }

    @Test
    fun observarTodasLasSubtareas_noTraeLasBorradasLogicamente() {
        runBlocking {
            val borrada = subtaskDao.insert(SubtaskEntity(taskId = 1L, title = "Borrada", position = 0))
            subtaskDao.insert(SubtaskEntity(taskId = 1L, title = "Activa", position = 1))
            subtaskDao.softDelete(borrada, updatedAt = 5L)

            val todas = subtaskDao.observeAll().first()

            assertThat(todas.map { it.title }).containsExactly("Activa")
        }
    }

    @Test
    fun actualizarDone_cambiaSoloElTildeDeLaSubtarea() {
        runBlocking {
            val id = subtaskDao.insert(SubtaskEntity(taskId = 1L, title = "Pan", position = 0))

            subtaskDao.updateDone(id, true, updatedAt = 7L)

            val subtarea = subtaskDao.getByTask(1L).single()
            assertThat(subtarea.done).isTrue()
            assertThat(subtarea.title).isEqualTo("Pan")
            assertThat(subtarea.updatedAt).isEqualTo(7L)
        }
    }
}
