package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Transacción (FI-01 / FI-02). `type` es el nombre de `TransactionType`.
// - `amount`: siempre positivo, en centavos de ARS; el signo lo da el tipo.
// - `date`: epochDay.
// - Transferencia: UNA sola fila; `accountId` es el origen y `transferAccountId` el destino. No lleva
//   categoría ni cuenta como ingreso/gasto.
// Todas las claves foráneas apuntan al `id` numérico (no al uuid).
@Entity(
    tableName = "finance_transaction",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["transferAccountId"]),
        Index(value = ["date"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val type: String,
    val amount: Long,
    val date: Long,
    val accountId: Long,
    val categoryId: Long? = null,
    val transferAccountId: Long? = null,
    val note: String? = null,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
