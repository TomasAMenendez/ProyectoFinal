package com.example.aplicacionparacelular.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Cliente GATT del peluche para el provisionamiento WiFi por BLE.
 *
 * Escanea 8 segundos buscando el nombre exacto `TEO`, se conecta, habilita las
 * notificaciones de estado y expone las escrituras del protocolo. Todos los
 * callbacks se entregan en el hilo principal.
 *
 * El llamador es responsable de pedir los permisos (ver [missingPermissions])
 * y de llamar a [close] en `onDestroyView`.
 */
class TeoBleClient(context: Context) {

    /** Motivos de falla que la UI tiene que poder explicarle al padre. */
    enum class Failure {
        BLUETOOTH_OFF,
        NO_PERMISSION,
        NOT_FOUND,
        GATT_FAILED,
        DISCONNECTED,
        SERVICE_MISSING,
        OPERATION_FAILED,
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    var onScanStarted: (() -> Unit)? = null
    var onReady: (() -> Unit)? = null
    var onStatus: ((String) -> Unit)? = null
    var onNetworks: ((List<WifiNetworkUi>) -> Unit)? = null
    var onFailure: ((Failure) -> Unit)? = null

    private var gatt: BluetoothGatt? = null
    private var scanning = false
    private var closed = false

    private val adapter: BluetoothAdapter?
        get() = (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    // ------------------------------------------------------------------
    // Scan + conexión
    // ------------------------------------------------------------------

    /** Escanea hasta [SCAN_TIMEOUT_MS] y se conecta al primer `TEO` que aparezca. */
    @SuppressLint("MissingPermission")
    fun startScanAndConnect() {
        if (closed || scanning || gatt != null) return
        val bluetooth = adapter
        if (bluetooth == null || !bluetooth.isEnabled) {
            emitFailure(Failure.BLUETOOTH_OFF)
            return
        }
        if (missingPermissions(appContext).isNotEmpty()) {
            emitFailure(Failure.NO_PERMISSION)
            return
        }
        val scanner = bluetooth.bluetoothLeScanner
        if (scanner == null) {
            emitFailure(Failure.BLUETOOTH_OFF)
            return
        }
        scanning = true
        emit { onScanStarted?.invoke() }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        try {
            // Sin ScanFilter de nombre: varios equipos no reportan el nombre en el
            // advertisement, así que se filtra a mano contra el scan record.
            scanner.startScan(null, settings, scanCallback)
        } catch (e: SecurityException) {
            scanning = false
            Log.w(TAG, "startScan sin permiso: ${e.message}")
            emitFailure(Failure.NO_PERMISSION)
            return
        }
        mainHandler.postDelayed(scanTimeout, SCAN_TIMEOUT_MS)
    }

    private val scanTimeout = Runnable {
        if (!scanning) return@Runnable
        stopScan()
        if (gatt == null) emitFailure(Failure.NOT_FOUND)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            if (!isTeo(result)) return
            stopScan()
            connect(device)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "scan BLE falló: $errorCode")
            stopScan()
            emitFailure(Failure.GATT_FAILED)
        }
    }

