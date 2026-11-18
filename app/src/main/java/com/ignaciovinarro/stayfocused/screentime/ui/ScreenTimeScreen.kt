package com.ignaciovinarro.stayfocused.screentime.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ignaciovinarro.stayfocused.R

// Pantalla provisoria: se reemplaza cuando se implemente tiempo de pantalla (fase 4). Por ahora solo
// ocupa su lugar en la barra inferior; todavía no lee el uso del teléfono ni pide ningún permiso.
@Composable
fun ScreenTimeScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.nav_screentime))
    }
}
