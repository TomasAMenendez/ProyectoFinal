package com.example.aplicacionparacelular.ui.myteo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentWifiChangeBinding
import com.example.aplicacionparacelular.databinding.ItemWifiNetworkBinding
import com.example.aplicacionparacelular.myteo.ConnectionRules
import com.example.aplicacionparacelular.myteo.WifiChoice
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.onboarding.WifiDraft
import com.example.aplicacionparacelular.ui.SystemBarInsets
import com.example.aplicacionparacelular.ui.onboarding.WifiNetworkRow

class WifiChangeFragment : Fragment() {
    private var _binding: FragmentWifiChangeBinding? = null
    private val binding get() = _binding!!
    private var choices: List<WifiChoice> = emptyList()
    private var selectedIndex = -1
    private var sending = false
    private var draftSsid: String? = null
    private var draftPassword: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWifiChangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.wifiScroll)
        draftSsid = savedInstanceState?.getString(KEY_SSID)
        draftPassword = savedInstanceState?.getString(KEY_PASSWORD)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnRefresh.setOnClickListener { load() }
        binding.btnSend.setOnClickListener { send() }
        if (choices.isEmpty()) load()
    }

    private fun load() {
        if (sending) return
        binding.progressWifi.isVisible = true
        binding.btnRefresh.isEnabled = false
        binding.btnSend.isEnabled = false
        binding.txtWifiError.isVisible = false
        RobotConnectionManager.fetchWifiNetworks { result ->
            if (_binding == null) return@fetchWifiNetworks
            binding.progressWifi.isVisible = false
            binding.btnRefresh.isEnabled = true
            when (result) {
                is ApiResult.Success -> show(ConnectionRules.parseNetworks(result.data).second)
                is ApiResult.Error -> {
                    binding.txtWifiError.setText(R.string.config_change_wifi_error)
                    binding.txtWifiError.isVisible = true
                }
            }
        }
    }

    private fun show(items: List<WifiChoice>) {
        val previous = selected()
        val typed = binding.editPassword.text?.toString().orEmpty().ifEmpty { draftPassword.orEmpty() }
        choices = items
        binding.wifiChoices.removeAllViews()
        if (items.isEmpty()) {
            binding.txtWifiError.setText(R.string.config_change_wifi_empty)
            binding.txtWifiError.isVisible = true
            binding.cardPassword.isVisible = false
            binding.btnSend.isEnabled = false
            selectedIndex = -1
            return
        }
        val keep = items.indexOfFirst { it.ssid == (draftSsid ?: previous?.ssid) }
        selectedIndex = if (keep >= 0) keep else 0
        val inflater = LayoutInflater.from(requireContext())
        items.forEachIndexed { index, choice ->
            val item = ItemWifiNetworkBinding.inflate(inflater, binding.wifiChoices, false)
            WifiNetworkRow.bind(item, choice.ssid, choice.secure, index == selectedIndex)
            item.root.setOnClickListener {
                if (sending) return@setOnClickListener
                val previousSsid = selected()?.ssid
                selectedIndex = index
                if (previousSsid != choice.ssid) binding.editPassword.text?.clear()
                draftSsid = choice.ssid
                draftPassword = binding.editPassword.text?.toString()
                showSelection()
                bindChoiceRows()
            }
            binding.wifiChoices.addView(item.root)
        }
        val keptSsid = items.getOrNull(selectedIndex)?.ssid
        val password = WifiDraft.passwordAfterRefresh(draftSsid ?: previous?.ssid, keptSsid, typed)
        binding.editPassword.setText(password)
        draftSsid = keptSsid
        draftPassword = password
        showSelection()
    }

    private fun bindChoiceRows() {
        for (index in 0 until binding.wifiChoices.childCount) {
            val row = binding.wifiChoices.getChildAt(index)
            val choice = choices.getOrNull(index) ?: continue
            WifiNetworkRow.bind(ItemWifiNetworkBinding.bind(row), choice.ssid, choice.secure, index == selectedIndex)
        }
    }

    private fun selected(): WifiChoice? = choices.getOrNull(selectedIndex)

    private fun showSelection() {
        val choice = selected()
        val secure = choice?.secure == true
        binding.cardPassword.isVisible = choice != null
        binding.tilPassword.isVisible = choice != null && secure
        binding.txtOpen.isVisible = choice != null && !secure
        if (choice != null) {
            binding.txtPasswordLabel.text = getString(R.string.wifi_setup_password_title, choice.ssid)
        }
        binding.btnSend.isEnabled = choice != null && !sending
        draftSsid = choice?.ssid
    }

    private fun send() {
        val choice = selected() ?: return
        if (sending) return
        sending = true
        binding.btnSend.isEnabled = false
        binding.txtWifiError.isVisible = false
        val password = if (choice.secure) binding.editPassword.text?.toString().orEmpty() else ""
        RobotConnectionManager.connectWifi(choice.ssid, password) { result ->
            if (_binding == null) return@connectWifi
            sending = false
            when (result) {
                is ApiResult.Success -> {
                    RobotConnectionManager.forgetAndRediscover(requireContext())
                    findNavController().popBackStack()
                }
                is ApiResult.Error -> {
                    val auth = result.message.contains("clave", ignoreCase = true)
                    binding.txtWifiError.setText(
                        if (auth) R.string.config_change_wifi_auth else R.string.config_change_wifi_error,
                    )
                    binding.txtWifiError.isVisible = true
                    binding.btnSend.isEnabled = true
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (_binding != null) {
            outState.putString(KEY_SSID, selected()?.ssid ?: draftSsid)
            outState.putString(KEY_PASSWORD, binding.editPassword.text?.toString() ?: draftPassword)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val KEY_SSID = "wifi_draft_ssid"
        private const val KEY_PASSWORD = "wifi_draft_password"
    }
}
