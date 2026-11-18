package com.ignaciovinarro.stayfocused.core.ui.navigation

import com.ignaciovinarro.stayfocused.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Navegación: Dieta es una pestaña dentro de Gimnasio (ticket 0023) y Tiempo de pantalla cierra la barra inferior (0025).
class GymDietNavigationTest {

    @Test
    fun `la barra inferior tiene cinco destinos en este orden, con Tiempo de pantalla al final`() {
        assertEquals(
            listOf("today", "finance", "gym", "meditation", "screentime"),
            StayFocusedDestination.entries.map { it.route }
        )
    }

    @Test
    fun `Dieta ya no es un destino de la barra inferior`() {
        assertFalse(StayFocusedDestination.entries.any { it.route == "nutrition" })
    }

    // Ticket 0025: 5 es el maximo que admite la barra de navegacion de Material 3.
    @Test
    fun `la barra no pasa del maximo de cinco destinos`() {
        assertTrue(StayFocusedDestination.entries.size <= 5)
    }

    @Test
    fun `Tiempo de pantalla es el ultimo destino con su etiqueta`() {
        val ultimo = StayFocusedDestination.entries.last()

        assertEquals(StayFocusedDestination.SCREEN_TIME, ultimo)
        assertEquals(R.string.nav_screentime, ultimo.labelRes)
    }

    @Test
    fun `Ajustes no esta en la barra inferior, es un destino secundario`() {
        assertFalse(StayFocusedDestination.entries.any { it.route == ROUTE_SETTINGS })
    }

    @Test
    fun `la ruta de Ajustes no choca con la de ningun destino de la barra`() {
        assertEquals("settings", ROUTE_SETTINGS)
        assertTrue(StayFocusedDestination.entries.map { it.route }.none { it == ROUTE_SETTINGS })
    }

    @Test
    fun `las pestanas de Gimnasio son Gimnasio y luego Dieta`() {
        assertEquals(listOf(GymDietTab.GYM, GymDietTab.DIET), GymDietTab.entries)
        assertEquals(listOf(R.string.nav_gym, R.string.nav_nutrition), GymDietTab.entries.map { it.labelRes })
    }

    @Test
    fun `la pestana por defecto es Gimnasio y es la primera`() {
        assertEquals(GymDietTab.GYM, GymDietTab.DEFAULT)
        assertEquals(0, GymDietTab.DEFAULT.ordinal)
    }
}
