package com.ignaciovinarro.stayfocused.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ignaciovinarro.stayfocused.gym.data.ExerciseDao
import com.ignaciovinarro.stayfocused.gym.data.ExerciseEntity
import com.ignaciovinarro.stayfocused.gym.data.RoutineExerciseEntity
import com.ignaciovinarro.stayfocused.gym.data.SessionDao
import com.ignaciovinarro.stayfocused.gym.data.SessionSetEntity
import com.ignaciovinarro.stayfocused.gym.data.WorkoutRoutineDao
import com.ignaciovinarro.stayfocused.gym.data.WorkoutRoutineEntity
import com.ignaciovinarro.stayfocused.gym.data.WorkoutSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GymDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var routineDao: WorkoutRoutineDao
    private lateinit var sessionDao: SessionDao

    @Before
    fun crearBaseEnMemoria() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        exerciseDao = database.exerciseDao()
        routineDao = database.workoutRoutineDao()
        sessionDao = database.sessionDao()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    private fun ejercicio(nombre: String) = ExerciseEntity(
        name = nombre, muscleGroup = "CHEST", equipment = "BARBELL", trackingType = "WEIGHT_REPS"
    )

    private fun serie(sessionId: Long, exerciseId: Long, position: Int, weight: Long = 50_000, reps: Int = 10) =
        SessionSetEntity(
            sessionId = sessionId, exerciseId = exerciseId, position = position,
            weight = weight, reps = reps, type = "NORMAL", completed = true
        )

    @Test
    fun ejercicios_seObservanOrdenadosYElBorradoEsLogico() = runBlocking {
        exerciseDao.insert(ejercicio("Sentadilla"))
        val bancaId = exerciseDao.insert(ejercicio("banca"))

        assertThat(exerciseDao.observeAll().first().map { it.name }).containsExactly("banca", "Sentadilla").inOrder()

        exerciseDao.softDelete(bancaId, 1L)

        assertThat(exerciseDao.observeAll().first().map { it.name }).containsExactly("Sentadilla")
        assertThat(exerciseDao.getById(bancaId)).isNull()
        assertThat(exerciseDao.countAllRows()).isEqualTo(2)
    }

    @Test
    fun rutina_reemplazaEjerciciosYLosOrdenaPorPosicion() = runBlocking {
        val rutinaId = routineDao.insert(WorkoutRoutineEntity(name = "Push"))
        val a = exerciseDao.insert(ejercicio("A"))
        val b = exerciseDao.insert(ejercicio("B"))

        routineDao.replaceItems(
            rutinaId,
            listOf(
                RoutineExerciseEntity(routineId = rutinaId, exerciseId = b, position = 9),
                RoutineExerciseEntity(routineId = rutinaId, exerciseId = a, position = 9, supersetGroup = 1)
            ),
            now = 10L
        )
        assertThat(routineDao.getItems(rutinaId).map { it.exerciseId }).containsExactly(b, a).inOrder()
        assertThat(routineDao.getItems(rutinaId).map { it.position }).containsExactly(0, 1).inOrder()

        routineDao.replaceItems(rutinaId, listOf(RoutineExerciseEntity(routineId = rutinaId, exerciseId = a, position = 0)), 20L)
        assertThat(routineDao.getItems(rutinaId)).hasSize(1)
    }

    @Test
    fun rutina_borrarEsLogicoYBorraSusEjercicios() = runBlocking {
        val rutinaId = routineDao.insert(WorkoutRoutineEntity(name = "Pull"))
        val a = exerciseDao.insert(ejercicio("A"))
        routineDao.replaceItems(rutinaId, listOf(RoutineExerciseEntity(routineId = rutinaId, exerciseId = a, position = 0)), 1L)

        routineDao.softDelete(rutinaId, 5L)

        assertThat(routineDao.getById(rutinaId)).isNull()
        assertThat(routineDao.getItems(rutinaId)).isEmpty()
        assertThat(routineDao.observeAll().first()).isEmpty()
    }

    @Test
    fun sesion_enCursoEHistorial() = runBlocking {
        val enCurso = sessionDao.insert(WorkoutSessionEntity(date = 19000, startedAt = 1, state = "IN_PROGRESS"))
        val terminada = sessionDao.insert(WorkoutSessionEntity(date = 18999, startedAt = 0, endedAt = 5, state = "COMPLETED"))
        sessionDao.insert(WorkoutSessionEntity(date = 18998, startedAt = 0, endedAt = 5, state = "ABANDONED"))

        assertThat(sessionDao.observeInProgress().first()?.id).isEqualTo(enCurso)
        assertThat(sessionDao.observeHistory().first().map { it.id }).containsExactly(terminada)
        assertThat(sessionDao.observeBetween(18999, 19000).first()).hasSize(2)
    }

    @Test
    fun ultimaSesion_devuelveLasSeriesDeLaSesionTerminadaMasReciente() = runBlocking {
        val ejercicioId = exerciseDao.insert(ejercicio("Banca"))
        val vieja = sessionDao.insert(WorkoutSessionEntity(date = 1, startedAt = 100, endedAt = 200, state = "COMPLETED"))
        val reciente = sessionDao.insert(WorkoutSessionEntity(date = 2, startedAt = 300, endedAt = 400, state = "COMPLETED"))
        val abandonada = sessionDao.insert(WorkoutSessionEntity(date = 3, startedAt = 500, endedAt = 600, state = "ABANDONED"))
        val actual = sessionDao.insert(WorkoutSessionEntity(date = 4, startedAt = 700, state = "IN_PROGRESS"))
        sessionDao.insertSet(serie(vieja, ejercicioId, 0, weight = 40_000))
        sessionDao.insertSet(serie(reciente, ejercicioId, 0, weight = 60_000))
        sessionDao.insertSet(serie(reciente, ejercicioId, 1, weight = 65_000))
        sessionDao.insertSet(serie(abandonada, ejercicioId, 0, weight = 99_000))
        sessionDao.insertSet(serie(actual, ejercicioId, 0, weight = 70_000))

        val ultimas = sessionDao.getLastSets(ejercicioId, excludeSessionId = actual)

        assertThat(ultimas.map { it.weight }).containsExactly(60_000L, 65_000L).inOrder()
        assertThat(sessionDao.getLastSets(ejercicioId + 1, actual)).isEmpty()
    }

    @Test
    fun notas_unaPorSesionYEjercicioYSeReemplaza() = runBlocking {
        val sesion = sessionDao.insert(WorkoutSessionEntity(date = 1, startedAt = 1, state = "IN_PROGRESS"))
        val ejercicioId = exerciseDao.insert(ejercicio("Banca"))

        sessionDao.upsertNote(
            com.ignaciovinarro.stayfocused.gym.data.SessionExerciseNoteEntity(
                sessionId = sesion, exerciseId = ejercicioId, note = "primera"
            )
        )
        val existente = sessionDao.getNote(sesion, ejercicioId)!!
        sessionDao.upsertNote(existente.copy(note = "segunda"))

        assertThat(sessionDao.observeNotes(sesion).first().map { it.note }).containsExactly("segunda")
        assertThat(sessionDao.getLastNote(ejercicioId)?.note).isEqualTo("segunda")
    }

    @Test
    fun borrarSesion_borraSeriesNotasYRecords() = runBlocking {
        val ejercicioId = exerciseDao.insert(ejercicio("Banca"))
        val sesion = sessionDao.insert(WorkoutSessionEntity(date = 1, startedAt = 1, endedAt = 2, state = "COMPLETED"))
        val serieId = sessionDao.insertSet(serie(sesion, ejercicioId, 0))
        sessionDao.insertRecords(
            listOf(
                com.ignaciovinarro.stayfocused.gym.data.PersonalRecordEntity(
                    exerciseId = ejercicioId, kind = "MAX_WEIGHT", value = 50_000, sessionSetId = serieId, achievedAt = 2
                )
            )
        )

        sessionDao.softDelete(sesion, 9L)

        assertThat(sessionDao.getById(sesion)).isNull()
        assertThat(sessionDao.getSets(sesion)).isEmpty()
        assertThat(sessionDao.getRecords(ejercicioId)).isEmpty()
        assertThat(sessionDao.observeHistory().first()).isEmpty()
    }
}
