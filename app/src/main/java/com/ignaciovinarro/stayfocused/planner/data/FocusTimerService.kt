package com.ignaciovinarro.stayfocused.planner.data

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.notifications.NotificationChannels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

// Servicio en primer plano del enfoque (PL-13). Su trabajo es mantener viva la app mientras el
// temporizador corre con la pantalla apagada y mostrar la notificación persistente. NO cuenta el
// tiempo (eso lo hace el reloj monotónico del controlador) ni suena al final (eso lo hace la alarma):
// solo refleja la sesión y se apaga cuando ya no hay ninguna.
@AndroidEntryPoint
class FocusTimerService : Service() {

    @Inject lateinit var controller: FocusTimerController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android obliga a llamar startForeground enseguida después de startForegroundService, aunque la
        // sesión ya no exista (carrera con "Detener"). Por eso va primero, con lo que haya.
        val session = controller.session.value
        val notification = if (session != null) {
            FocusNotifier.buildOngoing(this, session, controller.remainingMs(session))
        } else {
            NotificationCompat.Builder(this, NotificationChannels.FOCUS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .build()
        }
        startInForeground(notification)

        if (observer == null) {
            observer = scope.launch {
                controller.session.collect { current ->
                    if (current == null) {
                        FocusNotifier.cancelOngoing(this@FocusTimerService)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        FocusNotifier.updateOngoing(this@FocusTimerService, current, controller.remainingMs(current))
                    }
                }
            }
        }
        return START_NOT_STICKY // si Android lo mata, no se relanza solo: la sesión no sobrevive igual
    }

    // Desde Android 14 hay que declarar el tipo; antes, la versión de dos argumentos alcanza.
    private fun startInForeground(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(FocusNotifier.ONGOING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(FocusNotifier.ONGOING_ID, notification)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
