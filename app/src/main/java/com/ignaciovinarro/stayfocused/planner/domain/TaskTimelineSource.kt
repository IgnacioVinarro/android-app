package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.core.timeline.TimelineBlock
import com.ignaciovinarro.stayfocused.core.timeline.TimelineSource
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val taskRepository: TaskRepository
) : TimelineSource {

    // Las tareas sin hora van a la bandeja (Inbox): no entran a la línea de tiempo.
    override fun blocksFor(date: LocalDate): Flow<List<TimelineBlock>> =
        taskRepository.observeByDate(date.toEpochDay()).map { tasks ->
            tasks.filter { it.startMinutes != null }.map { it.toTimelineBlock() }
        }
}

private fun TaskEntity.toTimelineBlock() = TimelineBlock(
    id = uuid,
    source = TIMELINE_SOURCE_PLANNER,
    date = LocalDate.ofEpochDay(date),
    startMinutes = requireNotNull(startMinutes),
    durationMinutes = durationMinutes,
    title = title,
    iconKey = iconKey,
    colorKey = colorKey,
    completed = completedAt != null,
    route = "planner/task/$id"
)
