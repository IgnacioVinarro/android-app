package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceTest {

    // Lunes 2026-03-02
    private val inicio = LocalDate.of(2026, 3, 2)

    private fun tarea(
        recurrenceType: String?,
        recurrenceInterval: Int? = null,
        recurrenceDays: Int? = null,
        recurrenceEnd: Long? = null
    ) = TaskEntity(
        title = "Tarea",
        date = inicio.toEpochDay(),
        durationMinutes = 30,
        iconKey = "task_default",
        colorKey = "blue",
        recurrenceType = recurrenceType,
        recurrenceInterval = recurrenceInterval,
        recurrenceDays = recurrenceDays,
        recurrenceEnd = recurrenceEnd
    )

    @Test
    fun `tarea no recurrente nunca tiene ocurrencia`() {
        assertFalse(occursOn(tarea(recurrenceType = null), inicio))
        assertFalse(occursOn(tarea(recurrenceType = null), inicio.plusDays(5)))
    }

    @Test
    fun `ninguna recurrencia tiene ocurrencia antes del inicio`() {
        val tarea = tarea(recurrenceType = RecurrenceType.DAILY.name)
        assertFalse(occursOn(tarea, inicio.minusDays(1)))
    }

    @Test
    fun `diaria ocurre todos los dias desde el inicio`() {
        val tarea = tarea(recurrenceType = RecurrenceType.DAILY.name)
        assertTrue(occursOn(tarea, inicio))
        assertTrue(occursOn(tarea, inicio.plusDays(1)))
        assertTrue(occursOn(tarea, inicio.plusDays(100)))
    }

    @Test
    fun `semanal ocurre el mismo dia de la semana`() {
        val tarea = tarea(recurrenceType = RecurrenceType.WEEKLY.name)
        assertTrue(occursOn(tarea, inicio.plusWeeks(3)))
        assertFalse(occursOn(tarea, inicio.plusDays(1)))
    }

    @Test
    fun `dias especificos ocurre solo en los dias marcados en la mascara`() {
        val lunesYMiercoles = dayOfWeekBit(DayOfWeek.MONDAY) or dayOfWeekBit(DayOfWeek.WEDNESDAY)
        val tarea = tarea(recurrenceType = RecurrenceType.SPECIFIC_DAYS.name, recurrenceDays = lunesYMiercoles)

        assertTrue(occursOn(tarea, inicio)) // lunes
        assertTrue(occursOn(tarea, inicio.plusDays(2))) // miércoles
        assertFalse(occursOn(tarea, inicio.plusDays(1))) // martes
    }

    @Test
    fun `mensual ocurre el mismo dia del mes`() {
        val tarea = tarea(recurrenceType = RecurrenceType.MONTHLY.name)
        assertTrue(occursOn(tarea, LocalDate.of(2026, 4, 2)))
        assertFalse(occursOn(tarea, LocalDate.of(2026, 4, 3)))
    }

    @Test
    fun `cada n dias respeta el intervalo configurado`() {
        val tarea = tarea(recurrenceType = RecurrenceType.EVERY_N_DAYS.name, recurrenceInterval = 3)
        assertTrue(occursOn(tarea, inicio.plusDays(3)))
        assertTrue(occursOn(tarea, inicio.plusDays(6)))
        assertFalse(occursOn(tarea, inicio.plusDays(4)))
    }

    @Test
    fun `respeta la fecha de fin de la recurrencia`() {
        val tarea = tarea(
            recurrenceType = RecurrenceType.DAILY.name,
            recurrenceEnd = inicio.plusDays(5).toEpochDay()
        )
        assertTrue(occursOn(tarea, inicio.plusDays(5)))
        assertFalse(occursOn(tarea, inicio.plusDays(6)))
    }
}
