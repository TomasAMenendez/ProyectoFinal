package com.example.aplicacionparacelular.network

import java.net.DatagramSocket
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryScanGateTest {
    @Test
    fun cancellationClosesTheListeningSocketBeforeANewScanStarts() {
        val scans = DiscoveryScanGate()
        val first = scans.begin()!!
        val socket = DatagramSocket(0)
        try {
            assertTrue(scans.attachSocket(first, socket))
            assertNull(scans.begin())
            scans.cancel()
            assertTrue(socket.isClosed)
            assertNotNull(scans.begin())
        } finally {
            socket.close()
            scans.cancel()
        }
    }

    @Test
    fun aCancelledScanCannotPublishOrFinishTheNewScan() {
        val scans = DiscoveryScanGate()
        val old = scans.begin()!!
        scans.cancel()
        val current = scans.begin()!!
        assertFalse(scans.accepts(old))
        assertFalse(scans.finish(old))
        assertTrue(scans.accepts(current))
        assertTrue(scans.finish(current))
        assertFalse(scans.accepts(current))
    }

    @Test
    fun socketCreatedAfterCancellationIsClosedInsteadOfBeingRegistered() {
        val scans = DiscoveryScanGate()
        val old = scans.begin()!!
        scans.cancel()
        DatagramSocket(0).use { socket ->
            assertFalse(scans.attachSocket(old, socket))
            assertTrue(socket.isClosed)
        }
    }

    @Test
    fun anOldWorkerFinishingCannotCloseTheNewListeningSocket() {
        val scans = DiscoveryScanGate()
        val old = scans.begin()!!
        DatagramSocket(0).use { firstSocket ->
            scans.attachSocket(old, firstSocket)
            scans.cancel()
            val current = scans.begin()!!
            DatagramSocket(0).use { currentSocket ->
                scans.attachSocket(current, currentSocket)
                scans.releaseSocket(old, firstSocket)
                assertFalse(currentSocket.isClosed)
                assertTrue(scans.accepts(current))
            }
        }
        scans.cancel()
    }
}
