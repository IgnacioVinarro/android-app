package com.ignaciovinarro.stayfocused.planner.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

// Recibe la alarma de fin del enfoque y los botones de la notificación persistente (Pausar,
// Reanudar y Detener). Todo cambio de la sesión se le pide al controlador.
@AndroidEntryPoint
class FocusTimerReceiver : BroadcastReceiver() {

    @Inject lateinit var controller: FocusTimerController
    @Inject lateinit var taskRepository: TaskRepository

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FOCUS_PAUSE -> controller.pause()
            ACTION_FOCUS_RESUME -> controller.resume()
            ACTION_FOCUS_STOP -> controller.stop()
            ACTION_FOCUS_END -> onEnd(context, intent)
        }
    }

    private fun onEnd(context: Context, intent: Intent) {
        // Si había sesión, el título ya lo tenemos; si el proceso murió y volvió, hay que leerlo de la
        // tarea. Room no se puede usar en el hilo principal: goAsync() mantiene vivo el receptor.
        val fallbackTitle = controller.session.value?.taskTitle
        if (!controller.onEndAlarmFired()) return
        val taskId = intent.getLongExtra(EXTRA_FOCUS_TASK_ID, -1)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val title = fallbackTitle ?: taskRepository.getById(taskId)?.title.orEmpty()
                FocusNotifier.showEnd(context, title)
            } finally {
                pending.finish()
            }
        }
    }
}
