package com.ignaciovinarro.stayfocused.core.timeline

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlapArrangerTest {

    private fun bloque(
        id: String,
        startMinutes: Int,
        durationMinutes: Int
    ) = TimelineBlock(
        id = id,
        source = "planner",
        date = LocalDate.of(2026, 3, 5),
        startMinutes = startMinutes,
        durationMinutes = durationMinutes,
        title = id,
        iconKey = "task_default",
        colorKey = "blue",
        completed = false,
        route = "planner/task/$id"
    )

    @Test
    fun `lista vacia devuelve lista vacia`() {
        assertEquals(emptyList<PositionedTimelineBlock>(), OverlapArranger.arrange(emptyList()))
    }

    @Test
    fun `bloques sin superposicion quedan todos en columna 0`() {
        val a = bloque("a", startMinutes = 9 * 60, durationMinutes = 30)
        val b = bloque("b", startMinutes = 10 * 60, durationMinutes = 30)

        val resultado = OverlapArranger.arrange(listOf(a, b))

        assertEquals(0, resultado.first { it.block.id == "a" }.column)
        assertEquals(0, resultado.first { it.block.id == "b" }.column)
        assertEquals(1, resultado.first { it.block.id == "a" }.columnCount)
        assertEquals(1, resultado.first { it.block.id == "b" }.columnCount)
    }

    @Test
    fun `dos bloques que se pisan quedan en columnas distintas`() {
        val a = bloque("a", startMinutes = 9 * 60, durationMinutes = 60)
        val b = bloque("b", startMinutes = 9 * 60 + 30, durationMinutes = 60)

        val resultado = OverlapArranger.arrange(listOf(a, b))

        val columnaA = resultado.first { it.block.id == "a" }.column
        val columnaB = resultado.first { it.block.id == "b" }.column
        assertEquals(setOf(0, 1), setOf(columnaA, columnaB))
        assertEquals(2, resultado.first().columnCount)
    }

    @Test
    fun `tres bloques reusan columna cuando la primera ya termino`() {
        // a: 9:00-9:30, b: 9:15-9:45 (se pisa con a), c: 9:30-10:00 (empieza cuando a ya terminó)
        val a = bloque("a", startMinutes = 9 * 60, durationMinutes = 30)
        val b = bloque("b", startMinutes = 9 * 60 + 15, durationMinutes = 30)
        val c = bloque("c", startMinutes = 9 * 60 + 30, durationMinutes = 30)

        val resultado = OverlapArranger.arrange(listOf(a, b, c))

        val columnaA = resultado.first { it.block.id == "a" }.column
        val columnaC = resultado.first { it.block.id == "c" }.column
        assertEquals(columnaA, columnaC)
        assertEquals(2, resultado.first().columnCount)
    }

    @Test
    fun `clusters separados no se afectan entre si`() {
        val a = bloque("a", startMinutes = 9 * 60, durationMinutes = 30)
        val b = bloque("b", startMinutes = 9 * 60 + 15, durationMinutes = 30)
        val c = bloque("c", startMinutes = 14 * 60, durationMinutes = 30)

        val resultado = OverlapArranger.arrange(listOf(a, b, c))

        assertEquals(0, resultado.first { it.block.id == "c" }.column)
        assertEquals(1, resultado.first { it.block.id == "c" }.columnCount)
    }
}
