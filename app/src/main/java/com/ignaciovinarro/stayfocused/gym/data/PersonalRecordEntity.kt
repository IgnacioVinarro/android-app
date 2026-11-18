package com.ignaciovinarro.stayfocused.gym.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Récord personal (GY-09). `kind` es el nombre de `RecordKind`; `value` en gramos (peso, volumen, 1RM) o
// repeticiones (MAX_REPS_AT_WEIGHT). Se deriva de las series; se guarda para consultarlo rápido.
@Entity(
    tableName = "personal_record",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["exerciseId", "kind"]),
        Index(value = ["sessionSetId"])
    ]
)
data class PersonalRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val exerciseId: Long,
    val kind: String,
    val value: Long,
    // Peso (gramos) con el que se logró; solo importa en MAX_REPS_AT_WEIGHT (0 en los demás).
    val weight: Long = 0,
    val sessionSetId: Long,
    val achievedAt: Long,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
