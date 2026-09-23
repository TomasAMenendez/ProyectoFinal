package com.example.aplicacionparacelular.onboarding

enum class OnboardingScreen { WIFI, RETRY, CHILD_PROFILE, NONE }

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
}
