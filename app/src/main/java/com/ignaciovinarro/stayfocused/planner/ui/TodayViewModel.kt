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
import com.ignaciovinarro.stayfocused.planner.data.SubtaskRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import com.ignaciovinarro.stayfocused.planner.domain.DayProgress
import com.ignaciovinarro.stayfocused.planner.domain.SubtaskItem
import com.ignaciovinarro.stayfocused.planner.domain.TIMELINE_SOURCE_PLANNER
import com.ignaciovinarro.stayfocused.planner.domain.dayProgress
import com.ignaciovinarro.stayfocused.planner.domain.subtaskItemsFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
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
    val positionedBlocks: List<PositionedTimelineBlock> = emptyList(),
    val nowMinutes: Int = minuteOfDay(LocalTime.now()),
    val inboxTasks: List<TaskEntity> = emptyList(),
    // Subtareas de las tareas de la bandeja (clave = id de la tarea); las que no tienen no figuran.
    val inboxParts: Map<Long, PartsCount> = emptyMap(),
    val progress: DayProgress = DayProgress(done = 0, total = 0),
    val subtaskSheet: SubtaskSheet? = null
)

// Qué tarea tiene abierta la hoja de subtareas. Se guarda aparte del contenido: el contenido sale de
// la base y cambia solo.
private data class SheetTarget(val taskId: Long, val title: String, val recurring: Boolean)

internal fun minuteOfDay(time: LocalTime): Int = time.hour * 60 + time.minute

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository,
    private val taskRepository: TaskRepository,
    private val taskOccurrenceRepository: TaskOccurrenceRepository,
    private val subtaskRepository: SubtaskRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val sheetTarget = MutableStateFlow<SheetTarget?>(null)

    // Subtareas activas y tildes de hoy, juntos para no pasar de 5 flujos en el combine principal.
    private val subtaskData = combine(
        subtaskRepository.observeAll(),
        subtaskRepository.observeCompletions(today.toEpochDay())
    ) { subtasks, completions -> subtasks to completions }

    val uiState: StateFlow<TodayUiState> = combine(
        timelineRepository.blocksFor(today),
        nowTicker(),
        taskRepository.observeByDate(today.toEpochDay()),
        subtaskData,
        sheetTarget
    ) { blocks, nowMinutes, tasks, (subtasks, completions), target ->
        val inbox = tasks.filter { it.startMinutes == null }
        val inboxItems = inbox.associate { task ->
            task.id to subtaskItemsFor(task.id, task.recurrenceType != null, today, subtasks, completions)
        }
        val plannerBlocks = blocks.filter { it.source == TIMELINE_SOURCE_PLANNER }

        // Progreso del día: tareas (bloques de Planificación + bandeja) y sus subtareas.
        val progress = dayProgress(
            tasksDone = plannerBlocks.count { it.completed } + inbox.count { it.completedAt != null },
            tasksTotal = plannerBlocks.size + inbox.size,
            subtasksDone = plannerBlocks.sumOf { it.partsDone } + inboxItems.values.sumOf { items -> items.count { it.done } },
            subtasksTotal = plannerBlocks.sumOf { it.partsTotal } + inboxItems.values.sumOf { it.size }
        )

        TodayUiState(
            positionedBlocks = OverlapArranger.arrange(blocks),
            nowMinutes = nowMinutes,
            inboxTasks = inbox,
            inboxParts = inboxItems
                .filterValues { it.isNotEmpty() }
                .mapValues { (_, items) -> PartsCount(done = items.count { it.done }, total = items.size) },
            progress = progress,
            subtaskSheet = target?.let {
                SubtaskSheet(it.title, subtaskItemsFor(it.taskId, it.recurring, today, subtasks, completions))
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState()
    )

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

    // Tarea simple: el tilde va en la subtarea. Recurrente: va por ocurrencia (solo hoy). El estado
    // de la tarea no cambia: tarea y subtareas son independientes (ticket 0016).
    fun onToggleSubtask(item: SubtaskItem) {
        val target = sheetTarget.value ?: return
        viewModelScope.launch {
            if (target.recurring) {
                subtaskRepository.setDoneOn(item.id, today.toEpochDay(), !item.done)
            } else {
                subtaskRepository.setDone(item.id, !item.done)
            }
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
