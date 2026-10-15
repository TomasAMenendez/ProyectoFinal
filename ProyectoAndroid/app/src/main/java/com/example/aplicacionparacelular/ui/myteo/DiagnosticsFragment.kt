package com.example.aplicacionparacelular.ui.myteo

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentDiagnosticsBinding
import com.example.aplicacionparacelular.myteo.ManualAddress
import com.example.aplicacionparacelular.network.RobotApiClient
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.ui.SystemBarInsets
import java.net.Inet4Address
import java.net.NetworkInterface

class DiagnosticsFragment : Fragment() {
    private var _binding: FragmentDiagnosticsBinding? = null
    private val binding get() = _binding!!
    private var attempt: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiagnosticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.diagnosticsScroll)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnConnect.setOnClickListener { connect() }
        attempt = savedInstanceState?.getString(KEY_ATTEMPT)
        RobotConnectionManager.isConnected.observe(viewLifecycleOwner) { render() }
        RobotConnectionManager.lastError.observe(viewLifecycleOwner) { error ->
            if (attempt != null && !error.isNullOrBlank() && RobotConnectionManager.isConnected.value != true) {
                binding.progressManual.isVisible = false
                render()
            }
        }
        render()
    }

    private fun connect() {
        val typed = binding.editIp.text?.toString().orEmpty()
        val problem = ManualAddress.problem(typed)
        if (problem != null) {
            binding.tilIp.error = problem
            return
        }
        binding.tilIp.error = null
        attempt = typed.trim()
        binding.progressManual.isVisible = true
        binding.txtResult.text = getString(R.string.diagnostics_progress)
        val port = RobotApiClient.getRobotPort(requireContext())
        RobotConnectionManager.connectManually(requireContext(), attempt!!, port)
        render()
    }

    private fun render() {
        if (_binding == null) return
        val connected = RobotConnectionManager.isConnected.value == true
        val configured = RobotApiClient.getRobotIp(requireContext())
        val port = RobotApiClient.getRobotPort(requireContext())
        binding.txtStatus.text = getString(
            R.string.diagnostics_status,
            getString(if (connected) R.string.connection_connected else R.string.connection_disconnected),
        )
        val phone = phoneAddress()
        binding.txtPhone.text = if (phone == null) {
            getString(R.string.diagnostics_phone_missing)
        } else {
            getString(R.string.diagnostics_phone, phone)
        }
        binding.txtAddress.text = if (configured.isBlank()) {
            getString(R.string.diagnostics_address_missing)
        } else {
            getString(R.string.diagnostics_address, configured)
        }
        binding.txtPort.text = getString(R.string.diagnostics_port, port)
        val typed = attempt
        if (typed != null) {
            val lines = mutableListOf(
                getString(R.string.diagnostics_typed, typed),
                if (configured.isBlank()) {
                    getString(R.string.diagnostics_address_missing)
                } else {
                    getString(R.string.diagnostics_configured, configured)
                },
            )
            if (connected && configured == typed) {
                binding.progressManual.isVisible = false
                lines.add(getString(R.string.diagnostics_confirmed, typed))
            } else if (!connected && RobotConnectionManager.lastError.value != null && !binding.progressManual.isVisible) {
                lines.add(getString(R.string.diagnostics_rejected))
            }
            binding.txtResult.text = lines.joinToString("\n")
        }
    }

    private fun phoneAddress(): String? {
        try {
            @Suppress("DEPRECATION")
            val wifi = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifi?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    (ipInt shr 8) and 0xff,
                    (ipInt shr 16) and 0xff,
                    (ipInt shr 24) and 0xff,
                )
            }
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val addresses = interfaces.nextElement().inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        attempt?.let { outState.putString(KEY_ATTEMPT, it) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val KEY_ATTEMPT = "manual_attempt"
    }
}
