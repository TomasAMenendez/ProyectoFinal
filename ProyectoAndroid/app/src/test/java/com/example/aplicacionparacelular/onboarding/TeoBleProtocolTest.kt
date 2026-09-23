package com.example.aplicacionparacelular.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contrato BLE con `ble_setup.py` de la Pi: UUIDs y payloads. */
class TeoBleProtocolTest {

    @Test fun uuidsMatchPythonConstants() {
        assertEquals("TEO", TeoBleUuids.ADV_NAME)
        assertEquals("74656f57-4946-4000-8000-0000a0000001", TeoBleUuids.SERVICE.toString())
        assertEquals("74656f57-4946-4000-8000-0000a0000002", TeoBleUuids.CHAR_PIN.toString())
        assertEquals("74656f57-4946-4000-8000-0000a0000003", TeoBleUuids.CHAR_SCAN.toString())
        assertEquals("74656f57-4946-4000-8000-0000a0000004", TeoBleUuids.CHAR_CONNECT.toString())
        assertEquals("74656f57-4946-4000-8000-0000a0000005", TeoBleUuids.CHAR_STATUS.toString())
        assertEquals("00002902-0000-1000-8000-00805f9b34fb", TeoBleUuids.CCCD.toString())
    }

    @Test fun parsesScanJson() {
        val networks = WifiNetworkUi.parseList("""[{"ssid":"Casa","signal":80,"secure":true}]""")
        assertEquals(1, networks.size)
        assertEquals("Casa", networks[0].ssid)
        assertEquals(80, networks[0].signal)
        assertTrue(networks[0].secure)
    }

    @Test fun parsesOpenNetworkAndSortsBySignal() {
        val json = """
            [{"ssid":"Lejos","signal":20,"secure":true},
             {"ssid":"Directa","signal":70,"secure":false},
             {"ssid":"Casa","signal":80,"secure":true}]
        """.trimIndent()
        val networks = WifiNetworkUi.parseList(json)
        assertEquals(listOf("Casa", "Directa", "Lejos"), networks.map { it.ssid })
        assertFalse(networks[1].secure)
    }

    @Test fun emptyArrayGivesEmptyList() {
        assertEquals(emptyList<WifiNetworkUi>(), WifiNetworkUi.parseList("[]"))
    }

    @Test fun malformedJsonGivesEmptyList() {
        assertEquals(emptyList<WifiNetworkUi>(), WifiNetworkUi.parseList("no soy json"))
        assertEquals(emptyList<WifiNetworkUi>(), WifiNetworkUi.parseList(""))
        assertEquals(emptyList<WifiNetworkUi>(), WifiNetworkUi.parseList("""{"ssid":"Casa"}"""))
    }

    @Test fun skipsEntriesWithoutSsidAndClampsSignal() {
        val json = """
            [{"ssid":"","signal":50,"secure":true},
             {"signal":50,"secure":true},
             {"ssid":"Casa","signal":500,"secure":true},
             {"ssid":"Sotano","signal":-5,"secure":false}]
        """.trimIndent()
        val networks = WifiNetworkUi.parseList(json)
        assertEquals(listOf("Casa", "Sotano"), networks.map { it.ssid })
        assertEquals(100, networks[0].signal)
        assertEquals(0, networks[1].signal)
    }

    @Test fun defaultsSecureToTrueWhenMissing() {
        val networks = WifiNetworkUi.parseList("""[{"ssid":"Casa","signal":40}]""")
        assertTrue(networks[0].secure)
    }

    @Test fun deduplicatesBySsidKeepingBestSignal() {
        val json = """[{"ssid":"Casa","signal":40,"secure":true},{"ssid":"Casa","signal":80,"secure":true}]"""
        val networks = WifiNetworkUi.parseList(json)
        assertEquals(1, networks.size)
        assertEquals(80, networks[0].signal)
    }

    @Test fun connectPayloadCarriesSsidAndPassword() {
        val payload = WifiNetworkUi.connectPayload("Casa de la abuela", "clave con \"comillas\"")
        assertEquals(
            """{"ssid":"Casa de la abuela","password":"clave con \"comillas\""}""",
            payload,
        )
    }

    @Test fun connectPayloadForOpenNetworkUsesEmptyPassword() {
        assertEquals("""{"ssid":"Directa","password":""}""", WifiNetworkUi.connectPayload("Directa", ""))
    }

    @Test fun pinIsSixDigits() {
        assertTrue(TeoBleUuids.isValidPin("123456"))
        assertFalse(TeoBleUuids.isValidPin("12345"))
        assertFalse(TeoBleUuids.isValidPin("1234567"))
        assertFalse(TeoBleUuids.isValidPin("12345a"))
        assertFalse(TeoBleUuids.isValidPin(""))
    }

    @Test fun statusConstantsMatchPython() {
        assertEquals("idle", TeoBleStatus.IDLE)
        assertEquals("pin_ok", TeoBleStatus.PIN_OK)
        assertEquals("connecting", TeoBleStatus.CONNECTING)
        assertEquals("connected", TeoBleStatus.CONNECTED)
        assertEquals("failed:pin", TeoBleStatus.FAILED_PIN)
        assertEquals("failed:session", TeoBleStatus.FAILED_SESSION)
        assertEquals("failed:auth", TeoBleStatus.FAILED_AUTH)
        assertEquals("failed:timeout", TeoBleStatus.FAILED_TIMEOUT)
    }
}
