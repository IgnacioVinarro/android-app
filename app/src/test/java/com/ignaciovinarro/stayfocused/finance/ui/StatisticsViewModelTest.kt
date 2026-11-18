package com.ignaciovinarro.stayfocused.finance.ui

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.finance.data.CategoryEntity
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val month = YearMonth.now()

    private val comida = CategoryEntity(id = 1, name = "Comida", type = "EXPENSE", iconKey = "category_food", colorKey = "orange")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun tx(type: String, amount: Long, m: YearMonth = month, categoryId: Long? = 1L) =
        TransactionEntity(type = type, amount = amount, date = m.atDay(1).toEpochDay(), accountId = 1, categoryId = categoryId)

    private fun viewModel(transactions: List<TransactionEntity>): StatisticsViewModel {
        every { transactionRepository.observeAll() } returns flowOf(transactions)
        every { categoryRepository.observeAll() } returns flowOf(listOf(comida))
        return StatisticsViewModel(transactionRepository, categoryRepository)
    }

    private suspend fun TestScope.cargado(vm: StatisticsViewModel, block: (StatisticsUiState) -> Unit) {
        vm.uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            block(state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resumen del mes con la variacion contra el mes anterior`() = runTest {
        val vm = viewModel(
            listOf(
                tx("INCOME", 200_000), tx("EXPENSE", 150_000),
                tx("INCOME", 100_000, month.minusMonths(1)), tx("EXPENSE", 100_000, month.minusMonths(1))
            )
        )
        cargado(vm) { state ->
            assertEquals(SummaryFigure(200_000, 100), state.income)
            assertEquals(SummaryFigure(150_000, 50), state.expense)
            assertEquals(SummaryFigure(50_000, null), state.savings) // el mes anterior ahorró 0: sin base
            assertTrue(state.hasMonthData)
        }
    }

    @Test
    fun `el ranking resuelve el nombre de la categoria y deja las sin categoria con nombre nulo`() = runTest {
        val vm = viewModel(listOf(tx("EXPENSE", 6_000, categoryId = 1L), tx("EXPENSE", 4_000, categoryId = null)))
        cargado(vm) { state ->
            assertEquals(listOf<String?>("Comida", null), state.categories.map { it.name })
            assertEquals(listOf(60, 40), state.categories.map { it.percent })
            assertNull(state.categories[1].iconKey)
        }
    }

    @Test
    fun `la evolucion trae seis meses con el actual al final y valores en pesos`() = runTest {
        val vm = viewModel(listOf(tx("EXPENSE", 150_000), tx("EXPENSE", 50_000, month.minusMonths(5))))
        cargado(vm) { state ->
            assertEquals(STATISTICS_MONTHS, state.monthlyExpense.size)
            assertEquals(1500.0, state.monthlyExpense.last().value, 0.0)
            assertEquals(500.0, state.monthlyExpense.first().value, 0.0)
        }
    }

    @Test
    fun `sin movimientos no hay datos del mes ni ranking`() = runTest {
        val vm = viewModel(emptyList())
        cargado(vm) { state ->
            assertFalse(state.hasMonthData)
            assertTrue(state.categories.isEmpty())
            assertTrue(state.monthlyExpense.all { it.value == 0.0 })
        }
    }
}
