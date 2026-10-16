package com.example.aplicacionparacelular.network

import java.io.Closeable

/** Mantiene una sola búsqueda y descarta el trabajo de búsquedas canceladas. */
internal class DiscoveryScanGate {
    private var generation = 0L
    private var running = false
    private var socket: Closeable? = null

    @Synchronized
    fun begin(): Long? {
        if (running) return null
        running = true
        return ++generation
    }

    @Synchronized
    fun accepts(scan: Long): Boolean = running && generation == scan

    @Synchronized
    fun isCurrentGeneration(scan: Long): Boolean = generation == scan

    @Synchronized
    fun attachSocket(scan: Long, connection: Closeable): Boolean {
        if (!accepts(scan)) {
            connection.close()
            return false
        }
        socket = connection
        return true
    }

    @Synchronized
    fun releaseSocket(scan: Long, connection: Closeable) {
        if (generation == scan && socket === connection) socket = null
        connection.close()
    }

    @Synchronized
    fun finish(scan: Long): Boolean {
        if (!accepts(scan)) return false
        running = false
        closeSocket()
        return true
    }

    @Synchronized
    fun cancel(): Long {
        running = false
        ++generation
        closeSocket()
        return generation
    }

    private fun closeSocket() {
        runCatching { socket?.close() }
        socket = null
    }
}
