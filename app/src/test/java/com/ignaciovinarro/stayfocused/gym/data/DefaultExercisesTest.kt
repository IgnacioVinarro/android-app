package com.ignaciovinarro.stayfocused.gym.data

import com.ignaciovinarro.stayfocused.gym.domain.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultExercisesTest {

    private val ejercicios = defaultExercises { "texto-$it" }

    @Test
    fun `todos se marcan como de la biblioteca y usan el nombre traducido`() {
        assertTrue(ejercicios.none { it.isCustom })
        assertEquals(DEFAULT_EXERCISE_SPECS.map { "texto-${it.nameRes}" }, ejercicios.map { it.name })
    }

    @Test
    fun `los uuid y los nombres no se repiten`() {
        assertEquals(ejercicios.size, ejercicios.map { it.uuid }.toSet().size)
        assertEquals(ejercicios.size, ejercicios.map { it.name }.toSet().size)
    }

    @Test
    fun `hay ejercicios de los tres tipos de registro`() {
        TrackingType.entries.forEach { tipo ->
            assertTrue(ejercicios.any { it.trackingType == tipo.name })
        }
    }

    @Test
    fun `el descanso nunca es negativo`() {
        assertFalse(ejercicios.any { it.restSeconds < 0 })
    }
}
