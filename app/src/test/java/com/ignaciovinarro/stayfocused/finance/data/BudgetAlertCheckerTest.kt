package com.ignaciovinarro.stayfocused.finance.data

import com.ignaciovinarro.stayfocused.finance.domain.BudgetAlert
import com.ignaciovinarro.stayfocused.finance.domain.toBudgetKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.YearMonth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class BudgetAlertCheckerTest {

    private val budgetRepository = mockk<BudgetRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val transactionRepository = mockk<TransactionRepository>()
    private val store = mockk<BudgetAlertStore>(relaxed = true)
    private val notifier = mockk<BudgetAlertNotifier>(relaxed = true)
    private lateinit var checker: BudgetAlertChecker

    private val nov = YearMonth.of(2026, 11)
    private val comida = CategoryEntity(id = 1, name = "Comida", type = "EXPENSE", iconKey = "i", colorKey = "blue")
    private val sueldo = CategoryEntity(id = 2, name = "Sueldo", type = "INCOME", iconKey = "i", colorKey = "blue")

    @Before
    fun setUp() {
        every { categoryRepository.observeAll() } returns flowOf(listOf(comida, sueldo))
        coEvery { store.load(nov) } returns emptyMap()
        checker = BudgetAlertChecker(budgetRepository, categoryRepository, transactionRepository, store, notifier)
    }

    private fun conDatos(limit: Long, spent: Long, categoryId: Long? = 1L) {
        every { budgetRepository.observeAll() } returns flowOf(
            listOf(BudgetEntity(categoryId = categoryId, effectiveFrom = nov.toBudgetKey(), amount = limit))
        )
        every { transactionRepository.observeAll() } returns flowOf(
            listOf(TransactionEntity(type = "EXPENSE", amount = spent, date = nov.atDay(3).toEpochDay(), accountId = 1, categoryId = 1L))
        )
    }

    @Test
    fun `al llegar al 80 por ciento avisa con el nombre de la categoria y guarda el nivel`() = runBlocking {
        conDatos(limit = 10_000, spent = 8_000)

        checker.check(nov)

        verify { notifier.show(nov, 1L, "Comida", BudgetAlert.WARNING_80) }
        coVerify { store.save(nov, mapOf(1L to BudgetAlert.WARNING_80)) }
    }

    @Test
    fun `el presupuesto total se avisa sin nombre de categoria`() = runBlocking {
        conDatos(limit = 10_000, spent = 10_000, categoryId = null)

        checker.check(nov)

        verify { notifier.show(nov, null, null, BudgetAlert.REACHED_100) }
    }

    @Test
    fun `no repite un aviso que ya se hizo`() = runBlocking {
        conDatos(limit = 10_000, spent = 8_500)
        coEvery { store.load(nov) } returns mapOf(1L to BudgetAlert.WARNING_80)

        checker.check(nov)

        verify(exactly = 0) { notifier.show(any(), any(), any(), any()) }
        coVerify { store.save(nov, mapOf(1L to BudgetAlert.WARNING_80)) }
    }

    @Test
    fun `sube del 80 al 100 y avisa de nuevo`() = runBlocking {
        conDatos(limit = 10_000, spent = 10_500)
        coEvery { store.load(nov) } returns mapOf(1L to BudgetAlert.WARNING_80)

        checker.check(nov)

        verify { notifier.show(nov, 1L, "Comida", BudgetAlert.REACHED_100) }
    }

    @Test
    fun `si el gasto bajo del nivel guarda el nivel nuevo sin avisar`() = runBlocking {
        conDatos(limit = 10_000, spent = 1_000)
        coEvery { store.load(nov) } returns mapOf(1L to BudgetAlert.REACHED_100)

        checker.check(nov)

        verify(exactly = 0) { notifier.show(any(), any(), any(), any()) }
        coVerify { store.save(nov, emptyMap()) }
    }

    @Test
    fun `sin presupuestos no avisa nada`() = runBlocking {
        every { budgetRepository.observeAll() } returns flowOf(emptyList())
        every { transactionRepository.observeAll() } returns flowOf(emptyList())

        checker.check(nov)

        verify(exactly = 0) { notifier.show(any(), any(), any(), any()) }
    }
}
