package com.ignaciovinarro.stayfocused.planner.data

import com.ignaciovinarro.stayfocused.planner.domain.SubtaskDraft
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SubtaskRepositoryImplTest {

    private val dao = mockk<SubtaskDao>(relaxed = true)
    private lateinit var repository: SubtaskRepository

    @Before
    fun setUp() {
        repository = SubtaskRepositoryImpl(dao)
    }

    private fun existente(id: Long, title: String, position: Int, done: Boolean = false) =
        SubtaskEntity(id = id, taskId = 5L, title = title, done = done, position = position)

    private fun borrador(key: Long, title: String, id: Long? = null) = SubtaskDraft(key = key, title = title, id = id)

    @Test
    fun `getByTask delega en el DAO`() = runBlocking {
        val subtareas = listOf(existente(1L, "A", 0))
        coEvery { dao.getByTask(5L) } returns subtareas

        assertEquals(subtareas, repository.getByTask(5L))
    }

    @Test
    fun `replaceForTask inserta las nuevas con su posicion`() = runBlocking {
        coEvery { dao.getByTask(5L) } returns emptyList()
        val insertadas = mutableListOf<SubtaskEntity>()
        coEvery { dao.insert(capture(insertadas)) } returns 1L

        repository.replaceForTask(5L, listOf(borrador(1, "Primera"), borrador(2, "Segunda")))

        assertEquals(listOf("Primera", "Segunda"), insertadas.map { it.title })
        assertEquals(listOf(0, 1), insertadas.map { it.position })
        assertEquals(listOf(5L, 5L), insertadas.map { it.taskId })
        assertEquals(listOf(false, false), insertadas.map { it.done })
    }

    @Test
    fun `replaceForTask actualiza titulo y posicion de las existentes sin insertarlas de nuevo`() = runBlocking {
        coEvery { dao.getByTask(5L) } returns listOf(existente(1L, "A", 0, done = true), existente(2L, "B", 1))

        // Se invierte el orden y se cambia un titulo.
        repository.replaceForTask(5L, listOf(borrador(1, "B2", id = 2L), borrador(2, "A", id = 1L)))

        coVerify { dao.updateTitleAndPosition(2L, "B2", 0, any()) }
        coVerify { dao.updateTitleAndPosition(1L, "A", 1, any()) }
        coVerify(exactly = 0) { dao.insert(any()) }
        coVerify(exactly = 0) { dao.softDelete(any(), any()) }
    }

    @Test
    fun `replaceForTask borra logicamente las que sobran`() = runBlocking {
        coEvery { dao.getByTask(5L) } returns listOf(existente(1L, "A", 0), existente(2L, "B", 1))

        repository.replaceForTask(5L, listOf(borrador(1, "B", id = 2L)))

        coVerify { dao.softDelete(1L, any()) }
        coVerify(exactly = 0) { dao.softDelete(2L, any()) }
        coVerify { dao.updateTitleAndPosition(2L, "B", 0, any()) }
    }

    @Test
    fun `replaceForTask con lista vacia borra todas`() = runBlocking {
        coEvery { dao.getByTask(5L) } returns listOf(existente(1L, "A", 0), existente(2L, "B", 1))

        repository.replaceForTask(5L, emptyList())

        coVerify { dao.softDelete(1L, any()) }
        coVerify { dao.softDelete(2L, any()) }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `replaceForTask trata como nueva a una subtarea cuyo id no es de esta tarea`() = runBlocking {
        // Caso de la division "esta y siguientes": los ids de la lista son de la tarea original.
        coEvery { dao.getByTask(99L) } returns emptyList()
        val insertada = slot<SubtaskEntity>()
        coEvery { dao.insert(capture(insertada)) } returns 1L

        repository.replaceForTask(99L, listOf(borrador(1, "Heredada", id = 2L)))

        assertEquals("Heredada", insertada.captured.title)
        assertEquals(99L, insertada.captured.taskId)
        coVerify(exactly = 0) { dao.updateTitleAndPosition(any(), any(), any(), any()) }
    }

    @Test
    fun `deleteByTask hace el borrado logico de todas las subtareas de la tarea`() = runBlocking {
        repository.deleteByTask(5L)

        coVerify { dao.softDeleteByTask(5L, any()) }
    }
}
