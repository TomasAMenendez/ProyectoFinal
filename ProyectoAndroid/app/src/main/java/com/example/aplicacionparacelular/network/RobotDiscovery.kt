package com.example.aplicacionparacelular.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Descubre automáticamente la Raspberry Pi en la red local escuchando
 * beacons UDP que el robot emite cada 1.5 segundos en el puerto 5555.
 *
 * El beacon contiene un JSON con:
 * - device_id: "micompanero_robot"
 * - device_name: nombre del dispositivo
 * - api_port: puerto del servidor REST
 * - local_ip: IP del robot en la red local
 *
 * Cuando corre en el emulador de Android Studio, el beacon UDP no llega
 * (red virtual aislada), así que se prueba automáticamente la IP especial
 * 10.0.2.2 (host del emulador) como fallback.
 *
 * Si el beacon UDP no llega (ej: router con AP Isolation), se activa
 * automáticamente un fallback de scan HTTP paralelo sobre toda la subred /24.
 */
object RobotDiscovery {

    private const val TAG = "RobotDiscovery"
    private const val BEACON_PORT = 5555
    private const val LISTEN_TIMEOUT_MS = 15_000  // Escuchar máximo 15 segundos

    /** Timeout por cada intento HTTP durante el scan de subred. */
    private const val HTTP_SCAN_TIMEOUT_MS = 800

    /** Hilos paralelos para el scan HTTP de la subred. */
    private const val HTTP_SCAN_THREADS = 50

    /** IP especial que el emulador de Android Studio mapea al host. */
    private const val EMULATOR_HOST_IP = "10.0.2.2"
    private const val DEFAULT_API_PORT = 8080

    /**
     * Resultado del descubrimiento.
     */
    data class DiscoveredRobot(
        val ip: String,
        val port: Int,
        val deviceName: String,
        val deviceId: String,
        val pairingToken: String = ""
    )

    private val _discoveredRobot = MutableLiveData<DiscoveredRobot?>()
    val discoveredRobot: LiveData<DiscoveredRobot?> = _discoveredRobot

    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _scanError = MutableLiveData<String?>()
    val scanError: LiveData<String?> = _scanError

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scans = DiscoveryScanGate()

    @Volatile
    private var scanThread: Thread? = null

    /** Contexto de la aplicación, necesario para adquirir el MulticastLock. */
    private var appContext: Context? = null

