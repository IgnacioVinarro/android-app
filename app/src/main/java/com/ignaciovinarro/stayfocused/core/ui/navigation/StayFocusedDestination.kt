package com.ignaciovinarro.stayfocused.core.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.ignaciovinarro.stayfocused.R

// Los 5 destinos de la barra inferior (CLAUDE.md: Hoy, Finanzas, Gimnasio, Meditación, Tiempo de pantalla;
// 5 es el máximo que admite la barra de Material 3, ticket 0025).
// Dieta no es un destino propio: es una pestaña dentro de Gimnasio (ver GymDietScreen, ticket 0023).
enum class StayFocusedDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @StringRes val contentDescriptionRes: Int,
    val icon: ImageVector
) {
    TODAY("today", R.string.nav_today, R.string.a11y_nav_today, Icons.Filled.Today),
    FINANCE("finance", R.string.nav_finance, R.string.a11y_nav_finance, Icons.Filled.AccountBalance),
    GYM("gym", R.string.nav_gym, R.string.a11y_nav_gym, Icons.Filled.FitnessCenter),
    MEDITATION("meditation", R.string.nav_meditation, R.string.a11y_nav_meditation, Icons.Filled.SelfImprovement),
    SCREEN_TIME("screentime", R.string.nav_screentime, R.string.a11y_nav_screentime, Icons.Filled.Smartphone),
}

// Ajustes NO está en la barra inferior: es un destino secundario que se abre desde "Hoy" (ticket 0025).
const val ROUTE_SETTINGS = "settings"
