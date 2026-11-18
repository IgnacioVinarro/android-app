package com.ignaciovinarro.stayfocused.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    // --- filterMoneyInput ---

    @Test
    fun `el filtro deja digitos punto y coma`() {
        assertEquals("12.34,50", filterMoneyInput("1a2.3b4,50$ "))
    }

    @Test
    fun `el filtro deja el signo menos solo si se pide`() {
        assertEquals("100", filterMoneyInput("-100"))
        assertEquals("-100", filterMoneyInput("-100", allowNegative = true))
    }

    // --- formatArs ---

    @Test
    fun `formatea con miles y decimales`() {
        assertEquals("$ 1.234,56", formatArs(123_456))
        assertEquals("$ 1.234.567,00", formatArs(123_456_700))
    }

    @Test
    fun `formatea cero y montos chicos`() {
        assertEquals("$ 0,00", formatArs(0))
        assertEquals("$ 0,05", formatArs(5))
        assertEquals("$ 12,30", formatArs(1_230))
        assertEquals("$ 999,99", formatArs(99_999))
    }

    @Test
    fun `formatea negativos con signo adelante`() {
        assertEquals("-$ 1.234,56", formatArs(-123_456))
        assertEquals("-$ 0,01", formatArs(-1))
    }

    // --- formatArsForInput ---

    @Test
    fun `para el campo de texto no lleva miles y omite los centavos en cero`() {
        assertEquals("1500", formatArsForInput(150_000))
        assertEquals("1500,50", formatArsForInput(150_050))
        assertEquals("0,05", formatArsForInput(5))
        assertEquals("-200", formatArsForInput(-20_000))
        assertEquals("0", formatArsForInput(0))
    }

    // --- parseArsToCents ---

    @Test
    fun `interpreta enteros y decimales`() {
        assertEquals(123_400L, parseArsToCents("1234"))
        assertEquals(123_450L, parseArsToCents("1234,5"))
        assertEquals(123_456L, parseArsToCents("1234,56"))
        assertEquals(0L, parseArsToCents("0"))
    }

    @Test
    fun `interpreta el punto como separador de miles`() {
        assertEquals(123_456L, parseArsToCents("1.234,56"))
        assertEquals(123_400L, parseArsToCents("1.234"))
        assertEquals(123_456_700L, parseArsToCents("1.234.567"))
    }

    @Test
    fun `ignora espacios y el signo pesos`() {
        assertEquals(150_000L, parseArsToCents("  $ 1500 "))
    }

    @Test
    fun `rechaza texto invalido`() {
        assertNull(parseArsToCents(""))
        assertNull(parseArsToCents("abc"))
        assertNull(parseArsToCents("12,345")) // 3 decimales
        assertNull(parseArsToCents("12.5")) // punto que no separa miles
        assertNull(parseArsToCents("1,2,3"))
        assertNull(parseArsToCents("1.23.456"))
        assertNull(parseArsToCents(",50"))
    }

    @Test
    fun `los negativos solo se aceptan si se pide`() {
        assertNull(parseArsToCents("-100"))
        assertEquals(-10_000L, parseArsToCents("-100", allowNegative = true))
        assertEquals(-12_350L, parseArsToCents("-123,5", allowNegative = true))
    }

    @Test
    fun `rechaza montos demasiado grandes`() {
        assertNull(parseArsToCents("1".repeat(13)))
        assertEquals(99_999_999_999_900L, parseArsToCents("999999999999"))
    }

    @Test
    fun `ida y vuelta entre formatArsForInput y parseArsToCents`() {
        listOf(0L, 5L, 150_050L, 150_000L, 123_456_789L).forEach {
            assertEquals(it, parseArsToCents(formatArsForInput(it)))
        }
        assertEquals(-20_050L, parseArsToCents(formatArsForInput(-20_050L), allowNegative = true))
    }
}
