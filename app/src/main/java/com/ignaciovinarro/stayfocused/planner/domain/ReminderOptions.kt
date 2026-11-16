package com.ignaciovinarro.stayfocused.planner.domain

// Reglas puras de los recordatorios de una tarea (PL-10). `minutesBefore` = cuántos minutos antes
// de la hora de inicio avisa; 0 = "a la hora".

// Tope de lo que se puede escribir a mano: 7 días. Es una decisión de diseño del ticket 0012
// (no viene del documento funcional); se cambia acá.
const val MAX_REMINDER_MINUTES_BEFORE = 7 * 24 * 60

// Opciones de un toque: a la hora, 5, 10, 15, 30 min, 1 hora y 1 día antes.
val PRESET_REMINDER_MINUTES: List<Int> = listOf(0, 5, 10, 15, 30, 60, 24 * 60)

fun isValidReminderMinutes(minutesBefore: Int): Boolean = minutesBefore in 0..MAX_REMINDER_MINUTES_BEFORE

// Unidad en la que se muestra un recordatorio: la más grande que lo divide exacto (90 min se ve
// "90 minutos", no "1,5 horas"; 120 min se ve "2 horas").
enum class ReminderUnit { MINUTES, HOURS, DAYS }

data class ReminderAmount(val value: Int, val unit: ReminderUnit)

fun reminderAmount(minutesBefore: Int): ReminderAmount = when {
    minutesBefore > 0 && minutesBefore % (24 * 60) == 0 -> ReminderAmount(minutesBefore / (24 * 60), ReminderUnit.DAYS)
    minutesBefore > 0 && minutesBefore % 60 == 0 -> ReminderAmount(minutesBefore / 60, ReminderUnit.HOURS)
    else -> ReminderAmount(minutesBefore, ReminderUnit.MINUTES)
}

// Recordatorios elegidos, sin repetidos y de menor a mayor antelación. Descarta valores inválidos.
fun normalizeReminderMinutes(minutesBefore: Collection<Int>): List<Int> =
    minutesBefore.filter(::isValidReminderMinutes).distinct().sorted()
