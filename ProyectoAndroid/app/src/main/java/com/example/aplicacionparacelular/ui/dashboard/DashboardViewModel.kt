package com.example.aplicacionparacelular.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Observer
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotConnectionManager
import org.json.JSONObject

class DashboardViewModel : ViewModel() {
    private val gate = HomeActionGate()

    private val _interactions = MutableLiveData<String?>(null)
    val interactions: LiveData<String?> = _interactions

    private val _signal = MutableLiveData<String?>(null)
    val signal: LiveData<String?> = _signal

    private val _playback = MutableLiveData(PlaybackSnapshot(null, null, null))
    val playback: LiveData<PlaybackSnapshot> = _playback

    private val _catalogCount = MutableLiveData(-1)
    val catalogCount: LiveData<Int> = _catalogCount

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    private var readingStoryId: String? = null
    private var connected = false
    private val connectionObserver = Observer<Boolean> { value ->
        connected = value == true
    }

    init {
        RobotConnectionManager.isConnected.observeForever(connectionObserver)
    }

    override fun onCleared() {
        RobotConnectionManager.isConnected.removeObserver(connectionObserver)
        super.onCleared()
    }

    fun refreshTelemetry() {
        RobotConnectionManager.fetchTelemetryToday { result ->
            when (result) {
                is ApiResult.Success -> {
                    val summary = TodaySummary.parse(result.data)
                    _interactions.value = summary.interactions
                    _signal.value = summary.signal
                }
                is ApiResult.Error -> {
                    val absent = TodaySummary.absent()
                    _interactions.value = absent.interactions
                    _signal.value = absent.signal
                }
            }
        }
    }

    fun applyRobotStatus(status: JSONObject) {
        if (!connected) return
        val snapshot = NowPlaying.snapshot(status)
        readingStoryId = snapshot.storyId
        _playback.value = snapshot
    }

    fun refreshMusic() {
        RobotConnectionManager.fetchMusic { result ->
            if (result is ApiResult.Success) {
                val names = NowPlaying.songNames(result.data)
                _catalogCount.value = names.size
                if (readingStoryId == null && connected) {
                    val playing = NowPlaying.snapshot(result.data)
                    if (playing.songName != null || _playback.value?.storyId == null) {
                        _playback.value = playing.copy(storyTitle = null, storyId = null)
                    }
                }
            }
        }
    }

    fun requestPower(turnOn: Boolean) {
        if (!gate.tryStart(HomeAction.POWER)) return
        publishBusy()
        RobotConnectionManager.setPower(turnOn) { result ->
            when (result) {
                is ApiResult.Error -> {
                    _message.value = result.message.ifBlank { "No se pudo cambiar la actividad de TEO." }
                    finish(HomeAction.POWER)
                }
                is ApiResult.Success -> confirmPower(result.data)
            }
        }
    }

    fun playNow() {
        if (!connected) {
            _message.value = NowPlaying.NOT_CURRENT
            return
        }
        if (!gate.tryStart(HomeAction.PLAY)) return
        publishBusy()
        val storyId = readingStoryId
        if (!storyId.isNullOrBlank()) {
            RobotConnectionManager.playStory(storyId) { result ->
                confirmPlay(result, expectedStoryId = storyId, expectedSong = null)
            }
            return
        }
        RobotConnectionManager.fetchMusic { result ->
            when (result) {
                is ApiResult.Error -> {
                    _message.value = result.message.ifBlank { "No se pudo consultar la música." }
                    finish(HomeAction.PLAY)
                }
                is ApiResult.Success -> {
                    val rejected = NowPlaying.rejection(result.data)
                    if (rejected != null) {
                        _message.value = rejected
                        finish(HomeAction.PLAY)
                        return@fetchMusic
                    }
                    val names = NowPlaying.songNames(result.data)
                    _catalogCount.value = names.size
                    if (names.isEmpty()) {
                        _message.value = NowPlaying.EMPTY_CATALOG
                        finish(HomeAction.PLAY)
                        return@fetchMusic
                    }
                    val last = RobotConnectionManager.lastPlayedSong()
                    val target = last?.takeIf { it in names } ?: names.first()
                    RobotConnectionManager.playMusic(target) { playResult ->
                        confirmPlay(playResult, expectedStoryId = null, expectedSong = target)
                    }
                }
            }
        }
    }

