package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.core.timeline.TimelineBlock
import com.ignaciovinarro.stayfocused.core.timeline.TimelineSource
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

const val TIMELINE_SOURCE_PLANNER = "planner"

@Module
@InstallIn(SingletonComponent::class)
abstract class TaskTimelineModule {
    @Binds
    @IntoSet
    abstract fun bindTaskTimelineSource(source: TaskTimelineSource): TimelineSource
}

class TaskTimelineSource @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskOccurrenceRepository: TaskOccurrenceRepository
) : TimelineSource {

    // Las tareas sin hora van a la bandeja (Inbox): no entran a la línea de tiempo. Las tareas
    // recurrentes solo aparecen el día que les toca según `occursOn`; si esa ocurrencia puntual
    // tiene fila en `task_occurrence`, la hora y si está completada salen de ahí (y si está
    // saltada, no se muestra); si no tiene fila, salen de la tarea.
    override fun blocksFor(date: LocalDate): Flow<List<TimelineBlock>> {
        val epochDay = date.toEpochDay()
        return combine(
            taskRepository.observeByDate(epochDay),
            taskOccurrenceRepository.observeByDateRange(epochDay, epochDay)
        ) { tasks, occurrences ->
            val occurrenceByTaskId = occurrences.associateBy { it.taskId }
            tasks.mapNotNull { task ->
                val startMinutes = task.startMinutes ?: return@mapNotNull null

                if (task.recurrenceType == null) {
                    return@mapNotNull task.toTimelineBlock(
                        date = date,
                        startMinutes = startMinutes,
                        durationMinutes = task.durationMinutes,
                        title = task.title,
                        iconKey = task.iconKey,
                        colorKey = task.colorKey,
                        completed = task.completedAt != null,
                        movable = true
                    )
                }

                if (!occursOn(task, date)) return@mapNotNull null
                val occurrence = occurrenceByTaskId[task.id]
                if (occurrence?.skipped == true) return@mapNotNull null

                task.toTimelineBlock(
                    date = date,
                    startMinutes = occurrence?.overrideStartMinutes ?: startMinutes,
                    durationMinutes = occurrence?.overrideDurationMinutes ?: task.durationMinutes,
                    title = occurrence?.overrideTitle ?: task.title,
                    iconKey = occurrence?.overrideIconKey ?: task.iconKey,
                    colorKey = occurrence?.overrideColorKey ?: task.colorKey,
                    completed = occurrence?.completedAt != null,
                    movable = false // mover una ocurrencia puntual sin tocar la regla es un paso aparte
                )
            }
        }
    }
}

private fun TaskEntity.toTimelineBlock(
    date: LocalDate,
    startMinutes: Int,
    durationMinutes: Int,
    title: String,
    iconKey: String,
    colorKey: String,
    completed: Boolean,
    movable: Boolean
) = TimelineBlock(
    id = uuid,
    source = TIMELINE_SOURCE_PLANNER,
    date = date,
    startMinutes = startMinutes,
    durationMinutes = durationMinutes,
    title = title,
    iconKey = iconKey,
    colorKey = colorKey,
    completed = completed,
    movable = movable,
    route = "planner/task/$id"
)
