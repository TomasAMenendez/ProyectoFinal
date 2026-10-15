package com.example.aplicacionparacelular.ui.routines

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentRoutineEditorBinding
import com.example.aplicacionparacelular.ui.SystemBarInsets

class RoutineEditorFragment : Fragment() {

    private var _binding: FragmentRoutineEditorBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RoutinesViewModel
    private var editing: RoutineItem? = null
    private var pictogramId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        viewModel = ViewModelProvider(requireActivity())[RoutinesViewModel::class.java]
        _binding = FragmentRoutineEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.editorScroll)
        val editMode = arguments?.getString("mode") == "edit"
        val routineId = arguments?.getString("routineId").orEmpty()
        editing = if (editMode) viewModel.routines.value.orEmpty().firstOrNull { it.id == routineId } else null
        if (editMode && editing == null) {
            findNavController().popBackStack()
            return
        }
        binding.txtTitle.setText(if (editMode) R.string.routines_edit_title else R.string.routines_new_title)
        binding.btnSave.setText(if (editMode) R.string.routines_save_changes else R.string.routines_save)
        binding.txtCreateNote.visibility = if (editMode) View.GONE else View.VISIBLE
        binding.txtHelp.visibility = if (editMode) View.GONE else View.VISIBLE
        val current = editing
        if (savedInstanceState?.getBoolean("pictogram_kept") == true) {
            pictogramId = savedInstanceState.getString("pictogram_id")?.trim()?.ifEmpty { null }
        } else {
            pictogramId = current?.pictogramId
        }
        if (current != null) {
            binding.editName.setText(current.name)
            binding.editTime.setText(current.time)
            binding.editMessage.setText(current.reminderMessage)
        } else if (binding.editTime.text.isNullOrBlank()) {
            binding.editTime.setText("08:00")
        }
        renderPictogram()
        binding.btnPictogram.setOnClickListener {
            PictogramPicker.show(requireContext(), pictogramId) { chosen ->
                pictogramId = chosen
                renderPictogram()
            }
        }
        val openTime = View.OnClickListener { showTimePicker() }
        binding.editTime.setOnClickListener(openTime)
        binding.inputTime.setEndIconOnClickListener { showTimePicker() }
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }
        binding.btnSave.setOnClickListener { save(editMode) }
    }

    private fun showTimePicker() {
        val current = binding.editTime.text?.toString().orEmpty().split(":")
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                binding.editTime.setText(String.format("%02d:%02d", hour, minute))
            },
            current.getOrNull(0)?.toIntOrNull() ?: 8,
            current.getOrNull(1)?.toIntOrNull() ?: 0,
            true,
        ).show()
    }

    private fun save(editMode: Boolean) {
        val fields = RoutineFields(
            name = binding.editName.text?.toString().orEmpty(),
            time = binding.editTime.text?.toString().orEmpty(),
            message = binding.editMessage.text?.toString().orEmpty(),
            pictogramId = pictogramId,
        )
        val outcome = if (editMode) {
            RoutineDraft.forEdit(editing ?: return, fields)
        } else {
            RoutineDraft.forCreate(fields, RoutineDraft.newId(fields.name))
        }
        when (outcome) {
            is RoutineOutcome.Invalid -> {
                binding.inputName.error = outcome.nameError
                binding.txtFormError.visibility = if (outcome.formError == null) View.GONE else View.VISIBLE
                binding.txtFormError.text = outcome.formError
            }
            is RoutineOutcome.Ready -> {
                binding.inputName.error = null
                binding.txtFormError.visibility = View.GONE
                binding.btnSave.isEnabled = false
                viewModel.save(outcome.item, isNew = !editMode) { ok, message ->
                    if (_binding == null) return@save
                    binding.btnSave.isEnabled = true
                    if (ok) {
                        findNavController().popBackStack()
                    } else {
                        binding.txtFormError.visibility = View.VISIBLE
                        binding.txtFormError.text = message
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("pictogram_kept", true)
        outState.putString("pictogram_id", pictogramId.orEmpty())
    }

    private fun renderPictogram() {
        val entry = PictogramCatalog.find(pictogramId)
        val image = binding.imgPictogram
        if (entry == null) {
            image.setImageDrawable(null)
            image.visibility = View.GONE
            binding.txtPictogram.setText(
                if (pictogramId.isNullOrBlank()) R.string.routines_pictogram_none
                else R.string.routines_pictogram_missing,
            )
        } else {
            image.setImageResource(entry.drawableRes)
            image.visibility = View.VISIBLE
            image.scaleType = ImageView.ScaleType.FIT_CENTER
            binding.txtPictogram.setText(entry.labelRes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
