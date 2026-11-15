package com.ignaciovinarro.stayfocused.planner.domain

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class TaskTimelineSourceTest {

    private val taskRepository = mockk<TaskRepository>()
    private val source = TaskTimelineSource(taskRepository)
    private val fecha = LocalDate.of(2026, 3, 5)

    private fun tarea(
        title: String,
        startMinutes: Int?,
        completedAt: Long? = null
    ) = TaskEntity(
        title = title,
        date = fecha.toEpochDay(),
        startMinutes = startMinutes,
        durationMinutes = 30,
        iconKey = "task_default",
        colorKey = "blue",
        completedAt = completedAt
    )

    @Test
    fun `filtra las tareas sin hora, que van a la bandeja`() = runTest {
        every { taskRepository.observeByDate(fecha.toEpochDay()) } returns flowOf(
            listOf(tarea("Con hora", startMinutes = 9 * 60), tarea("Sin hora", startMinutes = null))
        )

        source.blocksFor(fecha).test {
            val bloques = awaitItem()
            assertEquals(1, bloques.size)
            assertEquals("Con hora", bloques.first().title)
            awaitComplete()
        }
    }

    @Test
    fun `mapea los campos de la tarea al bloque`() = runTest {
        every { taskRepository.observeByDate(fecha.toEpochDay()) } returns flowOf(
            listOf(tarea("Reunión", startMinutes = 9 * 60, completedAt = 123L))
        )

        source.blocksFor(fecha).test {
            val bloque = awaitItem().single()
            assertEquals(TIMELINE_SOURCE_PLANNER, bloque.source)
            assertEquals(fecha, bloque.date)
            assertEquals(9 * 60, bloque.startMinutes)
            assertEquals(30, bloque.durationMinutes)
            assertTrue(bloque.completed)
            assertEquals("planner/task/0", bloque.route)
            awaitComplete()
        }
    }
}
