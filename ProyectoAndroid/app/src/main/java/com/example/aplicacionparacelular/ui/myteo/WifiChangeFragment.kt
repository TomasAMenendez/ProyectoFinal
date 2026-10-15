package com.example.aplicacionparacelular.ui.myteo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentWifiChangeBinding
import com.example.aplicacionparacelular.myteo.ConnectionRules
import com.example.aplicacionparacelular.myteo.WifiChoice
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotConnectionManager
import com.example.aplicacionparacelular.ui.SystemBarInsets

class WifiChangeFragment : Fragment() {
    private var _binding: FragmentWifiChangeBinding? = null
    private val binding get() = _binding!!
    private var choices: List<WifiChoice> = emptyList()
    private var sending = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWifiChangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SystemBarInsets.applyVertical(binding.wifiScroll)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnRefresh.setOnClickListener { load() }
        binding.wifiChoices.setOnCheckedChangeListener { _, _ -> showSelection() }
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
        choices = items
        binding.wifiChoices.removeAllViews()
        if (items.isEmpty()) {
            binding.txtWifiError.setText(R.string.config_change_wifi_empty)
            binding.txtWifiError.isVisible = true
            binding.btnSend.isEnabled = false
            return
        }
        items.forEachIndexed { index, choice ->
            val button = RadioButton(requireContext())
            button.id = View.generateViewId()
            button.text = choice.ssid
            button.minHeight = (48 * resources.displayMetrics.density).toInt()
            button.setTextColor(requireContext().getColor(R.color.teo_text))
            button.buttonTintList = requireContext().getColorStateList(R.color.teo_checkbox)
            button.tag = index
            binding.wifiChoices.addView(button)
        }
        binding.wifiChoices.check(binding.wifiChoices.getChildAt(0).id)
        showSelection()
    }

    private fun selected(): WifiChoice? {
        val button = binding.wifiChoices.findViewById<RadioButton>(binding.wifiChoices.checkedRadioButtonId)
        val index = button?.tag as? Int ?: return null
        return choices.getOrNull(index)
    }

    private fun showSelection() {
        val choice = selected()
        val secure = choice?.secure != false
        binding.tilPassword.isVisible = choice != null && secure
        binding.txtOpen.isVisible = choice != null && !secure
        binding.btnSend.isEnabled = choice != null && !sending
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
