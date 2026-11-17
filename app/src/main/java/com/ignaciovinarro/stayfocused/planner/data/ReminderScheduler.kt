package com.ignaciovinarro.stayfocused.planner.data

import com.ignaciovinarro.stayfocused.core.notifications.AlarmRequest
import com.ignaciovinarro.stayfocused.core.notifications.AlarmScheduler
import com.ignaciovinarro.stayfocused.planner.domain.nextReminder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Singleton

const val EXTRA_REMINDER_ID = "reminderId"
const val EXTRA_TASK_ID = "taskId"
const val EXTRA_OCCURRENCE_DATE = "occurrenceDate"

// Une las reglas puras (`nextReminderTrigger`) con AlarmScheduler: para cada recordatorio de una
// tarea programa SOLO el próximo aviso (CLAUDE.md); al dispararse, el receptor vuelve a llamar acá
// para programar el siguiente. Es idempotente: se puede llamar las veces que haga falta.
class ReminderScheduler(
    private val taskRepository: TaskRepository,
    private val occurrenceRepository: TaskOccurrenceRepository,
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    // Devuelve false si falta el permiso de alarma exacta (no se programó nada).
    suspend fun rescheduleTask(taskId: Long): Boolean {
        val task = taskRepository.getById(taskId)
        val reminders = reminderRepository.getByOwner(REMINDER_OWNER_TASK, taskId)
        if (task == null) {
            // Tarea borrada: ya no hay recordatorios activos (se borran con ella), así que no hay
            // nada que cancelar acá; las alarmas viejas se ignoran en el receptor.
            return true
        }
        val occurrences = occurrenceRepository.getByTask(taskId)
        val now = LocalDateTime.now(clock)
        val zone = clock.zone
        var allScheduled = true
        reminders.forEach { reminder ->
            val next = nextReminder(task, reminder.minutesBefore, occurrences, now)
            if (next == null) {
                alarmScheduler.cancel(reminder.id.toInt(), fireRequest(task.id, reminder.id, 0))
            } else {
                val millis = next.triggerAt.atZone(zone).toInstant().toEpochMilli()
                val scheduled = alarmScheduler.schedule(
                    reminder.id.toInt(), millis, fireRequest(task.id, reminder.id, next.occurrenceDate.toEpochDay())
                )
                if (!scheduled) allScheduled = false
            }
        }
        return allScheduled
    }

    // Reinicio, cambio de hora/zona, actualización de la app y apertura de la app.
    suspend fun rescheduleAll(): Boolean {
        var allScheduled = true
        taskRepository.getAllTimed().forEach { if (!rescheduleTask(it.id)) allScheduled = false }
        return allScheduled
    }

    // "Posponer": un único aviso extra, con otra clave para no pisar el próximo aviso regular.
    fun snooze(taskId: Long, reminderId: Long, occurrenceDate: Long, minutes: Int): Boolean {
        val millis = clock.millis() + minutes * 60_000L
        return alarmScheduler.schedule(-reminderId.toInt(), millis, fireRequest(taskId, reminderId, occurrenceDate))
    }

    private fun fireRequest(taskId: Long, reminderId: Long, occurrenceDate: Long) = AlarmRequest(
        receiver = ReminderReceiver::class.java,
        action = ACTION_REMINDER_FIRE,
        extras = mapOf(
            EXTRA_REMINDER_ID to reminderId,
            EXTRA_TASK_ID to taskId,
            EXTRA_OCCURRENCE_DATE to occurrenceDate
        )
    )
}

@Module
@InstallIn(SingletonComponent::class)
object ReminderSchedulerModule {
    @Provides
    @Singleton
    fun provideReminderScheduler(
        taskRepository: TaskRepository,
        occurrenceRepository: TaskOccurrenceRepository,
        reminderRepository: ReminderRepository,
        alarmScheduler: AlarmScheduler
    ): ReminderScheduler =
        ReminderScheduler(taskRepository, occurrenceRepository, reminderRepository, alarmScheduler)
}
