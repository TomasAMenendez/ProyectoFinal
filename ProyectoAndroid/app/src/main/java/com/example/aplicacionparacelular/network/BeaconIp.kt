package com.example.aplicacionparacelular.network

/**
 * Elige la IP del peluche a partir del beacon UDP.
 *
 * El JSON puede traer `local_ip` = `0.0.0.0` si la Pi arrancó sin WiFi.
 * Esa dirección no es alcanzable. En ese caso usamos la IP de origen del paquete.
 */
object BeaconIp {

    fun isUsable(ip: String?): Boolean {
        val value = ip?.trim().orEmpty()
        if (value.isEmpty()) return false
        if (value == "0.0.0.0" || value == "::" || value == "::0") return false
        if (value.startsWith("127.")) return false
        return true
    }

    fun pick(advertised: String?, packetHost: String?): String {
        if (isUsable(advertised)) return advertised!!.trim()
        return packetHost?.trim().orEmpty()
    }
}
