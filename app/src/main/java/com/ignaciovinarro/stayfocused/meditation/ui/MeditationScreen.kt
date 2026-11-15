package com.ignaciovinarro.stayfocused.meditation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ignaciovinarro.stayfocused.R

// Pantalla provisoria: se reemplaza cuando se implemente meditación (fase 3)
@Composable
fun MeditationScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.nav_meditation))
    }
}
