package com.example.aplicacionparacelular.onboarding

import org.json.JSONObject

enum class Gender(val value: String) {
    NENE("nene"),
    NENA("nena"),
    NO_DECIR("no_decir"),
}

typealias ChildGender = Gender

data class ChildProfile(
    val name: String = "",
    val gender: String = "",
    val interests: String = "",
    val costume: String = DEFAULT_COSTUME,
    val extra: String = "",
    val complete: Boolean = false,
) {
    val canSubmit: Boolean
        get() = name.trim().isNotEmpty() && gender in GENDERS

    fun sanitize(): ChildProfile {
        val sanitizedCostume = costume.trim().takeIf { it in COSTUMES } ?: DEFAULT_COSTUME
        return copy(
            name = name.trim().take(NAME_MAX_LEN),
            gender = gender.trim(),
            interests = interests.trim().take(TEXT_MAX_LEN),
            costume = sanitizedCostume,
            extra = extra.trim().take(TEXT_MAX_LEN),
        )
    }

    companion object {
        const val NAME_MAX_LEN = 40
        const val TEXT_MAX_LEN = 600
        const val DEFAULT_COSTUME = "oso"

        val GENDERS = setOf("nene", "nena", "no_decir")

        val COSTUMES = listOf(
            "oso",
            "perro",
            "oveja",
            "dinosaurio",
            "chancho",
            "tiburon",
            "stitch",
            "spiderman",
            "chimuelo",
        )

        fun parse(json: JSONObject): ChildProfile = ChildProfile(
            name = json.optString("name", ""),
            gender = json.optString("gender", ""),
            interests = json.optString("interests", ""),
            costume = json.optString("costume", DEFAULT_COSTUME),
            extra = json.optString("extra", ""),
            complete = json.optBoolean("complete", false),
        ).sanitize()
    }
}
