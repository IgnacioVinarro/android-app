package com.ignaciovinarro.stayfocused.planner.ui

import com.ignaciovinarro.stayfocused.core.settings.SettingsRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateTaskViewModelTest {

    private val taskRepository = mockk<TaskRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { settingsRepository.defaultTaskDuration } returns flowOf(45)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(): CreateTaskViewModel {
        val viewModel = CreateTaskViewModel(taskRepository, settingsRepository)
        dispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `toma la duracion por defecto de Ajustes al iniciar`() = runTest(dispatcher) {
        val viewModel = crearViewModel()

        assertEquals(45, viewModel.uiState.value.durationMinutes)
    }

    @Test
    fun `no guarda si el titulo esta vacio`() = runTest(dispatcher) {
        val viewModel = crearViewModel()

        var guardado = false
        viewModel.save(onSaved = { guardado = true })
        dispatcher.scheduler.advanceUntilIdle()

        assert(!guardado)
        coVerify(exactly = 0) { taskRepository.create(any()) }
    }

    @Test
    fun `guarda la tarea con los datos cargados y notifica al terminar`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.create(capture(capturada)) } returns 1L

        viewModel.onTitleChange("Comprar pan")
        viewModel.onDateChange(LocalDate.of(2026, 3, 5))
        viewModel.onStartTimeChange(LocalTime.of(9, 30))
        viewModel.onDurationChange(20)
        viewModel.onIconChange("task_home")
        viewModel.onColorChange("green")
        viewModel.onNotesChange("Integral")

        var guardado = false
        viewModel.save(onSaved = { guardado = true })
        dispatcher.scheduler.advanceUntilIdle()

        assert(guardado)
        val tarea = capturada.captured
        assertEquals("Comprar pan", tarea.title)
        assertEquals(LocalDate.of(2026, 3, 5).toEpochDay(), tarea.date)
        assertEquals(9 * 60 + 30, tarea.startMinutes)
        assertEquals(20, tarea.durationMinutes)
        assertEquals("task_home", tarea.iconKey)
        assertEquals("green", tarea.colorKey)
        assertEquals("Integral", tarea.notes)
    }

    @Test
    fun `sin hora activado guarda startMinutes nulo`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.create(capture(capturada)) } returns 1L

        viewModel.onTitleChange("Leer un libro")
        viewModel.onHasStartTimeChange(false)
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(capturada.captured.startMinutes)
    }

    @Test
    fun `notas vacias se guardan como null`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.create(capture(capturada)) } returns 1L

        viewModel.onTitleChange("Tarea sin notas")
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(capturada.captured.notes)
    }
}
