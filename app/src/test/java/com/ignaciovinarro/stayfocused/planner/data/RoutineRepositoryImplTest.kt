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

class RoutineRepositoryImplTest {

    private val dao = mockk<RoutineDao>()
    private lateinit var repository: RoutineRepository

    @Before
    fun setUp() {
        repository = RoutineRepositoryImpl(dao)
    }

    @Test
    fun `observeAll delega en el DAO`() = runBlocking {
        val rutinas = listOf(rutina())
        every { dao.observeAll() } returns flowOf(rutinas)

        val resultado = repository.observeAll()

        assertEquals(rutinas, resultado.first())
    }

    @Test
    fun `getById delega en el DAO`() = runBlocking {
        val r = rutina()
        coEvery { dao.getById(1L) } returns r

        assertEquals(r, repository.getById(1L))
    }

    @Test
    fun `create delega el insert en el DAO`() = runBlocking {
        val r = rutina()
        coEvery { dao.insert(r) } returns 9L

        val id = repository.create(r)

        assertEquals(9L, id)
        coVerify { dao.insert(r) }
    }

    @Test
    fun `update actualiza updatedAt antes de delegar en el DAO`() = runBlocking {
        val r = rutina(updatedAt = 1L)
        val capturada = slot<RoutineEntity>()
        coEvery { dao.update(capture(capturada)) } returns Unit

        repository.update(r)

        coVerify { dao.update(any()) }
        assert(capturada.captured.updatedAt > 1L)
    }

    @Test
    fun `delete delega el borrado logico en el DAO`() = runBlocking {
        coEvery { dao.softDelete(any(), any()) } returns Unit

        repository.delete(5L)

        coVerify { dao.softDelete(5L, any()) }
    }

    private fun rutina(updatedAt: Long = System.currentTimeMillis()) = RoutineEntity(
        name = "Rutina de mañana",
        startMinutes = 420,
        updatedAt = updatedAt
    )
}
