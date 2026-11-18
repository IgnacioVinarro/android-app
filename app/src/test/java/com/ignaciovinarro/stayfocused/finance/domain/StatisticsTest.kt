package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsTest {

    private val nov = YearMonth.of(2026, 11)
    private val oct = YearMonth.of(2026, 10)

    private fun tx(
        type: String,
        amount: Long,
        month: YearMonth = nov,
        day: Int = 10,
        categoryId: Long? = 1L,
        status: Int = 0
    ) = TransactionEntity(
        type = type, amount = amount, date = month.atDay(day).toEpochDay(), accountId = 1,
        categoryId = categoryId, status = status
    )

    @Test
    fun `totales del mes suman ingresos y gastos y calculan el ahorro`() {
        val totals = monthTotals(
            listOf(tx("INCOME", 100_000), tx("EXPENSE", 30_000), tx("EXPENSE", 20_000)),
            nov
        )

        assertEquals(100_000L, totals.income)
        assertEquals(50_000L, totals.expense)
        assertEquals(50_000L, totals.savings)
    }

    @Test
    fun `las transferencias no cuentan como ingreso ni gasto`() {
        val totals = monthTotals(listOf(tx("TRANSFER", 80_000, categoryId = null)), nov)

        assertEquals(MonthTotals(0, 0), totals)
    }

    @Test
    fun `no cuentan otros meses ni las transacciones borradas`() {
        val totals = monthTotals(
            listOf(
                tx("EXPENSE", 1_000, month = oct),
                tx("EXPENSE", 2_000, status = -1),
                tx("EXPENSE", 500)
            ),
            nov
        )

        assertEquals(500L, totals.expense)
    }

    @Test
    fun `los limites del mes se incluyen y el ahorro puede ser negativo`() {
        val totals = monthTotals(
            listOf(tx("EXPENSE", 300, day = 1), tx("EXPENSE", 400, day = 30), tx("INCOME", 100)),
            nov
        )

        assertEquals(700L, totals.expense)
        assertEquals(-600L, totals.savings)
    }

    @Test
    fun `gasto por categoria ordena de mayor a menor y calcula porcentajes`() {
        val result = spendByCategory(
            listOf(
                tx("EXPENSE", 2_000, categoryId = 1L),
                tx("EXPENSE", 6_000, categoryId = 2L),
                tx("EXPENSE", 2_000, categoryId = 1L),
                tx("INCOME", 99_000, categoryId = 3L)
            ),
            nov
        )

        assertEquals(listOf<Long?>(2L, 1L), result.map { it.categoryId })
        assertEquals(listOf(6_000L, 4_000L), result.map { it.amount })
        assertEquals(listOf(60, 40), result.map { it.percent })
    }

    @Test
    fun `a igual monto el orden es estable por id y sin categoria va al final`() {
        val result = spendByCategory(
            listOf(
                tx("EXPENSE", 1_000, categoryId = null),
                tx("EXPENSE", 1_000, categoryId = 5L),
                tx("EXPENSE", 1_000, categoryId = 2L)
            ),
            nov
        )

        assertEquals(listOf<Long?>(2L, 5L, null), result.map { it.categoryId })
        assertTrue(result.all { it.percent == 33 }) // hacia abajo
    }

    @Test
    fun `sin gastos en el mes el ranking esta vacio`() {
        assertTrue(spendByCategory(listOf(tx("INCOME", 1_000), tx("EXPENSE", 500, month = oct)), nov).isEmpty())
    }

    @Test
    fun `variacion porcentual contra el mes anterior`() {
        assertEquals(50, percentChange(150, 100))
        assertEquals(-25, percentChange(75, 100))
        assertEquals(0, percentChange(100, 100))
        assertEquals(-100, percentChange(0, 100))
    }

    @Test
    fun `variacion sin base de comparacion es null`() {
        assertNull(percentChange(500, 0))
        assertNull(percentChange(0, 0))
    }

    @Test
    fun `la variacion usa el valor absoluto de la base`() {
        // Ahorro de -100 a +50: mejoró, así que la variación es positiva.
        assertEquals(150, percentChange(50, -100))
    }

    @Test
    fun `serie mensual va del mes mas viejo al mas nuevo y rellena con cero`() {
        val series = monthlyExpenseSeries(
            listOf(tx("EXPENSE", 700, month = nov), tx("EXPENSE", 300, month = YearMonth.of(2026, 9))),
            last = nov,
            count = 3
        )

        assertEquals(listOf(YearMonth.of(2026, 9), oct, nov), series.map { it.first })
        assertEquals(listOf(300L, 0L, 700L), series.map { it.second })
    }

    @Test
    fun `la serie cruza el cambio de anio`() {
        val series = monthlyExpenseSeries(emptyList(), last = YearMonth.of(2027, 2), count = 4)

        assertEquals(
            listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 12), YearMonth.of(2027, 1), YearMonth.of(2027, 2)),
            series.map { it.first }
        )
    }
}
