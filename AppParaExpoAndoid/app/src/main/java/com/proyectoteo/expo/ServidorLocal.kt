package com.proyectoteo.expo

import android.content.res.AssetManager
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlin.concurrent.thread

/**
 * Sirve los juegos en http://127.0.0.1. En la tablet, la cámara solo se puede
 * pedir desde esa dirección, no desde un archivo de Descargas.
 */
class ServidorLocal(private val assets: AssetManager) {
    private val servidor = ServerSocket()
    val puerto: Int

    @Volatile
    private var activo = true

    init {
        servidor.reuseAddress = true
        servidor.bind(InetSocketAddress("127.0.0.1", 0))
        puerto = servidor.localPort
        thread(isDaemon = true, name = "teo-juegos") { aceptar() }
    }

    fun cerrar() {
        activo = false
        try {
            servidor.close()
        } catch (_: IOException) {
        }
    }

    private fun aceptar() {
        while (activo) {
            val cliente = try {
                servidor.accept()
            } catch (_: IOException) {
                break
            }
            thread(isDaemon = true) { atender(cliente) }
        }
    }

    private fun atender(cliente: Socket) {
        cliente.use { socket ->
            socket.soTimeout = 8000
            val linea = try {
                socket.getInputStream().bufferedReader(Charsets.ISO_8859_1).readLine()
            } catch (_: IOException) {
                null
            } ?: return
            val partes = linea.split(' ')
            if (partes.size < 2 || (partes[0] != "GET" && partes[0] != "HEAD")) {
                responder(socket, 405, "text/plain", ByteArray(0))
                return
            }
            val ruta = resolver(partes[1].substringBefore('?'))
            val recurso = if (ruta == null) null else abrir(ruta)
            if (ruta == null || recurso == null) {
                responder(socket, 404, "text/plain", ByteArray(0))
                return
            }
            if (partes[0] == "HEAD") {
                recurso.flujo.close()
                responder(socket, 200, tipo(ruta), ByteArray(0), recurso.largo)
            } else {
                val cuerpo = recurso.flujo.use { it.readBytes() }
                responder(socket, 200, tipo(ruta), cuerpo)
            }
        }
    }

    private fun resolver(ruta: String): String? {
        val pedido = try {
            URLDecoder.decode(ruta, "UTF-8")
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (pedido.contains('\\') || pedido.contains('\u0000')) return null
        val partes = pedido.split('/').filter { it.isNotEmpty() && it != "." }
        if (partes.any { it == ".." }) return null
        val archivo = if (partes.isEmpty()) "index.html" else partes.joinToString("/")
        return "www/$archivo"
    }

    private fun abrir(ruta: String): Recurso? {
        try {
            val descriptor = assets.openFd(ruta)
            return Recurso(descriptor.createInputStream(), descriptor.length)
        } catch (_: IOException) {
        }
        return try {
            val bytes = assets.open(ruta).use { it.readBytes() }
            Recurso(ByteArrayInputStream(bytes), bytes.size.toLong())
        } catch (_: IOException) {
            null
        }
    }

    private fun tipo(ruta: String): String {
        val nombre = ruta.substringAfterLast('/').lowercase()
        return when {
            nombre.endsWith(".html") -> "text/html; charset=utf-8"
            nombre.endsWith(".js") -> "text/javascript"
            nombre.endsWith(".css") -> "text/css"
            nombre.endsWith(".json") -> "application/json"
            nombre.endsWith(".wasm") -> "application/wasm"
            nombre.endsWith(".png") -> "image/png"
            nombre.endsWith(".jpg") || nombre.endsWith(".jpeg") -> "image/jpeg"
            nombre.endsWith(".svg") -> "image/svg+xml"
            nombre.endsWith(".mp3") -> "audio/mpeg"
            nombre.endsWith(".wav") -> "audio/wav"
            else -> "application/octet-stream"
        }
    }

    private fun responder(socket: Socket, estado: Int, tipo: String, cuerpo: ByteArray, largo: Long = cuerpo.size.toLong()) {
        val texto = if (estado == 200) "OK" else if (estado == 404) "Not Found" else "Method Not Allowed"
        val encabezado = "HTTP/1.1 $estado $texto\r\nContent-Type: $tipo\r\nContent-Length: $largo\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n"
        val salida = socket.getOutputStream()
        salida.write(encabezado.toByteArray(Charsets.ISO_8859_1))
        if (cuerpo.isNotEmpty()) salida.write(cuerpo)
        salida.flush()
    }

    private class Recurso(val flujo: InputStream, val largo: Long)
}
