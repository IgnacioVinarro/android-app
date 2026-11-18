package com.ignaciovinarro.stayfocused.planner.data

import com.ignaciovinarro.stayfocused.core.notifications.AlarmScheduler
import com.ignaciovinarro.stayfocused.planner.domain.FocusStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FocusTimerControllerTest {

    private class FakeClock(var mono: Long = 10_000, var wall: Long = 1_700_000_000_000) : FocusClock {
        override fun monotonicMillis() = mono
        override fun wallMillis() = wall
    }

    private val minuto = 60_000L
    private val clock = FakeClock()
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val launcher = mockk<FocusServiceLauncher>(relaxed = true)
    private lateinit var controller: FocusTimerController

    @Before
    fun setUp() {
        every { alarmScheduler.schedule(any(), any(), any()) } returns true
        controller = FocusTimerController(clock, alarmScheduler, launcher)
    }

    // --- start ---

    @Test
    fun `iniciar crea la sesion, programa la alarma de fin y arranca el servicio`() {
        val millis = slot<Long>()
        every { alarmScheduler.schedule(FOCUS_ALARM_KEY, capture(millis), any()) } returns true

        val ok = controller.start(taskId = 5, taskTitle = "Estudiar", minutes = 25)

        assertTrue(ok)
        val sesion = controller.session.value!!
        assertEquals(5L, sesion.taskId)
        assertEquals(FocusStatus.RUNNING, sesion.status)
        assertEquals(clock.wall + 25 * minuto, millis.captured)
        verify { launcher.start() }
    }

    @Test
    fun `la alarma de fin avisa al receptor del enfoque con la tarea`() {
        val pedido = slot<com.ignaciovinarro.stayfocused.core.notifications.AlarmRequest>()
        every { alarmScheduler.schedule(any(), any(), capture(pedido)) } returns true

        controller.start(5, "Estudiar", 25)

        assertEquals(FocusTimerReceiver::class.java, pedido.captured.receiver)
        assertEquals(ACTION_FOCUS_END, pedido.captured.action)
        assertEquals(mapOf(EXTRA_FOCUS_TASK_ID to 5L), pedido.captured.extras)
    }

    @Test
    fun `iniciar con minutos invalidos no hace nada`() {
        val ok = controller.start(5, "Estudiar", 0)

        assertFalse(ok)
        assertNull(controller.session.value)
        verify(exactly = 0) { alarmScheduler.schedule(any(), any(), any()) }
        verify(exactly = 0) { launcher.start() }
    }

    @Test
    fun `iniciar sin permiso de alarma exacta no crea la sesion ni arranca el servicio`() {
        every { alarmScheduler.schedule(any(), any(), any()) } returns false

        val ok = controller.start(5, "Estudiar", 25)

        assertFalse(ok)
        assertNull(controller.session.value)
        verify(exactly = 0) { launcher.start() }
    }

    @Test
    fun `iniciar con una sesion en curso la reemplaza`() {
        controller.start(1, "Primera", 25)

        controller.start(2, "Segunda", 50)

        assertEquals(2L, controller.session.value!!.taskId)
        assertEquals(50 * minuto, controller.session.value!!.totalMs)
    }

    // --- pause / resume ---

    @Test
    fun `pausar congela lo que falta y cancela la alarma`() {
        controller.start(5, "Estudiar", 25)
        clock.mono += 5 * minuto

        controller.pause()

        val sesion = controller.session.value!!
        assertEquals(FocusStatus.PAUSED, sesion.status)
        assertEquals(20 * minuto, controller.remainingMs(sesion))
        clock.mono += 100 * minuto
        assertEquals(20 * minuto, controller.remainingMs(controller.session.value!!))
        verify { alarmScheduler.cancel(FOCUS_ALARM_KEY, any()) }
    }

    @Test
    fun `reanudar reprograma la alarma con lo que quedaba`() {
        controller.start(5, "Estudiar", 25)
        clock.mono += 5 * minuto
        controller.pause()
        clock.wall += 60 * minuto
        clock.mono += 60 * minuto
        val millis = slot<Long>()
        every { alarmScheduler.schedule(FOCUS_ALARM_KEY, capture(millis), any()) } returns true

        val ok = controller.resume()

        assertTrue(ok)
        assertEquals(FocusStatus.RUNNING, controller.session.value!!.status)
        assertEquals(clock.wall + 20 * minuto, millis.captured)
        assertEquals(20 * minuto, controller.remainingMs(controller.session.value!!))
    }

    @Test
    fun `reanudar sin permiso de alarma deja la sesion en pausa`() {
        controller.start(5, "Estudiar", 25)
        controller.pause()
        every { alarmScheduler.schedule(any(), any(), any()) } returns false

        val ok = controller.resume()

        assertFalse(ok)
        assertEquals(FocusStatus.PAUSED, controller.session.value!!.status)
    }

    @Test
    fun `reanudar sin sesion devuelve false`() {
        assertFalse(controller.resume())
    }

    @Test
    fun `pausar sin sesion no hace nada`() {
        controller.pause()

        assertNull(controller.session.value)
        verify(exactly = 0) { alarmScheduler.cancel(any(), any()) }
    }

    // --- stop ---

    @Test
    fun `detener borra la sesion y cancela la alarma`() {
        controller.start(5, "Estudiar", 25)

        controller.stop()

        assertNull(controller.session.value)
        verify { alarmScheduler.cancel(FOCUS_ALARM_KEY, any()) }
    }

    @Test
    fun `detener sin sesion no hace nada`() {
        controller.stop()

        verify(exactly = 0) { alarmScheduler.cancel(any(), any()) }
    }

    // --- onEndAlarmFired ---

    @Test
    fun `cuando suena la alarma de una sesion corriendo avisa y limpia la sesion`() {
        controller.start(5, "Estudiar", 25)

        assertTrue(controller.onEndAlarmFired())
        assertNull(controller.session.value)
    }

    @Test
    fun `una alarma vieja con la sesion en pausa se ignora`() {
        controller.start(5, "Estudiar", 25)
        controller.pause()

        assertFalse(controller.onEndAlarmFired())
        assertEquals(FocusStatus.PAUSED, controller.session.value!!.status)
    }

    @Test
    fun `una alarma sin sesion en memoria igual avisa`() {
        assertTrue(controller.onEndAlarmFired())
    }
}
