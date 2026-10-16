package com.example.aplicacionparacelular.ui.dashboard

import com.example.aplicacionparacelular.network.ApiResult
import org.json.JSONObject

object CelebrationResult {
    const val SENT = "Celebración enviada a TEO"
    const val ASLEEP = "TEO está dormido"
    const val UNAVAILABLE = "Celebración no disponible"
    const val DISCONNECTED = "Sin conexión. No se envió la celebración."

    fun from(connected: Boolean, result: ApiResult<JSONObject>): String {
        if (!connected) return DISCONNECTED
        return when (result) {
            is ApiResult.Error -> when {
                result.message.contains("no disponible", ignoreCase = true) -> UNAVAILABLE
                result.message.contains("dormido", ignoreCase = true) -> ASLEEP
                result.message.isBlank() -> "No se pudo enviar la celebración."
                else -> result.message
            }
            is ApiResult.Success -> fromBody(result.data)
        }
    }

    fun fromBody(data: JSONObject): String {
        val message = data.optString("message", "")
        return when {
            message.contains("enviada", ignoreCase = true) -> SENT
            message.contains("dormido", ignoreCase = true) -> ASLEEP
            message.contains("no disponible", ignoreCase = true) -> UNAVAILABLE
            else -> message.ifBlank { "No se pudo confirmar la celebración." }
        }
    }
}
