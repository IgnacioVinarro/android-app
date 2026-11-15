package com.ignaciovinarro.stayfocused.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta base (se usa para construir los esquemas claro/oscuro)
object StayFocusedColors {
    // Primarios
    val Primary = Color(0xFF4A6FA5)
    val OnPrimary = Color.White
    val PrimaryContainer = Color(0xFFD6E4F0)
    val OnPrimaryContainer = Color(0xFF002040)

    // Secundarios
    val Secondary = Color(0xFF5C6B7A)
    val OnSecondary = Color.White
    val SecondaryContainer = Color(0xFFDDE3EB)
    val OnSecondaryContainer = Color(0xFF1A232C)

    // Terciarios (acento)
    val Tertiary = Color(0xFF7A5C8A)
    val OnTertiary = Color.White
    val TertiaryContainer = Color(0xFFEDE0F0)
    val OnTertiaryContainer = Color(0xFF2E1A3A)

    // Error
    val Error = Color(0xFFBA1A1A)
    val OnError = Color.White
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF410002)

    // Superficies
    val Surface = Color(0xFFFFFBFE)
    val OnSurface = Color(0xFF1C1B1F)
    val SurfaceVariant = Color(0xFFE7E0EC)
    val OnSurfaceVariant = Color(0xFF49454F)
    val Outline = Color(0xFF79747E)
    val OutlineVariant = Color(0xFFCAC4D0)

    // Inversos
    val InverseSurface = Color(0xFF313033)
    val InverseOnSurface = Color(0xFFF4EFF4)
    val InversePrimary = Color(0xFFA0C4E8)

    // Sombra / scrim
    val Shadow = Color.Black
    val Scrim = Color.Black

    // Colores de módulos (para línea de tiempo, íconos, etc.)
    val PlannerColor = Color(0xFF4A6FA5)
    val FinanceColor = Color(0xFF2E7D32)
    val GymColor = Color(0xFFC62828)
    val NutritionColor = Color(0xFFEF6C00)
    val MeditationColor = Color(0xFF6A1B9A)
    val MusicColor = Color(0xFF00695C)
    val ScreenTimeColor = Color(0xFF546E7A)

    // Fondos de superficie por elevación
    val SurfaceContainerLowest = Color.White
    val SurfaceContainerLow = Color(0xFFF7F2FA)
    val SurfaceContainer = Color(0xFFF0EBF5)
    val SurfaceContainerHigh = Color(0xFFECE6F0)
    val SurfaceContainerHighest = Color(0xFFE6E0EB)
}

// Extensión para acceder a colores custom desde MaterialTheme
val MaterialTheme.customColors: StayFocusedColors
    @Composable
    get() = StayFocusedColors