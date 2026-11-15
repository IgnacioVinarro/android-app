package com.ignaciovinarro.stayfocused.planner.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.ui.TaskColorKey
import com.ignaciovinarro.stayfocused.core.ui.TaskIconKey
import com.ignaciovinarro.stayfocused.core.ui.TimePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_task_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
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

            Text(stringResource(R.string.create_task_field_icon), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskIconKey.entries.forEach { iconOption ->
                    IconOption(
                        icon = iconOption,
                        selected = iconOption.key == uiState.iconKey,
                        onClick = { viewModel.onIconChange(iconOption.key) }
                    )
                }
            }

            Text(stringResource(R.string.create_task_field_color), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskColorKey.entries.forEach { colorOption ->
                    ColorOption(
                        colorOption = colorOption,
                        selected = colorOption.key == uiState.colorKey,
                        onClick = { viewModel.onColorChange(colorOption.key) }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.create_task_field_notes)) },
                modifier = Modifier.fillMaxWidth()
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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date.toEpochMillisUtc()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateChange(millis.toLocalDateUtc())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) { DatePicker(state = datePickerState) }
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
}

private fun LocalDate.toEpochMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun IconOption(icon: TaskIconKey, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(48.dp)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon.icon,
                contentDescription = if (selected) stringResource(R.string.a11y_task_icon_selected) else null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorOption(colorOption: TaskColorKey, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colorOption.color,
        modifier = Modifier.size(40.dp)
    ) {
        if (selected) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.a11y_task_color_selected),
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
}
