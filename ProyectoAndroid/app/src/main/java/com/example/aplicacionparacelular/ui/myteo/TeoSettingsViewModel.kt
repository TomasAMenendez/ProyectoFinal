package com.example.aplicacionparacelular.ui.myteo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.aplicacionparacelular.myteo.LoadState
import com.example.aplicacionparacelular.myteo.SettingsSession
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotConnectionManager

enum class SettingsNotice {
    LOADING,
    FAILED,
    APPLIED,
    APPLY_ERROR,
    NIGHT_ON,
    NIGHT_OFF,
    NIGHT_ERROR,
    PLAYTIME_APPLIED,
    PLAYTIME_ERROR,
    VOICE_APPLIED,
    VOICE_ERROR,
    LOCAL_VOICE_ON,
    LOCAL_VOICE_OFF,
    LOCAL_VOICE_ERROR,
    NONE,
}

data class SettingsSnapshot(
    val load: LoadState,
    val volume: Int?,
    val brightness: Int?,
    val canApply: Boolean,
    val night: Boolean?,
    val controlsEnabled: Boolean,
    val playtimeMinutes: Int?,
    val playtimeField: String,
    val canApplyPlaytime: Boolean,
    val playtimeEnabled: Boolean,
    val voice: String?,
    val voiceEnabled: Boolean,
    val localVoice: Boolean?,
    val localVoiceEnabled: Boolean,
)

class TeoSettingsViewModel : ViewModel() {
    private val session = SettingsSession()
    private val _snapshot = MutableLiveData(current())
    val snapshot: LiveData<SettingsSnapshot> = _snapshot
    private val _notice = MutableLiveData(SettingsNotice.LOADING)
    val notice: LiveData<SettingsNotice> = _notice

    fun refresh() {
        RobotConnectionManager.fetchStatus { result ->
            when (result) {
                is ApiResult.Success -> session.onStatus(result.data)
                is ApiResult.Error -> session.onStatus(null)
            }
            if (_notice.value == SettingsNotice.LOADING || _notice.value == SettingsNotice.FAILED) {
                _notice.value = if (session.load == LoadState.READY) SettingsNotice.NONE else SettingsNotice.FAILED
            }
            publish()
        }
    }

    fun onSlider(volume: Int, brightness: Int) {
        session.edit(volume, brightness)
        publish()
    }

    fun apply() {
        val body = session.beginApply() ?: return
        publish()
        val volume = body.getInt("volume_limit")
        val brightness = (body.getDouble("brightness") * 100).toInt()
        RobotConnectionManager.updateSensory(volume, brightness) { result ->
            when (result) {
                is ApiResult.Success -> {
                    session.confirmApply(result.data)
                    _notice.value = SettingsNotice.APPLIED
                    RobotConnectionManager.fetchStatus { status ->
                        if (status is ApiResult.Success) session.onStatus(status.data)
                        publish()
                    }
                }
                is ApiResult.Error -> {
                    session.failApply()
                    _notice.value = SettingsNotice.APPLY_ERROR
                    publish()
                }
            }
        }
    }

    fun onNight(enabled: Boolean): Boolean {
        val send = session.beginNight(enabled) ?: return false
        publish()
        RobotConnectionManager.setNightMode(send) { result ->
            when (result) {
                is ApiResult.Success -> {
                    session.confirmNight(send)
                    _notice.value = if (send) SettingsNotice.NIGHT_ON else SettingsNotice.NIGHT_OFF
                    RobotConnectionManager.fetchStatus { status ->
                        if (status is ApiResult.Success) session.onStatus(status.data)
                        publish()
                    }
                }
                is ApiResult.Error -> {
                    session.failNight()
                    _notice.value = SettingsNotice.NIGHT_ERROR
                    publish()
                }
            }
        }
        return true
    }

    fun onVoice(voice: String): Boolean {
        val body = session.beginVoice(voice) ?: return false
        val sent = body.getString("tts_voice")
        publish()
        RobotConnectionManager.updateVoice(sent) { result ->
            when (result) {
                is ApiResult.Success -> {
                    val confirmed = session.confirmVoice(result.data)
                    _notice.value = if (confirmed) SettingsNotice.VOICE_APPLIED else SettingsNotice.VOICE_ERROR
                    RobotConnectionManager.fetchStatus { status ->
                        if (status is ApiResult.Success) session.onStatus(status.data)
                        publish()
                    }
                }
                is ApiResult.Error -> {
                    session.failVoice()
                    _notice.value = SettingsNotice.VOICE_ERROR
                    publish()
                }
            }
        }
        return true
    }

    fun onLocalVoice(enabled: Boolean): Boolean {
        val body = session.beginLocalVoice(enabled) ?: return false
        val sent = body.getBoolean("tts_local_only")
        publish()
        RobotConnectionManager.updateLocalVoice(sent) { result ->
            when (result) {
                is ApiResult.Success -> {
                    val confirmed = session.confirmLocalVoice(result.data)
                    _notice.value = when {
                        !confirmed -> SettingsNotice.LOCAL_VOICE_ERROR
                        sent -> SettingsNotice.LOCAL_VOICE_ON
                        else -> SettingsNotice.LOCAL_VOICE_OFF
                    }
                    RobotConnectionManager.fetchStatus { status ->
                        if (status is ApiResult.Success) session.onStatus(status.data)
                        publish()
                    }
                }
                is ApiResult.Error -> {
                    session.failLocalVoice()
                    _notice.value = SettingsNotice.LOCAL_VOICE_ERROR
                    publish()
                }
            }
        }
        return true
    }

    fun onPlaytime(raw: String) {
        session.editPlaytime(raw)
        publish()
    }

    fun applyPlaytime() {
        val body = session.beginPlaytimeApply() ?: return
        val sent = body.getInt("playtime_limit_minutes")
        publish()
        RobotConnectionManager.updatePlaytime(sent) { result ->
            when (result) {
                is ApiResult.Success -> {
                    val confirmed = session.confirmPlaytime(result.data)
                    _notice.value = if (confirmed) {
                        SettingsNotice.PLAYTIME_APPLIED
                    } else {
                        SettingsNotice.PLAYTIME_ERROR
                    }
                    RobotConnectionManager.fetchStatus { status ->
                        if (status is ApiResult.Success) session.onStatus(status.data)
                        publish()
                    }
                }
                is ApiResult.Error -> {
                    session.failPlaytime()
                    _notice.value = SettingsNotice.PLAYTIME_ERROR
                    publish()
                }
            }
        }
    }

    private fun publish() {
        _snapshot.value = current()
    }

    private fun current(): SettingsSnapshot {
        val ready = session.load == LoadState.READY
        return SettingsSnapshot(
            load = session.load,
            volume = session.draft?.volume,
            brightness = session.draft?.brightness,
            canApply = session.canApply(),
            night = session.displayedNight,
            controlsEnabled = ready && !session.applying,
            playtimeMinutes = session.playtimeConfirmed,
            playtimeField = session.playtimeText,
            canApplyPlaytime = session.canApplyPlaytime(),
            playtimeEnabled = session.playtimeEnabled(),
            voice = session.displayedVoice,
            voiceEnabled = session.voiceEnabled(),
            localVoice = session.displayedLocalVoice,
            localVoiceEnabled = session.localVoiceEnabled(),
        )
    }
}
