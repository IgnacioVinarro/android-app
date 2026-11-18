package com.ignaciovinarro.stayfocused.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Choice(val id: Long, val label: String)

// Elección única entre pocas opciones (cuentas, categorías…), en filas que se acomodan solas. La
// opción elegida lleva un tilde además del color, para que el estado no dependa solo del color.
@Composable
fun ChoiceChips(
    choices: List<Choice>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    MultiChoiceChips(
        choices = choices,
        selectedIds = setOfNotNull(selectedId),
        onToggle = onSelect,
        modifier = modifier
    )
}

// Elección múltiple (etiquetas…): igual que ChoiceChips, pero cada toque prende o apaga una opción.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiChoiceChips(
    choices: List<Choice>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        choices.forEach { choice ->
            val selected = choice.id in selectedIds
            FilterChip(
                selected = selected,
                onClick = { onToggle(choice.id) },
                label = { Text(choice.label) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                }
            )
        }
    }
}
