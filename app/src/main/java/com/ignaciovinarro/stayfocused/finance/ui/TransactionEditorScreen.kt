package com.ignaciovinarro.stayfocused.finance.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.ui.AppDatePickerDialog
import com.ignaciovinarro.stayfocused.core.ui.Choice
import com.ignaciovinarro.stayfocused.core.ui.ChoiceChips
import com.ignaciovinarro.stayfocused.core.util.formatShortDate
import com.ignaciovinarro.stayfocused.finance.domain.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isEditing = uiState.transactionId != null
    val isTransfer = uiState.type == TransactionType.TRANSFER

    // Movimiento nuevo: el cursor arranca en el monto, para cargarlo sin tocar nada más.
    val amountFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (!isEditing) amountFocus.requestFocus() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditing) R.string.transaction_editor_title_edit else R.string.transaction_editor_title_new
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.transaction_delete))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionType.entries.forEach { type ->
                    val selected = type == uiState.type
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(stringResource(transactionTypeLabel(type))) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(R.string.transaction_field_amount)) },
                singleLine = true,
                isError = uiState.isAmountInvalid,
                supportingText = {
                    if (uiState.isAmountInvalid) Text(stringResource(R.string.transaction_amount_invalid))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocus)
            )

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("${stringResource(R.string.transaction_field_date)}: ${formatShortDate(uiState.date)}")
            }

            Text(
                stringResource(
                    if (isTransfer) R.string.transaction_field_account_from else R.string.transaction_field_account
                ),
                style = MaterialTheme.typography.labelLarge
            )
            ChoiceChips(
                choices = uiState.accounts.map { Choice(it.id, it.name) },
                selectedId = uiState.accountId,
                onSelect = viewModel::onAccountChange
            )

            if (isTransfer) {
                Text(stringResource(R.string.transaction_field_account_to), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(
                    choices = uiState.destinationAccounts.map { Choice(it.id, it.name) },
                    selectedId = uiState.transferAccountId,
                    onSelect = viewModel::onTransferAccountChange
                )
            } else {
                Text(stringResource(R.string.transaction_field_category), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(
                    choices = uiState.visibleCategories.map { Choice(it.id, it.name) },
                    selectedId = uiState.categoryId,
                    onSelect = viewModel::onCategoryChange
                )
            }

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.transaction_field_note)) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.save(onSaved = onSaved) },
                enabled = uiState.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = uiState.date,
            onConfirm = viewModel::onDateChange,
            onDismiss = { showDatePicker = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.transaction_delete_confirm_title)) },
            text = { Text(stringResource(R.string.transaction_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onDeleted = onDeleted)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
