package com.ignaciovinarro.stayfocused.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

// Reprograma las alarmas tras reinicio, cambio de hora/zona horaria o actualización de la app.
// La reprogramación en sí (AlarmScheduler + RescheduleWorker) se implementa junto con el módulo
// de planificación, cuando existan tareas con recordatorio.
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // TODO: encolar RescheduleWorker cuando AlarmScheduler esté implementado (fase 1, planner)
    }
}
