package com.ignaciovinarro.stayfocused.planner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderOptionsTest {

    @Test
    fun `las opciones fijas son las del ticket y todas son validas`() {
        assertEquals(listOf(0, 5, 10, 15, 30, 60, 1440), PRESET_REMINDER_MINUTES)
        assertTrue(PRESET_REMINDER_MINUTES.all(::isValidReminderMinutes))
    }

    @Test
    fun `cero (a la hora) y el tope son validos`() {
        assertTrue(isValidReminderMinutes(0))
        assertTrue(isValidReminderMinutes(MAX_REMINDER_MINUTES_BEFORE))
    }

    @Test
    fun `negativos y mayores al tope no son validos`() {
        assertFalse(isValidReminderMinutes(-1))
        assertFalse(isValidReminderMinutes(MAX_REMINDER_MINUTES_BEFORE + 1))
    }

    @Test
    fun `el tope es de 7 dias`() {
        assertEquals(10080, MAX_REMINDER_MINUTES_BEFORE)
    }

    @Test
    fun `cero se muestra en minutos (la pantalla lo traduce a a la hora)`() {
        assertEquals(ReminderAmount(0, ReminderUnit.MINUTES), reminderAmount(0))
    }

    @Test
    fun `menos de una hora se muestra en minutos`() {
        assertEquals(ReminderAmount(5, ReminderUnit.MINUTES), reminderAmount(5))
        assertEquals(ReminderAmount(59, ReminderUnit.MINUTES), reminderAmount(59))
    }

    @Test
    fun `multiplos exactos de una hora se muestran en horas`() {
        assertEquals(ReminderAmount(1, ReminderUnit.HOURS), reminderAmount(60))
        assertEquals(ReminderAmount(2, ReminderUnit.HOURS), reminderAmount(120))
    }

    @Test
    fun `una hora y media no se redondea, se muestra en minutos`() {
        assertEquals(ReminderAmount(90, ReminderUnit.MINUTES), reminderAmount(90))
    }

    @Test
    fun `multiplos exactos de un dia se muestran en dias`() {
        assertEquals(ReminderAmount(1, ReminderUnit.DAYS), reminderAmount(1440))
        assertEquals(ReminderAmount(7, ReminderUnit.DAYS), reminderAmount(MAX_REMINDER_MINUTES_BEFORE))
    }

    @Test
    fun `normalizar ordena, saca repetidos y descarta invalidos`() {
        assertEquals(
            listOf(0, 10, 30),
            normalizeReminderMinutes(listOf(30, 10, 30, -5, 0, MAX_REMINDER_MINUTES_BEFORE + 1))
        )
    }

    @Test
    fun `normalizar una lista vacia devuelve vacio`() {
        assertEquals(emptyList<Int>(), normalizeReminderMinutes(emptyList()))
    }
}
