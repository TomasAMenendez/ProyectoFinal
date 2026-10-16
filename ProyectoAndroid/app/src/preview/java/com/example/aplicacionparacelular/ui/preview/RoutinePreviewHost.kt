package com.example.aplicacionparacelular.ui.preview

import android.app.TimePickerDialog
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentRoutineEditorBinding
import com.example.aplicacionparacelular.databinding.FragmentRoutinesBinding
import com.example.aplicacionparacelular.ui.routines.RoutineAdapter
import com.example.aplicacionparacelular.ui.routines.RoutineDeleteDialog
import com.example.aplicacionparacelular.ui.routines.PictogramCatalog
import com.example.aplicacionparacelular.ui.routines.PictogramPicker
import com.example.aplicacionparacelular.ui.routines.RoutineDraft
import com.example.aplicacionparacelular.ui.routines.RoutineFields
import com.example.aplicacionparacelular.ui.routines.RoutineItem
import com.example.aplicacionparacelular.ui.routines.RoutineOutcome

class RoutinePreviewHost(
    private val activity: android.app.Activity,
    private val onAlerts: () -> Unit,
    private val onConfig: () -> Unit,
    private val onEditorVisible: (Boolean) -> Unit,
) {
    private val routines = RoutinePreviewSamples.items().toMutableList()
    private val list = FragmentRoutinesBinding.inflate(activity.layoutInflater)
    private val editor = FragmentRoutineEditorBinding.inflate(activity.layoutInflater)
    private var editing: RoutineItem? = null
    private var pictogramId: String? = null
    private val adapter = RoutineAdapter(
        onToggle = { item ->
            val index = routines.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                routines[index] = routines[index].copy(enabled = !routines[index].enabled)
                showList()
            }
        },
        onEdit = { item -> showEditor(item) },
        onDelete = { item ->
            RoutineDeleteDialog.show(activity, item.name) {
                routines.removeAll { it.id == item.id }
                showList()
            }
        },
    )

    val root: FrameLayout = FrameLayout(activity).apply {
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )
        addView(list.root)
        addView(editor.root)
    }

    val isEditing: Boolean
        get() = editor.root.visibility == View.VISIBLE

    init {
        list.recyclerRoutines.layoutManager = LinearLayoutManager(activity)
        list.recyclerRoutines.adapter = adapter
        list.btnAdd.setOnClickListener { showEditor(null) }
        list.btnAlertsBell.setOnClickListener { onAlerts() }
        list.btnMyTeo.setOnClickListener { onConfig() }
        editor.btnBack.setOnClickListener { showList() }
        editor.btnCancel.setOnClickListener { showList() }
        editor.btnSave.setOnClickListener { savePreview() }
        editor.btnPictogram.setOnClickListener {
            PictogramPicker.show(activity, pictogramId) { chosen ->
                pictogramId = chosen
                renderPictogram()
            }
        }
        val openTime = View.OnClickListener { showTimePicker() }
        editor.editTime.setOnClickListener(openTime)
        editor.inputTime.setEndIconOnClickListener { showTimePicker() }
        showList()
    }

    fun showList() {
        adapter.submitList(routines.toList())
        val empty = routines.isEmpty()
        list.txtEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        list.recyclerRoutines.visibility = if (empty) View.GONE else View.VISIBLE
        editor.root.visibility = View.GONE
        list.root.visibility = View.VISIBLE
        onEditorVisible(false)
    }

    private fun showEditor(item: RoutineItem?) {
        editing = item
        val editMode = item != null
        editor.txtTitle.setText(if (editMode) R.string.routines_edit_title else R.string.routines_new_title)
        editor.btnSave.setText(if (editMode) R.string.routines_save_changes else R.string.routines_save)
        editor.txtCreateNote.visibility = if (editMode) View.GONE else View.VISIBLE
        editor.txtHelp.visibility = if (editMode) View.GONE else View.VISIBLE
        editor.inputName.error = null
        editor.txtFormError.visibility = View.GONE
        editor.editName.setText(item?.name.orEmpty())
        editor.editTime.setText(item?.time ?: "08:00")
        editor.editMessage.setText(item?.reminderMessage.orEmpty())
        pictogramId = item?.pictogramId
        renderPictogram()
        list.root.visibility = View.GONE
        editor.root.visibility = View.VISIBLE
        onEditorVisible(true)
    }

    private fun showTimePicker() {
        val current = editor.editTime.text?.toString().orEmpty().split(":")
        TimePickerDialog(
            activity,
            { _, hour, minute ->
                editor.editTime.setText(String.format("%02d:%02d", hour, minute))
            },
            current.getOrNull(0)?.toIntOrNull() ?: 8,
            current.getOrNull(1)?.toIntOrNull() ?: 0,
            true,
        ).show()
    }

    private fun savePreview() {
        val fields = RoutineFields(
            name = editor.editName.text?.toString().orEmpty(),
            time = editor.editTime.text?.toString().orEmpty(),
            message = editor.editMessage.text?.toString().orEmpty(),
            pictogramId = pictogramId,
        )
        val current = editing
        val outcome = if (current == null) {
            RoutineDraft.forCreate(fields, RoutineDraft.newId(fields.name))
        } else {
            RoutineDraft.forEdit(current, fields)
        }
        when (outcome) {
            is RoutineOutcome.Invalid -> {
                editor.inputName.error = outcome.nameError
                editor.txtFormError.visibility = if (outcome.formError == null) View.GONE else View.VISIBLE
                editor.txtFormError.text = outcome.formError
            }
            is RoutineOutcome.Ready -> {
                if (current == null) {
                    routines.add(outcome.item)
                } else {
                    val index = routines.indexOfFirst { it.id == current.id }
                    if (index >= 0) routines[index] = outcome.item
                }
                showList()
            }
        }
    }

    private fun renderPictogram() {
        val entry = PictogramCatalog.find(pictogramId)
        val image = editor.imgPictogram
        if (entry == null) {
            image.setImageDrawable(null)
            image.visibility = View.GONE
            editor.txtPictogram.setText(
                if (pictogramId.isNullOrBlank()) {
                    com.example.aplicacionparacelular.R.string.routines_pictogram_none
                } else {
                    com.example.aplicacionparacelular.R.string.routines_pictogram_missing
                },
            )
        } else {
            image.setImageResource(entry.drawableRes)
            image.visibility = View.VISIBLE
            image.scaleType = ImageView.ScaleType.FIT_CENTER
            editor.txtPictogram.setText(entry.labelRes)
        }
    }
}
