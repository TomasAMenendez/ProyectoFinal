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

    fun parseMinutes(raw: String): Int? {
        val text = raw.trim()
        if (text.isEmpty() || text.any { !it.isDigit() }) return null
        val minutes = text.toIntOrNull() ?: return null
        if (minutes !in 0..240) return null
        return minutes
    }

    fun readPlaytime(status: JSONObject): Int? {
        if (!status.has("playtime_limit_minutes") || status.isNull("playtime_limit_minutes")) return null
        val raw = status.opt("playtime_limit_minutes")
        val minutes = when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        } ?: return null
        if (minutes !in 0..240) return null
        return minutes
    }

    /** Solo la voz. Volumen, brillo y tiempo de juego quedan fuera. */
    fun voiceBody(voice: String): JSONObject? {
        if (voice != "topa" && voice != "micaela") return null
        return JSONObject().put("tts_voice", voice)
    }

    /** Voz que devolvió el peluche. Un valor ausente o inválido no cuenta como confirmación. */
    fun echoVoice(echo: JSONObject): String? {
        if (!echo.has("tts_voice") || echo.isNull("tts_voice")) return null
        val raw = echo.optString("tts_voice")
        return if (raw == "topa" || raw == "micaela") raw else null
    }

    /** Solo el interruptor de voz local. */
    fun localVoiceBody(enabled: Boolean): JSONObject =
        JSONObject().put("tts_local_only", enabled)

    /** Un valor ausente o que no es booleano no cuenta como confirmación. */
    fun echoLocalVoice(echo: JSONObject): Boolean? {
        if (!echo.has("tts_local_only") || echo.isNull("tts_local_only")) return null
        val raw = echo.opt("tts_local_only")
        return if (raw is Boolean) raw else null
    }

    /** Si el estado no trae el interruptor, queda apagado: intenta internet. */
    fun readLocalVoice(status: JSONObject): Boolean = status.opt("tts_local_only") == true

    /** Si el estado no trae una voz válida, la elección mostrada es Topa. */
    fun readVoice(status: JSONObject): String {
        val raw = status.optString("tts_voice", "")
        return if (raw == "micaela") "micaela" else "topa"
    }

    /** Solo el total de juego. Volumen, brillo y pulsador quedan fuera. */
    fun playtimeBody(minutes: Int): JSONObject? {
        if (minutes !in 0..240) return null
        return JSONObject().put("playtime_limit_minutes", minutes)
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
    var voiceConfirmed: String? = null
        private set
    private var pendingVoice: String? = null
    private var voiceBusy: Boolean = false

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
        readPlaytime(status)
        readVoice(status)
        readLocalVoice(status)
    }

    private fun readVoice(status: JSONObject?) {
        if (status == null || load != LoadState.READY) {
            if (!voiceBusy) {
                voiceConfirmed = null
            }
            return
        }
        if (!voiceBusy) {
            voiceConfirmed = TeoSettingsRules.readVoice(status)
        }
    }

    fun beginVoice(voice: String): JSONObject? {
        if (voiceConfirmed == null || voiceBusy || voice == voiceConfirmed) return null
        val body = TeoSettingsRules.voiceBody(voice) ?: return null
        pendingVoice = voice
        voiceBusy = true
        return body
    }

    fun confirmVoice(echo: JSONObject?): Boolean {
        val sent = pendingVoice
        voiceBusy = false
        pendingVoice = null
        val accepted = echo?.let { TeoSettingsRules.echoVoice(it) }
        if (sent == null || accepted != sent) return false
        voiceConfirmed = accepted
        return true
    }

    fun failVoice() {
        voiceBusy = false
        pendingVoice = null
    }

    val displayedVoice: String? get() = pendingVoice ?: voiceConfirmed

    var localVoiceConfirmed: Boolean? = null
        private set
    private var pendingLocalVoice: Boolean? = null
    private var localVoiceBusy: Boolean = false

    val displayedLocalVoice: Boolean? get() = pendingLocalVoice ?: localVoiceConfirmed

    private fun readLocalVoice(status: JSONObject?) {
        if (status == null || load != LoadState.READY) {
            if (!localVoiceBusy) localVoiceConfirmed = null
            return
        }
        if (!localVoiceBusy) {
            localVoiceConfirmed = TeoSettingsRules.readLocalVoice(status)
        }
    }

    fun beginLocalVoice(enabled: Boolean): JSONObject? {
        if (localVoiceConfirmed == null || localVoiceBusy || enabled == localVoiceConfirmed) return null
        pendingLocalVoice = enabled
        localVoiceBusy = true
        return TeoSettingsRules.localVoiceBody(enabled)
    }

    fun confirmLocalVoice(echo: JSONObject?): Boolean {
        val sent = pendingLocalVoice
        localVoiceBusy = false
        pendingLocalVoice = null
        val accepted = echo?.let { TeoSettingsRules.echoLocalVoice(it) }
        if (sent == null || accepted != sent) return false
        localVoiceConfirmed = accepted
        return true
    }

    fun failLocalVoice() {
        localVoiceBusy = false
        pendingLocalVoice = null
    }

    fun localVoiceEnabled(): Boolean =
        load == LoadState.READY && localVoiceConfirmed != null && !localVoiceBusy

    fun voiceEnabled(): Boolean =
        load == LoadState.READY && voiceConfirmed != null && !voiceBusy

    private fun readPlaytime(status: JSONObject?) {
        if (status == null) {
            if (!playtimeApplying && playtimeDraft == playtimeConfirmed) {
                playtimeConfirmed = null
                playtimeDraft = null
                playtimeText = ""
            }
            return
        }
        val limit = TeoSettingsRules.readPlaytime(status) ?: return
        if (!playtimeApplying && playtimeDraft == playtimeConfirmed) {
            playtimeConfirmed = limit
            playtimeDraft = limit
            playtimeText = limit.toString()
        } else {
            playtimeConfirmed = limit
        }
    }

    fun editPlaytime(raw: String) {
        if (load != LoadState.READY || playtimeConfirmed == null || playtimeApplying) return
        playtimeText = raw
        playtimeDraft = TeoSettingsRules.parseMinutes(raw)
    }

    fun canApplyPlaytime(): Boolean {
        val draft = playtimeDraft
        return load == LoadState.READY && !playtimeApplying && draft != null && draft != playtimeConfirmed
    }

    fun playtimeEnabled(): Boolean =
        load == LoadState.READY && playtimeConfirmed != null && !playtimeApplying

    fun beginPlaytimeApply(): JSONObject? {
        val minutes = playtimeDraft ?: return null
        if (!canApplyPlaytime()) return null
        playtimeApplying = true
        pendingPlaytime = minutes
        return TeoSettingsRules.playtimeBody(minutes)
    }

    fun confirmPlaytime(echo: JSONObject?): Boolean {
        val sent = pendingPlaytime
        playtimeApplying = false
        pendingPlaytime = null
        val accepted = echo?.let { TeoSettingsRules.readPlaytime(it) }
        if (sent == null || accepted != sent) return false
        playtimeConfirmed = accepted
        playtimeDraft = accepted
        playtimeText = accepted.toString()
        return true
    }

    fun failPlaytime() {
        playtimeApplying = false
        pendingPlaytime = null
    }

    fun edit(volume: Int, brightness: Int) {
        if (load != LoadState.READY) return
        draft = SensoryDraft(TeoSettingsRules.snap(volume), TeoSettingsRules.snap(brightness))
        editing = draft != confirmed
    }

    fun canApply(): Boolean = load == LoadState.READY && editing && draft != null && !applying

    var playtimeConfirmed: Int? = null
        private set
    var playtimeText: String = ""
        private set
    private var playtimeDraft: Int? = null
    private var playtimeApplying: Boolean = false
    private var pendingPlaytime: Int? = null

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
