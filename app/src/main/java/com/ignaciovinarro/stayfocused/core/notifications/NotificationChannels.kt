package com.ignaciovinarro.stayfocused.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.ignaciovinarro.stayfocused.R

// Un canal por módulo (CLAUDE.md). Por ahora solo existe el de tareas; los demás se agregan cuando
// entra su módulo.
object NotificationChannels {
    const val TASKS = "tasks"

    // Crear un canal que ya existe no hace nada, así que es seguro llamarlo en cada arranque.
    // minSdk 26 = los canales existen siempre, no hace falta chequear la versión.
    fun createAll(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                TASKS,
                context.getString(R.string.notification_channel_tasks),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = context.getString(R.string.notification_channel_tasks_description) }
        )
    }
}
