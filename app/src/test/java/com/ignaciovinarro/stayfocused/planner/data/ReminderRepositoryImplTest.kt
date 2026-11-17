package com.ignaciovinarro.stayfocused.planner.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReminderRepositoryImplTest {

    private val dao = mockk<ReminderDao>(relaxed = true)
    private lateinit var repository: ReminderRepository

    @Before
    fun setUp() {
        repository = ReminderRepositoryImpl(dao)
    }

    private fun existente(id: Long, minutesBefore: Int) =
        ReminderEntity(id = id, ownerId = 5L, minutesBefore = minutesBefore)

    @Test
    fun `getByOwner delega en el DAO`() = runBlocking {
        val recordatorios = listOf(existente(1L, 10))
        coEvery { dao.getByOwner(REMINDER_OWNER_TASK, 5L) } returns recordatorios

        assertEquals(recordatorios, repository.getByOwner(REMINDER_OWNER_TASK, 5L))
    }

    @Test
    fun `replaceForOwner inserta los que faltan`() = runBlocking {
        coEvery { dao.getByOwner(REMINDER_OWNER_TASK, 5L) } returns emptyList()
        val insertados = mutableListOf<ReminderEntity>()
        coEvery { dao.insert(capture(insertados)) } returns 1L

        repository.replaceForOwner(REMINDER_OWNER_TASK, 5L, listOf(30, 0))

        assertEquals(listOf(0, 30), insertados.map { it.minutesBefore })
        assertEquals(setOf(REMINDER_OWNER_TASK), insertados.map { it.ownerType }.toSet())
        assertEquals(setOf(5L), insertados.map { it.ownerId }.toSet())
        assertEquals(setOf(REMINDER_KIND_NOTICE), insertados.map { it.kind }.toSet())
    }

    @Test
    fun `replaceForOwner borra logicamente los que sobran`() = runBlocking {
        coEvery { dao.getByOwner(REMINDER_OWNER_TASK, 5L) } returns listOf(existente(1L, 10), existente(2L, 30))

        repository.replaceForOwner(REMINDER_OWNER_TASK, 5L, listOf(30))

        coVerify { dao.softDelete(1L, any()) }
        coVerify(exactly = 0) { dao.softDelete(2L, any()) }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `replaceForOwner no toca los que ya estaban`() = runBlocking {
        coEvery { dao.getByOwner(REMINDER_OWNER_TASK, 5L) } returns listOf(existente(1L, 10))

        repository.replaceForOwner(REMINDER_OWNER_TASK, 5L, listOf(10))

        coVerify(exactly = 0) { dao.softDelete(any(), any()) }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `replaceForOwner con repetidos inserta una sola vez`() = runBlocking {
        coEvery { dao.getByOwner(REMINDER_OWNER_TASK, 5L) } returns emptyList()
        val insertados = mutableListOf<ReminderEntity>()
        coEvery { dao.insert(capture(insertados)) } returns 1L

        repository.replaceForOwner(REMINDER_OWNER_TASK, 5L, listOf(15, 15))

        assertEquals(1, insertados.size)
    }

    @Test
    fun `replaceForOwner con conjunto vacio borra todos`() = runBlocking {
        coEvery { dao.getByOwner(REMINDER_OWNER_TASK, 5L) } returns listOf(existente(1L, 0), existente(2L, 5))

        repository.replaceForOwner(REMINDER_OWNER_TASK, 5L, emptyList())

        coVerify { dao.softDelete(1L, any()) }
        coVerify { dao.softDelete(2L, any()) }
    }

    @Test
    fun `deleteByOwner marca borrados los recordatorios del dueno con updatedAt actual`() = runBlocking {
        val updatedAt = slot<Long>()
        coEvery { dao.softDeleteByOwner(REMINDER_OWNER_TASK, 5L, capture(updatedAt)) } returns Unit
        val antes = System.currentTimeMillis()

        repository.deleteByOwner(REMINDER_OWNER_TASK, 5L)

        assertTrue(updatedAt.captured >= antes)
    }

    @Test
    fun `replaceForOwner inserta con el kind pedido`() = runBlocking {
        coEvery { dao.getByOwner(REMINDER_OWNER_TASK, 5L) } returns emptyList()
        val insertados = mutableListOf<ReminderEntity>()
        coEvery { dao.insert(capture(insertados)) } returns 1L

        repository.replaceForOwner(REMINDER_OWNER_TASK, 5L, listOf(0, 15), REMINDER_KIND_ALARM)

        assertTrue(insertados.all { it.kind == REMINDER_KIND_ALARM })
    }

    @Test
    fun `replaceForOwner cambia el kind de los existentes que ya estaban`() = runBlocking {
        coEvery { dao.getByOwner(REMINDER_OWNER_TASK, 5L) } returns listOf(existente(1L, 10), existente(2L, 20))

        repository.replaceForOwner(REMINDER_OWNER_TASK, 5L, listOf(10, 20), REMINDER_KIND_ALARM)

        coVerify { dao.updateKind(1L, REMINDER_KIND_ALARM, any()) }
        coVerify { dao.updateKind(2L, REMINDER_KIND_ALARM, any()) }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun `replaceForOwner no toca el kind si no cambio`() = runBlocking {
        coEvery { dao.getByOwner(REMINDER_OWNER_TASK, 5L) } returns listOf(existente(1L, 10))

        repository.replaceForOwner(REMINDER_OWNER_TASK, 5L, listOf(10))

        coVerify(exactly = 0) { dao.updateKind(any(), any(), any()) }
    }

    @Test
    fun `replaceForOwner no cambia el kind de los que se borran`() = runBlocking {
        coEvery { dao.getByOwner(REMINDER_OWNER_TASK, 5L) } returns listOf(existente(1L, 10), existente(2L, 20))

        repository.replaceForOwner(REMINDER_OWNER_TASK, 5L, listOf(20), REMINDER_KIND_ALARM)

        coVerify { dao.softDelete(1L, any()) }
        coVerify(exactly = 0) { dao.updateKind(1L, any(), any()) }
        coVerify { dao.updateKind(2L, REMINDER_KIND_ALARM, any()) }
    }
}
