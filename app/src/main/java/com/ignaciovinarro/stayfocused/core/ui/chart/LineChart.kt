package com.ignaciovinarro.stayfocused.core.ui.chart

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private val LineWidth = 2.dp
private val PointRadius = 4.dp

// Gráfico de línea (progreso en el tiempo: peso, 1RM, peso corporal). Mismo marco que ColumnChart. El eje Y se
// ajusta al rango de los datos (no fuerza el cero: así se ven los cambios chicos). Cada dato lleva un punto, así
// con un solo dato igual se ve algo y la línea no es lo único que lo marca. `summary`: texto para TalkBack.
@Composable
fun LineChart(
    entries: List<ChartEntry>,
    summary: String,
    modifier: Modifier = Modifier,
    valueFormat: (Double) -> String = ::defaultChartValueFormat,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    ChartFrame(entries, summary, includeZero = false, modifier, valueFormat) { plot ->
        if (entries.size > 1) {
            val path = Path()
            entries.forEachIndexed { i, entry ->
                val x = plot.xCenter(i)
                val y = plot.y(entry.value)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = LineWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        entries.forEachIndexed { i, entry ->
            drawCircle(lineColor, PointRadius.toPx(), Offset(plot.xCenter(i), plot.y(entry.value)))
        }
    }
}
