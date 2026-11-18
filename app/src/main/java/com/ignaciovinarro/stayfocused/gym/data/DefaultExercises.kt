package com.ignaciovinarro.stayfocused.gym.data

import android.content.Context
import androidx.annotation.StringRes
import com.ignaciovinarro.stayfocused.R
import com.ignaciovinarro.stayfocused.gym.domain.Equipment
import com.ignaciovinarro.stayfocused.gym.domain.MuscleGroup
import com.ignaciovinarro.stayfocused.gym.domain.TrackingType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Biblioteca de ejercicios precargada (GY-01): lista propia y chica. Los nombres viven en strings.xml.
data class DefaultExerciseSpec(
    @StringRes val nameRes: Int,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val trackingType: TrackingType,
    val restSeconds: Int
)

val DEFAULT_EXERCISE_SPECS = listOf(
    DefaultExerciseSpec(R.string.gym_exercise_bench_press, MuscleGroup.CHEST, Equipment.BARBELL, TrackingType.WEIGHT_REPS, 120),
    DefaultExerciseSpec(R.string.gym_exercise_incline_dumbbell_press, MuscleGroup.CHEST, Equipment.DUMBBELL, TrackingType.WEIGHT_REPS, 90),
    DefaultExerciseSpec(R.string.gym_exercise_chest_fly, MuscleGroup.CHEST, Equipment.DUMBBELL, TrackingType.WEIGHT_REPS, 75),
    DefaultExerciseSpec(R.string.gym_exercise_push_up, MuscleGroup.CHEST, Equipment.BODYWEIGHT, TrackingType.REPS_ONLY, 60),
    DefaultExerciseSpec(R.string.gym_exercise_dip, MuscleGroup.TRICEPS, Equipment.BODYWEIGHT, TrackingType.REPS_ONLY, 90),
    DefaultExerciseSpec(R.string.gym_exercise_pull_up, MuscleGroup.BACK, Equipment.BODYWEIGHT, TrackingType.REPS_ONLY, 90),
    DefaultExerciseSpec(R.string.gym_exercise_lat_pulldown, MuscleGroup.BACK, Equipment.CABLE, TrackingType.WEIGHT_REPS, 90),
    DefaultExerciseSpec(R.string.gym_exercise_barbell_row, MuscleGroup.BACK, Equipment.BARBELL, TrackingType.WEIGHT_REPS, 120),
    DefaultExerciseSpec(R.string.gym_exercise_dumbbell_row, MuscleGroup.BACK, Equipment.DUMBBELL, TrackingType.WEIGHT_REPS, 90),
    DefaultExerciseSpec(R.string.gym_exercise_seated_cable_row, MuscleGroup.BACK, Equipment.CABLE, TrackingType.WEIGHT_REPS, 90),
    DefaultExerciseSpec(R.string.gym_exercise_deadlift, MuscleGroup.BACK, Equipment.BARBELL, TrackingType.WEIGHT_REPS, 180),
    DefaultExerciseSpec(R.string.gym_exercise_overhead_press, MuscleGroup.SHOULDERS, Equipment.BARBELL, TrackingType.WEIGHT_REPS, 120),
    DefaultExerciseSpec(R.string.gym_exercise_lateral_raise, MuscleGroup.SHOULDERS, Equipment.DUMBBELL, TrackingType.WEIGHT_REPS, 60),
    DefaultExerciseSpec(R.string.gym_exercise_face_pull, MuscleGroup.SHOULDERS, Equipment.CABLE, TrackingType.WEIGHT_REPS, 60),
    DefaultExerciseSpec(R.string.gym_exercise_barbell_curl, MuscleGroup.BICEPS, Equipment.BARBELL, TrackingType.WEIGHT_REPS, 75),
    DefaultExerciseSpec(R.string.gym_exercise_hammer_curl, MuscleGroup.BICEPS, Equipment.DUMBBELL, TrackingType.WEIGHT_REPS, 60),
    DefaultExerciseSpec(R.string.gym_exercise_triceps_pushdown, MuscleGroup.TRICEPS, Equipment.CABLE, TrackingType.WEIGHT_REPS, 60),
    DefaultExerciseSpec(R.string.gym_exercise_skull_crusher, MuscleGroup.TRICEPS, Equipment.BARBELL, TrackingType.WEIGHT_REPS, 75),
    DefaultExerciseSpec(R.string.gym_exercise_wrist_curl, MuscleGroup.FOREARMS, Equipment.DUMBBELL, TrackingType.WEIGHT_REPS, 45),
    DefaultExerciseSpec(R.string.gym_exercise_squat, MuscleGroup.QUADS, Equipment.BARBELL, TrackingType.WEIGHT_REPS, 150),
    DefaultExerciseSpec(R.string.gym_exercise_leg_press, MuscleGroup.QUADS, Equipment.MACHINE, TrackingType.WEIGHT_REPS, 120),
    DefaultExerciseSpec(R.string.gym_exercise_leg_extension, MuscleGroup.QUADS, Equipment.MACHINE, TrackingType.WEIGHT_REPS, 75),
    DefaultExerciseSpec(R.string.gym_exercise_lunge, MuscleGroup.QUADS, Equipment.DUMBBELL, TrackingType.WEIGHT_REPS, 90),
    DefaultExerciseSpec(R.string.gym_exercise_romanian_deadlift, MuscleGroup.HAMSTRINGS, Equipment.BARBELL, TrackingType.WEIGHT_REPS, 120),
    DefaultExerciseSpec(R.string.gym_exercise_leg_curl, MuscleGroup.HAMSTRINGS, Equipment.MACHINE, TrackingType.WEIGHT_REPS, 75),
    DefaultExerciseSpec(R.string.gym_exercise_hip_thrust, MuscleGroup.GLUTES, Equipment.BARBELL, TrackingType.WEIGHT_REPS, 120),
    DefaultExerciseSpec(R.string.gym_exercise_standing_calf_raise, MuscleGroup.CALVES, Equipment.MACHINE, TrackingType.WEIGHT_REPS, 60),
    DefaultExerciseSpec(R.string.gym_exercise_crunch, MuscleGroup.ABS, Equipment.BODYWEIGHT, TrackingType.REPS_ONLY, 45),
    DefaultExerciseSpec(R.string.gym_exercise_hanging_leg_raise, MuscleGroup.ABS, Equipment.BODYWEIGHT, TrackingType.REPS_ONLY, 60),
    DefaultExerciseSpec(R.string.gym_exercise_plank, MuscleGroup.ABS, Equipment.BODYWEIGHT, TrackingType.TIME, 45),
    DefaultExerciseSpec(R.string.gym_exercise_kettlebell_swing, MuscleGroup.FULL_BODY, Equipment.KETTLEBELL, TrackingType.WEIGHT_REPS, 75),
    DefaultExerciseSpec(R.string.gym_exercise_burpee, MuscleGroup.FULL_BODY, Equipment.BODYWEIGHT, TrackingType.REPS_ONLY, 60),
    DefaultExerciseSpec(R.string.gym_exercise_running, MuscleGroup.CARDIO, Equipment.OTHER, TrackingType.TIME, 0),
    DefaultExerciseSpec(R.string.gym_exercise_jump_rope, MuscleGroup.CARDIO, Equipment.OTHER, TrackingType.TIME, 30),
    DefaultExerciseSpec(R.string.gym_exercise_rowing_machine, MuscleGroup.CARDIO, Equipment.MACHINE, TrackingType.TIME, 0)
)

// Arma las filas a sembrar. `getString` se inyecta para poder probarlo sin Android.
fun defaultExercises(getString: (Int) -> String): List<ExerciseEntity> =
    DEFAULT_EXERCISE_SPECS.map {
        ExerciseEntity(
            name = getString(it.nameRes),
            muscleGroup = it.muscleGroup.name,
            equipment = it.equipment.name,
            trackingType = it.trackingType.name,
            isCustom = false,
            restSeconds = it.restSeconds
        )
    }

// Siembra la biblioteca la primera vez que corre la app (la tabla nunca tuvo filas).
@Singleton
class GymSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseRepository: ExerciseRepository
) {
    suspend fun seedIfNeeded() {
        exerciseRepository.seedDefaults(defaultExercises { context.getString(it) })
    }
}
