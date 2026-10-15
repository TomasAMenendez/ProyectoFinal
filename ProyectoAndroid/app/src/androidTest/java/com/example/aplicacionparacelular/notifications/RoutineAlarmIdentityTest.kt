package com.example.aplicacionparacelular.notifications

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineAlarmIdentityTest {
    @Test fun collidingRoutineCodesStillHaveDifferentPendingIntentIdentities() {
        assertEquals(ReminderPlan.routineCode("Aa"), ReminderPlan.routineCode("BB"))
        val first = Intent(ReminderReceiver.ACTION_REMINDER)
            .setData(Uri.parse(ReminderPlan.routineAlarmIdentity("Aa")))
        val second = Intent(ReminderReceiver.ACTION_REMINDER)
            .setData(Uri.parse(ReminderPlan.routineAlarmIdentity("BB")))
        assertFalse(first.filterEquals(second))
    }

    @Test fun reprogrammingTheSameRoutineReplacesItsAlarmIdentity() {
        val original = Intent(ReminderReceiver.ACTION_REMINDER)
            .setData(Uri.parse(ReminderPlan.routineAlarmIdentity("a")))
            .putExtra(ReminderReceiver.EXTRA_ROUTINE_TOKEN, "old")
        val replacement = Intent(ReminderReceiver.ACTION_REMINDER)
            .setData(Uri.parse(ReminderPlan.routineAlarmIdentity("a")))
            .putExtra(ReminderReceiver.EXTRA_ROUTINE_TOKEN, "new")
        assertTrue(original.filterEquals(replacement))
    }
}
