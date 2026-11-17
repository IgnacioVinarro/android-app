package com.ignaciovinarro.stayfocused.planner.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.ui.theme.StayFocusedTheme
import com.ignaciovinarro.stayfocused.planner.data.ACTION_REMINDER_DISMISS
import com.ignaciovinarro.stayfocused.planner.data.ACTION_REMINDER_SNOOZE
import com.ignaciovinarro.stayfocused.planner.data.EXTRA_OCCURRENCE_DATE
import com.ignaciovinarro.stayfocused.planner.data.EXTRA_REMINDER_ID
import com.ignaciovinarro.stayfocused.planner.data.EXTRA_TASK_ID
import com.ignaciovinarro.stayfocused.planner.data.ReminderNotifier
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Pantalla completa de una alarma sonora (PL-11). No tiene lógica propia: Posponer y Descartar
// disparan los mismos PendingIntent que los botones de la notificación, así que hay una sola
// implementación (en ReminderReceiver). Se muestra sobre la pantalla de bloqueo y la enciende
// (atributos showWhenLocked/turnScreenOn en el manifiesto).
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1)
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val occurrenceDate = intent.getLongExtra(EXTRA_OCCURRENCE_DATE, Long.MIN_VALUE)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val startMinutes = intent.getIntExtra(EXTRA_START_MINUTES, 0)
        if (reminderId < 0 || taskId < 0 || occurrenceDate == Long.MIN_VALUE) {
            finish()
            return
        }

        fun act(action: String) {
            ReminderNotifier.actionPendingIntent(this, action, reminderId, taskId, occurrenceDate).send()
            finish()
        }

        setContent {
            StayFocusedTheme(darkTheme = isSystemInDarkTheme()) {
                AlarmScreen(
                    title = title,
                    startMinutes = startMinutes,
                    onSnooze = { act(ACTION_REMINDER_SNOOZE) },
                    onDismiss = { act(ACTION_REMINDER_DISMISS) }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_START_MINUTES = "startMinutes"

        fun intent(
            context: Context,
            reminderId: Long,
            taskId: Long,
            occurrenceDate: Long,
            title: String,
            startMinutes: Int
        ): Intent = Intent(context, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_REMINDER_ID, reminderId)
            .putExtra(EXTRA_TASK_ID, taskId)
            .putExtra(EXTRA_OCCURRENCE_DATE, occurrenceDate)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_START_MINUTES, startMinutes)
    }
}

@Composable
private fun AlarmScreen(title: String, startMinutes: Int, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val time = LocalTime.of(startMinutes / 60, startMinutes % 60).format(DateTimeFormatter.ofPattern("HH:mm"))
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.alarm_screen_title), style = MaterialTheme.typography.labelLarge)
            Text(time, style = MaterialTheme.typography.displayLarge)
            Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            // Botones grandes (>= 48 dp) y con texto: se usan a medio despertar, con el teléfono en la mano.
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text(stringResource(R.string.notification_action_dismiss))
            }
            OutlinedButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                Text(stringResource(R.string.notification_action_snooze))
            }
        }
    }
}
