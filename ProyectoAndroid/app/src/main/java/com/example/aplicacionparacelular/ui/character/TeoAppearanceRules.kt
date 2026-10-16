package com.example.aplicacionparacelular.ui.character

import com.example.aplicacionparacelular.onboarding.ChildProfile
import org.json.JSONObject

object TeoAppearanceRules {
    fun confirmedCostume(profile: JSONObject): String? {
        if (!profile.optBoolean("complete", false)) return null
        return (profile.opt("costume") as? String)?.trim()?.takeIf { it in ChildProfile.COSTUMES }
    }
}
