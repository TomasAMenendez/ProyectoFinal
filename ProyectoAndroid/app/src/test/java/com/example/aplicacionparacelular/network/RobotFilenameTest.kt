package com.example.aplicacionparacelular.network

import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse

class RobotFilenameTest {
    @Test
    fun aTokenResponseFromThePreviousAddressIsDiscarded() {
        val saved = CopyOnWriteArrayList<String>()
        val requested = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/server-info") { exchange ->
            requested.countDown()
            release.await(2, TimeUnit.SECONDS)
            val body = """{"pairing_token":"previous-token"}""".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        val base = RobotApiClient::class.java.getDeclaredField("baseUrl").apply { isAccessible = true }
        val previous = base.get(RobotApiClient)
        server.start()
        val thread = Thread { RobotApiClient.refreshPairingToken { saved.add(it) } }
        try {
            base.set(RobotApiClient, "http://127.0.0.1:${server.address.port}")
            thread.start()
            assertTrue(requested.await(2, TimeUnit.SECONDS))
            base.set(RobotApiClient, "http://127.0.0.1:1")
            release.countDown()
            thread.join(2000)
            assertFalse(thread.isAlive)
            assertTrue(saved.isEmpty())
        } finally {
            release.countDown()
            thread.join(2000)
            base.set(RobotApiClient, previous)
            server.stop(0)
        }
    }

    @Test
    fun deleteKeepsTheCompleteFilenameInOneEncodedPathSegment() {
        val paths = CopyOnWriteArrayList<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/music/") { exchange ->
            paths.add(exchange.requestURI.rawPath)
            val body = """{"status":"ok"}""".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        val base = RobotApiClient::class.java.getDeclaredField("baseUrl").apply { isAccessible = true }
        val previous = base.get(RobotApiClient)
        server.start()
        try {
            base.set(RobotApiClient, "http://127.0.0.1:${server.address.port}")
            assertTrue(RobotApiClient.deleteMusic("cancion#1.mp3") is ApiResult.Success)
            assertEquals("/api/music/cancion%231.mp3", paths.single())
        } finally {
            base.set(RobotApiClient, previous)
            server.stop(0)
        }
    }

    @Test
    fun uploadHeadersPreserveSpacesPlusPercentAndUnicode() {
        val names = CopyOnWriteArrayList<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/music/upload") { exchange ->
            names.add(exchange.requestHeaders.getFirst("X-Filename"))
            exchange.requestBody.use { it.readBytes() }
            val body = """{"status":"ok"}""".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        val base = RobotApiClient::class.java.getDeclaredField("baseUrl").apply { isAccessible = true }
        val previous = base.get(RobotApiClient)
        server.start()
        try {
            base.set(RobotApiClient, "http://127.0.0.1:${server.address.port}")
            assertTrue(RobotApiClient.uploadMusic("canción + 50%.mp3", byteArrayOf(1)) is ApiResult.Success)
            assertEquals("canci%C3%B3n%20%2B%2050%25.mp3", names.single())
        } finally {
            base.set(RobotApiClient, previous)
            server.stop(0)
        }
    }

    @Test
    fun pdfFilenameAndTitleUseTheSameLosslessHeaderEncoding() {
        val headers = CopyOnWriteArrayList<Pair<String, String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/stories/upload") { exchange ->
            headers.add(exchange.requestHeaders.getFirst("X-Filename") to exchange.requestHeaders.getFirst("X-Story-Title"))
            exchange.requestBody.use { it.readBytes() }
            val body = """{"status":"ready","id":"cuento"}""".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        val base = RobotApiClient::class.java.getDeclaredField("baseUrl").apply { isAccessible = true }
        val previous = base.get(RobotApiClient)
        server.start()
        try {
            base.set(RobotApiClient, "http://127.0.0.1:${server.address.port}")
            assertTrue(RobotApiClient.uploadStory("Mi cuento +1.pdf", byteArrayOf(1), "Mi título +1") is ApiResult.Success)
            assertEquals("Mi%20cuento%20%2B1.pdf" to "Mi%20t%C3%ADtulo%20%2B1", headers.single())
        } finally {
            base.set(RobotApiClient, previous)
            server.stop(0)
        }
    }
}
