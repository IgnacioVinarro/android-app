package com.ignaciovinarro.stayfocused.gym.domain

// Mejores marcas previas de un ejercicio (de sesiones anteriores). Todo en 0 / vacío = sin historial.
data class PersonalBests(
    val maxWeight: Long = 0,
    val maxVolume: Long = 0,
    val maxOneRm: Long = 0,
    // Máximo de repeticiones logradas con cada peso (gramos).
    val maxRepsAtWeight: Map<Long, Int> = emptyMap()
)

// Un récord nuevo. `setId` es la serie que lo logró (0 si no hay una sola: ver MAX_VOLUME, que usa la
// serie que cerró el volumen). `weight` solo importa para MAX_REPS_AT_WEIGHT.
data class NewRecord(val kind: RecordKind, val value: Long, val setId: Long, val weight: Long = 0)

// Récords que logran las series de UN ejercicio en UNA sesión contra las mejores marcas previas (GY-09).
// Solo se comparan series que cuentan; hay que superar la marca (empatar no es récord) y con historial
// vacío cualquier serie válida con valor > 0 es récord. Por tipo se informa un solo récord: el más alto.
fun detectRecords(sets: List<SetLog>, bests: PersonalBests): List<NewRecord> {
    val valid = sets.filter { it.countsForStats() && it.weight > 0 && it.reps > 0 }
    if (valid.isEmpty()) return emptyList()
    val records = mutableListOf<NewRecord>()

    val heaviest = valid.maxBy { it.weight }
    if (heaviest.weight > bests.maxWeight) {
        records += NewRecord(RecordKind.MAX_WEIGHT, heaviest.weight, heaviest.id)
    }

    val bestOneRmSet = valid.maxBy { estimatedOneRm(it.weight, it.reps) }
    val oneRm = estimatedOneRm(bestOneRmSet.weight, bestOneRmSet.reps)
    if (oneRm > bests.maxOneRm) {
        records += NewRecord(RecordKind.ESTIMATED_1RM, oneRm, bestOneRmSet.id)
    }

    val volume = exerciseVolume(valid)
    if (volume > bests.maxVolume) {
        records += NewRecord(RecordKind.MAX_VOLUME, volume, valid.last().id)
    }

    valid.groupBy { it.weight }.forEach { (weight, group) ->
        val top = group.maxBy { it.reps }
        if (top.reps > (bests.maxRepsAtWeight[weight] ?: 0)) {
            records += NewRecord(RecordKind.MAX_REPS_AT_WEIGHT, top.reps.toLong(), top.id, weight)
        }
    }
    return records
}
