package com.ignaciovinarro.stayfocused.core.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.notifications.ReminderPermissionIssue
import com.ignaciovinarro.stayfocused.core.notifications.ReminderPermissions

// Aviso reutilizable: si a los recordatorios les falta un permiso, lo explica y deja pedirlo; si
// está todo en orden, no dibuja nada. Se muestra en el momento en que hace falta (cuando la persona
// elige un recordatorio), no al abrir la app (CLAUDE.md). Si rechaza, el resto de la app sigue igual.
@Composable
fun ReminderPermissionPrompt(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var issue by remember { mutableStateOf(ReminderPermissions.currentIssue(context)) }
    // Android 13+ solo muestra el diálogo del sistema un par de veces; después hay que ir a Ajustes.
    var askedOnce by rememberSaveable { mutableStateOf(false) }

    // El permiso se cambia en pantallas del sistema: al volver a la app hay que volver a mirar.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) issue = ReminderPermissions.currentIssue(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askedOnce = true
        issue = ReminderPermissions.currentIssue(context)
    }

    val current = issue ?: return
    val message = when (current) {
        ReminderPermissionIssue.NOTIFICATIONS -> R.string.reminder_permission_notifications_message
        ReminderPermissionIssue.EXACT_ALARM -> R.string.reminder_permission_alarm_message
    }
    val canAskInline = current == ReminderPermissionIssue.NOTIFICATIONS &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedOnce
    val buttonLabel = when {
        canAskInline -> R.string.reminder_permission_notifications_button
        else -> R.string.reminder_permission_settings_button
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // El ícono acompaña al texto: el estado no se comunica solo con el color.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = {
                when {
                    canAskInline -> notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    current == ReminderPermissionIssue.NOTIFICATIONS ->
                        context.startActivity(ReminderPermissions.notificationSettingsIntent(context))
                    else -> context.startActivity(ReminderPermissions.exactAlarmSettingsIntent(context))
                }
            }) {
                Text(stringResource(buttonLabel))
            }
        }
    }
}
