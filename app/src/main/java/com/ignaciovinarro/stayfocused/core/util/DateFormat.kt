package com.ignaciovinarro.stayfocused.core.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// Fecha corta al estilo argentino (`05/11/2026`), igual en cualquier teléfono.
fun formatShortDate(date: LocalDate): String = date.format(SHORT_DATE)

private val MONTH_YEAR = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("es", "AR"))

// Mes y año en español, con mayúscula inicial (`Noviembre 2026`), sin depender del idioma del teléfono.
fun formatMonthYear(month: YearMonth): String =
    month.format(MONTH_YEAR).replaceFirstChar { it.uppercase() }

// Mes abreviado a tres letras (`Nov`), para las etiquetas del eje de un gráfico.
fun formatMonthShort(month: YearMonth): String = formatMonthYear(month).substringBefore(' ').take(3)
