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

class RoutineItemRepositoryImplTest {

    private val dao = mockk<RoutineItemDao>()
    private lateinit var repository: RoutineItemRepository

    @Before
    fun setUp() {
        repository = RoutineItemRepositoryImpl(dao)
    }

    @Test
    fun `observeByRoutine delega en el DAO`() = runBlocking {
        val items = listOf(item())
        every { dao.observeByRoutine(1L) } returns flowOf(items)

        val resultado = repository.observeByRoutine(1L)

        assertEquals(items, resultado.first())
    }

    @Test
    fun `getByRoutine delega en el DAO`() = runBlocking {
        val items = listOf(item())
        coEvery { dao.getByRoutine(1L) } returns items

        assertEquals(items, repository.getByRoutine(1L))
    }

    @Test
    fun `create delega el insert en el DAO`() = runBlocking {
        val i = item()
        coEvery { dao.insert(i) } returns 9L

        val id = repository.create(i)

        assertEquals(9L, id)
        coVerify { dao.insert(i) }
    }

    @Test
    fun `update actualiza updatedAt antes de delegar en el DAO`() = runBlocking {
        val i = item(updatedAt = 1L)
        val capturada = slot<RoutineItemEntity>()
        coEvery { dao.update(capture(capturada)) } returns Unit

        repository.update(i)

        coVerify { dao.update(any()) }
        assert(capturada.captured.updatedAt > 1L)
    }

    @Test
    fun `delete delega el borrado logico en el DAO`() = runBlocking {
        coEvery { dao.softDelete(any(), any()) } returns Unit

        repository.delete(5L)

        coVerify { dao.softDelete(5L, any()) }
    }

    private fun item(updatedAt: Long = System.currentTimeMillis()) = RoutineItemEntity(
        routineId = 1L,
        title = "Ítem",
        offsetMinutes = 0,
        durationMinutes = 30,
        iconKey = "task_default",
        colorKey = "blue",
        position = 0,
        updatedAt = updatedAt
    )
}
