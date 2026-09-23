package com.example.aplicacionparacelular.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingGateTest {
    @Test fun wifiWhenNoLanAndBle() {
        assertEquals(
            OnboardingScreen.WIFI,
            OnboardingGate.decide(hasLan = false, bleTeo = true, hasChildProfile = false),
        )
    }
    @Test fun retryWhenNoLanNoBle() {
        assertEquals(
            OnboardingScreen.RETRY,
            OnboardingGate.decide(hasLan = false, bleTeo = false, hasChildProfile = false),
        )
    }
    @Test fun childProfileWhenLanIncomplete() {
        assertEquals(
            OnboardingScreen.CHILD_PROFILE,
            OnboardingGate.decide(hasLan = true, bleTeo = false, hasChildProfile = false),
        )
    }
    @Test fun noneWhenLanComplete() {
        assertEquals(
            OnboardingScreen.NONE,
            OnboardingGate.decide(hasLan = true, bleTeo = true, hasChildProfile = true),
        )
    }
    @Test fun lanWinsOverBle() {
        assertEquals(
            OnboardingScreen.CHILD_PROFILE,
            OnboardingGate.decide(hasLan = true, bleTeo = true, hasChildProfile = false),
        )
    }

    @Test fun retryAndWifiAlwaysBlockChrome() {
        assertEquals(
            true,
            OnboardingGate.shouldBlockChrome(
                OnboardingScreen.RETRY,
                onChildProfileDestination = false,
            ),
        )
        assertEquals(
            true,
            OnboardingGate.shouldBlockChrome(
                OnboardingScreen.WIFI,
                onChildProfileDestination = false,
            ),
        )
        assertEquals(
            true,
            OnboardingGate.shouldBlockChrome(
                OnboardingScreen.CHILD_PROFILE,
                onChildProfileDestination = false,
            ),
        )
    }

    @Test fun stickyLockBlocksChromeOnChildProfileDestination() {
        assertEquals(
            true,
            OnboardingGate.shouldBlockChrome(
                OnboardingScreen.NONE,
                onChildProfileDestination = true,
            ),
        )
        assertEquals(
            false,
            OnboardingGate.shouldBlockChrome(
                OnboardingScreen.NONE,
                onChildProfileDestination = false,
            ),
        )
    }

    @Test fun adoptLanDiscoveryOnlyWhileDisconnected() {
        assertEquals(true, OnboardingGate.shouldAdoptLanDiscovery(hasLan = false))
        assertEquals(false, OnboardingGate.shouldAdoptLanDiscovery(hasLan = true))
    }
}
