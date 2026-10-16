package com.example.aplicacionparacelular.ui.character

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.aplicacionparacelular.onboarding.ChildProfile
import org.json.JSONObject

/** Último disfraz confirmado por TEO. La vista previa mantiene su propio estado. */
object TeoAppearance {
    private var preferences: SharedPreferences? = null
    private val selected = MutableLiveData(ChildProfile.DEFAULT_COSTUME)
    val costume: LiveData<String> = selected

    fun init(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences("teo_appearance", Context.MODE_PRIVATE)
        selected.value = TeoCostumeCatalog.find(preferences?.getString("costume", null)).id
    }

    fun acceptConfirmed(profile: JSONObject) {
        val id = TeoAppearanceRules.confirmedCostume(profile) ?: return
        if (selected.value == id) return
        preferences?.edit()?.putString("costume", id)?.apply()
        selected.value = id
    }
}
