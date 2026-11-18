package com.ignaciovinarro.stayfocused.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DateFormatTest {

    @Test
    fun `formatea dia mes y anio con ceros`() {
        assertEquals("05/11/2026", formatShortDate(LocalDate.of(2026, 11, 5)))
        assertEquals("31/12/2027", formatShortDate(LocalDate.of(2027, 12, 31)))
    }

    @Test
    fun `formatea mes y anio en espanol con mayuscula inicial`() {
        assertEquals("Noviembre 2026", formatMonthYear(YearMonth.of(2026, 11)))
        assertEquals("Enero 2027", formatMonthYear(YearMonth.of(2027, 1)))
    }

    @Test
    fun `abrevia el mes a tres letras`() {
        assertEquals("Nov", formatMonthShort(YearMonth.of(2026, 11)))
        assertEquals("Mar", formatMonthShort(YearMonth.of(2027, 3)))
    }
}
