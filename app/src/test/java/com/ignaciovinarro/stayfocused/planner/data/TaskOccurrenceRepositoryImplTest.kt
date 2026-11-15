package com.ignaciovinarro.stayfocused.planner.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TaskOccurrenceRepositoryImplTest {

    private val dao = mockk<TaskOccurrenceDao>()
    private lateinit var repository: TaskOccurrenceRepository

    @Before
    fun setUp() {
        repository = TaskOccurrenceRepositoryImpl(dao)
    }

    @Test
    fun `observeByDateRange delega en el DAO`() = runBlocking {
        val ocurrencias = listOf(ocurrencia())
        every { dao.observeByDateRange(19000L, 19010L) } returns flowOf(ocurrencias)

        val resultado = repository.observeByDateRange(19000L, 19010L)

        assertEquals(ocurrencias, resultado.first())
    }

    @Test
    fun `getByTaskAndDate delega en el DAO`() = runBlocking {
        val ocurrencia = ocurrencia()
        coEvery { dao.getByTaskAndDate(1L, 19000L) } returns ocurrencia

        val resultado = repository.getByTaskAndDate(1L, 19000L)

        assertEquals(ocurrencia, resultado)
    }

    @Test
    fun `create delega el insert en el DAO`() = runBlocking {
        val ocurrencia = ocurrencia()
        coEvery { dao.insert(ocurrencia) } returns 9L

        val id = repository.create(ocurrencia)

        assertEquals(9L, id)
        coVerify { dao.insert(ocurrencia) }
    }

    @Test
    fun `update actualiza updatedAt antes de delegar en el DAO`() = runBlocking {
        val ocurrencia = ocurrencia(updatedAt = 1L)
        val capturada = slot<TaskOccurrenceEntity>()
        coEvery { dao.update(capture(capturada)) } returns Unit

        repository.update(ocurrencia)

        coVerify { dao.update(any()) }
        assert(capturada.captured.updatedAt > 1L)
    }

    @Test
    fun `delete delega el borrado logico en el DAO`() = runBlocking {
        coEvery { dao.softDelete(any(), any()) } returns Unit

        repository.delete(5L)

        coVerify { dao.softDelete(5L, any()) }
    }

    private fun ocurrencia(updatedAt: Long = System.currentTimeMillis()) = TaskOccurrenceEntity(
        taskId = 1L,
        date = 19000L,
        updatedAt = updatedAt
    )
}
