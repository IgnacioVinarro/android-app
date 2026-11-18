package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.AccountEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity

// Función pura (FI-05): saldo = saldo inicial + ingresos − gastos − transferencias salientes +
// transferencias entrantes. Se calcula siempre, nunca se guarda. Ignora transacciones borradas y de
// tipo desconocido. Montos en centavos.
fun accountBalance(account: AccountEntity, transactions: List<TransactionEntity>): Long {
    var balance = account.initialBalance
    for (t in transactions) {
        if (t.status != 0) continue
        when (t.typeOrNull()) {
            TransactionType.INCOME -> if (t.accountId == account.id) balance += t.amount
            TransactionType.EXPENSE -> if (t.accountId == account.id) balance -= t.amount
            TransactionType.TRANSFER -> {
                if (t.accountId == account.id) balance -= t.amount
                if (t.transferAccountId == account.id) balance += t.amount
            }
            null -> Unit
        }
    }
    return balance
}
