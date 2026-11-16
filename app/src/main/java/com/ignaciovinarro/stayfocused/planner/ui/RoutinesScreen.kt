package com.ignaciovinarro.stayfocused.planner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    onBack: () -> Unit,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val routines by viewModel.routines.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.routines_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRoutine) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.a11y_add_routine))
            }
        }
    ) { innerPadding ->
        if (routines.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.state_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(routines, key = { it.id }) { routine ->
                    RoutineRow(
                        routine = routine,
                        onApplyToday = { viewModel.onApplyToday(routine) },
                        onEdit = { onEditRoutine(routine.id) },
                        onDelete = { viewModel.onDelete(routine) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineRow(
    routine: RoutineEntity,
    onApplyToday: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                Text(formatMinutesOfDay(routine.startMinutes), style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onApplyToday) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.a11y_apply_routine_today, routine.name)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            }
        }
    }
}

internal fun formatMinutesOfDay(minutes: Int): String {
    val hour = (minutes / 60).coerceIn(0, 23)
    val minute = (minutes % 60).coerceIn(0, 59)
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
