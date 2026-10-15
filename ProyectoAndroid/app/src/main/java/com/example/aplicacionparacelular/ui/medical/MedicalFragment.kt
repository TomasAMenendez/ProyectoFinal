package com.example.aplicacionparacelular.ui.medical

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentMedicalBinding
import com.example.aplicacionparacelular.databinding.ItemAppointmentBinding
import com.example.aplicacionparacelular.databinding.ItemVaccineBinding
import com.example.aplicacionparacelular.ui.SystemBarInsets
import com.example.aplicacionparacelular.ui.routines.RoutineDeleteDialog
import com.google.android.material.snackbar.Snackbar

class MedicalFragment : Fragment() {
    private var _binding: FragmentMedicalBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MedicalViewModel
    private var warnedNotifications = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(requireActivity())[MedicalViewModel::class.java]
        _binding = FragmentMedicalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyTop(binding.agendaScroll)
        viewModel.loadData(requireContext())
        if (viewModel.appointmentLoad.value == RecordLoad.READY && viewModel.vaccineLoad.value == RecordLoad.READY) {
            viewModel.scheduleReminders(requireContext())
        }
        binding.tabAppointments.setOnClickListener { show(false) }
        binding.tabVaccines.setOnClickListener { show(true) }
        binding.btnAdd.setOnClickListener { findNavController().navigate(R.id.nav_appointment_form) }
        binding.btnAlertsBell.setOnClickListener { findNavController().navigate(R.id.nav_alerts) }
        binding.btnMyTeo.setOnClickListener { findNavController().navigate(R.id.nav_config) }
        viewModel.appointments.observe(viewLifecycleOwner) { render() }
        viewModel.vaccines.observe(viewLifecycleOwner) { render() }
        viewModel.appointmentLoad.observe(viewLifecycleOwner) { render() }
        viewModel.vaccineLoad.observe(viewLifecycleOwner) { render() }
        show(viewModel.sectionIsVaccines)
        warnIfNotificationsDisabled()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun show(vaccines: Boolean) {
        viewModel.sectionIsVaccines = vaccines
        render()
    }

    private fun render() {
        if (_binding == null) return
        val vaccines = viewModel.sectionIsVaccines
        paintTab(!vaccines)
        binding.txtSubtitle.setText(
            if (vaccines) R.string.agenda_vaccines_subtitle else R.string.agenda_appointments_subtitle,
        )
        binding.txtLocal.setText(
            if (vaccines) R.string.agenda_local_vaccines else R.string.agenda_local_appointments,
        )
        binding.btnAdd.isVisible = !vaccines
        binding.listContainer.removeAllViews()
        if (vaccines) renderVaccines() else renderAppointments()
    }

    private fun renderAppointments() {
        if (viewModel.appointmentLoad.value == RecordLoad.FAILED) {
            binding.txtEmpty.isVisible = true
            binding.txtEmpty.setText(R.string.agenda_read_failed)
            return
        }
        val items = viewModel.appointments.value.orEmpty()
        binding.txtEmpty.isVisible = items.isEmpty()
        binding.txtEmpty.setText(R.string.agenda_empty_appointments)
        val inflater = LayoutInflater.from(requireContext())
        items.forEach { item ->
            val row = ItemAppointmentBinding.inflate(inflater, binding.listContainer, true)
            row.txtTitle.text = item.title
            row.txtWhen.text = AgendaRules.displayWhen(item)
            row.txtNotes.isVisible = item.notes.isNotBlank()
            row.txtNotes.text = item.notes
            row.btnDelete.setOnClickListener {
                RoutineDeleteDialog.show(
                    requireContext(),
                    item.title,
                    title = getString(R.string.agenda_delete_title),
                    question = getString(R.string.agenda_delete_question, item.title),
                ) {
                    viewModel.removeAppointment(requireContext(), item.id)
                }
            }
        }
    }

    private fun renderVaccines() {
        if (viewModel.vaccineLoad.value == RecordLoad.FAILED) {
            binding.txtEmpty.isVisible = true
            binding.txtEmpty.setText(R.string.agenda_read_failed)
            return
        }
        binding.txtEmpty.isVisible = false
        val inflater = LayoutInflater.from(requireContext())
        viewModel.vaccines.value.orEmpty().forEach { item ->
            val row = ItemVaccineBinding.inflate(inflater, binding.listContainer, true)
            val presentation = AgendaRules.vaccine(item)
            row.txtName.text = item.name
            row.txtAge.text = getString(R.string.agenda_age, item.scheduledAge)
            row.checkApplied.setText(
                if (presentation.status == VaccineStatus.APPLIED) R.string.agenda_applied else R.string.agenda_pending,
            )
            row.rowSchedule.isVisible = presentation.showSchedule
            if (presentation.dueText == null) {
                row.txtSchedule.setText(R.string.agenda_no_date)
                row.btnSchedule.setText(R.string.agenda_schedule)
            } else {
                row.txtSchedule.text = getString(R.string.agenda_scheduled, presentation.dueText)
                row.btnSchedule.setText(R.string.agenda_change_date)
            }
            row.checkApplied.setOnCheckedChangeListener(null)
            row.checkApplied.isChecked = item.applied
            row.checkApplied.setOnCheckedChangeListener { _, checked ->
                if (checked != item.applied) viewModel.toggleVaccine(requireContext(), item.id)
            }
            row.btnSchedule.setOnClickListener { pickDueDate(item) }
        }
    }

    private fun pickDueDate(item: Vaccine) {
        val initial = item.dueDate.split("-")
        val now = java.util.Calendar.getInstance()
        val year = initial.getOrNull(0)?.toIntOrNull() ?: now.get(java.util.Calendar.YEAR)
        val month = initial.getOrNull(1)?.toIntOrNull()?.minus(1) ?: now.get(java.util.Calendar.MONTH)
        val day = initial.getOrNull(2)?.toIntOrNull() ?: now.get(java.util.Calendar.DAY_OF_MONTH)
        android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
            val iso = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d)
            viewModel.setVaccineDueDate(requireContext(), item.id, iso)
        }, year, month, day).show()
    }

    private fun paintTab(appointments: Boolean) {
        val active = ContextCompat.getColor(requireContext(), R.color.teo_primary)
        val idle = ContextCompat.getColor(requireContext(), R.color.teo_secondary)
        binding.txtTabAppointments.setTextColor(if (appointments) active else idle)
        binding.txtTabVaccines.setTextColor(if (appointments) idle else active)
        binding.markAppointments.setBackgroundColor(if (appointments) active else android.graphics.Color.TRANSPARENT)
        binding.markVaccines.setBackgroundColor(if (appointments) android.graphics.Color.TRANSPARENT else active)
        binding.txtTabAppointments.compoundDrawableTintList = ColorStateList.valueOf(if (appointments) active else idle)
        binding.txtTabVaccines.compoundDrawableTintList = ColorStateList.valueOf(if (appointments) idle else active)
    }

    private fun warnIfNotificationsDisabled() {
        if (warnedNotifications) return
        val enabled = androidx.core.app.NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()
        if (enabled) return
        warnedNotifications = true
        Snackbar.make(binding.root, R.string.agenda_notifications_off, Snackbar.LENGTH_LONG)
            .setAction(R.string.agenda_notifications_open) {
                startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        requireContext().packageName,
                    ),
                )
            }
            .show()
    }
}
