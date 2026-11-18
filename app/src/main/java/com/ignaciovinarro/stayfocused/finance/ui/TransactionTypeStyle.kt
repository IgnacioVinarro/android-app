package com.ignaciovinarro.stayfocused.finance.ui

import androidx.annotation.StringRes
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.finance.domain.TransactionType

// Nombre del tipo de transacción para mostrar; un tipo desconocido (no debería pasar) se ve como
// transferencia, igual que el ícono.
@StringRes
fun transactionTypeLabel(type: TransactionType?): Int = when (type) {
    TransactionType.INCOME -> R.string.transaction_type_income
    TransactionType.EXPENSE -> R.string.transaction_type_expense
    else -> R.string.transaction_type_transfer
}
