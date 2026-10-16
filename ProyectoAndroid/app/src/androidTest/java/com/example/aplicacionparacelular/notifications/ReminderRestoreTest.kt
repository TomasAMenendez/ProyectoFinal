package com.example.aplicacionparacelular.notifications

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReminderRestoreTest {
    @Test fun oneCorruptCollectionDoesNotStopRestoringOtherEmptyCollections() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reminder-restore-test-${UUID.randomUUID()}"
        val prefs = base.getSharedPreferences(name, Context.MODE_PRIVATE)
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(ignored: String, mode: Int): SharedPreferences = prefs
        }
        try {
            prefs.edit()
                .putString("routines_json", "not-json-routines")
                .putString("vaccines_json", " [ ] ")
                .putString("appointments_json", " [ ] ")
                .commit()
            // Empty valid plans have no PendingIntents or alarm operations.
            ReminderScheduler.restoreAll(context)
            assertEquals("not-json-routines", prefs.getString("routines_json", null))
            assertEquals("[]", prefs.getString("vaccines_json", null))
            assertEquals("[]", prefs.getString("appointments_json", null))
        } finally {
            base.deleteSharedPreferences(name)
        }
    }

    @Test fun malformedCollectionsArePreservedWithoutOpeningTheAlarmManager() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reminder-restore-test-${UUID.randomUUID()}"
        val prefs = base.getSharedPreferences(name, Context.MODE_PRIVATE)
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(ignored: String, mode: Int): SharedPreferences = prefs
            override fun getSystemService(serviceName: String): Any? {
                check(serviceName != Context.ALARM_SERVICE) { "Malformed records must not schedule or cancel alarms" }
                return super.getSystemService(serviceName)
            }
        }
        val corrupt = mapOf(
            "routines_json" to "not-json-routines",
            "vaccines_json" to "not-json-vaccines",
            "appointments_json" to "not-json-appointments",
        )
        try {
            val editor = prefs.edit()
            corrupt.forEach { (key, value) -> editor.putString(key, value) }
            editor.commit()
            ReminderScheduler.restoreAll(context)
            corrupt.forEach { (key, value) -> assertEquals(value, prefs.getString(key, null)) }
        } finally {
            base.deleteSharedPreferences(name)
        }
    }
}
