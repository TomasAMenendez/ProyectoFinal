package com.example.aplicacionparacelular.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun etiquetasSinCorchetesNiEmoji() {
        assertEquals("Logro", ParentInbox.label("logro"))
        assertEquals("Pedido", ParentInbox.label("pedido"))
        assertEquals("Atención", ParentInbox.label("crisis"))
        assertEquals("Aviso", ParentInbox.label("aviso"))
    }

    @Test
    fun fechaInvalidaSeOmiteYOrdenaPorRecepcion() {
        val dated = ParentInbox.notice("aviso", "Con fecha", "2026-10-14T10:00:00", 1L)!!
        val undated = ParentInbox.notice("logro", "Sin fecha", "no-es-fecha", 4_000_000_000_000L)!!
        assertNull(undated.eventAtMillis)
        val ordered = ParentInbox.merge(listOf(dated), listOf(undated))
        assertEquals("Sin fecha", ordered.first().message)
    }

    @Test
    fun laRecepcionNoCambiaAlReabrir() {
        val original = ParentInbox.notice("pedido", "Llamado", null, 20L)!!
        val reopened = ParentInbox.merge(listOf(original), emptyList()).single()
        assertEquals(20L, reopened.receivedAtMillis)
    }

    @Test
    fun conservaVeinteMasNuevos() {
        val notices = (1..21).map { index ->
            ParentInbox.notice("aviso", "Aviso $index", null, index.toLong())!!
        }
        val kept = ParentInbox.merge(emptyList(), notices)
        assertEquals(20, kept.size)
        assertEquals("Aviso 21", kept.first().message)
        assertFalse(kept.any { it.message == "Aviso 1" })
    }
}
