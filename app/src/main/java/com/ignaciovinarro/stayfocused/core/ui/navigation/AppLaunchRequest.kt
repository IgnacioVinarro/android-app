package com.ignaciovinarro.stayfocused.core.ui.navigation

import android.content.Context
import android.content.Intent
import com.ignaciovinarro.stayfocused.MainActivity

// Pedidos que le llegan a la app desde afuera (hoy, el widget de inicio, PL-16): abrir directo en una
// pantalla puntual. MainActivity los lee del Intent y StayFocusedApp los atiende una sola vez.
enum class AppLaunchRequest {
    NEW_TASK; // abrir el formulario de nueva tarea, con la fecha de hoy

    companion object {
        const val EXTRA = "com.ignaciovinarro.stayfocused.extra.LAUNCH_REQUEST"

        // Un nombre desconocido o ausente es "sin pedido": un Intent ajeno no puede romper nada.
        fun fromName(name: String?): AppLaunchRequest? = entries.firstOrNull { it.name == name }

        fun fromIntent(intent: Intent?): AppLaunchRequest? = fromName(intent?.getStringExtra(EXTRA))

        fun intent(context: Context, request: AppLaunchRequest): Intent =
            Intent(context, MainActivity::class.java)
                .setAction("com.ignaciovinarro.stayfocused.action.LAUNCH_${request.name}")
                .putExtra(EXTRA, request.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
