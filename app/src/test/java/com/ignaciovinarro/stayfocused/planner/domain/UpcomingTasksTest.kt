package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.core.timeline.TimelineBlock
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class UpcomingTasksTest {

    private fun hora(h: Int, m: Int = 0) = h * 60 + m

    private fun bloque(
        title: String,
        startMinutes: Int,
        durationMinutes: Int = 30,
        completed: Boolean = false,
        source: String = TIMELINE_SOURCE_PLANNER,
        colorKey: String = "blue"
    ) = TimelineBlock(
        id = title,
        source = source,
        date = LocalDate.of(2026, 3, 5),
        startMinutes = startMinutes,
        durationMinutes = durationMinutes,
        title = title,
        iconKey = "task_default",
        colorKey = colorKey,
        completed = completed,
        route = "planner/task/1"
    )

    @Test
    fun `sin bloques no hay tareas ni progreso`() {
        val s = buildWidgetSnapshot(emptyList(), nowMinutes = hora(10))

        assertEquals(emptyList<WidgetTask>(), s.tasks)
        assertEquals(0, s.done)
        assertEquals(0, s.total)
    }

    @Test
    fun `devuelve las futuras ordenadas por hora`() {
        val s = buildWidgetSnapshot(
            listOf(bloque("C", hora(15)), bloque("A", hora(11)), bloque("B", hora(13))),
            nowMinutes = hora(10)
        )

        assertEquals(listOf("A", "B", "C"), s.tasks.map { it.title })
    }

    @Test
    fun `una tarea que ya termino no aparece`() {
        val s = buildWidgetSnapshot(listOf(bloque("Pasada", hora(8), durationMinutes = 60)), nowMinutes = hora(10))

        assertEquals(emptyList<WidgetTask>(), s.tasks)
    }

    @Test
    fun `una tarea en curso si aparece`() {
        val s = buildWidgetSnapshot(listOf(bloque("En curso", hora(9, 30), durationMinutes = 60)), nowMinutes = hora(10))

        assertEquals(listOf("En curso"), s.tasks.map { it.title })
    }

    @Test
    fun `una tarea que termina justo ahora ya no aparece`() {
        val s = buildWidgetSnapshot(listOf(bloque("Justa", hora(9), durationMinutes = 60)), nowMinutes = hora(10))

        assertEquals(emptyList<WidgetTask>(), s.tasks)
    }

    @Test
    fun `una tarea completada no aparece aunque sea futura`() {
        val s = buildWidgetSnapshot(listOf(bloque("Hecha", hora(15), completed = true)), nowMinutes = hora(10))

        assertEquals(emptyList<WidgetTask>(), s.tasks)
    }

    @Test
    fun `los bloques de otros modulos no aparecen ni cuentan`() {
        val s = buildWidgetSnapshot(
            listOf(bloque("Gimnasio", hora(18), source = "gym"), bloque("Tarea", hora(12))),
            nowMinutes = hora(10)
        )

        assertEquals(listOf("Tarea"), s.tasks.map { it.title })
        assertEquals(1, s.total)
    }

    @Test
    fun `respeta el limite`() {
        val bloques = (1..6).map { bloque("T$it", hora(10 + it)) }

        val s = buildWidgetSnapshot(bloques, nowMinutes = hora(9))

        assertEquals(WIDGET_MAX_TASKS, s.tasks.size)
        assertEquals(listOf("T1", "T2", "T3", "T4"), s.tasks.map { it.title })
    }

    @Test
    fun `el limite se puede cambiar`() {
        val bloques = (1..6).map { bloque("T$it", hora(10 + it)) }

        assertEquals(2, buildWidgetSnapshot(bloques, nowMinutes = hora(9), limit = 2).tasks.size)
    }

    @Test
    fun `el progreso cuenta completadas sobre todas las del dia, pasadas incluidas`() {
        val s = buildWidgetSnapshot(
            listOf(
                bloque("Hecha temprano", hora(8), completed = true),
                bloque("Perdida", hora(9)),
                bloque("Futura", hora(15))
            ),
            nowMinutes = hora(12)
        )

        assertEquals(1, s.done)
        assertEquals(3, s.total)
    }

    @Test
    fun `cada fila lleva hora y color de la tarea`() {
        val s = buildWidgetSnapshot(listOf(bloque("Leer", hora(14, 15), colorKey = "green")), nowMinutes = hora(10))

        assertEquals(WidgetTask("Leer", hora(14, 15), "green"), s.tasks.single())
    }

    @Test
    fun `dos tareas a la misma hora aparecen las dos`() {
        val s = buildWidgetSnapshot(listOf(bloque("A", hora(11)), bloque("B", hora(11))), nowMinutes = hora(10))

        assertEquals(2, s.tasks.size)
    }
}
