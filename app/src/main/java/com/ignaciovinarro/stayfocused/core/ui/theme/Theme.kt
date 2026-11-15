package com.ignaciovinarro.stayfocused.core.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = StayFocusedColors.Primary,
    onPrimary = StayFocusedColors.OnPrimary,
    primaryContainer = StayFocusedColors.PrimaryContainer,
    onPrimaryContainer = StayFocusedColors.OnPrimaryContainer,
    secondary = StayFocusedColors.Secondary,
    onSecondary = StayFocusedColors.OnSecondary,
    secondaryContainer = StayFocusedColors.SecondaryContainer,
    onSecondaryContainer = StayFocusedColors.OnSecondaryContainer,
    tertiary = StayFocusedColors.Tertiary,
    onTertiary = StayFocusedColors.OnTertiary,
    tertiaryContainer = StayFocusedColors.TertiaryContainer,
    onTertiaryContainer = StayFocusedColors.OnTertiaryContainer,
    error = StayFocusedColors.Error,
    onError = StayFocusedColors.OnError,
    errorContainer = StayFocusedColors.ErrorContainer,
    onErrorContainer = StayFocusedColors.OnErrorContainer,
    surface = StayFocusedColors.Surface,
    onSurface = StayFocusedColors.OnSurface,
    surfaceVariant = StayFocusedColors.SurfaceVariant,
    onSurfaceVariant = StayFocusedColors.OnSurfaceVariant,
    outline = StayFocusedColors.Outline,
    outlineVariant = StayFocusedColors.OutlineVariant,
    inverseSurface = StayFocusedColors.InverseSurface,
    inverseOnSurface = StayFocusedColors.InverseOnSurface,
    inversePrimary = StayFocusedColors.InversePrimary,
    scrim = StayFocusedColors.Scrim,
    surfaceContainerLowest = StayFocusedColors.SurfaceContainerLowest,
    surfaceContainerLow = StayFocusedColors.SurfaceContainerLow,
    surfaceContainer = StayFocusedColors.SurfaceContainer,
    surfaceContainerHigh = StayFocusedColors.SurfaceContainerHigh,
    surfaceContainerHighest = StayFocusedColors.SurfaceContainerHighest,
)

private val LightColorScheme = lightColorScheme(
    primary = StayFocusedColors.Primary,
    onPrimary = StayFocusedColors.OnPrimary,
    primaryContainer = StayFocusedColors.PrimaryContainer,
    onPrimaryContainer = StayFocusedColors.OnPrimaryContainer,
    secondary = StayFocusedColors.Secondary,
    onSecondary = StayFocusedColors.OnSecondary,
    secondaryContainer = StayFocusedColors.SecondaryContainer,
    onSecondaryContainer = StayFocusedColors.OnSecondaryContainer,
    tertiary = StayFocusedColors.Tertiary,
    onTertiary = StayFocusedColors.OnTertiary,
    tertiaryContainer = StayFocusedColors.TertiaryContainer,
    onTertiaryContainer = StayFocusedColors.OnTertiaryContainer,
    error = StayFocusedColors.Error,
    onError = StayFocusedColors.OnError,
    errorContainer = StayFocusedColors.ErrorContainer,
    onErrorContainer = StayFocusedColors.OnErrorContainer,
    surface = StayFocusedColors.Surface,
    onSurface = StayFocusedColors.OnSurface,
    surfaceVariant = StayFocusedColors.SurfaceVariant,
    onSurfaceVariant = StayFocusedColors.OnSurfaceVariant,
    outline = StayFocusedColors.Outline,
    outlineVariant = StayFocusedColors.OutlineVariant,
    inverseSurface = StayFocusedColors.InverseSurface,
    inverseOnSurface = StayFocusedColors.InverseOnSurface,
    inversePrimary = StayFocusedColors.InversePrimary,
    scrim = StayFocusedColors.Scrim,
    surfaceContainerLowest = StayFocusedColors.SurfaceContainerLowest,
    surfaceContainerLow = StayFocusedColors.SurfaceContainerLow,
    surfaceContainer = StayFocusedColors.SurfaceContainer,
    surfaceContainerHigh = StayFocusedColors.SurfaceContainerHigh,
    surfaceContainerHighest = StayFocusedColors.SurfaceContainerHighest,
)

@Composable
fun StayFocusedTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = androidx.compose.ui.platform.LocalView.current
    val context = view.context

    SideEffect {
        val activity = (context as? Activity) ?: return@SideEffect
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = colorScheme.surface.toArgb()
        window.navigationBarColor = colorScheme.surface.toArgb()
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StayFocusedTypography,
        content = content
    )
}