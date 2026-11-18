package com.ignaciovinarro.stayfocused.planner.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Punto de entrada que el launcher conoce (PL-16). No dibuja nada: delega todo en el actualizador.
// Se usa un EntryPoint de Hilt (como RescheduleWorker) en vez de @AndroidEntryPoint para no depender de
// cómo Hilt genera la clase base de un AppWidgetProvider.
class UpcomingTasksWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun updater(): UpcomingTasksWidgetUpdater
    }

    // El sistema llama acá al agregar el widget, al reiniciar y cuando el trabajador periódico lo pide.
    // Leer la base no se puede en el hilo principal: goAsync() mantiene vivo el receptor unos segundos.
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updater(context).updateAll()
            } finally {
                pending.finish()
            }
        }
    }

    // Primer widget agregado: empieza el refresco periódico. Último quitado: se corta (sin widget no hay
    // nada que refrescar).
    override fun onEnabled(context: Context) {
        WidgetRefreshWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        WidgetRefreshWorker.cancel(context)
    }

    private fun updater(context: Context): UpcomingTasksWidgetUpdater =
        EntryPointAccessors.fromApplication(context.applicationContext, Dependencies::class.java).updater()
}
