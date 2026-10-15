package com.example.aplicacionparacelular.onboarding

enum class FindTeoPhase {
    SEARCHING,
    NOT_FOUND,
    BLUETOOTH_OFF,
    PERMISSION_NEEDED,
    PERMISSION_BLOCKED,
}

enum class FindHint { POWER, BLUETOOTH, WIFI }

object FindTeoRules {
    fun phase(
        searching: Boolean,
        bluetoothOn: Boolean,
        permissionsGranted: Boolean,
        permissionBlocked: Boolean,
    ): FindTeoPhase {
        if (!permissionsGranted) {
            return if (permissionBlocked) FindTeoPhase.PERMISSION_BLOCKED else FindTeoPhase.PERMISSION_NEEDED
        }
        if (!bluetoothOn) return FindTeoPhase.BLUETOOTH_OFF
        return if (searching) FindTeoPhase.SEARCHING else FindTeoPhase.NOT_FOUND
    }

    fun hints(phase: FindTeoPhase): Set<FindHint> = when (phase) {
        FindTeoPhase.NOT_FOUND -> setOf(FindHint.POWER, FindHint.BLUETOOTH, FindHint.WIFI)
        FindTeoPhase.BLUETOOTH_OFF -> setOf(FindHint.BLUETOOTH)
        else -> emptySet()
    }

    fun canStartSearch(phase: FindTeoPhase): Boolean = phase != FindTeoPhase.SEARCHING
}

object WifiStepRules {
    fun showsPin(pinAccepted: Boolean): Boolean = !pinAccepted

    fun showsNetworks(pinAccepted: Boolean): Boolean = pinAccepted

    fun showsBluetoothChip(connectedByBle: Boolean, httpChange: Boolean): Boolean =
        connectedByBle && !httpChange
}

object WifiDraft {
    fun passwordAfterRefresh(previousSsid: String?, selectedSsid: String?, typed: String): String =
        if (previousSsid != null && previousSsid == selectedSsid) typed else ""
}
