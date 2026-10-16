package com.example.aplicacionparacelular.myteo

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionRulesTest {
    @Test fun phasesCoverSearchFailureAndRecovery() {
        assertEquals(ConnectionPhase.SEARCHING, ConnectionRules.phase(false, true, true))
        assertEquals(ConnectionPhase.CONNECTED, ConnectionRules.phase(true, true, false))
        assertEquals(ConnectionPhase.FAILED, ConnectionRules.phase(false, false, true))
        assertEquals(ConnectionPhase.DISCONNECTED, ConnectionRules.phase(false, false, false))
    }

    @Test fun currentIsProfileNameAndDoesNotInventSsid() {
        val data = JSONObject()
            .put("current", "Perfil de TEO")
            .put(
                "networks",
                JSONArray()
                    .put(JSONObject().put("ssid", "Perfil de TEO").put("secure", true))
                    .put(JSONObject().put("ssid", "Jardin").put("secure", false))
                    .put(JSONObject().put("ssid", "  ").put("secure", true)),
            )
        val (profile, choices) = ConnectionRules.parseNetworks(data)
        assertEquals("Perfil de TEO", profile)
        assertEquals(listOf(WifiChoice("Jardin", false)), choices)
        assertEquals("", ConnectionRules.profileLabel("  "))
    }

    @Test fun manualAddressRejectsInvalidAndKeepsStoredPort() {
        assertNull(ManualAddress.problem("10.0.0.8"))
        assertTrue(ManualAddress.problem("999.1.1.1") != null)
        assertTrue(ManualAddress.problem("0.0.0.0") != null)
        assertTrue(ManualAddress.problem("127.0.0.1") != null)
        assertEquals(9090, ManualAddress.effectivePort(9090))
        assertEquals(8080, ManualAddress.effectivePort(0))
    }
}
