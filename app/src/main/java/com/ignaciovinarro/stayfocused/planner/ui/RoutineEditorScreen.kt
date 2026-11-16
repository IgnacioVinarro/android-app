package com.ignaciovinarro.stayfocused.planner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.ui.TaskColorSelector
import com.ignaciovinarro.stayfocused.core.ui.TaskIconSelector
import com.ignaciovinarro.stayfocused.core.ui.TimePickerDialog
import com.ignaciovinarro.stayfocused.core.ui.colorForKey
import com.ignaciovinarro.stayfocused.core.ui.iconForKey
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showStartTimePicker by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var showingNewItemEditor by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.routine_editor_title)) },
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
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.routine_editor_field_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("${stringResource(R.string.routine_editor_field_start_time)}: ${formatMinutesOfDay(minuteOfDay(uiState.startTime))}")
            }

            Text(stringResource(R.string.routine_editor_items_title), style = MaterialTheme.typography.labelLarge)

            uiState.items.forEachIndexed { index, item ->
                RoutineItemRow(
                    item = item,
                    onClick = { editingItemIndex = index },
                    onRemove = { viewModel.onRemoveItem(index) }
                )
            }

            OutlinedButton(onClick = { showingNewItemEditor = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.routine_editor_add_item))
            }

            Button(
                onClick = { viewModel.save(onSaved = onSaved) },
                enabled = uiState.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = uiState.startTime.hour,
            initialMinute = uiState.startTime.minute,
            is24Hour = true
        )
        TimePickerDialog(
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                viewModel.onStartTimeChange(LocalTime.of(timePickerState.hour, timePickerState.minute))
                showStartTimePicker = false
            }
        ) { TimePicker(state = timePickerState) }
    }

    editingItemIndex?.let { index ->
        RoutineItemEditorDialog(
            initial = uiState.items[index],
            onDismiss = { editingItemIndex = null },
            onConfirm = { draft ->
                viewModel.onUpdateItem(index, draft)
                editingItemIndex = null
            }
        )
    }

    if (showingNewItemEditor) {
        RoutineItemEditorDialog(
            initial = RoutineItemDraft(time = uiState.startTime),
            onDismiss = { showingNewItemEditor = false },
            onConfirm = { draft ->
                viewModel.onAddItem(draft)
                showingNewItemEditor = false
            }
        )
    }
}

@Composable
private fun RoutineItemRow(item: RoutineItemDraft, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(iconForKey(item.iconKey), contentDescription = null, tint = colorForKey(item.colorKey))
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(item.title.ifBlank { stringResource(R.string.routine_editor_item_untitled) })
                    Text(
                        "${formatMinutesOfDay(minuteOfDay(item.time))} · ${item.durationMinutes} min",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineItemEditorDialog(
    initial: RoutineItemDraft,
    onDismiss: () -> Unit,
    onConfirm: (RoutineItemDraft) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var time by remember { mutableStateOf(initial.time) }
    var durationMinutes by remember { mutableStateOf(initial.durationMinutes) }
    var iconKey by remember { mutableStateOf(initial.iconKey) }
    var colorKey by remember { mutableStateOf(initial.colorKey) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        initial.copy(
                            title = title,
                            time = time,
                            durationMinutes = durationMinutes,
                            iconKey = iconKey,
                            colorKey = colorKey
                        )
                    )
                },
                enabled = title.isNotBlank() && durationMinutes > 0
            ) { Text(stringResource(R.string.action_done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.routine_editor_field_item_title)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("${stringResource(R.string.routine_editor_field_item_time)}: ${formatMinutesOfDay(minuteOfDay(time))}")
                }
                OutlinedTextField(
                    value = durationMinutes.toString(),
                    onValueChange = { value -> value.toIntOrNull()?.let { durationMinutes = it } },
                    label = { Text(stringResource(R.string.create_task_field_duration)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.create_task_field_icon), style = MaterialTheme.typography.labelLarge)
                TaskIconSelector(selectedKey = iconKey, onSelect = { iconKey = it })
                Text(stringResource(R.string.create_task_field_color), style = MaterialTheme.typography.labelLarge)
                TaskColorSelector(selectedKey = colorKey, onSelect = { colorKey = it })
            }
        }
    )

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true
        )
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                showTimePicker = false
            }
        ) { TimePicker(state = timePickerState) }
    }
}
