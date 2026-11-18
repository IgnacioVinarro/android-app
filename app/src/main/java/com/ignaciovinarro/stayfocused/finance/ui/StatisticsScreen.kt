package com.ignaciovinarro.stayfocused.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.ui.chart.ColumnChart
import com.ignaciovinarro.stayfocused.core.ui.colorForKey
import com.ignaciovinarro.stayfocused.core.ui.iconForKey
import com.ignaciovinarro.stayfocused.core.util.formatArs
import com.ignaciovinarro.stayfocused.core.util.formatMonthYear

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) return@Scaffold
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(formatMonthYear(uiState.month), style = MaterialTheme.typography.titleMedium)

            SectionTitle(stringResource(R.string.statistics_summary))
            SummaryRow(stringResource(R.string.statistics_income), uiState.income, higherIsBetter = true)
            SummaryRow(stringResource(R.string.statistics_expense), uiState.expense, higherIsBetter = false)
            SummaryRow(stringResource(R.string.statistics_savings), uiState.savings, higherIsBetter = true)

            SectionTitle(stringResource(R.string.statistics_by_category))
            if (uiState.categories.isEmpty()) {
                Text(stringResource(R.string.statistics_no_expenses), style = MaterialTheme.typography.bodyMedium)
            } else {
                uiState.categories.forEach { CategorySpendCard(it) }
            }

            SectionTitle(stringResource(R.string.statistics_evolution))
            val summary = uiState.monthlyExpense.joinToString(", ") { "${it.label} ${formatArs((it.value * 100).toLong())}" }
            ColumnChart(
                entries = if (uiState.monthlyExpense.any { it.value != 0.0 }) uiState.monthlyExpense else emptyList(),
                summary = stringResource(R.string.a11y_statistics_chart, summary)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
}

// Una cifra del resumen con su variación contra el mes anterior. La flecha y el texto acompañan al color:
// la tendencia no depende solo del color. `higherIsBetter` decide si subir es bueno (ingresos, ahorro) o
// malo (gastos).
@Composable
private fun SummaryRow(label: String, figure: SummaryFigure, higherIsBetter: Boolean) {
    val amountText = formatArs(figure.amount)
    val change = figure.change
    val changeText = when {
        change == null -> stringResource(R.string.statistics_no_comparison)
        change > 0 -> stringResource(R.string.statistics_change_up, change)
        change < 0 -> stringResource(R.string.statistics_change_down, -change)
        else -> stringResource(R.string.statistics_change_same)
    }
    val description = stringResource(R.string.a11y_statistics_summary_row, label, amountText, changeText)
    val good = MaterialTheme.colorScheme.primary
    val bad = MaterialTheme.colorScheme.error
    val changeColor = when {
        change == null || change == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        (change > 0) == higherIsBetter -> good
        else -> bad
    }
    Card(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(amountText, style = MaterialTheme.typography.titleLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = when {
                        change == null || change == 0 -> Icons.Filled.Remove
                        change > 0 -> Icons.Filled.ArrowUpward
                        else -> Icons.Filled.ArrowDownward
                    },
                    contentDescription = null,
                    tint = changeColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(changeText, style = MaterialTheme.typography.bodySmall, color = changeColor)
            }
        }
    }
}

@Composable
private fun CategorySpendCard(row: CategorySpendRow) {
    val name = row.name ?: stringResource(R.string.finance_no_category)
    val amountText = formatArs(row.amount)
    val description = stringResource(R.string.a11y_statistics_category_row, name, amountText, row.percent)
    Card(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (row.iconKey != null && row.colorKey != null) {
                    Icon(
                        imageVector = iconForKey(row.iconKey),
                        contentDescription = null,
                        tint = colorForKey(row.colorKey),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("$amountText · ${row.percent} %", style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(
                progress = { row.percent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
