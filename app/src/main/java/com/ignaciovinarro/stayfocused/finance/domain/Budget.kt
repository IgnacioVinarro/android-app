package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.BudgetEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import java.time.YearMonth

enum class BudgetAlert { NONE, WARNING_80, REACHED_100 }

data class BudgetProgress(val percent: Int, val alert: BudgetAlert)

// Mes como número AAAAMM (ej. 202611): se guarda así en `BudgetEntity.effectiveFrom` y se compara
// como entero.
fun YearMonth.toBudgetKey(): Int = year * 100 + monthValue

// Función pura (FI-06): porcentaje gastado (entero, redondeado hacia abajo) y alerta. Alerta al
// LLEGAR al 80 % y al 100 %. Un límite <= 0 no es un presupuesto válido: sin progreso ni alerta.
fun budgetProgress(limit: Long, spent: Long): BudgetProgress {
    if (limit <= 0) return BudgetProgress(0, BudgetAlert.NONE)
    val percent = (spent.coerceAtLeast(0) * 100 / limit).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val alert = when {
        percent >= 100 -> BudgetAlert.REACHED_100
        percent >= 80 -> BudgetAlert.WARNING_80
        else -> BudgetAlert.NONE
    }
    return BudgetProgress(percent, alert)
}

// Función pura: el límite que rige en `month` para esa categoría (`null` = total). Es la fila con
// `effectiveFrom` más reciente que no sea posterior al mes; `null` si todavía no hay presupuesto.
// Así un presupuesto se repite solo cada mes y, si se edita, el nuevo monto se mantiene hasta la
// próxima edición.
fun budgetLimitFor(budgets: List<BudgetEntity>, categoryId: Long?, month: YearMonth): Long? =
    budgets
        .filter { it.status == 0 && it.categoryId == categoryId && it.effectiveFrom <= month.toBudgetKey() }
        .maxByOrNull { it.effectiveFrom }
        ?.amount

// Función pura: gasto del mes. Suma solo gastos (las transferencias no cuentan) con fecha dentro del
// mes; si `categoryId` es null suma todos los gastos (presupuesto total).
fun monthlySpent(transactions: List<TransactionEntity>, categoryId: Long?, month: YearMonth): Long {
    val from = month.atDay(1).toEpochDay()
    val to = month.atEndOfMonth().toEpochDay()
    return transactions
        .filter {
            it.status == 0 &&
                it.typeOrNull() == TransactionType.EXPENSE &&
                it.date in from..to &&
                (categoryId == null || it.categoryId == categoryId)
        }
        .sumOf { it.amount }
}
