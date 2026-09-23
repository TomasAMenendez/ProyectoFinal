package com.example.aplicacionparacelular.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaytimeRingTest {
    @Test
    fun hiddenWhenNoLimit() {
        val ring = PlaytimeRing.from(limitMinutes = 0, remainingSeconds = 999)
        assertFalse(ring.visible)
        assertEquals(0f, ring.fraction, 0.001f)
    }

    @Test
    fun fullCircleAtStart() {
        val ring = PlaytimeRing.from(limitMinutes = 2, remainingSeconds = 120)
        assertTrue(ring.visible)
        assertEquals(1f, ring.fraction, 0.001f)
        assertEquals("2:00", ring.label)
        assertEquals(-90f, ring.startAngle, 0.01f)
        assertEquals(360f, ring.sweepAngle, 0.01f)
    }

    @Test
    fun vanishesClockwiseAtThreeQuarters() {
        val ring = PlaytimeRing.from(limitMinutes = 2, remainingSeconds = 90)
        assertEquals(0.75f, ring.fraction, 0.001f)
        assertEquals("1:30", ring.label)
        assertEquals(0f, ring.startAngle, 0.01f)
        assertEquals(270f, ring.sweepAngle, 0.01f)
    }

    @Test
    fun emptyAtZero() {
        val ring = PlaytimeRing.from(limitMinutes = 1, remainingSeconds = 0)
        assertTrue(ring.visible)
        assertEquals(0f, ring.fraction, 0.001f)
        assertEquals("0:00", ring.label)
        assertEquals(270f, ring.startAngle, 0.01f)
        assertEquals(0f, ring.sweepAngle, 0.01f)
    }

    @Test
    fun padsSeconds() {
        val ring = PlaytimeRing.from(limitMinutes = 10, remainingSeconds = 65)
        assertEquals("1:05", ring.label)
    }
}
