package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import java.time.YearMonth

// Ingresos y gastos de un mes (centavos). Las transferencias no cuentan (CLAUDE.md, reglas de Finanzas).
data class MonthTotals(val income: Long, val expense: Long) {
    val savings: Long get() = income - expense
}

// Transacciones vivas cuya fecha cae dentro del mes.
private fun List<TransactionEntity>.inMonth(month: YearMonth): List<TransactionEntity> {
    val from = month.atDay(1).toEpochDay()
    val to = month.atEndOfMonth().toEpochDay()
    return filter { it.status == 0 && it.date in from..to }
}

// Función pura (FI-10): ingresos y gastos del mes.
fun monthTotals(transactions: List<TransactionEntity>, month: YearMonth): MonthTotals {
    val inMonth = transactions.inMonth(month)
    return MonthTotals(
        income = inMonth.filter { it.typeOrNull() == TransactionType.INCOME }.sumOf { it.amount },
        expense = inMonth.filter { it.typeOrNull() == TransactionType.EXPENSE }.sumOf { it.amount }
    )
}

// Gasto de una categoría (`categoryId` null = gastos sin categoría) y su porcentaje del gasto total del mes.
data class CategorySpend(val categoryId: Long?, val amount: Long, val percent: Int)

// Función pura (FI-08): gasto del mes por categoría, de mayor a menor (a igual monto, por id para que el
// orden sea estable). El porcentaje es entero, redondeado hacia abajo; sin gastos devuelve lista vacía.
fun spendByCategory(transactions: List<TransactionEntity>, month: YearMonth): List<CategorySpend> {
    val expenses = transactions.inMonth(month).filter { it.typeOrNull() == TransactionType.EXPENSE }
    val total = expenses.sumOf { it.amount }
    if (total <= 0) return emptyList()
    return expenses.groupBy { it.categoryId }
        .map { (categoryId, list) ->
            val amount = list.sumOf { it.amount }
            CategorySpend(categoryId, amount, (amount * 100 / total).toInt())
        }
        .sortedWith(compareByDescending<CategorySpend> { it.amount }.thenBy { it.categoryId ?: Long.MAX_VALUE })
}

// Función pura: variación porcentual (entera, hacia cero) de `current` respecto de `previous`. `null` si no
// hay base de comparación (el mes anterior fue 0): no se inventa un porcentaje.
fun percentChange(current: Long, previous: Long): Int? =
    if (previous == 0L) null else ((current - previous) * 100 / kotlin.math.abs(previous)).toInt()

// Función pura (FI-08): gasto de cada uno de los últimos `count` meses terminando en `last`, del más viejo
// al más nuevo. Los meses sin gastos valen 0.
fun monthlyExpenseSeries(
    transactions: List<TransactionEntity>,
    last: YearMonth,
    count: Int
): List<Pair<YearMonth, Long>> =
    (count - 1 downTo 0).map { back ->
        val month = last.minusMonths(back.toLong())
        month to monthTotals(transactions, month).expense
    }
