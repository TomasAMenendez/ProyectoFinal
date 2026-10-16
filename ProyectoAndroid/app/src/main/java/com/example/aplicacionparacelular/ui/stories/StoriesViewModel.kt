package com.example.aplicacionparacelular.ui.stories

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.ui.dashboard.NowPlaying
import com.example.aplicacionparacelular.ui.dashboard.PlaybackSnapshot
import org.json.JSONObject

enum class ContentSection {
    MUSIC,
    STORIES,
}

data class StoryItem(
    val id: String,
    val title: String,
    val wordCount: Int,
)

data class SongItem(
    val filename: String,
    val sizeBytes: Long,
    val modified: String,
)

class StoriesViewModel : ViewModel() {
    var section: ContentSection = ContentSection.MUSIC

    private val _stories = MutableLiveData<List<StoryItem>>(emptyList())
    val stories: LiveData<List<StoryItem>> = _stories

    private val _storyLoad = MutableLiveData(CatalogLoad.LOADING)
    val storyLoad: LiveData<CatalogLoad> = _storyLoad

    private val _songs = MutableLiveData<List<SongItem>>(emptyList())
    val songs: LiveData<List<SongItem>> = _songs

    private val _musicLoad = MutableLiveData(CatalogLoad.LOADING)
    val musicLoad: LiveData<CatalogLoad> = _musicLoad

    private val _playback = MutableLiveData(PlaybackSnapshot(null, null, null))
    val playback: LiveData<PlaybackSnapshot> = _playback

    private val _connected = MutableLiveData(false)
    val connected: LiveData<Boolean> = _connected

    private val _uploadingName = MutableLiveData<String?>(null)
    val uploadingName: LiveData<String?> = _uploadingName

    private val _pending = MutableLiveData(false)
    val pending: LiveData<Boolean> = _pending

    private val _notice = MutableLiveData<String?>()
    val notice: LiveData<String?> = _notice

    private val _storyToEdit = MutableLiveData<StoryDetail?>()
    val storyToEdit: LiveData<StoryDetail?> = _storyToEdit

    private val _formMessage = MutableLiveData<String?>()
    val formMessage: LiveData<String?> = _formMessage

    private val _formAccepted = MutableLiveData(false)
    val formAccepted: LiveData<Boolean> = _formAccepted

    var pdfUri: Uri? = null
        private set
    var pdfName: String? = null
        private set
    var pdfSize: Long = -1
        private set

    private val connectionObserver = Observer<Boolean> { value ->
        _connected.value = value == true
    }
    private val statusObserver = Observer<JSONObject?> { status ->
        if (status != null) _playback.value = NowPlaying.snapshot(status)
    }

    init {
        RobotConnectionManager.isConnected.observeForever(connectionObserver)
        RobotConnectionManager.robotStatus.observeForever(statusObserver)
    }

    override fun onCleared() {
        RobotConnectionManager.isConnected.removeObserver(connectionObserver)
        RobotConnectionManager.robotStatus.removeObserver(statusObserver)
        super.onCleared()
    }

    fun consumeNotice() {
        _notice.value = null
    }

    fun refreshStatus() {
        if (_connected.value == true) RobotConnectionManager.fetchStatus { }
    }

    fun loadStories() {
        if (_storyLoad.value != CatalogLoad.READY) _storyLoad.value = CatalogLoad.LOADING
        RobotConnectionManager.fetchStories { result ->
            when (result) {
                is ApiResult.Success -> {
                    _stories.value = parseStories(result.data)
                    _storyLoad.value = CatalogLoad.READY
                }
                is ApiResult.Error -> _storyLoad.value = CatalogLoad.FAILED
            }
        }
    }

    fun loadSongs() {
        if (_musicLoad.value != CatalogLoad.READY) _musicLoad.value = CatalogLoad.LOADING
        RobotConnectionManager.fetchMusic { result ->
            when (result) {
                is ApiResult.Success -> {
                    _songs.value = parseSongs(result.data)
                    _musicLoad.value = CatalogLoad.READY
                }
                is ApiResult.Error -> _musicLoad.value = CatalogLoad.FAILED
            }
        }
    }

