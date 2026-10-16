package com.example.aplicacionparacelular.ui.dashboard

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingTest {
    @Test
    fun sinReproduccionDetenerQuedaVisibleYDeshabilitado() {
        val view = NowPlaying.view(true, PlaybackSnapshot(null, null, null), catalogCount = 2)
        assertEquals("Nada en reproducción", view.title)
        assertEquals("Reproducir música", view.actionLabel)
        assertFalse(view.stopEnabled)
        assertTrue(view.playEnabled)
    }

    @Test
    fun cuentoActivoCambiaLaEtiqueta() {
        val snapshot = PlaybackSnapshot("El bosque", "cuento-1", null)
        val view = NowPlaying.view(true, snapshot, catalogCount = 0)
        assertEquals("Volver a leer cuento", view.actionLabel)
        assertEquals("El bosque", view.title)
        assertTrue(view.stopEnabled)
        assertNull(view.hint)
    }

    @Test
    fun catalogoVacioNoAnunciaReproduccion() {
        val view = NowPlaying.view(true, PlaybackSnapshot(null, null, null), catalogCount = 0)
        assertEquals(NowPlaying.EMPTY_CATALOG, view.hint)
        assertFalse(view.playEnabled)
    }

    @Test
    fun solicitudRechazadaNoCuentaComoExito() {
        val body = JSONObject().put("status", "ok").put("message", "Teo está dormido")
        assertEquals("Teo está dormido", NowPlaying.rejection(body))
    }

    @Test
    fun estadoQueNoConfirmaConservaLoConocido() {
        val after = PlaybackSnapshot(null, null, null)
        assertFalse(NowPlaying.playConfirmed("cuento-1", null, after))
        assertFalse(NowPlaying.stopConfirmed(PlaybackSnapshot("El bosque", "cuento-1", null), PlaybackSnapshot("El bosque", "cuento-1", null)))
    }

    @Test
    fun detenerConfirmadoCuandoYaNoHayReproduccion() {
        val before = PlaybackSnapshot(null, null, "cancion.mp3")
        val after = PlaybackSnapshot(null, null, null)
        assertTrue(NowPlaying.stopConfirmed(before, after))
    }

    @Test
    fun desconexionDejaDePresentarLaReproduccionComoVigente() {
        val known = PlaybackSnapshot(null, null, "cancion.mp3")
        val view = NowPlaying.view(false, known, catalogCount = 3)
        assertFalse(view.current)
        assertFalse(view.stopEnabled)
        assertEquals(NowPlaying.NOT_CURRENT, view.title)
    }
}
