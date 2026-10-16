package com.example.aplicacionparacelular.ui.preview

import com.example.aplicacionparacelular.network.ParentInbox
import com.example.aplicacionparacelular.ui.dashboard.NowPlaying
import com.example.aplicacionparacelular.ui.dashboard.TeoActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomePreviewScenarioTest {
    @Test
    fun muestraElEscenarioDelBocetoConDatosDeEjemplo() {
        val status = HomePreviewScenario.status
        assertEquals(TeoActivity.AWAKE, status.activity)
        assertEquals("Conectado", status.connectionLabel)
        assertEquals("Despierto", status.activityLabel)
        assertEquals("Dormir TEO", status.actionLabel)
        assertTrue(status.actionEnabled)
        assertEquals("12", HomePreviewScenario.INTERACTIONS)
        assertEquals("Tranquilo", HomePreviewScenario.SIGNAL)
        assertEquals(NowPlaying.IDLE, HomePreviewScenario.playback.title)
        assertEquals("Reproducir música", HomePreviewScenario.playback.actionLabel)
        assertTrue(HomePreviewScenario.playback.playEnabled)
        assertFalse(HomePreviewScenario.playback.stopEnabled)
        assertEquals("logro", HomePreviewScenario.notice.type)
        assertEquals("Logro", ParentInbox.label(HomePreviewScenario.notice.type))
        assertTrue(HomePreviewScenario.notice.message.startsWith("Ejemplo"))
        assertNotNull(HomePreviewScenario.notice.eventAtMillis)
    }

    @Test
    fun laEntradaQuedaFueraDelManifiestoPrincipal() {
        val main = File("src/main/AndroidManifest.xml").readText()
        val preview = File("src/preview/AndroidManifest.xml").readText()
        val mainStrings = File("src/main/res/values/strings.xml").readText()
        assertFalse(main.contains("HomePreviewActivity"))
        assertFalse(mainStrings.contains("TEO Vista previa"))
        assertFalse(mainStrings.contains("Vista previa · Datos de ejemplo"))
        assertTrue(File("src/preview/res/values/strings.xml").readText().contains("Vista previa · Datos de ejemplo"))
        assertTrue(preview.contains("HomePreviewActivity"))
        assertTrue(preview.contains("android.intent.action.MAIN"))
        assertTrue(preview.contains("android.intent.category.LAUNCHER"))
        assertTrue(preview.contains("tools:node=\"remove\""))
    }
}
