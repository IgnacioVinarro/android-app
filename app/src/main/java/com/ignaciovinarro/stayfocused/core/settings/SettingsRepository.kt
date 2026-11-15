package com.ignaciovinarro.stayfocused.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.InstallIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Claves de preferencias
object SettingsKeys {
    val THEME = stringPreferencesKey("theme") // "system", "light", "dark"
    val CURRENCY = stringPreferencesKey("currency") // "ARS", "USD", etc.
    val WEIGHT_UNIT = stringPreferencesKey("weight_unit") // "kg", "lb"
    val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit") // "celsius", "fahrenheit"
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val DEFAULT_TASK_DURATION = intPreferencesKey("default_task_duration") // minutos
    val MEDITATION_COMPLETION_THRESHOLD = intPreferencesKey("meditation_completion_threshold") // porcentaje 0-100
}

// Tema de la app
enum class AppTheme(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark")
}

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
        SettingsRepositoryImpl(dataStore)
}

interface SettingsRepository {
    val theme: Flow<AppTheme>
    val currency: Flow<String>
    val weightUnit: Flow<String>
    val temperatureUnit: Flow<String>
    val notificationsEnabled: Flow<Boolean>
    val defaultTaskDuration: Flow<Int>
    val meditationCompletionThreshold: Flow<Int>

    suspend fun setTheme(theme: AppTheme)
    suspend fun setCurrency(currency: String)
    suspend fun setWeightUnit(unit: String)
    suspend fun setTemperatureUnit(unit: String)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setDefaultTaskDuration(minutes: Int)
    suspend fun setMeditationCompletionThreshold(percent: Int)
}

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val theme: Flow<AppTheme> = dataStore.data
        .map { prefs -> AppTheme.values().firstOrNull { it.key == prefs[SettingsKeys.THEME] } ?: AppTheme.SYSTEM }
        .distinctUntilChanged()

    override val currency: Flow<String> = dataStore.data
        .map { prefs -> prefs[SettingsKeys.CURRENCY] ?: "ARS" }
        .distinctUntilChanged()

    override val weightUnit: Flow<String> = dataStore.data
        .map { prefs -> prefs[SettingsKeys.WEIGHT_UNIT] ?: "kg" }
        .distinctUntilChanged()

    override val temperatureUnit: Flow<String> = dataStore.data
        .map { prefs -> prefs[SettingsKeys.TEMPERATURE_UNIT] ?: "celsius" }
        .distinctUntilChanged()

    override val notificationsEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true }
        .distinctUntilChanged()

    override val defaultTaskDuration: Flow<Int> = dataStore.data
        .map { prefs -> prefs[SettingsKeys.DEFAULT_TASK_DURATION] ?: 30 }
        .distinctUntilChanged()

    override val meditationCompletionThreshold: Flow<Int> = dataStore.data
        .map { prefs -> prefs[SettingsKeys.MEDITATION_COMPLETION_THRESHOLD] ?: 80 }
        .distinctUntilChanged()

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[SettingsKeys.THEME] = theme.key }
    }

    override suspend fun setCurrency(currency: String) {
        dataStore.edit { it[SettingsKeys.CURRENCY] = currency }
    }

    override suspend fun setWeightUnit(unit: String) {
        dataStore.edit { it[SettingsKeys.WEIGHT_UNIT] = unit }
    }

    override suspend fun setTemperatureUnit(unit: String) {
        dataStore.edit { it[SettingsKeys.TEMPERATURE_UNIT] = unit }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[SettingsKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    override suspend fun setDefaultTaskDuration(minutes: Int) {
        dataStore.edit { it[SettingsKeys.DEFAULT_TASK_DURATION] = minutes }
    }

    override suspend fun setMeditationCompletionThreshold(percent: Int) {
        dataStore.edit { it[SettingsKeys.MEDITATION_COMPLETION_THRESHOLD] = percent }
    }
}
