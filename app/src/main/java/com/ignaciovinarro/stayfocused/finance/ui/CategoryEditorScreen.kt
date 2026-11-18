package com.ignaciovinarro.stayfocused.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.core.ui.CategoryIconSelector
import com.ignaciovinarro.stayfocused.core.ui.Choice
import com.ignaciovinarro.stayfocused.core.ui.ChoiceChips
import com.ignaciovinarro.stayfocused.core.ui.TaskColorSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditing = uiState.categoryId != null

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditing) R.string.category_editor_title_edit else R.string.category_editor_title_new
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
                        IconButton(onClick = viewModel::requestDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.category_delete))
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
            // El tipo se ve pero no se cambia: lo define el + de la lista, o ya estaba guardado.
            Text(stringResource(categoryTypeLabel(uiState.type)), style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.category_field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.category_field_icon), style = MaterialTheme.typography.labelLarge)
            CategoryIconSelector(selectedKey = uiState.iconKey, onSelect = viewModel::onIconChange)

            Text(stringResource(R.string.account_field_color), style = MaterialTheme.typography.labelLarge)
            TaskColorSelector(selectedKey = uiState.colorKey, onSelect = viewModel::onColorChange)

            Button(
                onClick = { viewModel.save(onSaved = onSaved) },
                enabled = uiState.isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    uiState.deleteRequest?.let { request ->
        DeleteCategoryDialog(
            name = uiState.name,
            request = request,
            onDismiss = viewModel::onDismissDelete,
            onConfirm = { reassignToId -> viewModel.confirmDelete(reassignToId, onDeleted) }
        )
    }
}

// Tres casos: sin transacciones (confirmación simple), con transacciones (elegir destino) y con
// transacciones pero sin otra categoría del mismo tipo (no se puede borrar).
@Composable
private fun DeleteCategoryDialog(
    name: String,
    request: CategoryDeleteRequest,
    onDismiss: () -> Unit,
    onConfirm: (reassignToId: Long?) -> Unit
) {
    var targetId by remember { mutableStateOf<Long?>(null) }

    if (request.isBlocked) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.category_delete_blocked_title)) },
            text = { Text(stringResource(R.string.category_delete_blocked_message)) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_delete_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (request.needsReassign) {
                    Text(stringResource(R.string.category_delete_reassign_message, name, request.transactionCount))
                    ChoiceChips(
                        choices = request.candidates.map { Choice(it.id, it.name) },
                        selectedId = targetId,
                        onSelect = { targetId = it }
                    )
                } else {
                    Text(stringResource(R.string.category_delete_confirm_message, name))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(targetId) },
                enabled = !request.needsReassign || targetId != null
            ) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
