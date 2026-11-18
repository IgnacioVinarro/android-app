package com.ignaciovinarro.stayfocused.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// Mecánica compartida de "mantener algo al día cuando cambian ciertas tablas" (alarmas, widget): espera
// `debounceMs` sin cambios nuevos y recién ahí ejecuta `action`. El debounce junta ráfagas (guardar una
// tarea escribe varias tablas seguidas) en una sola pasada. Corre una vez al empezar si `changes` emite
// al arrancar (los Flow de Room lo hacen).
@OptIn(FlowPreview::class)
fun syncOnChange(scope: CoroutineScope, changes: Flow<*>, debounceMs: Long, action: suspend () -> Unit) {
    changes
        .debounce(debounceMs)
        .onEach { action() }
        .launchIn(scope)
}
