package com.example.aplicacionparacelular.onboarding

/** Dirty flag for Datos del chico: skip GET overwrite while the parent is editing. */
class FormDirtyTracker {
    var dirty: Boolean = false
        private set
    private var applying: Boolean = false
    private var loaded: Boolean = false
    val hasModel: Boolean get() = loaded || dirty

    fun shouldApplyRemote(): Boolean = !dirty

    fun onUserEdit() {
        if (!applying) dirty = true
    }

    fun beginApply() {
        applying = true
    }

    fun endApply() {
        loaded = true
        dirty = false
        applying = false
    }

    fun markClean() {
        loaded = true
        dirty = false
    }

    fun markDirty() {
        dirty = true
    }
}
