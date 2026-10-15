package com.example.aplicacionparacelular.ui.stories

import com.example.aplicacionparacelular.ui.dashboard.NowPlaying
import com.example.aplicacionparacelular.ui.dashboard.PlaybackSnapshot
import org.json.JSONObject
import java.util.Locale

enum class CatalogLoad {
    LOADING,
    READY,
    FAILED,
}

object ContentRules {
    val audioExtensions: Set<String> = setOf("mp3", "wav", "ogg", "m4a", "flac")
    const val PDF_LIMIT_BYTES = 8L * 1024 * 1024
    const val TITLE_MAX = 80

    fun extensionOf(filename: String): String {
        val base = filename.substringAfterLast('/').substringAfterLast('\\')
        val dot = base.lastIndexOf('.')
        if (dot <= 0 || dot == base.lastIndex) return ""
        return base.substring(dot + 1).lowercase(Locale.ROOT)
    }

    fun audioAccepted(filename: String): Boolean = extensionOf(filename) in audioExtensions

    fun audioProblem(filename: String): String? {
        if (!audioAccepted(filename)) return "Elegí un audio MP3, WAV, OGG, M4A o FLAC."
        return null
    }

    fun formatLabel(filename: String): String = extensionOf(filename).uppercase(Locale.ROOT)

    fun wrappedName(filename: String): String {
        val extension = extensionOf(filename)
        val rawStem = if (extension.isEmpty()) filename else filename.dropLast(extension.length + 1)
        val stem = rawStem.replace("_", "_\u200B")
        if (extension.isEmpty()) return stem
        val glued = extension.toCharArray().joinToString("\u2060")
        return "$stem.\u2060$glued"
    }

    fun sizeLabel(bytes: Long): String {
        if (bytes < 0) return ""
        if (bytes < 1024) return "$bytes B"
        val kb = 1024.0
        val mb = kb * 1024.0
        return if (bytes >= mb) {
            String.format(Locale("es", "AR"), "%.1f MB", bytes / mb)
        } else {
            String.format(Locale("es", "AR"), "%.0f KB", bytes / kb)
        }
    }

    fun pdfProblem(filename: String, bytes: Long): String? {
        if (extensionOf(filename) != "pdf") return "Elegí un archivo PDF."
        if (bytes > PDF_LIMIT_BYTES) return "El PDF supera los 8 MB."
        if (bytes == 0L) return "El PDF está vacío."
        return null
    }

    fun createTitle(raw: String): Pair<String?, String?> {
        val trimmed = raw.trim()
        if (trimmed.length > TITLE_MAX) return null to "El título admite hasta 80 caracteres."
        return trimmed.ifBlank { null } to null
    }

    fun editTitleProblem(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "El título es obligatorio."
        if (trimmed.length > TITLE_MAX) return "El título admite hasta 80 caracteres."
        return null
    }

    fun deleteQuestion(name: String): String = "¿Querés eliminar «${name.trim()}»?"

    fun storyReady(body: JSONObject): Boolean = body.optString("status") == "ready"

    fun storyReason(body: JSONObject): String {
        val reason = body.optString("reason").trim()
        if (reason.isNotEmpty()) return reason
        return NowPlaying.rejection(body) ?: "No se pudo publicar el cuento."
    }

    fun markPlaying(
        response: JSONObject,
        expectedSong: String?,
        expectedStoryId: String?,
        after: PlaybackSnapshot,
    ): Boolean {
        if (NowPlaying.rejection(response) != null) return false
        return NowPlaying.playConfirmed(expectedStoryId, expectedSong, after)
    }

    fun catalogNotice(
        connected: Boolean,
        load: CatalogLoad,
        count: Int,
        emptyText: String,
        errorText: String,
        offlineText: String,
    ): String? = when {
        !connected && count == 0 -> offlineText
        load == CatalogLoad.FAILED && count == 0 -> errorText
        load == CatalogLoad.READY && count == 0 -> emptyText
        !connected -> offlineText
        load == CatalogLoad.FAILED -> errorText
        else -> null
    }
}
