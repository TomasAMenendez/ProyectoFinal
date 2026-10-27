package com.example.aplicacionparacelular.ui.dashboard

import org.json.JSONObject

enum class PlaytimeKind { HIDDEN, ABSENT, VISIBLE }

data class PlaytimeView(
    val kind: PlaytimeKind,
    val label: String? = null,
    val fraction: Float? = null,
)

object PlaytimeReading {
    private const val LIMIT = "playtime_limit_minutes"
    private const val SECONDS = "playtime_remaining_seconds"
    private const val FRACTION = "playtime_remaining_fraction"

    fun from(connected: Boolean, status: JSONObject?): PlaytimeView {
        if (!connected || status == null) return PlaytimeView(PlaytimeKind.ABSENT)
        val limit = integer(status, LIMIT) ?: return PlaytimeView(PlaytimeKind.ABSENT)
        if (limit == 0) return PlaytimeView(PlaytimeKind.HIDDEN)
        if (limit < 0) return PlaytimeView(PlaytimeKind.ABSENT)
        val seconds = integer(status, SECONDS) ?: return PlaytimeView(PlaytimeKind.ABSENT)
        val fraction = fraction(status) ?: return PlaytimeView(PlaytimeKind.ABSENT)
        return PlaytimeView(PlaytimeKind.VISIBLE, formatSeconds(seconds), fraction)
    }

    fun formatSeconds(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        return "%02d:%02d".format(safe / 60, safe % 60)
    }

    private fun integer(status: JSONObject, key: String): Int? {
        if (!status.has(key) || status.isNull(key)) return null
        val raw = status.opt(key)
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        }
    }

    private fun fraction(status: JSONObject): Float? {
        if (!status.has(FRACTION) || status.isNull(FRACTION)) return null
        val raw = status.opt(FRACTION)
        val number = when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull()
            else -> null
        } ?: return null
        if (number.isNaN()) return null
        return number.toFloat().coerceIn(0f, 1f)
    }
}
