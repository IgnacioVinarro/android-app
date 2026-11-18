package com.ignaciovinarro.stayfocused.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownTest {

    @Test
    fun `cero milisegundos es 00 00`() {
        assertEquals("00:00", formatCountdown(0))
    }

    @Test
    fun `un valor negativo se muestra como 00 00`() {
        assertEquals("00:00", formatCountdown(-5_000))
    }

    @Test
    fun `redondea los segundos hacia arriba`() {
        assertEquals("00:01", formatCountdown(1))
        assertEquals("00:01", formatCountdown(1_000))
        assertEquals("00:02", formatCountdown(1_001))
    }

    @Test
    fun `muestra minutos y segundos con dos digitos`() {
        assertEquals("25:00", formatCountdown(25 * 60_000L))
        assertEquals("09:05", formatCountdown(9 * 60_000L + 5_000))
    }

    @Test
    fun `un minuto menos un milisegundo se muestra como un minuto`() {
        assertEquals("01:00", formatCountdown(59_001))
    }

    @Test
    fun `pasada una hora muestra horas`() {
        assertEquals("1:00:00", formatCountdown(60 * 60_000L))
        assertEquals("2:03:04", formatCountdown((2 * 3600 + 3 * 60 + 4) * 1000L))
    }

    @Test
    fun `59 minutos 59 segundos todavia no muestra horas`() {
        assertEquals("59:59", formatCountdown((59 * 60 + 59) * 1000L))
    }
}
