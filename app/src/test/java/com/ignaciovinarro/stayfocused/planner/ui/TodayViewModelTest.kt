package com.ignaciovinarro.stayfocused.planner.ui

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.core.timeline.TimelineBlock
import com.ignaciovinarro.stayfocused.core.timeline.TimelineRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import com.ignaciovinarro.stayfocused.planner.domain.RecurrenceType
import com.ignaciovinarro.stayfocused.planner.domain.TIMELINE_SOURCE_PLANNER
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
class TodayViewModelTest {

    private val timelineRepository = mockk<TimelineRepository>()
    private val taskRepository = mockk<TaskRepository>()
    private val taskOccurrenceRepository = mockk<TaskOccurrenceRepository>()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { taskRepository.observeByDate(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun bloque(
        id: String = "a",
        source: String = TIMELINE_SOURCE_PLANNER,
        startMinutes: Int = 9 * 60,
        route: String = "planner/task/1",
        completed: Boolean = false,
        date: LocalDate = LocalDate.now(),
        movable: Boolean = true
    ) = TimelineBlock(
        id = id,
        source = source,
        date = date,
        startMinutes = startMinutes,
        durationMinutes = 30,
        title = "Tarea",
        iconKey = "task_default",
        colorKey = "blue",
        completed = completed,
        movable = movable,
        route = route
    )

    @Test
    fun `arma el estado con los bloques posicionados`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(listOf(bloque()))
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.uiState.test {
            awaitItem() // estado inicial, antes de que llegue el primer valor combinado
            val estado = awaitItem()
            assertEquals(1, estado.positionedBlocks.size)
            assertEquals("a", estado.positionedBlocks.first().block.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onToggleComplete marca una tarea no completada como completada`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val tarea = tarea(id = 1L, completedAt = null)
        coEvery { taskRepository.getById(1L) } returns tarea
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.update(capture(capturada)) } returns Unit
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onToggleComplete(bloque(route = "planner/task/1", completed = false))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { taskRepository.update(any()) }
        assert(capturada.captured.completedAt != null)
    }

    @Test
    fun `onToggleComplete descompleta una tarea ya completada`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val tarea = tarea(id = 1L, completedAt = 123L)
        coEvery { taskRepository.getById(1L) } returns tarea
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.update(capture(capturada)) } returns Unit
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onToggleComplete(bloque(route = "planner/task/1", completed = true))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(capturada.captured.completedAt)
    }

    @Test
    fun `onToggleComplete ignora bloques que no son de Planificacion`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onToggleComplete(bloque(source = "gym", route = "gym/session/1"))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.getById(any()) }
    }

    @Test
    fun `onToggleComplete de una tarea recurrente sin ocurrencia previa crea una completada`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val hoy = LocalDate.now()
        val tareaRecurrente = tarea(id = 1L, completedAt = null).copy(recurrenceType = RecurrenceType.DAILY.name)
        coEvery { taskRepository.getById(1L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(1L, hoy.toEpochDay()) } returns null
        val capturada = slot<TaskOccurrenceEntity>()
        coEvery { taskOccurrenceRepository.create(capture(capturada)) } returns 1L
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onToggleComplete(bloque(route = "planner/task/1", completed = false, date = hoy))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(hoy.toEpochDay(), capturada.captured.date)
        assert(capturada.captured.completedAt != null)
        coVerify(exactly = 0) { taskRepository.update(any()) } // la tarea en sí no se toca
    }

    @Test
    fun `onToggleComplete de una tarea recurrente con ocurrencia ya completada la descompleta`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val hoy = LocalDate.now()
        val tareaRecurrente = tarea(id = 1L, completedAt = null).copy(recurrenceType = RecurrenceType.DAILY.name)
        val ocurrenciaExistente = TaskOccurrenceEntity(id = 9L, taskId = 1L, date = hoy.toEpochDay(), completedAt = 123L)
        coEvery { taskRepository.getById(1L) } returns tareaRecurrente
        coEvery { taskOccurrenceRepository.getByTaskAndDate(1L, hoy.toEpochDay()) } returns ocurrenciaExistente
        val capturada = slot<TaskOccurrenceEntity>()
        coEvery { taskOccurrenceRepository.update(capture(capturada)) } returns Unit
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onToggleComplete(bloque(route = "planner/task/1", completed = true, date = hoy))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(capturada.captured.completedAt)
    }

    @Test
    fun `onMoveTask ignora ocurrencias de tareas recurrentes (no movable)`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onMoveTask(bloque(route = "planner/task/1", movable = false), 600)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.getById(any()) }
    }

    @Test
    fun `onResizeTask ignora ocurrencias de tareas recurrentes (no movable)`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onResizeTask(bloque(route = "planner/task/1", movable = false), 60)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.getById(any()) }
    }

    @Test
    fun `arma la bandeja con las tareas sin hora e ignora las que ya tienen`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val sinHora = tarea(id = 1L, completedAt = null, startMinutes = null)
        val conHora = tarea(id = 2L, completedAt = null, startMinutes = 9 * 60)
        every { taskRepository.observeByDate(any()) } returns flowOf(listOf(sinHora, conHora))
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.uiState.test {
            awaitItem()
            val estado = awaitItem()
            assertEquals(listOf(1L), estado.inboxTasks.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAssignTime le pone hora a una tarea de la bandeja`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val sinHora = tarea(id = 1L, completedAt = null, startMinutes = null)
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.update(capture(capturada)) } returns Unit
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onAssignTime(sinHora, LocalTime.of(14, 30))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(14 * 60 + 30, capturada.captured.startMinutes)
    }

    @Test
    fun `onMoveTask engancha al intervalo de 5 minutos y respeta los limites del dia`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val tarea = tarea(id = 1L, completedAt = null, startMinutes = 9 * 60)
        coEvery { taskRepository.getById(1L) } returns tarea
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.update(capture(capturada)) } returns Unit
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onMoveTask(bloque(route = "planner/task/1"), 1438)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1410, capturada.captured.startMinutes) // 1440 - 30 (duración), enganchado a 5
    }

    @Test
    fun `onMoveTask ignora bloques que no son de Planificacion`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onMoveTask(bloque(source = "gym", route = "gym/session/1"), 600)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.getById(any()) }
    }

    @Test
    fun `onResizeTask engancha la duracion y respeta la duracion minima`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val tarea = tarea(id = 1L, completedAt = null, startMinutes = 9 * 60)
        coEvery { taskRepository.getById(1L) } returns tarea
        val capturada = slot<TaskEntity>()
        coEvery { taskRepository.update(capture(capturada)) } returns Unit
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onResizeTask(bloque(route = "planner/task/1"), 2)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(5, capturada.captured.durationMinutes)
    }

    @Test
    fun `onResizeTask ignora bloques que no son de Planificacion`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = TodayViewModel(timelineRepository, taskRepository, taskOccurrenceRepository)

        viewModel.onResizeTask(bloque(source = "gym", route = "gym/session/1"), 60)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.getById(any()) }
    }

    private fun tarea(id: Long, completedAt: Long?, startMinutes: Int? = 9 * 60) = TaskEntity(
        id = id,
        title = "Tarea",
        date = LocalDate.now().toEpochDay(),
        startMinutes = startMinutes,
        durationMinutes = 30,
        iconKey = "task_default",
        colorKey = "blue",
        completedAt = completedAt
    )
}
