package com.ignaciovinarro.stayfocused.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateFormatTest {

    @Test
    fun `formatea dia mes y anio con ceros`() {
        assertEquals("05/11/2026", formatShortDate(LocalDate.of(2026, 11, 5)))
        assertEquals("31/12/2027", formatShortDate(LocalDate.of(2027, 12, 31)))
    }
}
