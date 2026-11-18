package com.ignaciovinarro.stayfocused.core.timeline

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeGapsTest {

    private fun bloque(startMinutes: Int, durationMinutes: Int, id: String = "b$startMinutes") = TimelineBlock(
        id = id,
        source = "planner",
        date = LocalDate.of(2026, 3, 5),
        startMinutes = startMinutes,
        durationMinutes = durationMinutes,
        title = "Tarea",
        iconKey = "task_default",
        colorKey = "blue",
        completed = false,
        route = "planner/task/1"
    )

    private fun hora(h: Int, m: Int = 0) = h * 60 + m

    // --- freeGaps ---

    @Test
    fun `un dia sin bloques no tiene huecos`() {
        assertTrue(freeGaps(emptyList()).isEmpty())
    }

    @Test
    fun `un bloque en el medio deja un hueco antes y otro despues`() {
        val gaps = freeGaps(listOf(bloque(hora(10), 60)))

        assertEquals(listOf(FreeGap(0, hora(10)), FreeGap(hora(11), 24 * 60)), gaps)
    }

    @Test
    fun `dos bloques separados dejan un hueco entre ambos`() {
        val gaps = freeGaps(listOf(bloque(hora(9), 60), bloque(hora(12), 60)))

        assertTrue(FreeGap(hora(10), hora(12)) in gaps)
        assertEquals(3, gaps.size)
    }

    @Test
    fun `un hueco de menos de 30 minutos no se cuenta`() {
        val gaps = freeGaps(listOf(bloque(hora(9), 60), bloque(hora(10, 25), 60)))

        assertTrue(gaps.none { it.startMinutes == hora(10) })
    }

    @Test
    fun `un hueco de exactamente 30 minutos se cuenta`() {
        val gaps = freeGaps(listOf(bloque(hora(9), 60), bloque(hora(10, 30), 60)))

        assertTrue(FreeGap(hora(10), hora(10, 30)) in gaps)
    }

    @Test
    fun `bloques pegados no dejan hueco entre ellos`() {
        val gaps = freeGaps(listOf(bloque(hora(9), 60), bloque(hora(10), 60)))

        assertEquals(listOf(FreeGap(0, hora(9)), FreeGap(hora(11), 24 * 60)), gaps)
    }

    @Test
    fun `bloques superpuestos se juntan y no dejan hueco adentro`() {
        // El segundo empieza adentro del primero y termina despues: el ocupado es de 9 a 12.
        val gaps = freeGaps(listOf(bloque(hora(9), 120), bloque(hora(10), 120)))

        assertEquals(listOf(FreeGap(0, hora(9)), FreeGap(hora(12), 24 * 60)), gaps)
    }

    @Test
    fun `un bloque largo que contiene a otro corto no deja hueco por el corto`() {
        val gaps = freeGaps(listOf(bloque(hora(9), 240), bloque(hora(10), 30)))

        assertEquals(listOf(FreeGap(0, hora(9)), FreeGap(hora(13), 24 * 60)), gaps)
    }

    @Test
    fun `el orden de entrada no importa`() {
        val a = freeGaps(listOf(bloque(hora(12), 60), bloque(hora(9), 60)))
        val b = freeGaps(listOf(bloque(hora(9), 60), bloque(hora(12), 60)))

        assertEquals(a, b)
    }

    @Test
    fun `un bloque que llega a medianoche no deja hueco al final`() {
        val gaps = freeGaps(listOf(bloque(hora(22), 120)))

        assertEquals(listOf(FreeGap(0, hora(22))), gaps)
    }

    @Test
    fun `un bloque que arranca a las 00 00 no deja hueco al principio`() {
        val gaps = freeGaps(listOf(bloque(0, 60)))

        assertEquals(listOf(FreeGap(60, 24 * 60)), gaps)
    }

    @Test
    fun `el minimo se puede cambiar`() {
        val bloques = listOf(bloque(hora(9), 60), bloque(hora(10, 15), 60))

        assertTrue(freeGaps(bloques, minGapMinutes = 10).any { it.startMinutes == hora(10) })
        assertTrue(freeGaps(bloques, minGapMinutes = 30).none { it.startMinutes == hora(10) })
    }

    @Test
    fun `la duracion de un hueco es fin menos inicio`() {
        assertEquals(90, FreeGap(hora(10), hora(11, 30)).durationMinutes)
    }

    // --- durationForGap ---

    @Test
    fun `si el hueco es mas largo que la duracion por defecto se usa la por defecto`() {
        assertEquals(30, durationForGap(defaultMinutes = 30, gapMinutes = 120))
    }

    @Test
    fun `si el hueco es mas corto se usa el hueco`() {
        assertEquals(45, durationForGap(defaultMinutes = 60, gapMinutes = 45))
    }

    @Test
    fun `nunca baja de la duracion minima de un bloque`() {
        assertEquals(MIN_BLOCK_DURATION_MINUTES, durationForGap(defaultMinutes = 60, gapMinutes = 2))
    }
}
