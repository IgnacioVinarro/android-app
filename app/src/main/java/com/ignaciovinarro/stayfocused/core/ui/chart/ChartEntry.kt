package com.ignaciovinarro.stayfocused.core.ui.chart

// Un dato de un gráfico: la etiqueta del eje X ("L", "Ene", "12/3") y su valor. Lo usan ColumnChart y LineChart
// (y lo arman los módulos: gimnasio, finanzas, tiempo de pantalla). El valor es Double solo para dibujar; los
// montos (centavos) y pesos (gramos) siguen siendo Long en la base y se convierten al armar la lista.
data class ChartEntry(val label: String, val value: Double)

// Formato por defecto de las marcas del eje Y: sin decimales si el número es entero ("40"), con uno si no ("0,5").
fun defaultChartValueFormat(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)
