package com.example.aplicacionparacelular.onboarding

import org.json.JSONObject

/**
 * Lógica pura del provisionamiento WiFi por BLE, separada de la radio y de la
 * UI para poder testearla sin celular ni peluche.
 */

/** Qué tiene que hacer la pantalla con un estado que llegó de la Pi. */
enum class TeoStatusEffect {
    /** La sesión espera el PIN (o volvió a esperarlo). */
    WAIT_PIN,

    /** PIN aceptado: se puede pedir la lista de redes. */
    PIN_ACCEPTED,

    /** La Pi está buscando redes; hay que esperar. */
    SCANNING_NETWORKS,

    /** La Pi está intentando entrar a la red elegida. */
    CONNECTING_WIFI,

    /** La Pi entró a la red: hay que redescubrirla en la LAN. */
    JOINED_WIFI,

    /** PIN equivocado: se vuelve a pedir. */
    RETRY_PIN,

    /** La sesión se reinició: el PIN de la pantallita cambió. */
    RESTART_SESSION,

    /** La contraseña WiFi no sirvió: solo se vuelve a pedir la clave. */
    RETRY_PASSWORD,

    /** Pasaron los 10 minutos de la sesión de la Pi. */
    SESSION_EXPIRED,

    /** Un `failed:*` que no está en el contrato: error genérico con reintento. */
    UNKNOWN_FAILURE,

    /** Un valor que no entendemos: no es error, pero dejamos salida a mano. */
    UNKNOWN,
}

/**
 * Decide si el scan de presencia puede correr o si hay que contestar ya mismo.
 *
 * Existe para garantizar que `onResult` se llame **siempre**: el bug original
 * era un `return` temprano que dejaba a la app clavada en la pantalla de
 * reintentar después de recrear la Activity.
 */
object BlePresenceLogic {

    /**
     * Respuesta inmediata para [TeoBlePresence.scanOnce], o `null` si hay que
     * escanear de verdad.
     */
    fun immediateAnswer(
        busy: Boolean,
        scannerAvailable: Boolean,
        permissionsGranted: Boolean,
    ): Boolean? = if (busy || !scannerAvailable || !permissionsGranted) false else null
}

/** Envoltorio que garantiza que una respuesta se entregue una sola vez. */
class SingleShot<T>(private val action: (T) -> Unit) {

    private var fired = false

    /** Entrega [value] si nadie contestó antes. Devuelve true si entregó. */
    fun deliver(value: T): Boolean {
        if (fired) return false
        fired = true
        action(value)
        return true
    }

    fun isFired(): Boolean = fired
}

/** Qué hacer con la lista de redes cuando llega un estado de la Pi. */
enum class ScanListAction {
    /** Leer CHAR_SCAN. */
    READ,

    /** Ya tenemos lo que vamos a mostrar: apagar el spinner y mostrar la lista. */
    SETTLE,

    /** Hay lista usable y la Pi está refrescando: avisar sin bloquear. */
    REFRESH_HINT,

    /** Todavía no hay nada que mostrar: spinner. */
    WAIT,

    /** No tocar la pantalla. */
    KEEP,
}

/**
 * Corta el ping-pong de scans con la Pi.
 *
 * El bug: la pantalla releía CHAR_SCAN en **cada** `pin_ok` y cada lectura hacía
 * que la Pi volviera a escanear, notificara `scanning` y terminara otra vez en
 * `pin_ok`. La Pi ahora refresca solo con el cache vacío o vencido; esta es la
 * otra mitad: con una lista ya en pantalla nunca volvemos a leer solos.
 */
object WifiScanPolicy {

    /** Lecturas automáticas que nos permitimos mientras la lista siga vacía. */
    const val MAX_AUTO_READS = 3

    /**
     * Decide qué hacer con un `pin_ok`. El primero llega tras verificar el PIN,
     * pero la Pi puede repetirlo cada vez que su sesión vuelve a reposo.
     */
    fun onPinOk(
        hasNetworks: Boolean,
        userChoosing: Boolean,
        readInFlight: Boolean,
        autoReads: Int,
    ): ScanListAction = when {
        userChoosing -> ScanListAction.KEEP
        readInFlight -> ScanListAction.KEEP
        hasNetworks -> ScanListAction.SETTLE
        autoReads >= MAX_AUTO_READS -> ScanListAction.SETTLE
        else -> ScanListAction.READ
    }

    /** Decide qué hacer con un `scanning`. Nunca borra una lista usable. */
    fun onScanning(hasNetworks: Boolean, userChoosing: Boolean): ScanListAction = when {
        userChoosing -> ScanListAction.KEEP
        hasNetworks -> ScanListAction.REFRESH_HINT
        else -> ScanListAction.WAIT
    }

    /** El botón "Buscar redes de nuevo" siempre lee, salvo que ya haya un read en vuelo. */
    fun allowsManualRead(readInFlight: Boolean): Boolean = !readInFlight

    /**
     * Qué lista mostrar cuando llega una respuesta de CHAR_SCAN. Una respuesta
     * vacía de un read automático no borra las redes que el padre ya ve; un
     * refresh pedido a mano sí manda, para poder ver que de verdad no hay nada.
     */
    fun listToShow(
        shown: List<WifiNetworkUi>,
        answer: List<WifiNetworkUi>,
        manual: Boolean,
    ): List<WifiNetworkUi> =
        if (answer.isEmpty() && !manual && shown.isNotEmpty()) shown else answer
}

/** Decisiones sobre los permisos de Bluetooth que la pantalla tiene que explicar. */
object BlePermissionUi {

    /**
     * True si algún permiso quedó denegado y el sistema ya no deja volver a
     * pedirlo ("no preguntar de nuevo"): la única salida son los ajustes de la
     * app.
     */
    fun permanentlyDenied(
        grants: Map<String, Boolean>,
        showRationale: (String) -> Boolean,
    ): Boolean = grants.any { (permission, granted) -> !granted && !showRationale(permission) }
}

/**
 * Lectura tolerante de la respuesta de `POST /api/wifi/provisioning`.
 *
 * La Pi contesta 200 con `{"status":"ok","enabled":true}` cuando arrancó el
 * BLE, y marca la falla explícitamente cuando no hay radio disponible. Acá
 * solo tratamos como fracaso las señales **explícitas** de fracaso: un cambio
 * chico de contrato del lado de la Pi no tiene que romper la app.
 */
object WifiProvisioningResponse {

    private val FAILED_STATUSES = setOf("error", "failed", "fail", "unavailable")

    private val FLAGS = listOf("enabled", "started", "ok", "success", "ble_available")

    fun startedOk(body: JSONObject?): Boolean {
        if (body == null) return false
        val status = body.optString("status", "").trim().lowercase()
        if (status in FAILED_STATUSES) return false
        if (body.has("error") && body.optString("error", "").isNotBlank()) return false
        return FLAGS.none { body.has(it) && !truthy(body.opt(it)) }
    }

    private fun truthy(value: Any?): Boolean = when (value) {
        null, JSONObject.NULL -> false
        is Boolean -> value
        is Number -> value.toDouble() != 0.0
        is String -> value.trim().lowercase() in setOf("true", "1", "yes", "ok", "si", "sí")
        else -> true
    }
}
