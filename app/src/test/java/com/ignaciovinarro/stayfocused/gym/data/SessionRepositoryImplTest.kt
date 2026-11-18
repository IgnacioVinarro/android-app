package com.ignaciovinarro.stayfocused.gym.data

import com.ignaciovinarro.stayfocused.gym.domain.RecordKind
import com.ignaciovinarro.stayfocused.gym.domain.SessionState
import com.ignaciovinarro.stayfocused.gym.domain.SetType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionRepositoryImplTest {

    private val dao = mockk<SessionDao>(relaxed = true)
    private lateinit var repository: SessionRepository

    @Before
    fun setUp() {
        repository = SessionRepositoryImpl(dao)
    }

    private fun session(state: SessionState = SessionState.IN_PROGRESS) =
        WorkoutSessionEntity(id = 1, date = 19000, startedAt = 1_000, state = state.name)

    private fun set(id: Long, exerciseId: Long, weight: Long, reps: Int, type: SetType = SetType.NORMAL) =
        SessionSetEntity(
            id = id, sessionId = 1, exerciseId = exerciseId, position = id.toInt(),
            weight = weight, reps = reps, type = type.name, completed = true
        )

    @Test
    fun `start crea una sesion en curso`() = runBlocking {
        val slot = slot<WorkoutSessionEntity>()
        coEvery { dao.insert(capture(slot)) } returns 3L

        val id = repository.start(routineId = 7L, date = 19000, startedAt = 5_000)

        assertEquals(3L, id)
        assertEquals(SessionState.IN_PROGRESS.name, slot.captured.state)
        assertEquals(7L, slot.captured.routineId)
        assertEquals(5_000L, slot.captured.startedAt)
        assertEquals(null, slot.captured.endedAt)
    }

    @Test
    fun `saveSet inserta si es nueva y actualiza si ya existe`() = runBlocking {
        coEvery { dao.insertSet(any()) } returns 11L

        assertEquals(11L, repository.saveSet(set(0, 1, 50_000, 10)))
        assertEquals(12L, repository.saveSet(set(12, 1, 50_000, 10)))

        coVerify(exactly = 1) { dao.insertSet(any()) }
        coVerify(exactly = 1) { dao.updateSet(match { it.id == 12L }) }
    }

    @Test
    fun `complete guarda los records y cierra la sesion`() = runBlocking {
        coEvery { dao.getById(1) } returns session()
        coEvery { dao.getSets(1) } returns listOf(set(1, exerciseId = 2, weight = 80_000, reps = 10))
        coEvery { dao.getRecords(2) } returns emptyList()
        val saved = slot<List<PersonalRecordEntity>>()
        coEvery { dao.insertRecords(capture(saved)) } returns Unit
        val updated = slot<WorkoutSessionEntity>()
        coEvery { dao.update(capture(updated)) } returns Unit

        val records = repository.complete(1, endedAt = 9_000)

        assertEquals(4, records.size)
        assertTrue(records.all { it.exerciseId == 2L })
        assertEquals(RecordKind.entries.toSet(), saved.captured.map { RecordKind.valueOf(it.kind) }.toSet())
        assertTrue(saved.captured.all { it.achievedAt == 9_000L && it.exerciseId == 2L })
        assertEquals(SessionState.COMPLETED.name, updated.captured.state)
        assertEquals(9_000L, updated.captured.endedAt)
    }

    @Test
    fun `complete compara contra los records de sesiones anteriores`() = runBlocking {
        coEvery { dao.getById(1) } returns session()
        coEvery { dao.getSets(1) } returns listOf(set(1, exerciseId = 2, weight = 80_000, reps = 10))
        coEvery { dao.getRecords(2) } returns listOf(
            PersonalRecordEntity(exerciseId = 2, kind = RecordKind.MAX_WEIGHT.name, value = 100_000, sessionSetId = 99, achievedAt = 1),
            PersonalRecordEntity(exerciseId = 2, kind = RecordKind.ESTIMATED_1RM.name, value = 200_000, sessionSetId = 99, achievedAt = 1),
            PersonalRecordEntity(exerciseId = 2, kind = RecordKind.MAX_VOLUME.name, value = 5_000_000, sessionSetId = 99, achievedAt = 1),
            PersonalRecordEntity(exerciseId = 2, kind = RecordKind.MAX_REPS_AT_WEIGHT.name, value = 12, weight = 80_000, sessionSetId = 99, achievedAt = 1)
        )

        val records = repository.complete(1, endedAt = 9_000)

        assertTrue(records.isEmpty())
        coVerify(exactly = 0) { dao.insertRecords(any()) }
        coVerify { dao.update(match { it.state == SessionState.COMPLETED.name }) }
    }

    @Test
    fun `complete evalua cada ejercicio por separado`() = runBlocking {
        coEvery { dao.getById(1) } returns session()
        coEvery { dao.getSets(1) } returns listOf(
            set(1, exerciseId = 2, weight = 80_000, reps = 10),
            set(2, exerciseId = 3, weight = 40_000, reps = 10)
        )
        coEvery { dao.getRecords(any()) } returns emptyList()

        val records = repository.complete(1, endedAt = 9_000)

        assertEquals(setOf(2L, 3L), records.map { it.exerciseId }.toSet())
    }

    @Test
    fun `complete no hace nada si la sesion no esta en curso`() = runBlocking {
        coEvery { dao.getById(1) } returns session(SessionState.COMPLETED)
        assertTrue(repository.complete(1, endedAt = 9_000).isEmpty())

        coEvery { dao.getById(2) } returns null
        assertTrue(repository.complete(2, endedAt = 9_000).isEmpty())

        coVerify(exactly = 0) { dao.update(any()) }
        coVerify(exactly = 0) { dao.insertRecords(any()) }
    }

    @Test
    fun `abandon conserva las series y no crea records`() = runBlocking {
        coEvery { dao.getById(1) } returns session()
        val updated = slot<WorkoutSessionEntity>()
        coEvery { dao.update(capture(updated)) } returns Unit

        repository.abandon(1, endedAt = 7_000)

        assertEquals(SessionState.ABANDONED.name, updated.captured.state)
        assertEquals(7_000L, updated.captured.endedAt)
        coVerify(exactly = 0) { dao.softDeleteSet(any(), any()) }
        coVerify(exactly = 0) { dao.insertRecords(any()) }
    }

    @Test
    fun `abandon ignora una sesion que ya termino`() = runBlocking {
        coEvery { dao.getById(1) } returns session(SessionState.COMPLETED)
        repository.abandon(1, endedAt = 7_000)
        coVerify(exactly = 0) { dao.update(any()) }
    }

    @Test
    fun `saveNote reutiliza la nota existente de la sesion`() = runBlocking {
        val existente = SessionExerciseNoteEntity(id = 5, sessionId = 1, exerciseId = 2, note = "vieja", updatedAt = 1)
        coEvery { dao.getNote(1, 2) } returns existente

        repository.saveNote(1, 2, "nueva")

        coVerify { dao.upsertNote(match { it.id == 5L && it.note == "nueva" && it.updatedAt > 1L }) }
    }

    @Test
    fun `saveNote crea la nota si no existia`() = runBlocking {
        coEvery { dao.getNote(1, 2) } returns null

        repository.saveNote(1, 2, "hola")

        coVerify { dao.upsertNote(match { it.id == 0L && it.sessionId == 1L && it.exerciseId == 2L && it.note == "hola" }) }
    }

    @Test
    fun `delete y deleteSet son borrado logico`() = runBlocking {
        repository.delete(1)
        repository.deleteSet(4)
        coVerify { dao.softDelete(1, any()) }
        coVerify { dao.softDeleteSet(4, any()) }
    }
}
