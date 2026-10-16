package com.example.aplicacionparacelular.myteo

import org.json.JSONObject

enum class ConnectionPhase { CONNECTED, DISCONNECTED, SEARCHING, FAILED }

data class WifiChoice(val ssid: String, val secure: Boolean)

object ConnectionRules {
    fun phase(connected: Boolean, searching: Boolean, failed: Boolean): ConnectionPhase {
        if (searching && !connected) return ConnectionPhase.SEARCHING
        if (connected) return ConnectionPhase.CONNECTED
        if (failed) return ConnectionPhase.FAILED
        return ConnectionPhase.DISCONNECTED
    }

    /**
     * `current` es el nombre del perfil de NetworkManager. Puede no ser el SSID.
     * Una cadena vacía no se reemplaza por un nombre de ejemplo.
     */
    fun profileLabel(current: String?): String {
        val name = current?.trim().orEmpty()
        if (name.isEmpty()) return ""
        return name
    }

    fun parseNetworks(data: JSONObject): Pair<String, List<WifiChoice>> {
        val profile = profileLabel(data.optString("current", ""))
        val array = data.optJSONArray("networks")
        val choices = mutableListOf<WifiChoice>()
        if (array != null) {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val ssid = item.optString("ssid", "").trim()
                if (ssid.isEmpty() || ssid == profile) continue
                choices.add(WifiChoice(ssid, item.optBoolean("secure", true)))
            }
        }
        return profile to choices
    }
}

object ManualAddress {
    fun problem(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return "Escribí la dirección de TEO."
        val parts = value.split(".")
        if (parts.size != 4) return "La dirección tiene que tener cuatro números."
        val octets = parts.map { it.toIntOrNull() }
        if (octets.any { it == null || it !in 0..255 }) {
            return "Cada número tiene que estar entre 0 y 255."
        }
        if (value == "0.0.0.0" || value.startsWith("127.")) {
            return "Esa dirección no sirve para llegar a TEO."
        }
        return null
    }

    fun effectivePort(stored: Int): Int = if (stored in 1..65535) stored else 8080
}
