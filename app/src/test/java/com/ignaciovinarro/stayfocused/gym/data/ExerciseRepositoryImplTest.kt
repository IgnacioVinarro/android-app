package com.ignaciovinarro.stayfocused.gym.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExerciseRepositoryImplTest {

    private val dao = mockk<ExerciseDao>(relaxed = true)
    private lateinit var repository: ExerciseRepository

    private val sample = ExerciseEntity(name = "Sentadilla", muscleGroup = "QUADS", equipment = "BARBELL", trackingType = "WEIGHT_REPS")

    @Before
    fun setUp() {
        repository = ExerciseRepositoryImpl(dao)
    }

    @Test
    fun `observeAll delega en el DAO`() = runBlocking {
        every { dao.observeAll() } returns flowOf(listOf(sample))
        assertEquals(listOf(sample), repository.observeAll().first())
    }

    @Test
    fun `create devuelve el id`() = runBlocking {
        coEvery { dao.insert(sample) } returns 5L
        assertEquals(5L, repository.create(sample))
    }

    @Test
    fun `update refresca updatedAt`() = runBlocking {
        val viejo = sample.copy(updatedAt = 1L)
        repository.update(viejo)
        coVerify { dao.update(match { it.name == "Sentadilla" && it.updatedAt > 1L }) }
    }

    @Test
    fun `delete es borrado logico`() = runBlocking {
        repository.delete(4L)
        coVerify { dao.softDelete(4L, any()) }
    }

    @Test
    fun `seedDefaults siembra solo si la tabla nunca tuvo filas`() = runBlocking {
        coEvery { dao.countAllRows() } returns 0
        repository.seedDefaults(listOf(sample))
        coVerify(exactly = 1) { dao.insertAll(listOf(sample)) }
    }

    @Test
    fun `seedDefaults no hace nada si ya hubo filas`() = runBlocking {
        coEvery { dao.countAllRows() } returns 3
        repository.seedDefaults(listOf(sample))
        coVerify(exactly = 0) { dao.insertAll(any()) }
    }
}
