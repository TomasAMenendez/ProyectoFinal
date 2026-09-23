package com.example.aplicacionparacelular.ui.onboarding

import android.text.Editable
import android.text.TextWatcher
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.IncludeChildProfileFieldsBinding
import com.example.aplicacionparacelular.onboarding.ChildProfile
import com.example.aplicacionparacelular.onboarding.FormDirtyTracker
import org.json.JSONObject

/** Une los campos compartidos de Datos del chico con el modelo. */
class ChildProfileForm(private val form: IncludeChildProfileFieldsBinding) {

    private val dirtyTracker = FormDirtyTracker()

    fun bind(onChanged: () -> Unit) {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                dirtyTracker.onUserEdit()
                onChanged()
            }
        }
        form.editChildName.addTextChangedListener(watcher)
        form.editChildInterests.addTextChangedListener(watcher)
        form.editChildExtra.addTextChangedListener(watcher)
        form.toggleGender.addOnButtonCheckedListener { _, _, _ ->
            dirtyTracker.onUserEdit()
            onChanged()
        }
        form.chipGroupCostume.setOnCheckedStateChangeListener { _, _ ->
            dirtyTracker.onUserEdit()
            onChanged()
        }
        refreshSaveEnabled()
    }

    fun populate(profile: ChildProfile) {
        if (!dirtyTracker.shouldApplyRemote()) return
        dirtyTracker.beginApply()
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
                form.toggleGender.clearChecked()
            }
            val costumeId = costumeChipId(profile.costume)
            form.chipGroupCostume.check(costumeId)
            refreshSaveEnabled()
        } finally {
            dirtyTracker.endApply()
        }
    }

    fun markClean() {
        dirtyTracker.markClean()
    }

    fun read(): ChildProfile = ChildProfile(
        name = form.editChildName.text?.toString().orEmpty(),
        gender = genderValue(),
        interests = form.editChildInterests.text?.toString().orEmpty(),
        costume = costumeValue(),
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

    private fun genderValue(): String = when (form.toggleGender.checkedButtonId) {
        R.id.btn_gender_nene -> "nene"
        R.id.btn_gender_nena -> "nena"
        R.id.btn_gender_no_decir -> "no_decir"
        else -> ""
    }

    private fun costumeValue(): String = when (form.chipGroupCostume.checkedChipId) {
        R.id.chip_costume_oso -> "oso"
        R.id.chip_costume_perro -> "perro"
        R.id.chip_costume_oveja -> "oveja"
        R.id.chip_costume_dinosaurio -> "dinosaurio"
        R.id.chip_costume_chancho -> "chancho"
        R.id.chip_costume_tiburon -> "tiburon"
        R.id.chip_costume_stitch -> "stitch"
        R.id.chip_costume_spiderman -> "spiderman"
        R.id.chip_costume_chimuelo -> "chimuelo"
        else -> ChildProfile.DEFAULT_COSTUME
    }

    private fun costumeChipId(costume: String): Int = when (costume) {
        "perro" -> R.id.chip_costume_perro
        "oveja" -> R.id.chip_costume_oveja
        "dinosaurio" -> R.id.chip_costume_dinosaurio
        "chancho" -> R.id.chip_costume_chancho
        "tiburon" -> R.id.chip_costume_tiburon
        "stitch" -> R.id.chip_costume_stitch
        "spiderman" -> R.id.chip_costume_spiderman
        "chimuelo" -> R.id.chip_costume_chimuelo
        else -> R.id.chip_costume_oso
    }

    companion object {
        private const val ViewIdNone = -1
    }
}
