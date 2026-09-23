package com.example.aplicacionparacelular.onboarding

import java.util.UUID

/**
 * Contrato GATT del peluche. Los strings tienen que coincidir carácter por
 * carácter con `ble_setup.py` de la Raspberry Pi.
 */
object TeoBleUuids {

    /** Nombre exacto del anuncio BLE de la Pi. */
    const val ADV_NAME = "TEO"

    val SERVICE: UUID = UUID.fromString("74656f57-4946-4000-8000-0000a0000001")

    /** Write: PIN de 6 dígitos en ASCII. */
    val CHAR_PIN: UUID = UUID.fromString("74656f57-4946-4000-8000-0000a0000002")

    /** Read: array JSON con las redes visibles. */
    val CHAR_SCAN: UUID = UUID.fromString("74656f57-4946-4000-8000-0000a0000003")

    /** Write: JSON `{ssid, password}`. */
    val CHAR_CONNECT: UUID = UUID.fromString("74656f57-4946-4000-8000-0000a0000004")

    /** Notify: estado de la sesión en UTF-8. */
    val CHAR_STATUS: UUID = UUID.fromString("74656f57-4946-4000-8000-0000a0000005")

    /** Descriptor estándar para habilitar las notificaciones de CHAR_STATUS. */
    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val PIN_LENGTH = 6

    fun isValidPin(pin: String): Boolean =
        pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }
}

/**
 * Los nueve estados del contrato que la Pi publica por CHAR_STATUS.
 *
 * Cualquier otro valor se trata como desconocido: si empieza con `failed:` es
 * un error con reintento, y si no, se deja la salida a mano visible. Así un
 * cambio del lado de la Pi nunca deja al padre encerrado.
 */
object TeoBleStatus {
    const val IDLE = "idle"
    const val PIN_OK = "pin_ok"
    const val SCANNING = "scanning"
    const val CONNECTING = "connecting"
    const val CONNECTED = "connected"
    const val FAILED_PIN = "failed:pin"
    const val FAILED_SESSION = "failed:session"
    const val FAILED_AUTH = "failed:auth"
    const val FAILED_TIMEOUT = "failed:timeout"

    /** El contrato completo, en el orden del plan. */
    val CONTRACT = listOf(
        IDLE,
        PIN_OK,
        SCANNING,
        CONNECTING,
        CONNECTED,
        FAILED_PIN,
        FAILED_SESSION,
        FAILED_AUTH,
        FAILED_TIMEOUT,
    )

    private const val FAILED_PREFIX = "failed"

    /** Traduce un estado crudo de CHAR_STATUS a lo que tiene que hacer la UI. */
    fun effectOf(raw: String): TeoStatusEffect = when (val status = raw.trim().lowercase()) {
        IDLE -> TeoStatusEffect.WAIT_PIN
        PIN_OK -> TeoStatusEffect.PIN_ACCEPTED
        SCANNING -> TeoStatusEffect.SCANNING_NETWORKS
        CONNECTING -> TeoStatusEffect.CONNECTING_WIFI
        CONNECTED -> TeoStatusEffect.JOINED_WIFI
        FAILED_PIN -> TeoStatusEffect.RETRY_PIN
        FAILED_SESSION -> TeoStatusEffect.RESTART_SESSION
        FAILED_AUTH -> TeoStatusEffect.RETRY_PASSWORD
        FAILED_TIMEOUT -> TeoStatusEffect.SESSION_EXPIRED
        else ->
            if (status.startsWith(FAILED_PREFIX)) TeoStatusEffect.UNKNOWN_FAILURE
            else TeoStatusEffect.UNKNOWN
    }
}
