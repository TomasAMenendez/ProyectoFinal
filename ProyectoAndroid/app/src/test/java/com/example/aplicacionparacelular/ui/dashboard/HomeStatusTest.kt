package com.example.aplicacionparacelular.ui.dashboard

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStatusTest {
    @Test
    fun despiertoCuandoEstaEncendidoSinPendiente() {
        val status = HomeStatus.from(true, powerOn = true, modelsReady = true, pendingWake = false)
        assertEquals(TeoActivity.AWAKE, status.activity)
        assertEquals("Dormir TEO", status.actionLabel)
        assertTrue(status.actionEnabled)
    }

    @Test
    fun dormidoCuandoLosModelosEstanListos() {
        val status = HomeStatus.from(true, powerOn = false, modelsReady = true, pendingWake = false)
        assertEquals(TeoActivity.ASLEEP, status.activity)
        assertEquals("Despertar TEO", status.actionLabel)
    }

    @Test
    fun pendienteNoSeMuestraDespiertoAunquePowerOnSeaVerdadero() {
        val status = HomeStatus.from(true, powerOn = true, modelsReady = false, pendingWake = true)
        assertEquals(TeoActivity.PREPARING, status.activity)
        assertEquals("Preparándose", status.activityLabel)
        assertFalse(status.actionEnabled)
    }

    @Test
    fun modelosNoListosQuedanEnPreparacion() {
        val status = HomeStatus.from(true, powerOn = false, modelsReady = false, pendingWake = false)
        assertEquals(TeoActivity.PREPARING, status.activity)
    }

    @Test
    fun desconectadoNoAfirmaActividad() {
        val status = HomeStatus.from(false, powerOn = true, modelsReady = true, pendingWake = false)
        assertEquals(TeoActivity.UNAVAILABLE, status.activity)
        assertEquals("Desconectado", status.connectionLabel)
        assertEquals("Sin actividad", status.activityLabel)
        assertFalse(status.actionEnabled)
    }

    @Test
    fun laRespuestaDeEncendidoNoCopiaElPedido() {
        val body = JSONObject()
            .put("power_on", true)
            .put("models_ready", false)
            .put("pending_wake", true)
            .put("wake_result", "queued")
        val reading = HomeStatus.reading(body)
        val visible = HomeStatus.from(true, reading.powerOn, reading.modelsReady, reading.pendingWake)
        assertEquals("queued", reading.wakeResult)
        assertEquals(TeoActivity.PREPARING, visible.activity)
    }
}
