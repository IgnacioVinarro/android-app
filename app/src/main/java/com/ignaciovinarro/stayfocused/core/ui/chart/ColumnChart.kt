package com.ignaciovinarro.stayfocused.core.ui.chart

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val BarCorner = 4.dp
private const val BAR_WIDTH_FRACTION = 0.6f

// Gráfico de columnas (volumen semanal, gasto por mes, tiempo de pantalla por día, calorías). Dibujado con Canvas
// (ticket 0027): sin librería. El eje Y siempre arranca en 0. `summary` es el texto que lee TalkBack (ej. "Volumen
// semanal: lunes 1200, martes 0…"): el dibujo no es accesible por sí solo, por eso quien lo usa tiene que darlo.
@Composable
fun ColumnChart(
    entries: List<ChartEntry>,
    summary: String,
    modifier: Modifier = Modifier,
    valueFormat: (Double) -> String = ::defaultChartValueFormat,
) {
    val barColor = MaterialTheme.colorScheme.primary
    ChartFrame(entries, summary, includeZero = true, modifier, valueFormat) { plot ->
        val barWidth = plot.slot * BAR_WIDTH_FRACTION
        val zeroY = plot.y(0.0)
        entries.forEachIndexed { i, entry ->
            val valueY = plot.y(entry.value)
            // Desde la línea del cero hasta el valor (hacia abajo si es negativo); mínimo 1 dp para que el cero se vea
            drawRoundRect(
                color = barColor,
                topLeft = Offset(plot.xCenter(i) - barWidth / 2, minOf(valueY, zeroY)),
                size = Size(barWidth, abs(zeroY - valueY).coerceAtLeast(1.dp.toPx())),
                cornerRadius = CornerRadius(BarCorner.toPx()),
            )
        }
    }
}
