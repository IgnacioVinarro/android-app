package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity

enum class TransactionError {
    INVALID_TYPE,
    AMOUNT_NOT_POSITIVE,
    CATEGORY_REQUIRED,
    CATEGORY_TYPE_MISMATCH,
    TRANSFER_ACCOUNT_NOT_ALLOWED,
    TRANSFER_ACCOUNT_REQUIRED,
    TRANSFER_SAME_ACCOUNT,
    CATEGORY_NOT_ALLOWED_IN_TRANSFER
}

// Función pura (FI-01 / FI-02): devuelve el primer error de la transacción o `null` si es válida.
// `categoryType` es el tipo de la categoría elegida (lo resuelve quien llama); si es null no se
// comprueba la coincidencia con el tipo de la transacción.
fun validateTransaction(transaction: TransactionEntity, categoryType: CategoryType?): TransactionError? {
    val type = transaction.typeOrNull() ?: return TransactionError.INVALID_TYPE
    if (transaction.amount <= 0) return TransactionError.AMOUNT_NOT_POSITIVE
    return when (type) {
        TransactionType.INCOME, TransactionType.EXPENSE -> when {
            transaction.transferAccountId != null -> TransactionError.TRANSFER_ACCOUNT_NOT_ALLOWED
            transaction.categoryId == null -> TransactionError.CATEGORY_REQUIRED
            categoryType != null && categoryType.name != type.name -> TransactionError.CATEGORY_TYPE_MISMATCH
            else -> null
        }
        TransactionType.TRANSFER -> when {
            transaction.transferAccountId == null -> TransactionError.TRANSFER_ACCOUNT_REQUIRED
            transaction.transferAccountId == transaction.accountId -> TransactionError.TRANSFER_SAME_ACCOUNT
            transaction.categoryId != null -> TransactionError.CATEGORY_NOT_ALLOWED_IN_TRANSFER
            else -> null
        }
    }
}
