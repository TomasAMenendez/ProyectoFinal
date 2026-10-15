package com.example.aplicacionparacelular.ui.onboarding

import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.IncludeChildProfileFieldsBinding
import com.example.aplicacionparacelular.onboarding.ChildProfile
import com.example.aplicacionparacelular.onboarding.FormDirtyTracker
import com.example.aplicacionparacelular.ui.character.TeoCostumeCatalog
import com.example.aplicacionparacelular.ui.dashboard.TeoCharacterPresenter
import org.json.JSONObject

/** Une los campos compartidos de Datos del chico con el modelo. */
class ChildProfileForm(private val form: IncludeChildProfileFieldsBinding) {

    private val dirtyTracker = FormDirtyTracker()
    private var applying = false
    private var selectedCostume = ChildProfile.DEFAULT_COSTUME
    private var picker: AlertDialog? = null
    val hasChanges: Boolean get() = dirtyTracker.dirty

    fun bind(onChanged: () -> Unit) {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (applying) return
                dirtyTracker.onUserEdit()
                onChanged()
            }
        }
        form.editChildName.addTextChangedListener(watcher)
        form.editChildInterests.addTextChangedListener(watcher)
        form.editChildExtra.addTextChangedListener(watcher)
        form.toggleGender.setOnCheckedStateChangeListener { _, _ ->
            if (applying) return@setOnCheckedStateChangeListener
            dirtyTracker.onUserEdit()
            onChanged()
        }
        form.btnCostume.setOnClickListener {
            picker?.dismiss()
            picker = CostumePicker.show(form.root.context, selectedCostume) { costume ->
                selectedCostume = costume
                renderCostume()
                dirtyTracker.onUserEdit()
                onChanged()
            }
        }
        form.root.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: android.view.View) = Unit
            override fun onViewDetachedFromWindow(v: android.view.View) {
                picker?.dismiss()
                picker = null
            }
        })
        renderCostume()
        refreshSaveEnabled()
    }

    fun populate(profile: ChildProfile) {
        if (!dirtyTracker.shouldApplyRemote()) return
        dirtyTracker.beginApply()
        applying = true
        try {
            form.editChildName.setText(profile.name)
            form.editChildInterests.setText(profile.interests)
            form.editChildExtra.setText(profile.extra)
            val genderId = when (profile.gender) {
                "nene" -> R.id.btn_gender_nene
                "nena" -> R.id.btn_gender_nena
                "no_decir" -> R.id.btn_gender_no_decir
                else -> ViewIdNone
            }
            if (genderId != ViewIdNone) {
                form.toggleGender.check(genderId)
            } else {
                form.toggleGender.clearCheck()
            }
            selectedCostume = TeoCostumeCatalog.find(profile.costume).id
            renderCostume()
            refreshSaveEnabled()
        } finally {
            dirtyTracker.endApply()
            applying = false
        }
    }

    fun markClean() {
        dirtyTracker.markClean()
    }

    fun restoreDraft(profile: ChildProfile) {
        populate(profile)
        dirtyTracker.markDirty()
    }

    fun read(): ChildProfile = ChildProfile(
        name = form.editChildName.text?.toString().orEmpty(),
        gender = genderValue(),
        interests = form.editChildInterests.text?.toString().orEmpty(),
        costume = selectedCostume,
        extra = form.editChildExtra.text?.toString().orEmpty(),
    )

    fun payload(): JSONObject {
        val clean = read().sanitize()
        return JSONObject().apply {
            put("name", clean.name)
            put("gender", clean.gender)
            put("interests", clean.interests)
            put("costume", clean.costume)
            put("extra", clean.extra)
        }
    }

    fun refreshSaveEnabled() {
        form.btnSave.isEnabled = read().canSubmit
    }

    private fun genderValue(): String = when (form.toggleGender.checkedChipId) {
        R.id.btn_gender_nene -> "nene"
        R.id.btn_gender_nena -> "nena"
        R.id.btn_gender_no_decir -> "no_decir"
        else -> ""
    }

    private fun renderCostume() {
        val entry = TeoCostumeCatalog.find(selectedCostume)
        form.txtCostume.setText(entry.labelRes)
        TeoCharacterPresenter.applyFront(form.imgCostume, entry.id)
        form.btnCostume.contentDescription = form.root.context.getString(
            R.string.profile_costume_selected, form.root.context.getString(entry.labelRes),
        )
    }

    companion object {
        private const val ViewIdNone = -1
    }
}
