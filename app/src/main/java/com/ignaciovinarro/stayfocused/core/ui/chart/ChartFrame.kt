package com.ignaciovinarro.stayfocused.core.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ignaciovinarro.stayfocused.core.util.ChartScale
import com.ignaciovinarro.stayfocused.core.util.niceScale

private val AxisGap = 8.dp

// Zona donde cada gráfico dibuja sus datos: posición, tamaño y cómo pasar un valor/índice a coordenadas.
internal class PlotArea(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val scale: ChartScale,
    val count: Int,
) {
    // Ancho de la "ranura" de cada dato (alineada con su etiqueta del eje X)
    val slot: Float get() = width / count

    fun xCenter(index: Int): Float = left + slot * index + slot / 2
    fun y(value: Double): Float = top + height * (1f - scale.fraction(value))
}

// Marco común de los gráficos (norma de reutilizar): eje Y con marcas redondas, líneas guía, etiquetas del eje X,
// estado vacío y descripción para TalkBack. Cada gráfico solo aporta `drawPlot`, lo que dibuja con los datos.
// `includeZero`: columnas = true (el eje arranca en 0); línea = false (se ajusta al rango de los datos).
@Composable
internal fun ChartFrame(
    entries: List<ChartEntry>,
    summary: String,
    includeZero: Boolean,
    modifier: Modifier,
    valueFormat: (Double) -> String,
    drawPlot: DrawScope.(PlotArea) -> Unit,
) {
    if (entries.isEmpty()) {
        ChartEmptyState(modifier)
        return
    }

    val scale = remember(entries, includeZero) { niceScale(entries.map { it.value }, includeZero) }
    val ticks = remember(scale) { scale.ticks() }
    val tickLabels = remember(ticks, valueFormat) { ticks.map(valueFormat) }

    val axisStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val labelsWidthPx = remember(tickLabels, axisStyle) {
        tickLabels.maxOf { measurer.measure(it, axisStyle).size.width }
    }
    val labelsWidth = with(LocalDensity.current) { labelsWidthPx.toDp() }
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = summary },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .clearAndSetSemantics { },
        ) {
            val plotLeft = labelsWidthPx + AxisGap.toPx()
            // Margen arriba y abajo para que la mitad de la etiqueta de la marca extrema no se corte
            val inset = measurer.measure(tickLabels.last(), axisStyle).size.height / 2f
            val plot = PlotArea(plotLeft, inset, size.width - plotLeft, size.height - inset * 2, scale, entries.size)

            ticks.forEachIndexed { i, tick ->
                val y = plot.y(tick)
                drawLine(gridColor, Offset(plotLeft, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                val label = measurer.measure(tickLabels[i], axisStyle)
                drawText(label, topLeft = Offset(labelsWidthPx - label.size.width.toFloat(), y - label.size.height / 2f))
            }

            drawPlot(plot)
        }

        // Etiquetas del eje X: una por dato, alineadas con la ranura de cada uno
        Row(modifier = Modifier.fillMaxWidth().padding(start = labelsWidth + AxisGap).clearAndSetSemantics { }) {
            entries.forEach { entry ->
                Text(
                    text = entry.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
