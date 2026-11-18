package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.AccountEntity
import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountBalanceTest {

    private val cuenta = AccountEntity(id = 1, name = "Banco", type = "BANK", initialBalance = 100_000, iconKey = "i", colorKey = "blue")

    private fun tx(
        type: TransactionType,
        amount: Long,
        accountId: Long = 1,
        transferAccountId: Long? = null,
        status: Int = 0
    ) = TransactionEntity(
        type = type.name, amount = amount, date = 19000, accountId = accountId,
        transferAccountId = transferAccountId, status = status
    )

    @Test
    fun `sin transacciones el saldo es el saldo inicial`() {
        assertEquals(100_000L, accountBalance(cuenta, emptyList()))
    }

    @Test
    fun `ingresos suman y gastos restan`() {
        val saldo = accountBalance(cuenta, listOf(tx(TransactionType.INCOME, 50_000), tx(TransactionType.EXPENSE, 20_000)))
        assertEquals(130_000L, saldo)
    }

    @Test
    fun `transferencia saliente resta de la cuenta origen`() {
        val saldo = accountBalance(cuenta, listOf(tx(TransactionType.TRANSFER, 30_000, accountId = 1, transferAccountId = 2)))
        assertEquals(70_000L, saldo)
    }

    @Test
    fun `transferencia entrante suma a la cuenta destino`() {
        val saldo = accountBalance(cuenta, listOf(tx(TransactionType.TRANSFER, 30_000, accountId = 2, transferAccountId = 1)))
        assertEquals(130_000L, saldo)
    }

    @Test
    fun `ignora transacciones de otras cuentas`() {
        val saldo = accountBalance(
            cuenta,
            listOf(tx(TransactionType.INCOME, 999, accountId = 2), tx(TransactionType.EXPENSE, 999, accountId = 3))
        )
        assertEquals(100_000L, saldo)
    }

    @Test
    fun `ignora transacciones borradas y de tipo desconocido`() {
        val desconocida = tx(TransactionType.INCOME, 5_000).copy(type = "OTRO")
        val saldo = accountBalance(cuenta, listOf(tx(TransactionType.INCOME, 5_000, status = -1), desconocida))
        assertEquals(100_000L, saldo)
    }

    @Test
    fun `el saldo puede quedar negativo`() {
        assertEquals(-50_000L, accountBalance(cuenta, listOf(tx(TransactionType.EXPENSE, 150_000))))
    }
}
