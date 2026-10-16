package com.example.aplicacionparacelular.myteo

import org.json.JSONObject

data class SensoryDraft(val volume: Int, val brightness: Int)

enum class LoadState { LOADING, READY, FAILED }

object TeoSettingsRules {
    fun snap(value: Int): Int = ((value / 5) * 5).coerceIn(0, 100)

    fun readSensory(status: JSONObject): SensoryDraft? {
        if (!status.has("volume_limit") || !status.has("brightness")) return null
        val volume = snap(status.optInt("volume_limit"))
        val brightness = snap((status.optDouble("brightness") * 100).toInt())
        return SensoryDraft(volume, brightness)
    }

    fun readNight(status: JSONObject): Boolean? {
        if (!status.has("night_mode")) return null
        return status.optBoolean("night_mode")
    }

    /** Solo volumen y brillo. El tiempo de uso y el pulsador quedan fuera. */
    fun sensoryBody(volume: Int, brightnessPercent: Int): JSONObject {
        return JSONObject().apply {
            put("volume_limit", snap(volume))
            put("brightness", snap(brightnessPercent) / 100.0)
        }
    }
}

class SettingsSession {
    var load: LoadState = LoadState.LOADING
        private set
    var confirmed: SensoryDraft? = null
        private set
    var draft: SensoryDraft? = null
        private set
    var editing: Boolean = false
        private set
    var nightConfirmed: Boolean? = null
        private set
    var pendingNight: Boolean? = null
        private set
    var nightBusy: Boolean = false
        private set

    val displayedNight: Boolean? get() = pendingNight ?: nightConfirmed

    fun onStatus(status: JSONObject?) {
        val sensory = status?.let { TeoSettingsRules.readSensory(it) }
        val night = status?.let { TeoSettingsRules.readNight(it) }
        if (sensory == null) {
            if (!editing) {
                load = LoadState.FAILED
                confirmed = null
                draft = null
            }
        } else if (!editing) {
            load = LoadState.READY
            confirmed = sensory
            draft = sensory
        } else {
            load = LoadState.READY
            confirmed = sensory
        }
        if (night != null && !nightBusy) nightConfirmed = night
    }

    fun edit(volume: Int, brightness: Int) {
        if (load != LoadState.READY) return
        draft = SensoryDraft(TeoSettingsRules.snap(volume), TeoSettingsRules.snap(brightness))
        editing = draft != confirmed
    }

    fun canApply(): Boolean = load == LoadState.READY && editing && draft != null && !applying

    var applying: Boolean = false
        private set

    fun beginApply(): JSONObject? {
        if (!canApply() || applying) return null
        applying = true
        val current = draft ?: return null
        return TeoSettingsRules.sensoryBody(current.volume, current.brightness)
    }

    fun confirmApply(echo: JSONObject?) {
        applying = false
        val accepted = echo?.let { TeoSettingsRules.readSensory(it) } ?: draft
        confirmed = accepted
        draft = accepted
        editing = false
        if (accepted != null) load = LoadState.READY
    }

    fun failApply() {
        applying = false
    }

    fun beginNight(enabled: Boolean): Boolean? {
        if (nightConfirmed == null || nightBusy) return null
        if (enabled == nightConfirmed) return null
        pendingNight = enabled
        nightBusy = true
        return enabled
    }

    fun confirmNight(enabled: Boolean) {
        nightConfirmed = enabled
        pendingNight = null
        nightBusy = false
    }

    fun failNight() {
        pendingNight = null
        nightBusy = false
    }
}
