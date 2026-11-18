package com.ignaciovinarro.stayfocused.core.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// Fecha corta al estilo argentino (`05/11/2026`), igual en cualquier teléfono.
fun formatShortDate(date: LocalDate): String = date.format(SHORT_DATE)
