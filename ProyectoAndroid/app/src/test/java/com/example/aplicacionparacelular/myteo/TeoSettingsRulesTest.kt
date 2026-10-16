package com.example.aplicacionparacelular.myteo

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeoSettingsRulesTest {
    @Test fun sensoryBodyOmitsPlaytimeAndBelly() {
        val body = TeoSettingsRules.sensoryBody(43, 67)
        assertEquals(40, body.getInt("volume_limit"))
        assertEquals(0.65, body.getDouble("brightness"), 0.001)
        assertFalse(body.has("playtime_limit_minutes"))
        assertFalse(body.has("belly_wake_enabled"))
        assertEquals(setOf("volume_limit", "brightness"), body.keys().asSequence().toSet())
    }

    @Test fun missingStatusDoesNotInventValues() {
        val session = SettingsSession()
        session.onStatus(JSONObject())
        assertEquals(LoadState.FAILED, session.load)
        assertNull(session.draft)
        assertNull(session.beginApply())
    }

    @Test fun remoteUpdateDoesNotClobberDraft() {
        val session = SettingsSession()
        session.onStatus(status(40, 0.5, false))
        session.edit(80, 20)
        session.onStatus(status(10, 0.1, false))
        assertEquals(SensoryDraft(80, 20), session.draft)
        assertEquals(SensoryDraft(10, 10), session.confirmed)
        val body = session.beginApply()
        assertEquals(80, body?.getInt("volume_limit"))
        assertFalse(body!!.has("playtime_limit_minutes"))
    }

    @Test fun failedApplyKeepsDraft() {
        val session = SettingsSession()
        session.onStatus(status(40, 0.4, false))
        session.edit(55, 15)
        assertTrue(session.beginApply() != null)
        session.failApply()
        assertEquals(SensoryDraft(55, 15), session.draft)
        assertTrue(session.canApply())
    }

    @Test fun nightFailureRestoresConfirmedAndBlocksDuplicates() {
        val session = SettingsSession()
        session.onStatus(status(40, 1.0, false))
        assertEquals(true, session.beginNight(true))
        assertNull(session.beginNight(false))
        session.failNight()
        assertEquals(false, session.nightConfirmed)
        assertEquals(true, session.beginNight(true))
    }

    @Test fun confirmedNightDoesNotResend() {
        val session = SettingsSession()
        session.onStatus(status(40, 1.0, true))
        assertNull(session.beginNight(true))
    }

    private fun status(volume: Int, brightness: Double, night: Boolean) = JSONObject()
        .put("volume_limit", volume)
        .put("brightness", brightness)
        .put("night_mode", night)
        .put("playtime_limit_minutes", 30)
}
