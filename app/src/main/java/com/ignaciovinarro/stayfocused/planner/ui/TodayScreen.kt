package com.ignaciovinarro.stayfocused.planner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.timeline.PositionedTimelineBlock
import com.ignaciovinarro.stayfocused.core.timeline.TimelineBlock
import com.ignaciovinarro.stayfocused.core.timeline.clampDurationMinutes
import com.ignaciovinarro.stayfocused.core.timeline.clampStartMinutes
import com.ignaciovinarro.stayfocused.core.ui.TimePickerDialog
import com.ignaciovinarro.stayfocused.core.ui.colorForKey
import com.ignaciovinarro.stayfocused.core.ui.iconForKey
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.domain.TIMELINE_SOURCE_PLANNER
import java.time.LocalTime
import kotlin.math.roundToInt

private val MINUTE_HEIGHT: Dp = 1.dp // 60 dp por hora
private val HOUR_LABEL_WIDTH: Dp = 48.dp
private val MIN_BLOCK_HEIGHT: Dp = 48.dp // área táctil mínima (CLAUDE.md)
private val RESIZE_HANDLE_HEIGHT: Dp = 16.dp
private const val HOURS_IN_DAY = 24
private const val MINUTES_IN_HOUR = 60

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onAddTask: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var taskToSchedule by remember { mutableStateOf<TaskEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (uiState.inboxTasks.isNotEmpty()) {
                InboxSection(
                    tasks = uiState.inboxTasks,
                    onTaskClick = { task -> taskToSchedule = task }
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val availableWidth = (maxWidth - HOUR_LABEL_WIDTH).coerceAtLeast(0.dp)

                Box(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(MINUTE_HEIGHT * (HOURS_IN_DAY * MINUTES_IN_HOUR))
                    ) {
                        HourScale()
                        PositionedBlocks(
                            blocks = uiState.positionedBlocks,
                            availableWidth = availableWidth,
                            onToggleComplete = viewModel::onToggleComplete,
                            onMoveTask = viewModel::onMoveTask,
                            onResizeTask = viewModel::onResizeTask
                        )
                        NowMarker(nowMinutes = uiState.nowMinutes)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddTask,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.a11y_add_task))
        }
    }

    taskToSchedule?.let { task ->
        val now = LocalTime.now()
        val timePickerState = rememberTimePickerState(
            initialHour = now.hour,
            initialMinute = now.minute,
            is24Hour = true
        )
        TimePickerDialog(
            onDismiss = { taskToSchedule = null },
            onConfirm = {
                viewModel.onAssignTime(task, LocalTime.of(timePickerState.hour, timePickerState.minute))
                taskToSchedule = null
            }
        ) { TimePicker(state = timePickerState) }
    }
}

