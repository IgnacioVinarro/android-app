package com.ignaciovinarro.stayfocused.gym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GymMathTest {

    @Test
    fun `volumen de una serie es peso por repeticiones`() {
        assertEquals(800_000L, setVolume(SetLog(weight = 80_000, reps = 10)))
    }

    @Test
    fun `el calentamiento y las series sin completar no cuentan`() {
        assertEquals(0L, setVolume(SetLog(weight = 20_000, reps = 10, type = SetType.WARMUP)))
        assertEquals(0L, setVolume(SetLog(weight = 80_000, reps = 10, completed = false)))
        assertFalse(SetLog(type = SetType.WARMUP).countsForStats())
        assertTrue(SetLog(type = SetType.FAILURE).countsForStats())
        assertTrue(SetLog(type = SetType.DROP).countsForStats())
    }

    @Test
    fun `volumen del ejercicio suma solo las series que cuentan`() {
        val sets = listOf(
            SetLog(weight = 20_000, reps = 10, type = SetType.WARMUP),
            SetLog(weight = 80_000, reps = 10),
            SetLog(weight = 80_000, reps = 8, type = SetType.FAILURE),
            SetLog(weight = 80_000, reps = 5, completed = false)
        )
        assertEquals(800_000L + 640_000L, exerciseVolume(sets))
        assertEquals(0L, exerciseVolume(emptyList()))
    }

    @Test
    fun `1RM Epley redondea a gramos enteros`() {
        // 100 kg x 1 rep: 100 * (1 + 1/30) = 103,33 kg -> 103.333 g
        assertEquals(103_333L, estimatedOneRm(100_000, 1))
        // 80 kg x 10: 80 * (1 + 10/30) = 106,666… kg -> 106.667 g (half-up)
        assertEquals(106_667L, estimatedOneRm(80_000, 10))
        // 60 kg x 30: 60 * 2 = 120 kg exactos
        assertEquals(120_000L, estimatedOneRm(60_000, 30))
    }

    @Test
    fun `1RM sin peso o sin repeticiones es cero`() {
        assertEquals(0L, estimatedOneRm(0, 10))
        assertEquals(0L, estimatedOneRm(80_000, 0))
        assertEquals(0L, estimatedOneRm(-1, 5))
    }

    @Test
    fun `volumen por grupo muscular agrupa y excluye calentamiento`() {
        val result = volumeByMuscleGroup(
            listOf(
                MuscleGroup.CHEST to SetLog(weight = 100_000, reps = 5),
                MuscleGroup.CHEST to SetLog(weight = 100_000, reps = 5, type = SetType.WARMUP),
                MuscleGroup.CHEST to SetLog(weight = 50_000, reps = 10),
                MuscleGroup.BACK to SetLog(weight = 60_000, reps = 10)
            )
        )
        assertEquals(1_000_000L, result[MuscleGroup.CHEST])
        assertEquals(600_000L, result[MuscleGroup.BACK])
        assertEquals(2, result.size)
    }
}
