package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.TransactionTagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagsTest {

    // --- validateTagName ---

    @Test
    fun `nombre valido`() {
        assertNull(validateTagName("vacaciones", listOf("cumple")))
    }

    @Test
    fun `nombre vacio o solo espacios`() {
        assertEquals(TagNameError.EMPTY, validateTagName("", emptyList()))
        assertEquals(TagNameError.EMPTY, validateTagName("   ", emptyList()))
    }

    @Test
    fun `nombre demasiado largo`() {
        assertEquals(TagNameError.TOO_LONG, validateTagName("a".repeat(TAG_NAME_MAX_LENGTH + 1), emptyList()))
        assertNull(validateTagName("a".repeat(TAG_NAME_MAX_LENGTH), emptyList()))
    }

    @Test
    fun `nombre repetido ignorando mayusculas y espacios`() {
        assertEquals(TagNameError.DUPLICATE, validateTagName("  Vacaciones ", listOf("vacaciones")))
    }

    @Test
    fun `normalizar recorta espacios`() {
        assertEquals("viaje", normalizeTagName("  viaje "))
    }

    // --- planTagLinkChanges ---

    private fun link(tagId: Long, status: Int = 0) =
        TransactionTagEntity(id = tagId, transactionId = 1, tagId = tagId, status = status)

    @Test
    fun `sin uniones previas inserta todas`() {
        val cambios = planTagLinkChanges(emptyList(), 1, setOf(3, 1), now = 10)
        assertEquals(listOf(1L, 3L), cambios.toInsert.map { it.tagId })
        assertTrue(cambios.toUpdate.isEmpty())
        assertTrue(cambios.toInsert.all { it.transactionId == 1L && it.status == 0 })
    }

    @Test
    fun `quita las que ya no se quieren`() {
        val cambios = planTagLinkChanges(listOf(link(1), link(2)), 1, setOf(1), now = 10)
        assertTrue(cambios.toInsert.isEmpty())
        assertEquals(1, cambios.toUpdate.size)
        assertEquals(2L, cambios.toUpdate[0].tagId)
        assertEquals(-1, cambios.toUpdate[0].status)
        assertEquals(10L, cambios.toUpdate[0].updatedAt)
    }

    @Test
    fun `reactiva una union borrada en vez de insertar otra`() {
        val cambios = planTagLinkChanges(listOf(link(2, status = -1)), 1, setOf(2), now = 10)
        assertTrue(cambios.toInsert.isEmpty())
        assertEquals(0, cambios.toUpdate[0].status)
    }

    @Test
    fun `sin cambios no hace nada`() {
        val cambios = planTagLinkChanges(listOf(link(1), link(2, status = -1)), 1, setOf(1), now = 10)
        assertTrue(cambios.toInsert.isEmpty())
        assertTrue(cambios.toUpdate.isEmpty())
    }
}
