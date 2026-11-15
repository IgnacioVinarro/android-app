package com.ignaciovinarro.stayfocused.core.database

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    @Test
    fun `fromLocalDate y toLocalDate son inversas`() {
        val date = LocalDate.of(2026, 3, 5)
        val epochDay = Converters.toLocalDate(date)
        assertEquals(date, Converters.fromLocalDate(epochDay))
    }

    @Test
    fun `fromLocalDate con null devuelve null`() {
        assertNull(Converters.fromLocalDate(null))
    }

    @Test
    fun `fromLocalDateTime y toLocalDateTime son inversas al segundo`() {
        val dateTime = LocalDateTime.of(2026, 3, 5, 10, 30, 0)
        val epochMillis = Converters.toLocalDateTime(dateTime)
        assertEquals(dateTime, Converters.fromLocalDateTime(epochMillis))
    }
}
