package com.example.aplicacionparacelular.onboarding

enum class OnboardingScreen { WIFI, RETRY, CHILD_PROFILE, NONE }

enum class GatePlace { OTHER, WIFI_SETUP, FIRST_USE_PROFILE, FORM, RETRY }

enum class GateMove { TO_WIFI, TO_RETRY, TO_PROFILE, POP_TO_HOME, STAY }

object OnboardingGate {
    fun decide(hasLan: Boolean, bleTeo: Boolean, hasChildProfile: Boolean): OnboardingScreen {
        if (!hasLan) return if (bleTeo) OnboardingScreen.WIFI else OnboardingScreen.RETRY
        return if (!hasChildProfile) OnboardingScreen.CHILD_PROFILE else OnboardingScreen.NONE
    }

    fun shouldBlockChrome(
        screen: OnboardingScreen,
        onChildProfileDestination: Boolean,
    ): Boolean {
        if (onChildProfileDestination) return true
        return screen != OnboardingScreen.NONE
    }

    /** Un beacon UDP nuevo solo se adopta si todavía no hay LAN al peluche. */
    fun shouldAdoptLanDiscovery(hasLan: Boolean): Boolean = !hasLan

    /**
     * Un formulario de Mi TEO o el primer uso no se abandonan por un cambio de
     * estado. La recuperación BLE tampoco se pisa mientras el adulto escribe el PIN.
     */
    fun move(screen: OnboardingScreen, place: GatePlace): GateMove = when (screen) {
        OnboardingScreen.WIFI -> when (place) {
            GatePlace.WIFI_SETUP, GatePlace.FORM -> GateMove.STAY
            else -> GateMove.TO_WIFI
        }
        OnboardingScreen.RETRY -> when (place) {
            GatePlace.WIFI_SETUP, GatePlace.FORM -> GateMove.STAY
            else -> GateMove.TO_RETRY
        }
        OnboardingScreen.CHILD_PROFILE -> when (place) {
            GatePlace.FIRST_USE_PROFILE, GatePlace.FORM -> GateMove.STAY
            else -> GateMove.TO_PROFILE
        }
        OnboardingScreen.NONE -> if (place == GatePlace.RETRY) GateMove.POP_TO_HOME else GateMove.STAY
    }
}
