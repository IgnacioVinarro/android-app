package com.ignaciovinarro.stayfocused.planner.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.semantics.Role
import com.ignaciovinarro.stayfocused.planner.domain.DayProgress
import com.ignaciovinarro.stayfocused.planner.domain.SubtaskItem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.pluralStringResource
import com.ignaciovinarro.stayfocused.core.ui.AppDatePickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.ignaciovinarro.stayfocused.core.timeline.FreeGap
import com.ignaciovinarro.stayfocused.core.timeline.PositionedTimelineBlock
import com.ignaciovinarro.stayfocused.core.timeline.TimelineBlock
import com.ignaciovinarro.stayfocused.core.timeline.clampDurationMinutes
import com.ignaciovinarro.stayfocused.core.timeline.clampStartMinutes
import com.ignaciovinarro.stayfocused.core.ui.TimePickerDialog
import com.ignaciovinarro.stayfocused.core.ui.colorForKey
import com.ignaciovinarro.stayfocused.core.ui.formatDuration
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
    // Crear una tarea en el día que se está viendo (epochDay), y crearla en un hueco libre (PL-03).
    onAddTask: (date: Long) -> Unit = {},
    onCreateTaskInGap: (date: Long, startMinutes: Int, gapMinutes: Int) -> Unit = { _, _, _ -> },
    onOpenRoutines: () -> Unit = {},
    onEditTask: (taskId: Long, occurrenceDate: Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
    focusViewModel: FocusTimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusState by focusViewModel.uiState.collectAsState()
    // Tarea para la que se está eligiendo la duración del enfoque (PL-13): id y título.
    var focusTarget by remember { mutableStateOf<Pair<Long, String>?>(null) }
    // Al arrancar la sesión, el diálogo se cierra solo.
    LaunchedEffect(focusState.session != null) {
        if (focusState.session != null) focusTarget = null
    }
    var taskToSchedule by remember { mutableStateOf<TaskEntity?>(null) }
    // Hoja de tareas atrasadas (PL-14) y la tarea para la que se está eligiendo nueva fecha.
    var showOverdue by rememberSaveable { mutableStateOf(false) }
    var overdueToReschedule by remember { mutableStateOf<TaskEntity?>(null) }

    // Si la última atrasada sale de la lista (reprogramada o llevada a la bandeja), la hoja se cierra sola.
    LaunchedEffect(uiState.overdueTasks.isEmpty()) {
        if (uiState.overdueTasks.isEmpty()) showOverdue = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Vista semanal (PL-12): elegir el día que se ve abajo y ver la carga de la semana.
            WeekStrip(
                week = uiState.weekLoad,
                selectedDate = uiState.selectedDate,
                today = LocalDate.now(),
                onSelectDay = viewModel::onSelectDay,
                onPreviousWeek = viewModel::onPreviousWeek,
                onNextWeek = viewModel::onNextWeek,
                onGoToToday = viewModel::onGoToToday
            )

            // Enfoque en curso (PL-13): solo si hay una sesión.
            FocusBar(
                state = focusState,
                onPause = focusViewModel::onPause,
                onResume = focusViewModel::onResume,
                onStop = focusViewModel::onStop
            )

            // Tareas atrasadas (PL-14): solo si hay.
            if (uiState.overdueTasks.isNotEmpty()) {
                OverdueNotice(count = uiState.overdueTasks.size, onClick = { showOverdue = true })
            }

            // Progreso del día (PL-05): solo si hay algo que completar.
            if (uiState.progress.total > 0) {
                DayProgressHeader(progress = uiState.progress)
            }

            if (uiState.inboxTasks.isNotEmpty()) {
                InboxSection(
                    tasks = uiState.inboxTasks,
                    partsByTask = uiState.inboxParts,
                    onTaskClick = { task -> taskToSchedule = task },
                    onOpenSubtasks = viewModel::onOpenSubtasks
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
                        // Los huecos van antes que los bloques: se dibujan debajo y un bloque corto,
                        // que tiene alto mínimo, no le quita el toque a otro.
                        FreeGapSlots(
                            gaps = uiState.freeGaps,
                            availableWidth = availableWidth,
                            onClick = { gap ->
                                onCreateTaskInGap(
                                    uiState.selectedDate.toEpochDay(), gap.startMinutes, gap.durationMinutes
                                )
                            }
                        )
                        PositionedBlocks(
                            blocks = uiState.positionedBlocks,
                            availableWidth = availableWidth,
                            onToggleComplete = viewModel::onToggleComplete,
                            onOpenSubtasks = viewModel::onOpenSubtasks,
                            onMoveTask = viewModel::onMoveTask,
                            onResizeTask = viewModel::onResizeTask,
                            onEditTask = onEditTask,
                            onStartFocus = if (focusState.session == null) {
                                { block, taskId -> focusTarget = taskId to block.title }
                            } else {
                                null
                            }
                        )
                        // La línea de "ahora" solo tiene sentido en el día de hoy.
                        if (uiState.isToday) NowMarker(nowMinutes = uiState.nowMinutes)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenRoutines,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Repeat, contentDescription = stringResource(R.string.a11y_open_routines))
        }

        FloatingActionButton(
            onClick = { onAddTask(uiState.selectedDate.toEpochDay()) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.a11y_add_task))
        }
    }

    if (showOverdue && uiState.overdueTasks.isNotEmpty()) {
        OverdueSheetContent(
            tasks = uiState.overdueTasks,
            onReschedule = { task -> overdueToReschedule = task },
            onMoveToInbox = viewModel::onMoveOverdueToInbox,
            onDismiss = { showOverdue = false }
        )
    }

    overdueToReschedule?.let { task ->
        // Solo hoy o días futuros: reprogramar una atrasada a otro día pasado no la saca de atrasada.
        val today = LocalDate.now()
        AppDatePickerDialog(
            initialDate = today,
            minDate = today,
            onConfirm = { date -> viewModel.onRescheduleOverdue(task, date) },
            onDismiss = { overdueToReschedule = null }
        )
    }

    focusTarget?.let { (taskId, title) ->
        FocusStartDialog(
            taskTitle = title,
            startFailed = focusState.startFailed,
            onStart = { minutes -> focusViewModel.onStart(taskId, title, minutes) },
            onDismiss = {
                focusViewModel.onDismissStartError()
                focusTarget = null
            }
        )
    }

    uiState.subtaskSheet?.let { sheet ->
        SubtaskSheetContent(
            sheet = sheet,
            onToggle = viewModel::onToggleSubtask,
            onDismiss = viewModel::onDismissSubtasks
        )
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
private fun InboxSection(
    tasks: List<TaskEntity>,
    partsByTask: Map<Long, PartsCount>,
    onTaskClick: (TaskEntity) -> Unit,
    onOpenSubtasks: (TaskEntity) -> Unit
) {
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
                        partsByTask[task.id]?.let { parts ->
                            PartsChip(parts.done, parts.total, onClick = { onOpenSubtasks(task) })
                        }
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

// Huecos libres del día (PL-03): borde punteado con "+" y la duración libre. Tocarlos crea una tarea ahí.
// Llevan ícono y texto además del borde: no dependen solo del color. Miden 1 dp por minuto, así que uno
// de 30 min queda por debajo de los 48 dp recomendados (ver ticket 0020).
@Composable
private fun FreeGapSlots(gaps: List<FreeGap>, availableWidth: Dp, onClick: (FreeGap) -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    gaps.forEach { gap ->
        val duration = formatDuration(gap.durationMinutes)
        val description = stringResource(
            R.string.a11y_free_gap, duration, formatClockTime(gap.startMinutes)
        )
        Box(
            modifier = Modifier
                .offset(x = HOUR_LABEL_WIDTH, y = MINUTE_HEIGHT * gap.startMinutes)
                .width(availableWidth)
                .height(MINUTE_HEIGHT * gap.durationMinutes)
                .padding(2.dp)
                .drawBehind {
                    drawRoundRect(
                        color = outline,
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                        )
                    )
                }
                .clickable(onClick = { onClick(gap) })
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = outline, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.free_gap_label, duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// "HH:mm" para minutos desde las 00:00.
private fun formatClockTime(minutes: Int): String =
    "%02d:%02d".format(minutes / MINUTES_IN_HOUR, minutes % MINUTES_IN_HOUR)

@Composable
private fun PositionedBlocks(
    blocks: List<PositionedTimelineBlock>,
    availableWidth: Dp,
    onToggleComplete: (TimelineBlock) -> Unit,
    onOpenSubtasks: (TimelineBlock) -> Unit,
    onMoveTask: (TimelineBlock, Int) -> Unit,
    onResizeTask: (TimelineBlock, Int) -> Unit,
    onEditTask: (taskId: Long, occurrenceDate: Long) -> Unit,
    // Nulo = no se ofrece iniciar enfoque (ya hay una sesión en curso).
    onStartFocus: ((block: TimelineBlock, taskId: Long) -> Unit)?
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
                partsDone = block.partsDone,
                partsTotal = block.partsTotal,
                onOpenSubtasks = { onOpenSubtasks(block) },
                onToggleComplete = { onToggleComplete(block) },
                onStartFocus = if (onStartFocus != null && block.source == TIMELINE_SOURCE_PLANNER && !block.completed) {
                    {
                        val taskId = block.route.substringAfterLast("/").toLongOrNull()
                        taskId?.let { onStartFocus(block, it) }
                    }
                } else {
                    null
                },
                onEditTask = if (block.source == TIMELINE_SOURCE_PLANNER) {
                    {
                        val taskId = block.route.substringAfterLast("/").toLongOrNull()
                        taskId?.let { onEditTask(it, block.date.toEpochDay()) }
                    }
                } else {
                    null
                },
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
    partsDone: Int,
    partsTotal: Int,
    onOpenSubtasks: () -> Unit,
    onToggleComplete: () -> Unit,
    onStartFocus: (() -> Unit)?,
    onEditTask: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val color = colorForKey(colorKey)
    // Al completar/descompletar el bloque cambia de opacidad con una transición corta en vez de a golpe.
    val alpha by animateFloatAsState(targetValue = if (completed) 0.4f else 0.9f, label = "blockAlpha")

    Surface(
        modifier = modifier
            .padding(1.dp)
            .clickable(onClick = onToggleComplete),
        color = color.copy(alpha = alpha),
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
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (partsTotal > 0) {
                PartsChip(partsDone, partsTotal, onClick = onOpenSubtasks)
            }
            // Ícono chico para enfocar (PL-13); mismo criterio de área táctil que el de editar.
            if (onStartFocus != null) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = stringResource(R.string.a11y_focus_start, title),
                    tint = Color.White,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                        .clickable(onClick = onStartFocus)
                )
            }
            // Ícono chico para editar/borrar (PL-15): el bloque puede ser angosto, así que el área
            // táctil acá es más chica que los 48 dp recomendados, mismo criterio pragmático que
            // el asa de redimensionar (RESIZE_HANDLE_HEIGHT).
            if (onEditTask != null) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.a11y_edit_task),
                    tint = Color.White,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                        .clickable(onClick = onEditTask)
                )
            }
        }
    }
}

