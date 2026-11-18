package com.ignaciovinarro.stayfocused.finance.data

import com.ignaciovinarro.stayfocused.finance.domain.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCategoriesTest {

    private val categorias = defaultCategories { "texto-$it" }

    @Test
    fun `hay categorias de gasto y de ingreso`() {
        assertTrue(categorias.any { it.type == CategoryType.EXPENSE.name })
        assertTrue(categorias.any { it.type == CategoryType.INCOME.name })
    }

    @Test
    fun `todas se marcan como predefinidas y usan el nombre traducido`() {
        assertTrue(categorias.all { it.isDefault })
        assertEquals(DEFAULT_CATEGORY_SPECS.map { "texto-${it.nameRes}" }, categorias.map { it.name })
    }

    @Test
    fun `los uuid son distintos`() {
        assertEquals(categorias.size, categorias.map { it.uuid }.toSet().size)
    }

    @Test
    fun `los nombres no se repiten dentro del mismo tipo`() {
        val porTipo = categorias.groupBy { it.type }
        porTipo.values.forEach { assertEquals(it.size, it.map { c -> c.name }.toSet().size) }
    }
}
