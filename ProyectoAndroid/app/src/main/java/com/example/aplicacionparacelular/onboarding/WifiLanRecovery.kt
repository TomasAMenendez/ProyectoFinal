package com.example.aplicacionparacelular.onboarding

enum class WifiLanPhase { PROVISIONING, WAITING, NOT_FOUND, CONFIRMED }

/** El WiFi confirmado por BLE todavía necesita una respuesta real por LAN. */
class WifiLanRecovery {
    var phase: WifiLanPhase = WifiLanPhase.PROVISIONING
        private set

    val hasJoinedWifi: Boolean get() = phase != WifiLanPhase.PROVISIONING

    fun joined() {
        phase = WifiLanPhase.WAITING
    }

    fun timeOut(): Boolean {
        if (phase != WifiLanPhase.WAITING) return false
        phase = WifiLanPhase.NOT_FOUND
        return true
    }

    fun retryLan(): Boolean {
        if (!hasJoinedWifi) return false
        phase = WifiLanPhase.WAITING
        return true
    }

    fun confirmed() {
        if (hasJoinedWifi) phase = WifiLanPhase.CONFIRMED
    }
}