    fun uploadSong(filename: String, data: ByteArray) {
        if (_uploadingName.value != null || _pending.value == true) return
        val audioProblem = ContentRules.audioProblem(filename)
        if (audioProblem != null) {
            _notice.value = audioProblem
            return
        }
        _uploadingName.value = filename
        RobotConnectionManager.uploadMusic(filename, data) { result ->
            _uploadingName.value = null
            when (result) {
                is ApiResult.Success -> {
                    val rejected = NowPlaying.rejection(result.data)
                    if (rejected != null) {
                        _notice.value = rejected
                    } else {
                        loadSongs()
                    }
                }
                is ApiResult.Error -> _notice.value = result.message.ifBlank { "No se pudo enviar la música." }
            }
        }
    }

    fun playSong(filename: String) {
        if (_pending.value == true || _connected.value != true) return
        _pending.value = true
        RobotConnectionManager.playMusic(filename) { result ->
            confirmPlay(result, expectedSong = filename, expectedStoryId = null)
        }
    }

    fun playStory(storyId: String) {
        if (_pending.value == true || _connected.value != true) return
        _pending.value = true
        RobotConnectionManager.playStory(storyId) { result ->
            confirmPlay(result, expectedSong = null, expectedStoryId = storyId)
        }
    }

    fun stopMusic() {
        val before = _playback.value ?: PlaybackSnapshot(null, null, null)
        if (before.songName.isNullOrBlank() || _pending.value == true) return
        _pending.value = true
        RobotConnectionManager.stopMusic { result -> confirmStop(result, before) }
    }

    fun stopStory() {
        val before = _playback.value ?: PlaybackSnapshot(null, null, null)
        if (before.storyId.isNullOrBlank() || _pending.value == true) return
        _pending.value = true
        RobotConnectionManager.stopStory { result -> confirmStop(result, before) }
    }

    fun uploadPdf(filename: String, data: ByteArray, title: String?) {
        if (_pending.value == true) return
        val problem = ContentRules.pdfProblem(filename, data.size.toLong())
        if (problem != null) {
            _formMessage.value = problem
            return
        }
        val (storedTitle, titleProblem) = ContentRules.createTitle(title.orEmpty())
        if (titleProblem != null) {
            _formMessage.value = titleProblem
            return
        }
        _pending.value = true
        _formAccepted.value = false
        RobotConnectionManager.uploadStory(filename, data, storedTitle) { result ->
            _pending.value = false
            when (result) {
                is ApiResult.Success -> {
                    if (ContentRules.storyReady(result.data)) {
                        _formAccepted.value = true
                        loadStories()
                    } else {
                        _formMessage.value = ContentRules.storyReason(result.data)
                    }
                }
                is ApiResult.Error -> _formMessage.value = result.message.ifBlank { "No se pudo enviar el cuento." }
            }
        }
    }

    fun loadStory(id: String) {
        _formAccepted.value = false
        RobotConnectionManager.fetchStory(id) { result ->
            when (result) {
                is ApiResult.Success -> _storyToEdit.value = StoryJson.parseDetail(result.data)
                is ApiResult.Error -> _formMessage.value = result.message.ifBlank { "No se pudo abrir el cuento." }
            }
        }
    }

    fun saveStory(id: String, title: String, text: String) {
        val problem = ContentRules.editTitleProblem(title)
        if (problem != null) {
            _formMessage.value = problem
            return
        }
        _pending.value = true
        _formAccepted.value = false
        RobotConnectionManager.updateStory(id, title.trim(), text) { result ->
            _pending.value = false
            when (result) {
                is ApiResult.Success -> {
                    val update = StoryJson.parseUpdateStatus(result.data)
                    if (update.status == "rejected") {
                        _formMessage.value = update.reason ?: "No se pudo guardar"
                    } else if (NowPlaying.rejection(result.data) != null) {
                        _formMessage.value = NowPlaying.rejection(result.data)
                    } else {
                        _formAccepted.value = true
                        _storyToEdit.value = null
                        loadStories()
                    }
                }
                is ApiResult.Error -> _formMessage.value = result.message.ifBlank { "No se pudo guardar el cuento." }
            }
        }
    }

