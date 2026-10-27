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

    @Test fun playtimeBodySendsOnlyTheLimit() {
        val body = TeoSettingsRules.playtimeBody(45)
        assertEquals(45, body?.getInt("playtime_limit_minutes"))
        assertEquals(setOf("playtime_limit_minutes"), body?.keys()?.asSequence()?.toSet())
    }

    @Test fun invalidPlaytimeDoesNotBuildABody() {
        assertNull(TeoSettingsRules.playtimeBody(-1))
        assertNull(TeoSettingsRules.playtimeBody(241))
        assertNull(TeoSettingsRules.parseMinutes(""))
        assertNull(TeoSettingsRules.parseMinutes("3.5"))
        assertNull(TeoSettingsRules.parseMinutes("241"))
    }

    @Test fun playtimeConfirmRequiresTheSameEcho() {
        val session = SettingsSession()
        session.onStatus(status(40, 0.5, false))
        session.editPlaytime("15")
        val body = session.beginPlaytimeApply()
        assertEquals(15, body?.getInt("playtime_limit_minutes"))
        assertFalse(session.confirmPlaytime(JSONObject().put("playtime_limit_minutes", 30)))
        assertEquals(30, session.playtimeConfirmed)
        assertEquals("15", session.playtimeText)
        assertTrue(session.canApplyPlaytime())

        assertTrue(session.beginPlaytimeApply() != null)
        assertTrue(session.confirmPlaytime(JSONObject().put("playtime_limit_minutes", 15)))
        assertEquals(15, session.playtimeConfirmed)
        assertFalse(session.canApplyPlaytime())
    }

    @Test fun voiceBodySendsOnlyTheChoice() {
        val body = TeoSettingsRules.voiceBody("micaela")
        assertEquals("micaela", body?.getString("tts_voice"))
        assertEquals(setOf("tts_voice"), body?.keys()?.asSequence()?.toSet())
        assertNull(TeoSettingsRules.voiceBody("elena"))
    }

    @Test fun missingVoiceOnStatusShowsTopa() {
        val session = SettingsSession()
        session.onStatus(status(40, 0.5, false))
        assertEquals("topa", session.displayedVoice)
        assertNull(session.beginVoice("topa"))
    }

    @Test fun voiceFailureRestoresConfirmed() {
        val session = SettingsSession()
        session.onStatus(status(40, 0.5, false).put("tts_voice", "topa"))
        val body = session.beginVoice("micaela")
        assertEquals("micaela", body?.getString("tts_voice"))
        assertEquals("micaela", session.displayedVoice)
        assertFalse(session.confirmVoice(JSONObject().put("volume_limit", 40)))
        assertEquals("topa", session.displayedVoice)
        assertTrue(session.beginVoice("micaela") != null)
        assertTrue(session.confirmVoice(JSONObject().put("tts_voice", "micaela")))
        assertEquals("micaela", session.voiceConfirmed)
        assertNull(session.beginVoice("micaela"))
    }

    @Test fun missingLocalVoiceShowsOff() {
        val session = SettingsSession()
        session.onStatus(status(40, 0.5, false))
        assertEquals(false, session.displayedLocalVoice)
        assertNull(session.beginLocalVoice(false))
    }

    @Test fun localVoiceSendsOnlyTheSwitch() {
        val session = SettingsSession()
        session.onStatus(status(40, 0.5, false))
        val body = session.beginLocalVoice(true)
        assertEquals(true, body?.getBoolean("tts_local_only"))
        assertEquals(setOf("tts_local_only"), body?.keys()?.asSequence()?.toSet())
        assertEquals(true, session.displayedLocalVoice)
        assertFalse(session.confirmLocalVoice(JSONObject().put("tts_voice", "topa")))
        assertEquals(false, session.displayedLocalVoice)
        assertTrue(session.beginLocalVoice(true) != null)
        assertTrue(session.confirmLocalVoice(JSONObject().put("tts_local_only", true)))
        assertEquals(true, session.localVoiceConfirmed)
    }

    private fun status(volume: Int, brightness: Double, night: Boolean) = JSONObject()
        .put("volume_limit", volume)
        .put("brightness", brightness)
        .put("night_mode", night)
        .put("playtime_limit_minutes", 30)
}
