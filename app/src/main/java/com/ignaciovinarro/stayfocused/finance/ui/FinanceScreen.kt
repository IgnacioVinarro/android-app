package com.ignaciovinarro.stayfocused.finance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.ignaciovinarro.stayfocused.core.ui.colorForKey
import com.ignaciovinarro.stayfocused.core.util.formatArs
import com.ignaciovinarro.stayfocused.core.util.formatShortDate
import com.ignaciovinarro.stayfocused.finance.domain.TransactionType
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    onAddAccount: () -> Unit = {},
    onEditAccount: (accountId: Long) -> Unit = {},
    onAddTransaction: () -> Unit = {},
    onEditTransaction: (transactionId: Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_finance)) }) },
        floatingActionButton = {
            // Sin cuentas no se puede registrar nada: el alta de movimientos espera a la primera.
            if (uiState.accounts.isNotEmpty()) {
                FloatingActionButton(onClick = onAddTransaction) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.a11y_add_transaction))
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Unit
            uiState.accounts.isEmpty() -> EmptyAccounts(
                onCreate = onAddAccount,
                modifier = Modifier.padding(innerPadding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { TotalBalanceCard(uiState.totalBalance) }
                item {
                    SectionHeader(
                        title = stringResource(R.string.finance_accounts_title),
                        actionLabel = stringResource(R.string.a11y_add_account),
                        onAction = onAddAccount
                    )
                }
                items(uiState.accounts, key = { "account-${it.account.id}" }) { item ->
                    AccountCard(item = item, onClick = { onEditAccount(item.account.id) })
                }
                item {
                    SectionHeader(title = stringResource(R.string.finance_transactions_title))
                }
                if (uiState.transactions.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.finance_transactions_empty),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(uiState.transactions, key = { "tx-${it.transaction.id}" }) { row ->
                        TransactionCard(row = row, onClick = { onEditTransaction(row.transaction.id) })
                    }
                }
                // Aire al final para que el botón flotante no tape el último movimiento.
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (actionLabel != null) {
            IconButton(onClick = onAction) {
                Icon(Icons.Filled.Add, contentDescription = actionLabel)
            }
        }
    }
}

@Composable
private fun TotalBalanceCard(totalBalance: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.finance_total_balance), style = MaterialTheme.typography.labelLarge)
            Text(formatArs(totalBalance), style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun AccountCard(item: AccountWithBalance, onClick: () -> Unit) {
    val account = item.account
    val type = accountTypeOf(account.type)
    val typeLabel = stringResource(accountTypeLabel(type))
    val balanceText = formatArs(item.balance)
    val description = stringResource(R.string.a11y_account_card, account.name, typeLabel, balanceText)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = accountTypeIcon(type),
                contentDescription = null,
                tint = colorForKey(account.colorKey),
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium)
                Text(typeLabel, style = MaterialTheme.typography.bodySmall)
            }
            Text(balanceText, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TransactionCard(row: TransactionRow, onClick: () -> Unit) {
    val transaction = row.transaction
    val typeLabel = stringResource(transactionTypeLabel(row.type))
    val title = when (row.type) {
        TransactionType.TRANSFER -> stringResource(
            R.string.finance_transfer_row,
            row.accountName,
            row.transferAccountName.orEmpty()
        )
        else -> row.categoryName ?: stringResource(R.string.finance_no_category)
    }
    val dateText = formatShortDate(LocalDate.ofEpochDay(transaction.date))
    val subtitle = if (row.type == TransactionType.TRANSFER) dateText else "$dateText · ${row.accountName}"
    // El signo acompaña al color: ingreso "+", gasto "-", transferencia sin signo.
    val amountText = when (row.type) {
        TransactionType.INCOME -> "+" + formatArs(transaction.amount)
        TransactionType.EXPENSE -> formatArs(-transaction.amount)
        else -> formatArs(transaction.amount)
    }
    val description = stringResource(R.string.a11y_transaction_row, typeLabel, title, amountText, subtitle)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = description }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = transactionTypeIcon(row.type),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Text(amountText, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun EmptyAccounts(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.finance_empty_accounts), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onCreate) { Text(stringResource(R.string.finance_create_account)) }
    }
}

private fun transactionTypeIcon(type: TransactionType?) = when (type) {
    TransactionType.INCOME -> Icons.Filled.ArrowUpward
    TransactionType.EXPENSE -> Icons.Filled.ArrowDownward
    else -> Icons.Filled.SwapHoriz
}
