package com.ignaciovinarro.stayfocused.planner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.planner.data.FocusTimerController
import com.ignaciovinarro.stayfocused.planner.domain.FocusSession
import com.ignaciovinarro.stayfocused.planner.domain.FocusStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Cada cuánto se refresca el contador en pantalla mientras corre. Medio segundo alcanza para que el
// segundo no se "salte" a la vista, sin gastar de más.
private const val TICK_MS = 500L

data class FocusUiState(
    // Sesión en curso; nula = no hay enfoque, la barra no se dibuja.
    val session: FocusSession? = null,
    val remainingMs: Long = 0,
    // El último intento de iniciar falló (por ejemplo, falta el permiso de alarma exacta).
    val startFailed: Boolean = false
) {
    val isPaused: Boolean get() = session?.status == FocusStatus.PAUSED
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FocusTimerViewModel @Inject constructor(
    private val controller: FocusTimerController
) : ViewModel() {

    private val startFailed = MutableStateFlow(false)

    // Mientras corre, emite el restante cada TICK_MS; en pausa o sin sesión, una sola vez.
    private val ticking = controller.session.flatMapLatest { session ->
        when {
            session == null -> flowOf(FocusUiState())
            session.status == FocusStatus.PAUSED -> flowOf(FocusUiState(session, controller.remainingMs(session)))
            else -> flow {
                while (true) {
                    emit(FocusUiState(session, controller.remainingMs(session)))
                    delay(TICK_MS)
                }
            }
        }
    }

    val uiState: StateFlow<FocusUiState> = combine(ticking, startFailed) { state, failed ->
        state.copy(startFailed = failed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusUiState()
    )

    fun onStart(taskId: Long, taskTitle: String, minutes: Int) {
        startFailed.value = !controller.start(taskId, taskTitle, minutes)
    }

    fun onPause() = controller.pause()

    fun onResume() {
        startFailed.value = !controller.resume()
    }

    fun onStop() = controller.stop()

    fun onDismissStartError() {
        startFailed.value = false
    }
}
