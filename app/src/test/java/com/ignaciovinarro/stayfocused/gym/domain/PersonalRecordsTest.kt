package com.ignaciovinarro.stayfocused.gym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecordsTest {

    private fun List<NewRecord>.of(kind: RecordKind) = firstOrNull { it.kind == kind }

    @Test
    fun `sin historial todo es record`() {
        val records = detectRecords(listOf(SetLog(id = 1, weight = 80_000, reps = 10)), PersonalBests())

        assertEquals(80_000L, records.of(RecordKind.MAX_WEIGHT)?.value)
        assertEquals(106_667L, records.of(RecordKind.ESTIMATED_1RM)?.value)
        assertEquals(800_000L, records.of(RecordKind.MAX_VOLUME)?.value)
        val reps = records.of(RecordKind.MAX_REPS_AT_WEIGHT)
        assertEquals(10L, reps?.value)
        assertEquals(80_000L, reps?.weight)
        assertEquals(1L, reps?.setId)
    }

    @Test
    fun `empatar la marca no es record`() {
        val bests = PersonalBests(
            maxWeight = 80_000,
            maxVolume = 800_000,
            maxOneRm = 106_667,
            maxRepsAtWeight = mapOf(80_000L to 10)
        )
        assertTrue(detectRecords(listOf(SetLog(id = 1, weight = 80_000, reps = 10)), bests).isEmpty())
    }

    @Test
    fun `superar solo el peso maximo marca solo ese record`() {
        val bests = PersonalBests(
            maxWeight = 80_000,
            maxVolume = 10_000_000,
            maxOneRm = 200_000,
            maxRepsAtWeight = mapOf(85_000L to 20)
        )
        val records = detectRecords(listOf(SetLog(id = 7, weight = 85_000, reps = 5)), bests)

        assertEquals(listOf(RecordKind.MAX_WEIGHT), records.map { it.kind })
        assertEquals(7L, records.single().setId)
    }

    @Test
    fun `mas repeticiones con el mismo peso es record de repeticiones`() {
        val bests = PersonalBests(
            maxWeight = 100_000,
            maxVolume = 10_000_000,
            maxOneRm = 200_000,
            maxRepsAtWeight = mapOf(60_000L to 8)
        )
        val records = detectRecords(
            listOf(SetLog(id = 1, weight = 60_000, reps = 8), SetLog(id = 2, weight = 60_000, reps = 9)),
            bests
        )
        val reps = records.single()
        assertEquals(RecordKind.MAX_REPS_AT_WEIGHT, reps.kind)
        assertEquals(9L, reps.value)
        assertEquals(2L, reps.setId)
    }

    @Test
    fun `el volumen es el total del ejercicio en la sesion`() {
        val bests = PersonalBests(
            maxWeight = 100_000,
            maxVolume = 1_000_000,
            maxOneRm = 200_000,
            maxRepsAtWeight = mapOf(50_000L to 20)
        )
        val sets = listOf(
            SetLog(id = 1, weight = 50_000, reps = 10),
            SetLog(id = 2, weight = 50_000, reps = 10),
            SetLog(id = 3, weight = 50_000, reps = 10)
        )
        val records = detectRecords(sets, bests)
        val volume = records.single()
        assertEquals(RecordKind.MAX_VOLUME, volume.kind)
        assertEquals(1_500_000L, volume.value)
    }

    @Test
    fun `el calentamiento y las series sin completar no generan records`() {
        val sets = listOf(
            SetLog(id = 1, weight = 200_000, reps = 5, type = SetType.WARMUP),
            SetLog(id = 2, weight = 200_000, reps = 5, completed = false)
        )
        assertTrue(detectRecords(sets, PersonalBests()).isEmpty())
    }

    @Test
    fun `series sin peso o sin repeticiones no generan records`() {
        assertTrue(detectRecords(listOf(SetLog(id = 1, weight = 0, reps = 10)), PersonalBests()).isEmpty())
        assertTrue(detectRecords(listOf(SetLog(id = 1, weight = 50_000, reps = 0)), PersonalBests()).isEmpty())
        assertTrue(detectRecords(emptyList(), PersonalBests()).isEmpty())
    }

    @Test
    fun `por tipo se informa un solo record el mas alto`() {
        val sets = listOf(
            SetLog(id = 1, weight = 90_000, reps = 3),
            SetLog(id = 2, weight = 100_000, reps = 1)
        )
        val records = detectRecords(sets, PersonalBests())
        assertEquals(1, records.count { it.kind == RecordKind.MAX_WEIGHT })
        assertEquals(100_000L, records.of(RecordKind.MAX_WEIGHT)?.value)
        assertEquals(2L, records.of(RecordKind.MAX_WEIGHT)?.setId)
        assertNull(records.of(RecordKind.MAX_REPS_AT_WEIGHT)?.takeIf { it.weight == 0L })
    }
}
