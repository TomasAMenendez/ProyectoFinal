package com.example.aplicacionparacelular.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentInboxTest {
    @Test
    fun avisosLogrosYLlamadosEntran() {
        assertTrue(ParentInbox.accepts("aviso"))
        assertTrue(ParentInbox.accepts("logro"))
        assertTrue(ParentInbox.accepts("pedido"))
        assertTrue(ParentInbox.accepts("crisis"))
        assertTrue(ParentInbox.accepts("PEDIDO"))
    }

    @Test
    fun vocabularioMusicaYAgendaNoEntran() {
        assertFalse(ParentInbox.accepts("vocabulario"))
        assertFalse(ParentInbox.accepts("musica"))
        assertFalse(ParentInbox.accepts("rutina"))
        assertFalse(ParentInbox.accepts("vacuna"))
        assertFalse(ParentInbox.accepts("turno"))
        assertFalse(ParentInbox.accepts(""))
    }

    @Test
    fun filtraLaColaHaciaLaListaVisible() {
        val shown = ParentInbox.visibleLines(
            listOf(
                "vocabulario" to "El nene usó 5 palabras nuevas: casa, parque.",
                "aviso" to "El nene pide ayuda.",
                "logro" to "El nene logró: ganó piedra papel o tijera.",
                "pedido" to "El nene está pidiendo hablar con mamá o papá.",
                "musica" to "Reproduciendo: cancion.mp3",
            ),
        )
        assertEquals(
            listOf(
                "ℹ️ Aviso: El nene pide ayuda.",
                "🎉 Logro: El nene logró: ganó piedra papel o tijera.",
                "📞 Pedido: El nene está pidiendo hablar con mamá o papá.",
            ),
            shown,
        )
    }
}
