package com.ignaciovinarro.stayfocused.planner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.core.settings.SettingsRepository
import com.ignaciovinarro.stayfocused.core.ui.TaskColorKey
import com.ignaciovinarro.stayfocused.core.ui.TaskIconKey
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val notes: String = ""
) {
    val isValid: Boolean get() = title.isNotBlank() && durationMinutes > 0
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
    fun onHasStartTimeChange(value: Boolean) = _uiState.update { it.copy(hasStartTime = value) }
    fun onStartTimeChange(value: LocalTime) = _uiState.update { it.copy(startTime = value) }
    fun onDurationChange(value: Int) = _uiState.update { it.copy(durationMinutes = value) }
    fun onIconChange(value: String) = _uiState.update { it.copy(iconKey = value) }
    fun onColorChange(value: String) = _uiState.update { it.copy(colorKey = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

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
                    colorKey = state.colorKey
                )
            )
            onSaved()
        }
    }
}
