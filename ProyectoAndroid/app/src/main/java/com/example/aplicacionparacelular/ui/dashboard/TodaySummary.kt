package com.example.aplicacionparacelular.ui.dashboard

import org.json.JSONObject

data class TodaySummaryView(
    val interactions: String?,
    val signal: String?,
)

object TodaySummary {
    fun parse(data: JSONObject): TodaySummaryView {
        val summary = data.optJSONObject("summary")
        return TodaySummaryView(
            interactions = interactions(summary),
            signal = signal(data),
        )
    }

    fun absent() = TodaySummaryView(interactions = null, signal = null)

    private fun interactions(summary: JSONObject?): String? {
        if (summary == null || !summary.has("total_interactions") || summary.isNull("total_interactions")) {
            return null
        }
        val raw = summary.opt("total_interactions")
        return when (raw) {
            is Number -> raw.toInt().toString()
            is String -> raw.toIntOrNull()?.toString()
            else -> null
        }
    }

    private fun signal(data: JSONObject): String? {
        val events = data.optJSONArray("events") ?: return null
        for (index in events.length() - 1 downTo 0) {
            val event = events.optJSONObject(index) ?: continue
            val emotion = event.optString("emotion", "").trim()
            if (emotion.isNotEmpty() && !emotion.equals("null", ignoreCase = true)) {
                return label(emotion)
            }
        }
        return null
    }

    private fun label(emotion: String): String = when (emotion.lowercase()) {
        "happy", "feliz" -> "Feliz"
        "sad", "triste" -> "Triste"
        "angry", "enojado" -> "Enojado"
        "surprised", "sorprendido" -> "Sorprendido"
        "neutral", "calm", "calmo" -> "Tranquilo"
        "fear", "miedo" -> "Asustado"
        else -> emotion
    }
}
