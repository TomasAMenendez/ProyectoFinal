package com.example.aplicacionparacelular.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaytimeClockTest {
    @Test
    fun holdsUntilAFullSecondPasses() {
        val clock = PlaytimeClock(limitMinutes = 2, sampledRemaining = 120, sampledAtMs = 1_000, running = true)
        assertEquals(120, clock.remainingAt(1_999))
        assertEquals(119, clock.remainingAt(2_000))
        assertEquals(118, clock.remainingAt(3_500))
    }

    @Test
    fun doesNotGoBelowZero() {
        val clock = PlaytimeClock(limitMinutes = 1, sampledRemaining = 2, sampledAtMs = 0, running = true)
        assertEquals(0, clock.remainingAt(10_000))
    }

    @Test
    fun absorbKeepsLocalIfPiHasNotMoved() {
        val clock = PlaytimeClock(limitMinutes = 2, sampledRemaining = 120, sampledAtMs = 0, running = true)
        val next = clock.absorb(limitMinutes = 2, piRemaining = 120, nowMs = 5_000, running = true)
        assertEquals(115, next.sampledRemaining)
        assertEquals(5_000, next.sampledAtMs)
    }

    @Test
    fun absorbTakesPiIfChildUsedMore() {
        val clock = PlaytimeClock(limitMinutes = 2, sampledRemaining = 120, sampledAtMs = 0, running = true)
        val next = clock.absorb(limitMinutes = 2, piRemaining = 100, nowMs = 5_000, running = true)
        assertEquals(100, next.sampledRemaining)
    }

    @Test
    fun absorbResetsWhenLimitChanges() {
        val clock = PlaytimeClock(limitMinutes = 2, sampledRemaining = 10, sampledAtMs = 0, running = true)
        val next = clock.absorb(limitMinutes = 5, piRemaining = 300, nowMs = 8_000, running = true)
        assertEquals(5, next.limitMinutes)
        assertEquals(300, next.sampledRemaining)
    }

    @Test
    fun frozenUntilTeoIsReady() {
        val clock = PlaytimeClock(limitMinutes = 1, sampledRemaining = 60, sampledAtMs = 0, running = false)
        assertEquals(60, clock.remainingAt(30_000))
    }

    @Test
    fun absorbTakesPiWhenParentResetsQuota() {
        val clock = PlaytimeClock(limitMinutes = 1, sampledRemaining = 0, sampledAtMs = 0, running = false)
        val next = clock.absorb(limitMinutes = 1, piRemaining = 60, nowMs = 9_000, running = true)
        assertEquals(60, next.sampledRemaining)
        assertEquals(true, next.running)
        assertEquals(60, next.remainingAt(9_000))
    }
}
