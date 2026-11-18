package com.ignaciovinarro.stayfocused.core.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLaunchRequestTest {

    @Test
    fun `un nombre conocido se traduce al pedido`() {
        assertEquals(AppLaunchRequest.NEW_TASK, AppLaunchRequest.fromName("NEW_TASK"))
    }

    @Test
    fun `un nombre desconocido es sin pedido`() {
        assertNull(AppLaunchRequest.fromName("BORRAR_TODO"))
    }

    @Test
    fun `un nombre ausente es sin pedido`() {
        assertNull(AppLaunchRequest.fromName(null))
    }

    @Test
    fun `solo el nombre exacto vale, sin distinguir mayusculas por accidente`() {
        assertNull(AppLaunchRequest.fromName("new_task"))
    }
}
