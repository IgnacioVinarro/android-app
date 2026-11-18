package com.ignaciovinarro.stayfocused.gym.data

import com.ignaciovinarro.stayfocused.gym.domain.PersonalBests
import com.ignaciovinarro.stayfocused.gym.domain.RecordKind
import com.ignaciovinarro.stayfocused.gym.domain.SetLog
import com.ignaciovinarro.stayfocused.gym.domain.SetType

// Puente entre las filas de Room y los tipos puros del dominio.

// Un tipo guardado que ya no existe (dato viejo o corrupto) se trata como serie normal.
fun SessionSetEntity.toSetLog(): SetLog = SetLog(
    id = id,
    weight = weight,
    reps = reps,
    durationSeconds = durationSeconds,
    type = runCatching { SetType.valueOf(type) }.getOrDefault(SetType.NORMAL),
    completed = completed
)

// Mejores marcas previas a partir de los récords guardados del ejercicio.
fun List<PersonalRecordEntity>.toBests(): PersonalBests {
    fun best(kind: RecordKind): Long =
        filter { it.kind == kind.name }.maxOfOrNull { it.value } ?: 0L

    val repsAtWeight = filter { it.kind == RecordKind.MAX_REPS_AT_WEIGHT.name }
        .groupBy({ it.weight }, { it.value.toInt() })
        .mapValues { (_, reps) -> reps.max() }

    return PersonalBests(
        maxWeight = best(RecordKind.MAX_WEIGHT),
        maxVolume = best(RecordKind.MAX_VOLUME),
        maxOneRm = best(RecordKind.ESTIMATED_1RM),
        maxRepsAtWeight = repsAtWeight
    )
}
