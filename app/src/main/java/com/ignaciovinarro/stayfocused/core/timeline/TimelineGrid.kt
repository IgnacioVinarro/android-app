package com.ignaciovinarro.stayfocused.core.timeline

const val TIMELINE_SNAP_MINUTES = 5
const val MIN_BLOCK_DURATION_MINUTES = 5
internal const val MINUTES_IN_DAY = 24 * 60

// Funciones puras para mover/redimensionar bloques (PL-07): enganchan al intervalo de 5 minutos
// y evitan que un bloque quede fuera del día (antes de medianoche o después de las 23:59).

fun snapToGrid(minutes: Int, snapMinutes: Int = TIMELINE_SNAP_MINUTES): Int =
    Math.round(minutes / snapMinutes.toDouble()).toInt() * snapMinutes

fun clampStartMinutes(startMinutes: Int, durationMinutes: Int): Int =
    startMinutes.coerceIn(0, (MINUTES_IN_DAY - durationMinutes).coerceAtLeast(0))

fun clampDurationMinutes(durationMinutes: Int, startMinutes: Int): Int =
    durationMinutes.coerceIn(MIN_BLOCK_DURATION_MINUTES, (MINUTES_IN_DAY - startMinutes).coerceAtLeast(MIN_BLOCK_DURATION_MINUTES))
