package com.example.aplicacionparacelular.onboarding

import org.json.JSONArray
import org.json.JSONObject

/** Red WiFi visible desde el peluche, tal como la lista CHAR_SCAN. */
data class WifiNetworkUi(
    val ssid: String,
    val signal: Int,
    val secure: Boolean,
) {
    companion object {

        /**
         * Parsea el array JSON de CHAR_SCAN. Un payload roto o incompleto
         * devuelve lista vacía: la pantalla muestra "no encontré redes" en
         * lugar de romperse con el peluche en la mano.
         */
        fun parseList(json: String): List<WifiNetworkUi> {
            val array = try {
                JSONArray(json)
            } catch (_: Exception) {
                return emptyList()
            }
            val best = LinkedHashMap<String, WifiNetworkUi>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val network = parseItem(item) ?: continue
                val previous = best[network.ssid]
                if (previous == null || network.signal > previous.signal) {
                    best[network.ssid] = network
                }
            }
            return best.values.sortedByDescending { it.signal }
        }

        /**
         * JSON para CHAR_CONNECT. Se arma a mano para fijar el orden de las
         * claves; la clave nunca se loguea.
         */
        fun connectPayload(ssid: String, password: String): String =
            "{\"ssid\":${JSONObject.quote(ssid)},\"password\":${JSONObject.quote(password)}}"

        private fun parseItem(item: JSONObject): WifiNetworkUi? {
            val ssid = item.optString("ssid", "").trim()
            if (ssid.isEmpty()) return null
            return WifiNetworkUi(
                ssid = ssid,
                signal = item.optInt("signal", 0).coerceIn(0, 100),
                secure = item.optBoolean("secure", true),
            )
        }
    }
}
