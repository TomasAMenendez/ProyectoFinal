package com.example.aplicacionparacelular

import android.app.KeyguardManager
import android.content.Context
import android.Manifest
import android.os.Build
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.aplicacionparacelular.onboarding.TeoBleClient
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityStartupTest {
    @Before fun requireAnUnlockedDeviceAndPrepareRuntimePermissions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        assumeFalse("La recreación requiere que el usuario desbloquee el teléfono", keyguard.isKeyguardLocked)
        val permissions = TeoBleClient.requiredPermissions().toMutableList()
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        permissions.forEach { instrumentation.uiAutomation.grantRuntimePermission(context.packageName, it) }
    }

    @Test fun navigationIsAvailableDuringLaunchAndRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                assertNotNull(host.navController.currentDestination)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                val host = activity.supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                assertNotNull(host.navController.currentDestination)
            }
        }
    }
}