// Aviso "N atrasadas": ícono + texto (el estado no depende solo del color). El chip de Material 3 ya
// respeta el área táctil mínima de 48 dp.
@Composable
private fun OverdueNotice(count: Int, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(pluralStringResource(R.plurals.overdue_notice, count, count)) },
        leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null) },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

// Hoja inferior con las tareas atrasadas (PL-14): una fila por tarea con su fecha original y dos
// acciones, "Reprogramar" y "A la bandeja". Los botones llevan el título de la tarea en su descripción
// para que con TalkBack se sepa a cuál pertenece cada uno.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverdueSheetContent(
    tasks: List<TaskEntity>,
    onReschedule: (TaskEntity) -> Unit,
    onMoveToInbox: (TaskEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                stringResource(R.string.overdue_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            tasks.forEach { task ->
                val rescheduleDescription = stringResource(R.string.a11y_overdue_reschedule, task.title)
                val inboxDescription = stringResource(R.string.a11y_overdue_to_inbox, task.title)
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(task.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(
                            R.string.overdue_original_date,
                            LocalDate.ofEpochDay(task.date).format(dateFormat)
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { onReschedule(task) },
                            modifier = Modifier.semantics { contentDescription = rescheduleDescription }
                        ) { Text(stringResource(R.string.overdue_action_reschedule)) }
                        TextButton(
                            onClick = { onMoveToInbox(task) },
                            modifier = Modifier.semantics { contentDescription = inboxDescription }
                        ) { Text(stringResource(R.string.overdue_action_to_inbox)) }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

// Chip "X/Y" de subtareas, compartido por los bloques y la bandeja (una sola implementación). Lleva
// texto además de color. El área táctil es chica porque el bloque es angosto: mismo criterio
// pragmático que el ícono de editar (ticket 0016); la hoja que abre sí tiene filas de 48 dp.
@Composable
private fun PartsChip(done: Int, total: Int, onClick: () -> Unit) {
    val description = stringResource(R.string.a11y_subtasks_chip, done, total)
    Text(
        text = stringResource(R.string.subtasks_chip, done, total),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .padding(start = 4.dp)
            .background(Color.Black.copy(alpha = 0.25f), MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .semantics { contentDescription = description }
    )
}

// Encabezado de Hoy: "X de Y completadas" + barra animada. El texto va siempre: el estado no depende
// solo del color de la barra.
@Composable
private fun DayProgressHeader(progress: DayProgress) {
    val animated by animateFloatAsState(targetValue = progress.fraction, label = "dayProgress")
    val description = stringResource(R.string.a11y_day_progress, progress.done, progress.total)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        Text(
            stringResource(R.string.day_progress_label, progress.done, progress.total),
            style = MaterialTheme.typography.labelLarge
        )
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}

// Hoja inferior con las subtareas de una tarea: una fila de al menos 48 dp por subtarea, toda la fila
// es tocable (no solo la casilla). El tilde cambia en la base y la lista se redibuja sola.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubtaskSheetContent(
    sheet: SubtaskSheet,
    onToggle: (SubtaskItem) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                sheet.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (sheet.items.isEmpty()) {
                Text(stringResource(R.string.subtasks_sheet_empty))
            }
            sheet.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = item.done,
                            role = Role.Checkbox,
                            onValueChange = { onToggle(item) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // La fila entera maneja el toque (toggleable); la casilla solo dibuja el estado.
                    Checkbox(checked = item.done, onCheckedChange = null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        item.title,
                        textDecoration = if (item.done) TextDecoration.LineThrough else null
                    )
                }
            }
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
