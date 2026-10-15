package com.example.aplicacionparacelular.ui.dashboard

import com.example.aplicacionparacelular.network.ApiResult
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CelebrationResultTest {
    @Test
    fun enviadaSoloCuandoElCuerpoLoDice() {
        val body = JSONObject().put("status", "ok").put("message", "¡Celebración enviada!")
        assertEquals(CelebrationResult.SENT, CelebrationResult.from(true, ApiResult.Success(body)))
    }

    @Test
    fun dormidoNoEsEnvioNiFestejoTerminado() {
        val body = JSONObject().put("status", "ok").put("message", "Teo está dormido")
        val text = CelebrationResult.from(true, ApiResult.Success(body))
        assertEquals(CelebrationResult.ASLEEP, text)
        assertFalse(text.contains("termin"))
    }

    @Test
    fun noDisponibleQuedaExplicito() {
        val text = CelebrationResult.from(true, ApiResult.Error("Celebración no disponible"))
        assertEquals(CelebrationResult.UNAVAILABLE, text)
    }
}
