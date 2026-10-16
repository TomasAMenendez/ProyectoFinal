package com.example.aplicacionparacelular.ui.medical

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MedicalPersistenceTest {
    @Test fun aNewViewModelLoadsSavedRecordsBeforeAddingATurn() = withIsolatedPreferences { context ->
        val first = Appointment("one", "Consulta", "", atMillis = 10_000L)
        val second = Appointment("two", "Control", "", atMillis = 20_000L)
        val vaccine = Vaccine("vac", "Vacuna", "", false, dueDate = "2030-01-02")
        val prefs = context.getSharedPreferences("medical_data", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("appointments", MedicalRecords.appointmentsTo(listOf(first, second)))
            .putString("vaccines", MedicalRecords.vaccinesTo(listOf(vaccine)))
            .commit()
        var scheduledVaccines: List<Vaccine> = emptyList()
        var scheduledAppointments: List<Appointment> = emptyList()
        val viewModel = MedicalViewModel { _, vaccines, appointments ->
            scheduledVaccines = vaccines
            scheduledAppointments = appointments
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertTrue(viewModel.addAppointment(context, Appointment("three", "Nuevo", "", atMillis = 30_000L)))
        }
        assertEquals(listOf("one", "two", "three"), MedicalRecords.appointmentsFrom(prefs.getString("appointments", "")!!)!!.map { it.id })
        assertEquals(listOf(vaccine), scheduledVaccines)
        assertEquals(3, scheduledAppointments.size)
    }

    @Test fun malformedStoredAppointmentsAreNotReplacedByANewTurn() = withIsolatedPreferences { context ->
        val prefs = context.getSharedPreferences("medical_data", Context.MODE_PRIVATE)
        prefs.edit().putString("appointments", "malformed").commit()
        var scheduled = false
        val viewModel = MedicalViewModel { _, _, _ -> scheduled = true }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertFalse(viewModel.addAppointment(context, Appointment("new", "Nuevo", "")))
            assertEquals(RecordLoad.FAILED, viewModel.appointmentLoad.value)
        }
        assertEquals("malformed", prefs.getString("appointments", null))
        assertFalse(scheduled)
    }

    private fun withIsolatedPreferences(test: (Context) -> Unit) {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "medical-test-${UUID.randomUUID()}"
        val context = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(ignored: String, mode: Int): SharedPreferences =
                base.getSharedPreferences(name, mode)
        }
        try {
            test(context)
        } finally {
            base.deleteSharedPreferences(name)
        }
    }
}
