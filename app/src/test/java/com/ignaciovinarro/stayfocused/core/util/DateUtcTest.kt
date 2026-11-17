package com.ignaciovinarro.stayfocused.core.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtcTest {

    @Test
    fun `un dia a las cero UTC se convierte en milisegundos conocidos`() {
        // 1970-01-02 00:00 UTC = 86.400.000 ms
        assertEquals(86_400_000L, LocalDate.of(1970, 1, 2).toEpochMillisUtc())
    }

    @Test
    fun `convertir a milisegundos y volver devuelve el mismo dia`() {
        val fechas = listOf(
            LocalDate.of(2026, 3, 5),
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2026, 12, 31),
            LocalDate.of(2026, 1, 1)
        )

        fechas.forEach { assertEquals(it, it.toEpochMillisUtc().toLocalDateUtc()) }
    }

    @Test
    fun `cualquier hora del mismo dia UTC da la misma fecha`() {
        val mediaNoche = LocalDate.of(2026, 3, 5).toEpochMillisUtc()

        assertEquals(LocalDate.of(2026, 3, 5), (mediaNoche + 23 * 3_600_000L).toLocalDateUtc())
        assertEquals(LocalDate.of(2026, 3, 4), (mediaNoche - 1L).toLocalDateUtc())
    }
}
