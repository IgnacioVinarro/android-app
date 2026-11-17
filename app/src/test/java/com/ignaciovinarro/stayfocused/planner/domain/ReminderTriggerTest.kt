package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTriggerTest {

    // Lunes 2026-03-02
    private val lunes = LocalDate.of(2026, 3, 2)
    private val diezAm = 10 * 60

    private fun tarea(
        date: LocalDate = lunes,
        startMinutes: Int? = diezAm,
        recurrenceType: String? = null,
        completedAt: Long? = null,
        status: Int = 0,
        recurrenceEnd: Long? = null
    ) = TaskEntity(
        id = 1,
        title = "Tarea",
        date = date.toEpochDay(),
        startMinutes = startMinutes,
        durationMinutes = 30,
        iconKey = "task_default",
        colorKey = "blue",
        recurrenceType = recurrenceType,
        recurrenceEnd = recurrenceEnd,
        completedAt = completedAt,
        status = status
    )

    private fun ocurrencia(
        date: LocalDate,
        skipped: Boolean = false,
        completedAt: Long? = null,
        overrideStart: Int? = null
    ) = TaskOccurrenceEntity(
        taskId = 1,
        date = date.toEpochDay(),
        skipped = skipped,
        completedAt = completedAt,
        overrideStartMinutes = overrideStart
    )

    private fun triggerAt(
        task: TaskEntity,
        minutesBefore: Int,
        occurrences: List<TaskOccurrenceEntity>,
        now: java.time.LocalDateTime
    ) = nextReminder(task, minutesBefore, occurrences, now)?.triggerAt

    private fun at(date: LocalDate, hour: Int, minute: Int = 0) = date.atTime(hour, minute)

    @Test
    fun `tarea simple a la hora avisa a la hora de inicio`() {
        val result = triggerAt(tarea(), 0, emptyList(), at(lunes, 8))
        assertEquals(at(lunes, 10), result)
    }

    @Test
    fun `tarea simple con minutos antes resta esos minutos`() {
        val result = triggerAt(tarea(), 15, emptyList(), at(lunes, 8))
        assertEquals(at(lunes, 9, 45), result)
    }

    @Test
    fun `un dia antes avisa el dia anterior`() {
        val result = triggerAt(tarea(), 24 * 60, emptyList(), at(lunes.minusDays(2), 8))
        assertEquals(at(lunes.minusDays(1), 10), result)
    }

    @Test
    fun `aviso que ya paso no se programa`() {
        assertNull(triggerAt(tarea(), 15, emptyList(), at(lunes, 9, 50)))
    }

    @Test
    fun `aviso justo en este instante no cuenta como futuro`() {
        assertNull(triggerAt(tarea(), 0, emptyList(), at(lunes, 10)))
    }

    @Test
    fun `tarea sin hora no tiene aviso`() {
        assertNull(triggerAt(tarea(startMinutes = null), 0, emptyList(), at(lunes, 8)))
    }

    @Test
    fun `tarea completada o borrada no tiene aviso`() {
        assertNull(triggerAt(tarea(completedAt = 1L), 0, emptyList(), at(lunes, 8)))
        assertNull(triggerAt(tarea(status = -1), 0, emptyList(), at(lunes, 8)))
    }

    @Test
    fun `recurrente diaria avisa la proxima ocurrencia de hoy si todavia no paso`() {
        val tarea = tarea(recurrenceType = RecurrenceType.DAILY.name)
        val result = triggerAt(tarea, 0, emptyList(), at(lunes.plusDays(3), 8))
        assertEquals(at(lunes.plusDays(3), 10), result)
    }

    @Test
    fun `recurrente diaria pasa a manana si el aviso de hoy ya paso`() {
        val tarea = tarea(recurrenceType = RecurrenceType.DAILY.name)
        val result = triggerAt(tarea, 0, emptyList(), at(lunes.plusDays(3), 11))
        assertEquals(at(lunes.plusDays(4), 10), result)
    }

    @Test
    fun `recurrente salta la ocurrencia saltada`() {
        val tarea = tarea(recurrenceType = RecurrenceType.DAILY.name)
        val hoy = lunes.plusDays(1)
        val result = triggerAt(tarea, 0, listOf(ocurrencia(hoy, skipped = true)), at(hoy, 8))
        assertEquals(at(hoy.plusDays(1), 10), result)
    }

    @Test
    fun `recurrente salta la ocurrencia ya completada`() {
        val tarea = tarea(recurrenceType = RecurrenceType.DAILY.name)
        val hoy = lunes.plusDays(1)
        val result = triggerAt(tarea, 0, listOf(ocurrencia(hoy, completedAt = 5L)), at(hoy, 8))
        assertEquals(at(hoy.plusDays(1), 10), result)
    }

    @Test
    fun `recurrente usa la hora modificada de la ocurrencia`() {
        val tarea = tarea(recurrenceType = RecurrenceType.DAILY.name)
        val hoy = lunes.plusDays(1)
        val result = triggerAt(tarea, 30, listOf(ocurrencia(hoy, overrideStart = 15 * 60)), at(hoy, 8))
        assertEquals(at(hoy, 14, 30), result)
    }

    @Test
    fun `recurrente con aviso de un dia antes avisa hoy por la ocurrencia de manana`() {
        val tarea = tarea(recurrenceType = RecurrenceType.DAILY.name)
        val hoy = lunes.plusDays(1)
        // Hoy 10:00 ya pasó (es el aviso de la ocurrencia de mañana); el próximo es el de mañana
        // 10:00, que corresponde a la ocurrencia de pasado mañana.
        val result = triggerAt(tarea, 24 * 60, emptyList(), at(hoy, 11))
        assertEquals(at(hoy.plusDays(1), 10), result)
    }

    @Test
    fun `recurrente que ya termino no tiene aviso`() {
        val tarea = tarea(recurrenceType = RecurrenceType.DAILY.name, recurrenceEnd = lunes.toEpochDay())
        assertNull(triggerAt(tarea, 0, emptyList(), at(lunes.plusDays(1), 8)))
    }

    @Test
    fun `recurrente semanal salta hasta el dia que corresponde`() {
        val tarea = tarea(recurrenceType = RecurrenceType.WEEKLY.name)
        val result = triggerAt(tarea, 0, emptyList(), at(lunes.plusDays(1), 8))
        assertEquals(at(lunes.plusDays(7), 10), result)
    }

    @Test
    fun `recurrente con todas las ocurrencias cercanas saltadas busca la siguiente`() {
        val tarea = tarea(recurrenceType = RecurrenceType.DAILY.name)
        val saltadas = (0L..2L).map { ocurrencia(lunes.plusDays(it), skipped = true) }
        val result = triggerAt(tarea, 0, saltadas, at(lunes, 8))
        assertEquals(at(lunes.plusDays(3), 10), result)
    }

    @Test
    fun `aviso de un dia antes informa el dia de la ocurrencia y no el del aviso`() {
        val result = nextReminder(tarea(), 24 * 60, emptyList(), at(lunes.minusDays(2), 8))
        assertEquals(at(lunes.minusDays(1), 10), result?.triggerAt)
        assertEquals(lunes, result?.occurrenceDate)
    }

    @Test
    fun `recurrente informa la fecha de la ocurrencia elegida`() {
        val tarea = tarea(recurrenceType = RecurrenceType.DAILY.name)
        val hoy = lunes.plusDays(1)
        val result = nextReminder(tarea, 0, listOf(ocurrencia(hoy, skipped = true)), at(hoy, 8))
        assertEquals(hoy.plusDays(1), result?.occurrenceDate)
    }
}
