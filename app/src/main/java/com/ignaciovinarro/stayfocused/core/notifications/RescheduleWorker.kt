package com.ignaciovinarro.stayfocused.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ignaciovinarro.stayfocused.planner.data.ReminderScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

// Reprograma todos los recordatorios. Se encola tras reinicio, cambio de hora/zona y actualización
// de la app (BootReceiver), y al abrir la app.
//
// Se usa un EntryPoint de Hilt en vez de @HiltWorker para no sumar la librería androidx.hilt:hilt-work
// (CLAUDE.md: antes de agregar una librería, consultarla).
class RescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun reminderScheduler(): ReminderScheduler
    }

    override suspend fun doWork(): Result {
        EntryPointAccessors
            .fromApplication(applicationContext, Dependencies::class.java)
            .reminderScheduler()
            .rescheduleAll()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "reschedule_reminders"

        // REPLACE: si llegan varios eventos seguidos (p. ej. hora y zona), alcanza con una pasada.
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RescheduleWorker>().build()
            )
        }
    }
}
