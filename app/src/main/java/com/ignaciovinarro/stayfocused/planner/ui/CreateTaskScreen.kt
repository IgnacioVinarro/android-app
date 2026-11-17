package com.ignaciovinarro.stayfocused.planner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.ui.TaskColorSelector
import com.ignaciovinarro.stayfocused.core.ui.TaskIconSelector
import com.ignaciovinarro.stayfocused.core.ui.TimePickerDialog
import com.ignaciovinarro.stayfocused.planner.domain.EditScope
import com.ignaciovinarro.stayfocused.planner.domain.MAX_REMINDER_MINUTES_BEFORE
import com.ignaciovinarro.stayfocused.planner.domain.PRESET_REMINDER_MINUTES
import com.ignaciovinarro.stayfocused.planner.domain.RecurrenceType
import com.ignaciovinarro.stayfocused.core.ui.AppDatePickerDialog
import com.ignaciovinarro.stayfocused.core.ui.ReminderPermissionPrompt
import com.ignaciovinarro.stayfocused.planner.domain.ReminderUnit
import com.ignaciovinarro.stayfocused.planner.domain.SubtaskDraft
import com.ignaciovinarro.stayfocused.planner.domain.dayOfWeekBit
import com.ignaciovinarro.stayfocused.planner.domain.isValidReminderMinutes
import com.ignaciovinarro.stayfocused.planner.domain.reminderAmount
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateTaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRecurrenceEndPicker by remember { mutableStateOf(false) }
    var showCustomReminderDialog by remember { mutableStateOf(false) }

    val isEditing = uiState.taskId != null
    val isEditingRecurring = isEditing && uiState.isRecurringTask

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (isEditing) R.string.edit_task_title else R.string.create_task_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { viewModel.delete(onDeleted = onDone) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text(stringResource(R.string.create_task_field_title)) },
                modifier = Modifier.fillMaxWidth()
            )

            if (isEditingRecurring) {
                Text("${stringResource(R.string.create_task_field_date)}: ${uiState.date}")
            } else {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${stringResource(R.string.create_task_field_date)}: ${uiState.date}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.create_task_field_no_time))
                    Switch(
                        checked = !uiState.hasStartTime,
                        onCheckedChange = { checked -> viewModel.onHasStartTimeChange(!checked) }
                    )
                }
            }

            if (uiState.hasStartTime) {
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${stringResource(R.string.create_task_field_start_time)}: " +
                            "${uiState.startTime.hour.toString().padStart(2, '0')}:" +
                            uiState.startTime.minute.toString().padStart(2, '0')
                    )
                }
            }

            OutlinedTextField(
                value = uiState.durationMinutes.toString(),
                onValueChange = { value -> value.toIntOrNull()?.let(viewModel::onDurationChange) },
                label = { Text(stringResource(R.string.create_task_field_duration)) },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            if (isEditingRecurring) {
                Text(stringResource(R.string.edit_task_recurrence_notice), style = MaterialTheme.typography.bodyMedium)
            } else if (uiState.hasStartTime) {
                RecurrenceSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    onShowRecurrenceEndPicker = { showRecurrenceEndPicker = true }
                )
            }

            // Recordatorios (PL-10): solo con hora de inicio, no hay hora de aviso sin hora de inicio.
            if (uiState.hasStartTime) {
                ReminderSection(
                    selectedMinutes = uiState.reminderMinutes,
                    alarmSound = uiState.alarmSound,
                    onAlarmSoundChange = viewModel::onAlarmSoundChange,
                    showRecurringNotice = isEditingRecurring,
                    onToggle = viewModel::onReminderToggle,
                    onOther = { showCustomReminderDialog = true }
                )
                // Se pide el permiso recién cuando hay un recordatorio elegido, con su explicación.
                if (uiState.reminderMinutes.isNotEmpty()) ReminderPermissionPrompt()
            }

            Text(stringResource(R.string.create_task_field_icon), style = MaterialTheme.typography.labelLarge)
            TaskIconSelector(selectedKey = uiState.iconKey, onSelect = viewModel::onIconChange)

            Text(stringResource(R.string.create_task_field_color), style = MaterialTheme.typography.labelLarge)
            TaskColorSelector(selectedKey = uiState.colorKey, onSelect = viewModel::onColorChange)

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.create_task_field_notes)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Subtareas (PL-04): valen con o sin hora.
            SubtaskSection(
                subtasks = uiState.subtasks,
                showRecurringNotice = isEditingRecurring,
                onAdd = viewModel::onSubtaskAdd,
                onTitleChange = viewModel::onSubtaskTitleChange,
                onRemove = viewModel::onSubtaskRemove
            )

            Button(
                onClick = { viewModel.save(onSaved = onDone) },
                enabled = uiState.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = uiState.date,
            onConfirm = viewModel::onDateChange,
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.startTime.hour,
            initialMinute = uiState.startTime.minute,
            is24Hour = true
        )
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                viewModel.onStartTimeChange(
                    java.time.LocalTime.of(timePickerState.hour, timePickerState.minute)
                )
                showTimePicker = false
            }
        ) { TimePicker(state = timePickerState) }
    }

    if (showRecurrenceEndPicker) {
        AppDatePickerDialog(
            initialDate = uiState.recurrenceEnd,
            onConfirm = viewModel::onRecurrenceEndChange,
            onDismiss = { showRecurrenceEndPicker = false }
        )
    }

    if (showCustomReminderDialog) {
        CustomReminderDialog(
            onConfirm = { minutes ->
                if (minutes !in uiState.reminderMinutes) viewModel.onReminderToggle(minutes)
                showCustomReminderDialog = false
            },
            onDismiss = { showCustomReminderDialog = false }
        )
    }

    uiState.pendingScopeAction?.let { pendingAction ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissScopeDialog,
            title = {
                val titleRes = if (pendingAction == PendingScopeAction.DELETE) {
                    R.string.edit_scope_dialog_title_delete
                } else {
                    R.string.edit_scope_dialog_title_save
                }
                Text(stringResource(titleRes))
            },
            text = {
                Column {
                    TextButton(onClick = { viewModel.onScopeChosen(EditScope.ONLY_THIS) }) {
                        Text(stringResource(R.string.edit_scope_only_this))
                    }
                    TextButton(onClick = { viewModel.onScopeChosen(EditScope.THIS_AND_FOLLOWING) }) {
                        Text(stringResource(R.string.edit_scope_this_and_following))
                    }
                    TextButton(onClick = { viewModel.onScopeChosen(EditScope.ALL) }) {
                        Text(stringResource(R.string.edit_scope_all))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::onDismissScopeDialog) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrenceSection(
    uiState: CreateTaskUiState,
    viewModel: CreateTaskViewModel,
    onShowRecurrenceEndPicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.create_task_recurrence_enable))
            Switch(checked = uiState.recurrenceEnabled, onCheckedChange = viewModel::onRecurrenceEnabledChange)
        }

        if (uiState.recurrenceEnabled) {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RecurrenceType.entries.forEach { type ->
                    RecurrenceTypeRow(
                        type = type,
                        selected = uiState.recurrenceType == type,
                        onClick = { viewModel.onRecurrenceTypeChange(type) }
                    )
                }
            }

            when (uiState.recurrenceType) {
                RecurrenceType.SPECIFIC_DAYS -> {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        daysOfWeekOrdenados().forEach { day ->
                            val selected = (uiState.recurrenceDays and dayOfWeekBit(day)) != 0
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.onRecurrenceDayToggle(day) },
                                label = { Text(dayShortLabel(day)) }
                            )
                        }
                    }
                }
                RecurrenceType.EVERY_N_DAYS -> {
                    OutlinedTextField(
                        value = uiState.recurrenceInterval.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let(viewModel::onRecurrenceIntervalChange) },
                        label = { Text(stringResource(R.string.create_task_recurrence_interval)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                RecurrenceType.DAILY, RecurrenceType.WEEKLY, RecurrenceType.MONTHLY -> Unit
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.create_task_recurrence_has_end))
                Switch(checked = uiState.hasRecurrenceEnd, onCheckedChange = viewModel::onHasRecurrenceEndChange)
            }

            if (uiState.hasRecurrenceEnd) {
                OutlinedButton(onClick = onShowRecurrenceEndPicker, modifier = Modifier.fillMaxWidth()) {
                    Text("${stringResource(R.string.create_task_recurrence_end_date)}: ${uiState.recurrenceEnd}")
                }
            }
        }
    }
}

