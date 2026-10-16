package com.example.aplicacionparacelular.ui.metrics

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityRulesTest {
    private val labels = ActivityAreaLabels("Emocional", "Cognitivo", "Vincular", "Autonomía")

    @Test
    fun rangeWithoutVocabularySourceStaysUnknown() {
        val snap = MetricsPeriodParser.parse(rangePayload(interactions = 24, days = 7))
        assertNull(snap.vocabularyTotal)
        assertEquals(0, snap.headline.crisisCount)
        assertTrue(snap.crisesKnown)
        val ready = ActivitySnapshots.of(MetricsScreen(7, ActivityPhase.READY, snap), labels)
        assertEquals("Total acumulado: Sin datos", ActivityCopy.totalLine(ready.vocabularyTotal))
        assertEquals("Últimos 7 días", ActivityCopy.rangeLabel(ready.requestedDays))
        assertEquals(7, ready.days?.size)
        assertEquals("01/09", ready.days?.first()?.label)
    }

    @Test
    fun confirmedVocabularyZeroIsKept() {
        val payload = rangePayload(interactions = 1, days = 1)
        payload.put(
            "events",
            JSONArray().put(JSONObject().put("type", "vocabulary").put("total_known_words", 0)),
        )
        assertEquals(0, MetricsPeriodParser.parse(payload).vocabularyTotal)
    }

    @Test
    fun vocabularyEventWithoutTotalIsNotASource() {
        val payload = rangePayload(interactions = 1, days = 1)
        payload.put("events", JSONArray().put(JSONObject().put("type", "vocabulary").put("words", "hola")))
        assertNull(MetricsPeriodParser.parse(payload).vocabularyTotal)
    }

    @Test
    fun missingCounterIsNotAZero() {
        val summary = JSONObject().put("total_interactions", 4).put("daily", JSONArray())
        val payload = JSONObject().put("summary", summary).put("events", JSONArray())
        val snap = MetricsPeriodParser.parse(payload)
        assertFalse(snap.crisesKnown)
        assertFalse(snap.gamesKnown)
        assertFalse(snap.pillarsKnown)
        val ready = ActivitySnapshots.of(MetricsScreen(1, ActivityPhase.READY, snap), labels)
        assertEquals(4, ready.interactions)
        assertNull(ready.crises)
        assertNull(ready.areas)
        assertEquals("Sin datos", ActivityCopy.countText(ready.crises))
    }

    @Test
    fun areasStayIndependentFromTheTotal() {
        val summary = JSONObject()
            .put("total_interactions", 24)
            .put("games_played", 3)
            .put("routines_completed", 2)
            .put("crisis_count", 1)
            .put("new_words_today", 5)
            .put(
                "pillar_counts",
                JSONObject()
                    .put("emocional", 8)
                    .put("cognitivo", 6)
                    .put("vincular", 5)
                    .put("autonomia", 3)
                    .put("general", 2),
            )
            .put("daily", JSONArray())
        val snap = MetricsPeriodParser.parse(JSONObject().put("summary", summary).put("events", JSONArray()))
        assertEquals(2, snap.pillars.general)
        val ready = ActivitySnapshots.of(MetricsScreen(7, ActivityPhase.READY, snap), labels)
        assertEquals(listOf("Emocional", "Cognitivo", "Vincular", "Autonomía"), ready.areas?.map { it.label })
        assertEquals(22, ready.areas?.sumOf { it.value })
        assertEquals(24, ready.interactions)
    }

    @Test
    fun loadingHidesThePreviousPeriod() {
        val snap = MetricsPeriodParser.parse(rangePayload(interactions = 90, days = 30))
        val loading = ActivitySnapshots.of(MetricsScreen(7, ActivityPhase.LOADING, snap), labels)
        assertNull(loading.interactions)
        assertNull(loading.days)
        assertEquals(7, loading.requestedDays)
        assertEquals("Últimos 30 días", ActivityCopy.rangeLabel(30))
        assertEquals("Hoy", ActivityCopy.rangeLabel(1))
        assertEquals("0 palabras nuevas", ActivityCopy.wordsLine(0))
        assertEquals("Hoy", ActivityCopy.periodLine(1))
        assertEquals("En los últimos 7 días", ActivityCopy.periodLine(7))
        assertEquals("6 Juegos", ActivityCopy.counterLine(6, "Juegos"))
        assertEquals("Sin datos · Juegos", ActivityCopy.counterLine(null, "Juegos"))
    }

    @Test
    fun monthLabelsStayReadable() {
        val snap = MetricsPeriodParser.parse(rangePayload(interactions = 30, days = 30))
        val ready = ActivitySnapshots.of(MetricsScreen(30, ActivityPhase.READY, snap), labels)
        assertEquals(30, ready.days?.size)
        assertTrue(ready.days!!.all { it.label.length == 5 })
    }

    @Test
    fun staleRequestIsDropped() {
        val gate = MetricsRequestGate()
        val first = gate.begin()
        val second = gate.begin()
        assertFalse(gate.isCurrent(first))
        assertTrue(gate.isCurrent(second))
    }

    @Test
    fun failedLoadSeparatesOfflineFromError() {
        assertEquals(ActivityPhase.OFFLINE, MetricsLoad.phase(connected = false, success = false, summaryKnown = false))
        assertEquals(ActivityPhase.FAILED, MetricsLoad.phase(connected = true, success = false, summaryKnown = false))
        assertEquals(ActivityPhase.FAILED, MetricsLoad.phase(connected = true, success = true, summaryKnown = false))
        assertEquals(ActivityPhase.READY, MetricsLoad.phase(connected = false, success = true, summaryKnown = true))
    }

    private fun rangePayload(interactions: Int, days: Int): JSONObject {
        val daily = JSONArray()
        for (i in 0 until days) {
            daily.put(
                JSONObject()
                    .put("date", "2026-09-%02d".format(i + 1))
                    .put("interactions", if (i == days - 1) interactions else 0)
                    .put("new_words", 0)
                    .put("crisis_count", 0)
                    .put("games_played", 0)
                    .put("routines_completed", 0),
            )
        }
        val summary = JSONObject()
            .put("total_interactions", interactions)
            .put("total_duration_s", 0)
            .put("crisis_count", 0)
            .put("games_played", 0)
            .put("routines_completed", 0)
            .put("new_words_today", 0)
            .put("pillar_counts", JSONObject().put("cognitivo", interactions))
            .put("daily", daily)
        return JSONObject().put("summary", summary).put("events", JSONArray())
    }
}
