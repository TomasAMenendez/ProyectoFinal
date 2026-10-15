package com.example.aplicacionparacelular.ui.medical

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentAppointmentFormBinding
import com.example.aplicacionparacelular.ui.SystemBarInsets
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AppointmentFormFragment : Fragment() {
    private var _binding: FragmentAppointmentFormBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MedicalViewModel
    private var year: Int? = null
    private var month: Int? = null
    private var day: Int? = null
    private var hour: Int? = null
    private var minute: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(requireActivity())[MedicalViewModel::class.java]
        _binding = FragmentAppointmentFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.formScroll)
        savedInstanceState?.let { state ->
            if (state.containsKey(STATE_YEAR)) {
                year = state.getInt(STATE_YEAR)
                month = state.getInt(STATE_MONTH)
                day = state.getInt(STATE_DAY)
            }
            if (state.containsKey(STATE_HOUR)) {
                hour = state.getInt(STATE_HOUR)
                minute = state.getInt(STATE_MINUTE)
            }
        }
        renderWhen()
        binding.editTitle.doAfterTextChanged { clearTitleError() }
        binding.btnDate.setOnClickListener { pickDate() }
        binding.btnTime.setOnClickListener { pickTime() }
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }
        binding.btnSave.setOnClickListener { save() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        year?.let {
            outState.putInt(STATE_YEAR, it)
            outState.putInt(STATE_MONTH, month ?: 0)
            outState.putInt(STATE_DAY, day ?: 1)
        }
        hour?.let {
            outState.putInt(STATE_HOUR, it)
            outState.putInt(STATE_MINUTE, minute ?: 0)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun pickDate() {
        val now = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
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
            requireContext(),
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
        if (_binding == null) return
        val chosenYear = year
        val chosenMonth = month
        val chosenDay = day
        binding.txtDate.text = if (chosenYear == null || chosenMonth == null || chosenDay == null) {
            getString(R.string.agenda_choose_date)
        } else {
            String.format(Locale.US, "%02d/%02d/%04d", chosenDay, chosenMonth + 1, chosenYear)
        }
        val chosenHour = hour
        val chosenMinute = minute
        binding.txtTime.text = if (chosenHour == null || chosenMinute == null) {
            getString(R.string.agenda_choose_time)
        } else {
            String.format(Locale.US, "%02d:%02d hs", chosenHour, chosenMinute)
        }
    }

    private fun clearTitleError() {
        if (_binding == null) return
        if (AgendaRules.titleProblem(binding.editTitle.text?.toString().orEmpty()) != null) return
        binding.inputTitle.error = null
        if (binding.txtFormError.text?.toString() == AgendaRules.titleProblem("")) {
            binding.txtFormError.isVisible = false
        }
    }

    private fun save() {
        val title = binding.editTitle.text?.toString().orEmpty()
        val titleProblem = AgendaRules.titleProblem(title)
        val whenProblem = AgendaRules.whenProblem(year != null, hour != null)
        val problem = titleProblem ?: whenProblem
        binding.inputTitle.error = titleProblem
        binding.txtFormError.isVisible = problem != null
        binding.txtFormError.text = problem
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
        viewModel.addAppointment(
            requireContext(),
            Appointment(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                date = AgendaRules.storedDate(millis),
                notes = binding.editNotes.text?.toString().orEmpty().trim(),
                atMillis = millis,
            ),
        )
        findNavController().popBackStack()
    }

    companion object {
        private const val STATE_YEAR = "agenda_year"
        private const val STATE_MONTH = "agenda_month"
        private const val STATE_DAY = "agenda_day"
        private const val STATE_HOUR = "agenda_hour"
        private const val STATE_MINUTE = "agenda_minute"
    }
}
