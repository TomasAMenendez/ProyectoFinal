package com.example.aplicacionparacelular.ui.dashboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeActionInProgressTest {
    @Test
    fun impideUnaSegundaSolicitudMientrasSigue() {
        val gate = HomeActionGate()
        assertTrue(gate.tryStart(HomeAction.PLAY))
        assertFalse(gate.tryStart(HomeAction.PLAY))
        assertTrue(gate.waiting)
    }

    @Test
    fun recuperaLaAccionDespuesDeExitoOError() {
        val gate = HomeActionGate()
        gate.tryStart(HomeAction.CELEBRATE)
        gate.finish(HomeAction.CELEBRATE)
        assertFalse(gate.isBusy(HomeAction.CELEBRATE))
        assertTrue(gate.tryStart(HomeAction.CELEBRATE))
    }
}
