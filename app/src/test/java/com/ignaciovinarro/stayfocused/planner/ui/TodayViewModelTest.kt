package com.ignaciovinarro.stayfocused.planner.ui

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.core.timeline.TimelineBlock
import com.ignaciovinarro.stayfocused.core.timeline.TimelineRepository
import com.ignaciovinarro.stayfocused.planner.data.REMINDER_OWNER_TASK
import com.ignaciovinarro.stayfocused.planner.data.ReminderRepository
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskRepository
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
    private val subtaskRepository = mockk<SubtaskRepository>(relaxed = true)
    private val reminderRepository = mockk<ReminderRepository>(relaxed = true)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { taskRepository.observeByDate(any()) } returns flowOf(emptyList())
        // Un Flow de un mock relajado no emite nunca: hay que dar uno que emita para que el combine avance.
        every { subtaskRepository.observeAll() } returns flowOf(emptyList())
        every { subtaskRepository.observeCompletions(any()) } returns flowOf(emptyList())
        every { taskRepository.observeOverdue(any()) } returns flowOf(emptyList())
    }

    private fun crearViewModel() =
        TodayViewModel(
            timelineRepository, taskRepository, taskOccurrenceRepository, subtaskRepository, reminderRepository
        )

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
        val viewModel = crearViewModel()

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
        val viewModel = crearViewModel()

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
        val viewModel = crearViewModel()

        viewModel.onToggleComplete(bloque(route = "planner/task/1", completed = true))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(capturada.captured.completedAt)
    }

    @Test
    fun `onToggleComplete ignora bloques que no son de Planificacion`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = crearViewModel()

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
        val viewModel = crearViewModel()

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
        val viewModel = crearViewModel()

        viewModel.onToggleComplete(bloque(route = "planner/task/1", completed = true, date = hoy))
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(capturada.captured.completedAt)
    }

    @Test
    fun `onMoveTask ignora ocurrencias de tareas recurrentes (no movable)`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = crearViewModel()

        viewModel.onMoveTask(bloque(route = "planner/task/1", movable = false), 600)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.getById(any()) }
    }

    @Test
    fun `onResizeTask ignora ocurrencias de tareas recurrentes (no movable)`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = crearViewModel()

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
        val viewModel = crearViewModel()

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
        val viewModel = crearViewModel()

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
        val viewModel = crearViewModel()

        viewModel.onMoveTask(bloque(route = "planner/task/1"), 1438)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1410, capturada.captured.startMinutes) // 1440 - 30 (duración), enganchado a 5
    }

    @Test
    fun `onMoveTask ignora bloques que no son de Planificacion`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = crearViewModel()

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
        val viewModel = crearViewModel()

        viewModel.onResizeTask(bloque(route = "planner/task/1"), 2)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(5, capturada.captured.durationMinutes)
    }

    @Test
    fun `onResizeTask ignora bloques que no son de Planificacion`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = crearViewModel()

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

    // --- Subtareas y progreso del dia (PL-05, ticket 0016) ---

    private fun bloqueConPartes(
        route: String = "planner/task/1",
        completed: Boolean = false,
        partsDone: Int = 0,
        partsTotal: Int = 0,
        source: String = TIMELINE_SOURCE_PLANNER
    ) = bloque(route = route, completed = completed, source = source).copy(partsDone = partsDone, partsTotal = partsTotal)

    private fun tareaSimpleHoy(id: Long = 1, title: String = "Tarea", startMinutes: Int? = 9 * 60, completedAt: Long? = null) =
        TaskEntity(
            id = id, title = title, date = LocalDate.now().toEpochDay(), startMinutes = startMinutes,
            durationMinutes = 30, iconKey = "task_default", colorKey = "blue", completedAt = completedAt
        )

    @Test
    fun `el progreso suma tareas y subtareas de los bloques de Planificacion`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(
            listOf(
                bloqueConPartes(route = "planner/task/1", completed = true, partsDone = 1, partsTotal = 2),
                bloqueConPartes(route = "planner/task/2", completed = false)
            )
        )
        val viewModel = crearViewModel()

        viewModel.uiState.test {
            skipItems(1)
            val progress = awaitItem().progress
            // Tareas: 1 de 2. Subtareas: 1 de 2. Total: 2 de 4.
            assertEquals(2, progress.done)
            assertEquals(4, progress.total)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `el progreso no cuenta los bloques de otros modulos`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(
            listOf(
                bloqueConPartes(route = "planner/task/1"),
                bloqueConPartes(route = "gym/session/1", source = "gym", completed = true)
            )
        )
        val viewModel = crearViewModel()

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(1, awaitItem().progress.total)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `el progreso cuenta las tareas de la bandeja y sus subtareas`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        every { taskRepository.observeByDate(any()) } returns flowOf(listOf(tareaSimpleHoy(id = 5, startMinutes = null)))
        every { subtaskRepository.observeAll() } returns flowOf(
            listOf(
                SubtaskEntity(id = 1, taskId = 5, title = "A", done = true, position = 0),
                SubtaskEntity(id = 2, taskId = 5, title = "B", done = false, position = 1)
            )
        )
        val viewModel = crearViewModel()

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem()
            // Tarea de bandeja sin completar (0/1) + subtareas 1/2.
            assertEquals(1, state.progress.done)
            assertEquals(3, state.progress.total)
            assertEquals(PartsCount(done = 1, total = 2), state.inboxParts[5L])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sin tareas el progreso es cero de cero`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = crearViewModel()

        viewModel.uiState.test {
            // El estado calculado es igual al inicial, asi que el StateFlow no emite dos veces.
            assertEquals(0, awaitItem().progress.total)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `abrir las subtareas de un bloque arma la hoja con sus items`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(listOf(bloqueConPartes(partsTotal = 2)))
        coEvery { taskRepository.getById(1L) } returns tareaSimpleHoy(id = 1)
        every { subtaskRepository.observeAll() } returns flowOf(
            listOf(
                SubtaskEntity(id = 10, taskId = 1, title = "Pan", done = true, position = 0),
                SubtaskEntity(id = 11, taskId = 1, title = "Leche", done = false, position = 1)
            )
        )
        val viewModel = crearViewModel()

        viewModel.uiState.test {
            skipItems(1)
            awaitItem()
            viewModel.onOpenSubtasks(bloqueConPartes(route = "planner/task/1"))
            dispatcher.scheduler.runCurrent()
            val sheet = expectMostRecentItem().subtaskSheet
            assertEquals(listOf("Pan", "Leche"), sheet?.items?.map { it.title })
            assertEquals(listOf(true, false), sheet?.items?.map { it.done })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cerrar la hoja la saca del estado`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(listOf(bloqueConPartes(partsTotal = 1)))
        coEvery { taskRepository.getById(1L) } returns tareaSimpleHoy(id = 1)
        every { subtaskRepository.observeAll() } returns flowOf(
            listOf(SubtaskEntity(id = 10, taskId = 1, title = "Pan", position = 0))
        )
        val viewModel = crearViewModel()

        viewModel.uiState.test {
            skipItems(1)
            awaitItem()
            viewModel.onOpenSubtasks(bloqueConPartes(route = "planner/task/1"))
            dispatcher.scheduler.runCurrent()
            assertEquals(true, expectMostRecentItem().subtaskSheet != null)
            viewModel.onDismissSubtasks()
            dispatcher.scheduler.runCurrent()
            assertNull(expectMostRecentItem().subtaskSheet)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `abrir las subtareas de un bloque de otro modulo no hace nada`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val viewModel = crearViewModel()

        viewModel.onOpenSubtasks(bloqueConPartes(route = "gym/session/1", source = "gym"))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.getById(any()) }
    }

    @Test
    fun `tildar una subtarea de una tarea simple la guarda en la propia subtarea`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(listOf(bloqueConPartes(partsTotal = 1)))
        coEvery { taskRepository.getById(1L) } returns tareaSimpleHoy(id = 1)
        every { subtaskRepository.observeAll() } returns flowOf(
            listOf(SubtaskEntity(id = 10, taskId = 1, title = "Pan", position = 0))
        )
        val viewModel = crearViewModel()

        viewModel.uiState.test {
            skipItems(1)
            awaitItem()
            viewModel.onOpenSubtasks(bloqueConPartes(route = "planner/task/1"))
            dispatcher.scheduler.runCurrent()
            val item = expectMostRecentItem().subtaskSheet!!.items.single()
            viewModel.onToggleSubtask(item)
            dispatcher.scheduler.runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { subtaskRepository.setDone(10L, true) }
        coVerify(exactly = 0) { subtaskRepository.setDoneOn(any(), any(), any()) }
    }

    @Test
    fun `destildar una subtarea tildada manda done en falso`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(listOf(bloqueConPartes(partsTotal = 1)))
        coEvery { taskRepository.getById(1L) } returns tareaSimpleHoy(id = 1)
        every { subtaskRepository.observeAll() } returns flowOf(
            listOf(SubtaskEntity(id = 10, taskId = 1, title = "Pan", done = true, position = 0))
        )
        val viewModel = crearViewModel()

        viewModel.uiState.test {
            skipItems(1)
            awaitItem()
            viewModel.onOpenSubtasks(bloqueConPartes(route = "planner/task/1"))
            dispatcher.scheduler.runCurrent()
            viewModel.onToggleSubtask(expectMostRecentItem().subtaskSheet!!.items.single())
            dispatcher.scheduler.runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { subtaskRepository.setDone(10L, false) }
    }

    @Test
    fun `tildar una subtarea de una tarea recurrente la guarda solo para hoy`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(listOf(bloqueConPartes(partsTotal = 1)))
        coEvery { taskRepository.getById(1L) } returns tareaSimpleHoy(id = 1).copy(
            recurrenceType = RecurrenceType.DAILY.name
        )
        every { subtaskRepository.observeAll() } returns flowOf(
            listOf(SubtaskEntity(id = 10, taskId = 1, title = "Paso", position = 0))
        )
        val viewModel = crearViewModel()

        viewModel.uiState.test {
            skipItems(1)
            awaitItem()
            viewModel.onOpenSubtasks(bloqueConPartes(route = "planner/task/1"))
            dispatcher.scheduler.runCurrent()
            viewModel.onToggleSubtask(expectMostRecentItem().subtaskSheet!!.items.single())
            dispatcher.scheduler.runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { subtaskRepository.setDoneOn(10L, LocalDate.now().toEpochDay(), true) }
        coVerify(exactly = 0) { subtaskRepository.setDone(any(), any()) }
    }

    @Test
    fun `tildar una subtarea no cambia el estado de la tarea`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(listOf(bloqueConPartes(partsTotal = 1)))
        coEvery { taskRepository.getById(1L) } returns tareaSimpleHoy(id = 1)
        every { subtaskRepository.observeAll() } returns flowOf(
            listOf(SubtaskEntity(id = 10, taskId = 1, title = "Pan", position = 0))
        )
        val viewModel = crearViewModel()

        viewModel.uiState.test {
            skipItems(1)
            awaitItem()
            viewModel.onOpenSubtasks(bloqueConPartes(route = "planner/task/1"))
            dispatcher.scheduler.runCurrent()
            viewModel.onToggleSubtask(expectMostRecentItem().subtaskSheet!!.items.single())
            dispatcher.scheduler.runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { taskRepository.update(any()) }
    }

    // --- Tareas atrasadas (PL-14, ticket 0017) ---

    private val hoyFecha = LocalDate.now()

    private fun tareaAtrasada(id: Long = 1, startMinutes: Int? = 9 * 60) = TaskEntity(
        id = id, title = "Atrasada", date = hoyFecha.minusDays(2).toEpochDay(), startMinutes = startMinutes,
        durationMinutes = 30, iconKey = "task_default", colorKey = "blue"
    )

    @Test
    fun `las tareas atrasadas del repositorio salen en el estado`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        every { taskRepository.observeOverdue(hoyFecha.toEpochDay()) } returns flowOf(listOf(tareaAtrasada()))
        val viewModel = crearViewModel()

        viewModel.uiState.test {
            awaitItem() // estado inicial
            assertEquals(listOf("Atrasada"), awaitItem().overdueTasks.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reprogramar una atrasada cambia el dia y conserva la hora`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        val atrasada = tareaAtrasada(startMinutes = 14 * 60)
        coEvery { taskRepository.getById(1L) } returns atrasada
        val actualizada = slot<TaskEntity>()
        coEvery { taskRepository.update(capture(actualizada)) } returns Unit
        val viewModel = crearViewModel()

        viewModel.onRescheduleOverdue(atrasada, hoyFecha.plusDays(1))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(hoyFecha.plusDays(1).toEpochDay(), actualizada.captured.date)
        assertEquals(14 * 60, actualizada.captured.startMinutes)
    }

    @Test
    fun `reprogramar a una fecha pasada no cambia nada`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        coEvery { taskRepository.getById(1L) } returns tareaAtrasada()
        val viewModel = crearViewModel()

        viewModel.onRescheduleOverdue(tareaAtrasada(), hoyFecha.minusDays(1))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.update(any()) }
    }

    @Test
    fun `reprogramar usa la tarea fresca de la base y no actua si ya no esta atrasada`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        // La pantalla tenia la tarea como atrasada, pero en la base ya se completo.
        coEvery { taskRepository.getById(1L) } returns tareaAtrasada().copy(completedAt = 5L)
        val viewModel = crearViewModel()

        viewModel.onRescheduleOverdue(tareaAtrasada(), hoyFecha.plusDays(1))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.update(any()) }
    }

    @Test
    fun `reprogramar una tarea que ya no existe no hace nada`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        coEvery { taskRepository.getById(1L) } returns null
        val viewModel = crearViewModel()

        viewModel.onRescheduleOverdue(tareaAtrasada(), hoyFecha.plusDays(1))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.update(any()) }
    }

    @Test
    fun `llevar a la bandeja pone la fecha de hoy sin hora y borra los recordatorios`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        coEvery { taskRepository.getById(1L) } returns tareaAtrasada()
        val actualizada = slot<TaskEntity>()
        coEvery { taskRepository.update(capture(actualizada)) } returns Unit
        val viewModel = crearViewModel()

        viewModel.onMoveOverdueToInbox(tareaAtrasada())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(hoyFecha.toEpochDay(), actualizada.captured.date)
        assertNull(actualizada.captured.startMinutes)
        coVerify { reminderRepository.deleteByOwner(REMINDER_OWNER_TASK, 1L) }
    }

    @Test
    fun `llevar a la bandeja una tarea que ya no esta atrasada no toca nada`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        coEvery { taskRepository.getById(1L) } returns tareaAtrasada().copy(date = hoyFecha.toEpochDay())
        val viewModel = crearViewModel()

        viewModel.onMoveOverdueToInbox(tareaAtrasada())
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { taskRepository.update(any()) }
        coVerify(exactly = 0) { reminderRepository.deleteByOwner(any(), any()) }
    }

    @Test
    fun `reprogramar no borra los recordatorios`() = runTest(dispatcher) {
        every { timelineRepository.blocksFor(any()) } returns flowOf(emptyList())
        coEvery { taskRepository.getById(1L) } returns tareaAtrasada()
        coEvery { taskRepository.update(any()) } returns Unit
        val viewModel = crearViewModel()

        viewModel.onRescheduleOverdue(tareaAtrasada(), hoyFecha)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { reminderRepository.deleteByOwner(any(), any()) }
    }
}
