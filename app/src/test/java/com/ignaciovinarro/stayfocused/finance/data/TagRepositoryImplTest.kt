package com.ignaciovinarro.stayfocused.finance.data

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

class TagRepositoryImplTest {

    private val tagDao = mockk<TagDao>(relaxed = true)
    private val linkDao = mockk<TransactionTagDao>(relaxed = true)
    private lateinit var repository: TagRepository

    @Before
    fun setUp() {
        repository = TagRepositoryImpl(tagDao, linkDao)
    }

    @Test
    fun `observeAll y observeLinks delegan en los DAOs`() = runBlocking {
        val tags = listOf(TagEntity(name = "a"))
        val links = listOf(TransactionTagEntity(transactionId = 1, tagId = 2))
        every { tagDao.observeAll() } returns flowOf(tags)
        every { linkDao.observeActive() } returns flowOf(links)

        assertEquals(tags, repository.observeAll().first())
        assertEquals(links, repository.observeLinks().first())
    }

    @Test
    fun `create recorta el nombre`() = runBlocking {
        coEvery { tagDao.insert(any()) } returns 4L

        val id = repository.create("  viaje ")

        assertEquals(4L, id)
        coVerify { tagDao.insert(match { it.name == "viaje" }) }
    }

    @Test
    fun `rename cambia el nombre y actualiza updatedAt`() = runBlocking {
        coEvery { tagDao.getById(1L) } returns TagEntity(id = 1, name = "viejo", updatedAt = 1L)

        repository.rename(1L, " nuevo ")

        coVerify { tagDao.update(match { it.id == 1L && it.name == "nuevo" && it.updatedAt > 1L }) }
    }

    @Test
    fun `rename de una etiqueta inexistente no hace nada`() = runBlocking {
        coEvery { tagDao.getById(1L) } returns null

        repository.rename(1L, "x")

        coVerify(exactly = 0) { tagDao.update(any()) }
    }

    @Test
    fun `delete delega el borrado logico en el DAO`() = runBlocking {
        repository.delete(6L)

        coVerify { tagDao.softDelete(6L, any()) }
    }
}
