package com.ignaciovinarro.stayfocused

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ignaciovinarro.stayfocused.core.settings.AppTheme
import com.ignaciovinarro.stayfocused.core.settings.SettingsRepository
import com.ignaciovinarro.stayfocused.core.ui.navigation.StayFocusedApp
import com.ignaciovinarro.stayfocused.core.ui.theme.StayFocusedTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by settingsRepository.theme.collectAsState(initial = AppTheme.SYSTEM)
            StayFocusedRoot(theme)
        }
    }
}

@Composable
private fun StayFocusedRoot(theme: AppTheme) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (theme) {
        AppTheme.SYSTEM -> systemInDarkTheme
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    StayFocusedTheme(darkTheme = darkTheme) {
        StayFocusedApp()
    }
}
