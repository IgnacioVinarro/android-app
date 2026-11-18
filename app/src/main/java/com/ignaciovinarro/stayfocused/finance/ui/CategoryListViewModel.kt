package com.ignaciovinarro.stayfocused.finance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.finance.data.CategoryEntity
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.domain.CategoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CategoryListUiState(
    val selectedType: CategoryType = CategoryType.EXPENSE,
    val categories: List<CategoryEntity> = emptyList() // solo las del tipo elegido
)

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val selectedType = MutableStateFlow(CategoryType.EXPENSE)

    val uiState: StateFlow<CategoryListUiState> = combine(
        selectedType,
        categoryRepository.observeAll()
    ) { type, all ->
        CategoryListUiState(selectedType = type, categories = all.filter { it.type == type.name })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoryListUiState()
    )

    fun onTypeChange(type: CategoryType) {
        selectedType.value = type
    }
}
