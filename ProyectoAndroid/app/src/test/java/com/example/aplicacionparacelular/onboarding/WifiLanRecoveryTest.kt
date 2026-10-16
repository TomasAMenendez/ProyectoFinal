package com.example.aplicacionparacelular.onboarding

import org.junit.Assert.*
import org.junit.Test

class WifiLanRecoveryTest {
    @Test fun wifiConfirmationDoesNotMeanLanIsConfirmed() {
        val recovery = WifiLanRecovery()
        recovery.joined()
        assertTrue(recovery.hasJoinedWifi)
        assertEquals(WifiLanPhase.WAITING, recovery.phase)
        assertTrue(recovery.timeOut())
        assertEquals(WifiLanPhase.NOT_FOUND, recovery.phase)
    }

    @Test fun retryAfterTimeoutKeepsTheConfirmedWifiAndSearchesLan() {
        val recovery = WifiLanRecovery()
        recovery.joined()
        recovery.timeOut()
        assertTrue(recovery.retryLan())
        assertEquals(WifiLanPhase.WAITING, recovery.phase)
        assertTrue(recovery.hasJoinedWifi)
        recovery.confirmed()
        assertFalse(recovery.timeOut())
    }

    @Test fun provisionRetryStillUsesBleBeforeWifiWasConfirmed() {
        assertFalse(WifiLanRecovery().retryLan())
    }
}