    fun rememberPdf(uri: Uri, name: String, sizeBytes: Long) {
        pdfUri = uri
        pdfName = name
        pdfSize = sizeBytes
    }

    fun clearPdf() {
        pdfUri = null
        pdfName = null
        pdfSize = -1
    }

    fun clearStoryToEdit() {
        _storyToEdit.value = null
        _formMessage.value = null
        _formAccepted.value = false
        clearPdf()
    }

    fun deleteStory(id: String) {
        RobotConnectionManager.deleteStory(id) { result ->
            when (result) {
                is ApiResult.Success -> {
                    val rejected = NowPlaying.rejection(result.data)
                    if (rejected != null) _notice.value = rejected else loadStories()
                }
                is ApiResult.Error -> _notice.value = result.message.ifBlank { "No se pudo eliminar el cuento." }
            }
        }
    }

    fun deleteSong(filename: String) {
        RobotConnectionManager.deleteMusic(filename) { result ->
            when (result) {
                is ApiResult.Success -> {
                    val rejected = NowPlaying.rejection(result.data)
                    if (rejected != null) _notice.value = rejected else loadSongs()
                }
                is ApiResult.Error -> _notice.value = result.message.ifBlank { "No se pudo eliminar la música." }
            }
        }
    }

    private fun confirmPlay(result: ApiResult<JSONObject>, expectedSong: String?, expectedStoryId: String?) {
        when (result) {
            is ApiResult.Error -> {
                _pending.value = false
                _notice.value = result.message.ifBlank { NowPlaying.NOT_CONFIRMED }
            }
            is ApiResult.Success -> {
                if (NowPlaying.rejection(result.data) != null) {
                    _pending.value = false
                    _notice.value = NowPlaying.rejection(result.data)
                    return
                }
                RobotConnectionManager.fetchStatus { statusResult ->
                    _pending.value = false
                    when (statusResult) {
                        is ApiResult.Error -> _notice.value = NowPlaying.NOT_CONFIRMED
                        is ApiResult.Success -> {
                            val after = NowPlaying.snapshot(statusResult.data)
                            if (!ContentRules.markPlaying(result.data, expectedSong, expectedStoryId, after)) {
                                _notice.value = NowPlaying.NOT_CONFIRMED
                            }
                        }
                    }
                }
            }
        }
    }

    private fun confirmStop(result: ApiResult<JSONObject>, before: PlaybackSnapshot) {
        when (result) {
            is ApiResult.Error -> {
                _pending.value = false
                _notice.value = result.message.ifBlank { NowPlaying.NOT_CONFIRMED }
            }
            is ApiResult.Success -> {
                if (NowPlaying.rejection(result.data) != null) {
                    _pending.value = false
                    _notice.value = NowPlaying.rejection(result.data)
                    return
                }
                RobotConnectionManager.fetchStatus { statusResult ->
                    _pending.value = false
                    when (statusResult) {
                        is ApiResult.Error -> _notice.value = NowPlaying.NOT_CONFIRMED
                        is ApiResult.Success -> {
                            val after = NowPlaying.snapshot(statusResult.data)
                            if (!NowPlaying.stopConfirmed(before, after)) {
                                _notice.value = NowPlaying.NOT_CONFIRMED
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseStories(data: JSONObject): List<StoryItem> {
        val arr = data.optJSONArray("stories") ?: return emptyList()
        val list = mutableListOf<StoryItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(
                StoryItem(
                    id = obj.optString("id", ""),
                    title = obj.optString("title", ""),
                    wordCount = obj.optInt("word_count", 0),
                ),
            )
        }
        return list
    }

    private fun parseSongs(data: JSONObject): List<SongItem> {
        val songsArray = data.optJSONArray("songs") ?: return emptyList()
        val list = mutableListOf<SongItem>()
        for (i in 0 until songsArray.length()) {
            val obj = songsArray.optJSONObject(i) ?: continue
            list.add(
                SongItem(
                    filename = obj.optString("filename", ""),
                    sizeBytes = obj.optLong("size_bytes", 0),
                    modified = obj.optString("modified", ""),
                ),
            )
        }
        return list
    }
}
