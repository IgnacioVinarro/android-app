package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineApplierTest {

    private val dia = LocalDate.of(2026, 3, 2)

    private fun routine(startMinutes: Int) = RoutineEntity(id = 1L, name = "Rutina de mañana", startMinutes = startMinutes)

    private fun item(offsetMinutes: Int, durationMinutes: Int = 30, position: Int = 0) = RoutineItemEntity(
        routineId = 1L,
        title = "Ítem",
        offsetMinutes = offsetMinutes,
        durationMinutes = durationMinutes,
        iconKey = "task_default",
        colorKey = "blue",
        position = position
    )

    @Test
    fun `cada tarea generada usa el ancla mas el offset del item`() {
        val tareas = applyRoutine(routine(startMinutes = 420), listOf(item(offsetMinutes = 0), item(offsetMinutes = 30, position = 1)), dia)

        assertEquals(2, tareas.size)
        assertEquals(420, tareas[0].startMinutes)
        assertEquals(450, tareas[1].startMinutes)
    }

    @Test
    fun `offset negativo corre la tarea antes de la hora ancla`() {
        val tareas = applyRoutine(routine(startMinutes = 420), listOf(item(offsetMinutes = -30)), dia)

        assertEquals(390, tareas.single().startMinutes)
    }

    @Test
    fun `se enclava dentro del dia si el offset se va de rango`() {
        val tareas = applyRoutine(routine(startMinutes = 0), listOf(item(offsetMinutes = -120)), dia)

        assertEquals(0, tareas.single().startMinutes)
    }

    @Test
    fun `cada tarea queda enlazada a la rutina, con fecha, icono y color del item`() {
        val rutina = routine(startMinutes = 420)
        val tarea = applyRoutine(rutina, listOf(item(offsetMinutes = 0, durationMinutes = 45)), dia).single()

        assertEquals(rutina.id, tarea.routineId)
        assertEquals(dia.toEpochDay(), tarea.date)
        assertEquals(45, tarea.durationMinutes)
        assertEquals("task_default", tarea.iconKey)
        assertEquals("blue", tarea.colorKey)
        assertEquals(null, tarea.recurrenceType)
    }
}
