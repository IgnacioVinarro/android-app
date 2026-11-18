package com.ignaciovinarro.stayfocused.core.ui.chart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ignaciovinarro.stayfocused.core.ui.theme.StayFocusedTheme

// Previsualizaciones para la comprobación manual del ticket 0027: claro/oscuro y los casos borde
// (sin datos, un solo dato, todos iguales, ceros y negativos).

private val week = listOf("L", "M", "X", "J", "V", "S", "D")

private fun entries(vararg values: Double) = values.mapIndexed { i, v -> ChartEntry(week[i], v) }

@Composable
private fun ChartPreviewContent(darkTheme: Boolean) {
    StayFocusedTheme(darkTheme = darkTheme) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ColumnChart(entries(1200.0, 0.0, 800.0, 1500.0, 950.0, 0.0, 400.0), summary = "Columnas")
                LineChart(
                    entries(72.5, 73.1, 71.8, 72.0, 71.2, 70.9, 70.5),
                    summary = "Línea",
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Preview(name = "Gráficos – claro", showBackground = true)
@Composable
private fun ChartsLightPreview() = ChartPreviewContent(darkTheme = false)

@Preview(name = "Gráficos – oscuro", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ChartsDarkPreview() = ChartPreviewContent(darkTheme = true)

@Preview(name = "Casos borde", showBackground = true)
@Composable
private fun ChartsEdgeCasesPreview() {
    StayFocusedTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ColumnChart(emptyList(), summary = "Vacío")
                ColumnChart(entries(500.0), summary = "Un solo dato", modifier = Modifier.padding(top = 16.dp))
                ColumnChart(entries(0.0, 0.0, 0.0), summary = "Ceros", modifier = Modifier.padding(top = 16.dp))
                ColumnChart(entries(-30.0, 20.0, 10.0), summary = "Negativos", modifier = Modifier.padding(top = 16.dp))
                LineChart(entries(50.0), summary = "Un punto", modifier = Modifier.padding(top = 16.dp))
                LineChart(entries(50.0, 50.0, 50.0), summary = "Todos iguales", modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}
