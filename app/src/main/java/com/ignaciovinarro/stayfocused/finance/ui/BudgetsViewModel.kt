package com.ignaciovinarro.stayfocused.finance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.core.util.filterMoneyInput
import com.ignaciovinarro.stayfocused.core.util.formatArsForInput
import com.ignaciovinarro.stayfocused.core.util.parseArsToCents
import com.ignaciovinarro.stayfocused.finance.data.BudgetRepository
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.data.TransactionRepository
import com.ignaciovinarro.stayfocused.finance.domain.BudgetProgress
import com.ignaciovinarro.stayfocused.finance.domain.CategoryType
import com.ignaciovinarro.stayfocused.finance.domain.budgetLimitFor
import com.ignaciovinarro.stayfocused.finance.domain.budgetProgress
import com.ignaciovinarro.stayfocused.finance.domain.monthlySpent
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Un renglón de la pantalla: el total del mes (`categoryId` null) o una categoría de gasto.
// `limit` null = todavía sin presupuesto (entonces `progress` también es null).
data class BudgetRow(
    val categoryId: Long?,
    val name: String?, // null en el renglón "Total": la pantalla pone el texto traducido
    val iconKey: String?,
    val colorKey: String?,
    val limit: Long?,
    val spent: Long,
    val progress: BudgetProgress?
)

// Diálogo para fijar el límite de un renglón.
data class BudgetDialogState(
    val categoryId: Long?,
    val name: String?,
    val amountText: String
) {
    val amountCents: Long? get() = parseArsToCents(amountText)
    val isAmountInvalid: Boolean get() = amountText.isNotBlank() && (amountCents ?: 0) <= 0
    val canSave: Boolean get() = (amountCents ?: 0) > 0
}

data class BudgetsUiState(
    val month: YearMonth = YearMonth.now(),
    val rows: List<BudgetRow> = emptyList(),
    val dialog: BudgetDialogState? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository
) : ViewModel() {

    // Por ahora solo el mes actual (ticket 0034).
    private val month = YearMonth.now()

    private val dialog = MutableStateFlow<BudgetDialogState?>(null)

    val uiState: StateFlow<BudgetsUiState> = combine(
        budgetRepository.observeAll(),
        categoryRepository.observeAll(),
        transactionRepository.observeAll(),
        dialog
    ) { budgets, categories, transactions, dialogState ->
        fun row(categoryId: Long?, name: String?, iconKey: String?, colorKey: String?): BudgetRow {
            val limit = budgetLimitFor(budgets, categoryId, month)
            val spent = monthlySpent(transactions, categoryId, month)
            return BudgetRow(categoryId, name, iconKey, colorKey, limit, spent, limit?.let { budgetProgress(it, spent) })
        }
        BudgetsUiState(
            month = month,
            rows = listOf(row(null, null, null, null)) +
                categories.filter { it.type == CategoryType.EXPENSE.name }
                    .map { row(it.id, it.name, it.iconKey, it.colorKey) },
            dialog = dialogState,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetsUiState(month = month)
    )

    fun onRowClick(row: BudgetRow) {
        dialog.value = BudgetDialogState(
            categoryId = row.categoryId,
            name = row.name,
            amountText = row.limit?.let(::formatArsForInput).orEmpty()
        )
    }

    fun onAmountChange(value: String) {
        dialog.value = dialog.value?.copy(amountText = filterMoneyInput(value))
    }

    fun onDismissDialog() {
        dialog.value = null
    }

    fun onSaveDialog() {
        val state = dialog.value ?: return
        val cents = state.amountCents?.takeIf { it > 0 } ?: return
        viewModelScope.launch {
            budgetRepository.setLimit(state.categoryId, month, cents)
            dialog.value = null
        }
    }
}
