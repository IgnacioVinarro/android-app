package com.ignaciovinarro.stayfocused.planner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtasksTest {

    private fun borrador(key: Long, title: String, id: Long? = null) = SubtaskDraft(key = key, title = title, id = id)

    @Test
    fun `recorta los espacios del titulo`() {
        val result = normalizeSubtasks(listOf(borrador(1, "  Comprar pan  ")))

        assertEquals(listOf("Comprar pan"), result.map { it.title })
    }

    @Test
    fun `descarta las subtareas vacias o solo con espacios`() {
        val result = normalizeSubtasks(listOf(borrador(1, ""), borrador(2, "   "), borrador(3, "Leer")))

        assertEquals(listOf("Leer"), result.map { it.title })
    }

    @Test
    fun `mantiene el orden de la lista`() {
        val result = normalizeSubtasks(listOf(borrador(1, "C"), borrador(2, "A"), borrador(3, "B")))

        assertEquals(listOf("C", "A", "B"), result.map { it.title })
    }

    @Test
    fun `conserva el id y la clave de las que ya existian`() {
        val result = normalizeSubtasks(listOf(borrador(key = 7, title = " Ya existia ", id = 42)))

        assertEquals(42L, result.single().id)
        assertEquals(7L, result.single().key)
    }

    @Test
    fun `una lista vacia queda vacia`() {
        assertTrue(normalizeSubtasks(emptyList()).isEmpty())
    }
}
