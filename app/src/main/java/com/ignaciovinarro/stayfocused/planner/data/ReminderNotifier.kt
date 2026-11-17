package com.ignaciovinarro.stayfocused.planner.data

import android.Manifest
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Dibuja la notificación de un recordatorio de tarea, con las acciones "Completar" y "Posponer".
// Solo habla con el sistema de notificaciones; las decisiones (¿corresponde avisar?) las toma el receptor.
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
        startMinutes: Int
    ) {
        // En Android 13+ sin permiso, notify() no hace nada y lint lo exige explícito. Si falta, la
        // función de avisos queda deshabilitada y el resto de la app sigue funcionando (CLAUDE.md).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val time = LocalTime.of(startMinutes / 60, startMinutes % 60).format(DateTimeFormatter.ofPattern("HH:mm"))
        val notification = NotificationCompat.Builder(context, NotificationChannels.TASKS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notification_task_starts_at, time))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, reminderId))
            .addAction(
                0, context.getString(R.string.notification_action_complete),
                actionIntent(context, ACTION_REMINDER_COMPLETE, reminderId, taskId, occurrenceDate)
            )
            .addAction(
                0, context.getString(R.string.notification_action_snooze),
                actionIntent(context, ACTION_REMINDER_SNOOZE, reminderId, taskId, occurrenceDate)
            )
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(reminderId), notification)
    }

    fun cancel(context: Context, reminderId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(reminderId))
    }

    private fun openAppIntent(context: Context, reminderId: Long): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context, notificationId(reminderId), launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Mismo requestCode para ambas acciones: se distinguen por `action`, que cuenta para la identidad.
    private fun actionIntent(
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
