package com.ignaciovinarro.stayfocused.core.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ignaciovinarro.stayfocused.R

// Selectores de ícono y color para tareas (y cualquier otro ítem que use la misma paleta, como
// los ítems de rutina): viven acá para no duplicarlos en cada pantalla (CLAUDE.md, regla de
// reuso).
@Composable
fun TaskIconSelector(selectedKey: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskIconKey.entries.forEach { iconOption ->
            IconOption(
                icon = iconOption,
                selected = iconOption.key == selectedKey,
                onClick = { onSelect(iconOption.key) }
            )
        }
    }
}

@Composable
fun TaskColorSelector(selectedKey: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskColorKey.entries.forEach { colorOption ->
            ColorOption(
                colorOption = colorOption,
                selected = colorOption.key == selectedKey,
                onClick = { onSelect(colorOption.key) }
            )
        }
    }
}

@Composable
private fun IconOption(icon: TaskIconKey, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(48.dp)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon.icon,
                contentDescription = if (selected) stringResource(R.string.a11y_task_icon_selected) else null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorOption(colorOption: TaskColorKey, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colorOption.color,
        modifier = Modifier.size(40.dp)
    ) {
        if (selected) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.a11y_task_color_selected),
                    tint = Color.White
                )
            }
        }
    }
}
