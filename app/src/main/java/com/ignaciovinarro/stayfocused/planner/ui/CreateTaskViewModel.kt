package com.ignaciovinarro.stayfocused.planner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.core.settings.SettingsRepository
import com.ignaciovinarro.stayfocused.core.ui.TaskColorKey
import com.ignaciovinarro.stayfocused.core.ui.TaskIconKey
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import com.ignaciovinarro.stayfocused.planner.domain.RecurrenceType
import com.ignaciovinarro.stayfocused.planner.domain.dayOfWeekBit
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

data class CreateTaskUiState(
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val hasStartTime: Boolean = true,
    val startTime: LocalTime = LocalTime.now(),
    val durationMinutes: Int = 30,
    val iconKey: String = TaskIconKey.DEFAULT.key,
    val colorKey: String = TaskColorKey.BLUE.key,
    val notes: String = "",
    // Recurrencia (PL-08): solo disponible con hora asignada, por eso onHasStartTimeChange la
    // apaga si se activa "sin hora" (ver nota de alcance en el ticket 0009).
    val recurrenceEnabled: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val recurrenceInterval: Int = 1,
    val recurrenceDays: Int = 0,
    val hasRecurrenceEnd: Boolean = false,
    val recurrenceEnd: LocalDate = LocalDate.now()
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
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTaskUiState())
    val uiState: StateFlow<CreateTaskUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val defaultDuration = settingsRepository.defaultTaskDuration.first()
            _uiState.update { it.copy(durationMinutes = defaultDuration) }
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

    fun onHasRecurrenceEndChange(value: Boolean) = _uiState.update { it.copy(hasRecurrenceEnd = value) }
    fun onRecurrenceEndChange(value: LocalDate) = _uiState.update { it.copy(recurrenceEnd = value) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            taskRepository.create(
                TaskEntity(
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
            )
            onSaved()
        }
    }
}
