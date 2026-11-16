package com.ignaciovinarro.stayfocused.planner.ui

import androidx.lifecycle.SavedStateHandle
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemRepository
import com.ignaciovinarro.stayfocused.planner.data.RoutineRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineEditorViewModelTest {

    private val routineRepository = mockk<RoutineRepository>()
    private val routineItemRepository = mockk<RoutineItemRepository>()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `una rutina nueva empieza vacia y sin items`() = runTest(dispatcher) {
        val viewModel = RoutineEditorViewModel(SavedStateHandle(mapOf("routineId" to 0L)), routineRepository, routineItemRepository)

        assertEquals("", viewModel.uiState.value.name)
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertFalseIsValid(viewModel)
    }

    @Test
    fun `carga una rutina existente mostrando la hora absoluta de cada item`() = runTest(dispatcher) {
        val rutina = RoutineEntity(id = 1L, name = "Rutina de mañana", startMinutes = 420)
        val items = listOf(
            RoutineItemEntity(id = 1L, routineId = 1L, title = "Ducharse", offsetMinutes = 0, durationMinutes = 15, iconKey = "task_default", colorKey = "blue", position = 0),
            RoutineItemEntity(id = 2L, routineId = 1L, title = "Desayunar", offsetMinutes = 30, durationMinutes = 20, iconKey = "task_default", colorKey = "blue", position = 1)
        )
        coEvery { routineRepository.getById(1L) } returns rutina
        coEvery { routineItemRepository.getByRoutine(1L) } returns items
        val viewModel = RoutineEditorViewModel(SavedStateHandle(mapOf("routineId" to 1L)), routineRepository, routineItemRepository)
        dispatcher.scheduler.advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals("Rutina de mañana", estado.name)
        assertEquals(LocalTime.of(7, 0), estado.startTime)
        assertEquals(LocalTime.of(7, 0), estado.items[0].time)
        assertEquals(LocalTime.of(7, 30), estado.items[1].time)
    }

    @Test
    fun `agregar y quitar items actualiza la lista`() = runTest(dispatcher) {
        val viewModel = RoutineEditorViewModel(SavedStateHandle(mapOf("routineId" to 0L)), routineRepository, routineItemRepository)

        viewModel.onAddItem(RoutineItemDraft(title = "Ducharse"))
        viewModel.onAddItem(RoutineItemDraft(title = "Desayunar"))
        assertEquals(2, viewModel.uiState.value.items.size)

        viewModel.onRemoveItem(0)
        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals("Desayunar", viewModel.uiState.value.items.single().title)
    }

    @Test
    fun `guardar una rutina nueva crea la rutina y sus items`() = runTest(dispatcher) {
        coEvery { routineRepository.create(any()) } returns 5L
        val capturadoItem = slot<RoutineItemEntity>()
        coEvery { routineItemRepository.create(capture(capturadoItem)) } returns 1L
        val viewModel = RoutineEditorViewModel(SavedStateHandle(mapOf("routineId" to 0L)), routineRepository, routineItemRepository)
        viewModel.onNameChange("Rutina de mañana")
        viewModel.onStartTimeChange(LocalTime.of(7, 0))
        viewModel.onAddItem(RoutineItemDraft(title = "Ducharse", time = LocalTime.of(7, 30), durationMinutes = 15))

        var guardado = false
        viewModel.save(onSaved = { guardado = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(guardado)
        coVerify { routineRepository.create(any()) }
        assertEquals(5L, capturadoItem.captured.routineId)
        assertEquals(30, capturadoItem.captured.offsetMinutes) // 07:30 - 07:00
    }

    @Test
    fun `guardar borra los items que se quitaron en el editor`() = runTest(dispatcher) {
        val rutina = RoutineEntity(id = 1L, name = "Rutina", startMinutes = 420)
        val itemQueSeQuita = RoutineItemEntity(id = 7L, routineId = 1L, title = "Ducharse", offsetMinutes = 0, durationMinutes = 15, iconKey = "task_default", colorKey = "blue", position = 0)
        val itemQueQueda = RoutineItemEntity(id = 8L, routineId = 1L, title = "Desayunar", offsetMinutes = 30, durationMinutes = 20, iconKey = "task_default", colorKey = "blue", position = 1)
        coEvery { routineRepository.getById(1L) } returns rutina
        coEvery { routineItemRepository.getByRoutine(1L) } returns listOf(itemQueSeQuita, itemQueQueda)
        coEvery { routineRepository.update(any()) } returns Unit
        coEvery { routineItemRepository.update(any()) } returns Unit
        coEvery { routineItemRepository.delete(7L) } returns Unit
        val viewModel = RoutineEditorViewModel(SavedStateHandle(mapOf("routineId" to 1L)), routineRepository, routineItemRepository)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onRemoveItem(0) // quita "Ducharse", queda "Desayunar"
        viewModel.save(onSaved = {})
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { routineItemRepository.delete(7L) }
        coVerify(exactly = 0) { routineItemRepository.create(any()) }
    }

    private fun assertFalseIsValid(viewModel: RoutineEditorViewModel) {
        assertTrue(!viewModel.uiState.value.isValid)
    }
}
