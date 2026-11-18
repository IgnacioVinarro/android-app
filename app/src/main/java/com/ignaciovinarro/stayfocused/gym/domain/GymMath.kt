package com.ignaciovinarro.stayfocused.gym.domain

// Una serie tal como la ven las reglas de dominio (sin clases de Room ni de Android).
// `weight` en gramos enteros; `reps` y `durationSeconds` según el tipo de registro.
data class SetLog(
    val id: Long = 0,
    val weight: Long = 0,
    val reps: Int = 0,
    val durationSeconds: Int = 0,
    val type: SetType = SetType.NORMAL,
    val completed: Boolean = true
)

// Solo cuentan las series completadas y que no sean de calentamiento (CLAUDE.md).
fun SetLog.countsForStats(): Boolean = completed && type != SetType.WARMUP

// Volumen de una serie = peso × repeticiones (gramos × reps).
fun setVolume(set: SetLog): Long = if (set.countsForStats()) set.weight * set.reps else 0L

// Volumen del ejercicio = suma de las series que cuentan.
fun exerciseVolume(sets: List<SetLog>): Long = sets.sumOf { setVolume(it) }

// 1RM estimado con Epley: peso × (1 + reps / 30), en gramos enteros (half-up, sin decimales).
// Una sola repetición es el propio peso. Sin peso o sin repeticiones no hay estimación.
fun estimatedOneRm(weight: Long, reps: Int): Long {
    if (weight <= 0 || reps <= 0) return 0L
    return (weight * (30 + reps) + 15) / 30
}

// Volumen por grupo muscular (GY-11 / GY-18): suma de volumen de las series que cuentan.
fun volumeByMuscleGroup(setsByGroup: List<Pair<MuscleGroup, SetLog>>): Map<MuscleGroup, Long> =
    setsByGroup
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, sets) -> exerciseVolume(sets) }
