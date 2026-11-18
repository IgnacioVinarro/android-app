package com.ignaciovinarro.stayfocused

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ignaciovinarro.stayfocused.core.settings.AppTheme
import com.ignaciovinarro.stayfocused.core.settings.SettingsRepository
import com.ignaciovinarro.stayfocused.core.ui.navigation.AppLaunchRequest
import com.ignaciovinarro.stayfocused.core.ui.navigation.StayFocusedApp
import com.ignaciovinarro.stayfocused.core.ui.theme.StayFocusedTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    // Pedido que llegó de afuera (hoy, el widget): abrir directo una pantalla. Se atiende una sola vez.
    private var launchRequest by mutableStateOf<AppLaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Al recrear la actividad (girar la pantalla) el Intent original sigue ahí: sin esta guarda el
        // pedido se volvería a atender y reabriría el formulario.
        if (savedInstanceState == null) launchRequest = AppLaunchRequest.fromIntent(intent)
        setContent {
            val theme by settingsRepository.theme.collectAsState(initial = AppTheme.SYSTEM)
            StayFocusedRoot(theme, launchRequest, onLaunchRequestHandled = { launchRequest = null })
        }
    }

    // launchMode="singleTop": si la app ya está abierta arriba de todo, el widget no crea otra
    // pantalla, llega por acá.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRequest = AppLaunchRequest.fromIntent(intent)
    }
}

@Composable
private fun StayFocusedRoot(
    theme: AppTheme,
    launchRequest: AppLaunchRequest?,
    onLaunchRequestHandled: () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (theme) {
        AppTheme.SYSTEM -> systemInDarkTheme
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    StayFocusedTheme(darkTheme = darkTheme) {
        StayFocusedApp(launchRequest = launchRequest, onLaunchRequestHandled = onLaunchRequestHandled)
    }
}
