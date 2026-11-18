package com.ignaciovinarro.stayfocused.finance.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

class BudgetRepositoryImplTest {

    private val dao = mockk<BudgetDao>(relaxed = true)
    private lateinit var repository: BudgetRepository

    @Before
    fun setUp() {
        repository = BudgetRepositoryImpl(dao)
    }

    @Test
    fun `setLimit crea un presupuesto nuevo si no hay uno para ese mes`() = runBlocking {
        coEvery { dao.findExact(1L, 202611) } returns null

        repository.setLimit(1L, YearMonth.of(2026, 11), 5_000)

        coVerify { dao.insert(match { it.categoryId == 1L && it.effectiveFrom == 202611 && it.amount == 5_000L }) }
    }

    @Test
    fun `setLimit reemplaza el monto si ya hay uno para ese mes`() = runBlocking {
        val existente = BudgetEntity(id = 7, categoryId = 1L, effectiveFrom = 202611, amount = 5_000, updatedAt = 1L)
        coEvery { dao.findExact(1L, 202611) } returns existente

        repository.setLimit(1L, YearMonth.of(2026, 11), 9_000)

        coVerify { dao.update(match { it.id == 7L && it.amount == 9_000L && it.updatedAt > 1L }) }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `setLimit acepta presupuesto total con categoria null`() = runBlocking {
        coEvery { dao.findExact(null, 202611) } returns null

        repository.setLimit(null, YearMonth.of(2026, 11), 20_000)

        coVerify { dao.insert(match { it.categoryId == null && it.amount == 20_000L }) }
    }

    @Test
    fun `setLimit rechaza montos cero o negativos`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.setLimit(1L, YearMonth.of(2026, 11), 0) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.setLimit(1L, YearMonth.of(2026, 11), -5) }
        }
    }
}
