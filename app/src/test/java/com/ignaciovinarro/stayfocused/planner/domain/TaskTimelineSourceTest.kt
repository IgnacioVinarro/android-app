package com.ignaciovinarro.stayfocused.planner.domain

import app.cash.turbine.test
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class TaskTimelineSourceTest {

    private val taskRepository = mockk<TaskRepository>()
    private val taskOccurrenceRepository = mockk<TaskOccurrenceRepository>()
    private val source = TaskTimelineSource(taskRepository, taskOccurrenceRepository)
    private val fecha = LocalDate.of(2026, 3, 5) // jueves

    private fun tarea(
        id: Long = 0,
        title: String,
        startMinutes: Int?,
        completedAt: Long? = null,
        date: LocalDate = fecha,
        recurrenceType: String? = null,
        recurrenceInterval: Int? = null
    ) = TaskEntity(
        id = id,
        title = title,
        date = date.toEpochDay(),
        startMinutes = startMinutes,
        durationMinutes = 30,
        iconKey = "task_default",
        colorKey = "blue",
        completedAt = completedAt,
        recurrenceType = recurrenceType,
        recurrenceInterval = recurrenceInterval
    )

    private fun stub(tasks: List<TaskEntity>, occurrences: List<TaskOccurrenceEntity> = emptyList()) {
        every { taskRepository.observeByDate(fecha.toEpochDay()) } returns flowOf(tasks)
        every {
            taskOccurrenceRepository.observeByDateRange(fecha.toEpochDay(), fecha.toEpochDay())
        } returns flowOf(occurrences)
    }

    @Test
    fun `filtra las tareas sin hora, que van a la bandeja`() = runTest {
        stub(listOf(tarea(title = "Con hora", startMinutes = 9 * 60), tarea(title = "Sin hora", startMinutes = null)))

        source.blocksFor(fecha).test {
            val bloques = awaitItem()
            assertEquals(1, bloques.size)
            assertEquals("Con hora", bloques.first().title)
            awaitComplete()
        }
    }

    @Test
    fun `mapea los campos de la tarea al bloque`() = runTest {
        stub(listOf(tarea(title = "Reunión", startMinutes = 9 * 60, completedAt = 123L)))

        source.blocksFor(fecha).test {
            val bloque = awaitItem().single()
            assertEquals(TIMELINE_SOURCE_PLANNER, bloque.source)
            assertEquals(fecha, bloque.date)
            assertEquals(9 * 60, bloque.startMinutes)
            assertEquals(30, bloque.durationMinutes)
            assertTrue(bloque.completed)
            assertTrue(bloque.movable)
            assertEquals("planner/task/0", bloque.route)
            awaitComplete()
        }
    }

    @Test
    fun `una tarea recurrente solo aparece el dia que le toca segun occursOn`() = runTest {
        // Diaria desde un lunes: el jueves (fecha) sí tiene ocurrencia.
        stub(
            listOf(
                tarea(
                    title = "Tomar agua",
                    startMinutes = 8 * 60,
                    date = fecha.minusDays(3),
                    recurrenceType = RecurrenceType.DAILY.name
                )
            )
        )

        source.blocksFor(fecha).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    @Test
    fun `una tarea recurrente no aparece un dia que no le toca`() = runTest {
        stub(
            listOf(
                tarea(
                    title = "Cada 3 dias",
                    startMinutes = 8 * 60,
                    date = fecha.minusDays(1), // un día antes: no cae cada 3 días
                    recurrenceType = RecurrenceType.EVERY_N_DAYS.name,
                    recurrenceInterval = 3
                )
            )
        )

        source.blocksFor(fecha).test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `las ocurrencias de tareas recurrentes no se pueden mover`() = runTest {
        stub(
            listOf(
                tarea(
                    id = 5L,
                    title = "Tomar agua",
                    startMinutes = 8 * 60,
                    date = fecha,
                    recurrenceType = RecurrenceType.DAILY.name
                )
            )
        )

        source.blocksFor(fecha).test {
            assertFalse(awaitItem().single().movable)
            awaitComplete()
        }
    }

    @Test
    fun `una ocurrencia completada usa el completedAt de task_occurrence, no el de la tarea`() = runTest {
        val tarea = tarea(
            id = 5L,
            title = "Tomar agua",
            startMinutes = 8 * 60,
            date = fecha,
            recurrenceType = RecurrenceType.DAILY.name,
            completedAt = null // la tarea en sí no está "completada"
        )
        val ocurrencia = TaskOccurrenceEntity(taskId = 5L, date = fecha.toEpochDay(), completedAt = 999L)
        stub(listOf(tarea), listOf(ocurrencia))

        source.blocksFor(fecha).test {
            assertTrue(awaitItem().single().completed)
            awaitComplete()
        }
    }

    @Test
    fun `una ocurrencia saltada no se muestra`() = runTest {
        val tarea = tarea(
            id = 5L,
            title = "Tomar agua",
            startMinutes = 8 * 60,
            date = fecha,
            recurrenceType = RecurrenceType.DAILY.name
        )
        val ocurrencia = TaskOccurrenceEntity(taskId = 5L, date = fecha.toEpochDay(), skipped = true)
        stub(listOf(tarea), listOf(ocurrencia))

        source.blocksFor(fecha).test {
            assertTrue(awaitItem().isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `una ocurrencia con hora propia usa overrideStartMinutes`() = runTest {
        val tarea = tarea(
            id = 5L,
            title = "Tomar agua",
            startMinutes = 8 * 60,
            date = fecha,
            recurrenceType = RecurrenceType.DAILY.name
        )
        val ocurrencia = TaskOccurrenceEntity(taskId = 5L, date = fecha.toEpochDay(), overrideStartMinutes = 10 * 60)
        stub(listOf(tarea), listOf(ocurrencia))

        source.blocksFor(fecha).test {
            assertEquals(10 * 60, awaitItem().single().startMinutes)
            awaitComplete()
        }
    }
}
