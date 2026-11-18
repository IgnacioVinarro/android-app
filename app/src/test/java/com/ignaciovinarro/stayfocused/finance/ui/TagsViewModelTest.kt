package com.ignaciovinarro.stayfocused.finance.ui

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.finance.data.TagEntity
import com.ignaciovinarro.stayfocused.finance.data.TagRepository
import com.ignaciovinarro.stayfocused.finance.domain.TagNameError
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class TagsViewModelTest {

    private val repository = mockk<TagRepository>(relaxed = true)
    private val viaje = TagEntity(id = 1, name = "viaje")
    private val auto = TagEntity(id = 2, name = "auto")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repository.observeAll() } returns flowOf(listOf(viaje, auto))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Deja la lista cargada y devuelve el ViewModel ya con suscriptor, para poder leer `uiState.value`.
    private suspend fun kotlinx.coroutines.test.TestScope.cargado(block: suspend (TagsViewModel) -> Unit) {
        val vm = TagsViewModel(repository)
        vm.uiState.test {
            var state = awaitItem()
            while (state.tags.isEmpty()) state = awaitItem()
            block(vm)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `crear una etiqueta valida guarda el nombre y cierra el dialogo`() = runTest {
        cargado { vm ->
            vm.onAddClick()
            vm.onNameChange("  salud ")
            assertTrue(vm.uiState.value.dialog!!.canSave)

            vm.onSaveDialog()

            coVerify { repository.create("  salud ") }
            assertNull(vm.uiState.value.dialog)
        }
    }

    @Test
    fun `un nombre vacio no se puede guardar pero no muestra error mientras no se escribe`() = runTest {
        cargado { vm ->
            vm.onAddClick()

            val dialog = vm.uiState.value.dialog!!
            assertFalse(dialog.canSave)
            assertNull(dialog.visibleError)

            vm.onSaveDialog()
            coVerify(exactly = 0) { repository.create(any()) }
        }
    }

    @Test
    fun `un nombre repetido ignora mayusculas y no se guarda`() = runTest {
        cargado { vm ->
            vm.onAddClick()
            vm.onNameChange("VIAJE")

            assertEquals(TagNameError.DUPLICATE, vm.uiState.value.dialog!!.visibleError)
            vm.onSaveDialog()
            coVerify(exactly = 0) { repository.create(any()) }
        }
    }

    @Test
    fun `un nombre demasiado largo es invalido`() = runTest {
        cargado { vm ->
            vm.onAddClick()
            vm.onNameChange("x".repeat(31))

            assertEquals(TagNameError.TOO_LONG, vm.uiState.value.dialog!!.visibleError)
        }
    }

    @Test
    fun `renombrar permite conservar el propio nombre y rechaza el de otra etiqueta`() = runTest {
        cargado { vm ->
            vm.onRenameClick(viaje)
            assertTrue(vm.uiState.value.dialog!!.canSave) // su propio nombre no cuenta como repetido

            vm.onNameChange("auto")
            assertEquals(TagNameError.DUPLICATE, vm.uiState.value.dialog!!.visibleError)

            vm.onNameChange("vacaciones")
            vm.onSaveDialog()
            coVerify { repository.rename(1L, "vacaciones") }
            assertNull(vm.uiState.value.dialog)
        }
    }

    @Test
    fun `borrar pide confirmacion y recien al confirmar borra`() = runTest {
        cargado { vm ->
            vm.onDeleteClick(auto)
            assertEquals(auto, vm.uiState.value.deleteTarget)
            coVerify(exactly = 0) { repository.delete(any()) }

            vm.onConfirmDelete()

            coVerify { repository.delete(2L) }
            assertNull(vm.uiState.value.deleteTarget)
        }
    }

    @Test
    fun `cancelar el borrado no borra nada`() = runTest {
        cargado { vm ->
            vm.onDeleteClick(auto)
            vm.onDismissDelete()

            assertNull(vm.uiState.value.deleteTarget)
            coVerify(exactly = 0) { repository.delete(any()) }
        }
    }
}
