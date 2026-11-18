package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity

// En la base los tipos se guardan como texto (nombre del enum), igual que `RecurrenceType`.
enum class TransactionType { INCOME, EXPENSE, TRANSFER }

enum class CategoryType { INCOME, EXPENSE }

enum class AccountType { CASH, BANK, CARD, WALLET }

fun TransactionEntity.typeOrNull(): TransactionType? =
    runCatching { TransactionType.valueOf(type) }.getOrNull()
