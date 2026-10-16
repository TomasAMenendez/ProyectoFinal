package com.example.aplicacionparacelular.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstUseRulesTest {
    @Test
    fun permissionsComeBeforeBluetoothAndSearch() {
        assertEquals(
            FindTeoPhase.PERMISSION_NEEDED,
            FindTeoRules.phase(searching = true, bluetoothOn = false, permissionsGranted = false, permissionBlocked = false),
        )
        assertEquals(
            FindTeoPhase.PERMISSION_BLOCKED,
            FindTeoRules.phase(searching = false, bluetoothOn = true, permissionsGranted = false, permissionBlocked = true),
        )
    }

    @Test
    fun bluetoothOffIsNotAFinishedSearch() {
        assertEquals(
            FindTeoPhase.BLUETOOTH_OFF,
            FindTeoRules.phase(searching = false, bluetoothOn = false, permissionsGranted = true, permissionBlocked = false),
        )
        assertEquals(setOf(FindHint.BLUETOOTH), FindTeoRules.hints(FindTeoPhase.BLUETOOTH_OFF))
    }

    @Test
    fun searchHidesHintsAndBlocksAnotherAttempt() {
        assertEquals(
            FindTeoPhase.SEARCHING,
            FindTeoRules.phase(searching = true, bluetoothOn = true, permissionsGranted = true, permissionBlocked = false),
        )
        assertTrue(FindTeoRules.hints(FindTeoPhase.SEARCHING).isEmpty())
        assertFalse(FindTeoRules.canStartSearch(FindTeoPhase.SEARCHING))
    }

    @Test
    fun missedSearchShowsTheThreeHints() {
        assertEquals(
            setOf(FindHint.POWER, FindHint.BLUETOOTH, FindHint.WIFI),
            FindTeoRules.hints(FindTeoPhase.NOT_FOUND),
        )
    }

    @Test
    fun pinAndNetworksStayExclusive() {
        assertTrue(WifiStepRules.showsPin(pinAccepted = false))
        assertFalse(WifiStepRules.showsNetworks(pinAccepted = false))
        assertFalse(WifiStepRules.showsPin(pinAccepted = true))
        assertTrue(WifiStepRules.showsNetworks(pinAccepted = true))
    }

    @Test
    fun bluetoothChipStaysOutOfTheHttpChange() {
        assertTrue(WifiStepRules.showsBluetoothChip(connectedByBle = true, httpChange = false))
        assertFalse(WifiStepRules.showsBluetoothChip(connectedByBle = true, httpChange = true))
        assertFalse(WifiStepRules.showsBluetoothChip(connectedByBle = false, httpChange = false))
    }

    @Test
    fun refreshKeepsThePasswordOnlyForTheSameNetwork() {
        assertEquals("secreta", WifiDraft.passwordAfterRefresh("Casa", "Casa", "secreta"))
        assertEquals("", WifiDraft.passwordAfterRefresh("Casa", "Otra", "secreta"))
        assertEquals("", WifiDraft.passwordAfterRefresh(null, "Casa", "secreta"))
    }
}
