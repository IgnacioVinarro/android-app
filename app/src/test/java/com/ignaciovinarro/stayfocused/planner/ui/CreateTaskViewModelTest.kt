package com.ignaciovinarro.stayfocused.planner.ui

import androidx.lifecycle.SavedStateHandle
import com.ignaciovinarro.stayfocused.core.settings.SettingsRepository
import com.ignaciovinarro.stayfocused.planner.data.REMINDER_OWNER_TASK
import com.ignaciovinarro.stayfocused.planner.data.ReminderEntity
import com.ignaciovinarro.stayfocused.planner.data.ReminderRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import com.ignaciovinarro.stayfocused.planner.domain.EditScope
import com.ignaciovinarro.stayfocused.planner.domain.MAX_REMINDER_MINUTES_BEFORE
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateTaskViewModelTest {

    private val taskRepository = mockk<TaskRepository>()
    private val taskOccurrenceRepository = mockk<TaskOccurrenceRepository>()
    private val reminderRepository = mockk<ReminderRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { settingsRepository.defaultTaskDuration } returns flowOf(45)
        coEvery { reminderRepository.getByOwner(any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModelNueva(): CreateTaskViewModel {
        val viewModel = CreateTaskViewModel(
            SavedStateHandle(mapOf("taskId" to 0L)),
            taskRepository,
            taskOccurrenceRepository,
            reminderRepository,
            settingsRepository
        )
        dispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    private fun crearViewModelEditando(taskId: Long, occurrenceDate: Long = -1L): CreateTaskViewModel {
        val viewModel = CreateTaskViewModel(
            SavedStateHandle(mapOf("taskId" to taskId, "date" to occurrenceDate)),
            taskRepository,
            taskOccurrenceRepository,
            reminderRepository,
            settingsRepository
        )
        dispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    // --- Crear tarea (comportamiento ya existente, sin cambios de alcance) ---

    @Test
    fun `toma la duracion por defecto de Ajustes al iniciar`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()

        assertEquals(45, viewModel.uiState.value.durationMinutes)
    }

    @Test
    fun `no guarda si el titulo esta vacio`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()

        var guardado = false
        viewModel.save(onSaved = { guardado = true })
        dispatcher.scheduler.advanceUntilIdle()

        assert(!guardado)
        coVerify(exactly = 0) { taskRepository.create(any()) }
    }

    @Test
    fun `guarda la tarea con los datos cargados y notifica al terminar`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()
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
        val viewModel = crearViewModelNueva()
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
        val viewModel = crearViewModelNueva()
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.create(capture(capturada)) } returns 1L

        viewModel.onTitleChange("Tarea sin notas")
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(capturada.captured.notes)
    }

    @Test
    fun `sin recurrencia activada no guarda ningun campo de recurrencia`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()
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
        val viewModel = crearViewModelNueva()

        viewModel.onHasStartTimeChange(false)
        viewModel.onRecurrenceEnabledChange(true)

        assert(viewModel.uiState.value.hasStartTime)
    }

    @Test
    fun `apagar la hora de inicio apaga la recurrencia`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()

        viewModel.onRecurrenceEnabledChange(true)
        viewModel.onHasStartTimeChange(false)

        assert(!viewModel.uiState.value.recurrenceEnabled)
    }

    @Test
    fun `recurrencia diaria guarda solo el tipo`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()
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
        val viewModel = crearViewModelNueva()
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
        val viewModel = crearViewModelNueva()
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
        val viewModel = crearViewModelNueva()
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
        val viewModel = crearViewModelNueva()

        viewModel.onTitleChange("Gimnasio")
        viewModel.onRecurrenceEnabledChange(true)
        viewModel.onRecurrenceTypeChange(RecurrenceType.SPECIFIC_DAYS)

        assert(!viewModel.uiState.value.isValid)
    }

    // --- Editar/borrar una tarea existente (PL-15) ---

    private val tareaSimple = TaskEntity(
        id = 10L,
        title = "Tarea simple",
        notes = "nota",
        date = LocalDate.of(2026, 5, 1).toEpochDay(),
        startMinutes = 8 * 60,
        durationMinutes = 30,
        iconKey = "task_default",
        colorKey = "blue"
    )

    private val tareaRecurrente = TaskEntity(
        id = 20L,
        title = "Gimnasio",
        date = LocalDate.of(2026, 1, 5).toEpochDay(), // lunes
        startMinutes = 7 * 60,
        durationMinutes = 60,
        iconKey = "task_gym",
        colorKey = "blue",
        recurrenceType = RecurrenceType.WEEKLY.name
    )

    @Test
    fun `cargar una tarea simple precarga el formulario`() = runTest(dispatcher) {
        coEvery { taskRepository.getById(10L) } returns tareaSimple

        val viewModel = crearViewModelEditando(10L)

        val estado = viewModel.uiState.value
        assertEquals(10L, estado.taskId)
        assertEquals("Tarea simple", estado.title)
        assertEquals("nota", estado.notes)
        assertEquals(30, estado.durationMinutes)
        assert(!estado.isRecurringTask)
    }

    @Test
    fun `editar una tarea simple actualiza directamente sin pedir alcance`() = runTest(dispatcher) {
        coEvery { taskRepository.getById(10L) } returns tareaSimple
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.update(capture(capturada)) } returns Unit
        val viewModel = crearViewModelEditando(10L)

        viewModel.onTitleChange("Tarea simple editada")
        var guardado = false
        viewModel.save(onSaved = { guardado = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(guardado)
        assertNull(viewModel.uiState.value.pendingScopeAction)
        assertEquals(10L, capturada.captured.id)
        assertEquals("Tarea simple editada", capturada.captured.title)
    }

    @Test
    fun `borrar una tarea simple borra directamente sin pedir alcance`() = runTest(dispatcher) {
        coEvery { taskRepository.getById(10L) } returns tareaSimple
        coEvery { taskRepository.delete(10L) } returns Unit
        val viewModel = crearViewModelEditando(10L)

        var borrado = false
        viewModel.delete(onDeleted = { borrado = true })
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(borrado)
        coVerify { taskRepository.delete(10L) }
    }

    @Test
    fun `cargar una ocurrencia con overrides precarga esos valores, no los de la tarea base`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19) // tercer lunes
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns TaskOccurrenceEntity(
            taskId = 20L,
            date = fecha.toEpochDay(),
            overrideTitle = "Gimnasio (pierna)",
            overrideDurationMinutes = 90
        )

        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        val estado = viewModel.uiState.value
        assertTrue(estado.isRecurringTask)
        assertEquals("Gimnasio (pierna)", estado.title)
        assertEquals(90, estado.durationMinutes)
    }

    @Test
    fun `editar una tarea recurrente pide alcance antes de guardar`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        viewModel.onTitleChange("Gimnasio editado")
        var guardado = false
        viewModel.save(onSaved = { guardado = true })
        dispatcher.scheduler.advanceUntilIdle()

        assert(!guardado)
        assertEquals(PendingScopeAction.SAVE, viewModel.uiState.value.pendingScopeAction)
        coVerify(exactly = 0) { taskRepository.update(any()) }
    }

    @Test
    fun `elegir todas al editar una tarea recurrente actualiza la tarea base y notifica`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.update(capture(capturada)) } returns Unit
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        viewModel.onTitleChange("Gimnasio editado")
        var guardado = false
        viewModel.save(onSaved = { guardado = true })
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onScopeChosen(EditScope.ALL)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(guardado)
        assertEquals(20L, capturada.captured.id)
        assertEquals("Gimnasio editado", capturada.captured.title)
    }

    @Test
    fun `elegir solo esta al editar crea un override de ocurrencia sin tocar la tarea base`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        val capturada = slot<TaskOccurrenceEntity>()
        coEvery { taskOccurrenceRepository.create(capture(capturada)) } returns 1L
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        viewModel.onTitleChange("Gimnasio solo hoy")
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onScopeChosen(EditScope.ONLY_THIS)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.update(any()) }
        assertEquals(20L, capturada.captured.taskId)
        assertEquals(fecha.toEpochDay(), capturada.captured.date)
        assertEquals("Gimnasio solo hoy", capturada.captured.overrideTitle)
    }

    @Test
    fun `elegir esta y siguientes recorta la original y crea una tarea nueva`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        val truncada = slot<TaskEntity>()
        val nueva = slot<TaskEntity>()
        coEvery { taskRepository.update(capture(truncada)) } returns Unit
        coEvery { taskRepository.create(capture(nueva)) } returns 99L
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        viewModel.onTitleChange("Gimnasio nuevo")
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onScopeChosen(EditScope.THIS_AND_FOLLOWING)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(20L, truncada.captured.id)
        assertEquals(LocalDate.of(2026, 1, 18).toEpochDay(), truncada.captured.recurrenceEnd)
        assertEquals(fecha.toEpochDay(), nueva.captured.date)
        assertEquals("Gimnasio nuevo", nueva.captured.title)
    }

    @Test
    fun `borrar una tarea recurrente pide alcance`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        var borrado = false
        viewModel.delete(onDeleted = { borrado = true })
        dispatcher.scheduler.advanceUntilIdle()

        assert(!borrado)
        assertEquals(PendingScopeAction.DELETE, viewModel.uiState.value.pendingScopeAction)
    }

    @Test
    fun `borrar solo esta ocurrencia la marca como salteada`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        val capturada = slot<TaskOccurrenceEntity>()
        coEvery { taskOccurrenceRepository.create(capture(capturada)) } returns 1L
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        viewModel.delete {}
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onScopeChosen(EditScope.ONLY_THIS)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(20L, capturada.captured.taskId)
        assertTrue(capturada.captured.skipped)
        coVerify(exactly = 0) { taskRepository.delete(any()) }
    }

    @Test
    fun `borrar todas una tarea recurrente borra la tarea base`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        coEvery { taskRepository.delete(20L) } returns Unit
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        viewModel.delete {}
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onScopeChosen(EditScope.ALL)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { taskRepository.delete(20L) }
    }

    @Test
    fun `cerrar el dialogo de alcance sin elegir no guarda ni borra nada`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        viewModel.delete {}
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onDismissScopeDialog()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingScopeAction)
        coVerify(exactly = 0) { taskRepository.delete(any()) }
    }

    // --- Recordatorios (PL-10, ticket 0012) ---

    @Test
    fun `prender recordatorios los deja ordenados y sin repetidos`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()

        viewModel.onReminderToggle(30)
        viewModel.onReminderToggle(0)
        viewModel.onReminderToggle(10)

        assertEquals(listOf(0, 10, 30), viewModel.uiState.value.reminderMinutes)
    }

    @Test
    fun `tocar dos veces un recordatorio lo apaga`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()

        viewModel.onReminderToggle(15)
        viewModel.onReminderToggle(15)

        assertEquals(emptyList<Int>(), viewModel.uiState.value.reminderMinutes)
    }

    @Test
    fun `un recordatorio fuera de rango se ignora`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()

        viewModel.onReminderToggle(-1)
        viewModel.onReminderToggle(MAX_REMINDER_MINUTES_BEFORE + 1)
        viewModel.onReminderToggle(MAX_REMINDER_MINUTES_BEFORE)

        assertEquals(listOf(MAX_REMINDER_MINUTES_BEFORE), viewModel.uiState.value.reminderMinutes)
    }

    @Test
    fun `crear una tarea guarda los recordatorios elegidos para el id nuevo`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()
        coEvery { taskRepository.create(any()) } returns 7L

        viewModel.onTitleChange("Reunion")
        viewModel.onReminderToggle(45) // minutos libres
        viewModel.onReminderToggle(0)
        viewModel.onReminderToggle(15)
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { reminderRepository.replaceForOwner(REMINDER_OWNER_TASK, 7L, listOf(0, 15, 45)) }
    }

    @Test
    fun `sin hora no guarda recordatorios aunque haya elegidos`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()
        coEvery { taskRepository.create(any()) } returns 7L

        viewModel.onTitleChange("Leer")
        viewModel.onReminderToggle(10)
        viewModel.onHasStartTimeChange(false)
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { reminderRepository.replaceForOwner(REMINDER_OWNER_TASK, 7L, emptyList()) }
    }

    @Test
    fun `apagar sin hora conserva la seleccion de recordatorios`() = runTest(dispatcher) {
        val viewModel = crearViewModelNueva()

        viewModel.onReminderToggle(10)
        viewModel.onHasStartTimeChange(false)
        viewModel.onHasStartTimeChange(true)

        assertEquals(listOf(10), viewModel.uiState.value.reminderMinutes)
    }

    @Test
    fun `cargar una tarea existente precarga sus recordatorios ordenados`() = runTest(dispatcher) {
        coEvery { taskRepository.getById(10L) } returns tareaSimple
        coEvery { reminderRepository.getByOwner(REMINDER_OWNER_TASK, 10L) } returns listOf(
            ReminderEntity(ownerId = 10L, minutesBefore = 30),
            ReminderEntity(ownerId = 10L, minutesBefore = 0)
        )

        val viewModel = crearViewModelEditando(10L)

        assertEquals(listOf(0, 30), viewModel.uiState.value.reminderMinutes)
    }

    @Test
    fun `editar una tarea simple reemplaza sus recordatorios`() = runTest(dispatcher) {
        coEvery { taskRepository.getById(10L) } returns tareaSimple
        coEvery { taskRepository.update(any()) } returns Unit
        coEvery { reminderRepository.getByOwner(REMINDER_OWNER_TASK, 10L) } returns listOf(
            ReminderEntity(ownerId = 10L, minutesBefore = 30)
        )
        val viewModel = crearViewModelEditando(10L)

        viewModel.onReminderToggle(30) // lo saca
        viewModel.onReminderToggle(5)
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { reminderRepository.replaceForOwner(REMINDER_OWNER_TASK, 10L, listOf(5)) }
    }

    @Test
    fun `editar una recurrente con todas reemplaza los recordatorios de la tarea`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        coEvery { taskRepository.update(any()) } returns Unit
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        viewModel.onReminderToggle(60)
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onScopeChosen(EditScope.ALL)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { reminderRepository.replaceForOwner(REMINDER_OWNER_TASK, 20L, listOf(60)) }
    }

    @Test
    fun `editar una recurrente con solo esta no toca los recordatorios`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        coEvery { taskOccurrenceRepository.create(any()) } returns 1L
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        viewModel.onReminderToggle(60)
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onScopeChosen(EditScope.ONLY_THIS)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { reminderRepository.replaceForOwner(any(), any(), any()) }
    }

    @Test
    fun `esta y siguientes deja los recordatorios en la original y los editados en la nueva`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        coEvery { taskRepository.update(any()) } returns Unit
        coEvery { taskRepository.create(any()) } returns 99L
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        viewModel.onReminderToggle(15)
        viewModel.save {}
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onScopeChosen(EditScope.THIS_AND_FOLLOWING)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { reminderRepository.replaceForOwner(REMINDER_OWNER_TASK, 99L, listOf(15)) }
        coVerify(exactly = 0) { reminderRepository.replaceForOwner(REMINDER_OWNER_TASK, 20L, any()) }
    }

    @Test
    fun `esta y siguientes desde la primera ocurrencia equivale a todas y reemplaza en la original`() =
        runTest(dispatcher) {
            val primera = LocalDate.ofEpochDay(tareaRecurrente.date)
            coEvery { taskRepository.getById(20L) } returns tareaRecurrente
            coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, primera.toEpochDay()) } returns null
            coEvery { taskRepository.update(any()) } returns Unit
            val viewModel = crearViewModelEditando(20L, primera.toEpochDay())

            viewModel.onReminderToggle(15)
            viewModel.save {}
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.onScopeChosen(EditScope.THIS_AND_FOLLOWING)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { reminderRepository.replaceForOwner(REMINDER_OWNER_TASK, 20L, listOf(15)) }
            coVerify(exactly = 0) { taskRepository.create(any()) }
        }

    @Test
    fun `borrar una tarea simple borra tambien sus recordatorios`() = runTest(dispatcher) {
        coEvery { taskRepository.getById(10L) } returns tareaSimple
        coEvery { taskRepository.delete(10L) } returns Unit
        val viewModel = crearViewModelEditando(10L)

        viewModel.delete {}
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { reminderRepository.deleteByOwner(REMINDER_OWNER_TASK, 10L) }
    }

    @Test
    fun `borrar todas una recurrente borra tambien sus recordatorios`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        coEvery { taskRepository.delete(20L) } returns Unit
        val viewModel = crearViewModelEditando(20L, fecha.toEpochDay())

        viewModel.delete {}
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onScopeChosen(EditScope.ALL)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { reminderRepository.deleteByOwner(REMINDER_OWNER_TASK, 20L) }
    }

    @Test
    fun `borrar solo esta o esta y siguientes (no la primera) conserva los recordatorios`() = runTest(dispatcher) {
        val fecha = LocalDate.of(2026, 1, 19)
        coEvery { taskRepository.getById(20L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, fecha.toEpochDay()) } returns null
        coEvery { taskOccurrenceRepository.create(any()) } returns 1L
        coEvery { taskRepository.update(any()) } returns Unit

        val soloEsta = crearViewModelEditando(20L, fecha.toEpochDay())
        soloEsta.delete {}
        dispatcher.scheduler.advanceUntilIdle()
        soloEsta.onScopeChosen(EditScope.ONLY_THIS)
        dispatcher.scheduler.advanceUntilIdle()

        val siguientes = crearViewModelEditando(20L, fecha.toEpochDay())
        siguientes.delete {}
        dispatcher.scheduler.advanceUntilIdle()
        siguientes.onScopeChosen(EditScope.THIS_AND_FOLLOWING)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { reminderRepository.deleteByOwner(any(), any()) }
    }

    @Test
    fun `borrar esta y siguientes desde la primera ocurrencia borra tambien los recordatorios`() =
        runTest(dispatcher) {
            val primera = LocalDate.ofEpochDay(tareaRecurrente.date)
            coEvery { taskRepository.getById(20L) } returns tareaRecurrente
            coEvery { taskOccurrenceRepository.getByTaskAndDate(20L, primera.toEpochDay()) } returns null
            coEvery { taskRepository.delete(20L) } returns Unit
            val viewModel = crearViewModelEditando(20L, primera.toEpochDay())

            viewModel.delete {}
            dispatcher.scheduler.advanceUntilIdle()
            viewModel.onScopeChosen(EditScope.THIS_AND_FOLLOWING)
            dispatcher.scheduler.advanceUntilIdle()

            coVerify { reminderRepository.deleteByOwner(REMINDER_OWNER_TASK, 20L) }
        }
}
