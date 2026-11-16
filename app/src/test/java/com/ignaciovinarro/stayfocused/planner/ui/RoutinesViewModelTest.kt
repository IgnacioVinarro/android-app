package com.ignaciovinarro.stayfocused.planner.ui

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemRepository
import com.ignaciovinarro.stayfocused.planner.data.RoutineRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesViewModelTest {

    private val routineRepository = mockk<RoutineRepository>()
    private val routineItemRepository = mockk<RoutineItemRepository>()
    private val taskRepository = mockk<TaskRepository>()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): RoutinesViewModel {
        every { routineRepository.observeAll() } returns flowOf(emptyList())
        return RoutinesViewModel(routineRepository, routineItemRepository, taskRepository)
    }

    @Test
    fun `arma el estado con las rutinas del repositorio`() = runTest(dispatcher) {
        val rutina = RoutineEntity(id = 1L, name = "Rutina de mañana", startMinutes = 420)
        every { routineRepository.observeAll() } returns flowOf(listOf(rutina))
        val viewModel = RoutinesViewModel(routineRepository, routineItemRepository, taskRepository)

        viewModel.routines.test {
            awaitItem() // estado inicial
            assertEquals(listOf(rutina), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onApplyToday crea una tarea por cada item de la rutina`() = runTest(dispatcher) {
        val rutina = RoutineEntity(id = 1L, name = "Rutina de mañana", startMinutes = 420)
        val items = listOf(
            RoutineItemEntity(id = 1L, routineId = 1L, title = "Ducharse", offsetMinutes = 0, durationMinutes = 15, iconKey = "task_default", colorKey = "blue", position = 0),
            RoutineItemEntity(id = 2L, routineId = 1L, title = "Desayunar", offsetMinutes = 20, durationMinutes = 20, iconKey = "task_default", colorKey = "blue", position = 1)
        )
        coEvery { routineItemRepository.getByRoutine(1L) } returns items
        val capturadas = mutableListOf<TaskEntity>()
        coEvery { taskRepository.create(capture(capturadas)) } returns 1L
        val viewModel = viewModel()

        viewModel.onApplyToday(rutina)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, capturadas.size)
        assertEquals(listOf(420, 440), capturadas.map { it.startMinutes })
        assert(capturadas.all { it.routineId == 1L })
    }

    @Test
    fun `onDelete borra la rutina en el repositorio`() = runTest(dispatcher) {
        coEvery { routineRepository.delete(any()) } returns Unit
        val viewModel = viewModel()

        viewModel.onDelete(RoutineEntity(id = 1L, name = "Rutina", startMinutes = 420))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { routineRepository.delete(1L) }
    }
}
