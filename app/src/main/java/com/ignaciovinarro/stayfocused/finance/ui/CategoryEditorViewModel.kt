package com.ignaciovinarro.stayfocused.finance.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.core.ui.CategoryIconKey
import com.ignaciovinarro.stayfocused.core.ui.TaskColorKey
import com.ignaciovinarro.stayfocused.finance.data.CategoryEntity
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.domain.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 0L = categoría nueva. `type` (nombre del CategoryType) solo se usa en las nuevas.
private const val ARG_CATEGORY_ID = "categoryId"
private const val ARG_TYPE = "type"

// Lo que la pantalla necesita para pedir confirmación al borrar. Con `transactionCount` > 0 hay que
// elegir una categoría del mismo tipo a la que pasar esas transacciones; si no hay ninguna, no se puede.
data class CategoryDeleteRequest(
    val transactionCount: Int,
    val candidates: List<CategoryEntity>
) {
    val needsReassign: Boolean get() = transactionCount > 0
    val isBlocked: Boolean get() = needsReassign && candidates.isEmpty()
}

data class CategoryEditorUiState(
    val categoryId: Long? = null, // null = categoría nueva
    val name: String = "",
    val type: CategoryType = CategoryType.EXPENSE,
    val iconKey: String = CategoryIconKey.OTHER.key,
    val colorKey: String = TaskColorKey.GRAY.key,
    val deleteRequest: CategoryDeleteRequest? = null
) {
    val isValid: Boolean get() = name.isNotBlank()
}

@HiltViewModel
class CategoryEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val existingCategoryId: Long = savedStateHandle.get<Long>(ARG_CATEGORY_ID) ?: 0L

    private val _uiState = MutableStateFlow(
        CategoryEditorUiState(
            type = savedStateHandle.get<String>(ARG_TYPE)
                ?.let { name -> CategoryType.entries.firstOrNull { it.name == name } }
                ?: CategoryType.EXPENSE
        )
    )
    val uiState: StateFlow<CategoryEditorUiState> = _uiState.asStateFlow()

    // Entidad tal como está en la base, para no perder su `uuid`/`createdAt` al guardar.
    private var loadedCategory: CategoryEntity? = null

    init {
        if (existingCategoryId != 0L) {
            viewModelScope.launch {
                val category = categoryRepository.getById(existingCategoryId) ?: return@launch
                loadedCategory = category
                _uiState.update {
                    it.copy(
                        categoryId = category.id,
                        name = category.name,
                        type = CategoryType.entries.firstOrNull { t -> t.name == category.type } ?: it.type,
                        iconKey = category.iconKey,
                        colorKey = category.colorKey
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onIconChange(value: String) = _uiState.update { it.copy(iconKey = value) }
    fun onColorChange(value: String) = _uiState.update { it.copy(colorKey = value) }
    fun onDismissDelete() = _uiState.update { it.copy(deleteRequest = null) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return
        viewModelScope.launch {
            val original = loadedCategory
            if (original == null) {
                categoryRepository.create(
                    CategoryEntity(
                        name = state.name.trim(),
                        type = state.type.name,
                        iconKey = state.iconKey,
                        colorKey = state.colorKey
                    )
                )
            } else {
                // El tipo no se cambia al editar: las transacciones ya cargadas dependen de él.
                categoryRepository.update(
                    original.copy(name = state.name.trim(), iconKey = state.iconKey, colorKey = state.colorKey)
                )
            }
            onSaved()
        }
    }

    // Paso 1 del borrado: calcula si hay que reasignar y deja el pedido en el estado para el diálogo.
    fun requestDelete() {
        val id = _uiState.value.categoryId ?: return
        val type = _uiState.value.type
        viewModelScope.launch {
            val count = categoryRepository.countTransactions(id)
            val candidates = categoryRepository.observeAll().first()
                .filter { it.type == type.name && it.id != id }
            _uiState.update { it.copy(deleteRequest = CategoryDeleteRequest(count, candidates)) }
        }
    }

    // Paso 2: borra. `reassignToId` es obligatorio si el pedido necesitaba reasignar.
    fun confirmDelete(reassignToId: Long?, onDeleted: () -> Unit) {
        val id = _uiState.value.categoryId ?: return
        val request = _uiState.value.deleteRequest ?: return
        if (request.needsReassign && reassignToId == null) return
        viewModelScope.launch {
            categoryRepository.delete(id, reassignToId.takeIf { request.needsReassign })
            _uiState.update { it.copy(deleteRequest = null) }
            onDeleted()
        }
    }
}
