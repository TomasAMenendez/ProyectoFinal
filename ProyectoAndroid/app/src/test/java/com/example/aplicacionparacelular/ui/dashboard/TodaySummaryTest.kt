package com.example.aplicacionparacelular.ui.dashboard

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodaySummaryTest {
    @Test
    fun ceroInformadoSeMuestraComoNumero() {
        val data = JSONObject().put("summary", JSONObject().put("total_interactions", 0))
        assertEquals("0", TodaySummary.parse(data).interactions)
    }

    @Test
    fun campoAusenteOInvalidoEsAusencia() {
        assertNull(TodaySummary.parse(JSONObject()).interactions)
        val invalid = JSONObject().put("summary", JSONObject().put("total_interactions", "no"))
        assertNull(TodaySummary.parse(invalid).interactions)
    }

    @Test
    fun laSenalSaleDelUltimoEventoSinEmoji() {
        val events = JSONArray()
            .put(JSONObject().put("emotion", "sad"))
            .put(JSONObject().put("emotion", "happy"))
        val data = JSONObject()
            .put("summary", JSONObject().put("total_interactions", 3))
            .put("events", events)
        assertEquals("Feliz", TodaySummary.parse(data).signal)
    }

    @Test
    fun sinEventoLaSenalEstaAusente() {
        val data = JSONObject().put("summary", JSONObject().put("total_interactions", 1))
        assertNull(TodaySummary.parse(data).signal)
    }
}
