package com.ignaciovinarro.stayfocused.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ignaciovinarro.stayfocused.R

private const val MINUTES_IN_HOUR = 60

// "45 min", "2 h" o "1 h 30 min". Compartido por la vista semanal y los huecos libres de la línea de
// tiempo (los textos viven en strings.xml, así que va como Composable).
@Composable
fun formatDuration(minutes: Int): String {
    val hours = minutes / MINUTES_IN_HOUR
    val rest = minutes % MINUTES_IN_HOUR
    return when {
        hours == 0 -> stringResource(R.string.duration_minutes, rest)
        rest == 0 -> stringResource(R.string.duration_hours, hours)
        else -> stringResource(R.string.duration_hours_minutes, hours, rest)
    }
}
