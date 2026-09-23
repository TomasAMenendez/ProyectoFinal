package com.example.aplicacionparacelular.ui.metrics

import org.json.JSONObject

data class PillarData(
    val emocional: Int = 0,
    val cognitivo: Int = 0,
    val vincular: Int = 0,
    val autonomia: Int = 0,
    val general: Int = 0,
)

data class DailySummary(
    val date: String,
    val interactions: Int,
    val durationMinutes: Int,
    val crisisCount: Int,
    val newWords: Int,
    val gamesPlayed: Int,
    val routinesCompleted: Int,
)

data class MetricsPeriodSnapshot(
    val headline: DailySummary,
    val pillars: PillarData,
    val days: List<DailySummary>,
    val newWords: Int,
    val vocabularyTotal: Int,
)

object MetricsPeriodParser {
    fun parse(data: JSONObject): MetricsPeriodSnapshot {
        val summary = data.optJSONObject("summary") ?: JSONObject()
        val dateStr = when {
            data.has("to") && data.optString("to").isNotBlank() -> data.optString("to")
            else -> data.optString("date", "")
        }
        val headline = DailySummary(
            date = dateStr,
            interactions = summary.optInt("total_interactions", 0),
            durationMinutes = (summary.optDouble("total_duration_s", 0.0) / 60).toInt(),
            crisisCount = summary.optInt("crisis_count", 0),
            newWords = summary.optInt("new_words_today", 0),
            gamesPlayed = summary.optInt("games_played", 0),
            routinesCompleted = summary.optInt("routines_completed", 0),
        )
        val pillarCounts = summary.optJSONObject("pillar_counts")
        val pillars = if (pillarCounts != null) {
            PillarData(
                emocional = pillarCounts.optInt("emocional", 0),
                cognitivo = pillarCounts.optInt("cognitivo", 0),
                vincular = pillarCounts.optInt("vincular", 0),
                autonomia = pillarCounts.optInt("autonomia", 0),
                general = pillarCounts.optInt("general", 0),
            )
        } else {
            PillarData()
        }
        val daily = summary.optJSONArray("daily")
        val days = mutableListOf<DailySummary>()
        var totalNewWords = headline.newWords
        if (daily != null) {
            totalNewWords = 0
            for (i in 0 until daily.length()) {
                val row = daily.optJSONObject(i) ?: continue
                val ds = DailySummary(
                    date = row.optString("date", ""),
                    interactions = row.optInt("interactions", 0),
                    durationMinutes = (row.optDouble("duration_s", 0.0) / 60).toInt(),
                    crisisCount = row.optInt("crisis_count", 0),
                    newWords = row.optInt("new_words", 0),
                    gamesPlayed = row.optInt("games_played", 0),
                    routinesCompleted = row.optInt("routines_completed", 0),
                )
                days.add(ds)
                totalNewWords += ds.newWords
            }
        }
        var vocabularyTotal = 0
        val events = data.optJSONArray("events")
        if (events != null) {
            for (i in events.length() - 1 downTo 0) {
                val event = events.optJSONObject(i) ?: continue
                if (event.optString("type") == "vocabulary") {
                    val total = event.optInt("total_known_words", 0)
                    if (total > 0) {
                        vocabularyTotal = total
                        break
                    }
                }
            }
        }
        return MetricsPeriodSnapshot(
            headline = headline,
            pillars = pillars,
            days = days,
            newWords = totalNewWords,
            vocabularyTotal = vocabularyTotal,
        )
    }
}
