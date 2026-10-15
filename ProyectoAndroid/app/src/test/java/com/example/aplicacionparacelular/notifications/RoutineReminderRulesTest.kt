package com.example.aplicacionparacelular.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineReminderRulesTest {
    @Test
    fun aRemovedRoutineCannotBorrowTheIdentityOfACollidingCurrentRoutine() {
        assertEquals(ReminderPlan.routineCode("Aa"), ReminderPlan.routineCode("BB"))
        val current = """[{"id":"BB","name":"Paseo","minutes":540,"alarmToken":"current"}]"""
        assertFalse(ReminderPlan.isCurrentRoutine(current, ReminderPlan.BASE_ROUTINE, "09:00", "Aa", "current"))
        assertTrue(ReminderPlan.isCurrentRoutine(current, ReminderPlan.BASE_ROUTINE, "09:00", "BB", "current"))
        assertFalse(ReminderPlan.routineAlarmIdentity("Aa") == ReminderPlan.routineAlarmIdentity("BB"))
    }

    @Test
    fun changingTheMessageInvalidatesTheOldProgrammingTokenAtTheSameTime() {
        val old = ReminderPlan.storedRoutines(listOf(Triple("a", "Desayuno", 480))) { "old" }
        val changed = ReminderPlan.storedRoutines(listOf(Triple("a", "Paseo", 480))) { "new" }
        assertTrue(ReminderPlan.isCurrentRoutine(old, ReminderPlan.BASE_ROUTINE, "08:00", "a", "old"))
        assertFalse(ReminderPlan.isCurrentRoutine(changed, ReminderPlan.BASE_ROUTINE, "08:00", "a", "old"))
        assertTrue(ReminderPlan.isCurrentRoutine(changed, ReminderPlan.BASE_ROUTINE, "08:00", "a", "new"))
    }

    @Test
    fun deletingAndRecreatingTheSameIdCannotReuseTheOldBroadcast() {
        val recreated = ReminderPlan.storedRoutines(listOf(Triple("a", "Desayuno", 480))) { "recreated" }
        assertFalse(ReminderPlan.isCurrentRoutine("[]", ReminderPlan.BASE_ROUTINE, "08:00", "a", "old"))
        assertFalse(ReminderPlan.isCurrentRoutine(recreated, ReminderPlan.BASE_ROUTINE, "08:00", "a", "old"))
        assertTrue(ReminderPlan.isCurrentRoutine(recreated, ReminderPlan.BASE_ROUTINE, "08:00", "a", "recreated"))
    }

    @Test
    fun migrationKeepsLegacyCodesSeparateFromTheNewIdentityNamespace() {
        val legacy = """[{"id":"a","name":"Desayuno","minutes":480}]"""
        val current = ReminderPlan.storedRoutines(listOf(Triple("a", "Desayuno", 480))) { "token" }
        assertEquals(setOf(ReminderPlan.routineCode("a")), ReminderPlan.legacyRoutineCodes(legacy))
        assertTrue(ReminderPlan.legacyRoutineCodes(current).isEmpty())
        assertFalse(ReminderPlan.isCurrentRoutine(legacy, ReminderPlan.routineCode("a"), "08:00"))
        assertFalse(ReminderPlan.routineAlarmIdentity("a/b") == ReminderPlan.routineAlarmIdentity("a%2Fb"))
    }

    @Test
    fun migratingRoutineAlarmsDoesNotCancelMedicalAlarmsWithAnOldSharedCode() {
        val routines = """[{"id":"Aa","minutes":540},{"id":"a","minutes":480}]"""
        val vaccines = """[{"id":"vac","alarmCodes":[44112]}]"""
        assertEquals(listOf(42097), ReminderPlan.routineLegacyCodesToCancel(routines, vaccines, "[]"))
        val appointments = """[{"title":"Consulta","alarmCodes":[44112]}]"""
        assertEquals(listOf(42097), ReminderPlan.routineLegacyCodesToCancel(routines, "[]", appointments))
    }

    @Test
    fun allPreviouslyScheduledRoutinesCanBeCancelledBeforeReplacingTheList() {
        val previous = """[{"id":"a","name":"Desayuno","minutes":450},{"id":"b","name":"Paseo","minutes":900}]"""
        assertEquals(setOf(42097, 42098), ReminderPlan.routineCodes(previous))
        assertTrue(ReminderPlan.routineCodes("[]").isEmpty())
    }

    @Test
    fun aRemovedRoutineCannotDeliverOrRearmAnAlreadyQueuedBroadcast() {
        assertFalse(ReminderPlan.isCurrentRoutine("[]", 42097, "07:30"))
    }

    @Test
    fun aChangedTimeRejectsTheOldBroadcastAndAcceptsTheCurrentOne() {
        val current = """[{"id":"a","name":"Desayuno","minutes":480,"alarmToken":"token"}]"""
        assertFalse(ReminderPlan.isCurrentRoutine(current, ReminderPlan.BASE_ROUTINE, "07:30", "a", "token"))
        assertTrue(ReminderPlan.isCurrentRoutine(current, ReminderPlan.BASE_ROUTINE, "08:00", "a", "token"))
        assertFalse(ReminderPlan.isCurrentRoutine(current, ReminderPlan.BASE_ROUTINE, "08:00", "b", "token"))
    }

    @Test
    fun invalidStoredDataCannotRearmAnAlarm() {
        assertFalse(ReminderPlan.isCurrentRoutine("invalid", 42097, "07:30"))
        assertFalse(ReminderPlan.isCurrentRoutine("""[{"id":"a","minutes":1440}]""", 42097, "24:00"))
        assertFalse(ReminderPlan.isCurrentRoutine("""[{"id":"a","minutes":450}]""", 42097, null))
        val current = """[{"id":"a","minutes":480,"alarmToken":"token"}]"""
        assertFalse(ReminderPlan.isCurrentRoutine(current, ReminderPlan.BASE_ROUTINE, "24:00", "a", "token"))
        assertFalse(ReminderPlan.isCurrentRoutine(current, ReminderPlan.BASE_ROUTINE, null, "a", "token"))
    }
}
