package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtaskProgressTest {

    private val hoy = LocalDate.of(2026, 3, 5)
    private val manana = hoy.plusDays(1)

    private fun sub(id: Long, taskId: Long = 1, position: Int = 0, done: Boolean = false, status: Int = 0) =
        SubtaskEntity(id = id, taskId = taskId, title = "S$id", done = done, position = position, status = status)

    private fun tilde(subtaskId: Long, date: LocalDate, completedAt: Long? = 100L, status: Int = 0) =
        SubtaskCompletionEntity(subtaskId = subtaskId, date = date.toEpochDay(), completedAt = completedAt, status = status)

    // --- subtaskItemsFor ---

    @Test
    fun `tarea simple usa el done de la subtarea`() {
        val items = subtaskItemsFor(1, recurring = false, date = hoy, subtasks = listOf(sub(1, done = true), sub(2, position = 1)), completions = emptyList())

        assertEquals(listOf(true, false), items.map { it.done })
    }

    @Test
    fun `tarea simple ignora los tildes por ocurrencia`() {
        val items = subtaskItemsFor(1, false, hoy, listOf(sub(1)), listOf(tilde(1, hoy)))

        assertEquals(listOf(false), items.map { it.done })
    }

    @Test
    fun `tarea recurrente usa el tilde de esa fecha y no el done`() {
        val items = subtaskItemsFor(1, true, hoy, listOf(sub(1, done = true), sub(2, position = 1)), listOf(tilde(2, hoy)))

        assertEquals(listOf(false, true), items.map { it.done })
    }

    @Test
    fun `tarea recurrente no hereda el tilde de otro dia`() {
        val completions = listOf(tilde(1, hoy))

        assertEquals(listOf(true), subtaskItemsFor(1, true, hoy, listOf(sub(1)), completions).map { it.done })
        assertEquals(listOf(false), subtaskItemsFor(1, true, manana, listOf(sub(1)), completions).map { it.done })
    }

    @Test
    fun `tarea recurrente con tilde destildado cuenta como no hecha`() {
        val items = subtaskItemsFor(1, true, hoy, listOf(sub(1)), listOf(tilde(1, hoy, completedAt = null)))

        assertEquals(listOf(false), items.map { it.done })
    }

    @Test
    fun `ignora subtareas de otras tareas y las borradas`() {
        val subtasks = listOf(sub(1, taskId = 1), sub(2, taskId = 2), sub(3, taskId = 1, status = -1))

        val items = subtaskItemsFor(1, false, hoy, subtasks, emptyList())

        assertEquals(listOf(1L), items.map { it.id })
    }

    @Test
    fun `ignora los tildes borrados logicamente`() {
        val items = subtaskItemsFor(1, true, hoy, listOf(sub(1)), listOf(tilde(1, hoy, status = -1)))

        assertEquals(listOf(false), items.map { it.done })
    }

    @Test
    fun `devuelve las subtareas ordenadas por posicion`() {
        val subtasks = listOf(sub(1, position = 2), sub(2, position = 0), sub(3, position = 1))

        val items = subtaskItemsFor(1, false, hoy, subtasks, emptyList())

        assertEquals(listOf(2L, 3L, 1L), items.map { it.id })
    }

    // --- dayProgress ---

    @Test
    fun `el progreso suma tareas y subtareas`() {
        val progress = dayProgress(tasksDone = 1, tasksTotal = 3, subtasksDone = 2, subtasksTotal = 4)

        assertEquals(3, progress.done)
        assertEquals(7, progress.total)
    }

    @Test
    fun `la fraccion es hechas sobre total`() {
        assertEquals(0.5f, dayProgress(1, 2, 0, 0).fraction, 0.0001f)
        assertEquals(1f, dayProgress(2, 2, 3, 3).fraction, 0.0001f)
    }

    @Test
    fun `sin nada que hacer la fraccion es cero y el total tambien`() {
        val progress = dayProgress(0, 0, 0, 0)

        assertEquals(0f, progress.fraction, 0f)
        assertTrue(progress.total == 0)
    }
}
