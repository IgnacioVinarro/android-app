package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.ExperimentalRoomApi
import com.ignaciovinarro.stayfocused.core.database.AppDatabase
import com.ignaciovinarro.stayfocused.core.util.syncOnChange
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

const val REMINDER_SYNC_DEBOUNCE_MS = 500L

// Mantiene las alarmas al día sin tocar cada pantalla: cada vez que cambia algo que afecta a un
// aviso (tarea, ocurrencia o recordatorio: crear, editar, completar, mover, borrar, aplicar una
// rutina...), reprograma. También corre una vez al arrancar, porque un Flow de Room emite al
// empezar: eso cubre "reprogramar una vez al abrir la app" (CLAUDE.md).
//
// Una sola pieza en vez de llamar a ReminderScheduler desde cada ViewModel (norma de reutilizar):
// un ViewModel nuevo que cambie tareas queda cubierto sin acordarse de nada.
//
// El debounce junta ráfagas (p. ej. al guardar una tarea se escriben varias tablas seguidas) en una
// sola pasada. rescheduleAll es idempotente, así que una pasada de más no hace daño.
class ReminderSyncer(
    private val changes: Flow<*>,
    private val reschedule: suspend () -> Unit,
    private val debounceMs: Long = REMINDER_SYNC_DEBOUNCE_MS
) {
    fun start(scope: CoroutineScope) = syncOnChange(scope, changes, debounceMs) { reschedule() }
}

@Module
@InstallIn(SingletonComponent::class)
object ReminderSyncerModule {

    // `createFlow` de Room emite al empezar y después cada vez que cambia alguna de estas tablas.
    // Es una API experimental de Room (pero es la forma directa de observar varias tablas a la vez).
    @OptIn(ExperimentalRoomApi::class, ExperimentalCoroutinesApi::class)
    @Provides
    @Singleton
    fun provideReminderSyncer(database: AppDatabase, scheduler: ReminderScheduler): ReminderSyncer =
        ReminderSyncer(
            changes = database.invalidationTracker.createFlow("task", "task_occurrence", "reminder"),
            reschedule = { scheduler.rescheduleAll() }
        )
}
