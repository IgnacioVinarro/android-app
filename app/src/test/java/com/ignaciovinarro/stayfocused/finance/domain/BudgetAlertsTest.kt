package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.BudgetEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetAlertsTest {

    private val nov = YearMonth.of(2026, 11)

    private fun budget(categoryId: Long?, amount: Long) =
        BudgetEntity(categoryId = categoryId, effectiveFrom = nov.toBudgetKey(), amount = amount)

    private fun expense(amount: Long, categoryId: Long) =
        TransactionEntity(type = "EXPENSE", amount = amount, date = nov.atDay(5).toEpochDay(), accountId = 1, categoryId = categoryId)

    private fun state(categoryId: Long?, percent: Int) =
        BudgetAlertState(categoryId, budgetProgress(100, percent.toLong()))

    @Test
    fun `solo evalua presupuestos con limite y el total va primero`() {
        val result = currentBudgetAlerts(
            budgets = listOf(budget(null, 10_000), budget(2L, 5_000)),
            transactions = listOf(expense(4_000, 1L), expense(5_000, 2L)),
            categoryIds = listOf(1L, 2L, 3L),
            month = nov
        )

        assertEquals(listOf<Long?>(null, 2L), result.map { it.categoryId }) // 1 y 3 no tienen límite
        assertEquals(BudgetAlert.WARNING_80, result[0].alert) // total: 9.000 de 10.000 = 90 %
        assertEquals(BudgetAlert.REACHED_100, result[1].alert)
    }

    @Test
    fun `sin presupuestos no hay estados`() {
        assertTrue(currentBudgetAlerts(emptyList(), listOf(expense(100, 1L)), listOf(1L), nov).isEmpty())
    }

    @Test
    fun `avisa cuando se llega al 80 por primera vez`() {
        val result = newlyReachedAlerts(listOf(state(1L, 80)), notified = emptyMap())

        assertEquals(listOf<Long?>(1L), result.map { it.categoryId })
    }

    @Test
    fun `no repite un nivel ya avisado`() {
        val result = newlyReachedAlerts(listOf(state(1L, 85)), notified = mapOf(1L to BudgetAlert.WARNING_80))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `avisa de nuevo al pasar del 80 al 100`() {
        val result = newlyReachedAlerts(listOf(state(1L, 100)), notified = mapOf(1L to BudgetAlert.WARNING_80))

        assertEquals(BudgetAlert.REACHED_100, result.single().alert)
    }

    @Test
    fun `saltar directo al 100 avisa una sola vez`() {
        val result = newlyReachedAlerts(listOf(state(null, 130)), notified = emptyMap())

        assertEquals(1, result.size)
        assertEquals(BudgetAlert.REACHED_100, result.single().alert)
    }

    @Test
    fun `por debajo del 80 no avisa y bajar de nivel tampoco`() {
        val current = listOf(state(1L, 50), state(2L, 80))

        val result = newlyReachedAlerts(current, notified = mapOf(2L to BudgetAlert.REACHED_100))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `el nivel guardado baja con el gasto y permite avisar otra vez`() {
        val alto = listOf(state(1L, 100))
        val bajo = listOf(state(1L, 10))

        val guardadoAlto = alertLevelsToStore(alto)
        val guardadoBajo = alertLevelsToStore(bajo)

        assertEquals(mapOf<Long?, BudgetAlert>(1L to BudgetAlert.REACHED_100), guardadoAlto)
        assertTrue(guardadoBajo.isEmpty())
        assertEquals(1, newlyReachedAlerts(alto, guardadoBajo).size)
    }

    @Test
    fun `el estado se guarda y se lee igual`() {
        val levels = mapOf<Long?, BudgetAlert>(null to BudgetAlert.WARNING_80, 10L to BudgetAlert.REACHED_100)

        val text = encodeAlertState(nov, levels)

        assertEquals("202611|total=1|10=2", text)
        assertEquals(levels, decodeAlertState(text, nov))
    }

    @Test
    fun `un estado de otro mes, vacio o roto se lee como vacio`() {
        assertTrue(decodeAlertState(encodeAlertState(nov.minusMonths(1), mapOf(1L to BudgetAlert.WARNING_80)), nov).isEmpty())
        assertTrue(decodeAlertState(null, nov).isEmpty())
        assertTrue(decodeAlertState("", nov).isEmpty())
        assertTrue(decodeAlertState("202611|abc=1|5=9|x|7=1=2", nov).isEmpty())
    }

    @Test
    fun `un renglon roto no descarta los buenos`() {
        assertEquals(
            mapOf<Long?, BudgetAlert>(7L to BudgetAlert.WARNING_80),
            decodeAlertState("202611|basura|7=1", nov)
        )
    }
}
