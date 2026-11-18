package com.ignaciovinarro.stayfocused.planner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTimerTest {

    private val minuto = 60_000L

    private fun sesion(minutes: Int = 25, nowMs: Long = 1_000) = startFocus(7, "Estudiar", minutes, nowMs)!!

    // --- isValidFocusMinutes ---

    @Test
    fun `los limites de minutos son validos`() {
        assertTrue(isValidFocusMinutes(MIN_FOCUS_MINUTES))
        assertTrue(isValidFocusMinutes(MAX_FOCUS_MINUTES))
    }

    @Test
    fun `cero negativo o mas del maximo no son validos`() {
        assertFalse(isValidFocusMinutes(0))
        assertFalse(isValidFocusMinutes(-5))
        assertFalse(isValidFocusMinutes(MAX_FOCUS_MINUTES + 1))
    }

    @Test
    fun `los preajustes son validos`() {
        assertTrue(FOCUS_PRESET_MINUTES.all(::isValidFocusMinutes))
    }

    // --- startFocus ---

    @Test
    fun `empezar crea una sesion corriendo que termina a los minutos pedidos`() {
        val s = sesion(minutes = 25, nowMs = 1_000)

        assertEquals(FocusStatus.RUNNING, s.status)
        assertEquals(25 * minuto, s.totalMs)
        assertEquals(1_000 + 25 * minuto, s.endAtMs)
        assertEquals(7L, s.taskId)
        assertEquals("Estudiar", s.taskTitle)
    }

    @Test
    fun `empezar con minutos invalidos devuelve nulo`() {
        assertNull(startFocus(1, "x", 0, 0))
        assertNull(startFocus(1, "x", MAX_FOCUS_MINUTES + 1, 0))
    }

    // --- remainingAt ---

    @Test
    fun `corriendo, lo que falta baja con el reloj`() {
        val s = sesion(minutes = 25, nowMs = 0)

        assertEquals(25 * minuto, s.remainingAt(0))
        assertEquals(20 * minuto, s.remainingAt(5 * minuto))
    }

    @Test
    fun `lo que falta nunca es negativo`() {
        val s = sesion(minutes = 1, nowMs = 0)

        assertEquals(0L, s.remainingAt(10 * minuto))
    }

    // --- pausa y reanudacion ---

    @Test
    fun `pausar congela lo que falta aunque pase el tiempo`() {
        val pausada = sesion(minutes = 25, nowMs = 0).pausedAt(5 * minuto)

        assertEquals(FocusStatus.PAUSED, pausada.status)
        assertEquals(20 * minuto, pausada.remainingAt(5 * minuto))
        assertEquals(20 * minuto, pausada.remainingAt(500 * minuto))
    }

    @Test
    fun `pausar dos veces no cambia nada`() {
        val pausada = sesion(minutes = 25, nowMs = 0).pausedAt(5 * minuto)

        assertEquals(pausada, pausada.pausedAt(9 * minuto))
    }

    @Test
    fun `reanudar sigue desde lo que quedaba`() {
        val reanudada = sesion(minutes = 25, nowMs = 0)
            .pausedAt(5 * minuto)
            .resumedAt(100 * minuto)

        assertEquals(FocusStatus.RUNNING, reanudada.status)
        assertEquals(20 * minuto, reanudada.remainingAt(100 * minuto))
        assertEquals(15 * minuto, reanudada.remainingAt(105 * minuto))
    }

    @Test
    fun `reanudar una sesion que ya corre no cambia nada`() {
        val s = sesion()

        assertEquals(s, s.resumedAt(5_000))
    }

    @Test
    fun `pausar y reanudar varias veces suma bien el tiempo pausado`() {
        val s = sesion(minutes = 10, nowMs = 0)
            .pausedAt(2 * minuto)          // faltan 8
            .resumedAt(50 * minuto)
            .pausedAt(53 * minuto)         // faltan 5
            .resumedAt(90 * minuto)

        assertEquals(5 * minuto, s.remainingAt(90 * minuto))
        assertEquals(90 * minuto + 5 * minuto, s.endAtMs)
    }

    // --- isFinishedAt ---

    @Test
    fun `termina cuando llega a cero`() {
        val s = sesion(minutes = 1, nowMs = 0)

        assertFalse(s.isFinishedAt(minuto - 1))
        assertTrue(s.isFinishedAt(minuto))
        assertTrue(s.isFinishedAt(2 * minuto))
    }

    @Test
    fun `una sesion en pausa nunca termina`() {
        val pausada = sesion(minutes = 1, nowMs = 0).pausedAt(10_000)

        assertFalse(pausada.isFinishedAt(100 * minuto))
    }
}
