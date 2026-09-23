package com.example.aplicacionparacelular.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.aplicacionparacelular.MainActivity
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentOnboardingRetryBinding
import com.example.aplicacionparacelular.network.RobotDiscovery

/**
 * Pantalla bloqueante para cuando no hay ni LAN ni un peluche visible por BLE.
 *
 * Reintentar dispara a la vez el descubrimiento UDP y un scan BLE de 8 s; el
 * gate de [MainActivity] se encarga de sacarnos de acá cuando alguno responda.
 */
class OnboardingRetryFragment : Fragment() {

    private var _binding: FragmentOnboardingRetryBinding? = null
    private val binding get() = _binding!!

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

        RobotDiscovery.isScanning.observe(viewLifecycleOwner) { scanning ->
            binding.progressRetry.visibility = if (scanning) View.VISIBLE else View.GONE
            binding.btnRetryDiscovery.isEnabled = !scanning
            binding.btnRetryDiscovery.text = getString(
                if (scanning) R.string.onboarding_retry_searching else R.string.onboarding_retry_button,
            )
        }

        retry()
    }

    private fun retry() {
        RobotDiscovery.startScan(requireContext())
        (activity as? MainActivity)?.refreshBlePresence()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
