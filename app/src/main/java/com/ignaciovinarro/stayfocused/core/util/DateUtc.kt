package com.ignaciovinarro.stayfocused.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// El selector de fecha de Material 3 trabaja con milisegundos a las 00:00 UTC del día elegido (sin
// zona horaria). Estas dos funciones convierten entre ese valor y un LocalDate sin que el huso del
// teléfono corra el día.
fun LocalDate.toEpochMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
