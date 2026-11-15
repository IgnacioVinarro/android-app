package com.ignaciovinarro.stayfocused.core.timeline

import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineRepositoryTest {

    private val fecha = LocalDate.of(2026, 3, 5)

    private fun bloque(id: String, startMinutes: Int) = TimelineBlock(
        id = id,
        source = "fake",
        date = fecha,
        startMinutes = startMinutes,
        durationMinutes = 30,
        title = id,
        iconKey = "task_default",
        colorKey = "blue",
        completed = false,
        route = "fake/$id"
    )

    private class FakeTimelineSource(private val blocks: List<TimelineBlock>) : TimelineSource {
        override fun blocksFor(date: LocalDate) = flowOf(blocks)
    }

    @Test
    fun `sin fuentes devuelve lista vacia`() = runTest {
        val repository = TimelineRepository(emptySet())

        repository.blocksFor(fecha).test {
            assertEquals(emptyList<TimelineBlock>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `combina y ordena los bloques de todas las fuentes por hora de inicio`() = runTest {
        val fuente1 = FakeTimelineSource(listOf(bloque("b", startMinutes = 10 * 60)))
        val fuente2 = FakeTimelineSource(listOf(bloque("a", startMinutes = 9 * 60)))
        val repository = TimelineRepository(setOf(fuente1, fuente2))

        repository.blocksFor(fecha).test {
            val bloques = awaitItem()
            assertEquals(listOf("a", "b"), bloques.map { it.id })
            awaitComplete()
        }
    }
}
