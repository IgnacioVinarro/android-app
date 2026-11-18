package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.BudgetEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetTest {

    // --- budgetProgress ---

    @Test
    fun `79 por ciento no alerta`() {
        assertEquals(BudgetProgress(79, BudgetAlert.NONE), budgetProgress(limit = 100, spent = 79))
    }

    @Test
    fun `80 por ciento avisa`() {
        assertEquals(BudgetProgress(80, BudgetAlert.WARNING_80), budgetProgress(100, 80))
    }

    @Test
    fun `99 por ciento sigue en aviso de 80`() {
        assertEquals(BudgetAlert.WARNING_80, budgetProgress(100, 99).alert)
    }

    @Test
    fun `100 por ciento llega al limite`() {
        assertEquals(BudgetProgress(100, BudgetAlert.REACHED_100), budgetProgress(100, 100))
    }

    @Test
    fun `mas del 100 por ciento sigue en limite alcanzado`() {
        assertEquals(BudgetProgress(150, BudgetAlert.REACHED_100), budgetProgress(100, 150))
    }

    @Test
    fun `el porcentaje se redondea hacia abajo`() {
        // 79,9 % no llega al 80 %.
        assertEquals(BudgetProgress(79, BudgetAlert.NONE), budgetProgress(limit = 1000, spent = 799))
    }

    @Test
    fun `limite cero o negativo no tiene progreso ni alerta`() {
        assertEquals(BudgetProgress(0, BudgetAlert.NONE), budgetProgress(0, 500))
        assertEquals(BudgetProgress(0, BudgetAlert.NONE), budgetProgress(-10, 500))
    }

    @Test
    fun `gasto negativo cuenta como cero`() {
        assertEquals(BudgetProgress(0, BudgetAlert.NONE), budgetProgress(100, -5))
    }

    // --- budgetLimitFor ---

    private fun budget(categoryId: Long?, from: Int, amount: Long, status: Int = 0) =
        BudgetEntity(categoryId = categoryId, effectiveFrom = from, amount = amount, status = status)

    @Test
    fun `sin presupuestos no hay limite`() {
        assertNull(budgetLimitFor(emptyList(), 1L, YearMonth.of(2026, 11)))
    }

    @Test
    fun `antes de que rija el presupuesto no hay limite`() {
        assertNull(budgetLimitFor(listOf(budget(1L, 202611, 5_000)), 1L, YearMonth.of(2026, 10)))
    }

    @Test
    fun `el presupuesto se repite en los meses siguientes`() {
        val budgets = listOf(budget(1L, 202611, 5_000))
        assertEquals(5_000L, budgetLimitFor(budgets, 1L, YearMonth.of(2026, 11)))
        assertEquals(5_000L, budgetLimitFor(budgets, 1L, YearMonth.of(2027, 3)))
    }

    @Test
    fun `una edicion posterior rige desde su mes y se mantiene`() {
        val budgets = listOf(budget(1L, 202611, 5_000), budget(1L, 202702, 8_000))
        assertEquals(5_000L, budgetLimitFor(budgets, 1L, YearMonth.of(2027, 1)))
        assertEquals(8_000L, budgetLimitFor(budgets, 1L, YearMonth.of(2027, 2)))
        assertEquals(8_000L, budgetLimitFor(budgets, 1L, YearMonth.of(2028, 6)))
    }

    @Test
    fun `distingue categorias y el total`() {
        val budgets = listOf(budget(1L, 202611, 5_000), budget(null, 202611, 20_000), budget(2L, 202611, 1_000))
        assertEquals(5_000L, budgetLimitFor(budgets, 1L, YearMonth.of(2026, 11)))
        assertEquals(20_000L, budgetLimitFor(budgets, null, YearMonth.of(2026, 11)))
        assertNull(budgetLimitFor(budgets, 3L, YearMonth.of(2026, 11)))
    }

    @Test
    fun `ignora presupuestos borrados`() {
        assertNull(budgetLimitFor(listOf(budget(1L, 202611, 5_000, status = -1)), 1L, YearMonth.of(2026, 11)))
    }

    @Test
    fun `la clave del mes es AAAAMM`() {
        assertEquals(202601, YearMonth.of(2026, 1).toBudgetKey())
        assertEquals(202612, YearMonth.of(2026, 12).toBudgetKey())
    }

    // --- monthlySpent ---

    private val mes = YearMonth.of(2026, 11)

    private fun gasto(day: LocalDate, amount: Long, categoryId: Long? = 1L, type: TransactionType = TransactionType.EXPENSE, status: Int = 0) =
        TransactionEntity(
            type = type.name, amount = amount, date = day.toEpochDay(), accountId = 1,
            categoryId = categoryId, status = status
        )

    @Test
    fun `suma gastos del mes de la categoria`() {
        val txs = listOf(
            gasto(LocalDate.of(2026, 11, 1), 100),
            gasto(LocalDate.of(2026, 11, 30), 200),
            gasto(LocalDate.of(2026, 10, 31), 999), // mes anterior
            gasto(LocalDate.of(2026, 12, 1), 999), // mes siguiente
            gasto(LocalDate.of(2026, 11, 15), 999, categoryId = 2L) // otra categoría
        )
        assertEquals(300L, monthlySpent(txs, 1L, mes))
    }

    @Test
    fun `categoria null suma todos los gastos del mes`() {
        val txs = listOf(gasto(LocalDate.of(2026, 11, 5), 100, categoryId = 1L), gasto(LocalDate.of(2026, 11, 6), 50, categoryId = 2L))
        assertEquals(150L, monthlySpent(txs, null, mes))
    }

    @Test
    fun `no cuenta ingresos transferencias ni borradas`() {
        val txs = listOf(
            gasto(LocalDate.of(2026, 11, 5), 100, type = TransactionType.INCOME),
            gasto(LocalDate.of(2026, 11, 5), 100, categoryId = null, type = TransactionType.TRANSFER),
            gasto(LocalDate.of(2026, 11, 5), 100, status = -1)
        )
        assertEquals(0L, monthlySpent(txs, null, mes))
    }
}
