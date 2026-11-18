package com.ignaciovinarro.stayfocused.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.ui.colorForKey
import com.ignaciovinarro.stayfocused.core.ui.iconForKey
import com.ignaciovinarro.stayfocused.core.util.formatArs
import com.ignaciovinarro.stayfocused.core.util.formatMonthYear
import com.ignaciovinarro.stayfocused.finance.domain.BudgetAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.budgets_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    formatMonthYear(uiState.month),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (!uiState.isLoading) {
                items(uiState.rows, key = { it.categoryId ?: -1L }) { row ->
                    BudgetRowCard(row = row, onClick = { viewModel.onRowClick(row) })
                }
            }
        }
    }

    uiState.dialog?.let { dialog ->
        val title = dialog.name ?: stringResource(R.string.budgets_total)
        AlertDialog(
            onDismissRequest = viewModel::onDismissDialog,
            title = { Text(stringResource(R.string.budgets_dialog_title, title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dialog.amountText,
                        onValueChange = viewModel::onAmountChange,
                        label = { Text(stringResource(R.string.budgets_field_limit)) },
                        singleLine = true,
                        isError = dialog.isAmountInvalid,
                        supportingText = {
                            if (dialog.isAmountInvalid) Text(stringResource(R.string.transaction_amount_invalid))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.budgets_dialog_hint), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::onSaveDialog, enabled = dialog.canSave) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDialog) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun BudgetRowCard(row: BudgetRow, onClick: () -> Unit) {
    val name = row.name ?: stringResource(R.string.budgets_total)
    val spentText = formatArs(row.spent)
    val alert = row.progress?.alert ?: BudgetAlert.NONE
    val alertText = when (alert) {
        BudgetAlert.NONE -> null
        BudgetAlert.WARNING_80 -> stringResource(R.string.budgets_alert_warning)
        BudgetAlert.REACHED_100 -> stringResource(R.string.budgets_alert_reached)
    }
    val summary = if (row.limit == null) {
        stringResource(R.string.budgets_no_limit, spentText)
    } else {
        stringResource(R.string.budgets_spent_of, spentText, formatArs(row.limit), row.progress?.percent ?: 0)
    }
    val description = listOfNotNull(name, summary, alertText, stringResource(R.string.a11y_budget_row_hint))
        .joinToString(". ")

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        colors = if (row.name == null) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (row.iconKey != null && row.colorKey != null) {
                    Icon(
                        imageVector = iconForKey(row.iconKey),
                        contentDescription = null,
                        tint = colorForKey(row.colorKey),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(name, style = MaterialTheme.typography.titleMedium)
            }
            Text(summary, style = MaterialTheme.typography.bodyMedium)
            row.progress?.let { progress ->
                // La barra se llena hasta el 100 %; si se pasó, el texto de alerta lo dice.
                LinearProgressIndicator(
                    progress = { (progress.percent / 100f).coerceAtMost(1f) },
                    color = if (alert == BudgetAlert.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (alertText != null) {
                // Ícono + texto: la alerta no depende solo del color.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = if (alert == BudgetAlert.REACHED_100) Icons.Filled.Error else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(alertText, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
