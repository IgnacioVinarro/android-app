package com.ignaciovinarro.stayfocused.planner.ui

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.core.timeline.TimelineBlock
import com.ignaciovinarro.stayfocused.core.timeline.TimelineRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
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
        completed: Boolean = false
    ) = TimelineBlock(
        id = id,
        source = source,
        date = LocalDate.now(),
        startMinutes = startMinutes,
        durationMinutes = 30,
        title = "Tarea",
        iconKey = "task_default",
        colorKey = "blue",
        completed = completed,
        route = route
    )

    @Test
    fun `arma el estado con los bloques posicionados`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(listOf(bloque()))
        val viewModel = TodayViewModel(timelineRepository, taskRepository)

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
        val viewModel = TodayViewModel(timelineRepository, taskRepository)

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
        val viewModel = TodayViewModel(timelineRepository, taskRepository)

        viewModel.onToggleComplete(bloque(route = "planner/task/1", completed = true))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(capturada.captured.completedAt)
    }

    @Test
    fun `onToggleComplete ignora bloques que no son de Planificacion`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = TodayViewModel(timelineRepository, taskRepository)

        viewModel.onToggleComplete(bloque(source = "gym", route = "gym/session/1"))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.getById(any()) }
    }

    @Test
    fun `arma la bandeja con las tareas sin hora e ignora las que ya tienen`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val sinHora = tarea(id = 1L, completedAt = null, startMinutes = null)
        val conHora = tarea(id = 2L, completedAt = null, startMinutes = 9 * 60)
        every { taskRepository.observeByDate(any()) } returns flowOf(listOf(sinHora, conHora))
        val viewModel = TodayViewModel(timelineRepository, taskRepository)

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
        val viewModel = TodayViewModel(timelineRepository, taskRepository)

        viewModel.onAssignTime(sinHora, LocalTime.of(14, 30))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(14 * 60 + 30, capturada.captured.startMinutes)
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
