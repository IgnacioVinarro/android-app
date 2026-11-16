package com.ignaciovinarro.stayfocused.planner.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.core.ui.TaskColorKey
import com.ignaciovinarro.stayfocused.core.ui.TaskIconKey
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemRepository
import com.ignaciovinarro.stayfocused.planner.data.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 0L = rutina nueva, todavía no guardada.
private const val ARG_ROUTINE_ID = "routineId"

data class RoutineItemDraft(
    val id: Long? = null, // null = ítem nuevo, todavía no guardado
    val title: String = "",
    val time: LocalTime = LocalTime.of(7, 0),
    val durationMinutes: Int = 30,
    val iconKey: String = TaskIconKey.DEFAULT.key,
    val colorKey: String = TaskColorKey.BLUE.key
)

data class RoutineEditorUiState(
    val routineId: Long? = null,
    val name: String = "",
    val startTime: LocalTime = LocalTime.of(7, 0),
    val items: List<RoutineItemDraft> = emptyList()
) {
    val isValid: Boolean
        get() = name.isNotBlank() && items.isNotEmpty() && items.all { it.title.isNotBlank() && it.durationMinutes > 0 }
}

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    private val routineItemRepository: RoutineItemRepository
) : ViewModel() {

    private val existingRoutineId: Long = savedStateHandle.get<Long>(ARG_ROUTINE_ID)?.takeIf { it != 0L } ?: 0L

    private val _uiState = MutableStateFlow(RoutineEditorUiState())
    val uiState: StateFlow<RoutineEditorUiState> = _uiState.asStateFlow()

    // Ítems que existían en la base y el usuario quitó en el editor: se borran (lógicamente) recién
    // al guardar, no al tocar "quitar" en la pantalla.
    private var removedItemIds: List<Long> = emptyList()

    // Entidades tal como están en la base, para no perder su `uuid`/`createdAt`/`status` al
    // guardar: al editar, `copy()` sobre esto en vez de construir una entidad nueva de cero.
    private var loadedRoutine: RoutineEntity? = null
    private var loadedItemsById: Map<Long, RoutineItemEntity> = emptyMap()

    init {
        if (existingRoutineId != 0L) {
            viewModelScope.launch {
                val routine = routineRepository.getById(existingRoutineId) ?: return@launch
                val items = routineItemRepository.getByRoutine(existingRoutineId)
                loadedRoutine = routine
                loadedItemsById = items.associateBy { it.id }
                _uiState.update {
                    it.copy(
                        routineId = routine.id,
                        name = routine.name,
                        startTime = routine.startMinutes.toLocalTime(),
                        items = items.map { item ->
                            RoutineItemDraft(
                                id = item.id,
                                title = item.title,
                                time = (routine.startMinutes + item.offsetMinutes).toLocalTime(),
                                durationMinutes = item.durationMinutes,
                                iconKey = item.iconKey,
                                colorKey = item.colorKey
                            )
                        }
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onStartTimeChange(value: LocalTime) = _uiState.update { it.copy(startTime = value) }

    fun onAddItem(draft: RoutineItemDraft) = _uiState.update { it.copy(items = it.items + draft) }

    fun onUpdateItem(index: Int, draft: RoutineItemDraft) = _uiState.update { state ->
        state.copy(items = state.items.toMutableList().also { it[index] = draft })
    }

    fun onRemoveItem(index: Int) = _uiState.update { state ->
        val removed = state.items[index]
        removed.id?.let { removedItemIds = removedItemIds + it }
        state.copy(items = state.items.toMutableList().also { it.removeAt(index) })
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            val startMinutes = minuteOfDay(state.startTime)
            val routineId = if (state.routineId == null) {
                routineRepository.create(RoutineEntity(name = state.name.trim(), startMinutes = startMinutes))
            } else {
                val original = loadedRoutine ?: RoutineEntity(id = state.routineId, name = state.name.trim(), startMinutes = startMinutes)
                routineRepository.update(original.copy(name = state.name.trim(), startMinutes = startMinutes))
                state.routineId
            }

            state.items.forEachIndexed { position, item ->
                val offsetMinutes = minuteOfDay(item.time) - startMinutes
                if (item.id == null) {
                    routineItemRepository.create(
                        RoutineItemEntity(
                            routineId = routineId,
                            title = item.title.trim(),
                            offsetMinutes = offsetMinutes,
                            durationMinutes = item.durationMinutes,
                            iconKey = item.iconKey,
                            colorKey = item.colorKey,
                            position = position
                        )
                    )
                } else {
                    val original = loadedItemsById[item.id] ?: RoutineItemEntity(
                        id = item.id,
                        routineId = routineId,
                        title = item.title.trim(),
                        offsetMinutes = offsetMinutes,
                        durationMinutes = item.durationMinutes,
                        iconKey = item.iconKey,
                        colorKey = item.colorKey,
                        position = position
                    )
                    routineItemRepository.update(
                        original.copy(
                            routineId = routineId,
                            title = item.title.trim(),
                            offsetMinutes = offsetMinutes,
                            durationMinutes = item.durationMinutes,
                            iconKey = item.iconKey,
                            colorKey = item.colorKey,
                            position = position
                        )
                    )
                }
            }

            removedItemIds.forEach { routineItemRepository.delete(it) }
            onSaved()
        }
    }
}

private fun Int.toLocalTime(): LocalTime {
    val normalizado = Math.floorMod(this, 24 * 60)
    return LocalTime.of(normalizado / 60, normalizado % 60)
}
