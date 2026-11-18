package com.ignaciovinarro.stayfocused.core.ui.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartEntryTest {

    @Test
    fun `un entero se muestra sin decimales`() {
        assertEquals("40", defaultChartValueFormat(40.0))
        assertEquals("0", defaultChartValueFormat(0.0))
        assertEquals("-30", defaultChartValueFormat(-30.0))
    }

    @Test
    fun `un valor con fraccion se muestra con un decimal`() {
        assertEquals(1, defaultChartValueFormat(71.5).count { it == '5' } )
        assertEquals(true, defaultChartValueFormat(71.5).startsWith("71"))
    }
}
