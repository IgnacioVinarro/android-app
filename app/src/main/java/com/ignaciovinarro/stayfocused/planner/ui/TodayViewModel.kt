package com.ignaciovinarro.stayfocused.planner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.core.timeline.OverlapArranger
import com.ignaciovinarro.stayfocused.core.timeline.PositionedTimelineBlock
import com.ignaciovinarro.stayfocused.core.timeline.TimelineBlock
import com.ignaciovinarro.stayfocused.core.timeline.TimelineRepository
import com.ignaciovinarro.stayfocused.core.timeline.clampDurationMinutes
import com.ignaciovinarro.stayfocused.core.timeline.clampStartMinutes
import com.ignaciovinarro.stayfocused.core.timeline.snapToGrid
import com.ignaciovinarro.stayfocused.planner.data.REMINDER_OWNER_TASK
import com.ignaciovinarro.stayfocused.planner.data.ReminderRepository
import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import com.ignaciovinarro.stayfocused.planner.domain.DayLoad
import com.ignaciovinarro.stayfocused.planner.domain.DayProgress
import com.ignaciovinarro.stayfocused.planner.domain.SubtaskItem
import com.ignaciovinarro.stayfocused.planner.domain.TIMELINE_SOURCE_PLANNER
import com.ignaciovinarro.stayfocused.planner.domain.buildWeekLoad
import com.ignaciovinarro.stayfocused.planner.domain.dayProgress
import com.ignaciovinarro.stayfocused.planner.domain.moveToInbox
import com.ignaciovinarro.stayfocused.planner.domain.occursOn
import com.ignaciovinarro.stayfocused.planner.domain.rescheduleOverdueTask
import com.ignaciovinarro.stayfocused.planner.domain.subtaskItemsFor
import com.ignaciovinarro.stayfocused.planner.domain.weekStartOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

// Subtareas hechas / total de una tarea ese día (PL-05).
data class PartsCount(val done: Int, val total: Int)

// Hoja inferior con las subtareas de una tarea (PL-05). Se arma en vivo con los datos del día, así
// que al tildar una casilla la lista se redibuja sola.
data class SubtaskSheet(val title: String, val items: List<SubtaskItem>)

data class TodayUiState(
    // Día que se está viendo (PL-12) y la carga de su semana (lunes a domingo).
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val weekLoad: List<DayLoad> = emptyList(),
    val positionedBlocks: List<PositionedTimelineBlock> = emptyList(),
    val nowMinutes: Int = minuteOfDay(LocalTime.now()),
    val inboxTasks: List<TaskEntity> = emptyList(),
    // Subtareas de las tareas de la bandeja (clave = id de la tarea); las que no tienen no figuran.
    val inboxParts: Map<Long, PartsCount> = emptyMap(),
    val progress: DayProgress = DayProgress(done = 0, total = 0),
    val subtaskSheet: SubtaskSheet? = null,
    // Tareas atrasadas (PL-14), de la más vieja a la más nueva; vacía = no se muestra el aviso.
    val overdueTasks: List<TaskEntity> = emptyList()
)

// Qué tarea tiene abierta la hoja de subtareas. Se guarda aparte del contenido: el contenido sale de
// la base y cambia solo.
private data class SheetTarget(val taskId: Long, val title: String, val recurring: Boolean)

internal fun minuteOfDay(time: LocalTime): Int = time.hour * 60 + time.minute

// Lo que cambia al elegir otro día: sus bloques, sus tareas y los tildes de subtareas de ese día.
private data class DayData(
    val date: LocalDate,
    val blocks: List<TimelineBlock>,
    val tasks: List<TaskEntity>,
    val completions: List<SubtaskCompletionEntity>
)

