package com.ignaciovinarro.stayfocused.finance.ui

import androidx.lifecycle.SavedStateHandle
import com.ignaciovinarro.stayfocused.finance.data.AccountEntity
import com.ignaciovinarro.stayfocused.finance.data.AccountRepository
import com.ignaciovinarro.stayfocused.finance.data.CategoryEntity
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionRepository
import com.ignaciovinarro.stayfocused.finance.domain.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionEditorViewModelTest {

    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val dispatcher = StandardTestDispatcher()

    private val efectivo = AccountEntity(id = 1, name = "Efectivo", type = "CASH", iconKey = "i", colorKey = "green")
    private val banco = AccountEntity(id = 2, name = "Banco", type = "BANK", iconKey = "i", colorKey = "blue")
    private val comida = CategoryEntity(id = 10, name = "Comida", type = "EXPENSE", iconKey = "i", colorKey = "orange")
    private val sueldo = CategoryEntity(id = 11, name = "Sueldo", type = "INCOME", iconKey = "i", colorKey = "green")

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { accountRepository.observeAll() } returns flowOf(listOf(efectivo, banco))
        every { categoryRepository.observeAll() } returns flowOf(listOf(comida, sueldo))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(transactionId: Long = 0L): TransactionEditorViewModel {
        val vm = TransactionEditorViewModel(
            SavedStateHandle(mapOf("transactionId" to transactionId)),
            transactionRepository, accountRepository, categoryRepository
        )
        dispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    @Test
    fun `movimiento nuevo arranca como gasto de hoy con la primera cuenta`() = runTest(dispatcher) {
        val vm = viewModel()

        val estado = vm.uiState.value
        assertNull(estado.transactionId)
        assertEquals(TransactionType.EXPENSE, estado.type)
        assertEquals(LocalDate.now(), estado.date)
        assertEquals(1L, estado.accountId)
        assertFalse(estado.isValid)
    }

    @Test
    fun `solo ofrece las categorias del tipo elegido`() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(listOf(comida), vm.uiState.value.visibleCategories)

        vm.onTypeChange(TransactionType.INCOME)
        assertEquals(listOf(sueldo), vm.uiState.value.visibleCategories)

        vm.onTypeChange(TransactionType.TRANSFER)
        assertTrue(vm.uiState.value.visibleCategories.isEmpty())
    }

    @Test
    fun `un gasto es valido con monto y categoria`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAmountChange("1.500,50")
        assertFalse(vm.uiState.value.isValid) // falta la categoría

        vm.onCategoryChange(10L)
        assertTrue(vm.uiState.value.isValid)
    }

    @Test
    fun `monto cero o mal escrito es invalido y se marca como error`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCategoryChange(10L)

        vm.onAmountChange("0")
        assertFalse(vm.uiState.value.isValid)
        assertTrue(vm.uiState.value.isAmountInvalid)

        vm.onAmountChange("12,345")
        assertFalse(vm.uiState.value.isValid)
        assertTrue(vm.uiState.value.isAmountInvalid)

        vm.onAmountChange("")
        assertFalse(vm.uiState.value.isAmountInvalid) // vacío: todavía no escribió nada
    }

    @Test
    fun `el campo de monto descarta caracteres que no son de un monto`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAmountChange("-1a0$")

        assertEquals("10", vm.uiState.value.amountText)
    }

    @Test
    fun `cambiar el tipo descarta la categoria de otro tipo`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onCategoryChange(10L)

        vm.onTypeChange(TransactionType.INCOME)

        assertNull(vm.uiState.value.categoryId)
    }

    @Test
    fun `una transferencia necesita una cuenta destino distinta del origen`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTypeChange(TransactionType.TRANSFER)
        vm.onAmountChange("500")
        assertFalse(vm.uiState.value.isValid) // falta el destino

        vm.onTransferAccountChange(2L)
        assertTrue(vm.uiState.value.isValid)
        assertEquals(listOf(banco), vm.uiState.value.destinationAccounts)
    }

    @Test
    fun `cambiar la cuenta de origen descarta el destino si coincide`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTypeChange(TransactionType.TRANSFER)
        vm.onTransferAccountChange(2L)

        vm.onAccountChange(2L)

        assertNull(vm.uiState.value.transferAccountId)
    }

    @Test
    fun `salir de transferencia descarta la cuenta destino`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onTypeChange(TransactionType.TRANSFER)
        vm.onTransferAccountChange(2L)

        vm.onTypeChange(TransactionType.EXPENSE)

        assertNull(vm.uiState.value.transferAccountId)
    }

    @Test
    fun `guardar un gasto nuevo lo crea con los datos del formulario`() = runTest(dispatcher) {
        val creada = slot<TransactionEntity>()
        coEvery { transactionRepository.create(capture(creada), any()) } returns 1L
        val vm = viewModel()
        vm.onAmountChange("1.500,50")
        vm.onCategoryChange(10L)
        vm.onAccountChange(2L)
        vm.onDateChange(LocalDate.of(2026, 11, 5))
        vm.onNoteChange("  almuerzo  ")
        var guardada = false

        vm.save { guardada = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(guardada)
        assertEquals("EXPENSE", creada.captured.type)
        assertEquals(150_050L, creada.captured.amount)
        assertEquals(LocalDate.of(2026, 11, 5).toEpochDay(), creada.captured.date)
        assertEquals(2L, creada.captured.accountId)
        assertEquals(10L, creada.captured.categoryId)
        assertNull(creada.captured.transferAccountId)
        assertEquals("almuerzo", creada.captured.note)
    }

    @Test
    fun `una nota vacia se guarda como nula`() = runTest(dispatcher) {
        val creada = slot<TransactionEntity>()
        coEvery { transactionRepository.create(capture(creada), any()) } returns 1L
        val vm = viewModel()
        vm.onAmountChange("10")
        vm.onCategoryChange(10L)
        vm.onNoteChange("   ")

        vm.save {}
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(creada.captured.note)
    }

    @Test
    fun `guardar con el formulario invalido no hace nada`() = runTest(dispatcher) {
        val vm = viewModel()
        var guardada = false

        vm.save { guardada = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(guardada)
        coVerify(exactly = 0) { transactionRepository.create(any(), any()) }
    }

    @Test
    fun `editar carga el movimiento y al guardar lo actualiza conservando su uuid`() = runTest(dispatcher) {
        val original = TransactionEntity(
            id = 7, uuid = "uuid-7", type = "INCOME", amount = 20_000, date = 19000,
            accountId = 2, categoryId = 11, note = "sueldo"
        )
        coEvery { transactionRepository.getById(7L) } returns original
        val actualizada = slot<TransactionEntity>()
        coEvery { transactionRepository.update(capture(actualizada), any()) } returns Unit
        val vm = viewModel(7L)

        val estado = vm.uiState.value
        assertEquals(7L, estado.transactionId)
        assertEquals(TransactionType.INCOME, estado.type)
        assertEquals("200", estado.amountText)
        assertEquals(2L, estado.accountId)
        assertEquals(11L, estado.categoryId)
        assertEquals("sueldo", estado.note)

        vm.onAmountChange("250")
        vm.save {}
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("uuid-7", actualizada.captured.uuid)
        assertEquals(25_000L, actualizada.captured.amount)
        coVerify(exactly = 0) { transactionRepository.create(any(), any()) }
    }

    @Test
    fun `borrar un movimiento existente lo borra y avisa`() = runTest(dispatcher) {
        coEvery { transactionRepository.getById(7L) } returns
            TransactionEntity(id = 7, type = "EXPENSE", amount = 100, date = 19000, accountId = 1, categoryId = 10)
        val vm = viewModel(7L)
        var borrada = false

        vm.delete { borrada = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(borrada)
        coVerify { transactionRepository.delete(7L) }
    }

    @Test
    fun `borrar un movimiento nuevo no hace nada`() = runTest(dispatcher) {
        val vm = viewModel()
        var borrada = false

        vm.delete { borrada = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(borrada)
        coVerify(exactly = 0) { transactionRepository.delete(any()) }
    }
}
