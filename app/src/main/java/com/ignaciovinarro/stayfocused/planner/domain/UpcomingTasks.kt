package com.ignaciovinarro.stayfocused.planner.domain

import com.ignaciovinarro.stayfocused.core.timeline.TimelineBlock

// Cuántas tareas muestra el widget (PL-16).
const val WIDGET_MAX_TASKS = 4

// Una fila del widget. Sin ícono: un RemoteViews no puede dibujar los íconos de Compose; la fila lleva
// hora y título en texto y una franja del color de la tarea.
data class WidgetTask(val title: String, val startMinutes: Int, val colorKey: String)

// Lo que dibuja el widget: las próximas tareas y el progreso del día (tareas hechas / tareas con hora de hoy).
data class WidgetSnapshot(val tasks: List<WidgetTask>, val done: Int, val total: Int)

// Función pura (PL-16). "Próximas" = tareas de Planificación no completadas que todavía no terminaron
// (`inicio + duración > ahora`: la que está en curso también cuenta), por hora de inicio, hasta `limit`.
// El progreso cuenta solo las tareas de Planificación con hora (los bloques de otros módulos se completan
// en su propia pantalla, igual que en el encabezado de Hoy).
fun buildWidgetSnapshot(
    blocks: List<TimelineBlock>,
    nowMinutes: Int,
    limit: Int = WIDGET_MAX_TASKS
): WidgetSnapshot {
    val planner = blocks.filter { it.source == TIMELINE_SOURCE_PLANNER }
    val upcoming = planner
        .filter { !it.completed && it.startMinutes + it.durationMinutes > nowMinutes }
        .sortedBy { it.startMinutes }
        .take(limit)
        .map { WidgetTask(title = it.title, startMinutes = it.startMinutes, colorKey = it.colorKey) }
    return WidgetSnapshot(tasks = upcoming, done = planner.count { it.completed }, total = planner.size)
}
