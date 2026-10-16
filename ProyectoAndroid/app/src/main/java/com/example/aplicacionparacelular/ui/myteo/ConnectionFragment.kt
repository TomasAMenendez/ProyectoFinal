package com.example.aplicacionparacelular.ui.myteo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentConnectionBinding
import com.example.aplicacionparacelular.myteo.ConnectionPhase
import com.example.aplicacionparacelular.myteo.ConnectionRules
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotApiClient
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.network.RobotDiscovery
import com.example.aplicacionparacelular.ui.SystemBarInsets

class ConnectionFragment : Fragment() {
    private var _binding: FragmentConnectionBinding? = null
    private val binding get() = _binding!!
    private var awaitingDiscovery = false
    private var checking = false
    private var searched = false
    private var failed = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.connectionScroll)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnSearch.setOnClickListener { search() }
        binding.btnWifi.setOnClickListener { openWifi() }
        binding.btnDiagnostics.setOnClickListener { findNavController().navigate(R.id.nav_diagnostics) }

        RobotConnectionManager.isConnected.observe(viewLifecycleOwner) {
            if (it == true) {
                failed = false
                checking = false
            }
            render()
            if (it == true) loadProfile()
        }
        RobotConnectionManager.lastError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank() && checking && RobotConnectionManager.isConnected.value != true) {
                checking = false
                failed = true
                render()
            }
        }
        RobotDiscovery.isScanning.observe(viewLifecycleOwner) { render() }
        RobotDiscovery.scanError.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank() && awaitingDiscovery && RobotDiscovery.isScanning.value != true) {
                failed = true
                awaitingDiscovery = false
            }
            render()
        }
        RobotDiscovery.discoveredRobot.observe(viewLifecycleOwner) { robot ->
            if (!awaitingDiscovery || robot == null) return@observe
            awaitingDiscovery = false
            checking = true
            if (robot.pairingToken.isNotBlank()) {
                RobotApiClient.setPairingToken(requireContext(), robot.pairingToken)
            }
            RobotConnectionManager.connectManually(requireContext(), robot.ip, robot.port)
            render()
        }
        render()
        if (RobotConnectionManager.isConnected.value == true) loadProfile()
    }

    private fun search() {
        searched = true
        failed = false
        awaitingDiscovery = true
        RobotDiscovery.startScan(requireContext())
        render()
    }

    private fun openWifi() {
        if (RobotConnectionManager.isConnected.value == true) {
            findNavController().navigate(R.id.nav_wifi_change)
        } else {
            findNavController().navigate(R.id.nav_wifi_setup, bundleOf(FROM_MY_TEO to true))
        }
    }

    private fun loadProfile() {
        RobotConnectionManager.fetchWifiNetworks { result ->
            if (_binding == null) return@fetchWifiNetworks
            when (result) {
                is ApiResult.Success -> {
                    val profile = ConnectionRules.profileLabel(ConnectionRules.parseNetworks(result.data).first)
                    binding.txtProfile.text = if (profile.isEmpty()) {
                        getString(R.string.connection_profile_missing)
                    } else {
                        getString(R.string.connection_profile_named, profile)
                    }
                    binding.txtProfileHelp.isVisible = profile.isNotEmpty()
                }
                is ApiResult.Error -> {
                    binding.txtProfile.setText(R.string.connection_profile_error)
                    binding.txtProfileHelp.isVisible = false
                }
            }
        }
    }

    private fun render() {
        if (_binding == null) return
        val connected = RobotConnectionManager.isConnected.value == true
        val searching = RobotDiscovery.isScanning.value == true
        val phase = ConnectionRules.phase(connected, searching, failed)
        binding.txtPhase.setText(
            when (phase) {
                ConnectionPhase.CONNECTED -> R.string.connection_connected
                ConnectionPhase.DISCONNECTED -> R.string.connection_disconnected
                ConnectionPhase.SEARCHING -> R.string.connection_searching
                ConnectionPhase.FAILED -> R.string.connection_failed
            },
        )
        binding.txtLive.setText(
            when {
                connected -> R.string.connection_live
                checking -> R.string.connection_found
                else -> R.string.connection_disconnected
            },
        )
        val saved = RobotApiClient.getRobotIp(requireContext())
        binding.txtSaved.text = if (saved.isBlank()) {
            getString(R.string.connection_no_address)
        } else {
            getString(R.string.connection_saved, saved)
        }
        binding.progressSearch.isVisible = phase == ConnectionPhase.SEARCHING
        binding.btnSearch.setText(
            if (connected || searched) R.string.connection_search_again else R.string.connection_search,
        )
        binding.btnSearch.isEnabled = phase != ConnectionPhase.SEARCHING
        binding.btnWifi.setText(
            if (connected) R.string.connection_change_wifi else R.string.connection_ble,
        )
        if (!connected) {
            binding.txtProfile.setText(R.string.connection_profile_missing)
            binding.txtProfileHelp.isVisible = false
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val FROM_MY_TEO = "fromMyTeo"
    }
}
