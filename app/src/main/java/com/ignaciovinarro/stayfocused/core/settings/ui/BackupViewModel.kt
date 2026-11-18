package com.ignaciovinarro.stayfocused.core.settings.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.core.backup.BackupFileStore
import com.ignaciovinarro.stayfocused.core.backup.BackupFormatException
import com.ignaciovinarro.stayfocused.core.backup.BackupRepository
import com.ignaciovinarro.stayfocused.planner.data.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Resultado de la última operación. Es un enum y no un texto: la pantalla lo traduce a un string de
// strings.xml (los ViewModels no conocen recursos) y el color nunca es el único indicador (siempre hay texto).
enum class BackupMessage {
    ExportDone,
    ExportFailed,
    ImportDone,
    // Se importó bien, pero falta el permiso de alarma exacta y no se pudieron reprogramar los avisos.
    ImportDoneAlarmsPending,
    ImportInvalidFile,
    ImportFailed
}

data class BackupUiState(
    val working: Boolean = false,
    val message: BackupMessage? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val fileStore: BackupFileStore,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun onExportTo(uri: Uri) = launchOperation(onFailure = { BackupMessage.ExportFailed }) {
        fileStore.write(uri, backupRepository.export())
        BackupMessage.ExportDone
    }

    // El usuario ya confirmó el reemplazo en el diálogo antes de elegir el archivo.
    fun onImportFrom(uri: Uri) = launchOperation(
        onFailure = { error -> if (error is BackupFormatException) BackupMessage.ImportInvalidFile else BackupMessage.ImportFailed }
    ) {
        backupRepository.import(fileStore.read(uri))
        // Los datos cambiaron por completo: se reprograman todos los avisos (CLAUDE.md, lista de reprogramación).
        if (reminderScheduler.rescheduleAll()) BackupMessage.ImportDone else BackupMessage.ImportDoneAlarmsPending
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    // Ignora un segundo toque mientras hay una operación en curso (evita dos importaciones a la vez).
    private fun launchOperation(onFailure: (Throwable) -> BackupMessage, block: suspend () -> BackupMessage) {
        if (_state.value.working) return
        _state.update { it.copy(working = true, message = null) }
        viewModelScope.launch {
            val result = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Archivo inválido, E/S, acceso revocado por el selector o error de la base: siempre mensaje, nunca caída.
                onFailure(e)
            }
            // Sin mensaje con el detalle del error: no se escribe nada de los datos en pantalla ni en logs.
            _state.update { BackupUiState(working = false, message = result) }
        }
    }
}
