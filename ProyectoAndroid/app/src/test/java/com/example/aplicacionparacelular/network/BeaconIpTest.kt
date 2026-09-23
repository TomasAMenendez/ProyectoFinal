package com.example.aplicacionparacelular.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BeaconIpTest {
    @Test fun wildcardIsNotUsable() {
        assertFalse(BeaconIp.isUsable("0.0.0.0"))
        assertFalse(BeaconIp.isUsable(""))
        assertFalse(BeaconIp.isUsable("127.0.0.1"))
        assertTrue(BeaconIp.isUsable("192.168.0.148"))
    }

    @Test fun advertisedWildcardFallsBackToPacketHost() {
        assertEquals(
            "192.168.0.148",
            BeaconIp.pick("0.0.0.0", "192.168.0.148"),
        )
    }

    @Test fun advertisedLanWins() {
        assertEquals(
            "192.168.0.148",
            BeaconIp.pick("192.168.0.148", "10.0.0.9"),
        )
    }

    @Test fun missingAdvertisedUsesPacket() {
        assertEquals("192.168.0.148", BeaconIp.pick(null, "192.168.0.148"))
    }
}