// Lo que no depende del día elegido: todas las subtareas, las atrasadas (contra el hoy real) y la hora.
private data class Support(
    val subtasks: List<SubtaskEntity>,
    val overdue: List<TaskEntity>,
    val nowMinutes: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository,
    private val taskRepository: TaskRepository,
    private val taskOccurrenceRepository: TaskOccurrenceRepository,
    private val subtaskRepository: SubtaskRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val selectedDate = MutableStateFlow(today)
    private val sheetTarget = MutableStateFlow<SheetTarget?>(null)

    private val dayData = selectedDate.flatMapLatest { date ->
        val epochDay = date.toEpochDay()
        combine(
            timelineRepository.blocksFor(date),
            taskRepository.observeByDate(epochDay),
            subtaskRepository.observeCompletions(epochDay)
        ) { blocks, tasks, completions -> DayData(date, blocks, tasks, completions) }
    }

    private val support = combine(
        subtaskRepository.observeAll(),
        taskRepository.observeOverdue(today.toEpochDay()),
        nowTicker()
    ) { subtasks, overdue, nowMinutes -> Support(subtasks, overdue, nowMinutes) }

    // Carga de la semana del día elegido (PL-12). Solo se vuelve a armar al cambiar de semana o de datos.
    private val weekLoad = selectedDate
        .map { weekStartOf(it) }
        .distinctUntilChanged()
        .flatMapLatest { weekStart ->
            val startDay = weekStart.toEpochDay()
            val endDay = weekStart.plusDays(6).toEpochDay()
            combine(
                taskRepository.observeForRange(startDay, endDay),
                taskOccurrenceRepository.observeByDateRange(startDay, endDay)
            ) { tasks, occurrences -> buildWeekLoad(weekStart, tasks, occurrences) }
        }

    val uiState: StateFlow<TodayUiState> = combine(
        dayData,
        support,
        sheetTarget,
        weekLoad
    ) { day, (subtasks, overdue, nowMinutes), target, week ->
        val date = day.date
        // Las recurrentes sin hora solo van a la bandeja los días que les toca.
        val inbox = day.tasks.filter {
            it.startMinutes == null && (it.recurrenceType == null || occursOn(it, date))
        }
        val inboxItems = inbox.associate { task ->
            task.id to subtaskItemsFor(task.id, task.recurrenceType != null, date, subtasks, day.completions)
        }
        val plannerBlocks = day.blocks.filter { it.source == TIMELINE_SOURCE_PLANNER }

        // Progreso del día: tareas (bloques de Planificación + bandeja) y sus subtareas.
        val progress = dayProgress(
            tasksDone = plannerBlocks.count { it.completed } + inbox.count { it.completedAt != null },
            tasksTotal = plannerBlocks.size + inbox.size,
            subtasksDone = plannerBlocks.sumOf { it.partsDone } + inboxItems.values.sumOf { items -> items.count { it.done } },
            subtasksTotal = plannerBlocks.sumOf { it.partsTotal } + inboxItems.values.sumOf { it.size }
        )

        TodayUiState(
            selectedDate = date,
            isToday = date == today,
            weekLoad = week,
            positionedBlocks = OverlapArranger.arrange(day.blocks),
            nowMinutes = nowMinutes,
            inboxTasks = inbox,
            inboxParts = inboxItems
                .filterValues { it.isNotEmpty() }
                .mapValues { (_, items) -> PartsCount(done = items.count { it.done }, total = items.size) },
            progress = progress,
            subtaskSheet = target?.let {
                SubtaskSheet(it.title, subtaskItemsFor(it.taskId, it.recurring, date, subtasks, day.completions))
            },
            overdueTasks = overdue
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState()
    )

    // Navegación por día (PL-12). Al cambiar de día se cierra la hoja de subtareas: era de otro día.
    fun onSelectDay(date: LocalDate) {
        sheetTarget.value = null
        selectedDate.value = date
    }

    // Las flechas mueven una semana entera y conservan el día de la semana elegido.
    fun onPreviousWeek() = shiftSelectedDate(-1)

    fun onNextWeek() = shiftSelectedDate(1)

    fun onGoToToday() = onSelectDay(today)

    private fun shiftSelectedDate(weeks: Long) {
        sheetTarget.value = null
        selectedDate.update { it.plusWeeks(weeks) }
    }

    // Desde la línea de tiempo solo se completan las tareas; los demás módulos se completan
    // en su propia pantalla. Completar una ocurrencia de una tarea recurrente no completa las
    // demás: se guarda en `task_occurrence`, no en la tarea.
    fun onToggleComplete(block: TimelineBlock) {
        if (block.source != TIMELINE_SOURCE_PLANNER) return
        val taskId = block.route.substringAfterLast("/").toLongOrNull() ?: return
        viewModelScope.launch {
            val task = taskRepository.getById(taskId) ?: return@launch
            if (task.recurrenceType == null) {
                val nuevoCompletedAt = if (task.completedAt == null) System.currentTimeMillis() else null
                taskRepository.update(task.copy(completedAt = nuevoCompletedAt))
            } else {
                toggleOccurrenceCompletion(task, block.date.toEpochDay())
            }
        }
    }

    private suspend fun toggleOccurrenceCompletion(task: TaskEntity, epochDay: Long) {
        val occurrence = taskOccurrenceRepository.getByTaskAndDate(task.id, epochDay)
        if (occurrence == null) {
            taskOccurrenceRepository.create(
                TaskOccurrenceEntity(taskId = task.id, date = epochDay, completedAt = System.currentTimeMillis())
            )
        } else {
            val nuevoCompletedAt = if (occurrence.completedAt == null) System.currentTimeMillis() else null
            taskOccurrenceRepository.update(occurrence.copy(completedAt = nuevoCompletedAt))
        }
    }

    // Hoja de subtareas (PL-05): se abre desde el chip "X/Y" de un bloque o de una tarea de la bandeja.
    // Se necesita la tarea para saber si es recurrente: de eso depende dónde se guarda el tilde.
    fun onOpenSubtasks(block: TimelineBlock) {
        if (block.source != TIMELINE_SOURCE_PLANNER) return
        val taskId = block.route.substringAfterLast("/").toLongOrNull() ?: return
        openSubtasks(taskId, block.title)
    }

    fun onOpenSubtasks(task: TaskEntity) = openSubtasks(task.id, task.title)

    private fun openSubtasks(taskId: Long, title: String) {
        viewModelScope.launch {
            val task = taskRepository.getById(taskId) ?: return@launch
            sheetTarget.value = SheetTarget(task.id, title, recurring = task.recurrenceType != null)
        }
    }

    fun onDismissSubtasks() {
        sheetTarget.value = null
    }

    // Tarea simple: el tilde va en la subtarea. Recurrente: va por ocurrencia (solo el día elegido). El estado
    // de la tarea no cambia: tarea y subtareas son independientes (ticket 0016).
    fun onToggleSubtask(item: SubtaskItem) {
        val target = sheetTarget.value ?: return
        viewModelScope.launch {
            if (target.recurring) {
                subtaskRepository.setDoneOn(item.id, selectedDate.value.toEpochDay(), !item.done)
            } else {
                subtaskRepository.setDone(item.id, !item.done)
            }
        }
    }

    // Tareas atrasadas (PL-14, ticket 0017). Se vuelve a leer la tarea de la base antes de cambiarla:
    // la que llega de la pantalla pudo quedar vieja, y las reglas puras vuelven a comprobar que siga
    // atrasada. Las alarmas se reprograman solas cuando cambia la tabla (ReminderSyncer).

    // "Reprogramar": cambia el día y conserva la hora y los recordatorios.
    fun onRescheduleOverdue(task: TaskEntity, newDate: LocalDate) {
        viewModelScope.launch {
            val current = taskRepository.getById(task.id) ?: return@launch
            val updated = rescheduleOverdueTask(current, newDate, today) ?: return@launch
            taskRepository.update(updated)
        }
    }

    // "A la bandeja": pasa a la bandeja de hoy, sin hora. Sin hora no hay recordatorios (ticket 0012),
    // así que se borran también (las filas hijas no se borran solas, CLAUDE.md).
    fun onMoveOverdueToInbox(task: TaskEntity) {
        viewModelScope.launch {
            val current = taskRepository.getById(task.id) ?: return@launch
            val updated = moveToInbox(current, today) ?: return@launch
            taskRepository.update(updated)
            reminderRepository.deleteByOwner(REMINDER_OWNER_TASK, current.id)
        }
    }

    // Por ahora, la única forma de sacar una tarea de la bandeja es asignarle una hora acá
    // (el arrastre desde la bandeja es un paso aparte, todavía no implementado).
    fun onAssignTime(task: TaskEntity, time: LocalTime) {
        viewModelScope.launch {
            taskRepository.update(task.copy(startMinutes = minuteOfDay(time)))
        }
    }

    // Mover un bloque (arrastre vertical) solo se permite para tareas simples: los demás módulos
    // se editan en su propia pantalla y las ocurrencias de tareas recurrentes todavía no se
    // pueden mover (ver `movable` en `TimelineBlock`).
    fun onMoveTask(block: TimelineBlock, newStartMinutes: Int) {
        if (block.source != TIMELINE_SOURCE_PLANNER || !block.movable) return
        val taskId = block.route.substringAfterLast("/").toLongOrNull() ?: return
        viewModelScope.launch {
            val task = taskRepository.getById(taskId) ?: return@launch
            val startMinutes = clampStartMinutes(snapToGrid(newStartMinutes), task.durationMinutes)
            taskRepository.update(task.copy(startMinutes = startMinutes))
        }
    }

    // Redimensionar (arrastre del borde inferior) solo se permite para tareas simples.
    fun onResizeTask(block: TimelineBlock, newDurationMinutes: Int) {
        if (block.source != TIMELINE_SOURCE_PLANNER || !block.movable) return
        val taskId = block.route.substringAfterLast("/").toLongOrNull() ?: return
        viewModelScope.launch {
            val task = taskRepository.getById(taskId) ?: return@launch
            val startMinutes = task.startMinutes ?: return@launch
            val durationMinutes = clampDurationMinutes(snapToGrid(newDurationMinutes), startMinutes)
            taskRepository.update(task.copy(durationMinutes = durationMinutes))
        }
    }
}

private fun nowTicker(): Flow<Int> = flow {
    while (true) {
        emit(minuteOfDay(LocalTime.now()))
        delay(30_000L)
    }
}
