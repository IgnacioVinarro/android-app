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
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import com.ignaciovinarro.stayfocused.planner.domain.TIMELINE_SOURCE_PLANNER
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class TodayUiState(
    val positionedBlocks: List<PositionedTimelineBlock> = emptyList(),
    val nowMinutes: Int = minuteOfDay(LocalTime.now()),
    val inboxTasks: List<TaskEntity> = emptyList()
)

internal fun minuteOfDay(time: LocalTime): Int = time.hour * 60 + time.minute

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val timelineRepository: TimelineRepository,
    private val taskRepository: TaskRepository,
    private val taskOccurrenceRepository: TaskOccurrenceRepository
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState: StateFlow<TodayUiState> = combine(
        timelineRepository.blocksFor(today),
        nowTicker(),
        taskRepository.observeByDate(today.toEpochDay())
    ) { blocks, nowMinutes, tasks ->
        TodayUiState(
            positionedBlocks = OverlapArranger.arrange(blocks),
            nowMinutes = nowMinutes,
            inboxTasks = tasks.filter { it.startMinutes == null }
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
