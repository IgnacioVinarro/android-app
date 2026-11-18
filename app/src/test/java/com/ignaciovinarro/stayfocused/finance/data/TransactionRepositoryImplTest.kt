package com.ignaciovinarro.stayfocused.finance.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TransactionRepositoryImplTest {

    private val dao = mockk<TransactionDao>(relaxed = true)
    private val tagDao = mockk<TransactionTagDao>(relaxed = true)
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        repository = TransactionRepositoryImpl(dao, tagDao)
    }

    private fun gasto(id: Long = 0, updatedAt: Long = System.currentTimeMillis()) = TransactionEntity(
        id = id, type = "EXPENSE", amount = 1_000, date = 19000, accountId = 1, categoryId = 2, updatedAt = updatedAt
    )

    @Test
    fun `observeAll y observeBetween delegan en el DAO`() = runBlocking {
        val lista = listOf(gasto(1))
        every { dao.observeAll() } returns flowOf(lista)
        every { dao.observeBetween(10, 20) } returns flowOf(lista)

        assertEquals(lista, repository.observeAll().first())
        assertEquals(lista, repository.observeBetween(10, 20).first())
    }

    @Test
    fun `create sin etiquetas solo inserta la transaccion`() = runBlocking {
        coEvery { dao.insert(any()) } returns 5L

        val id = repository.create(gasto())

        assertEquals(5L, id)
        coVerify(exactly = 0) { tagDao.replaceTags(any(), any(), any()) }
    }

    @Test
    fun `create con etiquetas las asigna a la transaccion nueva`() = runBlocking {
        coEvery { dao.insert(any()) } returns 5L

        repository.create(gasto(), setOf(1L, 2L))

        coVerify { tagDao.replaceTags(5L, setOf(1L, 2L), any()) }
    }

    @Test
    fun `update actualiza updatedAt y reemplaza las etiquetas`() = runBlocking {
        repository.update(gasto(id = 3, updatedAt = 1L), setOf(4L))

        coVerify { dao.update(match { it.id == 3L && it.updatedAt > 1L }) }
        coVerify { tagDao.replaceTags(3L, setOf(4L), any()) }
    }

    @Test
    fun `update con conjunto vacio quita todas las etiquetas`() = runBlocking {
        repository.update(gasto(id = 3), emptySet())

        coVerify { tagDao.replaceTags(3L, emptySet(), any()) }
    }

    @Test
    fun `delete delega el borrado logico en el DAO`() = runBlocking {
        repository.delete(8L)

        coVerify { dao.softDelete(8L, any()) }
    }
}
