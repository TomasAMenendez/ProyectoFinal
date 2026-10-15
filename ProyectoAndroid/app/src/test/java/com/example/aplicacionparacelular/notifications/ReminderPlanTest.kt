package com.example.aplicacionparacelular.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlanTest {
    @Test
    fun sameTitleKeepsIndependentFutureAlarms() {
        var next = 80_000
        val codes = mutableMapOf<String, Int>()
        val now = 1_000L
        val alarms = ReminderPlan.planAppointments(
            listOf(
                AppointmentReminder("a", "Control", now + 3 * HOUR, "20/10/2026 - 10:00 hs"),
                AppointmentReminder("b", "Control", now + 5 * HOUR, "20/10/2026 - 12:00 hs"),
            ),
            now,
        ) { key -> codes.getOrPut(key) { next++ } }
        val first = alarms.filter { it.recordId == "a" }.map { it.requestCode }
        val second = alarms.filter { it.recordId == "b" }.map { it.requestCode }
        assertEquals(2, first.size)
        assertEquals(2, second.size)
        assertTrue(first.intersect(second).isEmpty())
    }

    @Test
    fun pastAppointmentIsNotScheduledAndAppliedVaccineClearsAlarms() {
        val now = System.currentTimeMillis()
        val past = ReminderPlan.planAppointments(
            listOf(AppointmentReminder("a", "Control", now - HOUR, "ayer")),
            now,
        ) { 1 }
        assertTrue(past.isEmpty())
        val applied = ReminderPlan.planVaccines(
            listOf(ReminderScheduler.VaccineReminder("v", "BCG", "2026-11-03", true)),
            now,
        ) { 1 }
        assertTrue(applied.isEmpty())
        val blank = ReminderPlan.planVaccines(
            listOf(ReminderScheduler.VaccineReminder("v", "BCG", "", false)),
            now,
        ) { 1 }
        assertTrue(blank.isEmpty())
    }

    @Test
    fun changingOrApplyingCancelsTheStoredCodesWithoutTouchingRoutines() {
        val routine = ReminderPlan.routineCode("rutina-manana")
        val previous = """[{"id":"v1","title":"Control","alarmCodes":[80010,80011,$routine]}]"""
        val cancelled = ReminderPlan.codesToCancel(previous, setOf(routine)) {
            ReminderPlan.legacyVaccineCodes(it.optString("id")) +
                ReminderPlan.legacyAppointmentCodes(it.optString("title"))
        }
        assertTrue(cancelled.contains(80010))
        assertTrue(cancelled.contains(80011))
        assertFalse(cancelled.contains(routine))
        assertTrue(cancelled.containsAll(ReminderPlan.legacyAppointmentCodes("Control")))
    }

    @Test
    fun storedCodesStayWithTheirRecord() {
        val alarms = listOf(
            PlannedAlarm(80001, 10, ReminderReceiver.KIND_TURNO, "TEO: turno médico", "Control · hoy", "a"),
            PlannedAlarm(80002, 20, ReminderReceiver.KIND_TURNO, "TEO: turno médico", "Control · hoy", "b"),
        )
        val json = ReminderPlan.storedAppointments(
            listOf(
                AppointmentReminder("a", "Control", 10, "hoy"),
                AppointmentReminder("b", "Control", 20, "hoy"),
            ),
            alarms,
        )
        val first = org.json.JSONArray(json).getJSONObject(0)
        assertEquals("a", first.getString("id"))
        assertEquals(80001, first.getJSONArray("alarmCodes").getInt(0))
        assertEquals(1, first.getJSONArray("alarmCodes").length())
    }

    companion object {
        private const val HOUR = 60L * 60L * 1000L
    }
}
