package com.example.aplicacionparacelular.ui.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaytimeLimitInputTest {
    @Test
    fun emptyIsUnlimited() {
        assertEquals(0, PlaytimeLimitInput.parseMinutes(""))
        assertEquals(0, PlaytimeLimitInput.parseMinutes("  "))
        assertEquals(0, PlaytimeLimitInput.parseMinutes("0"))
    }

    @Test
    fun readsWholeMinutes() {
        assertEquals(30, PlaytimeLimitInput.parseMinutes("30"))
        assertEquals(45, PlaytimeLimitInput.parseMinutes(" 45 "))
    }

    @Test
    fun clampsToDailyCap() {
        assertEquals(240, PlaytimeLimitInput.parseMinutes("999"))
        assertEquals(0, PlaytimeLimitInput.parseMinutes("-3"))
    }

    @Test
    fun rejectsJunk() {
        assertNull(PlaytimeLimitInput.parseMinutes("abc"))
        assertNull(PlaytimeLimitInput.parseMinutes("12.5"))
    }
}
