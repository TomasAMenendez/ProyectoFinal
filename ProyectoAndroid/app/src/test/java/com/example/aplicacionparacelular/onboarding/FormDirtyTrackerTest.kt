package com.example.aplicacionparacelular.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormDirtyTrackerTest {
    @Test fun waitingForTheFirstReadDoesNotCreateAnEmptyDraft() {
        assertFalse(FormDirtyTracker().hasModel)
    }

    @Test fun aLoadedUnchangedProfileStillHasACompleteModelToRestore() {
        val tracker = FormDirtyTracker()
        tracker.beginApply()
        tracker.endApply()
        assertTrue(tracker.hasModel)
        assertFalse(tracker.dirty)
    }

    @Test fun editsBeforeTheFirstReadAreARealDraft() {
        val tracker = FormDirtyTracker()
        tracker.onUserEdit()
        assertTrue(tracker.hasModel)
        assertTrue(tracker.dirty)
    }

    @Test fun remoteApplyAllowedUntilUserEdits() {
        val tracker = FormDirtyTracker()
        assertTrue(tracker.shouldApplyRemote())
        tracker.onUserEdit()
        assertFalse(tracker.shouldApplyRemote())
    }

    @Test fun skipRemoteWhileDirtyThenApplyAfterSave() {
        val tracker = FormDirtyTracker()
        tracker.onUserEdit()
        assertFalse(tracker.shouldApplyRemote())
        tracker.markClean()
        assertTrue(tracker.shouldApplyRemote())
    }

    @Test fun editsDuringApplyDoNotMarkDirty() {
        val tracker = FormDirtyTracker()
        tracker.beginApply()
        tracker.onUserEdit()
        tracker.endApply()
        assertTrue(tracker.shouldApplyRemote())
    }
}
