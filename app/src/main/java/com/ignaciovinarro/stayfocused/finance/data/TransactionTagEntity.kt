package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Unión muchos-a-muchos entre transacciones y etiquetas. El par (transactionId, tagId) es único
// incluso con borrado lógico: quitar una etiqueta pone status = -1 y volver a ponerla reactiva la fila.
@Entity(
    tableName = "transaction_tag",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["transactionId", "tagId"], unique = true),
        Index(value = ["tagId"])
    ]
)
data class TransactionTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val transactionId: Long,
    val tagId: Long,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
