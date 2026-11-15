package com.ignaciovinarro.stayfocused.core.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.ignaciovinarro.stayfocused.R

// Los 5 destinos principales de la barra inferior (CLAUDE.md: Hoy, Finanzas, Gimnasio, Alimentación, Meditación)
enum class StayFocusedDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @StringRes val contentDescriptionRes: Int,
    val icon: ImageVector
) {
    TODAY("today", R.string.nav_today, R.string.a11y_nav_today, Icons.Filled.Today),
    FINANCE("finance", R.string.nav_finance, R.string.a11y_nav_finance, Icons.Filled.AccountBalance),
    GYM("gym", R.string.nav_gym, R.string.a11y_nav_gym, Icons.Filled.FitnessCenter),
    NUTRITION("nutrition", R.string.nav_nutrition, R.string.a11y_nav_nutrition, Icons.Filled.Restaurant),
    MEDITATION("meditation", R.string.nav_meditation, R.string.a11y_nav_meditation, Icons.Filled.SelfImprovement),
}
