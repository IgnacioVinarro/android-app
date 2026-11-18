package com.ignaciovinarro.stayfocused.planner.widget

import androidx.room.ExperimentalRoomApi
import com.ignaciovinarro.stayfocused.core.database.AppDatabase
import com.ignaciovinarro.stayfocused.core.util.syncOnChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

const val WIDGET_SYNC_DEBOUNCE_MS = 1_000L

// Actualiza el widget cuando cambia algo que muestra (tarea, ocurrencia, subtarea o sus tildes), igual
// que ReminderSyncer hace con las alarmas. Corre también al arrancar la app (el Flow de Room emite al
// empezar). Si no hay widgets puestos, el actualizador no hace nada.
@Singleton
class WidgetSyncer @Inject constructor(
    private val database: AppDatabase,
    private val updater: UpcomingTasksWidgetUpdater
) {
    // `createFlow` es una API experimental de Room, la misma que usa ReminderSyncer.
    @OptIn(ExperimentalRoomApi::class, ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) {
        syncOnChange(
            scope = scope,
            changes = database.invalidationTracker.createFlow("task", "task_occurrence", "subtask", "subtask_completion"),
            debounceMs = WIDGET_SYNC_DEBOUNCE_MS
        ) { updater.updateAll() }
    }
}
