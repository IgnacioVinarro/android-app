package com.ignaciovinarro.stayfocused.core.notifications

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

// Qué le falta a un recordatorio para poder sonar. Sin ninguno de los dos, el aviso no llega.
enum class ReminderPermissionIssue { NOTIFICATIONS, EXACT_ALARM }

// Función pura: cuál es el primer problema a resolver, o null si todo está en orden. Primero las
// notificaciones, porque sin ellas aunque suene la alarma no se ve nada.
fun missingReminderPermission(
    notificationsEnabled: Boolean,
    exactAlarmAllowed: Boolean
): ReminderPermissionIssue? = when {
    !notificationsEnabled -> ReminderPermissionIssue.NOTIFICATIONS
    !exactAlarmAllowed -> ReminderPermissionIssue.EXACT_ALARM
    else -> null
}

// Consulta el estado real al sistema y arma los accesos a Ajustes. Quien muestra la explicación
// (interfaz) decide cuándo pedir; esto no pide nada solo.
object ReminderPermissions {

    fun currentIssue(context: Context): ReminderPermissionIssue? {
        // areNotificationsEnabled cubre ambos casos: el permiso de Android 13+ y las notificaciones
        // apagadas a mano desde Ajustes en versiones anteriores.
        val notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exactAlarm = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        return missingReminderPermission(notifications, exactAlarm)
    }

    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    // Solo existe desde Android 12 (API 31); antes la alarma exacta no necesitaba permiso.
    fun exactAlarmSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
}
