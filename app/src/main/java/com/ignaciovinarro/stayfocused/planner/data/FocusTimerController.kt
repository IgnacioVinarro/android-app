package com.ignaciovinarro.stayfocused.planner.data

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.ignaciovinarro.stayfocused.core.notifications.AlarmRequest
import com.ignaciovinarro.stayfocused.core.notifications.AlarmScheduler
import com.ignaciovinarro.stayfocused.planner.domain.FocusSession
import com.ignaciovinarro.stayfocused.planner.domain.FocusStatus
import com.ignaciovinarro.stayfocused.planner.domain.pausedAt
import com.ignaciovinarro.stayfocused.planner.domain.remainingAt
import com.ignaciovinarro.stayfocused.planner.domain.resumedAt
import com.ignaciovinarro.stayfocused.planner.domain.startFocus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

const val EXTRA_FOCUS_TASK_ID = "focusTaskId"

// Clave de la alarma de fin de enfoque en AlarmScheduler. Las de recordatorios son el id del
// recordatorio (positivo) o su opuesto (posponer): `Int.MAX_VALUE` nunca choca con ellas.
const val FOCUS_ALARM_KEY = Int.MAX_VALUE

// Los dos relojes que hacen falta. El monotónico mide el tiempo del temporizador; el de pared solo
// sirve para decirle a AlarmManager a qué hora del día avisar. Interfaz para poder simularlos.
interface FocusClock {
    fun monotonicMillis(): Long
    fun wallMillis(): Long
}

class AndroidFocusClock : FocusClock {
    override fun monotonicMillis(): Long = SystemClock.elapsedRealtime()
    override fun wallMillis(): Long = System.currentTimeMillis()
}

// Pone en marcha el servicio en primer plano. El servicio se detiene solo cuando la sesión termina.
interface FocusServiceLauncher {
    fun start()
}

class AndroidFocusServiceLauncher(private val context: Context) : FocusServiceLauncher {
    override fun start() {
        ContextCompat.startForegroundService(context, Intent(context, FocusTimerService::class.java))
    }
}

// Dueño ÚNICO de la sesión de enfoque (PL-13): la pantalla, la notificación y el receptor de la alarma
// piden cambios acá, y todos los demás solo leen `session`. Las reglas de tiempo están en funciones
// puras (`FocusTimer.kt`); esta clase las une con la alarma de fin y el servicio.
@Singleton
class FocusTimerController @Inject constructor(
    private val clock: FocusClock,
    private val alarmScheduler: AlarmScheduler,
    private val serviceLauncher: FocusServiceLauncher
) {
    private val _session = MutableStateFlow<FocusSession?>(null)
    val session: StateFlow<FocusSession?> = _session.asStateFlow()

    // Devuelve false (sin cambiar nada) si los minutos no son válidos o no se pudo programar el aviso
    // de fin (falta el permiso de alarma exacta). Arrancar con una sesión en curso la reemplaza.
    fun start(taskId: Long, taskTitle: String, minutes: Int): Boolean {
        val session = startFocus(taskId, taskTitle, minutes, clock.monotonicMillis()) ?: return false
        if (!scheduleEnd(taskId, session.totalMs)) return false
        _session.value = session
        serviceLauncher.start()
        return true
    }

    fun pause() {
        val current = _session.value ?: return
        if (current.status == FocusStatus.PAUSED) return
        // Sin alarma el aviso no puede sonar mientras está en pausa.
        alarmScheduler.cancel(FOCUS_ALARM_KEY, endRequest(current.taskId))
        _session.update { it?.pausedAt(clock.monotonicMillis()) }
    }

    // false si no hay nada que reanudar o si falta el permiso de alarma (queda en pausa).
    fun resume(): Boolean {
        val current = _session.value ?: return false
        if (current.status == FocusStatus.RUNNING) return true
        if (!scheduleEnd(current.taskId, current.remainingMs)) return false
        _session.update { it?.resumedAt(clock.monotonicMillis()) }
        return true
    }

    fun stop() {
        val current = _session.value ?: return
        alarmScheduler.cancel(FOCUS_ALARM_KEY, endRequest(current.taskId))
        _session.value = null
    }

    // La alarma de fin sonó. Devuelve true si corresponde avisar. Una sesión en pausa se ignora (la
    // alarma ya se había cancelado: es un aviso viejo). Sin sesión (el proceso murió y volvió a
    // arrancar) también corresponde avisar: la alarma se programó y nadie la canceló.
    fun onEndAlarmFired(): Boolean {
        val current = _session.value
        if (current?.status == FocusStatus.PAUSED) return false
        _session.value = null
        return true
    }

    fun remainingMs(session: FocusSession): Long = session.remainingAt(clock.monotonicMillis())

    private fun scheduleEnd(taskId: Long, delayMs: Long): Boolean =
        alarmScheduler.schedule(FOCUS_ALARM_KEY, clock.wallMillis() + delayMs, endRequest(taskId))

    private fun endRequest(taskId: Long) = AlarmRequest(
        receiver = FocusTimerReceiver::class.java,
        action = ACTION_FOCUS_END,
        extras = mapOf(EXTRA_FOCUS_TASK_ID to taskId)
    )
}

@Module
@InstallIn(SingletonComponent::class)
object FocusTimerModule {
    @Provides
    @Singleton
    fun provideFocusClock(): FocusClock = AndroidFocusClock()

    @Provides
    @Singleton
    fun provideFocusServiceLauncher(@ApplicationContext context: Context): FocusServiceLauncher =
        AndroidFocusServiceLauncher(context)
}
