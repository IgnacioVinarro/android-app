package com.ignaciovinarro.stayfocused.core.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Tab bar reutilizable para pantallas con varias vistas (ej. Gimnasio / Dieta). Solo dibuja: recibe los
// títulos y cuál está elegida, y avisa el toque; no sabe qué contiene cada pestaña. La elegida se
// distingue por el indicador y el estado de selección (no solo por el color).
@OptIn(ExperimentalMaterial3Api::class) // PrimaryTabRow es experimental en Material 3 1.3
@Composable
fun SectionTabRow(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryTabRow(selectedTabIndex = selectedIndex, modifier = modifier) {
        titles.forEachIndexed { index, title ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                text = { Text(title) }
            )
        }
    }
}
