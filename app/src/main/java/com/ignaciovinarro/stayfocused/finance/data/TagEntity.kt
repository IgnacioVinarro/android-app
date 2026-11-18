package com.ignaciovinarro.stayfocused.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Etiqueta (FI-02): lista propia que se crea, renombra y elimina. El nombre no se repite entre las
// etiquetas existentes (se valida con `validateTagName`; Room no permite índices parciales por status).
@Entity(tableName = "tag", indices = [Index(value = ["uuid"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
