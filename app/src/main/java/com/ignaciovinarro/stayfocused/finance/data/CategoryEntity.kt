package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Categoría de ingreso o de gasto (FI-04). `type` es el nombre de `CategoryType`. `isDefault` marca
// las que se siembran al inicio; igual se pueden editar y borrar como las propias.
@Entity(tableName = "category", indices = [Index(value = ["uuid"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val iconKey: String,
    val colorKey: String,
    val isDefault: Boolean = false,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
