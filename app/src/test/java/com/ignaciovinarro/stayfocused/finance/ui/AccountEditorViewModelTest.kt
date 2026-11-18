package com.ignaciovinarro.stayfocused.finance.ui

import androidx.lifecycle.SavedStateHandle
import com.ignaciovinarro.stayfocused.finance.data.AccountEntity
import com.ignaciovinarro.stayfocused.finance.data.AccountRepository
import com.ignaciovinarro.stayfocused.finance.domain.AccountType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class AccountEditorViewModelTest {

    private val repository = mockk<AccountRepository>(relaxed = true)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(accountId: Long = 0L) =
        AccountEditorViewModel(SavedStateHandle(mapOf("accountId" to accountId)), repository)

    @Test
    fun `cuenta nueva arranca vacia e invalida`() = runTest(dispatcher) {
        val vm = viewModel()

        assertNull(vm.uiState.value.accountId)
        assertFalse(vm.uiState.value.isValid)
    }

    @Test
    fun `con nombre y saldo vacio es valida y el saldo inicial es cero`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onNameChange("Efectivo")

        assertTrue(vm.uiState.value.isValid)
        assertEquals(0L, vm.uiState.value.initialBalanceCents)
    }

    @Test
    fun `un saldo con formato invalido hace invalido el formulario`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onNameChange("Banco")
        vm.onInitialBalanceChange("12,345")

        assertFalse(vm.uiState.value.isBalanceValid)
        assertFalse(vm.uiState.value.isValid)
    }

    @Test
    fun `el campo de saldo descarta caracteres que no son de un monto`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onInitialBalanceChange("1a2.3b4,50$")

        assertEquals("12.34,50", vm.uiState.value.initialBalanceText)
    }

    @Test
    fun `guardar una cuenta nueva la crea con los datos del formulario`() = runTest(dispatcher) {
        val creada = slot<AccountEntity>()
        coEvery { repository.create(capture(creada)) } returns 1L
        val vm = viewModel()
        vm.onNameChange("  Banco  ")
        vm.onTypeChange(AccountType.BANK)
        vm.onInitialBalanceChange("1.500,50")
        vm.onColorChange("green")
        var guardada = false

        vm.save { guardada = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(guardada)
        assertEquals("Banco", creada.captured.name)
        assertEquals("BANK", creada.captured.type)
        assertEquals(150_050L, creada.captured.initialBalance)
        assertEquals("green", creada.captured.colorKey)
        assertEquals("account_bank", creada.captured.iconKey)
    }

    @Test
    fun `guardar con el formulario invalido no hace nada`() = runTest(dispatcher) {
        val vm = viewModel()
        var guardada = false

        vm.save { guardada = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(guardada)
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test
    fun `editar carga la cuenta y al guardar la actualiza conservando su uuid`() = runTest(dispatcher) {
        val original = AccountEntity(
            id = 5, uuid = "uuid-5", name = "Tarjeta", type = "CARD", initialBalance = -20_000,
            iconKey = "account_card", colorKey = "red"
        )
        coEvery { repository.getById(5L) } returns original
        val actualizada = slot<AccountEntity>()
        coEvery { repository.update(capture(actualizada)) } returns Unit
        val vm = viewModel(5L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(5L, vm.uiState.value.accountId)
        assertEquals("Tarjeta", vm.uiState.value.name)
        assertEquals(AccountType.CARD, vm.uiState.value.type)
        assertEquals("-200", vm.uiState.value.initialBalanceText)

        vm.onNameChange("Tarjeta Visa")
        vm.save {}
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("uuid-5", actualizada.captured.uuid)
        assertEquals("Tarjeta Visa", actualizada.captured.name)
        assertEquals(-20_000L, actualizada.captured.initialBalance)
        coVerify(exactly = 0) { repository.create(any()) }
    }

    @Test
    fun `borrar una cuenta sin transacciones avisa que se borro`() = runTest(dispatcher) {
        coEvery { repository.getById(5L) } returns AccountEntity(id = 5, name = "X", type = "CASH", iconKey = "i", colorKey = "blue")
        coEvery { repository.delete(5L) } returns true
        val vm = viewModel(5L)
        dispatcher.scheduler.advanceUntilIdle()
        var borrada = false

        vm.delete { borrada = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(borrada)
        assertFalse(vm.uiState.value.showDeleteBlocked)
    }

    @Test
    fun `borrar una cuenta con transacciones muestra el aviso y no sale de la pantalla`() = runTest(dispatcher) {
        coEvery { repository.getById(5L) } returns AccountEntity(id = 5, name = "X", type = "CASH", iconKey = "i", colorKey = "blue")
        coEvery { repository.delete(5L) } returns false
        val vm = viewModel(5L)
        dispatcher.scheduler.advanceUntilIdle()
        var borrada = false

        vm.delete { borrada = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(borrada)
        assertTrue(vm.uiState.value.showDeleteBlocked)

        vm.onDismissDeleteBlocked()
        assertFalse(vm.uiState.value.showDeleteBlocked)
    }

    @Test
    fun `borrar una cuenta nueva no hace nada`() = runTest(dispatcher) {
        val vm = viewModel()
        var borrada = false

        vm.delete { borrada = true }
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(borrada)
        coVerify(exactly = 0) { repository.delete(any()) }
    }
}
