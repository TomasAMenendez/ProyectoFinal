package com.example.aplicacionparacelular.ui.onboarding

import android.os.Parcelable
import android.util.SparseArray
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentProfileEditBinding
import com.example.aplicacionparacelular.onboarding.ChildProfile
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChildProfileRestoreTest {
    @Test fun restoringAnUnchangedProfileKeepsItsCostumeWithoutBecomingDirty() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().targetContext,
                R.style.Theme_AplicacionParaCelular,
            )
            val original = FragmentProfileEditBinding.inflate(LayoutInflater.from(context))
            val firstForm = ChildProfileForm(original.form)
            firstForm.bind { }
            firstForm.populate(ChildProfile(name = "Luna", gender = "nena", costume = "chimuelo"))
            assertTrue(firstForm.hasModel)
            assertFalse(firstForm.hasChanges)
            val model = ChildProfile.parse(firstForm.payload())
            val hierarchy = SparseArray<Parcelable>()
            original.root.saveHierarchyState(hierarchy)

            val recreated = FragmentProfileEditBinding.inflate(LayoutInflater.from(context))
            val restoredForm = ChildProfileForm(recreated.form)
            var edits = 0
            restoredForm.bind { edits++ }
            restoredForm.restoreDraft(model, dirty = false)
            recreated.root.restoreHierarchyState(hierarchy)
            assertEquals("chimuelo", restoredForm.read().costume)
            assertEquals("Luna", restoredForm.read().name)
            assertFalse(restoredForm.hasChanges)
            assertEquals(0, edits)
        }
    }

    @Test fun aViewWaitingForItsFirstReadDoesNotFreezeAnEmptyProfile() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().targetContext,
                R.style.Theme_AplicacionParaCelular,
            )
            val binding = FragmentProfileEditBinding.inflate(LayoutInflater.from(context))
            val form = ChildProfileForm(binding.form)
            form.bind { }
            assertFalse(form.hasModel)
            form.populate(ChildProfile(name = "Luna", gender = "nena", costume = "tiburon"))
            assertEquals("tiburon", form.read().costume)
            assertFalse(form.hasChanges)
        }
    }
}
