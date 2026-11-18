package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.BudgetEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import java.time.YearMonth

// Estado de un presupuesto del mes (`categoryId` null = total). Solo existe si hay un límite fijado.
data class BudgetAlertState(val categoryId: Long?, val progress: BudgetProgress) {
    val alert: BudgetAlert get() = progress.alert
}

// Función pura (FI-06): el nivel de alerta de cada presupuesto con límite en el mes: el total y cada
// categoría de gasto que tenga límite. `categoryIds` son las categorías de gasto existentes.
fun currentBudgetAlerts(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    categoryIds: List<Long>,
    month: YearMonth
): List<BudgetAlertState> =
    (listOf<Long?>(null) + categoryIds).mapNotNull { categoryId ->
        val limit = budgetLimitFor(budgets, categoryId, month) ?: return@mapNotNull null
        BudgetAlertState(categoryId, budgetProgress(limit, monthlySpent(transactions, categoryId, month)))
    }

// Función pura: de los presupuestos actuales, cuáles hay que avisar ahora: los que llegaron a un nivel
// MÁS ALTO que el último avisado (`notified`; lo que falta equivale a "ninguno"). Nunca avisa "NONE".
fun newlyReachedAlerts(
    current: List<BudgetAlertState>,
    notified: Map<Long?, BudgetAlert>
): List<BudgetAlertState> =
    current.filter { it.alert != BudgetAlert.NONE && it.alert > (notified[it.categoryId] ?: BudgetAlert.NONE) }

// Función pura: lo que queda registrado como "ya avisado" tras evaluar. Es el nivel actual de cada
// presupuesto (sin los que no tienen alerta): así, si el gasto baja y vuelve a subir, se avisa otra vez.
fun alertLevelsToStore(current: List<BudgetAlertState>): Map<Long?, BudgetAlert> =
    current.filter { it.alert != BudgetAlert.NONE }.associate { it.categoryId to it.alert }

private const val TOTAL_TOKEN = "total"

// Estado guardado = "AAAAMM|total=2|10=1": el mes y, por presupuesto, el nivel (BudgetAlert.ordinal).
// Texto simple para guardarlo en una sola preferencia de DataStore.
fun encodeAlertState(month: YearMonth, levels: Map<Long?, BudgetAlert>): String =
    (listOf(month.toBudgetKey().toString()) +
        levels.map { (categoryId, alert) -> "${categoryId ?: TOTAL_TOKEN}=${alert.ordinal}" }).joinToString("|")

// Inverso de `encodeAlertState`. Si el texto es de otro mes, está vacío o está roto, devuelve vacío: peor
// caso, se vuelve a avisar un nivel una vez; nunca falla.
fun decodeAlertState(text: String?, month: YearMonth): Map<Long?, BudgetAlert> {
    val parts = text?.split("|").orEmpty()
    if (parts.firstOrNull() != month.toBudgetKey().toString()) return emptyMap()
    return parts.drop(1).mapNotNull { part ->
        val (key, level) = part.split("=").takeIf { it.size == 2 } ?: return@mapNotNull null
        val categoryId = if (key == TOTAL_TOKEN) null else key.toLongOrNull() ?: return@mapNotNull null
        val alert = level.toIntOrNull()?.let { BudgetAlert.entries.getOrNull(it) } ?: return@mapNotNull null
        categoryId to alert
    }.toMap()
}
