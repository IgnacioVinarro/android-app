package com.ignaciovinarro.stayfocused.planner.ui

import com.ignaciovinarro.stayfocused.core.settings.SettingsRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import com.ignaciovinarro.stayfocused.planner.domain.RecurrenceType
import com.ignaciovinarro.stayfocused.planner.domain.dayOfWeekBit
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

    @Test
    fun `sin recurrencia activada no guarda ningun campo de recurrencia`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.create(capture(capturada)) } returns 1L

        viewModel.onTitleChange("Tarea simple")
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(capturada.captured.recurrenceType)
        assertNull(capturada.captured.recurrenceInterval)
        assertNull(capturada.captured.recurrenceDays)
        assertNull(capturada.captured.recurrenceEnd)
    }

    @Test
    fun `activar recurrencia fuerza la hora de inicio`() = runTest(dispatcher) {
        val viewModel = crearViewModel()

        viewModel.onHasStartTimeChange(false)
        viewModel.onRecurrenceEnabledChange(true)

        assert(viewModel.uiState.value.hasStartTime)
    }

    @Test
    fun `apagar la hora de inicio apaga la recurrencia`() = runTest(dispatcher) {
        val viewModel = crearViewModel()

        viewModel.onRecurrenceEnabledChange(true)
        viewModel.onHasStartTimeChange(false)

        assert(!viewModel.uiState.value.recurrenceEnabled)
    }

    @Test
    fun `recurrencia diaria guarda solo el tipo`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.create(capture(capturada)) } returns 1L

        viewModel.onTitleChange("Tomar agua")
        viewModel.onRecurrenceEnabledChange(true)
        viewModel.onRecurrenceTypeChange(RecurrenceType.DAILY)
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RecurrenceType.DAILY.name, capturada.captured.recurrenceType)
        assertNull(capturada.captured.recurrenceInterval)
        assertNull(capturada.captured.recurrenceDays)
    }

    @Test
    fun `recurrencia de dias especificos guarda la mascara de dias`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.create(capture(capturada)) } returns 1L

        viewModel.onTitleChange("Gimnasio")
        viewModel.onRecurrenceEnabledChange(true)
        viewModel.onRecurrenceTypeChange(RecurrenceType.SPECIFIC_DAYS)
        viewModel.onRecurrenceDayToggle(java.time.DayOfWeek.MONDAY)
        viewModel.onRecurrenceDayToggle(java.time.DayOfWeek.WEDNESDAY)
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()

        val mascaraEsperada = dayOfWeekBit(java.time.DayOfWeek.MONDAY) or dayOfWeekBit(java.time.DayOfWeek.WEDNESDAY)
        assertEquals(mascaraEsperada, capturada.captured.recurrenceDays)
    }

    @Test
    fun `recurrencia cada n dias guarda el intervalo`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.create(capture(capturada)) } returns 1L

        viewModel.onTitleChange("Regar plantas")
        viewModel.onRecurrenceEnabledChange(true)
        viewModel.onRecurrenceTypeChange(RecurrenceType.EVERY_N_DAYS)
        viewModel.onRecurrenceIntervalChange(3)
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, capturada.captured.recurrenceInterval)
    }

    @Test
    fun `fecha de fin de recurrencia solo se guarda si esta activada`() = runTest(dispatcher) {
        val viewModel = crearViewModel()
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.create(capture(capturada)) } returns 1L

        viewModel.onTitleChange("Tomar agua")
        viewModel.onRecurrenceEnabledChange(true)
        viewModel.onRecurrenceTypeChange(RecurrenceType.DAILY)
        viewModel.onHasRecurrenceEndChange(true)
        viewModel.onRecurrenceEndChange(LocalDate.of(2026, 4, 1))
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(LocalDate.of(2026, 4, 1).toEpochDay(), capturada.captured.recurrenceEnd)
    }

    @Test
    fun `dias especificos sin ningun dia marcado no es valido`() = runTest(dispatcher) {
        val viewModel = crearViewModel()

        viewModel.onTitleChange("Gimnasio")
        viewModel.onRecurrenceEnabledChange(true)
        viewModel.onRecurrenceTypeChange(RecurrenceType.SPECIFIC_DAYS)

        assert(!viewModel.uiState.value.isValid)
    }
}
