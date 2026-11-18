package com.ignaciovinarro.stayfocused.core.util

private const val MILLIS_IN_SECOND = 1000L
private const val SECONDS_IN_MINUTE = 60L
private const val SECONDS_IN_HOUR = 3600L

// Función pura: "mm:ss" (o "h:mm:ss" si pasa de una hora). Redondea los segundos hacia ARRIBA: así
// "00:00" solo se ve cuando el tiempo realmente se acabó, no 900 ms antes. Negativos cuentan como 0.
fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0) + MILLIS_IN_SECOND - 1) / MILLIS_IN_SECOND
    val hours = totalSeconds / SECONDS_IN_HOUR
    val minutes = (totalSeconds % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE
    val seconds = totalSeconds % SECONDS_IN_MINUTE
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
