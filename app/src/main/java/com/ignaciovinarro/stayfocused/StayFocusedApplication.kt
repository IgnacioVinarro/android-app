package com.ignaciovinarro.stayfocused

import android.app.Application
import com.ignaciovinarro.stayfocused.core.notifications.NotificationChannels
import com.ignaciovinarro.stayfocused.finance.data.FinanceSeeder
import com.ignaciovinarro.stayfocused.gym.data.GymSeeder
import com.ignaciovinarro.stayfocused.planner.data.ReminderSyncer
import com.ignaciovinarro.stayfocused.planner.widget.WidgetSyncer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class StayFocusedApplication : Application() {

    @Inject lateinit var reminderSyncer: ReminderSyncer
    @Inject lateinit var widgetSyncer: WidgetSyncer
    @Inject lateinit var financeSeeder: FinanceSeeder
    @Inject lateinit var gymSeeder: GymSeeder

    // Vive mientras viva el proceso de la app; SupervisorJob: si una pasada falla, no se corta la observación.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        reminderSyncer.start(appScope)
        widgetSyncer.start(appScope)
        appScope.launch { financeSeeder.seedIfNeeded() }
        appScope.launch { gymSeeder.seedIfNeeded() }
    }
}