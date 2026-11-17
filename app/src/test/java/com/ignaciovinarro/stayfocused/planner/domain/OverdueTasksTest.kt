package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverdueTasksTest {

    private val hoy = LocalDate.of(2026, 3, 5)
    private val ayer = hoy.minusDays(1)

    private fun tarea(
        date: LocalDate = ayer,
        startMinutes: Int? = 9 * 60,
        completedAt: Long? = null,
        recurrenceType: String? = null,
        status: Int = 0
    ) = TaskEntity(
        id = 1,
        title = "Tarea",
        date = date.toEpochDay(),
        startMinutes = startMinutes,
        durationMinutes = 45,
        iconKey = "task_default",
        colorKey = "blue",
        completedAt = completedAt,
        recurrenceType = recurrenceType,
        status = status
    )

    // --- isOverdue ---

    @Test
    fun `una tarea simple de ayer sin completar esta atrasada`() {
        assertTrue(isOverdue(tarea(date = ayer), hoy))
    }

    @Test
    fun `una tarea de ayer sin hora tambien esta atrasada`() {
        assertTrue(isOverdue(tarea(date = ayer, startMinutes = null), hoy))
    }

    @Test
    fun `una tarea de hoy no esta atrasada aunque su hora ya haya pasado`() {
        assertFalse(isOverdue(tarea(date = hoy, startMinutes = 0), hoy))
    }

    @Test
    fun `una tarea futura no esta atrasada`() {
        assertFalse(isOverdue(tarea(date = hoy.plusDays(1)), hoy))
    }

    @Test
    fun `una tarea completada no esta atrasada`() {
        assertFalse(isOverdue(tarea(completedAt = 1L), hoy))
    }

    @Test
    fun `una tarea recurrente no esta atrasada`() {
        assertFalse(isOverdue(tarea(recurrenceType = RecurrenceType.DAILY.name), hoy))
    }

    @Test
    fun `una tarea borrada no esta atrasada`() {
        assertFalse(isOverdue(tarea(status = -1), hoy))
    }

    // --- rescheduleOverdueTask ---

    @Test
    fun `reprogramar cambia el dia y conserva la hora y la duracion`() {
        val result = rescheduleOverdueTask(tarea(startMinutes = 14 * 60), hoy.plusDays(2), hoy)

        assertEquals(hoy.plusDays(2).toEpochDay(), result?.date)
        assertEquals(14 * 60, result?.startMinutes)
        assertEquals(45, result?.durationMinutes)
    }

    @Test
    fun `reprogramar para hoy esta permitido`() {
        assertEquals(hoy.toEpochDay(), rescheduleOverdueTask(tarea(), hoy, hoy)?.date)
    }

    @Test
    fun `reprogramar a una fecha pasada se rechaza`() {
        assertNull(rescheduleOverdueTask(tarea(date = hoy.minusDays(5)), hoy.minusDays(2), hoy))
    }

    @Test
    fun `reprogramar una tarea que no esta atrasada se rechaza`() {
        assertNull(rescheduleOverdueTask(tarea(date = hoy), hoy.plusDays(1), hoy))
        assertNull(rescheduleOverdueTask(tarea(completedAt = 1L), hoy.plusDays(1), hoy))
    }

    @Test
    fun `reprogramar una tarea sin hora la deja sin hora`() {
        val result = rescheduleOverdueTask(tarea(startMinutes = null), hoy.plusDays(1), hoy)

        assertNull(result?.startMinutes)
    }

    // --- moveToInbox ---

    @Test
    fun `llevar a la bandeja pone la fecha de hoy y saca la hora`() {
        val result = moveToInbox(tarea(date = ayer, startMinutes = 9 * 60), hoy)

        assertEquals(hoy.toEpochDay(), result?.date)
        assertNull(result?.startMinutes)
    }

    @Test
    fun `llevar a la bandeja conserva titulo duracion e identidad`() {
        val original = tarea()
        val result = moveToInbox(original, hoy)

        assertEquals(original.id, result?.id)
        assertEquals(original.uuid, result?.uuid)
        assertEquals(original.title, result?.title)
        assertEquals(original.durationMinutes, result?.durationMinutes)
    }

    @Test
    fun `llevar a la bandeja una tarea que no esta atrasada se rechaza`() {
        assertNull(moveToInbox(tarea(date = hoy), hoy))
        assertNull(moveToInbox(tarea(recurrenceType = RecurrenceType.DAILY.name), hoy))
    }
}
