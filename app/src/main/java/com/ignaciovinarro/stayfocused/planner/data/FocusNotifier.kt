package com.ignaciovinarro.stayfocused.planner.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.notifications.NotificationChannels
import com.ignaciovinarro.stayfocused.core.util.formatCountdown
import com.ignaciovinarro.stayfocused.planner.domain.FocusSession
import com.ignaciovinarro.stayfocused.planner.domain.FocusStatus

const val ACTION_FOCUS_END = "com.ignaciovinarro.stayfocused.action.FOCUS_END"
const val ACTION_FOCUS_PAUSE = "com.ignaciovinarro.stayfocused.action.FOCUS_PAUSE"
const val ACTION_FOCUS_RESUME = "com.ignaciovinarro.stayfocused.action.FOCUS_RESUME"
const val ACTION_FOCUS_STOP = "com.ignaciovinarro.stayfocused.action.FOCUS_STOP"

// Dibuja las dos notificaciones del enfoque (PL-13): la persistente de la sesión en curso (la del
// servicio en primer plano) y el aviso de fin. Solo habla con el sistema de notificaciones.
object FocusNotifier {

    const val ONGOING_ID = 9_001
    const val END_ID = 9_002

    // La notificación del servicio en primer plano. Corriendo, el sistema dibuja la cuenta regresiva
    // solo (cronómetro nativo): no hace falta refrescarla cada segundo. En pausa muestra lo que falta
    // fijo. `setOnlyAlertOnce` evita que cada cambio vuelva a sonar/vibrar.
    fun buildOngoing(context: Context, session: FocusSession, remainingMs: Long): Notification {
        val running = session.status == FocusStatus.RUNNING
        val builder = NotificationCompat.Builder(context, NotificationChannels.FOCUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(session.taskTitle)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(context))

        if (running) {
            builder
                .setContentText(context.getString(R.string.focus_notification_running))
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + remainingMs)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, context.getString(R.string.focus_action_pause), actionIntent(context, ACTION_FOCUS_PAUSE))
        } else {
            builder
                .setContentText(
                    context.getString(R.string.focus_notification_paused, formatCountdown(remainingMs))
                )
                .setShowWhen(false)
                .addAction(0, context.getString(R.string.focus_action_resume), actionIntent(context, ACTION_FOCUS_RESUME))
        }
        return builder
            .addAction(0, context.getString(R.string.focus_action_stop), actionIntent(context, ACTION_FOCUS_STOP))
            .build()
    }

    // Actualiza la notificación persistente (pausa/reanudación). Si el usuario la sacó o no hay permiso,
    // no hace nada.
    @SuppressLint("MissingPermission") // canNotify() comprueba POST_NOTIFICATIONS justo antes
    fun updateOngoing(context: Context, session: FocusSession, remainingMs: Long) {
        if (!canNotify(context)) return
        NotificationManagerCompat.from(context).notify(ONGOING_ID, buildOngoing(context, session, remainingMs))
    }

    @SuppressLint("MissingPermission") // canNotify() comprueba POST_NOTIFICATIONS justo antes
    fun showEnd(context: Context, title: String) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, NotificationChannels.FOCUS_END)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.focus_end_title))
            .setContentText(context.getString(R.string.focus_end_text, title))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(END_ID, notification)
    }

    fun cancelOngoing(context: Context) {
        NotificationManagerCompat.from(context).cancel(ONGOING_ID)
    }

    // En Android 13+ sin permiso, notify() no hace nada y lint lo exige explícito. Si falta, el aviso
    // queda deshabilitado y el resto de la app sigue (CLAUDE.md).
    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, context.packageManager.getLaunchIntentForPackage(context.packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Los botones mandan un broadcast al receptor, que le pide el cambio al controlador. Se distinguen
    // por `action` (cuenta para la identidad del PendingIntent).
    private fun actionIntent(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, FocusTimerReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
