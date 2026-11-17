package com.ignaciovinarro.stayfocused.planner.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ACTION_REMINDER_FIRE = "com.ignaciovinarro.stayfocused.action.REMINDER_FIRE"
const val ACTION_REMINDER_COMPLETE = "com.ignaciovinarro.stayfocused.action.REMINDER_COMPLETE"
const val ACTION_REMINDER_SNOOZE = "com.ignaciovinarro.stayfocused.action.REMINDER_SNOOZE"

// Cuánto dura "Posponer" (confirmado con el desarrollador, ticket 0013).
const val REMINDER_SNOOZE_MINUTES = 10

// Recibe tres cosas: la alarma que suena, y los botones "Completar" y "Posponer" de la notificación.
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var occurrenceRepository: TaskOccurrenceRepository
    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val occurrenceDate = intent.getLongExtra(EXTRA_OCCURRENCE_DATE, Long.MIN_VALUE)
        if (reminderId < 0 || taskId < 0 || occurrenceDate == Long.MIN_VALUE) return
        val action = intent.action ?: return

        // Room no se puede usar en el hilo principal. goAsync() mantiene vivo el receptor mientras
        // termina el trabajo (unos segundos como máximo).
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_REMINDER_FIRE -> onFire(context, reminderId, taskId, occurrenceDate)
                    ACTION_REMINDER_COMPLETE -> onComplete(context, reminderId, taskId, occurrenceDate)
                    ACTION_REMINDER_SNOOZE -> onSnooze(context, reminderId, taskId, occurrenceDate)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun onFire(context: Context, reminderId: Long, taskId: Long, occurrenceDate: Long) {
        try {
            val task = taskRepository.getById(taskId)
            val reminder = reminderRepository.getById(reminderId)
            // El recordatorio o la tarea pueden haberse borrado/completado después de programar la alarma.
            if (task != null && reminder != null) {
                val occurrence = occurrenceRepository.getByTaskAndDate(taskId, occurrenceDate)
                val done = if (task.recurrenceType == null) {
                    task.completedAt != null
                } else {
                    occurrence?.skipped == true || occurrence?.completedAt != null
                }
                val startMinutes = occurrence?.overrideStartMinutes ?: task.startMinutes
                if (!done && startMinutes != null) {
                    ReminderNotifier.show(
                        context, reminderId, taskId, occurrenceDate,
                        title = occurrence?.overrideTitle ?: task.title,
                        startMinutes = startMinutes
                    )
                }
            }
        } finally {
            // Siempre se programa el siguiente aviso, incluso si este ya no correspondía mostrarlo.
            reminderScheduler.rescheduleTask(taskId)
        }
    }

    private suspend fun onComplete(context: Context, reminderId: Long, taskId: Long, occurrenceDate: Long) {
        ReminderNotifier.cancel(context, reminderId)
        val task = taskRepository.getById(taskId) ?: return
        val now = System.currentTimeMillis()
        if (task.recurrenceType == null) {
            taskRepository.update(task.copy(completedAt = task.completedAt ?: now))
        } else {
            val occurrence = occurrenceRepository.getByTaskAndDate(taskId, occurrenceDate)
            if (occurrence == null) {
                occurrenceRepository.create(
                    TaskOccurrenceEntity(taskId = taskId, date = occurrenceDate, completedAt = now)
                )
            } else if (occurrence.completedAt == null) {
                occurrenceRepository.update(occurrence.copy(completedAt = now))
            }
        }
        reminderScheduler.rescheduleTask(taskId)
    }

    private fun onSnooze(context: Context, reminderId: Long, taskId: Long, occurrenceDate: Long) {
        ReminderNotifier.cancel(context, reminderId)
        reminderScheduler.snooze(taskId, reminderId, occurrenceDate, REMINDER_SNOOZE_MINUTES)
    }
}
