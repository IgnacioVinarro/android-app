package com.ignaciovinarro.stayfocused.core.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.ui.SectionTabRow
import com.ignaciovinarro.stayfocused.gym.ui.GymScreen
import com.ignaciovinarro.stayfocused.nutrition.ui.NutritionScreen

// Las pestañas del destino Gimnasio, en el orden en que se muestran. La primera es la que se ve por
// defecto al abrir.
enum class GymDietTab(@StringRes val labelRes: Int) {
    GYM(R.string.nav_gym),
    DIET(R.string.nav_nutrition);

    companion object {
        val DEFAULT = GYM
    }
}

// Destino "Gimnasio" de la barra inferior: arriba un tab bar para alternar entre Gimnasio y Dieta.
// Vive en core/ui/navigation y no dentro de un módulo porque `gym` y `nutrition` no se llaman entre sí
// (CLAUDE.md): solo esta capa, que ya conoce todas las pantallas, las junta.
@Composable
fun GymDietScreen(modifier: Modifier = Modifier) {
    // Se guarda el índice (un Int) para que sobreviva a girar la pantalla.
    var selectedIndex by rememberSaveable { mutableIntStateOf(GymDietTab.DEFAULT.ordinal) }
    val tabs = GymDietTab.entries

    Column(modifier) {
        SectionTabRow(
            titles = tabs.map { stringResource(it.labelRes) },
            selectedIndex = selectedIndex,
            onSelect = { selectedIndex = it }
        )
        when (tabs[selectedIndex]) {
            GymDietTab.GYM -> GymScreen()
            GymDietTab.DIET -> NutritionScreen()
        }
    }
}
