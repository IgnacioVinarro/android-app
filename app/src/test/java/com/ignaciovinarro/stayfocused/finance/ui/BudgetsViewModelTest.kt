package com.ignaciovinarro.stayfocused.finance.ui

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.finance.data.BudgetEntity
import com.ignaciovinarro.stayfocused.finance.data.BudgetRepository
import com.ignaciovinarro.stayfocused.finance.data.CategoryEntity
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionRepository
import com.ignaciovinarro.stayfocused.finance.domain.BudgetAlert
import com.ignaciovinarro.stayfocused.finance.domain.toBudgetKey
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
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
class BudgetsViewModelTest {

    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>()
    private val transactionRepository = mockk<TransactionRepository>()

    private val month = YearMonth.now()
    private val comida = CategoryEntity(id = 10, name = "Comida", type = "EXPENSE", iconKey = "category_food", colorKey = "orange")
    private val ocio = CategoryEntity(id = 11, name = "Ocio", type = "EXPENSE", iconKey = "category_leisure", colorKey = "purple")
    private val sueldo = CategoryEntity(id = 12, name = "Sueldo", type = "INCOME", iconKey = "category_salary", colorKey = "green")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun gasto(amount: Long, categoryId: Long, day: Long = month.atDay(1).toEpochDay()) =
        TransactionEntity(type = "EXPENSE", amount = amount, date = day, accountId = 1, categoryId = categoryId)

    private fun presupuesto(categoryId: Long?, amount: Long) =
        BudgetEntity(categoryId = categoryId, effectiveFrom = month.toBudgetKey(), amount = amount)

    private fun viewModel(
        budgets: List<BudgetEntity> = emptyList(),
        transactions: List<TransactionEntity> = emptyList()
    ): BudgetsViewModel {
        every { budgetRepository.observeAll() } returns flowOf(budgets)
        every { categoryRepository.observeAll() } returns flowOf(listOf(comida, ocio, sueldo))
        every { transactionRepository.observeAll() } returns flowOf(transactions)
        return BudgetsViewModel(budgetRepository, categoryRepository, transactionRepository)
    }

    // Espera a que lleguen los datos y recién ahí corre el bloque, con el ViewModel suscripto.
    private suspend fun TestScope.cargado(vm: BudgetsViewModel, block: suspend (BudgetsViewModel) -> Unit) {
        vm.uiState.test {
            var state = awaitItem()
            while (state.isLoading) state = awaitItem()
            block(vm)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `muestra el total primero y solo las categorias de gasto`() = runTest {
        cargado(viewModel()) { vm ->
            val rows = vm.uiState.value.rows
            assertEquals(listOf<Long?>(null, 10L, 11L), rows.map { it.categoryId })
            assertTrue(rows.all { it.limit == null && it.progress == null })
        }
    }

    @Test
    fun `calcula gastado, porcentaje y alerta por categoria y en el total`() = runTest {
        val vm = viewModel(
            budgets = listOf(presupuesto(10L, 10_000), presupuesto(null, 100_000)),
            transactions = listOf(gasto(8_000, 10L), gasto(1_000, 11L))
        )
        cargado(vm) {
            val rows = vm.uiState.value.rows.associateBy { it.categoryId }
            assertEquals(80, rows.getValue(10L).progress!!.percent)
            assertEquals(BudgetAlert.WARNING_80, rows.getValue(10L).progress!!.alert)
            assertEquals(9_000L, rows.getValue(null).spent)
            assertEquals(BudgetAlert.NONE, rows.getValue(null).progress!!.alert)
            assertNull(rows.getValue(11L).progress) // sin presupuesto, pero igual muestra lo gastado
            assertEquals(1_000L, rows.getValue(11L).spent)
        }
    }

    @Test
    fun `un gasto de otro mes no cuenta`() = runTest {
        val vm = viewModel(
            budgets = listOf(presupuesto(10L, 10_000)),
            transactions = listOf(gasto(9_000, 10L, day = month.minusMonths(1).atDay(5).toEpochDay()))
        )
        cargado(vm) {
            assertEquals(0L, vm.uiState.value.rows.first { it.categoryId == 10L }.spent)
        }
    }

    @Test
    fun `al llegar al cien por ciento marca la alerta maxima`() = runTest {
        val vm = viewModel(
            budgets = listOf(presupuesto(10L, 10_000)),
            transactions = listOf(gasto(12_000, 10L))
        )
        cargado(vm) {
            assertEquals(BudgetAlert.REACHED_100, vm.uiState.value.rows.first { it.categoryId == 10L }.progress!!.alert)
        }
    }

    @Test
    fun `tocar un renglon con limite abre el dialogo con el monto actual`() = runTest {
        val vm = viewModel(budgets = listOf(presupuesto(10L, 150_050)))
        cargado(vm) {
            vm.onRowClick(vm.uiState.value.rows.first { it.categoryId == 10L })

            val dialog = vm.uiState.value.dialog!!
            assertEquals(10L, dialog.categoryId)
            assertEquals("1500,50", dialog.amountText)
        }
    }

    @Test
    fun `guardar fija el limite del mes actual en centavos y cierra el dialogo`() = runTest {
        cargado(viewModel()) { vm ->
            vm.onRowClick(vm.uiState.value.rows.first { it.categoryId == 11L })
            vm.onAmountChange("2.500,50")
            assertTrue(vm.uiState.value.dialog!!.canSave)

            vm.onSaveDialog()

            coVerify { budgetRepository.setLimit(11L, month, 250_050L) }
            assertNull(vm.uiState.value.dialog)
        }
    }

    @Test
    fun `un monto vacio o cero no se puede guardar`() = runTest {
        cargado(viewModel()) { vm ->
            vm.onRowClick(vm.uiState.value.rows.first())
            assertFalse(vm.uiState.value.dialog!!.canSave)

            vm.onAmountChange("0")
            assertFalse(vm.uiState.value.dialog!!.canSave)
            assertTrue(vm.uiState.value.dialog!!.isAmountInvalid)

            vm.onSaveDialog()
            coVerify(exactly = 0) { budgetRepository.setLimit(any(), any(), any()) }
        }
    }

    @Test
    fun `cancelar cierra el dialogo sin guardar`() = runTest {
        cargado(viewModel()) { vm ->
            vm.onRowClick(vm.uiState.value.rows.first())
            vm.onDismissDialog()

            assertNull(vm.uiState.value.dialog)
            coVerify(exactly = 0) { budgetRepository.setLimit(any(), any(), any()) }
        }
    }
}
