package com.ignaciovinarro.stayfocused.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import com.ignaciovinarro.stayfocused.finance.data.DEFAULT_CATEGORY_SPECS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryIconKeyTest {

    @Test
    fun `toda categoria predefinida tiene un icono propio`() {
        val known = CategoryIconKey.entries.map { it.key }.toSet()
        DEFAULT_CATEGORY_SPECS.forEach { assertTrue("Falta ícono ${it.iconKey}", it.iconKey in known) }
    }

    @Test
    fun `las claves de iconos de categorias no se repiten ni chocan con las de tareas`() {
        val categoryKeys = CategoryIconKey.entries.map { it.key }
        assertEquals(categoryKeys.size, categoryKeys.toSet().size)
        assertTrue(categoryKeys.intersect(TaskIconKey.entries.map { it.key }.toSet()).isEmpty())
    }

    @Test
    fun `iconForKey resuelve claves de categorias y cae a un circulo si no conoce la clave`() {
        assertEquals(CategoryIconKey.FOOD.icon, iconForKey("category_food"))
        assertEquals(TaskIconKey.WORK.icon, iconForKey("task_work"))
        assertEquals(Icons.Filled.Circle, iconForKey("desconocida"))
        assertNotEquals(Icons.Filled.Circle, iconForKey("category_salary"))
    }
}
