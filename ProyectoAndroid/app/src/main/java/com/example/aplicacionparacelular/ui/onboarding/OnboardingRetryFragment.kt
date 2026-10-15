package com.example.aplicacionparacelular.ui.onboarding

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.aplicacionparacelular.MainActivity
import com.example.aplicacionparacelular.databinding.FragmentOnboardingRetryBinding
import com.example.aplicacionparacelular.network.RobotDiscovery
import com.example.aplicacionparacelular.onboarding.BlePermissionUi
import com.example.aplicacionparacelular.onboarding.FindTeoPhase
import com.example.aplicacionparacelular.onboarding.FindTeoRules
import com.example.aplicacionparacelular.onboarding.TeoBleClient
import com.example.aplicacionparacelular.ui.SystemBarInsets

/**
 * Pantalla bloqueante cuando no hay LAN ni un peluche visible por BLE.
 * Reintentar dispara el descubrimiento UDP y un scan BLE; el gate sale de acá
 * cuando alguno responde.
 */
class OnboardingRetryFragment : Fragment() {

    private var _binding: FragmentOnboardingRetryBinding? = null
    private val binding get() = _binding!!
    private val mainHandler = Handler(Looper.getMainLooper())
    private var searching = false
    private var permissionAsked = false
    private var permissionBlocked = false

    private val searchFinished = Runnable {
        if (_binding == null) return@Runnable
        searching = false
        render()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (_binding == null) return@registerForActivityResult
        permissionAsked = true
        if (grants.values.all { it }) {
            permissionBlocked = false
            beginSearch()
            return@registerForActivityResult
        }
        permissionBlocked = BlePermissionUi.permanentlyDenied(grants) { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)
        }
        searching = false
        render()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOnboardingRetryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.findScroll)
        permissionAsked = savedInstanceState?.getBoolean(KEY_ASKED) == true
        permissionBlocked = savedInstanceState?.getBoolean(KEY_BLOCKED) == true

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().moveTaskToBack(true)
                }
            },
        )

        binding.btnRetryDiscovery.setOnClickListener { retry() }
        binding.btnRetryBluetoothSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        binding.btnRetryAppSettings.setOnClickListener { openAppSettings() }

        if (savedInstanceState == null) retry() else render()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null && !searching) render()
    }

    private fun retry() {
        if (!FindTeoRules.canStartSearch(currentPhase())) return
        val phase = currentPhase()
        if (phase == FindTeoPhase.PERMISSION_NEEDED || phase == FindTeoPhase.PERMISSION_BLOCKED) {
            val missing = TeoBleClient.missingPermissions(requireContext())
            if (missing.isEmpty()) {
                beginSearch()
            } else {
                permissionLauncher.launch(missing.toTypedArray())
            }
            return
        }
        if (phase == FindTeoPhase.BLUETOOTH_OFF) {
            render()
            return
        }
        beginSearch()
    }

    private fun beginSearch() {
        if (searching) return
        searching = true
        render()
        RobotDiscovery.startScan(requireContext())
        (activity as? MainActivity)?.refreshBlePresence()
        mainHandler.removeCallbacks(searchFinished)
        mainHandler.postDelayed(searchFinished, TeoBleClient.SCAN_TIMEOUT_MS)
    }

    private fun render() {
        if (_binding == null) return
        FindTeoScreen.render(binding, currentPhase())
    }

    private fun currentPhase(): FindTeoPhase {
        val granted = TeoBleClient.missingPermissions(requireContext()).isEmpty()
        return FindTeoRules.phase(
            searching = searching,
            bluetoothOn = granted && bluetoothEnabled(),
            permissionsGranted = granted,
            permissionBlocked = permissionBlocked && permissionAsked,
        )
    }

    private fun bluetoothEnabled(): Boolean = try {
        val manager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter?.isEnabled == true
    } catch (_: SecurityException) {
        false
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null),
        )
        startActivity(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_ASKED, permissionAsked)
        outState.putBoolean(KEY_BLOCKED, permissionBlocked)
    }

    override fun onDestroyView() {
        mainHandler.removeCallbacks(searchFinished)
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_ASKED = "find_permission_asked"
        private const val KEY_BLOCKED = "find_permission_blocked"
    }
}
