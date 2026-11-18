package com.ignaciovinarro.stayfocused.planner.ui

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.planner.data.FocusTimerController
import com.ignaciovinarro.stayfocused.planner.domain.FocusSession
import com.ignaciovinarro.stayfocused.planner.domain.FocusStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
class FocusTimerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val controller = mockk<FocusTimerController>(relaxed = true)
    private val session = MutableStateFlow<FocusSession?>(null)

    private fun sesion(status: FocusStatus = FocusStatus.RUNNING) = FocusSession(
        taskId = 1, taskTitle = "Estudiar", totalMs = 25 * 60_000L,
        status = status, endAtMs = 25 * 60_000L, remainingMs = 10 * 60_000L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { controller.session } returns session
        every { controller.remainingMs(any()) } returns 12_345L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sin sesion el estado esta vacio`() = runTest(dispatcher) {
        val viewModel = FocusTimerViewModel(controller)

        viewModel.uiState.test {
            val estado = expectMostRecentItem()
            assertNull(estado.session)
            assertFalse(estado.isPaused)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `con una sesion corriendo muestra el restante del controlador`() = runTest(dispatcher) {
        session.value = sesion()
        val viewModel = FocusTimerViewModel(controller)

        viewModel.uiState.test {
            awaitItem() // estado inicial
            dispatcher.scheduler.runCurrent()
            val estado = expectMostRecentItem()
            assertEquals("Estudiar", estado.session!!.taskTitle)
            assertEquals(12_345L, estado.remainingMs)
            assertFalse(estado.isPaused)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `una sesion en pausa se marca como pausada`() = runTest(dispatcher) {
        session.value = sesion(FocusStatus.PAUSED)
        val viewModel = FocusTimerViewModel(controller)

        viewModel.uiState.test {
            awaitItem()
            dispatcher.scheduler.runCurrent()
            assertTrue(expectMostRecentItem().isPaused)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cuando la sesion termina el estado se vacia`() = runTest(dispatcher) {
        session.value = sesion()
        val viewModel = FocusTimerViewModel(controller)

        viewModel.uiState.test {
            awaitItem()
            dispatcher.scheduler.runCurrent()
            session.value = null
            dispatcher.scheduler.runCurrent()
            assertNull(expectMostRecentItem().session)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `iniciar le pide al controlador arrancar con esos datos`() = runTest(dispatcher) {
        every { controller.start(any(), any(), any()) } returns true
        val viewModel = FocusTimerViewModel(controller)

        viewModel.onStart(7, "Leer", 50)

        verify { controller.start(7, "Leer", 50) }
    }

    @Test
    fun `si iniciar falla el estado lo avisa y se puede descartar`() = runTest(dispatcher) {
        every { controller.start(any(), any(), any()) } returns false
        val viewModel = FocusTimerViewModel(controller)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onStart(7, "Leer", 50)
            dispatcher.scheduler.runCurrent()
            assertTrue(expectMostRecentItem().startFailed)
            viewModel.onDismissStartError()
            dispatcher.scheduler.runCurrent()
            assertFalse(expectMostRecentItem().startFailed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `si reanudar falla el estado lo avisa`() = runTest(dispatcher) {
        every { controller.resume() } returns false
        val viewModel = FocusTimerViewModel(controller)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onResume()
            dispatcher.scheduler.runCurrent()
            assertTrue(expectMostRecentItem().startFailed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pausar y detener se delegan en el controlador`() = runTest(dispatcher) {
        val viewModel = FocusTimerViewModel(controller)

        viewModel.onPause()
        viewModel.onStop()

        verify { controller.pause() }
        verify { controller.stop() }
    }
}
