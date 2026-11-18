package com.ignaciovinarro.stayfocused.finance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.finance.data.TagEntity
import com.ignaciovinarro.stayfocused.finance.data.TagRepository
import com.ignaciovinarro.stayfocused.finance.domain.TagNameError
import com.ignaciovinarro.stayfocused.finance.domain.validateTagName
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

// Diálogo de alta / renombrado. `editingId` null = etiqueta nueva.
data class TagDialogState(
    val editingId: Long? = null,
    val name: String = "",
    // Los demás nombres, para detectar duplicados (al renombrar se excluye la propia etiqueta).
    val otherNames: List<String> = emptyList()
) {
    val error: TagNameError? get() = validateTagName(name, otherNames)

    // Vacío no se marca como error mientras se escribe: solo deshabilita "Guardar".
    val visibleError: TagNameError? get() = error.takeIf { name.isNotEmpty() }
    val canSave: Boolean get() = error == null
}

data class TagsUiState(
    val tags: List<TagEntity> = emptyList(),
    val dialog: TagDialogState? = null,
    val deleteTarget: TagEntity? = null
)

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository: TagRepository
) : ViewModel() {

    private val dialog = MutableStateFlow<TagDialogState?>(null)
    private val deleteTarget = MutableStateFlow<TagEntity?>(null)

    val uiState: StateFlow<TagsUiState> = combine(
        tagRepository.observeAll(), dialog, deleteTarget
    ) { tags, dialogState, target ->
        TagsUiState(tags = tags, dialog = dialogState, deleteTarget = target)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TagsUiState()
    )

    fun onAddClick() {
        dialog.value = TagDialogState(otherNames = uiState.value.tags.map { it.name })
    }

    fun onRenameClick(tag: TagEntity) {
        dialog.value = TagDialogState(
            editingId = tag.id,
            name = tag.name,
            otherNames = uiState.value.tags.filter { it.id != tag.id }.map { it.name }
        )
    }

    fun onNameChange(value: String) {
        dialog.value = dialog.value?.copy(name = value)
    }

    fun onDismissDialog() {
        dialog.value = null
    }

    fun onSaveDialog() {
        val state = dialog.value ?: return
        if (!state.canSave) return
        viewModelScope.launch {
            if (state.editingId == null) tagRepository.create(state.name) else tagRepository.rename(state.editingId, state.name)
            dialog.value = null
        }
    }

    fun onDeleteClick(tag: TagEntity) {
        deleteTarget.value = tag
    }

    fun onDismissDelete() {
        deleteTarget.value = null
    }

    fun onConfirmDelete() {
        val tag = deleteTarget.value ?: return
        viewModelScope.launch {
            tagRepository.delete(tag.id)
            deleteTarget.value = null
        }
    }
}