    /**
     * Detecta si la app está corriendo en un emulador de Android Studio.
     *
     * Usa varias heurísticas del Build: fingerprint genérico, modelo "sdk",
     * hardware "goldfish"/"ranchu", etc.
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk", ignoreCase = true)
                || Build.MODEL.contains("Emulator", ignoreCase = true)
                || Build.MODEL.contains("Android SDK built for x86", ignoreCase = true)
                || Build.MANUFACTURER.contains("Genymotion", ignoreCase = true)
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.PRODUCT.contains("sdk", ignoreCase = true)
                || Build.HARDWARE.contains("goldfish", ignoreCase = true)
                || Build.HARDWARE.contains("ranchu", ignoreCase = true))
    }

    /**
     * Inicia un escaneo de la red local buscando el beacon UDP del robot.
     *
     * Si detecta que corre en un emulador, primero intenta conectar
     * directamente a 10.0.2.2:8080 (el host del emulador), ya que los
     * broadcasts UDP no atraviesan la red virtual del emulador.
     *
     * El escaneo se detiene automáticamente cuando encuentra un robot
     * o cuando se agota el timeout.
     */
    fun startScan(context: Context? = null) {
        val scan = scans.begin() ?: return

        // Guardar el contexto de la aplicación para el MulticastLock
        if (context != null) {
            appContext = context.applicationContext
        }

        postForScan(scan) {
            _isScanning.value = true
            _scanError.value = null
            _discoveredRobot.value = null
        }

        scanThread = Thread({
            if (!scans.accepts(scan)) return@Thread
            // En el emulador, intentar primero la IP del host directamente
            if (isEmulator()) {
                Log.d(TAG, "Emulador detectado, intentando 10.0.2.2:$DEFAULT_API_PORT...")
                val robot = tryDirectConnection(EMULATOR_HOST_IP, DEFAULT_API_PORT)
                if (robot != null) {
                    completeScan(scan, robot = robot)
                    return@Thread
                }
                Log.d(TAG, "Conexión directa a 10.0.2.2 falló, intentando beacon UDP...")
            }
            if (!scans.accepts(scan)) return@Thread

            // Adquirir MulticastLock para que el chip WiFi no descarte
            // los paquetes broadcast/multicast (beacons UDP del peluche).
            // Sin esto, en celulares físicos el receive() siempre da timeout.
            val multicastLock = acquireMulticastLock()

            // Intentar descubrimiento por beacon UDP (funciona en red real)
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(null)
                if (!scans.attachSocket(scan, socket)) return@Thread
                socket.broadcast = true
                socket.reuseAddress = true
                socket.bind(InetSocketAddress(BEACON_PORT))

                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)
                val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(LISTEN_TIMEOUT_MS.toLong())

                while (scans.accepts(scan)) {
                    val remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
                    if (remaining <= 0L) throw SocketTimeoutException()
                    socket.soTimeout = remaining.coerceAtLeast(1L).toInt()
                    packet.length = buffer.size
                    socket.receive(packet)
                    val data = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val json = runCatching { JSONObject(data) }.getOrNull() ?: continue
                    val deviceId = json.optString("device_id", "")
                    if (deviceId != "micompanero_robot") {
                        continue
                    }
                    val ip = BeaconIp.pick(
                        json.optString("local_ip", ""),
                        packet.address.hostAddress,
                    )
                    if (!BeaconIp.isUsable(ip)) {
                        continue
                    }
                    val port = json.optInt("api_port", DEFAULT_API_PORT)
                    if (port !in 1..65535) continue
                    val name = json.optString("device_name", "Peluche")
                    val robot = DiscoveredRobot(
                        ip = ip,
                        port = port,
                        deviceName = name,
                        deviceId = deviceId,
                        pairingToken = json.optString("pairing_token", "")
                    )
                    completeScan(scan, robot = robot)
                    return@Thread
                }
            } catch (e: java.net.SocketTimeoutException) {
                if (!scans.accepts(scan)) return@Thread
                // Beacon UDP no recibido — puede ser AP Isolation en el router.
                // Intentar fallback: scan HTTP paralelo sobre toda la subred /24.
                Log.d(TAG, "Beacon UDP sin respuesta, iniciando fallback HTTP scan de subred...")

                postForScan(scan) {
                    _scanError.value = "Beacon UDP sin respuesta. Escaneando red por HTTP..."
                }

                val robot = scanSubnetHttp(DEFAULT_API_PORT, scan)
                if (robot != null) {
                    completeScan(scan, robot = robot)
                    return@Thread
                }

                completeScan(scan, error = "No se encontró el peluche en la red. Verificá que estén en el mismo WiFi.")
            } catch (e: Exception) {
                completeScan(scan, error = "Error de red: ${e.message}. Ingresá la IP manualmente.")
            } finally {
                socket?.let { scans.releaseSocket(scan, it) }
                releaseMulticastLock(multicastLock)
            }
        }, "RobotDiscovery")

        scanThread?.isDaemon = true
        scanThread?.start()
    }

    /**
     * Intenta una conexión HTTP directa a la IP y puerto dados,
     * haciendo un GET /api/status. Si responde correctamente,
     * devuelve el robot encontrado, sin publicar resultados de búsquedas canceladas.
     */
    private fun tryDirectConnection(ip: String, port: Int): DiscoveredRobot? {
        return try {
            val url = URL("http://$ip:$port/api/status")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            try {
                if (conn.responseCode == 200) {
                    Log.d(TAG, "Conexión directa exitosa a $ip:$port")
                    DiscoveredRobot(
                        ip = ip,
                        port = port,
                        deviceName = "MiCompañero Peluche (local)",
                        deviceId = "micompanero_robot"
                    )
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Conexión directa a $ip:$port falló: ${e.message}")
            null
        }
    }

    /**
     * Escanea en paralelo toda la subred /24 del dispositivo buscando el
     * endpoint GET /api/status del robot. Usa [HTTP_SCAN_THREADS] hilos
     * concurrentes para terminar rápido (~5-10 segundos para 254 IPs).
     *
     * @param apiPort Puerto HTTP en el que escucha el robot.
     * @return [DiscoveredRobot] si se encontró, null si no.
     */
    private fun scanSubnetHttp(apiPort: Int, scan: Long): DiscoveredRobot? {
        if (!scans.accepts(scan)) return null
        // Obtener la IP local del dispositivo para deducir la subred
        val ctx = appContext ?: return null
        val wifiMgr = ctx.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val wifiInfo = wifiMgr.connectionInfo
        val ipInt = wifiInfo?.ipAddress ?: 0
        if (ipInt == 0) {
            Log.w(TAG, "No se pudo obtener la IP WiFi del dispositivo")
            return null
        }

        // Convertir de entero little-endian a string "A.B.C"
        val ipBytes = byteArrayOf(
            (ipInt and 0xFF).toByte(),
            (ipInt shr 8 and 0xFF).toByte(),
            (ipInt shr 16 and 0xFF).toByte(),
            (ipInt shr 24 and 0xFF).toByte()
        )
        val subnet = "${ipBytes[0].toInt() and 0xFF}.${ipBytes[1].toInt() and 0xFF}.${ipBytes[2].toInt() and 0xFF}"
        Log.d(TAG, "Scan HTTP paralelo sobre subred $subnet.1-254 (puerto $apiPort)")

        val found = AtomicBoolean(false)
        val result = AtomicReference<DiscoveredRobot?>(null)
        val latch = CountDownLatch(254)
        val executor = Executors.newFixedThreadPool(HTTP_SCAN_THREADS)

        try {
            for (i in 1..254) {
                val ip = "$subnet.$i"
                executor.submit {
                    try {
                        // Si se canceló la búsqueda o ya encontramos el robot, no abrir más conexiones.
                        if (!found.get() && scans.accepts(scan)) {
                            val url = URL("http://$ip:$apiPort/api/status")
                            val conn = url.openConnection() as HttpURLConnection
                            conn.connectTimeout = HTTP_SCAN_TIMEOUT_MS
                            conn.readTimeout = HTTP_SCAN_TIMEOUT_MS
                            conn.requestMethod = "GET"
                            try {
                                if (conn.responseCode == 200 && scans.accepts(scan)) {
                                    if (found.compareAndSet(false, true)) {
                                        result.set(
                                            DiscoveredRobot(
                                                ip = ip,
                                                port = apiPort,
                                                deviceName = "MiCompañero Peluche",
                                                deviceId = "micompanero_robot"
                                            )
                                        )
                                        Log.d(TAG, "Robot encontrado por HTTP scan en $ip:$apiPort")
                                    }
                                }
                            } finally {
                                conn.disconnect()
                            }
                        }
                    } catch (_: Exception) {
                        // IP sin respuesta, continuar.
                    } finally {
                        latch.countDown()
                    }
                }
            }
            // Esperar hasta que todos terminen o hasta 30 segundos máximo.
            latch.await(30, TimeUnit.SECONDS)
            return result.get().takeIf { scans.accepts(scan) }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return null
        } finally {
            executor.shutdownNow()
        }
    }

    /**
     * Cancela un escaneo en curso.
     */
    fun stopScan() {
        val cancelled = scans.cancel()
        scanThread?.interrupt()
        scanThread = null
        mainHandler.post {
            if (scans.isCurrentGeneration(cancelled)) _isScanning.value = false
        }
    }

    private fun postForScan(scan: Long, action: () -> Unit) {
        mainHandler.post {
            if (scans.accepts(scan)) action()
        }
    }

    private fun completeScan(scan: Long, robot: DiscoveredRobot? = null, error: String? = null) {
        mainHandler.post {
            if (!scans.finish(scan)) return@post
            _discoveredRobot.value = robot
            _scanError.value = error
            _isScanning.value = false
        }
    }

    // ------------------------------------------------------------------
    // MulticastLock helpers
    // ------------------------------------------------------------------

    /**
     * Adquiere un [WifiManager.MulticastLock].
     *
     * Por defecto, Android desactiva la recepción de paquetes multicast/broadcast
     * en el chip WiFi para ahorrar batería. El beacon UDP que emite el peluche
     * es un broadcast, así que sin este lock el [DatagramSocket.receive] nunca
     * recibe nada y siempre da timeout en celulares físicos.
     */
    private fun acquireMulticastLock(): WifiManager.MulticastLock? {
        return try {
            val ctx = appContext ?: return null
            val wifiMgr = ctx.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
            val lock = wifiMgr.createMulticastLock("RobotDiscovery")
            lock.setReferenceCounted(true)
            lock.acquire()
            Log.d(TAG, "MulticastLock adquirido")
            lock
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo adquirir MulticastLock: ${e.message}")
            null
        }
    }

    /**
     * Libera el [WifiManager.MulticastLock] si estaba adquirido,
     * para no consumir batería innecesariamente.
     */
    private fun releaseMulticastLock(multicastLock: WifiManager.MulticastLock?) {
        try {
            multicastLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "MulticastLock liberado")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al liberar MulticastLock: ${e.message}")
        }
    }
}
