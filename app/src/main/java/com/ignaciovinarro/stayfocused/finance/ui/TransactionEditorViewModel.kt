package com.ignaciovinarro.stayfocused.finance.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.core.util.filterMoneyInput
import com.ignaciovinarro.stayfocused.core.util.formatArsForInput
import com.ignaciovinarro.stayfocused.core.util.parseArsToCents
import com.ignaciovinarro.stayfocused.finance.data.AccountEntity
import com.ignaciovinarro.stayfocused.finance.data.AccountRepository
import com.ignaciovinarro.stayfocused.finance.data.CategoryEntity
import com.ignaciovinarro.stayfocused.finance.data.CategoryRepository
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionRepository
import com.ignaciovinarro.stayfocused.finance.domain.CategoryType
import com.ignaciovinarro.stayfocused.finance.domain.TransactionType
import com.ignaciovinarro.stayfocused.finance.domain.typeOrNull
import com.ignaciovinarro.stayfocused.finance.domain.validateTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 0L = movimiento nuevo, todavía no guardado.
private const val ARG_TRANSACTION_ID = "transactionId"

data class TransactionEditorUiState(
    val transactionId: Long? = null, // null = movimiento nuevo
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val date: LocalDate = LocalDate.now(),
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val transferAccountId: Long? = null,
    val note: String = "",
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList()
) {
    val amountCents: Long? get() = parseArsToCents(amountText)

    // Categorías que se pueden elegir: las del mismo tipo que el movimiento (ninguna en una transferencia).
    val visibleCategories: List<CategoryEntity> get() = categories.filter { it.type == type.name }

    // Cuentas destino posibles de una transferencia: todas menos la de origen.
    val destinationAccounts: List<AccountEntity> get() = accounts.filter { it.id != accountId }

    val isAmountInvalid: Boolean get() = amountText.isNotBlank() && (amountCents ?: 0) <= 0

    // Válido si hay monto > 0, cuenta, y `validateTransaction` (las reglas de negocio) no marca error.
    val isValid: Boolean
        get() {
            val cents = amountCents ?: return false
            val account = accountId ?: return false
            val categoryType = categories.firstOrNull { it.id == categoryId }
                ?.let { runCatching { CategoryType.valueOf(it.type) }.getOrNull() }
            val draft = TransactionEntity(
                type = type.name, amount = cents, date = date.toEpochDay(), accountId = account,
                categoryId = categoryId, transferAccountId = transferAccountId
            )
            return validateTransaction(draft, categoryType) == null
        }
}

@HiltViewModel
class TransactionEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val existingTransactionId: Long = savedStateHandle.get<Long>(ARG_TRANSACTION_ID) ?: 0L

    private val _uiState = MutableStateFlow(TransactionEditorUiState())
    val uiState: StateFlow<TransactionEditorUiState> = _uiState.asStateFlow()

    // Entidad tal como está en la base, para no perder su `uuid`/`createdAt` al guardar.
    private var loadedTransaction: TransactionEntity? = null

    init {
        // Cuentas y categorías: se siguen observando para que el formulario siempre ofrezca las vigentes.
        viewModelScope.launch {
            combine(accountRepository.observeAll(), categoryRepository.observeAll()) { accounts, categories ->
                accounts to categories
            }.collect { (accounts, categories) ->
                _uiState.update {
                    // Movimiento nuevo sin cuenta elegida: por defecto, la primera.
                    it.copy(
                        accounts = accounts,
                        categories = categories,
                        accountId = it.accountId ?: accounts.firstOrNull()?.id
                    )
                }
            }
        }
        if (existingTransactionId != 0L) {
            viewModelScope.launch {
                val transaction = transactionRepository.getById(existingTransactionId) ?: return@launch
                loadedTransaction = transaction
                _uiState.update {
                    it.copy(
                        transactionId = transaction.id,
                        type = transaction.typeOrNull() ?: TransactionType.EXPENSE,
                        amountText = formatArsForInput(transaction.amount),
                        date = LocalDate.ofEpochDay(transaction.date),
                        accountId = transaction.accountId,
                        categoryId = transaction.categoryId,
                        transferAccountId = transaction.transferAccountId,
                        note = transaction.note.orEmpty()
                    )
                }
            }
        }
    }

    // Al cambiar el tipo se descarta lo que ya no corresponde: la categoría de otro tipo y, si deja de
    // ser transferencia, la cuenta destino.
    fun onTypeChange(value: TransactionType) = _uiState.update { state ->
        val keptCategory = state.categoryId?.takeIf { id ->
            state.categories.firstOrNull { it.id == id }?.type == value.name
        }
        state.copy(
            type = value,
            categoryId = keptCategory,
            transferAccountId = if (value == TransactionType.TRANSFER) state.transferAccountId else null
        )
    }

    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = filterMoneyInput(value)) }
    fun onDateChange(value: LocalDate) = _uiState.update { it.copy(date = value) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }
    fun onCategoryChange(value: Long) = _uiState.update { it.copy(categoryId = value) }
    fun onTransferAccountChange(value: Long) = _uiState.update { it.copy(transferAccountId = value) }

    // Si la nueva cuenta de origen era el destino de la transferencia, el destino se descarta.
    fun onAccountChange(value: Long) = _uiState.update {
        it.copy(accountId = value, transferAccountId = it.transferAccountId?.takeIf { dest -> dest != value })
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val cents = state.amountCents
        val accountId = state.accountId
        if (!state.isValid || cents == null || accountId == null) return
        viewModelScope.launch {
            val note = state.note.trim().ifEmpty { null }
            val original = loadedTransaction
            if (original == null) {
                transactionRepository.create(
                    TransactionEntity(
                        type = state.type.name,
                        amount = cents,
                        date = state.date.toEpochDay(),
                        accountId = accountId,
                        categoryId = state.categoryId,
                        transferAccountId = state.transferAccountId,
                        note = note
                    )
                )
            } else {
                // Etiquetas: ver nota del ticket 0031 (todavía no hay forma de asignarlas).
                transactionRepository.update(
                    original.copy(
                        type = state.type.name,
                        amount = cents,
                        date = state.date.toEpochDay(),
                        accountId = accountId,
                        categoryId = state.categoryId,
                        transferAccountId = state.transferAccountId,
                        note = note
                    ),
                    emptySet()
                )
            }
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _uiState.value.transactionId ?: return
        viewModelScope.launch {
            transactionRepository.delete(id)
            onDeleted()
        }
    }
}
