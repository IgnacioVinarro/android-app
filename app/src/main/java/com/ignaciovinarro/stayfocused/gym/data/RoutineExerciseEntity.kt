package com.ignaciovinarro.stayfocused.gym.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Ejercicio dentro de una rutina (GY-03 / GY-16). `targetWeight` en gramos. Los ejercicios con el mismo
// `supersetGroup` forman una superserie o circuito (nulo = va solo).
@Entity(
    tableName = "routine_exercise",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["routineId"]),
        Index(value = ["exerciseId"])
    ]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val routineId: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val targetWeight: Long = 0,
    val supersetGroup: Int? = null,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
