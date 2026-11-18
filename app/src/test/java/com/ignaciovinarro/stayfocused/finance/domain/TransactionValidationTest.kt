package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionValidationTest {

    private fun tx(
        type: String,
        amount: Long = 1_000,
        accountId: Long = 1,
        categoryId: Long? = null,
        transferAccountId: Long? = null
    ) = TransactionEntity(
        type = type, amount = amount, date = 19000, accountId = accountId,
        categoryId = categoryId, transferAccountId = transferAccountId
    )

    @Test
    fun `gasto valido`() {
        assertNull(validateTransaction(tx("EXPENSE", categoryId = 5), CategoryType.EXPENSE))
    }

    @Test
    fun `ingreso valido`() {
        assertNull(validateTransaction(tx("INCOME", categoryId = 5), CategoryType.INCOME))
    }

    @Test
    fun `transferencia valida`() {
        assertNull(validateTransaction(tx("TRANSFER", transferAccountId = 2), null))
    }

    @Test
    fun `tipo desconocido`() {
        assertEquals(TransactionError.INVALID_TYPE, validateTransaction(tx("OTRO", categoryId = 5), null))
    }

    @Test
    fun `monto cero o negativo`() {
        assertEquals(TransactionError.AMOUNT_NOT_POSITIVE, validateTransaction(tx("EXPENSE", amount = 0, categoryId = 5), null))
        assertEquals(TransactionError.AMOUNT_NOT_POSITIVE, validateTransaction(tx("EXPENSE", amount = -1, categoryId = 5), null))
    }

    @Test
    fun `gasto sin categoria`() {
        assertEquals(TransactionError.CATEGORY_REQUIRED, validateTransaction(tx("EXPENSE"), null))
    }

    @Test
    fun `gasto con categoria de ingreso`() {
        assertEquals(
            TransactionError.CATEGORY_TYPE_MISMATCH,
            validateTransaction(tx("EXPENSE", categoryId = 5), CategoryType.INCOME)
        )
    }

    @Test
    fun `ingreso con categoria de gasto`() {
        assertEquals(
            TransactionError.CATEGORY_TYPE_MISMATCH,
            validateTransaction(tx("INCOME", categoryId = 5), CategoryType.EXPENSE)
        )
    }

    @Test
    fun `gasto con cuenta destino de transferencia`() {
        assertEquals(
            TransactionError.TRANSFER_ACCOUNT_NOT_ALLOWED,
            validateTransaction(tx("EXPENSE", categoryId = 5, transferAccountId = 2), null)
        )
    }

    @Test
    fun `transferencia sin cuenta destino`() {
        assertEquals(TransactionError.TRANSFER_ACCOUNT_REQUIRED, validateTransaction(tx("TRANSFER"), null))
    }

    @Test
    fun `transferencia a la misma cuenta`() {
        assertEquals(
            TransactionError.TRANSFER_SAME_ACCOUNT,
            validateTransaction(tx("TRANSFER", accountId = 1, transferAccountId = 1), null)
        )
    }

    @Test
    fun `transferencia con categoria`() {
        assertEquals(
            TransactionError.CATEGORY_NOT_ALLOWED_IN_TRANSFER,
            validateTransaction(tx("TRANSFER", transferAccountId = 2, categoryId = 5), null)
        )
    }
}
