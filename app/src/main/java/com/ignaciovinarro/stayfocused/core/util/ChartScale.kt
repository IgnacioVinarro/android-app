package com.ignaciovinarro.stayfocused.core.util

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private const val DEFAULT_TARGET_TICKS = 4
private const val NICE_1 = 1.0
private const val NICE_2 = 2.0
private const val NICE_5 = 5.0
private const val NICE_10 = 10.0
private const val NICE_1_LIMIT = 1.5
private const val NICE_2_LIMIT = 3.0
private const val NICE_5_LIMIT = 7.0

// Función pura: escala del eje Y de un gráfico. `min` y `max` son los extremos "lindos" del eje (múltiplos de
// `step`), así las marcas caen en números redondos (0, 50, 100 y no 0, 37, 74). Se usa en línea y columnas.
data class ChartScale(val min: Double, val max: Double, val step: Double) {

    // Marcas del eje, de abajo hacia arriba (incluye min y max).
    fun ticks(): List<Double> {
        val count = Math.round((max - min) / step).toInt()
        return (0..count).map { min + it * step }
    }

    // Posición relativa de un valor: 0f = borde inferior, 1f = borde superior. Se acota a 0..1.
    fun fraction(value: Double): Float =
        (((value - min) / (max - min)).coerceIn(0.0, 1.0)).toFloat()
}

// Calcula la escala para los valores dados. Sin valores, o todos cero: eje 0..1 (el gráfico vacío igual se dibuja).
// Con `includeZero` (columnas) el eje siempre arranca en 0; sin él (línea) se ajusta al rango de los datos.
fun niceScale(values: List<Double>, includeZero: Boolean, targetTicks: Int = DEFAULT_TARGET_TICKS): ChartScale {
    if (values.isEmpty()) return ChartScale(min = 0.0, max = 1.0, step = 1.0)
    var low = values.min()
    var high = values.max()
    if (includeZero) {
        low = minOf(low, 0.0)
        high = maxOf(high, 0.0)
    }
    if (low == high) {
        // Todos iguales: se abre un rango alrededor del valor (o 0..1 si es cero) para no dividir por cero.
        if (low == 0.0) return ChartScale(min = 0.0, max = 1.0, step = 1.0)
        val pad = kotlin.math.abs(low) * 0.1
        low -= pad
        high += pad
    }
    val step = niceStep((high - low) / targetTicks.coerceAtLeast(1))
    return ChartScale(
        min = floor(low / step) * step,
        max = ceil(high / step) * step,
        step = step,
    )
}

// Redondea un paso crudo al siguiente 1, 2, 5 o 10 × potencia de 10 (el método clásico de ejes "lindos").
private fun niceStep(rawStep: Double): Double {
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val fraction = rawStep / magnitude
    val nice = when {
        fraction <= NICE_1_LIMIT -> NICE_1
        fraction <= NICE_2_LIMIT -> NICE_2
        fraction <= NICE_5_LIMIT -> NICE_5
        else -> NICE_10
    }
    return nice * magnitude
}
