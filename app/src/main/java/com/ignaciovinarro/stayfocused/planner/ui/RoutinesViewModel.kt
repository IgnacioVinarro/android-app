package com.ignaciovinarro.stayfocused.planner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemRepository
import com.ignaciovinarro.stayfocused.planner.data.RoutineRepository
import com.ignaciovinarro.stayfocused.planner.data.TaskRepository
import com.ignaciovinarro.stayfocused.planner.domain.applyRoutine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val routineItemRepository: RoutineItemRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    val routines: StateFlow<List<RoutineEntity>> = routineRepository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // "Aplicar con un toque" solo puede ser a hoy por ahora: la navegación por día (PL-12)
    // todavía no existe, ver nota de alcance del ticket 0010.
    fun onApplyToday(routine: RoutineEntity) {
        viewModelScope.launch {
            val items = routineItemRepository.getByRoutine(routine.id)
            val tasks = applyRoutine(routine, items, LocalDate.now())
            tasks.forEach { taskRepository.create(it) }
        }
    }

    fun onDelete(routine: RoutineEntity) {
        viewModelScope.launch {
            routineRepository.delete(routine.id)
        }
    }
}
