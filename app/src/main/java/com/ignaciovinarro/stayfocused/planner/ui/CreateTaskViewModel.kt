package com.ignaciovinarro.stayfocused.planner.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.core.settings.SettingsRepository
import com.ignaciovinarro.stayfocused.core.ui.TaskColorKey
import com.ignaciovinarro.stayfocused.core.ui.TaskIconKey
import com.ignaciovinarro.stayfocused.planner.data.REMINDER_KIND_ALARM
import com.ignaciovinarro.stayfocused.planner.data.REMINDER_KIND_NOTICE
import com.ignaciovinarro.stayfocused.planner.data.REMINDER_OWNER_TASK
import com.ignaciovinarro.stayfocused.planner.data.ReminderRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import com.ignaciovinarro.stayfocused.planner.domain.EditScope
import com.ignaciovinarro.stayfocused.planner.domain.RecurrenceType
import com.ignaciovinarro.stayfocused.planner.domain.TaskEditFields
import com.ignaciovinarro.stayfocused.planner.domain.applyEditFromDate
import com.ignaciovinarro.stayfocused.planner.domain.applyEditToAll
import com.ignaciovinarro.stayfocused.planner.domain.isValidReminderMinutes
import com.ignaciovinarro.stayfocused.planner.domain.normalizeReminderMinutes
import com.ignaciovinarro.stayfocused.planner.domain.dayOfWeekBit
import com.ignaciovinarro.stayfocused.planner.domain.truncateRecurrenceFromDate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 0L = tarea nueva, todavía no guardada. Igual convención que RoutineEditorViewModel.
private const val ARG_TASK_ID = "taskId"
// Fecha de la ocurrencia puntual que se está editando (solo aplica a tareas recurrentes); -1 =
// no viene de un bloque puntual (tarea nueva, o tarea no recurrente).
private const val ARG_OCCURRENCE_DATE = "date"
private const val SIN_FECHA_DE_OCURRENCIA = -1L

// Qué acción quedó pendiente de elegir alcance (PL-15): se le pregunta al usuario solo cuando la
// tarea es recurrente, antes de guardar o de borrar.
enum class PendingScopeAction { SAVE, DELETE }

