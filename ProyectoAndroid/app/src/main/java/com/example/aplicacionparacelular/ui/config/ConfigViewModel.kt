package com.example.aplicacionparacelular.ui.config

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.onboarding.ChildProfile
import org.json.JSONObject

class ConfigViewModel : ViewModel() {

    private val _volumeLimit = MutableLiveData(100)
    val volumeLimit: LiveData<Int> = _volumeLimit

    private val _brightness = MutableLiveData(100)
    val brightness: LiveData<Int> = _brightness

    private val _nightMode = MutableLiveData(false)
    val nightMode: LiveData<Boolean> = _nightMode

    private val _playtimeLimit = MutableLiveData(0)
    val playtimeLimit: LiveData<Int> = _playtimeLimit

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> = _statusMessage

    private val _childProfile = MutableLiveData<ChildProfile>()
    val childProfile: LiveData<ChildProfile> = _childProfile

    private val _savedChildProfile = MutableLiveData<ChildProfile>()
    val savedChildProfile: LiveData<ChildProfile> = _savedChildProfile

    fun loadFromRobot() {
        RobotConnectionManager.executeAsync({ com.example.aplicacionparacelular.network.RobotApiClient.getStatus() }) { result ->
            when (result) {
                is ApiResult.Success -> {
                    _volumeLimit.value = result.data.optInt("volume_limit", 100)
                    _brightness.value = (result.data.optDouble("brightness", 1.0) * 100).toInt()
                    _nightMode.value = result.data.optBoolean("night_mode", false)
                    _playtimeLimit.value = result.data.optInt("playtime_limit_minutes", 0)
                }
                is ApiResult.Error -> { /* Keep defaults */ }
            }
        }
    }

    fun setVolumeLimit(value: Int) {
        _volumeLimit.value = value
    }

    fun setBrightness(value: Int) {
        _brightness.value = value
    }

    fun setPlaytimeLimit(value: Int) {
        _playtimeLimit.value = value
    }

    fun applyConfig() {
        val vol = _volumeLimit.value ?: 100
        val bright = (_brightness.value ?: 100) / 100f
        val playtime = _playtimeLimit.value ?: 0
        RobotConnectionManager.updateConfig(vol, bright, playtime) { result ->
            _statusMessage.value = when (result) {
                is ApiResult.Success -> "✅ Configuración aplicada"
                is ApiResult.Error -> "⚠ Error: ${result.message}"
            }
        }
    }

    fun setNightMode(enabled: Boolean) {
        if (enabled == (_nightMode.value ?: false)) return
        RobotConnectionManager.setNightMode(enabled) { result ->
            when (result) {
                is ApiResult.Success -> {
                    _nightMode.value = enabled
                    _statusMessage.value = if (enabled) "🌙 Modo noche activado" else "☀ Modo noche desactivado"
                }
                is ApiResult.Error -> {
                    _statusMessage.value = "⚠ Error: ${result.message}"
                }
            }
        }
    }

    fun toggleNightMode() {
        setNightMode(!(_nightMode.value ?: false))
    }

    fun loadChildProfile() {
        RobotConnectionManager.fetchChildProfile { result ->
            when (result) {
                is ApiResult.Success -> _childProfile.value = ChildProfile.parse(result.data)
                is ApiResult.Error -> {
                    _statusMessage.value = "⚠ No pude cargar los datos del chico."
                }
            }
        }
    }

    fun saveChildProfile(body: JSONObject) {
        RobotConnectionManager.saveChildProfile(body) { result ->
            when (result) {
                is ApiResult.Success -> {
                    val profile = ChildProfile.parse(result.data)
                    _savedChildProfile.value = profile
                    _statusMessage.value = "Listo, ya lo guardé"
                }
                is ApiResult.Error -> {
                    _statusMessage.value = "⚠ No pude guardar. Revisá la conexión e intentá de nuevo."
                }
            }
        }
    }
}
