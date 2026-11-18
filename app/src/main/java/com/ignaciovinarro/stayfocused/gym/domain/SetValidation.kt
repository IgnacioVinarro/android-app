package com.ignaciovinarro.stayfocused.gym.domain

enum class SetError { WEIGHT_REQUIRED, REPS_REQUIRED, DURATION_REQUIRED }

// Valida una serie según el tipo de registro del ejercicio (GY-01). Devuelve null si es válida.
// - WEIGHT_REPS: peso > 0 y repeticiones > 0 (peso corporal con lastre = peso > 0 igual).
// - REPS_ONLY: repeticiones > 0.
// - TIME: duración > 0.
fun validateSet(trackingType: TrackingType, set: SetLog): SetError? = when (trackingType) {
    TrackingType.WEIGHT_REPS -> when {
        set.weight <= 0 -> SetError.WEIGHT_REQUIRED
        set.reps <= 0 -> SetError.REPS_REQUIRED
        else -> null
    }
    TrackingType.REPS_ONLY -> if (set.reps <= 0) SetError.REPS_REQUIRED else null
    TrackingType.TIME -> if (set.durationSeconds <= 0) SetError.DURATION_REQUIRED else null
}