// Las opciones fijas más los valores libres ya elegidos, de menor a mayor antelación. Un valor
// libre que se destilda desaparece de la lista (no es una opción fija).
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderSection(
    selectedMinutes: List<Int>,
    alarmSound: Boolean,
    onAlarmSoundChange: (Boolean) -> Unit,
    showRecurringNotice: Boolean,
    onToggle: (Int) -> Unit,
    onOther: () -> Unit
) {
    val options = (PRESET_REMINDER_MINUTES + selectedMinutes).distinct().sorted()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.reminders_section_title), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { minutes ->
                val selected = minutes in selectedMinutes
                FilterChip(
                    selected = selected,
                    onClick = { onToggle(minutes) },
                    label = { Text(reminderLabel(minutes)) },
                    // El estado no se indica solo con el color: el tilde también lo marca.
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    }
                )
            }
            FilterChip(
                selected = false,
                onClick = onOther,
                label = { Text(stringResource(R.string.reminder_other)) }
            )
        }
        // PL-11: un interruptor por tarea; solo tiene sentido si ya hay algún recordatorio elegido.
        if (selectedMinutes.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.reminder_alarm_sound))
                    Text(stringResource(R.string.reminder_alarm_sound_hint), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = alarmSound, onCheckedChange = onAlarmSoundChange)
            }
        }
        if (showRecurringNotice) {
            Text(stringResource(R.string.reminder_recurring_notice), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// Una fila por subtarea (campo de texto + botón de borrar) y un botón para agregar otra. No se
// puede reordenar arrastrando todavía (ticket 0015).
@Composable
private fun SubtaskSection(
    subtasks: List<SubtaskDraft>,
    showRecurringNotice: Boolean,
    onAdd: () -> Unit,
    onTitleChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.subtasks_section_title), style = MaterialTheme.typography.labelLarge)
        subtasks.forEach { subtask ->
            // La clave estable evita que el campo pierda el foco o el texto al borrar otra fila.
            key(subtask.key) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = subtask.title,
                        onValueChange = { onTitleChange(subtask.key, it) },
                        label = { Text(stringResource(R.string.subtask_field_title)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemove(subtask.key) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.subtask_remove))
                    }
                }
            }
        }
        OutlinedButton(onClick = onAdd) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.subtask_add))
        }
        if (showRecurringNotice) {
            Text(stringResource(R.string.subtask_recurring_notice), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CustomReminderDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val minutes = text.toIntOrNull()
    val isValid = minutes != null && isValidReminderMinutes(minutes)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_custom_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value.filter(Char::isDigit).take(MAX_DIGITS_CUSTOM_REMINDER) },
                label = { Text(stringResource(R.string.reminder_custom_field)) },
                isError = text.isNotEmpty() && !isValid,
                supportingText = if (text.isNotEmpty() && !isValid) {
                    { Text(stringResource(R.string.reminder_custom_error, MAX_REMINDER_MINUTES_BEFORE)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { minutes?.let(onConfirm) }, enabled = isValid) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

// Alcanza para los 5 dígitos de 10080 y evita que `toIntOrNull` reciba un número gigante.
private const val MAX_DIGITS_CUSTOM_REMINDER = 5

@Composable
private fun reminderLabel(minutesBefore: Int): String {
    if (minutesBefore == 0) return stringResource(R.string.reminder_at_time)
    val (value, unit) = reminderAmount(minutesBefore)
    val plural = when (unit) {
        ReminderUnit.MINUTES -> R.plurals.reminder_minutes_before
        ReminderUnit.HOURS -> R.plurals.reminder_hours_before
        ReminderUnit.DAYS -> R.plurals.reminder_days_before
    }
    return pluralStringResource(plural, value, value)
}

@Composable
private fun RecurrenceTypeRow(type: RecurrenceType, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(recurrenceTypeLabel(type))
    }
}

@Composable
private fun recurrenceTypeLabel(type: RecurrenceType): String = when (type) {
    RecurrenceType.DAILY -> stringResource(R.string.create_task_recurrence_type_daily)
    RecurrenceType.WEEKLY -> stringResource(R.string.create_task_recurrence_type_weekly)
    RecurrenceType.SPECIFIC_DAYS -> stringResource(R.string.create_task_recurrence_type_specific_days)
    RecurrenceType.MONTHLY -> stringResource(R.string.create_task_recurrence_type_monthly)
    RecurrenceType.EVERY_N_DAYS -> stringResource(R.string.create_task_recurrence_type_every_n_days)
}

private fun daysOfWeekOrdenados(): List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
)

@Composable
private fun dayShortLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> stringResource(R.string.day_monday_short)
    DayOfWeek.TUESDAY -> stringResource(R.string.day_tuesday_short)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wednesday_short)
    DayOfWeek.THURSDAY -> stringResource(R.string.day_thursday_short)
    DayOfWeek.FRIDAY -> stringResource(R.string.day_friday_short)
    DayOfWeek.SATURDAY -> stringResource(R.string.day_saturday_short)
    DayOfWeek.SUNDAY -> stringResource(R.string.day_sunday_short)
}
