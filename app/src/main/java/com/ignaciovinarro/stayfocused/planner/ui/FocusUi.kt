package com.ignaciovinarro.stayfocused.planner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.ui.ReminderPermissionPrompt
import com.ignaciovinarro.stayfocused.core.util.formatCountdown
import com.ignaciovinarro.stayfocused.planner.domain.FOCUS_PRESET_MINUTES
import com.ignaciovinarro.stayfocused.planner.domain.MAX_FOCUS_MINUTES
import com.ignaciovinarro.stayfocused.planner.domain.MIN_FOCUS_MINUTES
import com.ignaciovinarro.stayfocused.planner.domain.isValidFocusMinutes

// Barra de la sesión de enfoque en curso (PL-13): título, tiempo restante y pausar/reanudar/detener.
// Solo dibuja; los toques los resuelve el ViewModel. "En pausa" va en texto, no solo con el ícono.
@Composable
fun FocusBar(
    state: FocusUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = state.session ?: return
    val time = formatCountdown(state.remainingMs)
    val description = if (state.isPaused) {
        stringResource(R.string.a11y_focus_bar_paused, session.taskTitle, time)
    } else {
        stringResource(R.string.a11y_focus_bar, session.taskTitle, time)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
                    // El título y el tiempo se leen juntos con TalkBack; el tiempo cambia cada segundo,
                    // así que no se anuncia solo (sin liveRegion).
                    .semantics(mergeDescendants = true) { contentDescription = description }
            ) {
                Text(session.taskTitle, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(time, style = MaterialTheme.typography.headlineSmall)
                if (state.isPaused) {
                    Text(stringResource(R.string.focus_bar_paused), style = MaterialTheme.typography.labelMedium)
                }
            }
            if (state.isPaused) {
                IconButton(onClick = onResume) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.a11y_focus_resume))
                }
            } else {
                IconButton(onClick = onPause) {
                    Icon(Icons.Filled.Pause, contentDescription = stringResource(R.string.a11y_focus_pause))
                }
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.a11y_focus_stop))
            }
        }
    }
}

// Diálogo para elegir cuánto enfocar: 25, 50 o "Otra" (minutos a mano, entre MIN y MAX). "Iniciar" queda
// deshabilitado si la duración no es válida o si faltan permisos de avisos (el aviso explica cuál).
@Composable
fun FocusStartDialog(
    taskTitle: String,
    startFailed: Boolean,
    onStart: (minutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    // `selected` = minutos del preajuste elegido, o CUSTOM para escribir los minutos.
    var selected by rememberSaveable { mutableIntStateOf(FOCUS_PRESET_MINUTES.first()) }
    var customText by rememberSaveable { mutableStateOf("") }
    var missingPermission by rememberSaveable { mutableStateOf(false) }

    val minutes = if (selected == CUSTOM) customText.toIntOrNull() else selected
    val customInvalid = selected == CUSTOM && customText.isNotEmpty() && minutes?.let(::isValidFocusMinutes) != true
    val canStart = minutes != null && isValidFocusMinutes(minutes) && !missingPermission

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.focus_dialog_title, taskTitle)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FOCUS_PRESET_MINUTES.forEach { preset ->
                        FilterChip(
                            selected = selected == preset,
                            onClick = { selected = preset },
                            label = { Text(stringResource(R.string.focus_dialog_minutes_option, preset)) }
                        )
                    }
                    FilterChip(
                        selected = selected == CUSTOM,
                        onClick = { selected = CUSTOM },
                        label = { Text(stringResource(R.string.focus_dialog_custom_option)) }
                    )
                }
                if (selected == CUSTOM) {
                    OutlinedTextField(
                        value = customText,
                        // Solo dígitos y hasta 3 (el máximo tiene 3): evita números enormes.
                        onValueChange = { text -> customText = text.filter(Char::isDigit).take(3) },
                        label = { Text(stringResource(R.string.focus_dialog_custom_label)) },
                        isError = customInvalid,
                        supportingText = if (customInvalid) {
                            { Text(stringResource(R.string.focus_dialog_custom_error, MIN_FOCUS_MINUTES, MAX_FOCUS_MINUTES)) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (startFailed) {
                    Text(
                        stringResource(R.string.focus_start_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ReminderPermissionPrompt(onMissingPermissionChange = { missingPermission = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { minutes?.let(onStart) }, enabled = canStart) {
                Text(stringResource(R.string.focus_dialog_start))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.focus_dialog_cancel)) }
        }
    )
}

// Marca para "Otra" en el selector (los preajustes son minutos positivos, así que 0 no choca).
private const val CUSTOM = 0
