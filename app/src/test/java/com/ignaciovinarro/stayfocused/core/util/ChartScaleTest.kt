package com.ignaciovinarro.stayfocused.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartScaleTest {

    private val delta = 1e-9

    @Test
    fun `columnas arrancan en cero y el maximo es multiplo del paso`() {
        val scale = niceScale(listOf(0.0, 37.0, 74.0), includeZero = true)

        assertEquals(0.0, scale.min, delta)
        assertEquals(80.0, scale.max, delta)
        assertEquals(20.0, scale.step, delta)
        assertEquals(listOf(0.0, 20.0, 40.0, 60.0, 80.0), scale.ticks())
    }

    @Test
    fun `columnas con todos positivos incluyen el cero aunque el minimo sea alto`() {
        val scale = niceScale(listOf(500.0, 900.0), includeZero = true)

        assertEquals(0.0, scale.min, delta)
        assertTrue(scale.max >= 900.0)
    }

    @Test
    fun `linea se ajusta al rango de los datos sin forzar el cero`() {
        val scale = niceScale(listOf(72.5, 73.1, 71.8), includeZero = false)

        assertEquals(71.5, scale.min, delta)
        assertEquals(73.5, scale.max, delta)
        assertEquals(0.5, scale.step, delta)
    }

    @Test
    fun `el eje siempre contiene todos los valores`() {
        val values = listOf(3.0, 1_234.5, 87.0, 640.0)
        listOf(true, false).forEach { includeZero ->
            val scale = niceScale(values, includeZero)
            assertTrue(scale.min <= values.min())
            assertTrue(scale.max >= values.max())
        }
    }

    @Test
    fun `sin valores devuelve un eje de cero a uno`() {
        val scale = niceScale(emptyList(), includeZero = true)

        assertEquals(0.0, scale.min, delta)
        assertEquals(1.0, scale.max, delta)
        assertEquals(listOf(0.0, 1.0), scale.ticks())
    }

    @Test
    fun `todos los valores en cero devuelve un eje de cero a uno`() {
        val scale = niceScale(listOf(0.0, 0.0), includeZero = false)

        assertEquals(0.0, scale.min, delta)
        assertEquals(1.0, scale.max, delta)
    }

    @Test
    fun `valores todos iguales en linea abre un rango alrededor sin dividir por cero`() {
        val scale = niceScale(listOf(50.0, 50.0, 50.0), includeZero = false)

        assertEquals(44.0, scale.min, delta)
        assertEquals(56.0, scale.max, delta)
        assertTrue(scale.max > scale.min)
    }

    @Test
    fun `valor unico en columnas va de cero al valor`() {
        val scale = niceScale(listOf(50.0), includeZero = true)

        assertEquals(0.0, scale.min, delta)
        assertEquals(50.0, scale.max, delta)
    }

    @Test
    fun `valores negativos incluyen el cero en columnas`() {
        val scale = niceScale(listOf(-30.0, 20.0), includeZero = true)

        assertEquals(-30.0, scale.min, delta)
        assertEquals(20.0, scale.max, delta)
        assertTrue(scale.ticks().contains(0.0))
    }

    @Test
    fun `fraction mapea el valor a la posicion relativa y acota los extremos`() {
        val scale = ChartScale(min = 0.0, max = 80.0, step = 20.0)

        assertEquals(0f, scale.fraction(0.0), 0f)
        assertEquals(0.5f, scale.fraction(40.0), 0f)
        assertEquals(1f, scale.fraction(80.0), 0f)
        assertEquals(0f, scale.fraction(-5.0), 0f)
        assertEquals(1f, scale.fraction(100.0), 0f)
    }

    @Test
    fun `montos grandes en centavos tambien dan marcas redondas`() {
        val scale = niceScale(listOf(0.0, 1_234_500.0), includeZero = true)

        assertEquals(0.0, scale.min, delta)
        assertEquals(1_500_000.0, scale.max, 1.0)
        assertEquals(500_000.0, scale.step, 1.0)
    }
}
