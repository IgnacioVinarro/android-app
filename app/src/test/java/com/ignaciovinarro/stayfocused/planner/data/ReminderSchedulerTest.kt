package com.ignaciovinarro.stayfocused.planner.data

import com.ignaciovinarro.stayfocused.core.notifications.AlarmRequest
import com.ignaciovinarro.stayfocused.core.notifications.AlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReminderSchedulerTest {

    // Lunes 2026-03-02, 08:00 UTC; el reloj fijo en UTC evita depender de la zona de la máquina.
    private val lunes = LocalDate.of(2026, 3, 2)
    private val ahora = lunes.atTime(8, 0).toInstant(ZoneOffset.UTC)
    private val clock = Clock.fixed(Instant.from(ahora), ZoneOffset.UTC)

    private val taskRepository = mockk<TaskRepository>()
    private val occurrenceRepository = mockk<TaskOccurrenceRepository>()
    private val reminderRepository = mockk<ReminderRepository>()
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private lateinit var scheduler: ReminderScheduler

    private fun tarea(id: Long = 1, startMinutes: Int? = 10 * 60) = TaskEntity(
        id = id, title = "Tarea", date = lunes.toEpochDay(), startMinutes = startMinutes,
        durationMinutes = 30, iconKey = "task_default", colorKey = "blue"
    )

    private fun recordatorio(id: Long, minutesBefore: Int, taskId: Long = 1) =
        ReminderEntity(id = id, ownerId = taskId, minutesBefore = minutesBefore)

    @Before
    fun setUp() {
        every { alarmScheduler.schedule(any(), any(), any()) } returns true
        coEvery { occurrenceRepository.getByTask(any()) } returns emptyList()
        scheduler = ReminderScheduler(taskRepository, occurrenceRepository, reminderRepository, alarmScheduler, clock)
    }

    @Test
    fun `programa un aviso por recordatorio con su hora y sus datos`() = runTest {
        coEvery { taskRepository.getById(1) } returns tarea()
        coEvery { reminderRepository.getByOwner(REMINDER_OWNER_TASK, 1) } returns
            listOf(recordatorio(10, 0), recordatorio(11, 15))

        val ok = scheduler.rescheduleTask(1)

        assertTrue(ok)
        val diezAm = lunes.atTime(10, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        verify {
            alarmScheduler.schedule(
                10, diezAm,
                match<AlarmRequest> {
                    it.action == ACTION_REMINDER_FIRE &&
                        it.extras == mapOf(
                            EXTRA_REMINDER_ID to 10L, EXTRA_TASK_ID to 1L, EXTRA_OCCURRENCE_DATE to lunes.toEpochDay()
                        )
                }
            )
        }
        verify { alarmScheduler.schedule(11, diezAm - 15 * 60_000L, any()) }
    }

    @Test
    fun `cancela el aviso cuando ya no hay proximo disparo`() = runTest {
        coEvery { taskRepository.getById(1) } returns tarea(startMinutes = 7 * 60) // ya pasó
        coEvery { reminderRepository.getByOwner(REMINDER_OWNER_TASK, 1) } returns listOf(recordatorio(10, 0))

        scheduler.rescheduleTask(1)

        verify { alarmScheduler.cancel(10, any()) }
        verify(exactly = 0) { alarmScheduler.schedule(any(), any(), any()) }
    }

    @Test
    fun `si falta el permiso de alarma exacta devuelve false`() = runTest {
        coEvery { taskRepository.getById(1) } returns tarea()
        coEvery { reminderRepository.getByOwner(REMINDER_OWNER_TASK, 1) } returns listOf(recordatorio(10, 0))
        every { alarmScheduler.schedule(any(), any(), any()) } returns false

        assertFalse(scheduler.rescheduleTask(1))
    }

    @Test
    fun `tarea borrada no programa nada`() = runTest {
        coEvery { taskRepository.getById(1) } returns null
        coEvery { reminderRepository.getByOwner(REMINDER_OWNER_TASK, 1) } returns emptyList()

        assertTrue(scheduler.rescheduleTask(1))

        verify(exactly = 0) { alarmScheduler.schedule(any(), any(), any()) }
    }

    @Test
    fun `rescheduleAll reprograma cada tarea con hora`() = runTest {
        coEvery { taskRepository.getAllTimed() } returns listOf(tarea(1), tarea(2))
        coEvery { taskRepository.getById(1) } returns tarea(1)
        coEvery { taskRepository.getById(2) } returns tarea(2)
        coEvery { reminderRepository.getByOwner(REMINDER_OWNER_TASK, 1) } returns listOf(recordatorio(10, 0, 1))
        coEvery { reminderRepository.getByOwner(REMINDER_OWNER_TASK, 2) } returns listOf(recordatorio(20, 0, 2))

        assertTrue(scheduler.rescheduleAll())

        verify { alarmScheduler.schedule(10, any(), any()) }
        verify { alarmScheduler.schedule(20, any(), any()) }
        coVerify(exactly = 1) { taskRepository.getAllTimed() }
    }

    @Test
    fun `posponer programa un aviso extra con otra clave a los minutos pedidos`() {
        val ok = scheduler.snooze(taskId = 1, reminderId = 10, occurrenceDate = lunes.toEpochDay(), minutes = 10)

        assertTrue(ok)
        val esperado = ahora.toEpochMilli() + 10 * 60_000L
        // La clave negativa (-10) no pisa la del aviso regular (10).
        verify { alarmScheduler.schedule(-10, esperado, any()) }
    }
}
