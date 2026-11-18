package com.ignaciovinarro.stayfocused.finance.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ignaciovinarro.stayfocused.core.ui.TaskColorKey
import com.ignaciovinarro.stayfocused.core.util.filterMoneyInput
import com.ignaciovinarro.stayfocused.core.util.formatArsForInput
import com.ignaciovinarro.stayfocused.core.util.parseArsToCents
import com.ignaciovinarro.stayfocused.finance.data.AccountEntity
import com.ignaciovinarro.stayfocused.finance.data.AccountRepository
import com.ignaciovinarro.stayfocused.finance.domain.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 0L = cuenta nueva, todavía no guardada.
private const val ARG_ACCOUNT_ID = "accountId"

data class AccountEditorUiState(
    val accountId: Long? = null, // null = cuenta nueva
    val name: String = "",
    val type: AccountType = AccountType.CASH,
    val initialBalanceText: String = "",
    val colorKey: String = TaskColorKey.BLUE.key,
    val showDeleteBlocked: Boolean = false
) {
    // Vacío = 0. Se permite negativo (por ejemplo una tarjeta con deuda).
    val initialBalanceCents: Long?
        get() = if (initialBalanceText.isBlank()) 0L else parseArsToCents(initialBalanceText, allowNegative = true)

    val isBalanceValid: Boolean get() = initialBalanceCents != null
    val isValid: Boolean get() = name.isNotBlank() && isBalanceValid
}

@HiltViewModel
class AccountEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val existingAccountId: Long = savedStateHandle.get<Long>(ARG_ACCOUNT_ID) ?: 0L

    private val _uiState = MutableStateFlow(AccountEditorUiState())
    val uiState: StateFlow<AccountEditorUiState> = _uiState.asStateFlow()

    // Entidad tal como está en la base, para no perder su `uuid`/`createdAt` al guardar.
    private var loadedAccount: AccountEntity? = null

    init {
        if (existingAccountId != 0L) {
            viewModelScope.launch {
                val account = accountRepository.getById(existingAccountId) ?: return@launch
                loadedAccount = account
                _uiState.update {
                    it.copy(
                        accountId = account.id,
                        name = account.name,
                        type = accountTypeOf(account.type),
                        initialBalanceText = formatArsForInput(account.initialBalance),
                        colorKey = account.colorKey
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onTypeChange(value: AccountType) = _uiState.update { it.copy(type = value) }
    fun onColorChange(value: String) = _uiState.update { it.copy(colorKey = value) }

    fun onInitialBalanceChange(value: String) =
        _uiState.update { it.copy(initialBalanceText = filterMoneyInput(value, allowNegative = true)) }

    fun onDismissDeleteBlocked() = _uiState.update { it.copy(showDeleteBlocked = false) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val balance = state.initialBalanceCents
        if (!state.isValid || balance == null) return
        viewModelScope.launch {
            val original = loadedAccount
            if (original == null) {
                accountRepository.create(
                    AccountEntity(
                        name = state.name.trim(),
                        type = state.type.name,
                        initialBalance = balance,
                        iconKey = accountIconKey(state.type),
                        colorKey = state.colorKey
                    )
                )
            } else {
                accountRepository.update(
                    original.copy(
                        name = state.name.trim(),
                        type = state.type.name,
                        initialBalance = balance,
                        iconKey = accountIconKey(state.type),
                        colorKey = state.colorKey
                    )
                )
            }
            onSaved()
        }
    }

    // Si la cuenta tiene transacciones el repositorio no la borra: se muestra el aviso.
    fun delete(onDeleted: () -> Unit) {
        val id = _uiState.value.accountId ?: return
        viewModelScope.launch {
            if (accountRepository.delete(id)) onDeleted() else _uiState.update { it.copy(showDeleteBlocked = true) }
        }
    }
}
