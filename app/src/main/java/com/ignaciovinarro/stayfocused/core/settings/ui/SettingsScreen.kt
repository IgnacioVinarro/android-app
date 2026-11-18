package com.ignaciovinarro.stayfocused.core.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.ignaciovinarro.stayfocused.R
import java.time.LocalDate

// Por ahora Ajustes solo lleva el respaldo de datos (ticket 0026). Qué otros ajustes entran (tema, moneda,
// unidades, etc., TR-08) es un paso aparte; lo que ya guarda SettingsRepository funciona sin pantalla.
// Es un destino secundario (se abre desde "Hoy", ticket 0025): lleva flecha para volver, como Rutinas.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    // rememberSaveable: si se gira la pantalla con el diálogo abierto, no se pierde ni se cierra solo.
    var showImportConfirm by rememberSaveable { mutableStateOf(false) }

    // Selectores de archivos del sistema: no necesitan permisos de almacenamiento.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.onExportTo(uri) }
    // "*/*": algunos proveedores de archivos no reconocen .json; si el archivo no sirve, la validación lo rechaza.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.onImportFrom(uri) }

    val message = state.message?.let { stringResource(it.stringRes()) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.backup_section_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.backup_section_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { exportLauncher.launch(defaultBackupFileName(LocalDate.now())) },
                enabled = !state.working,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.backup_export_button)) }
            OutlinedButton(
                onClick = { showImportConfirm = true },
                enabled = !state.working,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.backup_import_button)) }
            if (state.working) {
                // Texto además de la barra: el estado no depende solo de una señal visual.
                Text(stringResource(R.string.backup_working), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text(stringResource(R.string.backup_import_confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

// Nombre sugerido en el selector de archivos, con la fecha para distinguir respaldos entre sí.
internal fun defaultBackupFileName(date: LocalDate): String = "stayfocused-respaldo-$date.json"

private fun BackupMessage.stringRes(): Int = when (this) {
    BackupMessage.ExportDone -> R.string.backup_export_done
    BackupMessage.ExportFailed -> R.string.backup_export_failed
    BackupMessage.ImportDone -> R.string.backup_import_done
    BackupMessage.ImportDoneAlarmsPending -> R.string.backup_import_done_alarms_pending
    BackupMessage.ImportInvalidFile -> R.string.backup_import_invalid_file
    BackupMessage.ImportFailed -> R.string.backup_import_failed
}
