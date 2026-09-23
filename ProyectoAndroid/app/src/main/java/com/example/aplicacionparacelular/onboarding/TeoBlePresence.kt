package com.example.aplicacionparacelular.onboarding

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Scan BLE liviano que solo responde "¿se ve un peluche llamado TEO?".
 *
 * Lo usa [com.example.aplicacionparacelular.MainActivity] para decidir entre la
 * pantalla de WiFi y la de reintentar. No abre GATT ni guarda el dispositivo.
 *
 * Invariante: [scanOnce] llama a `onResult` **exactamente una vez** por cada
 * llamada, en el hilo principal, sin importar por qué camino salga. Un solo
 * camino silencioso dejaba a la app clavada en la pantalla de reintentar tras
 * recrear la Activity.
 */
object TeoBlePresence {

    private const val TAG = "TeoBlePresence"

    private val mainHandler = Handler(Looper.getMainLooper())
    private var running = false

    /**
     * Escanea durante [TeoBleClient.SCAN_TIMEOUT_MS] y llama a [onResult] en el
     * hilo principal. Si falta Bluetooth o permisos responde `false` enseguida.
     */
    @SuppressLint("MissingPermission")
    fun scanOnce(context: Context, onResult: (Boolean) -> Unit) {
        val once = SingleShot<Boolean> { found -> mainHandler.post { onResult(found) } }
        val appContext = context.applicationContext
        val adapter = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner

        val immediate = BlePresenceLogic.immediateAnswer(
            busy = running,
            scannerAvailable = scanner != null,
            permissionsGranted = TeoBleClient.missingPermissions(appContext).isEmpty(),
        )
        if (immediate != null || scanner == null) {
            once.deliver(immediate ?: false)
            return
        }

        running = true
        lateinit var callback: ScanCallback
        lateinit var timeout: Runnable

        fun finish(found: Boolean) {
            if (once.isFired()) return
            running = false
            mainHandler.removeCallbacks(timeout)
            try {
                scanner.stopScan(callback)
            } catch (e: Exception) {
                Log.w(TAG, "no pude detener el scan: ${e.message}")
            }
            once.deliver(found)
        }

        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val name = try {
                    result?.scanRecord?.deviceName ?: result?.device?.name
                } catch (_: SecurityException) {
                    null
                }
                if (name == TeoBleUuids.ADV_NAME) finish(true)
            }

            override fun onScanFailed(errorCode: Int) {
                Log.w(TAG, "scan de presencia falló: $errorCode")
                finish(false)
            }
        }

        timeout = Runnable { finish(false) }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            scanner.startScan(null, settings, callback)
        } catch (e: Exception) {
            // SecurityException si revocaron el permiso en caliente, pero también
            // IllegalStateException si el adaptador se apagó justo ahora.
            Log.w(TAG, "no pude arrancar el scan: ${e.message}")
            running = false
            once.deliver(false)
            return
        }
        mainHandler.postDelayed(timeout, TeoBleClient.SCAN_TIMEOUT_MS)
    }
}
