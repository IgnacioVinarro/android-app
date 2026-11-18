package com.ignaciovinarro.stayfocused.planner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.timeline.TimelineRepository
import com.ignaciovinarro.stayfocused.core.ui.colorForKey
import com.ignaciovinarro.stayfocused.core.ui.navigation.AppLaunchRequest
import com.ignaciovinarro.stayfocused.planner.domain.WidgetSnapshot
import com.ignaciovinarro.stayfocused.planner.domain.buildWidgetSnapshot
import com.ignaciovinarro.stayfocused.planner.ui.minuteOfDay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

// Arma y publica el widget de próximas tareas (PL-16). Lee la línea de tiempo de hoy, deja que la regla
// pura (`buildWidgetSnapshot`) decida qué mostrar y lo traduce a un RemoteViews. Es lo único que dibuja
// el widget: el proveedor, el trabajador periódico y el sincronizador solo le piden que actualice.
@Singleton
class UpcomingTasksWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timelineRepository: TimelineRepository
) {

    // Sin ningún widget puesto no hace nada: el sincronizador puede llamarlo en cada cambio sin costo.
    suspend fun updateAll() {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, UpcomingTasksWidgetProvider::class.java))
        if (ids.isEmpty()) return

        val blocks = timelineRepository.blocksFor(LocalDate.now()).first()
        val snapshot = buildWidgetSnapshot(blocks, nowMinutes = minuteOfDay(LocalTime.now()))
        manager.updateAppWidget(ids, buildViews(snapshot))
    }

    private fun buildViews(snapshot: WidgetSnapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_upcoming_tasks)

        // Tocar el widget (fuera de los botones y filas) abre la app; "+" abre la nueva tarea.
        val openApp = pendingIntent(REQUEST_OPEN_APP, request = null)
        val newTask = pendingIntent(REQUEST_NEW_TASK, request = AppLaunchRequest.NEW_TASK)
        views.setOnClickPendingIntent(R.id.widget_root, openApp)
        views.setOnClickPendingIntent(R.id.widget_add, newTask)

        if (snapshot.total > 0) {
            views.setTextViewText(R.id.widget_progress, context.getString(R.string.widget_progress, snapshot.done, snapshot.total))
            views.setViewVisibility(R.id.widget_progress, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_progress, View.GONE)
        }

        // Las filas se agregan una por una: el límite de filas vive en la regla, no en el XML.
        views.removeAllViews(R.id.widget_rows)
        snapshot.tasks.forEach { task ->
            val row = RemoteViews(context.packageName, R.layout.widget_task_row)
            row.setTextViewText(R.id.widget_row_time, formatTime(task.startMinutes))
            row.setTextViewText(R.id.widget_row_title, task.title)
            row.setInt(R.id.widget_row_bar, "setBackgroundColor", colorForKey(task.colorKey).toArgb())
            row.setOnClickPendingIntent(R.id.widget_row_root, openApp)
            views.addView(R.id.widget_rows, row)
        }

        val emptyMessage = when {
            snapshot.tasks.isNotEmpty() -> null
            snapshot.total == 0 -> R.string.widget_empty_no_tasks
            else -> R.string.widget_empty_all_done
        }
        if (emptyMessage != null) {
            views.setTextViewText(R.id.widget_empty, context.getString(emptyMessage))
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_empty, View.GONE)
        }
        return views
    }

    // `request` nulo = abrir la app sin pedido. Cada acción tiene su requestCode: así sus
    // PendingIntent no se pisan entre sí.
    private fun pendingIntent(requestCode: Int, request: AppLaunchRequest?): PendingIntent {
        val intent = if (request != null) {
            AppLaunchRequest.intent(context, request)
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        }
        return PendingIntent.getActivity(
            context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatTime(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

    private companion object {
        const val REQUEST_OPEN_APP = 1
        const val REQUEST_NEW_TASK = 2
    }
}
