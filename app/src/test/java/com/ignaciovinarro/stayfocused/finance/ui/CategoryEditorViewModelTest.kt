package com.ignaciovinarro.stayfocused.finance.ui

import androidx.lifecycle.SavedStateHandle
import com.ignaciovinarro.stayfocused.finance.data.CategoryEntity
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.domain.CategoryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class CategoryEditorViewModelTest {

    private val repository = mockk<CategoryRepository>(relaxed = true)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun categoria(id: Long, type: String = "EXPENSE") =
        CategoryEntity(id = id, name = "Cat $id", type = type, iconKey = "category_food", colorKey = "red")

    private fun viewModel(categoryId: Long = 0L, type: String? = null): CategoryEditorViewModel {
        val args = mutableMapOf<String, Any?>("categoryId" to categoryId)
        if (type != null) args["type"] = type
        return CategoryEditorViewModel(SavedStateHandle(args), repository)
    }

    @Test
    fun `categoria nueva arranca vacia, invalida y con el tipo recibido`() = runTest(dispatcher) {
        val vm = viewModel(type = "INCOME")

        assertNull(vm.uiState.value.categoryId)
        assertEquals(CategoryType.INCOME, vm.uiState.value.type)
        assertFalse(vm.uiState.value.isValid)
    }

    @Test
    fun `guardar una nueva crea la categoria con nombre recortado y el tipo`() = runTest(dispatcher) {
        val created = slot<CategoryEntity>()
        coEvery { repository.create(capture(created)) } returns 1L
        val vm = viewModel(type = "INCOME")
        vm.onNameChange("  Regalos  ")
        vm.onIconChange("category_gift")
        vm.onColorChange("pink")

        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()

        assertTrue(saved)
        assertEquals("Regalos", created.captured.name)
        assertEquals("INCOME", created.captured.type)
        assertEquals("category_gift", created.captured.iconKey)
        assertEquals("pink", created.captured.colorKey)
    }

    @Test
    fun `editar carga la categoria y guarda sin cambiar tipo ni uuid`() = runTest(dispatcher) {
        val original = categoria(5, "INCOME")
        coEvery { repository.getById(5L) } returns original
        val updated = slot<CategoryEntity>()
        coEvery { repository.update(capture(updated)) } returns Unit
        val vm = viewModel(5L)
        advanceUntilIdle()
        assertEquals("Cat 5", vm.uiState.value.name)
        assertEquals(CategoryType.INCOME, vm.uiState.value.type)

        vm.onNameChange("Nuevo nombre")
        vm.save {}
        advanceUntilIdle()

        assertEquals("Nuevo nombre", updated.captured.name)
        assertEquals("INCOME", updated.captured.type)
        assertEquals(original.uuid, updated.captured.uuid)
    }

    @Test
    fun `borrar sin transacciones pide confirmacion simple y borra sin reasignar`() = runTest(dispatcher) {
        coEvery { repository.getById(1L) } returns categoria(1)
        coEvery { repository.countTransactions(1L) } returns 0
        every { repository.observeAll() } returns flowOf(listOf(categoria(1), categoria(2)))
        val vm = viewModel(1L)
        advanceUntilIdle()

        vm.requestDelete()
        advanceUntilIdle()
        val request = vm.uiState.value.deleteRequest!!
        assertFalse(request.needsReassign)

        var deleted = false
        vm.confirmDelete(null) { deleted = true }
        advanceUntilIdle()

        assertTrue(deleted)
        coVerify { repository.delete(1L, null) }
        assertNull(vm.uiState.value.deleteRequest)
    }

    @Test
    fun `con transacciones ofrece solo categorias del mismo tipo y exige elegir destino`() = runTest(dispatcher) {
        coEvery { repository.getById(1L) } returns categoria(1)
        coEvery { repository.countTransactions(1L) } returns 4
        every { repository.observeAll() } returns flowOf(
            listOf(categoria(1), categoria(2), categoria(3, "INCOME"))
        )
        val vm = viewModel(1L)
        advanceUntilIdle()

        vm.requestDelete()
        advanceUntilIdle()
        val request = vm.uiState.value.deleteRequest!!
        assertTrue(request.needsReassign)
        assertEquals(listOf(2L), request.candidates.map { it.id })

        // Sin destino no borra.
        vm.confirmDelete(null) {}
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.delete(any(), any()) }

        vm.confirmDelete(2L) {}
        advanceUntilIdle()
        coVerify { repository.delete(1L, 2L) }
    }

    @Test
    fun `con transacciones y sin otra categoria del tipo el borrado queda bloqueado`() = runTest(dispatcher) {
        coEvery { repository.getById(1L) } returns categoria(1)
        coEvery { repository.countTransactions(1L) } returns 2
        every { repository.observeAll() } returns flowOf(listOf(categoria(1), categoria(3, "INCOME")))
        val vm = viewModel(1L)
        advanceUntilIdle()

        vm.requestDelete()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.deleteRequest!!.isBlocked)
    }

    @Test
    fun `cancelar el borrado limpia el pedido`() = runTest(dispatcher) {
        coEvery { repository.getById(1L) } returns categoria(1)
        coEvery { repository.countTransactions(1L) } returns 0
        every { repository.observeAll() } returns flowOf(listOf(categoria(1)))
        val vm = viewModel(1L)
        advanceUntilIdle()
        vm.requestDelete()
        advanceUntilIdle()

        vm.onDismissDelete()

        assertNull(vm.uiState.value.deleteRequest)
    }
}
