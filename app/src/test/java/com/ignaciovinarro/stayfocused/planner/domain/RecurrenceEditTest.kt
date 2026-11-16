package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenceEditTest {

    private val original = TaskEntity(
        id = 1L,
        title = "Gimnasio",
        notes = "Pierna",
        date = LocalDate.of(2026, 1, 5).toEpochDay(), // lunes
        startMinutes = 420,
        durationMinutes = 60,
        iconKey = "task_gym",
        colorKey = "blue",
        recurrenceType = RecurrenceType.WEEKLY.name,
        recurrenceEnd = LocalDate.of(2026, 6, 1).toEpochDay(),
        routineId = 7L
    )

    private val edit = TaskEditFields(
        title = "Gimnasio (editado)",
        notes = "Espalda",
        startMinutes = 450,
        durationMinutes = 45,
        iconKey = "task_gym2",
        colorKey = "green"
    )

    @Test
    fun `aplicar a todas pisa los campos editables y conserva id, uuid y recurrencia`() {
        val editado = applyEditToAll(original, edit)

        assertEquals(original.id, editado.id)
        assertEquals(original.uuid, editado.uuid)
        assertEquals(edit.title, editado.title)
        assertEquals(edit.notes, editado.notes)
        assertEquals(edit.startMinutes, editado.startMinutes)
        assertEquals(edit.durationMinutes, editado.durationMinutes)
        assertEquals(edit.iconKey, editado.iconKey)
        assertEquals(edit.colorKey, editado.colorKey)
        assertEquals(original.recurrenceType, editado.recurrenceType)
        assertEquals(original.recurrenceEnd, editado.recurrenceEnd)
    }

    @Test
    fun `esta y siguientes recorta la original y crea una tarea nueva desde esa fecha`() {
        val fromDate = LocalDate.of(2026, 1, 19) // tercer lunes

        val (truncada, nueva) = applyEditFromDate(original, edit, fromDate)

        assertEquals(LocalDate.of(2026, 1, 18).toEpochDay(), truncada.recurrenceEnd)
        assertEquals(original.title, truncada.title) // la original no cambia sus campos de contenido

        requireNotNull(nueva)
        assertEquals(fromDate.toEpochDay(), nueva.date)
        assertEquals(edit.title, nueva.title)
        assertEquals(edit.startMinutes, nueva.startMinutes)
        assertEquals(original.recurrenceType, nueva.recurrenceType)
        assertEquals(original.recurrenceEnd, nueva.recurrenceEnd) // el fin original sigue siendo el fin
        assertEquals(original.routineId, nueva.routineId)
    }

    @Test
    fun `esta y siguientes desde la primera ocurrencia equivale a editar todas`() {
        val fromDate = LocalDate.ofEpochDay(original.date)

        val (resultado, nueva) = applyEditFromDate(original, edit, fromDate)

        assertNull(nueva)
        assertEquals(edit.title, resultado.title)
        assertEquals(original.id, resultado.id)
    }

    @Test
    fun `truncar la recurrencia solo recorta el fin, sin tocar el resto de los campos`() {
        val fromDate = LocalDate.of(2026, 1, 19)

        val truncada = truncateRecurrenceFromDate(original, fromDate)

        assertEquals(LocalDate.of(2026, 1, 18).toEpochDay(), truncada.recurrenceEnd)
        assertEquals(original.title, truncada.title)
        assertEquals(original.id, truncada.id)
    }
}