    fun stopNow() {
        if (!connected) {
            _message.value = NowPlaying.NOT_CURRENT
            return
        }
        val before = _playback.value ?: PlaybackSnapshot(null, null, null)
        if (!before.hasPlayback) return
        if (!gate.tryStart(HomeAction.STOP)) return
        publishBusy()
        RobotConnectionManager.stopStory { }
        RobotConnectionManager.stopMusic { result ->
            when (result) {
                is ApiResult.Error -> {
                    _message.value = result.message.ifBlank { NowPlaying.NOT_CONFIRMED }
                    finish(HomeAction.STOP)
                }
                is ApiResult.Success -> {
                    val rejected = NowPlaying.rejection(result.data)
                    if (rejected != null) {
                        _message.value = rejected
                        finish(HomeAction.STOP)
                        return@stopMusic
                    }
                    confirmStop(before)
                }
            }
        }
    }

    fun celebrate() {
        if (!connected) {
            _message.value = CelebrationResult.DISCONNECTED
            return
        }
        if (!gate.tryStart(HomeAction.CELEBRATE)) return
        publishBusy()
        RobotConnectionManager.celebrate { result ->
            _message.value = CelebrationResult.from(connected, result)
            finish(HomeAction.CELEBRATE)
        }
    }

    private fun confirmPower(body: JSONObject) {
        val reading = HomeStatus.reading(body)
        if (reading.pendingWake || reading.wakeResult == "queued") {
            _message.value = "TEO se está preparando."
        }
        RobotConnectionManager.fetchStatus { result ->
            when (result) {
                is ApiResult.Error -> {
                    if (_message.value.isNullOrBlank()) {
                        _message.value = result.message.ifBlank { "No se pudo confirmar la actividad de TEO." }
                    }
                }
                is ApiResult.Success -> applyRobotStatus(result.data)
            }
            finish(HomeAction.POWER)
        }
    }

    private fun confirmPlay(result: ApiResult<JSONObject>, expectedStoryId: String?, expectedSong: String?) {
        when (result) {
            is ApiResult.Error -> {
                _message.value = result.message.ifBlank { NowPlaying.NOT_CONFIRMED }
                finish(HomeAction.PLAY)
            }
            is ApiResult.Success -> {
                val rejected = NowPlaying.rejection(result.data)
                if (rejected != null) {
                    _message.value = rejected
                    finish(HomeAction.PLAY)
                    return
                }
                RobotConnectionManager.fetchStatus { statusResult ->
                    when (statusResult) {
                        is ApiResult.Error -> _message.value = NowPlaying.NOT_CONFIRMED
                        is ApiResult.Success -> {
                            val after = NowPlaying.snapshot(statusResult.data)
                            if (NowPlaying.playConfirmed(expectedStoryId, expectedSong, after)) {
                                readingStoryId = after.storyId
                                _playback.value = after
                            } else {
                                _message.value = NowPlaying.NOT_CONFIRMED
                            }
                        }
                    }
                    finish(HomeAction.PLAY)
                }
            }
        }
    }

    private fun confirmStop(before: PlaybackSnapshot) {
        RobotConnectionManager.fetchStatus { result ->
            when (result) {
                is ApiResult.Error -> _message.value = NowPlaying.NOT_CONFIRMED
                is ApiResult.Success -> {
                    val after = NowPlaying.snapshot(result.data)
                    if (NowPlaying.stopConfirmed(before, after)) {
                        readingStoryId = null
                        _playback.value = after
                    } else {
                        _message.value = NowPlaying.NOT_CONFIRMED
                    }
                }
            }
            finish(HomeAction.STOP)
        }
    }

    private fun publishBusy() {
        _busy.value = gate.waiting
    }

    private fun finish(action: HomeAction) {
        gate.finish(action)
        _busy.value = gate.waiting
    }
}
