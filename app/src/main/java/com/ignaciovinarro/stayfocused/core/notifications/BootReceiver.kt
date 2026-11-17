package com.ignaciovinarro.stayfocused.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Reprograma las alarmas tras reinicio, cambio de hora/zona horaria o actualización de la app.
// Solo encola el trabajo: la reprogramación en sí la hace RescheduleWorker (fuera del receptor,
// que tiene muy poco tiempo para terminar).
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RescheduleWorker.enqueue(context)
    }
}