data class CreateTaskUiState(
    val taskId: Long? = null, // null = tarea nueva; si no es null, la pantalla está en modo edición
    val isRecurringTask: Boolean = false,
    val pendingScopeAction: PendingScopeAction? = null,
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val hasStartTime: Boolean = true,
    val startTime: LocalTime = LocalTime.now(),
    val durationMinutes: Int = 30,
    val iconKey: String = TaskIconKey.DEFAULT.key,
    val colorKey: String = TaskColorKey.BLUE.key,
    val notes: String = "",
    // Recurrencia (PL-08): solo disponible con hora asignada, por eso onHasStartTimeChange la
    // apaga si se activa "sin hora" (ver nota de alcance en el ticket 0009). En modo edición de
    // una tarea recurrente (PL-15) esta sección no se muestra: el patrón no se edita acá.
    val recurrenceEnabled: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val recurrenceInterval: Int = 1,
    val recurrenceDays: Int = 0,
    val hasRecurrenceEnd: Boolean = false,
    val recurrenceEnd: LocalDate = LocalDate.now(),
    // Recordatorios (PL-10): minutos antes de la hora de inicio, sin repetidos y ordenados. Solo
    // aplican con hora asignada; si se activa "sin hora" no se guarda ninguno (ticket 0012).
    val reminderMinutes: List<Int> = emptyList(),
    // PL-11: un interruptor por tarea; true = todos sus avisos suenan como alarma (kind = "alarm").
    val alarmSound: Boolean = false
) {
    val isValid: Boolean
        get() = title.isNotBlank() && durationMinutes > 0 && (!recurrenceEnabled || isRecurrenceValid)

    private val isRecurrenceValid: Boolean
        get() = when (recurrenceType) {
            RecurrenceType.SPECIFIC_DAYS -> recurrenceDays != 0
            RecurrenceType.EVERY_N_DAYS -> recurrenceInterval > 0
            RecurrenceType.DAILY, RecurrenceType.WEEKLY, RecurrenceType.MONTHLY -> true
        }
}

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val taskOccurrenceRepository: TaskOccurrenceRepository,
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val existingTaskId: Long = savedStateHandle.get<Long>(ARG_TASK_ID)?.takeIf { it != 0L } ?: 0L
    private val occurrenceEpochDay: Long? =
        savedStateHandle.get<Long>(ARG_OCCURRENCE_DATE)?.takeIf { it != SIN_FECHA_DE_OCURRENCIA }

    private val _uiState = MutableStateFlow(CreateTaskUiState())
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    // Tal como están en la base, para no perder `id`/`uuid`/`createdAt` al editar (mismo criterio
    // que RoutineEditorViewModel) y para tener el patrón de recurrencia original a mano al armar
    // "esta y siguientes".
    private var loadedTask: TaskEntity? = null
    private var loadedOccurrence: TaskOccurrenceEntity? = null

    // Callback de navegación (onSaved/onDeleted) a invocar cuando la acción termine; si la tarea
    // es recurrente, eso pasa después de que el usuario elige el alcance en el diálogo.
    private var pendingCompletion: (() -> Unit)? = null

    init {
        if (existingTaskId == 0L) {
            viewModelScope.launch {
                val defaultDuration = settingsRepository.defaultTaskDuration.first()
                _uiState.update { it.copy(durationMinutes = defaultDuration) }
            }
        } else {
            viewModelScope.launch {
                val task = taskRepository.getById(existingTaskId) ?: return@launch
                loadedTask = task
                val isRecurring = task.recurrenceType != null
                val effectiveDate = occurrenceEpochDay ?: task.date
                val occurrence = if (isRecurring) {
                    taskOccurrenceRepository.getByTaskAndDate(task.id, effectiveDate)
                } else {
                    null
                }
                loadedOccurrence = occurrence
                val storedReminders = reminderRepository.getByOwner(REMINDER_OWNER_TASK, task.id)
                val reminders = storedReminders.map { it.minutesBefore }
                val alarmSound = storedReminders.any { it.kind == REMINDER_KIND_ALARM }

                _uiState.update {
                    it.copy(
                        taskId = task.id,
                        isRecurringTask = isRecurring,
                        title = occurrence?.overrideTitle ?: task.title,
                        notes = (occurrence?.overrideNotes ?: task.notes) ?: "",
                        date = LocalDate.ofEpochDay(effectiveDate),
                        hasStartTime = task.startMinutes != null,
                        startTime = (occurrence?.overrideStartMinutes ?: task.startMinutes)?.toLocalTime()
                            ?: it.startTime,
                        durationMinutes = occurrence?.overrideDurationMinutes ?: task.durationMinutes,
                        iconKey = occurrence?.overrideIconKey ?: task.iconKey,
                        colorKey = occurrence?.overrideColorKey ?: task.colorKey,
                        recurrenceEnabled = isRecurring,
                        recurrenceType = task.recurrenceType?.let(RecurrenceType::valueOf) ?: it.recurrenceType,
                        recurrenceInterval = task.recurrenceInterval ?: it.recurrenceInterval,
                        recurrenceDays = task.recurrenceDays ?: it.recurrenceDays,
                        hasRecurrenceEnd = task.recurrenceEnd != null,
                        recurrenceEnd = task.recurrenceEnd?.let(LocalDate::ofEpochDay) ?: it.recurrenceEnd,
                        reminderMinutes = normalizeReminderMinutes(reminders),
                        alarmSound = alarmSound
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onDateChange(value: LocalDate) = _uiState.update { it.copy(date = value) }

    // Sin hora es incompatible con recurrencia en este paso (ver notas del ticket 0009): apagar
    // la hora apaga también la recurrencia.
    fun onHasStartTimeChange(value: Boolean) = _uiState.update {
        it.copy(hasStartTime = value, recurrenceEnabled = if (!value) false else it.recurrenceEnabled)
    }

    fun onStartTimeChange(value: LocalTime) = _uiState.update { it.copy(startTime = value) }
    fun onDurationChange(value: Int) = _uiState.update { it.copy(durationMinutes = value) }
    fun onIconChange(value: String) = _uiState.update { it.copy(iconKey = value) }
    fun onColorChange(value: String) = _uiState.update { it.copy(colorKey = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun onRecurrenceEnabledChange(value: Boolean) = _uiState.update {
        it.copy(recurrenceEnabled = value, hasStartTime = if (value) true else it.hasStartTime)
    }

    fun onRecurrenceTypeChange(value: RecurrenceType) = _uiState.update { it.copy(recurrenceType = value) }
    fun onRecurrenceIntervalChange(value: Int) = _uiState.update { it.copy(recurrenceInterval = value) }

    fun onRecurrenceDayToggle(day: DayOfWeek) = _uiState.update {
        val bit = dayOfWeekBit(day)
        val nuevaMascara = if (it.recurrenceDays and bit != 0) it.recurrenceDays and bit.inv() else it.recurrenceDays or bit
        it.copy(recurrenceDays = nuevaMascara)
    }

    fun onAlarmSoundChange(value: Boolean) = _uiState.update { it.copy(alarmSound = value) }

    // Prende o apaga un recordatorio de la lista (PL-10). Ignora valores fuera de rango.
    fun onReminderToggle(minutesBefore: Int) = _uiState.update {
        if (!isValidReminderMinutes(minutesBefore)) return@update it
        val nuevos = if (minutesBefore in it.reminderMinutes) {
            it.reminderMinutes - minutesBefore
        } else {
            it.reminderMinutes + minutesBefore
        }
        it.copy(reminderMinutes = normalizeReminderMinutes(nuevos))
    }

    fun onHasRecurrenceEndChange(value: Boolean) = _uiState.update { it.copy(hasRecurrenceEnd = value) }
    fun onRecurrenceEndChange(value: LocalDate) = _uiState.update { it.copy(recurrenceEnd = value) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            if (existingTaskId == 0L) {
                val newId = taskRepository.create(buildTaskFromState(newTaskSeed(), state))
                saveReminders(newId, state)
                onSaved()
                return@launch
            }
            val task = loadedTask ?: return@launch
            if (task.recurrenceType == null) {
                taskRepository.update(buildTaskFromState(task, state))
                saveReminders(task.id, state)
                onSaved()
                return@launch
            }
            pendingCompletion = onSaved
            _uiState.update { it.copy(pendingScopeAction = PendingScopeAction.SAVE) }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val task = loadedTask ?: return
        if (task.recurrenceType == null) {
            viewModelScope.launch {
                deleteTaskAndReminders(task.id)
                onDeleted()
            }
            return
        }
        pendingCompletion = onDeleted
        _uiState.update { it.copy(pendingScopeAction = PendingScopeAction.DELETE) }
    }

    fun onScopeChosen(scope: EditScope) {
        val action = _uiState.value.pendingScopeAction ?: return
        val completion = pendingCompletion
        pendingCompletion = null
        _uiState.update { it.copy(pendingScopeAction = null) }
        viewModelScope.launch {
            when (action) {
                PendingScopeAction.SAVE -> performScopedSave(scope)
                PendingScopeAction.DELETE -> performScopedDelete(scope)
            }
            completion?.invoke()
        }
    }

    fun onDismissScopeDialog() {
        pendingCompletion = null
        _uiState.update { it.copy(pendingScopeAction = null) }
    }

    private suspend fun performScopedSave(scope: EditScope) {
        val task = loadedTask ?: return
        val state = _uiState.value
        val edit = state.toEditFields()
        val date = effectiveDate(task)

        when (scope) {
            EditScope.ALL -> {
                taskRepository.update(applyEditToAll(task, edit))
                saveReminders(task.id, state)
            }
            // Los recordatorios son de la tarea, no de la ocurrencia: "solo esta" no los toca.
            EditScope.ONLY_THIS -> upsertOccurrenceOverride(task, date, edit)
            EditScope.THIS_AND_FOLLOWING -> {
                val (truncada, nueva) = applyEditFromDate(task, edit, date)
                taskRepository.update(truncada)
                if (nueva != null) {
                    // La original conserva sus recordatorios; la nueva se lleva los editados.
                    saveReminders(taskRepository.create(nueva), state)
                } else {
                    saveReminders(task.id, state) // primera ocurrencia: equivale a "todas"
                }
            }
        }
    }

    private suspend fun performScopedDelete(scope: EditScope) {
        val task = loadedTask ?: return
        val date = effectiveDate(task)

        when (scope) {
            EditScope.ALL -> deleteTaskAndReminders(task.id)
            EditScope.ONLY_THIS -> upsertOccurrenceSkip(task, date)
            EditScope.THIS_AND_FOLLOWING -> {
                val startDate = LocalDate.ofEpochDay(task.date)
                if (!date.isAfter(startDate)) {
                    deleteTaskAndReminders(task.id) // primera ocurrencia: equivale a borrar todas
                } else {
                    taskRepository.update(truncateRecurrenceFromDate(task, date))
                }
            }
        }
    }

    private suspend fun upsertOccurrenceOverride(task: TaskEntity, date: LocalDate, edit: TaskEditFields) {
        val epochDay = date.toEpochDay()
        val existing = loadedOccurrence ?: taskOccurrenceRepository.getByTaskAndDate(task.id, epochDay)
        val overridden = (existing ?: TaskOccurrenceEntity(taskId = task.id, date = epochDay)).copy(
            overrideTitle = edit.title,
            overrideNotes = edit.notes,
            overrideStartMinutes = edit.startMinutes,
            overrideDurationMinutes = edit.durationMinutes,
            overrideIconKey = edit.iconKey,
            overrideColorKey = edit.colorKey
        )
        if (existing == null) taskOccurrenceRepository.create(overridden) else taskOccurrenceRepository.update(overridden)
    }

    private suspend fun upsertOccurrenceSkip(task: TaskEntity, date: LocalDate) {
        val epochDay = date.toEpochDay()
        val existing = loadedOccurrence ?: taskOccurrenceRepository.getByTaskAndDate(task.id, epochDay)
        val saltada = (existing ?: TaskOccurrenceEntity(taskId = task.id, date = epochDay)).copy(skipped = true)
        if (existing == null) taskOccurrenceRepository.create(saltada) else taskOccurrenceRepository.update(saltada)
    }

    // Sin hora no hay recordatorios (PL-10): se guarda el conjunto vacío.
    private suspend fun saveReminders(taskId: Long, state: CreateTaskUiState) {
        val minutes = if (state.hasStartTime) normalizeReminderMinutes(state.reminderMinutes) else emptyList()
        val kind = if (state.alarmSound) REMINDER_KIND_ALARM else REMINDER_KIND_NOTICE
        reminderRepository.replaceForOwner(REMINDER_OWNER_TASK, taskId, minutes, kind)
    }

    // Las filas hijas no se borran solas (CLAUDE.md): al borrar la tarea se borran sus recordatorios.
    private suspend fun deleteTaskAndReminders(taskId: Long) {
        taskRepository.delete(taskId)
        reminderRepository.deleteByOwner(REMINDER_OWNER_TASK, taskId)
    }

    private fun effectiveDate(task: TaskEntity): LocalDate = LocalDate.ofEpochDay(occurrenceEpochDay ?: task.date)

    private fun newTaskSeed(): TaskEntity = TaskEntity(title = "", date = 0L, durationMinutes = 0, iconKey = "", colorKey = "")

    private fun buildTaskFromState(base: TaskEntity, state: CreateTaskUiState): TaskEntity = base.copy(
        title = state.title.trim(),
        notes = state.notes.ifBlank { null },
        date = state.date.toEpochDay(),
        startMinutes = if (state.hasStartTime) {
            state.startTime.hour * 60 + state.startTime.minute
        } else {
            null
        },
        durationMinutes = state.durationMinutes,
        iconKey = state.iconKey,
        colorKey = state.colorKey,
        recurrenceType = if (state.recurrenceEnabled) state.recurrenceType.name else null,
        recurrenceInterval = if (state.recurrenceEnabled && state.recurrenceType == RecurrenceType.EVERY_N_DAYS) {
            state.recurrenceInterval
        } else {
            null
        },
        recurrenceDays = if (state.recurrenceEnabled && state.recurrenceType == RecurrenceType.SPECIFIC_DAYS) {
            state.recurrenceDays
        } else {
            null
        },
        recurrenceEnd = if (state.recurrenceEnabled && state.hasRecurrenceEnd) {
            state.recurrenceEnd.toEpochDay()
        } else {
            null
        }
    )

    private fun CreateTaskUiState.toEditFields() = TaskEditFields(
        title = title.trim(),
        notes = notes.ifBlank { null },
        startMinutes = startTime.hour * 60 + startTime.minute,
        durationMinutes = durationMinutes,
        iconKey = iconKey,
        colorKey = colorKey
    )
}

private fun Int.toLocalTime(): LocalTime {
    val normalizado = Math.floorMod(this, 24 * 60)
    return LocalTime.of(normalizado / 60, normalizado % 60)
}
