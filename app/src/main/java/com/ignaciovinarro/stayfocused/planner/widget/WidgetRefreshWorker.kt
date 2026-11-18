package com.ignaciovinarro.stayfocused.planner.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

const val WIDGET_REFRESH_MINUTES = 30L

// Refresca el widget cada 30 min (PL-16) para que "las próximas" avancen con la hora y cambie de día,
// aunque nadie toque nada. Entre ticks lo mantiene al día el sincronizador (mientras la app esté viva).
// EntryPoint de Hilt en vez de @HiltWorker: mismo criterio que RescheduleWorker (sin librería nueva).
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun updater(): UpcomingTasksWidgetUpdater
    }

    override suspend fun doWork(): Result {
        EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).updater().updateAll()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "refresh_upcoming_tasks_widget"

        // KEEP: si ya está programado, no se reinicia el reloj cada vez que se agrega otro widget.
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WidgetRefreshWorker>(WIDGET_REFRESH_MINUTES, TimeUnit.MINUTES).build()
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
