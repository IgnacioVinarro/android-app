package com.ignaciovinarro.stayfocused.gym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SetValidationTest {

    @Test
    fun `peso y repeticiones exige las dos cosas`() {
        assertNull(validateSet(TrackingType.WEIGHT_REPS, SetLog(weight = 1_000, reps = 1)))
        assertEquals(SetError.WEIGHT_REQUIRED, validateSet(TrackingType.WEIGHT_REPS, SetLog(weight = 0, reps = 5)))
        assertEquals(SetError.REPS_REQUIRED, validateSet(TrackingType.WEIGHT_REPS, SetLog(weight = 1_000, reps = 0)))
    }

    @Test
    fun `solo repeticiones no pide peso`() {
        assertNull(validateSet(TrackingType.REPS_ONLY, SetLog(weight = 0, reps = 12)))
        assertEquals(SetError.REPS_REQUIRED, validateSet(TrackingType.REPS_ONLY, SetLog(reps = 0)))
    }

    @Test
    fun `tiempo exige duracion`() {
        assertNull(validateSet(TrackingType.TIME, SetLog(durationSeconds = 45)))
        assertEquals(SetError.DURATION_REQUIRED, validateSet(TrackingType.TIME, SetLog(durationSeconds = 0)))
    }
}
