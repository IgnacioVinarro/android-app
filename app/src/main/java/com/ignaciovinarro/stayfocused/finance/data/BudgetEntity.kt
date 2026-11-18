package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Presupuesto mensual (FI-06). No hay una fila por mes: cada fila dice "desde el mes `effectiveFrom`
// el límite es `amount`" y rige hasta que otra fila posterior lo reemplace (ver `budgetLimitFor`).
// - `categoryId` null = presupuesto total del mes.
// - `effectiveFrom`: mes en formato AAAAMM (ej. 202611). Se compara como número.
@Entity(
    tableName = "budget",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["categoryId"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val categoryId: Long? = null,
    val effectiveFrom: Int,
    val amount: Long,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
