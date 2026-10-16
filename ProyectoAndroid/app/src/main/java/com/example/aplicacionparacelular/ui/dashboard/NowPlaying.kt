package com.example.aplicacionparacelular.ui.dashboard

import org.json.JSONObject

data class PlaybackSnapshot(
    val storyTitle: String?,
    val storyId: String?,
    val songName: String?,
) {
    val hasPlayback: Boolean
        get() = !storyTitle.isNullOrBlank() || !songName.isNullOrBlank()
}

data class PlaybackView(
    val title: String,
    val actionLabel: String,
    val playEnabled: Boolean,
    val stopEnabled: Boolean,
    val hint: String?,
    val current: Boolean,
)

enum class PlaybackConfirmation {
    CONFIRMED,
    REJECTED,
    QUERY_FAILED,
    NOT_CONFIRMED,
}

object NowPlaying {
    const val EMPTY_CATALOG = "No hay una canción para iniciar. Podés cargar una en Contenido."
    const val NOT_CURRENT = "Sin conexión. La reproducción conocida ya no está vigente."
    const val NOT_CONFIRMED = "No se confirmó el resultado. Se conserva lo que se conocía."
    const val IDLE = "Nada en reproducción"

    fun snapshot(status: JSONObject): PlaybackSnapshot {
        val reading = status.optJSONObject("currently_reading")
        val title = reading?.optString("title").orEmpty().trim()
        val id = reading?.optString("id").orEmpty().trim()
        if (title.isNotEmpty() && id.isNotEmpty()) {
            return PlaybackSnapshot(storyTitle = title, storyId = id, songName = null)
        }
        val song = if (status.isNull("currently_playing")) {
            null
        } else {
            status.optString("currently_playing", "").trim().ifEmpty { null }
        }
        return PlaybackSnapshot(storyTitle = null, storyId = null, songName = song)
    }

    fun songNames(catalog: JSONObject): List<String> {
        val songs = catalog.optJSONArray("songs") ?: return emptyList()
        val names = mutableListOf<String>()
        for (index in 0 until songs.length()) {
            val name = songs.optJSONObject(index)?.optString("filename").orEmpty().trim()
            if (name.isNotEmpty()) names.add(name)
        }
        return names
    }

    fun view(
        connected: Boolean,
        snapshot: PlaybackSnapshot,
        catalogCount: Int,
    ): PlaybackView {
        if (!connected) {
            return PlaybackView(
                title = NOT_CURRENT,
                actionLabel = if (snapshot.storyId != null) "Volver a leer cuento" else "Reproducir música",
                playEnabled = false,
                stopEnabled = false,
                hint = null,
                current = false,
            )
        }
        val story = snapshot.storyId != null && !snapshot.storyTitle.isNullOrBlank()
        val title = when {
            story -> snapshot.storyTitle.orEmpty()
            !snapshot.songName.isNullOrBlank() -> snapshot.songName
            else -> IDLE
        }
        val empty = !story && catalogCount == 0 && !snapshot.hasPlayback
        return PlaybackView(
            title = title,
            actionLabel = if (story) "Volver a leer cuento" else "Reproducir música",
            playEnabled = story || catalogCount > 0,
            stopEnabled = snapshot.hasPlayback,
            hint = if (empty) EMPTY_CATALOG else null,
            current = true,
        )
    }

    fun rejection(data: JSONObject): String? {
        val message = data.optString("message", "").trim()
        val status = data.optString("status", "")
        if (message.contains("dormido", ignoreCase = true)) return message
        if (message.contains("no disponible", ignoreCase = true)) return message
        if (status.equals("error", ignoreCase = true)) {
            return message.ifEmpty { "No se pudo completar" }
        }
        return null
    }

    fun playConfirmed(expectedStoryId: String?, expectedSong: String?, after: PlaybackSnapshot): Boolean {
        if (!expectedStoryId.isNullOrBlank()) {
            return after.storyId == expectedStoryId
        }
        if (!expectedSong.isNullOrBlank()) {
            return after.songName == expectedSong && after.storyId == null
        }
        return false
    }

    fun stopConfirmed(before: PlaybackSnapshot, after: PlaybackSnapshot): Boolean {
        if (before.storyId != null) return after.storyId == null && after.songName == null
        if (!before.songName.isNullOrBlank()) return after.songName == null && after.storyId == null
        return after.storyId == null && after.songName == null
    }
}
