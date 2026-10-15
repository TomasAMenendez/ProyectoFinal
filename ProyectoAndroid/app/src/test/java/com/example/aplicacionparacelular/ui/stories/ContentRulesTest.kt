package com.example.aplicacionparacelular.ui.stories

import com.example.aplicacionparacelular.ui.dashboard.PlaybackSnapshot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentRulesTest {
    @Test
    fun aceptaSoloLosFormatosDeMusicaDelContrato() {
        assertTrue(ContentRules.audioAccepted("juego.mp3"))
        assertTrue(ContentRules.audioAccepted("suave.WAV"))
        assertTrue(ContentRules.audioAccepted("a.ogg"))
        assertTrue(ContentRules.audioAccepted("b.m4a"))
        assertTrue(ContentRules.audioAccepted("c.flac"))
        assertFalse(ContentRules.audioAccepted("nota.txt"))
        assertFalse(ContentRules.audioAccepted("cancion"))
        assertNull(ContentRules.audioProblem("juego.mp3"))
        assertEquals("Elegí un audio MP3, WAV, OGG, M4A o FLAC.", ContentRules.audioProblem("nota.txt"))
        assertEquals("MP3", ContentRules.formatLabel("musica_para_jugar.mp3"))
        assertEquals("nombre_\u200Blargo.\u2060m\u2060p\u20603", ContentRules.wrappedName("nombre_largo.mp3"))
    }

    @Test
    fun elTamanoUsaMegabytesConComa() {
        assertEquals("3,2 MB", ContentRules.sizeLabel(3_355_443L))
        assertEquals("125 B", ContentRules.sizeLabel(125))
        assertEquals("0 B", ContentRules.sizeLabel(0))
    }

    @Test
    fun elPdfRespetaExtensionYOchoMegabytes() {
        assertNull(ContentRules.pdfProblem("cuento.pdf", 1024L))
        assertEquals("Elegí un archivo PDF.", ContentRules.pdfProblem("cuento.docx", 1024L))
        assertEquals("El PDF supera los 8 MB.", ContentRules.pdfProblem("grande.pdf", ContentRules.PDF_LIMIT_BYTES + 1L))
        assertEquals("El PDF está vacío.", ContentRules.pdfProblem("vacio.pdf", 0L))
    }

    @Test
    fun elTituloVacioSeOmiteYElLargoSeRechaza() {
        val (auto, autoError) = ContentRules.createTitle("   ")
        assertNull(auto)
        assertNull(autoError)
        val (kept, keptError) = ContentRules.createTitle("  El jardín  ")
        assertEquals("El jardín", kept)
        assertNull(keptError)
        val (_, longError) = ContentRules.createTitle("a".repeat(81))
        assertEquals("El título admite hasta 80 caracteres.", longError)
        assertEquals("El título es obligatorio.", ContentRules.editTitleProblem("  "))
    }

    @Test
    fun unRechazoNoMarcaLaCancionAunqueElEstadoPosteriorCoincida() {
        val asleep = JSONObject("""{"status":"ok","message":"TEO está dormido"}""")
        val after = PlaybackSnapshot(storyTitle = null, storyId = null, songName = "a.mp3")
        assertFalse(ContentRules.markPlaying(asleep, "a.mp3", null, after))
    }

    @Test
    fun laCancionSoloQuedaActivaSiElEstadoPosteriorCoincide() {
        val ok = JSONObject("""{"status":"ok"}""")
        val playing = PlaybackSnapshot(null, null, "a.mp3")
        val idle = PlaybackSnapshot(null, null, null)
        assertTrue(ContentRules.markPlaying(ok, "a.mp3", null, playing))
        assertFalse(ContentRules.markPlaying(ok, "a.mp3", null, idle))
    }

    @Test
    fun unFalloDeConsultaNoSePresentaComoCatalogoVacio() {
        assertEquals(
            "No se pudo consultar.",
            ContentRules.catalogNotice(true, CatalogLoad.FAILED, 0, "Vacío", "No se pudo consultar.", "Sin conexión"),
        )
        assertEquals(
            "Vacío",
            ContentRules.catalogNotice(true, CatalogLoad.READY, 0, "Vacío", "No se pudo consultar.", "Sin conexión"),
        )
        assertEquals(
            "Sin conexión",
            ContentRules.catalogNotice(false, CatalogLoad.READY, 2, "Vacío", "No se pudo consultar.", "Sin conexión"),
        )
    }

    @Test
    fun elCuentoSoloEstaListoCuandoElCuerpoLoConfirma() {
        assertTrue(ContentRules.storyReady(JSONObject("""{"status":"ready","title":"Luna"}""")))
        assertFalse(ContentRules.storyReady(JSONObject("""{"status":"rejected","reason":"No parece un cuento"}""")))
        assertEquals("No parece un cuento", ContentRules.storyReason(JSONObject("""{"status":"rejected","reason":"No parece un cuento"}""")))
    }
}
