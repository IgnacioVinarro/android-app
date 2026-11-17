package com.ignaciovinarro.stayfocused.planner.data

import android.Manifest
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
import com.ignaciovinarro.stayfocused.planner.ui.AlarmActivity
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Cuánto puede sonar una alarma sin que nadie la atienda.
const val ALARM_TIMEOUT_MS = 5 * 60 * 1000L

// Dibuja la notificación de un recordatorio de tarea: simple (con "Completar" y "Posponer") o
// alarma sonora (con pantalla completa, "Posponer" y "Descartar"). Solo habla con el sistema de
// notificaciones; las decisiones (¿corresponde avisar?) las toma el receptor.
object ReminderNotifier {

    // El id de la notificación es el del recordatorio: avisar dos veces el mismo recordatorio
    // reemplaza la anterior en vez de apilar otra.
    fun notificationId(reminderId: Long): Int = reminderId.toInt()

    fun show(
        context: Context,
        reminderId: Long,
        taskId: Long,
        occurrenceDate: Long,
        title: String,
        startMinutes: Int,
        kind: String
    ) {
        // En Android 13+ sin permiso, notify() no hace nada y lint lo exige explícito. Si falta, la
        // función de avisos queda deshabilitada y el resto de la app sigue funcionando (CLAUDE.md).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val time = LocalTime.of(startMinutes / 60, startMinutes % 60).format(DateTimeFormatter.ofPattern("HH:mm"))
        val isAlarm = kind == REMINDER_KIND_ALARM
        val channel = if (isAlarm) NotificationChannels.ALARMS else NotificationChannels.TASKS
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notification_task_starts_at, time))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (isAlarm) {
            // Alarma sonora (PL-11): pantalla completa y sonido repetido hasta que se actúe. Con el
            // teléfono desbloqueado, Android la muestra como notificación emergente en su lugar.
            val screen = alarmScreenIntent(context, reminderId, taskId, occurrenceDate, title, startMinutes)
            builder
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true) // no se desliza para sacarla: hay que usar Posponer o Descartar
                .setAutoCancel(false)
                .setTimeoutAfter(ALARM_TIMEOUT_MS) // tope: no suena para siempre si no hay nadie
                .setFullScreenIntent(screen, true)
                .setContentIntent(screen)
                .addAction(
                    0, context.getString(R.string.notification_action_snooze),
                    actionPendingIntent(context, ACTION_REMINDER_SNOOZE, reminderId, taskId, occurrenceDate)
                )
                .addAction(
                    0, context.getString(R.string.notification_action_dismiss),
                    actionPendingIntent(context, ACTION_REMINDER_DISMISS, reminderId, taskId, occurrenceDate)
                )
        } else {
            builder
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(openAppIntent(context, reminderId))
                .addAction(
                    0, context.getString(R.string.notification_action_complete),
                    actionPendingIntent(context, ACTION_REMINDER_COMPLETE, reminderId, taskId, occurrenceDate)
                )
                .addAction(
                    0, context.getString(R.string.notification_action_snooze),
                    actionPendingIntent(context, ACTION_REMINDER_SNOOZE, reminderId, taskId, occurrenceDate)
                )
        }

        val notification = builder.build()
        // FLAG_INSISTENT: el sonido se repite hasta que la notificación se cancele o venza.
        if (isAlarm) notification.flags = notification.flags or Notification.FLAG_INSISTENT
        NotificationManagerCompat.from(context).notify(notificationId(reminderId), notification)
    }

    fun cancel(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(reminderId))
    }

    private fun alarmScreenIntent(
        context: Context,
        reminderId: Long,
        taskId: Long,
        occurrenceDate: Long,
        title: String,
        startMinutes: Int
    ): PendingIntent = PendingIntent.getActivity(
        context,
        notificationId(reminderId),
        AlarmActivity.intent(context, reminderId, taskId, occurrenceDate, title, startMinutes),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openAppIntent(context: Context, reminderId: Long): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context, notificationId(reminderId), launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Pública para que la pantalla de alarma dispare exactamente lo mismo que los botones de la
    // notificación. Mismo requestCode para todas las acciones: se distinguen por `action`, que cuenta
    // para la identidad del PendingIntent.
    fun actionPendingIntent(
        context: Context,
        action: String,
        reminderId: Long,
        taskId: Long,
        occurrenceDate: Long
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_REMINDER_ID, reminderId)
            .putExtra(EXTRA_TASK_ID, taskId)
            .putExtra(EXTRA_OCCURRENCE_DATE, occurrenceDate)
        return PendingIntent.getBroadcast(
            context, notificationId(reminderId), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
