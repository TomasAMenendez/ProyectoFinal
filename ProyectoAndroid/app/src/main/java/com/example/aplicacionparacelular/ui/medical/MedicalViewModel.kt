package com.example.aplicacionparacelular.ui.medical

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class Appointment(
    val id: String,
    val title: String,
    val date: String,
    val notes: String = "",
    val atMillis: Long = 0L,
)

data class Vaccine(
    val id: String,
    val name: String,
    val scheduledAge: String,
    val applied: Boolean,
    val appliedDate: String = "",
    val dueDate: String = ""
)

class MedicalViewModel(
    private val schedule: (Context, List<Vaccine>, List<Appointment>) -> Unit = ::scheduleMedicalReminders,
) : ViewModel() {
    private var loaded = false

    private val _appointments = MutableLiveData<List<Appointment>>(emptyList())
    val appointments: LiveData<List<Appointment>> = _appointments

    private val _vaccines = MutableLiveData<List<Vaccine>>(emptyList())
    val vaccines: LiveData<List<Vaccine>> = _vaccines

    private val _appointmentLoad = MutableLiveData(RecordLoad.READY)
    val appointmentLoad: LiveData<RecordLoad> = _appointmentLoad

    private val _vaccineLoad = MutableLiveData(RecordLoad.READY)
    val vaccineLoad: LiveData<RecordLoad> = _vaccineLoad

    var sectionIsVaccines: Boolean = false

    /**
     * Loads appointments and vaccines from local SharedPreferences.
     * Medical data is stored locally only (no communication with Pi needed).
     */
    fun loadData(context: Context) {
        val prefs = context.getSharedPreferences("medical_data", Context.MODE_PRIVATE)

        // Load appointments
        val appointmentsJson = prefs.getString("appointments", null)
        if (appointmentsJson != null) {
            val parsed = MedicalRecords.appointmentsFrom(appointmentsJson)
            if (parsed == null) {
                _appointmentLoad.value = RecordLoad.FAILED
            } else {
                _appointments.value = AgendaRules.sort(parsed)
                _appointmentLoad.value = RecordLoad.READY
            }
        } else {
            _appointments.value = emptyList()
            _appointmentLoad.value = RecordLoad.READY
        }

        // Load vaccines
        val vaccinesJson = prefs.getString("vaccines", null)
        if (vaccinesJson != null) {
            val parsed = MedicalRecords.vaccinesFrom(vaccinesJson)
            if (parsed == null) {
                _vaccineLoad.value = RecordLoad.FAILED
            } else {
                _vaccines.value = parsed.toList()
                _vaccineLoad.value = RecordLoad.READY
            }
        } else {
            _vaccines.value = AgendaRules.defaultVaccines()
            _vaccineLoad.value = RecordLoad.READY
        }
        loaded = true
    }

    private fun ensureLoaded(context: Context) {
        if (!loaded) loadData(context)
    }

    fun addAppointment(context: Context, appointment: Appointment): Boolean {
        ensureLoaded(context)
        if (_appointmentLoad.value != RecordLoad.READY) return false
        val current = _appointments.value.orEmpty() + appointment
        _appointments.value = AgendaRules.sort(current)
        _appointmentLoad.value = RecordLoad.READY
        saveAppointments(context)
        return true
    }

    fun removeAppointment(context: Context, id: String) {
        ensureLoaded(context)
        if (_appointmentLoad.value != RecordLoad.READY) return
        _appointments.value = AgendaRules.without(_appointments.value.orEmpty(), id)
        saveAppointments(context)
    }

    fun toggleVaccine(context: Context, id: String) {
        ensureLoaded(context)
        if (_vaccineLoad.value != RecordLoad.READY) return
        val current = _vaccines.value?.toMutableList() ?: return
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        val vaccine = current[index]
        current[index] = vaccine.copy(applied = !vaccine.applied)
        _vaccines.value = current
        saveVaccines(context)
    }

    fun setVaccineDueDate(context: Context, id: String, isoDate: String) {
        ensureLoaded(context)
        if (_vaccineLoad.value != RecordLoad.READY) return
        val current = _vaccines.value?.toMutableList() ?: return
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        current[index] = current[index].copy(dueDate = isoDate)
        _vaccines.value = current
        saveVaccines(context)
    }

    fun scheduleReminders(context: Context) {
        ensureLoaded(context)
        if (_appointmentLoad.value == RecordLoad.FAILED || _vaccineLoad.value == RecordLoad.FAILED) return
        schedule(context.applicationContext, _vaccines.value.orEmpty(), _appointments.value.orEmpty())
    }

    private fun saveAppointments(context: Context) {
        val prefs = context.getSharedPreferences("medical_data", Context.MODE_PRIVATE)
        prefs.edit().putString("appointments", MedicalRecords.appointmentsTo(_appointments.value.orEmpty())).apply()
        scheduleReminders(context)
    }

    private fun saveVaccines(context: Context) {
        val prefs = context.getSharedPreferences("medical_data", Context.MODE_PRIVATE)
        prefs.edit().putString("vaccines", MedicalRecords.vaccinesTo(_vaccines.value.orEmpty())).apply()
        scheduleReminders(context)
    }
}

private fun scheduleMedicalReminders(app: Context, vaccines: List<Vaccine>, appointments: List<Appointment>) {
    com.example.aplicacionparacelular.notifications.ReminderScheduler.saveAndScheduleVaccines(
        app,
        vaccines.map {
            com.example.aplicacionparacelular.notifications.ReminderScheduler.VaccineReminder(
                id = it.id,
                name = it.name,
                dueDateIso = it.dueDate,
                applied = it.applied,
            )
        },
    )
    com.example.aplicacionparacelular.notifications.ReminderScheduler.saveAndScheduleAppointments(
        app,
        appointments.map {
            com.example.aplicacionparacelular.notifications.AppointmentReminder(
                id = it.id,
                title = it.title,
                atMillis = AgendaRules.atMillis(it),
                label = AgendaRules.displayWhen(it),
            )
        },
    )
}
