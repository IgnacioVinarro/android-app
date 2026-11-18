package com.ignaciovinarro.stayfocused.finance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.core.ui.chart.ChartEntry
import com.ignaciovinarro.stayfocused.core.util.formatMonthShort
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.data.TransactionRepository
import com.ignaciovinarro.stayfocused.finance.domain.MonthTotals
import com.ignaciovinarro.stayfocused.finance.domain.monthTotals
import com.ignaciovinarro.stayfocused.finance.domain.monthlyExpenseSeries
import com.ignaciovinarro.stayfocused.finance.domain.percentChange
import com.ignaciovinarro.stayfocused.finance.domain.spendByCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// Cantidad de meses del gráfico de evolución (el actual y los 5 anteriores).
const val STATISTICS_MONTHS = 6

// Una cifra del resumen con su variación contra el mes anterior (`change` null = no hay base de comparación).
data class SummaryFigure(val amount: Long, val change: Int?)

// Renglón del ranking de gasto por categoría. `name` null = gastos sin categoría (la pantalla pone el texto).
data class CategorySpendRow(
    val name: String?,
    val iconKey: String?,
    val colorKey: String?,
    val amount: Long,
    val percent: Int
)

data class StatisticsUiState(
    val month: YearMonth = YearMonth.now(),
    val income: SummaryFigure = SummaryFigure(0, null),
    val expense: SummaryFigure = SummaryFigure(0, null),
    val savings: SummaryFigure = SummaryFigure(0, null),
    val categories: List<CategorySpendRow> = emptyList(),
    val monthlyExpense: List<ChartEntry> = emptyList(),
    val isLoading: Boolean = true
) {
    // Sin ningún movimiento en el mes ni en el anterior no hay nada que comparar.
    val hasMonthData: Boolean get() = income.amount != 0L || expense.amount != 0L
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    // Por ahora solo el mes actual (ticket 0035).
    private val month = YearMonth.now()

    val uiState: StateFlow<StatisticsUiState> = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll()
    ) { transactions, categories ->
        val current = monthTotals(transactions, month)
        val previous = monthTotals(transactions, month.minusMonths(1))
        val byId = categories.associateBy { it.id }
        StatisticsUiState(
            month = month,
            income = figure(current.income, previous.income),
            expense = figure(current.expense, previous.expense),
            savings = figure(current.savings, previous.savings),
            categories = spendByCategory(transactions, month).map {
                val category = it.categoryId?.let(byId::get)
                CategorySpendRow(category?.name, category?.iconKey, category?.colorKey, it.amount, it.percent)
            },
            // Se grafica en pesos (Double) solo para dibujar; los montos siguen en centavos.
            monthlyExpense = monthlyExpenseSeries(transactions, month, STATISTICS_MONTHS).map { (m, cents) ->
                ChartEntry(formatMonthShort(m), cents / 100.0)
            },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState(month = month)
    )

    private fun figure(current: Long, previous: Long) = SummaryFigure(current, percentChange(current, previous))
}
