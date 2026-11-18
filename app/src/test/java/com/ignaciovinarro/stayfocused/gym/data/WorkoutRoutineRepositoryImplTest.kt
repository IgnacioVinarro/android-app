package com.ignaciovinarro.stayfocused.gym.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WorkoutRoutineRepositoryImplTest {

    private val dao = mockk<WorkoutRoutineDao>(relaxed = true)
    private lateinit var repository: WorkoutRoutineRepository

    private val routine = WorkoutRoutineEntity(name = "Push")
    private val items = listOf(
        RoutineExerciseEntity(routineId = 0, exerciseId = 1, position = 0),
        RoutineExerciseEntity(routineId = 0, exerciseId = 2, position = 1)
    )

    @Before
    fun setUp() {
        repository = WorkoutRoutineRepositoryImpl(dao)
    }

    @Test
    fun `create inserta la rutina y despues reemplaza sus ejercicios`() = runBlocking {
        coEvery { dao.insert(routine) } returns 9L

        val id = repository.create(routine, items)

        assertEquals(9L, id)
        coVerifyOrder {
            dao.insert(routine)
            dao.replaceItems(9L, items, any())
        }
    }

    @Test
    fun `update refresca updatedAt y reemplaza los ejercicios`() = runBlocking {
        val existente = routine.copy(id = 4, updatedAt = 1L)

        repository.update(existente, items)

        coVerify { dao.update(match { it.id == 4L && it.updatedAt > 1L }) }
        coVerify { dao.replaceItems(4L, items, any()) }
    }

    @Test
    fun `delete es borrado logico`() = runBlocking {
        repository.delete(4L)
        coVerify { dao.softDelete(4L, any()) }
    }
}
