package com.example.aplicacionparacelular.ui.dashboard

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaytimeReadingTest {
    @Test fun limiteCeroQuedaOculto() {
        val view = PlaytimeReading.from(true, status(limit = 0, seconds = 0, fraction = 0.0))
        assertEquals(PlaytimeKind.HIDDEN, view.kind)
        assertNull(view.label)
        assertNull(view.fraction)
    }

    @Test fun datoFaltanteQuedaAusente() {
        assertEquals(PlaytimeKind.ABSENT, PlaytimeReading.from(true, JSONObject()).kind)
        assertEquals(
            PlaytimeKind.ABSENT,
            PlaytimeReading.from(true, status(limit = 30, seconds = null, fraction = 0.5)).kind,
        )
        assertEquals(
            PlaytimeKind.ABSENT,
            PlaytimeReading.from(true, status(limit = 30, seconds = 10, fraction = null)).kind,
        )
        assertEquals(
            PlaytimeKind.ABSENT,
            PlaytimeReading.from(false, status(limit = 30, seconds = 10, fraction = 0.5)).kind,
        )
    }

    @Test fun formateaSegundosConMinimoDosDigitos() {
        assertEquals("01:30", PlaytimeReading.formatSeconds(90))
        assertEquals("05:00", PlaytimeReading.formatSeconds(300))
        assertEquals("240:00", PlaytimeReading.formatSeconds(14400))
    }

    @Test fun restanteCeroMuestraLineaVacia() {
        val view = PlaytimeReading.from(true, status(limit = 30, seconds = 0, fraction = 0.0))
        assertEquals(PlaytimeKind.VISIBLE, view.kind)
        assertEquals("00:00", view.label)
        assertEquals(0f, view.fraction)
    }

    @Test fun recortaLaFraccionAlRangoVisible() {
        val high = PlaytimeReading.from(true, status(limit = 60, seconds = 1800, fraction = 1.4))
        val low = PlaytimeReading.from(true, status(limit = 60, seconds = 1800, fraction = -0.2))
        assertEquals("30:00", high.label)
        assertEquals(1f, high.fraction)
        assertEquals(0f, low.fraction)
    }

    private fun status(limit: Int?, seconds: Int?, fraction: Double?): JSONObject {
        val json = JSONObject()
        if (limit != null) json.put("playtime_limit_minutes", limit)
        if (seconds != null) json.put("playtime_remaining_seconds", seconds)
        if (fraction != null) json.put("playtime_remaining_fraction", fraction)
        return json
    }
}
