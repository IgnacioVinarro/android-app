package com.ignaciovinarro.stayfocused.core.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.util.toEpochMillisUtc
import com.ignaciovinarro.stayfocused.core.util.toLocalDateUtc
import java.time.LocalDate

// Selector de fecha reutilizable (formulario de tarea, fin de recurrencia, reprogramar atrasadas…).
// Entrega un LocalDate y esconde la conversión UTC. Con `minDate`, los días anteriores quedan
// deshabilitados.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate? = null
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochMillisUtc(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                minDate == null || !utcTimeMillis.toLocalDateUtc().isBefore(minDate)
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis -> onConfirm(millis.toLocalDateUtc()) }
                onDismiss()
            }) { Text(stringResource(R.string.action_done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    ) { DatePicker(state = state) }
}
