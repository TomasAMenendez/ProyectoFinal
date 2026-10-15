package com.example.aplicacionparacelular.network

import java.util.concurrent.atomic.AtomicLong

/** Vincula las respuestas a su dirección y el trabajo periódico a sus dueños. */
class ConnectionSession {
    private val targetRevision = AtomicLong(0L)
    val revision: Long get() = targetRevision.get()
    @Volatile var uiActive = false
    @Volatile var alertServiceActive = false
    val hasOwner: Boolean get() = uiActive || alertServiceActive

    fun changeTarget(): Long = targetRevision.incrementAndGet()
    fun accepts(revision: Long): Boolean = revision == targetRevision.get()
}
