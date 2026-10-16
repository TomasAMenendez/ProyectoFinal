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
    NONE,
}

data class SettingsSnapshot(
    val load: LoadState,
    val volume: Int?,
    val brightness: Int?,
    val canApply: Boolean,
    val night: Boolean?,
    val controlsEnabled: Boolean,
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
        )
    }
}
