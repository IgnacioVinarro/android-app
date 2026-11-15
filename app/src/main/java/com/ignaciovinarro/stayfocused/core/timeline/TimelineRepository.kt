package com.ignaciovinarro.stayfocused.core.timeline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject

class TimelineRepository @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards TimelineSource>
) {
    fun blocksFor(date: LocalDate): Flow<List<TimelineBlock>> {
        if (sources.isEmpty()) return flowOf(emptyList())
        val flows = sources.map { it.blocksFor(date) }
        return combine(flows) { blocksPorFuente ->
            blocksPorFuente.flatMap { it }.sortedBy { it.startMinutes }
        }
    }
}
