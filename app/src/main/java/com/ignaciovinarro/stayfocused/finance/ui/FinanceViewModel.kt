package com.ignaciovinarro.stayfocused.finance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.finance.data.AccountEntity
import com.ignaciovinarro.stayfocused.finance.data.AccountRepository
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.data.TagRepository
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionRepository
import com.ignaciovinarro.stayfocused.finance.domain.TransactionType
import com.ignaciovinarro.stayfocused.finance.domain.accountBalance
import com.ignaciovinarro.stayfocused.finance.domain.typeOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AccountWithBalance(val account: AccountEntity, val balance: Long)

// Una fila de la lista de movimientos. Los nombres vienen resueltos para que la pantalla solo
// dibuje; `type` es null si la fila tiene un tipo desconocido (no debería pasar).
data class TransactionRow(
    val transaction: TransactionEntity,
    val type: TransactionType?,
    val categoryName: String?,
    val accountName: String,
    val transferAccountName: String?,
    val tagNames: List<String> = emptyList()
)

// `isLoading` evita mostrar un instante el estado vacío ("no tenés cuentas") antes de que llegue la
// primera lectura de la base.
data class FinanceUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val totalBalance: Long = 0,
    val transactions: List<TransactionRow> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FinanceViewModel @Inject constructor(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    tagRepository: TagRepository
) : ViewModel() {

    // El saldo de cada cuenta se calcula con las transacciones (nunca se guarda). El total es la
    // suma de las cuentas: las transferencias entre cuentas se anulan solas.
    val uiState: StateFlow<FinanceUiState> = combine(
        accountRepository.observeAll(),
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        tagRepository.observeAll(),
        tagRepository.observeLinks()
    ) { accounts, transactions, categories, tags, links ->
        val items = accounts.map { AccountWithBalance(it, accountBalance(it, transactions)) }
        val accountNames = accounts.associate { it.id to it.name }
        val categoryNames = categories.associate { it.id to it.name }
        val tagNames = tags.associate { it.id to it.name }
        val tagsByTransaction = links.groupBy({ it.transactionId }, { it.tagId })
        FinanceUiState(
            accounts = items,
            totalBalance = items.sumOf { it.balance },
            transactions = transactions.map {
                TransactionRow(
                    transaction = it,
                    type = it.typeOrNull(),
                    categoryName = it.categoryId?.let(categoryNames::get),
                    accountName = accountNames[it.accountId].orEmpty(),
                    transferAccountName = it.transferAccountId?.let(accountNames::get),
                    tagNames = tagsByTransaction[it.id].orEmpty().mapNotNull(tagNames::get).sorted()
                )
            },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FinanceUiState()
    )
}
