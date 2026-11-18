package com.ignaciovinarro.stayfocused.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.ignaciovinarro.stayfocused.R

// Un canal por módulo (CLAUDE.md). Por ahora solo existe el de tareas; los demás se agregan cuando
// entra su módulo.
object NotificationChannels {
    const val TASKS = "tasks"
    const val ALARMS = "alarms"

    // Enfoque (PL-13): uno silencioso para la notificación persistente y otro con sonido para el fin.
    const val FOCUS = "focus"
    const val FOCUS_END = "focus_end"

    // Avisos de presupuesto al llegar al 80 % y al 100 % (FI-06).
    const val BUDGET = "budget"

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
        manager.createNotificationChannel(
            NotificationChannel(
                ALARMS,
                context.getString(R.string.notification_channel_alarms),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_alarms_description)
                // Tono de alarma que el usuario ya tiene configurado en Android (PL-11). USAGE_ALARM hace
                // que use el volumen de alarma y no el de notificaciones. Los atributos de sonido de un
                // canal no se pueden cambiar después de creado.
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                BUDGET,
                context.getString(R.string.notification_channel_budget),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.notification_channel_budget_description) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                FOCUS,
                context.getString(R.string.notification_channel_focus),
                NotificationManager.IMPORTANCE_LOW // sin sonido ni vibración: es una notificación fija
            ).apply { description = context.getString(R.string.notification_channel_focus_description) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                FOCUS_END,
                context.getString(R.string.notification_channel_focus_end),
                NotificationManager.IMPORTANCE_HIGH // suena y vibra con el sonido de notificación del usuario
            ).apply {
                description = context.getString(R.string.notification_channel_focus_end_description)
                enableVibration(true)
            }
        )
    }
}
