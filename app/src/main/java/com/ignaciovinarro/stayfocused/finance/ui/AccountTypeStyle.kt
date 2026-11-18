package com.ignaciovinarro.stayfocused.finance.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.ui.graphics.vector.ImageVector
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.finance.domain.AccountType

// El ícono de una cuenta lo da su tipo (ticket 0029: las cuentas no tienen selector de ícono).
fun accountTypeIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CASH -> Icons.Filled.Payments
    AccountType.BANK -> Icons.Filled.AccountBalance
    AccountType.CARD -> Icons.Filled.CreditCard
    AccountType.WALLET -> Icons.Filled.AccountBalanceWallet
}

@StringRes
fun accountTypeLabel(type: AccountType): Int = when (type) {
    AccountType.CASH -> R.string.account_type_cash
    AccountType.BANK -> R.string.account_type_bank
    AccountType.CARD -> R.string.account_type_card
    AccountType.WALLET -> R.string.account_type_wallet
}

// Lo que se guarda en `AccountEntity.iconKey`; al mostrar se usa el tipo, no esta clave.
fun accountIconKey(type: AccountType): String = "account_${type.name.lowercase()}"

// Tipo guardado como texto → AccountType; si el valor es desconocido, efectivo.
fun accountTypeOf(value: String): AccountType =
    runCatching { AccountType.valueOf(value) }.getOrDefault(AccountType.CASH)
