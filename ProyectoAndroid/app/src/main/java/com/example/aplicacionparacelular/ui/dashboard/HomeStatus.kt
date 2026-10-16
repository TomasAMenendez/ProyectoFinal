package com.example.aplicacionparacelular.ui.dashboard

import org.json.JSONObject

enum class TeoActivity {
    AWAKE,
    ASLEEP,
    PREPARING,
    UNAVAILABLE,
}

data class HomeStatusView(
    val connected: Boolean,
    val activity: TeoActivity,
    val connectionLabel: String,
    val activityLabel: String,
    val actionLabel: String,
    val actionEnabled: Boolean,
)

data class PowerReading(
    val powerOn: Boolean,
    val modelsReady: Boolean,
    val pendingWake: Boolean,
    val wakeResult: String?,
)

object HomeStatus {
    fun from(
        connected: Boolean,
        powerOn: Boolean,
        modelsReady: Boolean,
        pendingWake: Boolean,
    ): HomeStatusView {
        if (!connected) {
            return HomeStatusView(
                connected = false,
                activity = TeoActivity.UNAVAILABLE,
                connectionLabel = "Desconectado",
                activityLabel = "Sin actividad",
                actionLabel = "Dormir TEO",
                actionEnabled = false,
            )
        }
        if (pendingWake) {
            return preparing()
        }
        if (powerOn) {
            return HomeStatusView(
                connected = true,
                activity = TeoActivity.AWAKE,
                connectionLabel = "Conectado",
                activityLabel = "Despierto",
                actionLabel = "Dormir TEO",
                actionEnabled = true,
            )
        }
        if (!modelsReady) {
            return preparing()
        }
        return HomeStatusView(
            connected = true,
            activity = TeoActivity.ASLEEP,
            connectionLabel = "Conectado",
            activityLabel = "Dormido",
            actionLabel = "Despertar TEO",
            actionEnabled = true,
        )
    }

    fun reading(data: JSONObject): PowerReading {
        val wake = data.optString("wake_result", "").ifBlank { null }
        return PowerReading(
            powerOn = data.optBoolean("power_on", false),
            modelsReady = data.optBoolean("models_ready", false),
            pendingWake = data.optBoolean("pending_wake", false),
            wakeResult = wake,
        )
    }

    private fun preparing() = HomeStatusView(
        connected = true,
        activity = TeoActivity.PREPARING,
        connectionLabel = "Conectado",
        activityLabel = "Preparándose",
        actionLabel = "Despertar TEO",
        actionEnabled = false,
    )
}
