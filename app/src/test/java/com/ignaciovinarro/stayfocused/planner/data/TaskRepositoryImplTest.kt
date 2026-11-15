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

class TaskRepositoryImplTest {

    private val dao = mockk<TaskDao>()
    private lateinit var repository: TaskRepository

    @Before
    fun setUp() {
        repository = TaskRepositoryImpl(dao)
    }

    @Test
    fun `observeByDate delega en el DAO`() = runBlocking {
        val tareas = listOf(tarea())
        every { dao.observeByDate(19000L) } returns flowOf(tareas)

        val resultado = repository.observeByDate(19000L)

        assertEquals(tareas, resultado.first())
    }

    @Test
    fun `create delega el insert en el DAO`() = runBlocking {
        val tarea = tarea()
        coEvery { dao.insert(tarea) } returns 7L

        val id = repository.create(tarea)

        assertEquals(7L, id)
        coVerify { dao.insert(tarea) }
    }

    @Test
    fun `update actualiza updatedAt antes de delegar en el DAO`() = runBlocking {
        val tarea = tarea(updatedAt = 1L)
        val capturada = slot<TaskEntity>()
        coEvery { dao.update(capture(capturada)) } returns Unit

        repository.update(tarea)

        coVerify { dao.update(any()) }
        assert(capturada.captured.updatedAt > 1L)
    }

    @Test
    fun `delete delega el borrado logico en el DAO`() = runBlocking {
        coEvery { dao.softDelete(any(), any()) } returns Unit

        repository.delete(5L)

        coVerify { dao.softDelete(5L, any()) }
    }

    private fun tarea(updatedAt: Long = System.currentTimeMillis()) = TaskEntity(
        title = "Tarea",
        date = 19000L,
        durationMinutes = 30,
        iconKey = "task_default",
        colorKey = "blue",
        updatedAt = updatedAt
    )
}
