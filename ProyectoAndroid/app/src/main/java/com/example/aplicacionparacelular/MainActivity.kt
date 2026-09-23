package com.example.aplicacionparacelular

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.NavOptions
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.aplicacionparacelular.databinding.ActivityMainBinding
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.onboarding.OnboardingGate
import com.example.aplicacionparacelular.onboarding.OnboardingScreen
import com.example.aplicacionparacelular.onboarding.TeoBleClient
import com.example.aplicacionparacelular.onboarding.TeoBlePresence
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var onboardingLocked = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val _bleTeo = MutableLiveData(false)

    /** True mientras se vea un peluche anunciándose como `TEO` por BLE. */
    val bleTeo: LiveData<Boolean> = _bleTeo

    private var started = false
    private var blePresenceScanning = false
    private var bleLoopScheduled = false
    private var blePermissionRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        // Initialize Robot connection (auto-discovers or loads saved IP)
        RobotConnectionManager.init(this)
        requestNotificationPermission()
        com.example.aplicacionparacelular.notifications.AppNotificationHelper.ensureChannels(this)
        com.example.aplicacionparacelular.notifications.ReminderScheduler.restoreAll(this)
        com.example.aplicacionparacelular.notifications.TeoAlertPollService.start(this)

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        val navController = navHostFragment.navController

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton?>(R.id.fab)
            ?.visibility = android.view.View.GONE

        val topLevel = setOf(
            R.id.nav_dashboard,
            R.id.nav_routines,
            R.id.nav_stories,
            R.id.nav_medical,
        )
        val navView = findViewById<NavigationView>(R.id.nav_view)
        appBarConfiguration = if (navView != null) {
            AppBarConfiguration(topLevel, binding.drawerLayout)
        } else {
            AppBarConfiguration(topLevel)
        }
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView?.setupWithNavController(navController)
        binding.appBarMain.contentMain.bottomNavView?.setupWithNavController(navController)

        RobotConnectionManager.isConnected.observe(this) {
            applyOnboardingGate()
            scheduleBlePresenceScan(immediate = true)
        }
        RobotConnectionManager.hasChildProfile.observe(this) { applyOnboardingGate() }
        _bleTeo.observe(this) { applyOnboardingGate() }
    }

    override fun onStart() {
        super.onStart()
        started = true
        scheduleBlePresenceScan(immediate = true)
    }

    override fun onStop() {
        started = false
        mainHandler.removeCallbacks(blePresenceTick)
        mainHandler.removeCallbacks(blePresenceGuard)
        blePresenceScanning = false
        bleLoopScheduled = false
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        // El poll de avisos del peluche sigue en TeoAlertPollService.
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (onboardingLocked) return false
        menuInflater.inflate(R.menu.overflow, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.nav_config) {
            if (onboardingLocked) return true
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            if (navController.currentDestination?.id != R.id.nav_config) {
                navController.navigate(R.id.nav_config)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (onboardingLocked) {
            moveTaskToBack(true)
            return true
        }
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun applyOnboardingGate() {
        val hasLan = RobotConnectionManager.isConnected.value == true
        val bleTeo = _bleTeo.value == true
        val hasChildProfile = RobotConnectionManager.hasChildProfile.value != false
        val screen = OnboardingGate.decide(hasLan, bleTeo, hasChildProfile)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val dest = navController.currentDestination?.id
        when (screen) {
            OnboardingScreen.WIFI -> navigateToOnboarding(R.id.nav_wifi_setup, dest)
            OnboardingScreen.RETRY -> {
                // Si el padre ya está tipeando el PIN, un scan BLE que no vio a TEO
                // no tiene que arrancarlo de la pantalla de WiFi.
                if (dest != R.id.nav_wifi_setup) {
                    navigateToOnboarding(R.id.nav_onboarding_retry, dest)
                }
            }
            OnboardingScreen.CHILD_PROFILE -> navigateToOnboarding(R.id.nav_child_profile, dest)
            OnboardingScreen.NONE -> {
                // nav_wifi_setup no se cierra desde acá: puede venir de "Cambiar
                // red WiFi" con LAN viva, y el fragment se saca solo al terminar.
                if (dest in ONBOARDING_POP_DESTINATIONS) {
                    navController.popBackStack(R.id.nav_dashboard, false)
                }
            }
        }

        val destAfter = navController.currentDestination?.id
        val blocking = OnboardingGate.shouldBlockChrome(
            screen,
            destAfter == R.id.nav_child_profile,
        )
        val wasLocked = onboardingLocked
        onboardingLocked = blocking
        if (wasLocked != blocking) invalidateOptionsMenu()

        val showChrome = !blocking
        binding.appBarMain.contentMain.bottomNavView?.visibility =
            if (showChrome) View.VISIBLE else View.GONE
        binding.appBarMain.toolbar.visibility =
            if (showChrome) View.VISIBLE else View.GONE
        findViewById<NavigationView>(R.id.nav_view)?.visibility =
            if (showChrome) View.VISIBLE else View.GONE
        findViewById<DrawerLayout>(R.id.drawer_layout)?.setDrawerLockMode(
            if (showChrome) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
        )
    }

    private fun navigateToOnboarding(destinationId: Int, currentDestination: Int?) {
        if (currentDestination == destinationId) return
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(destinationId, false)
            .build()
        navController.navigate(destinationId, null, options)
    }

    // ------------------------------------------------------------------
    // Presencia BLE del peluche
    // ------------------------------------------------------------------

    /** Fuerza un scan BLE nuevo. Lo usa el botón Reintentar del onboarding. */
    fun refreshBlePresence() {
        blePermissionRequested = false
        scheduleBlePresenceScan(immediate = true)
    }

    private val blePresenceTick = Runnable {
        bleLoopScheduled = false
        scheduleBlePresenceScan(immediate = true)
    }

    /**
     * Mientras no haya LAN, busca cada tanto un peluche anunciándose como `TEO`.
     * Con LAN la radio se apaga: el gate ya no mira BLE.
     */
    private fun scheduleBlePresenceScan(immediate: Boolean) {
        if (!started || blePresenceScanning) return
        if (RobotConnectionManager.isConnected.value == true) {
            mainHandler.removeCallbacks(blePresenceTick)
            bleLoopScheduled = false
            if (_bleTeo.value == true) _bleTeo.value = false
            return
        }
        if (!immediate) {
            armBleLoop()
            return
        }
        if (TeoBleClient.missingPermissions(this).isNotEmpty()) {
            requestBlePermissions()
            return
        }
        blePresenceScanning = true
        // Cinturón y tirantes: TeoBlePresence garantiza una respuesta, pero si
        // por algún motivo no llega, este guardia devuelve el loop a la vida en
        // lugar de dejar al padre encerrado en la pantalla de reintentar.
        mainHandler.postDelayed(blePresenceGuard, BLE_PRESENCE_GUARD_MS)
        TeoBlePresence.scanOnce(this) { found ->
            mainHandler.removeCallbacks(blePresenceGuard)
            blePresenceScanning = false
            if (_bleTeo.value != found) _bleTeo.value = found
            armBleLoop()
        }
    }

    private val blePresenceGuard = Runnable {
        if (!blePresenceScanning) return@Runnable
        blePresenceScanning = false
        if (_bleTeo.value == true) _bleTeo.value = false
        armBleLoop()
    }

    private fun armBleLoop() {
        if (!started || bleLoopScheduled) return
        if (RobotConnectionManager.isConnected.value == true) return
        bleLoopScheduled = true
        mainHandler.postDelayed(blePresenceTick, BLE_PRESENCE_INTERVAL_MS)
    }

    private fun requestBlePermissions() {
        if (blePermissionRequested) {
            // Sin permiso no hay scan: el gate cae en la pantalla de reintentar,
            // que vuelve a pedirlo cuando el padre toca el botón.
            if (_bleTeo.value == true) _bleTeo.value = false
            return
        }
        blePermissionRequested = true
        ActivityCompat.requestPermissions(
            this,
            TeoBleClient.requiredPermissions().toTypedArray(),
            REQUEST_BLE_PERMISSIONS,
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_BLE_PERMISSIONS) return
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            scheduleBlePresenceScan(immediate = true)
        } else {
            if (_bleTeo.value == true) _bleTeo.value = false
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1001,
        )
    }

    companion object {
        private const val REQUEST_BLE_PERMISSIONS = 1002
        private const val BLE_PRESENCE_INTERVAL_MS = 15_000L

        /** Ventana de scan (8 s) más margen: si no contestó, algo se colgó. */
        private const val BLE_PRESENCE_GUARD_MS = 15_000L

        private val ONBOARDING_POP_DESTINATIONS = setOf(
            R.id.nav_child_profile,
            R.id.nav_onboarding_retry,
        )
    }
}
