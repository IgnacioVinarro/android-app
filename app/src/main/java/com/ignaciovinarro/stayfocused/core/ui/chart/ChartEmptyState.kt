package com.ignaciovinarro.stayfocused.core.ui.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ignaciovinarro.stayfocused.R

internal val ChartHeight = 180.dp

// Estado vacío compartido por todos los gráficos: ocupa el mismo alto que el gráfico para que la pantalla no
// salte cuando llegan los primeros datos.
@Composable
fun ChartEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(ChartHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.chart_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