    @SuppressLint("MissingPermission")
    private fun isTeo(result: ScanResult): Boolean {
        val advertised = try {
            result.scanRecord?.deviceName
        } catch (_: SecurityException) {
            null
        }
        val bonded = try {
            result.device?.name
        } catch (_: SecurityException) {
            null
        }
        return advertised == TeoBleUuids.ADV_NAME || bonded == TeoBleUuids.ADV_NAME
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        mainHandler.removeCallbacks(scanTimeout)
        if (!scanning) return
        scanning = false
        try {
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "no pude detener el scan: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        if (closed || gatt != null) return
        gatt = try {
            device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: SecurityException) {
            Log.w(TAG, "connectGatt sin permiso: ${e.message}")
            emitFailure(Failure.NO_PERMISSION)
            null
        }
    }

    // ------------------------------------------------------------------
    // Operaciones del protocolo
    // ------------------------------------------------------------------

    /** Escribe el PIN del LCD. La respuesta llega por [onStatus]. */
    fun writePin(pin: String) {
        writeCharacteristic(TeoBleUuids.CHAR_PIN, pin.toByteArray(Charsets.US_ASCII))
    }

    /** Pide la lista de redes. El resultado llega por [onNetworks]. */
    @SuppressLint("MissingPermission")
    fun readScan() {
        val characteristic = characteristic(TeoBleUuids.CHAR_SCAN) ?: return
        val ok = try {
            gatt?.readCharacteristic(characteristic) == true
        } catch (e: SecurityException) {
            Log.w(TAG, "readCharacteristic sin permiso: ${e.message}")
            emitFailure(Failure.NO_PERMISSION)
            return
        }
        if (!ok) emitFailure(Failure.OPERATION_FAILED)
    }

    /** Manda las credenciales. La clave nunca se loguea. */
    fun writeConnect(ssid: String, password: String) {
        val payload = WifiNetworkUi.connectPayload(ssid, password)
        writeCharacteristic(TeoBleUuids.CHAR_CONNECT, payload.toByteArray(Charsets.UTF_8))
    }

    @SuppressLint("MissingPermission")
    private fun writeCharacteristic(uuid: java.util.UUID, value: ByteArray) {
        val activeGatt = gatt
        val characteristic = characteristic(uuid)
        if (activeGatt == null || characteristic == null) {
            emitFailure(Failure.SERVICE_MISSING)
            return
        }
        val ok = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activeGatt.writeCharacteristic(
                    characteristic,
                    value,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                run {
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    characteristic.value = value
                    activeGatt.writeCharacteristic(characteristic)
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "writeCharacteristic sin permiso: ${e.message}")
            emitFailure(Failure.NO_PERMISSION)
            return
        }
        if (!ok) emitFailure(Failure.OPERATION_FAILED)
    }

    private fun characteristic(uuid: java.util.UUID): BluetoothGattCharacteristic? {
        val service = gatt?.getService(TeoBleUuids.SERVICE)
        if (service == null) {
            emitFailure(Failure.SERVICE_MISSING)
            return null
        }
        return service.getCharacteristic(uuid)
    }

    // ------------------------------------------------------------------
    // Callbacks GATT (hilo del binder de Bluetooth)
    // ------------------------------------------------------------------

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> requestMtuThenDiscover(g)
                BluetoothGatt.STATE_DISCONNECTED -> {
                    val wasConnected = gatt != null
                    closeGatt()
                    if (!closed && wasConnected) emitFailure(Failure.DISCONNECTED)
                }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            // El valor negociado no cambia el protocolo: con MTU chico Android
            // fragmenta solo (long read / long write) y la Pi respeta `offset`.
            Log.d(TAG, "MTU negociado: $mtu (status=$status)")
            discoverOnce(g)
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                emitFailure(Failure.GATT_FAILED)
                return
            }
            val service = g.getService(TeoBleUuids.SERVICE)
            val statusChar = service?.getCharacteristic(TeoBleUuids.CHAR_STATUS)
            if (statusChar == null) {
                emitFailure(Failure.SERVICE_MISSING)
                return
            }
            try {
                g.setCharacteristicNotification(statusChar, true)
                val cccd = statusChar.getDescriptor(TeoBleUuids.CCCD)
                if (cccd == null) {
                    // Sin descriptor no hay notificaciones, pero el resto del flujo sirve.
                    emit { onReady?.invoke() }
                    return
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    run {
                        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        g.writeDescriptor(cccd)
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "suscripción sin permiso: ${e.message}")
                emitFailure(Failure.NO_PERMISSION)
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            emit { onReady?.invoke() }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            handleRead(characteristic, characteristic.value ?: ByteArray(0), status)
        }

        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            handleRead(characteristic, value, status)
        }

