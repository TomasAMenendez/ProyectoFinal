package com.example.aplicacionparacelular.ui.metrics

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class MetricsPeriodParserTest {
    @Test
    fun monthThenWeekReplacesHeadlineAndBars() {
        val month = rangePayload(interactions = 90, days = 30)
        val week = rangePayload(interactions = 12, days = 7)
        val afterMonth = MetricsPeriodParser.parse(month)
        assertEquals(90, afterMonth.headline.interactions)
        assertEquals(30, afterMonth.days.size)
        val afterWeek = MetricsPeriodParser.parse(week)
        assertEquals(12, afterWeek.headline.interactions)
        assertEquals(7, afterWeek.days.size)
        assertEquals(12, afterWeek.days.sumOf { it.interactions })
    }

    private fun rangePayload(interactions: Int, days: Int): JSONObject {
        val daily = JSONArray()
        val perDay = interactions / days
        val remainder = interactions % days
        for (i in 0 until days) {
            val n = perDay + if (i == days - 1) remainder else 0
            daily.put(
                JSONObject()
                    .put("date", "2026-09-%02d".format(i + 1))
                    .put("interactions", n)
                    .put("duration_s", n * 60.0)
                    .put("crisis_count", 0)
                    .put("games_played", 0)
                    .put("routines_completed", 0)
                    .put("new_words", 0),
            )
        }
        val summary = JSONObject()
            .put("total_interactions", interactions)
            .put("total_duration_s", interactions * 60.0)
            .put("crisis_count", 0)
            .put("games_played", 0)
            .put("routines_completed", 0)
            .put("new_words_today", 0)
            .put("pillar_counts", JSONObject().put("cognitivo", interactions))
            .put("daily", daily)
        return JSONObject()
            .put("from", "2026-09-01")
            .put("to", "2026-09-30")
            .put("summary", summary)
            .put("events", JSONArray())
    }
}
