package com.ignaciovinarro.stayfocused.core.timeline

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineGridTest {

    @Test
    fun `snapToGrid redondea al multiplo de 5 mas cercano`() {
        assertEquals(100, snapToGrid(102))
        assertEquals(105, snapToGrid(103))
        assertEquals(60, snapToGrid(60))
    }

    @Test
    fun `clampStartMinutes no deja que el bloque empiece antes de medianoche`() {
        assertEquals(0, clampStartMinutes(-30, 60))
    }

    @Test
    fun `clampStartMinutes no deja que el bloque termine despues de medianoche`() {
        assertEquals(1380, clampStartMinutes(1400, 60)) // 1440 - 60
    }

    @Test
    fun `clampStartMinutes conserva un valor valido`() {
        assertEquals(540, clampStartMinutes(540, 60))
    }

    @Test
    fun `clampDurationMinutes respeta la duracion minima`() {
        assertEquals(MIN_BLOCK_DURATION_MINUTES, clampDurationMinutes(1, 540))
    }

    @Test
    fun `clampDurationMinutes no deja que el bloque termine despues de medianoche`() {
        assertEquals(60, clampDurationMinutes(120, 1380)) // 1440 - 1380
    }

    @Test
    fun `clampDurationMinutes conserva un valor valido`() {
        assertEquals(90, clampDurationMinutes(90, 540))
    }
}
