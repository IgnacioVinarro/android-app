package com.ignaciovinarro.stayfocused.gym.data

import com.ignaciovinarro.stayfocused.gym.domain.RecordKind
import com.ignaciovinarro.stayfocused.gym.domain.SetType
import org.junit.Assert.assertEquals
import org.junit.Test

class GymMappersTest {

    @Test
    fun `la serie se convierte al tipo del dominio`() {
        val log = SessionSetEntity(
            id = 3, sessionId = 1, exerciseId = 2, position = 0,
            weight = 50_000, reps = 8, type = SetType.FAILURE.name, completed = true
        ).toSetLog()

        assertEquals(3L, log.id)
        assertEquals(50_000L, log.weight)
        assertEquals(8, log.reps)
        assertEquals(SetType.FAILURE, log.type)
    }

    @Test
    fun `un tipo de serie desconocido se trata como normal`() {
        val log = SessionSetEntity(sessionId = 1, exerciseId = 2, position = 0, type = "VIEJO").toSetLog()
        assertEquals(SetType.NORMAL, log.type)
    }

    private fun record(kind: RecordKind, value: Long, weight: Long = 0) = PersonalRecordEntity(
        exerciseId = 1, kind = kind.name, value = value, weight = weight, sessionSetId = 1, achievedAt = 0
    )

    @Test
    fun `las mejores marcas salen del maximo de cada tipo`() {
        val bests = listOf(
            record(RecordKind.MAX_WEIGHT, 80_000),
            record(RecordKind.MAX_WEIGHT, 90_000),
            record(RecordKind.MAX_VOLUME, 500_000),
            record(RecordKind.ESTIMATED_1RM, 110_000),
            record(RecordKind.MAX_REPS_AT_WEIGHT, 8, weight = 60_000),
            record(RecordKind.MAX_REPS_AT_WEIGHT, 10, weight = 60_000),
            record(RecordKind.MAX_REPS_AT_WEIGHT, 5, weight = 80_000)
        ).toBests()

        assertEquals(90_000L, bests.maxWeight)
        assertEquals(500_000L, bests.maxVolume)
        assertEquals(110_000L, bests.maxOneRm)
        assertEquals(mapOf(60_000L to 10, 80_000L to 5), bests.maxRepsAtWeight)
    }

    @Test
    fun `sin records las mejores marcas estan vacias`() {
        val bests = emptyList<PersonalRecordEntity>().toBests()
        assertEquals(0L, bests.maxWeight)
        assertEquals(emptyMap<Long, Int>(), bests.maxRepsAtWeight)
    }
}
