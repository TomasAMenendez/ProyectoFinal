package com.example.aplicacionparacelular.ui.character

import org.junit.Assert.*
import org.junit.Test

class ProfileAppearanceGateTest {
    @Test fun aReadStartedBeforeSavingCannotRestoreTheOldCostume() {
        val gate = ProfileAppearanceGate()
        val oldRead = gate.beginRead()
        val save = gate.beginSave()
        assertTrue(gate.finishSave(save))
        assertFalse(gate.accepts(oldRead))
        assertTrue(gate.accepts(gate.beginRead()))
    }

    @Test fun aReadDuringSavingCannotOverwriteItsConfirmation() {
        val gate = ProfileAppearanceGate()
        val save = gate.beginSave()
        val concurrentRead = gate.beginRead()
        assertFalse(gate.accepts(concurrentRead))
        assertTrue(gate.finishSave(save))
        assertFalse(gate.accepts(concurrentRead))
    }

    @Test fun aLaterFailedAttemptDoesNotDiscardTheLastConfirmedSave() {
        val gate = ProfileAppearanceGate()
        val first = gate.beginSave()
        val second = gate.beginSave()
        assertFalse(gate.finishSave(second, confirmed = false))
        assertTrue(gate.finishSave(first, confirmed = true))
    }

    @Test fun anOlderResponseDoesNotUndoANewerConfirmedSave() {
        val gate = ProfileAppearanceGate()
        val first = gate.beginSave()
        val second = gate.beginSave()
        assertTrue(gate.finishSave(second, confirmed = true))
        assertFalse(gate.finishSave(first, confirmed = true))
    }
}
