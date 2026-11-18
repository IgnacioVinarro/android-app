package com.ignaciovinarro.stayfocused.planner.domain

const val MIN_FOCUS_MINUTES = 1
const val MAX_FOCUS_MINUTES = 240

// Duraciones rápidas del diálogo de inicio; además se puede escribir una a mano.
val FOCUS_PRESET_MINUTES = listOf(25, 50)

private const val MILLIS_IN_MINUTE = 60_000L

enum class FocusStatus { RUNNING, PAUSED }

// Una sesión de enfoque (PL-13). Todos los tiempos son del RELOJ MONOTÓNICO (milisegundos desde el
// arranque del teléfono, `elapsedRealtime`), no de la hora del día: no se rompe si el usuario cambia
// la hora o la zona, y no se "cuentan segundos" (CLAUDE.md).
// - RUNNING: `endAtMs` es el instante (monotónico) en que termina; `remainingMs` no se usa.
// - PAUSED: `remainingMs` es lo que faltaba al pausar; `endAtMs` no se usa.
data class FocusSession(
    val taskId: Long,
    val taskTitle: String,
    val totalMs: Long,
    val status: FocusStatus,
    val endAtMs: Long,
    val remainingMs: Long
)

fun isValidFocusMinutes(minutes: Int): Boolean = minutes in MIN_FOCUS_MINUTES..MAX_FOCUS_MINUTES

// Devuelve nulo si la duración no es válida: no se puede arrancar un enfoque de 0 minutos.
fun startFocus(taskId: Long, taskTitle: String, minutes: Int, nowMs: Long): FocusSession? {
    if (!isValidFocusMinutes(minutes)) return null
    val totalMs = minutes * MILLIS_IN_MINUTE
    return FocusSession(
        taskId = taskId,
        taskTitle = taskTitle,
        totalMs = totalMs,
        status = FocusStatus.RUNNING,
        endAtMs = nowMs + totalMs,
        remainingMs = totalMs
    )
}

// Cuánto falta, nunca negativo.
fun FocusSession.remainingAt(nowMs: Long): Long = when (status) {
    FocusStatus.RUNNING -> (endAtMs - nowMs).coerceAtLeast(0)
    FocusStatus.PAUSED -> remainingMs
}

// Pausar congela lo que falta. Si ya estaba en pausa, no cambia nada.
fun FocusSession.pausedAt(nowMs: Long): FocusSession =
    if (status == FocusStatus.PAUSED) this else copy(status = FocusStatus.PAUSED, remainingMs = remainingAt(nowMs))

// Reanudar vuelve a correr desde lo que quedaba. Si ya estaba corriendo, no cambia nada.
fun FocusSession.resumedAt(nowMs: Long): FocusSession =
    if (status == FocusStatus.RUNNING) this else copy(status = FocusStatus.RUNNING, endAtMs = nowMs + remainingMs)

// Solo una sesión corriendo puede terminar: una en pausa está congelada.
fun FocusSession.isFinishedAt(nowMs: Long): Boolean =
    status == FocusStatus.RUNNING && remainingAt(nowMs) == 0L