        override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) emitFailure(Failure.OPERATION_FAILED)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            handleNotification(characteristic, characteristic.value ?: ByteArray(0))
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleNotification(characteristic, value)
        }
    }

    /**
     * Pide un MTU grande antes de descubrir servicios.
     *
     * Con el MTU por defecto (23) la lista de redes vuelve como *long read* y
     * las credenciales salen como *long write*; con 517 entran en un solo
     * paquete. No damos la negociación por hecha: si `requestMtu` devuelve
     * false o la Pi nunca contesta, [mtuTimeout] sigue igual con el MTU chico.
     */
    @SuppressLint("MissingPermission")
    private fun requestMtuThenDiscover(g: BluetoothGatt) {
        val requested = try {
            g.requestMtu(PREFERRED_MTU)
        } catch (e: SecurityException) {
            Log.w(TAG, "requestMtu sin permiso: ${e.message}")
            emitFailure(Failure.NO_PERMISSION)
            return
        } catch (e: Exception) {
            Log.w(TAG, "requestMtu falló: ${e.message}")
            false
        }
        if (!requested) {
            discoverOnce(g)
            return
        }
        mainHandler.postDelayed(mtuTimeout, MTU_TIMEOUT_MS)
        pendingMtuGatt = g
    }

    // Se escriben desde el hilo del binder de Bluetooth y se leen desde el main.
    @Volatile private var pendingMtuGatt: BluetoothGatt? = null
    @Volatile private var servicesRequested = false

    private val mtuTimeout = Runnable {
        val g = pendingMtuGatt ?: return@Runnable
        Log.w(TAG, "la Pi no contestó el MTU; sigo con el MTU chico")
        discoverOnce(g)
    }

    /** Descubre servicios una sola vez, venga del MTU o del timeout. */
    @SuppressLint("MissingPermission")
    private fun discoverOnce(g: BluetoothGatt) {
        mainHandler.removeCallbacks(mtuTimeout)
        pendingMtuGatt = null
        if (closed || servicesRequested) return
        servicesRequested = true
        try {
            g.discoverServices()
        } catch (e: SecurityException) {
            Log.w(TAG, "discoverServices sin permiso: ${e.message}")
            emitFailure(Failure.NO_PERMISSION)
        }
    }

    private fun handleRead(characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
        if (characteristic.uuid != TeoBleUuids.CHAR_SCAN) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            emitFailure(Failure.OPERATION_FAILED)
            return
        }
        val networks = WifiNetworkUi.parseList(String(value, Charsets.UTF_8))
        emit { onNetworks?.invoke(networks) }
    }

    private fun handleNotification(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        if (characteristic.uuid != TeoBleUuids.CHAR_STATUS) return
        val status = String(value, Charsets.UTF_8).trim()
        if (status.isEmpty()) return
        emit { onStatus?.invoke(status) }
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------

    /** Corta scan y GATT. Idempotente; después de esto no se emiten callbacks. */
    fun close() {
        closed = true
        stopScan()
        closeGatt()
        onScanStarted = null
        onReady = null
        onStatus = null
        onNetworks = null
        onFailure = null
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        mainHandler.removeCallbacks(mtuTimeout)
        pendingMtuGatt = null
        servicesRequested = false
        val active = gatt ?: return
        gatt = null
        try {
            active.disconnect()
            active.close()
        } catch (e: Exception) {
            Log.w(TAG, "no pude cerrar el GATT: ${e.message}")
        }
    }

    private fun emitFailure(failure: Failure) = emit { onFailure?.invoke(failure) }

    private fun emit(action: () -> Unit) {
        mainHandler.post { if (!closed) action() }
    }

    companion object {
        private const val TAG = "TeoBleClient"

        /** Ventana de scan pedida por el plan. */
        const val SCAN_TIMEOUT_MS = 8_000L

        /** MTU que pedimos para que la lista de redes y las credenciales entren enteras. */
        private const val PREFERRED_MTU = 517

        /** Si la negociación de MTU no contesta, seguimos igual. */
        private const val MTU_TIMEOUT_MS = 3_000L

        /** Permisos runtime que faltan para escanear y hablar por BLE. */
        fun missingPermissions(context: Context): List<String> =
            requiredPermissions().filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }

        fun requiredPermissions(): List<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }

        fun isBluetoothOn(context: Context): Boolean {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            return manager?.adapter?.isEnabled == true
        }
    }
}
