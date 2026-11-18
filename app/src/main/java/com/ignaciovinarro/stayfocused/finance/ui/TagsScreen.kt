package com.ignaciovinarro.stayfocused.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.finance.domain.TAG_NAME_MAX_LENGTH
import com.ignaciovinarro.stayfocused.finance.domain.TagNameError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tags_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.a11y_add_tag))
            }
        }
    ) { innerPadding ->
        if (uiState.tags.isEmpty()) {
            Text(
                stringResource(R.string.tags_empty),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.tags, key = { it.id }) { tag ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${tag.name}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.onRenameClick(tag) }) {
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.a11y_tag_rename, tag.name))
                            }
                            IconButton(onClick = { viewModel.onDeleteClick(tag) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.a11y_tag_delete, tag.name))
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.dialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissDialog,
            title = {
                Text(
                    stringResource(
                        if (dialog.editingId == null) R.string.tag_dialog_title_new else R.string.tag_dialog_title_rename
                    )
                )
            },
            text = {
                OutlinedTextField(
                    value = dialog.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.tag_field_name)) },
                    singleLine = true,
                    isError = dialog.visibleError != null,
                    supportingText = {
                        // El error va con texto, no solo con el color rojo del campo.
                        when (dialog.visibleError) {
                            TagNameError.TOO_LONG -> Text(stringResource(R.string.tag_error_too_long, TAG_NAME_MAX_LENGTH))
                            TagNameError.DUPLICATE -> Text(stringResource(R.string.tag_error_duplicate))
                            TagNameError.EMPTY -> Text(stringResource(R.string.tag_error_empty))
                            null -> Unit
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
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

    uiState.deleteTarget?.let { tag ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            title = { Text(stringResource(R.string.tag_delete_confirm_title)) },
            text = { Text(stringResource(R.string.tag_delete_confirm_message, tag.name)) },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDelete) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDelete) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
