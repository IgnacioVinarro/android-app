package com.ignaciovinarro.stayfocused.core.timeline

// Un hueco libre del día (PL-03): de `startMinutes` (inclusive) a `endMinutes` (exclusivo), en minutos
// desde las 00:00.
data class FreeGap(val startMinutes: Int, val endMinutes: Int) {
    val durationMinutes: Int get() = endMinutes - startMinutes
}

// Un hueco más corto que esto no se muestra: es la duración por defecto de una tarea, así que nada
// "entraría" ahí.
const val MIN_FREE_GAP_MINUTES = 30

// Función pura (PL-03): los tramos libres del día entre los bloques, incluidos el de antes del primero
// y el de después del último. Los bloques que se superponen se juntan primero, así que no sale ningún
// hueco "adentro" de otro bloque. Cuentan los bloques de todos los módulos: un hueco es tiempo libre.
// Un día sin bloques no tiene huecos (sería el día entero: no hay a qué hora precargar nada).
fun freeGaps(blocks: List<TimelineBlock>, minGapMinutes: Int = MIN_FREE_GAP_MINUTES): List<FreeGap> {
    if (blocks.isEmpty()) return emptyList()

    val busy = blocks
        .map { it.startMinutes to (it.startMinutes + it.durationMinutes).coerceAtMost(MINUTES_IN_DAY) }
        .sortedBy { it.first }

    val gaps = mutableListOf<FreeGap>()
    var cursor = 0 // hasta dónde está ocupado el día
    for ((start, end) in busy) {
        if (start - cursor >= minGapMinutes) gaps += FreeGap(cursor, start)
        cursor = maxOf(cursor, end)
    }
    if (MINUTES_IN_DAY - cursor >= minGapMinutes) gaps += FreeGap(cursor, MINUTES_IN_DAY)
    return gaps
}

// Duración con la que se precarga una tarea creada en un hueco: la de por defecto, o menos si el hueco
// es más corto, para que la tarea nueva entre sin pisar la siguiente. Nunca menos que la mínima de un bloque.
fun durationForGap(defaultMinutes: Int, gapMinutes: Int): Int =
    minOf(defaultMinutes, gapMinutes).coerceAtLeast(MIN_BLOCK_DURATION_MINUTES)