@Composable
private fun InboxSection(tasks: List<TaskEntity>, onTaskClick: (TaskEntity) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        Text(stringResource(R.string.inbox_title), style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tasks.forEach { task ->
                val description = stringResource(R.string.a11y_inbox_assign_time, task.title)
                Surface(
                    onClick = { onTaskClick(task) },
                    color = colorForKey(task.colorKey).copy(alpha = 0.85f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.semantics { contentDescription = description }
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(iconForKey(task.iconKey), contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text(task.title, color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun HourScale() {
    Column {
        for (hour in 0 until HOURS_IN_DAY) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(MINUTE_HEIGHT * MINUTES_IN_HOUR)
            ) {
                Text(
                    text = "${hour.toString().padStart(2, '0')}:00",
                    modifier = Modifier
                        .width(HOUR_LABEL_WIDTH)
                        .padding(start = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
                HorizontalDivider(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = HOUR_LABEL_WIDTH)
                )
            }
        }
    }
}

@Composable
private fun PositionedBlocks(
    blocks: List<PositionedTimelineBlock>,
    availableWidth: Dp,
    onToggleComplete: (TimelineBlock) -> Unit,
    onMoveTask: (TimelineBlock, Int) -> Unit,
    onResizeTask: (TimelineBlock, Int) -> Unit
) {
    val density = LocalDensity.current
    // Mientras se arrastra un bloque, se muestra esta posición/duración provisoria en vez de la
    // que todavía tiene en la base; se limpia al soltar, cuando el ViewModel ya confirmó el cambio.
    val startOverrides = remember { mutableStateMapOf<String, Int>() }
    val durationOverrides = remember { mutableStateMapOf<String, Int>() }

    blocks.forEach { positioned ->
        val block = positioned.block
        // Las ocurrencias de tareas recurrentes todavía no se pueden mover/redimensionar
        // (eso tocaría overrideStartMinutes de la ocurrencia puntual, un paso aparte).
        val canDrag = block.source == TIMELINE_SOURCE_PLANNER && block.movable
        val startMinutes = startOverrides[block.id] ?: block.startMinutes
        val durationMinutes = durationOverrides[block.id] ?: block.durationMinutes
        val columnWidth = availableWidth / positioned.columnCount
        val height = (MINUTE_HEIGHT * durationMinutes).coerceAtLeast(MIN_BLOCK_HEIGHT)

        Box(
            modifier = Modifier
                .offset(
                    x = HOUR_LABEL_WIDTH + columnWidth * positioned.column,
                    y = MINUTE_HEIGHT * startMinutes
                )
                .width(columnWidth)
                .height(height)
        ) {
            TimelineBlockCard(
                title = block.title,
                iconKey = block.iconKey,
                colorKey = block.colorKey,
                completed = block.completed,
                onToggleComplete = { onToggleComplete(block) },
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (canDrag) {
                            Modifier.pointerInput(block.id) {
                                var arrastreMinutos = block.startMinutes
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { arrastreMinutos = block.startMinutes },
                                    onDragEnd = {
                                        onMoveTask(block, arrastreMinutos)
                                        startOverrides.remove(block.id)
                                    },
                                    onDragCancel = { startOverrides.remove(block.id) },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val deltaMinutos = with(density) { dragAmount.y.toDp() }.value.roundToInt()
                                        arrastreMinutos = clampStartMinutes(arrastreMinutos + deltaMinutos, durationMinutes)
                                        startOverrides[block.id] = arrastreMinutos
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            )

            if (canDrag) {
                ResizeHandle(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onResize = { nuevaDuracion ->
                        durationOverrides[block.id] = nuevaDuracion
                    },
                    onResizeEnd = { nuevaDuracion ->
                        onResizeTask(block, nuevaDuracion)
                        durationOverrides.remove(block.id)
                    },
                    onResizeCancel = { durationOverrides.remove(block.id) },
                    density = density,
                    startMinutes = startMinutes,
                    initialDurationMinutes = durationMinutes
                )
            }
        }
    }
}

@Composable
private fun TimelineBlockCard(
    title: String,
    iconKey: String,
    colorKey: String,
    completed: Boolean,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = colorForKey(colorKey)

    Surface(
        modifier = modifier
            .padding(1.dp)
            .clickable(onClick = onToggleComplete),
        color = color.copy(alpha = if (completed) 0.4f else 0.9f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconForKey(iconKey),
                contentDescription = null,
                tint = Color.White
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = title,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (completed) TextDecoration.LineThrough else null,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// Asa para redimensionar (cambiar la duración) arrastrando el borde inferior del bloque.
@Composable
private fun ResizeHandle(
    modifier: Modifier,
    onResize: (Int) -> Unit,
    onResizeEnd: (Int) -> Unit,
    onResizeCancel: () -> Unit,
    density: androidx.compose.ui.unit.Density,
    startMinutes: Int,
    initialDurationMinutes: Int
) {
    val description = stringResource(R.string.a11y_resize_task)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RESIZE_HANDLE_HEIGHT)
            .semantics { contentDescription = description }
            .pointerInput(startMinutes) {
                var arrastreDuracion = initialDurationMinutes
                detectDragGesturesAfterLongPress(
                    onDragStart = { arrastreDuracion = initialDurationMinutes },
                    onDragEnd = { onResizeEnd(arrastreDuracion) },
                    onDragCancel = { onResizeCancel() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val deltaMinutos = with(density) { dragAmount.y.toDp() }.value.roundToInt()
                        arrastreDuracion = clampDurationMinutes(arrastreDuracion + deltaMinutos, startMinutes)
                        onResize(arrastreDuracion)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.height(RESIZE_HANDLE_HEIGHT)
        )
    }
}

@Composable
private fun NowMarker(nowMinutes: Int) {
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = MINUTE_HEIGHT * nowMinutes),
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.error
    )
}
