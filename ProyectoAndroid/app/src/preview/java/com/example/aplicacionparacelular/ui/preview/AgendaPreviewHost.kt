package com.example.aplicacionparacelular.ui.preview

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentAppointmentFormBinding
import com.example.aplicacionparacelular.databinding.FragmentMedicalBinding
import com.example.aplicacionparacelular.databinding.ItemAppointmentBinding
import com.example.aplicacionparacelular.databinding.ItemVaccineBinding
import com.example.aplicacionparacelular.ui.medical.AgendaRules
import com.example.aplicacionparacelular.ui.medical.Appointment
import com.example.aplicacionparacelular.ui.medical.MedicalRecords
import com.example.aplicacionparacelular.ui.medical.Vaccine
import com.example.aplicacionparacelular.ui.medical.VaccineStatus
import com.example.aplicacionparacelular.ui.routines.RoutineDeleteDialog
import java.util.Calendar
import java.util.Locale
import java.util.UUID

enum class AgendaPreviewScenario(val labelRes: Int) {
    LOADED(R.string.preview_agenda_loaded),
    EMPTY(R.string.preview_agenda_empty),
}

class AgendaPreviewHost(
    private val activity: ComponentActivity,
    private val onAlerts: () -> Unit,
    private val onConfig: () -> Unit,
    private val onFormVisible: (Boolean) -> Unit,
) {
    private val list = FragmentMedicalBinding.inflate(activity.layoutInflater)
    private val form = FragmentAppointmentFormBinding.inflate(activity.layoutInflater)
    private var scenario = AgendaPreviewScenario.LOADED
    private var vaccinesTab = false
    private var appointments = sampleAppointments()
    private var vaccines = sampleVaccines()
    private var year: Int? = null
    private var month: Int? = null
    private var day: Int? = null
    private var hour: Int? = null
    private var minute: Int? = null
    private var restoredForm = false

    val root: FrameLayout = FrameLayout(activity).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        addView(list.root)
        addView(form.root)
    }
    val isFormVisible: Boolean
        get() = form.root.visibility == View.VISIBLE
    val scenarioLabel: String
        get() = activity.getString(scenario.labelRes)

    init {
        list.tabAppointments.setOnClickListener {
            vaccinesTab = false
            render()
        }
        list.tabVaccines.setOnClickListener {
            vaccinesTab = true
            render()
        }
        list.btnAlertsBell.setOnClickListener { onAlerts() }
        list.btnMyTeo.setOnClickListener { onConfig() }
        list.btnAdd.setOnClickListener { openForm() }
        form.btnBack.setOnClickListener { showList() }
        form.btnCancel.setOnClickListener { showList() }
        form.editTitle.doAfterTextChanged {
            if (AgendaRules.titleProblem(it?.toString().orEmpty()) != null) return@doAfterTextChanged
            form.inputTitle.error = null
            if (form.txtFormError.text?.toString() == AgendaRules.titleProblem("")) {
                form.txtFormError.isVisible = false
            }
        }
        form.btnDate.setOnClickListener { pickDate() }
        form.btnTime.setOnClickListener { pickTime() }
        form.btnSave.setOnClickListener { saveForm() }
        showList()
    }

    fun showList() {
        form.root.visibility = View.GONE
        list.root.visibility = View.VISIBLE
        onFormVisible(false)
        render()
    }

    fun chooseScenario(next: AgendaPreviewScenario) {
        scenario = next
        appointments = if (next == AgendaPreviewScenario.EMPTY) emptyList() else sampleAppointments()
        vaccines = sampleVaccines()
        render()
    }

    fun persist(out: Bundle) {
        out.putBoolean(KEY_FORM, isFormVisible)
        out.putBoolean(KEY_VACCINES, vaccinesTab)
        out.putString(KEY_APPOINTMENTS, MedicalRecords.appointmentsTo(appointments))
        out.putString(KEY_VACCINES_DATA, MedicalRecords.vaccinesTo(vaccines))
        out.putString(KEY_TITLE, form.editTitle.text?.toString())
        out.putString(KEY_NOTES, form.editNotes.text?.toString())
        year?.let { out.putInt(KEY_YEAR, it) }
        month?.let { out.putInt(KEY_MONTH, it) }
        day?.let { out.putInt(KEY_DAY, it) }
        hour?.let { out.putInt(KEY_HOUR, it) }
        minute?.let { out.putInt(KEY_MINUTE, it) }
    }

    fun restore(state: Bundle?) {
        if (state == null) return
        vaccinesTab = state.getBoolean(KEY_VACCINES)
        MedicalRecords.appointmentsFrom(state.getString(KEY_APPOINTMENTS).orEmpty())?.let { appointments = it }
        MedicalRecords.vaccinesFrom(state.getString(KEY_VACCINES_DATA).orEmpty())?.let { vaccines = it }
        if (state.containsKey(KEY_YEAR)) {
            year = state.getInt(KEY_YEAR)
            month = state.getInt(KEY_MONTH)
            day = state.getInt(KEY_DAY)
        }
        if (state.containsKey(KEY_HOUR)) {
            hour = state.getInt(KEY_HOUR)
            minute = state.getInt(KEY_MINUTE)
        }
        form.editTitle.setText(state.getString(KEY_TITLE).orEmpty())
        form.editNotes.setText(state.getString(KEY_NOTES).orEmpty())
        restoredForm = state.getBoolean(KEY_FORM)
        renderWhen()
    }

    fun showRestoredForm() {
        if (!restoredForm) return
        restoredForm = false
        form.root.visibility = View.VISIBLE
        list.root.visibility = View.GONE
        onFormVisible(true)
        renderWhen()
    }

    private fun openForm() {
        year = null
        month = null
        day = null
        hour = null
        minute = null
        form.editTitle.setText("")
        form.editNotes.setText("")
        form.inputTitle.error = null
        form.txtFormError.isVisible = false
        form.root.visibility = View.VISIBLE
        list.root.visibility = View.GONE
        onFormVisible(true)
        renderWhen()
    }

    private fun pickDate() {
        val now = Calendar.getInstance()
        DatePickerDialog(
            activity,
            { _, y, m, d ->
                year = y
                month = m
                day = d
                renderWhen()
            },
            year ?: now.get(Calendar.YEAR),
            month ?: now.get(Calendar.MONTH),
            day ?: now.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun pickTime() {
        TimePickerDialog(
            activity,
            { _, h, m ->
                hour = h
                minute = m
                renderWhen()
            },
            hour ?: 9,
            minute ?: 0,
            true,
        ).show()
    }

    private fun renderWhen() {
        val chosenYear = year
        val chosenMonth = month
        val chosenDay = day
        form.txtDate.text = if (chosenYear == null || chosenMonth == null || chosenDay == null) {
            activity.getString(R.string.agenda_choose_date)
        } else {
            String.format(Locale.US, "%02d/%02d/%04d", chosenDay, chosenMonth + 1, chosenYear)
        }
        val chosenHour = hour
        val chosenMinute = minute
        form.txtTime.text = if (chosenHour == null || chosenMinute == null) {
            activity.getString(R.string.agenda_choose_time)
        } else {
            String.format(Locale.US, "%02d:%02d hs", chosenHour, chosenMinute)
        }
    }

    private fun saveForm() {
        val title = form.editTitle.text?.toString().orEmpty()
        val titleProblem = AgendaRules.titleProblem(title)
        val whenProblem = AgendaRules.whenProblem(year != null, hour != null)
        val problem = titleProblem ?: whenProblem
        form.inputTitle.error = titleProblem
        form.txtFormError.isVisible = problem != null
        form.txtFormError.text = problem
        if (problem != null) return
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year ?: return)
        cal.set(Calendar.MONTH, month ?: return)
        cal.set(Calendar.DAY_OF_MONTH, day ?: return)
        cal.set(Calendar.HOUR_OF_DAY, hour ?: return)
        cal.set(Calendar.MINUTE, minute ?: return)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val millis = cal.timeInMillis
        appointments = AgendaRules.sort(
            appointments + Appointment(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                date = AgendaRules.storedDate(millis),
                notes = form.editNotes.text?.toString().orEmpty().trim(),
                atMillis = millis,
            ),
        )
        vaccinesTab = false
        showList()
    }

    private fun render() {
        val active = ContextCompat.getColor(activity, R.color.teo_primary)
        val idle = ContextCompat.getColor(activity, R.color.teo_secondary)
        list.txtTabAppointments.setTextColor(if (vaccinesTab) idle else active)
        list.txtTabVaccines.setTextColor(if (vaccinesTab) active else idle)
        list.txtTabAppointments.compoundDrawableTintList = ColorStateList.valueOf(if (vaccinesTab) idle else active)
        list.txtTabVaccines.compoundDrawableTintList = ColorStateList.valueOf(if (vaccinesTab) active else idle)
        list.markAppointments.setBackgroundColor(if (vaccinesTab) android.graphics.Color.TRANSPARENT else active)
        list.markVaccines.setBackgroundColor(if (vaccinesTab) active else android.graphics.Color.TRANSPARENT)
        list.txtSubtitle.setText(if (vaccinesTab) R.string.agenda_vaccines_subtitle else R.string.agenda_appointments_subtitle)
        list.txtLocal.setText(if (vaccinesTab) R.string.agenda_local_vaccines else R.string.agenda_local_appointments)
        list.btnAdd.isVisible = !vaccinesTab
        list.listContainer.removeAllViews()
        if (vaccinesTab) renderVaccines() else renderAppointments()
    }

    private fun renderAppointments() {
        list.txtEmpty.isVisible = appointments.isEmpty()
        list.txtEmpty.setText(R.string.agenda_empty_appointments)
        val inflater = LayoutInflater.from(activity)
        appointments.forEach { item ->
            val row = ItemAppointmentBinding.inflate(inflater, list.listContainer, true)
            row.txtTitle.text = item.title
            row.txtWhen.text = AgendaRules.displayWhen(item)
            row.txtNotes.isVisible = item.notes.isNotBlank()
            row.txtNotes.text = item.notes
            row.btnDelete.setOnClickListener {
                RoutineDeleteDialog.show(
                    activity,
                    item.title,
                    title = activity.getString(R.string.agenda_delete_title),
                    question = activity.getString(R.string.agenda_delete_question, item.title),
                ) {
                    appointments = AgendaRules.without(appointments, item.id)
                    render()
                }
            }
        }
    }

    private fun renderVaccines() {
        list.txtEmpty.isVisible = false
        val inflater = LayoutInflater.from(activity)
        vaccines.forEach { item ->
            val row = ItemVaccineBinding.inflate(inflater, list.listContainer, true)
            val presentation = AgendaRules.vaccine(item)
            row.txtName.text = item.name
            row.txtAge.text = activity.getString(R.string.agenda_age, item.scheduledAge)
            row.checkApplied.setText(
                if (presentation.status == VaccineStatus.APPLIED) R.string.agenda_applied else R.string.agenda_pending,
            )
            row.rowSchedule.isVisible = presentation.showSchedule
            if (presentation.dueText == null) {
                row.txtSchedule.setText(R.string.agenda_no_date)
                row.btnSchedule.setText(R.string.agenda_schedule)
            } else {
                row.txtSchedule.text = activity.getString(R.string.agenda_scheduled, presentation.dueText)
                row.btnSchedule.setText(R.string.agenda_change_date)
            }
            row.checkApplied.setOnCheckedChangeListener(null)
            row.checkApplied.isChecked = item.applied
            row.checkApplied.setOnCheckedChangeListener { _, checked ->
                if (checked == item.applied) return@setOnCheckedChangeListener
                vaccines = vaccines.map { current ->
                    if (current.id == item.id) current.copy(applied = checked) else current
                }
                render()
            }
            row.btnSchedule.setOnClickListener { pickDueDate(item) }
        }
    }

    private fun pickDueDate(item: Vaccine) {
        val initial = item.dueDate.split("-")
        val now = Calendar.getInstance()
        DatePickerDialog(
            activity,
            { _, y, m, d ->
                val iso = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                vaccines = vaccines.map { current ->
                    if (current.id == item.id) current.copy(dueDate = iso) else current
                }
                render()
            },
            initial.getOrNull(0)?.toIntOrNull() ?: now.get(Calendar.YEAR),
            initial.getOrNull(1)?.toIntOrNull()?.minus(1) ?: now.get(Calendar.MONTH),
            initial.getOrNull(2)?.toIntOrNull() ?: now.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private fun sampleAppointments(): List<Appointment> {
        val first = at(2026, 10, 20, 10, 30)
        val second = at(2026, 10, 22, 16, 0)
        return listOf(
            Appointment(
                id = "preview-1",
                title = "Control de seguimiento con un nombre largo para ver cómo se acomoda",
                date = AgendaRules.storedDate(first),
                notes = "Traer el carnet, los estudios anteriores y llegar diez minutos antes de la hora.",
                atMillis = first,
            ),
            Appointment(
                id = "preview-2",
                title = "Fonoaudióloga",
                date = AgendaRules.storedDate(second),
                notes = "",
                atMillis = second,
            ),
        )
    }

    private fun sampleVaccines(): List<Vaccine> = listOf(
        Vaccine(
            "preview-v1",
            "Vacuna con un nombre largo que tiene que ocupar más de una línea",
            "12 meses",
            true,
        ),
        Vaccine("preview-v2", "Hepatitis A", "12 meses", false),
        Vaccine("preview-v3", "Triple viral", "12 meses", false, dueDate = "2026-11-03"),
    )

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val KEY_FORM = "preview_agenda_form"
        private const val KEY_VACCINES = "preview_agenda_vaccines_tab"
        private const val KEY_APPOINTMENTS = "preview_agenda_appointments"
        private const val KEY_VACCINES_DATA = "preview_agenda_vaccines"
        private const val KEY_TITLE = "preview_agenda_title"
        private const val KEY_NOTES = "preview_agenda_notes"
        private const val KEY_YEAR = "preview_agenda_year"
        private const val KEY_MONTH = "preview_agenda_month"
        private const val KEY_DAY = "preview_agenda_day"
        private const val KEY_HOUR = "preview_agenda_hour"
        private const val KEY_MINUTE = "preview_agenda_minute"
    }
}
