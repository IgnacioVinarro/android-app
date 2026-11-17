package com.ignaciovinarro.stayfocused.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Único punto que habla con AlarmManager (CLAUDE.md). Es una interfaz para poder simularla en pruebas.
// No sabe nada de tareas: recibe una clave numérica estable (para poder cancelar/reemplazar esa
// alarma) y un AlarmRequest que dice a qué receptor avisar y con qué datos.
interface AlarmScheduler {
    // En Android 12+ la alarma exacta necesita un permiso que el usuario puede quitar.
    fun canScheduleExact(): Boolean

    // Devuelve false (sin programar nada) si falta el permiso; quien llama guía al usuario a Ajustes.
    fun schedule(key: Int, triggerAtMillis: Long, request: AlarmRequest): Boolean

    fun cancel(key: Int, request: AlarmRequest)
}

// A quién se le avisa cuando suena la alarma. Es un dato simple (sin Intent) para que quien programa
// se pueda probar sin Android. Los extras son números: ids y fechas (epochDay).
data class AlarmRequest(
    val receiver: Class<out BroadcastReceiver>,
    val action: String,
    val extras: Map<String, Long> = emptyMap()
)

class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun schedule(key: Int, triggerAtMillis: Long, request: AlarmRequest): Boolean {
        if (!canScheduleExact()) return false
        // setExactAndAllowWhileIdle: suena a la hora aunque el teléfono esté en reposo (Doze).
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent(key, request))
        return true
    }

    override fun cancel(key: Int, request: AlarmRequest) {
        alarmManager.cancel(pendingIntent(key, request))
    }

    // El mismo requestCode + mismo Intent (sin contar extras) identifica la misma alarma, por eso
    // programar dos veces con la misma clave la reemplaza en vez de duplicarla.
    private fun pendingIntent(key: Int, request: AlarmRequest): PendingIntent {
        val intent = Intent(context, request.receiver).setAction(request.action)
        request.extras.forEach { (name, value) -> intent.putExtra(name, value) }
        return PendingIntent.getBroadcast(
            context, key, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AlarmSchedulerModule {
    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): AlarmScheduler =
        AndroidAlarmScheduler(context)
}
