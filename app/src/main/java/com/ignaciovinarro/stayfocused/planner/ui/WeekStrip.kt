package com.ignaciovinarro.stayfocused.planner.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.ui.formatDuration
import com.ignaciovinarro.stayfocused.planner.domain.DayLoad
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val DAY_CELL_MIN_HEIGHT = 64.dp
private val LOAD_BAR_HEIGHT = 4.dp

// Vista semanal (PL-12): flechas de semana, 7 días (lunes a domingo) con su carga y, abajo, el resumen
// del día elegido. Es solo dibujo: recibe el estado y avisa los toques (los composables no deciden nada).
@Composable
fun WeekStrip(
    week: List<DayLoad>,
    selectedDate: LocalDate,
    today: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onGoToToday: () -> Unit,
    modifier: Modifier = Modifier,
    // Acción opcional al final de la fila del resumen (ej. el engranaje de Ajustes, que pone la pantalla).
    trailingAction: @Composable () -> Unit = {}
) {
    // Sin datos todavía (primer cuadro) no hay nada que dibujar.
    if (week.isEmpty()) return
    val locale = Locale.getDefault()
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale) }
    val selectedLoad = week.firstOrNull { it.date == selectedDate }

    Column(modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousWeek) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.week_previous))
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { load ->
                    DayCell(
                        load = load,
                        selected = load.date == selectedDate,
                        isToday = load.date == today,
                        locale = locale,
                        dateFormat = dateFormat,
                        onClick = { onSelectDay(load.date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            IconButton(onClick = onNextWeek) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.week_next))
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(selectedDate.format(dateFormat), style = MaterialTheme.typography.labelLarge)
                if (selectedLoad != null) {
                    Text(loadSummary(selectedLoad), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (selectedDate != today) {
                TextButton(onClick = onGoToToday) { Text(stringResource(R.string.week_go_today)) }
            }
            trailingAction()
        }
    }
}

@Composable
private fun DayCell(
    load: DayLoad,
    selected: Boolean,
    isToday: Boolean,
    locale: Locale,
    dateFormat: DateTimeFormatter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayMarker = stringResource(R.string.week_today_marker)
    val selectedMarker = stringResource(R.string.week_selected_marker)
    val summary = loadSummary(load)
    val description = buildString {
        append(stringResource(R.string.a11y_week_day, load.date.format(dateFormat), summary))
        if (isToday) append(", ").append(todayMarker)
        if (selected) append(", ").append(selectedMarker)
    }
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    // Hoy se marca con borde (no solo con color); el elegido, con fondo y negrita.
    val border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null

    Surface(
        onClick = onClick,
        color = container,
        contentColor = content,
        border = border,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
            .heightIn(min = DAY_CELL_MIN_HEIGHT)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
    ) {
        Column(
            Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val weight = if (selected) FontWeight.Bold else FontWeight.Normal
            Text(
                load.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = weight
            )
            Text(load.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = weight)
            Text(
                load.taskCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = weight,
                textAlign = TextAlign.Center
            )
            LoadBar(fraction = load.fraction)
        }
    }
}

// Barra proporcional al día más cargado de la semana. El número de tareas va siempre en texto: la barra
// es un apoyo visual, no la única señal.
@Composable
private fun LoadBar(fraction: Float) {
    Box(
        Modifier
            .padding(horizontal = 6.dp)
            .fillMaxWidth()
            .height(LOAD_BAR_HEIGHT)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(LOAD_BAR_HEIGHT)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// "4 tareas · 3 h" (o "Sin tareas"). Los minutos solo se muestran si hay algo con hora.
@Composable
private fun loadSummary(load: DayLoad): String {
    if (load.taskCount == 0) return stringResource(R.string.week_load_empty)
    val tasks = pluralStringResource(R.plurals.week_task_count, load.taskCount, load.taskCount)
    if (load.plannedMinutes == 0) return tasks
    return stringResource(R.string.week_load_summary, tasks, formatDuration(load.plannedMinutes))
}
