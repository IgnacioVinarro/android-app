package com.ignaciovinarro.stayfocused.finance.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CategoryRepositoryImplTest {

    private val dao = mockk<CategoryDao>(relaxed = true)
    private lateinit var repository: CategoryRepository

    @Before
    fun setUp() {
        repository = CategoryRepositoryImpl(dao)
    }

    private fun categoria(id: Long, type: String = "EXPENSE") =
        CategoryEntity(id = id, name = "Cat $id", type = type, iconKey = "i", colorKey = "blue")

    @Test
    fun `borrar una categoria sin transacciones no pide reasignar`() = runBlocking {
        coEvery { dao.getById(1L) } returns categoria(1)
        coEvery { dao.countTransactions(1L) } returns 0

        repository.delete(1L)

        coVerify { dao.deleteReassigning(1L, null, any()) }
    }

    @Test
    fun `borrar con transacciones y sin destino falla y no borra`() {
        coEvery { dao.getById(1L) } returns categoria(1)
        coEvery { dao.countTransactions(1L) } returns 3

        assertThrows(IllegalStateException::class.java) { runBlocking { repository.delete(1L) } }

        coVerify(exactly = 0) { dao.deleteReassigning(any(), any(), any()) }
    }

    @Test
    fun `borrar con transacciones reasigna al destino`() = runBlocking {
        coEvery { dao.getById(1L) } returns categoria(1)
        coEvery { dao.getById(2L) } returns categoria(2)
        coEvery { dao.countTransactions(1L) } returns 3

        repository.delete(1L, reassignToId = 2L)

        coVerify { dao.deleteReassigning(1L, 2L, any()) }
    }

    @Test
    fun `reasignar a la misma categoria falla`() {
        coEvery { dao.getById(1L) } returns categoria(1)
        coEvery { dao.countTransactions(1L) } returns 3

        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.delete(1L, 1L) } }
    }

    @Test
    fun `reasignar a una categoria inexistente falla`() {
        coEvery { dao.getById(1L) } returns categoria(1)
        coEvery { dao.getById(9L) } returns null
        coEvery { dao.countTransactions(1L) } returns 3

        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.delete(1L, 9L) } }
    }

    @Test
    fun `reasignar a una categoria de otro tipo falla`() {
        coEvery { dao.getById(1L) } returns categoria(1, "EXPENSE")
        coEvery { dao.getById(2L) } returns categoria(2, "INCOME")
        coEvery { dao.countTransactions(1L) } returns 3

        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.delete(1L, 2L) } }
    }

    @Test
    fun `borrar una categoria inexistente no hace nada`() = runBlocking {
        coEvery { dao.getById(1L) } returns null

        repository.delete(1L)

        coVerify(exactly = 0) { dao.deleteReassigning(any(), any(), any()) }
    }

    @Test
    fun `seedDefaults inserta solo si la tabla nunca tuvo filas`() = runBlocking {
        val defaults = listOf(categoria(0))
        coEvery { dao.countAllRows() } returns 0
        repository.seedDefaults(defaults)
        coVerify { dao.insertAll(defaults) }
    }

    @Test
    fun `seedDefaults no inserta si ya hubo filas`() = runBlocking {
        coEvery { dao.countAllRows() } returns 2
        repository.seedDefaults(listOf(categoria(0)))
        coVerify(exactly = 0) { dao.insertAll(any()) }
    }

    @Test
    fun `update actualiza updatedAt`() = runBlocking {
        val c = categoria(1).copy(updatedAt = 1L)
        repository.update(c)
        coVerify { dao.update(match { it.updatedAt > 1L }) }
    }
}
