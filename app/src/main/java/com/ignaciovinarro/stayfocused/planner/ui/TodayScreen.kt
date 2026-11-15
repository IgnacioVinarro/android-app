package com.ignaciovinarro.stayfocused.planner.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ignaciovinarro.stayfocused.R

// Pantalla provisoria: se reemplaza cuando se implemente la línea de tiempo (fase 1)
@Composable
fun TodayScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.nav_today))
    }
}
