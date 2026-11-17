package com.ignaciovinarro.stayfocused.planner.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSyncerTest {

    // extraBufferCapacity: permite emitir con tryEmit sin suspender ni esperar a un colector.
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 10)
    private var passes = 0

    private fun syncer() = ReminderSyncer(changes, reschedule = { passes++ }, debounceMs = 500)

    @Test
    fun `un cambio reprograma una vez`() = runTest {
        syncer().start(backgroundScope)
        runCurrent()

        changes.tryEmit(Unit)
        advanceTimeBy(501)
        runCurrent()

        assertEquals(1, passes)
    }

    @Test
    fun `una rafaga de cambios se junta en una sola pasada`() = runTest {
        syncer().start(backgroundScope)
        runCurrent()

        repeat(3) { changes.tryEmit(Unit) }
        advanceTimeBy(501)
        runCurrent()

        assertEquals(1, passes)
    }

    @Test
    fun `cambios separados por mas que el debounce reprograman cada uno`() = runTest {
        syncer().start(backgroundScope)
        runCurrent()

        changes.tryEmit(Unit)
        advanceTimeBy(600)
        runCurrent()
        changes.tryEmit(Unit)
        advanceTimeBy(600)
        runCurrent()

        assertEquals(2, passes)
    }

    @Test
    fun `sin cambios no reprograma`() = runTest {
        syncer().start(backgroundScope)
        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(0, passes)
    }

    @Test
    fun `no reprograma antes de que pase el debounce`() = runTest {
        syncer().start(backgroundScope)
        runCurrent()

        changes.tryEmit(Unit)
        advanceTimeBy(400)
        runCurrent()

        assertEquals(0, passes)
    }
}
