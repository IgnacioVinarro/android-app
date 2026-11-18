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

class AccountRepositoryImplTest {

    private val dao = mockk<AccountDao>(relaxed = true)
    private lateinit var repository: AccountRepository

    @Before
    fun setUp() {
        repository = AccountRepositoryImpl(dao)
    }

    private fun cuenta(updatedAt: Long = System.currentTimeMillis()) =
        AccountEntity(name = "Efectivo", type = "CASH", iconKey = "i", colorKey = "green", updatedAt = updatedAt)

    @Test
    fun `observeAll y getById delegan en el DAO`() = runBlocking {
        val c = cuenta()
        every { dao.observeAll() } returns flowOf(listOf(c))
        coEvery { dao.getById(1L) } returns c

        assertEquals(listOf(c), repository.observeAll().first())
        assertEquals(c, repository.getById(1L))
    }

    @Test
    fun `create delega el insert en el DAO`() = runBlocking {
        val c = cuenta()
        coEvery { dao.insert(c) } returns 3L

        assertEquals(3L, repository.create(c))
    }

    @Test
    fun `update actualiza updatedAt`() = runBlocking {
        repository.update(cuenta(updatedAt = 1L))

        coVerify { dao.update(match { it.updatedAt > 1L }) }
    }
}
