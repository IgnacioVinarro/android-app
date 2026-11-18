package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekLoadTest {

    // Lunes 2 de marzo de 2026.
    private val lunes = LocalDate.of(2026, 3, 2)

    private fun tarea(
        id: Long = 1,
        date: LocalDate = lunes,
        startMinutes: Int? = 9 * 60,
        durationMinutes: Int = 60,
        recurrenceType: String? = null,
        recurrenceEnd: Long? = null,
        status: Int = 0
    ) = TaskEntity(
        id = id,
        title = "Tarea",
        date = date.toEpochDay(),
        startMinutes = startMinutes,
        durationMinutes = durationMinutes,
        iconKey = "task_default",
        colorKey = "blue",
        recurrenceType = recurrenceType,
        recurrenceEnd = recurrenceEnd,
        status = status
    )

    private fun ocurrencia(
        taskId: Long,
        date: LocalDate,
        skipped: Boolean = false,
        overrideStartMinutes: Int? = null,
        overrideDurationMinutes: Int? = null,
        status: Int = 0
    ) = TaskOccurrenceEntity(
        taskId = taskId,
        date = date.toEpochDay(),
        skipped = skipped,
        overrideStartMinutes = overrideStartMinutes,
        overrideDurationMinutes = overrideDurationMinutes,
        status = status
    )

    // --- weekStartOf ---

    @Test
    fun `weekStartOf de un miercoles devuelve el lunes anterior`() {
        assertEquals(lunes, weekStartOf(lunes.plusDays(2)))
    }

    @Test
    fun `weekStartOf de un lunes es el mismo lunes`() {
        assertEquals(lunes, weekStartOf(lunes))
    }

    @Test
    fun `weekStartOf de un domingo devuelve el lunes de esa semana`() {
        assertEquals(lunes, weekStartOf(lunes.plusDays(6)))
    }

    // --- buildWeekLoad ---

    @Test
    fun `devuelve siete dias consecutivos desde el lunes`() {
        val semana = buildWeekLoad(lunes, emptyList(), emptyList())

        assertEquals((0L..6L).map { lunes.plusDays(it) }, semana.map { it.date })
    }

    @Test
    fun `una semana sin tareas tiene carga cero y fraccion cero`() {
        val semana = buildWeekLoad(lunes, emptyList(), emptyList())

        assertEquals(List(7) { 0 }, semana.map { it.taskCount })
        assertEquals(List(7) { 0 }, semana.map { it.plannedMinutes })
        assertEquals(List(7) { 0f }, semana.map { it.fraction })
    }

    @Test
    fun `una tarea simple cuenta solo en su dia y suma su duracion`() {
        val semana = buildWeekLoad(lunes, listOf(tarea(date = lunes.plusDays(2), durationMinutes = 90)), emptyList())

        assertEquals(listOf(0, 0, 1, 0, 0, 0, 0), semana.map { it.taskCount })
        assertEquals(90, semana[2].plannedMinutes)
    }

    @Test
    fun `una tarea sin hora cuenta en la cantidad pero no suma minutos`() {
        val semana = buildWeekLoad(lunes, listOf(tarea(startMinutes = null, durationMinutes = 60)), emptyList())

        assertEquals(1, semana[0].taskCount)
        assertEquals(0, semana[0].plannedMinutes)
    }

    @Test
    fun `varias tareas del mismo dia suman cantidad y minutos`() {
        val tareas = listOf(
            tarea(id = 1, durationMinutes = 30),
            tarea(id = 2, durationMinutes = 45, startMinutes = 14 * 60)
        )

        val semana = buildWeekLoad(lunes, tareas, emptyList())

        assertEquals(2, semana[0].taskCount)
        assertEquals(75, semana[0].plannedMinutes)
    }

    @Test
    fun `una tarea de fuera de la semana no cuenta`() {
        val tareas = listOf(tarea(date = lunes.minusDays(1)), tarea(id = 2, date = lunes.plusDays(7)))

        val semana = buildWeekLoad(lunes, tareas, emptyList())

        assertEquals(List(7) { 0 }, semana.map { it.taskCount })
    }

    @Test
    fun `una tarea borrada logicamente no cuenta`() {
        val semana = buildWeekLoad(lunes, listOf(tarea(status = -1)), emptyList())

        assertEquals(0, semana[0].taskCount)
    }

    @Test
    fun `una tarea diaria cuenta los siete dias`() {
        val semana = buildWeekLoad(lunes, listOf(tarea(recurrenceType = "DAILY")), emptyList())

        assertEquals(List(7) { 1 }, semana.map { it.taskCount })
    }

    @Test
    fun `una tarea semanal cuenta solo el dia que le toca`() {
        // Empezó el lunes de la semana anterior: le toca cada lunes.
        val tarea = tarea(date = lunes.minusWeeks(1), recurrenceType = "WEEKLY")

        val semana = buildWeekLoad(lunes, listOf(tarea), emptyList())

        assertEquals(listOf(1, 0, 0, 0, 0, 0, 0), semana.map { it.taskCount })
    }

    @Test
    fun `una recurrente que termino antes de la semana no cuenta`() {
        val tarea = tarea(
            date = lunes.minusWeeks(2),
            recurrenceType = "DAILY",
            recurrenceEnd = lunes.minusDays(1).toEpochDay()
        )

        val semana = buildWeekLoad(lunes, listOf(tarea), emptyList())

        assertEquals(List(7) { 0 }, semana.map { it.taskCount })
    }

    @Test
    fun `una recurrente que empieza a mitad de semana cuenta desde su inicio`() {
        val tarea = tarea(date = lunes.plusDays(4), recurrenceType = "DAILY")

        val semana = buildWeekLoad(lunes, listOf(tarea), emptyList())

        assertEquals(listOf(0, 0, 0, 0, 1, 1, 1), semana.map { it.taskCount })
    }

    @Test
    fun `una ocurrencia saltada no cuenta ese dia`() {
        val tarea = tarea(recurrenceType = "DAILY")
        val saltada = ocurrencia(taskId = 1, date = lunes.plusDays(1), skipped = true)

        val semana = buildWeekLoad(lunes, listOf(tarea), listOf(saltada))

        assertEquals(listOf(1, 0, 1, 1, 1, 1, 1), semana.map { it.taskCount })
    }

    @Test
    fun `una ocurrencia modificada usa su propia duracion`() {
        val tarea = tarea(recurrenceType = "DAILY", durationMinutes = 30)
        val modificada = ocurrencia(taskId = 1, date = lunes, overrideDurationMinutes = 120)

        val semana = buildWeekLoad(lunes, listOf(tarea), listOf(modificada))

        assertEquals(120, semana[0].plannedMinutes)
        assertEquals(30, semana[1].plannedMinutes)
    }

    @Test
    fun `una ocurrencia a la que se le puso hora suma minutos aunque la tarea no tenga hora`() {
        val tarea = tarea(recurrenceType = "DAILY", startMinutes = null, durationMinutes = 30)
        val conHora = ocurrencia(taskId = 1, date = lunes, overrideStartMinutes = 10 * 60)

        val semana = buildWeekLoad(lunes, listOf(tarea), listOf(conHora))

        assertEquals(30, semana[0].plannedMinutes)
        assertEquals(0, semana[1].plannedMinutes)
    }

    @Test
    fun `una ocurrencia borrada logicamente se ignora`() {
        val tarea = tarea(recurrenceType = "DAILY", durationMinutes = 30)
        val borrada = ocurrencia(taskId = 1, date = lunes, skipped = true, status = -1)

        val semana = buildWeekLoad(lunes, listOf(tarea), listOf(borrada))

        assertEquals(1, semana[0].taskCount)
    }

    @Test
    fun `la fraccion es relativa al dia con mas minutos`() {
        val tareas = listOf(
            tarea(id = 1, date = lunes, durationMinutes = 120),
            tarea(id = 2, date = lunes.plusDays(1), durationMinutes = 60)
        )

        val semana = buildWeekLoad(lunes, tareas, emptyList())

        assertEquals(1f, semana[0].fraction, 0.0001f)
        assertEquals(0.5f, semana[1].fraction, 0.0001f)
        assertEquals(0f, semana[2].fraction, 0.0001f)
    }
}
