package com.example.aplicacionparacelular.network

import org.junit.Assert.*
import org.junit.Test

class ConnectionSessionTest {
    @Test fun aPreviousAddressCannotConfirmTheNewAttempt() {
        val session = ConnectionSession()
        val first = session.revision
        val second = session.changeTarget()
        assertFalse(session.accepts(first))
        assertTrue(session.accepts(second))
    }

    @Test fun anotherAttemptAtTheSameAddressAlsoInvalidatesPendingResults() {
        val session = ConnectionSession()
        val attempt = session.changeTarget()
        session.changeTarget()
        assertFalse(session.accepts(attempt))
    }

    @Test fun stoppingTheServiceKeepsPollingWhileTheUiIsVisible() {
        val session = ConnectionSession()
        session.uiActive = true
        session.alertServiceActive = true
        session.alertServiceActive = false
        assertTrue(session.hasOwner)
        session.uiActive = false
        assertFalse(session.hasOwner)
    }

    @Test fun backgroundServiceCanOwnPollingWithoutAnActivity() {
        val session = ConnectionSession()
        assertFalse(session.hasOwner)
        session.alertServiceActive = true
        assertTrue(session.hasOwner)
    }
}
