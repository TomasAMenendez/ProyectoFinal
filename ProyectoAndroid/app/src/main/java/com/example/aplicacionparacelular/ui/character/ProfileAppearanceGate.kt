package com.example.aplicacionparacelular.ui.character

/** Evita que una consulta anterior o concurrente deshaga un guardado confirmado. */
class ProfileAppearanceGate {
    data class Read(val revision: Long, val startedWithoutSave: Boolean)
    private var revision = 0L
    private var confirmedRevision = 0L
    private var savesInFlight = 0

    fun beginRead() = Read(revision, savesInFlight == 0)
    fun beginSave(): Long {
        savesInFlight++
        return ++revision
    }
    fun finishSave(saveRevision: Long, confirmed: Boolean = true): Boolean {
        savesInFlight--
        if (!confirmed || saveRevision < confirmedRevision) return false
        confirmedRevision = saveRevision
        return true
    }
    fun accepts(read: Read) = read.startedWithoutSave && read.revision == revision && savesInFlight == 0
}
