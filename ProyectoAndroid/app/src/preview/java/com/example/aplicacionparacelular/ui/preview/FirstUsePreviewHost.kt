package com.example.aplicacionparacelular.ui.preview

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentOnboardingRetryBinding
import com.example.aplicacionparacelular.databinding.FragmentWifiSetupBinding
import com.example.aplicacionparacelular.databinding.ItemWifiNetworkBinding
import com.example.aplicacionparacelular.onboarding.FindTeoPhase
import com.example.aplicacionparacelular.onboarding.TeoBleUuids
import com.example.aplicacionparacelular.onboarding.WifiNetworkUi
import com.example.aplicacionparacelular.ui.SystemBarInsets
import com.example.aplicacionparacelular.ui.dashboard.TeoCharacterPresenter
import com.example.aplicacionparacelular.ui.onboarding.FindTeoScreen
import com.example.aplicacionparacelular.ui.onboarding.WifiNetworkRow
import com.google.android.material.snackbar.Snackbar

enum class FirstUsePreviewScenario(val labelRes: Int) {
    SEARCHING(R.string.preview_first_searching),
    MISSED(R.string.preview_first_missed),
    BLUETOOTH_OFF(R.string.preview_first_bluetooth),
    PERMISSION(R.string.preview_first_permission),
    BLOCKED(R.string.preview_first_blocked),
    PIN(R.string.preview_first_pin),
    PIN_REJECTED(R.string.preview_first_pin_rejected),
    PIN_EXPIRED(R.string.preview_first_pin_expired),
    NETWORKS(R.string.preview_first_networks),
    OPEN(R.string.preview_first_open),
    EMPTY(R.string.preview_first_empty),
    NETWORK_ERROR(R.string.preview_first_network_error),
    SENDING(R.string.preview_first_sending),
    CHECKING(R.string.preview_first_checking),
}

class FirstUsePreviewHost(
    private val activity: AppCompatActivity,
    parent: FrameLayout,
    private val onBack: () -> Unit,
    private val onChanged: () -> Unit,
) {
    private val retry = FragmentOnboardingRetryBinding.inflate(LayoutInflater.from(activity), parent, false)
    private val wifi = FragmentWifiSetupBinding.inflate(LayoutInflater.from(activity), parent, false)
    val root: FrameLayout = FrameLayout(activity).apply {
        addView(retry.root)
        addView(wifi.root)
        visibility = View.GONE
    }
    var scenario = FirstUsePreviewScenario.MISSED
    val scenarioLabel: String get() = activity.getString(scenario.labelRes)
    val visible: Boolean get() = root.visibility == View.VISIBLE
    private var selectedSsid = "Casa"

    init {
        parent.addView(root)
        SystemBarInsets.applyBottom(retry.root)
        SystemBarInsets.applyBottom(wifi.root)
        retry.btnRetryDiscovery.setOnClickListener {
            if (scenario == FirstUsePreviewScenario.SEARCHING) return@setOnClickListener
            choose(FirstUsePreviewScenario.SEARCHING)
        }
        retry.btnRetryBluetoothSettings.setOnClickListener { simulated() }
        retry.btnRetryAppSettings.setOnClickListener { simulated() }
        wifi.btnBack.isVisible = false
        wifi.editWifiPin.doAfterTextChanged {
            wifi.btnVerifyPin.isEnabled = TeoBleUuids.isValidPin(it?.toString().orEmpty())
        }
        wifi.btnVerifyPin.setOnClickListener {
            if (!TeoBleUuids.isValidPin(wifi.editWifiPin.text?.toString().orEmpty())) return@setOnClickListener
            simulated()
            choose(FirstUsePreviewScenario.NETWORKS)
        }
        wifi.btnRefreshNetworks.setOnClickListener { choose(FirstUsePreviewScenario.EMPTY) }
        wifi.btnWifiConnect.setOnClickListener {
            simulated()
            choose(FirstUsePreviewScenario.SENDING)
        }
        wifi.btnWifiRetry.setOnClickListener { choose(FirstUsePreviewScenario.PIN) }
        wifi.btnWifiBluetoothSettings.setOnClickListener { simulated() }
        wifi.btnWifiAppSettings.setOnClickListener { simulated() }
        TeoCharacterPresenter.applyWave(wifi.imgCharacter, null)
    }

    fun choose(next: FirstUsePreviewScenario) {
        scenario = next
        render()
        onChanged()
    }

    fun render() {
        val finding = scenario in setOf(
            FirstUsePreviewScenario.SEARCHING,
            FirstUsePreviewScenario.MISSED,
            FirstUsePreviewScenario.BLUETOOTH_OFF,
            FirstUsePreviewScenario.PERMISSION,
            FirstUsePreviewScenario.BLOCKED,
        )
        retry.root.isVisible = finding
        wifi.root.isVisible = !finding
        if (finding) {
            val phase = when (scenario) {
                FirstUsePreviewScenario.SEARCHING -> FindTeoPhase.SEARCHING
                FirstUsePreviewScenario.BLUETOOTH_OFF -> FindTeoPhase.BLUETOOTH_OFF
                FirstUsePreviewScenario.PERMISSION -> FindTeoPhase.PERMISSION_NEEDED
                FirstUsePreviewScenario.BLOCKED -> FindTeoPhase.PERMISSION_BLOCKED
                else -> FindTeoPhase.NOT_FOUND
            }
            FindTeoScreen.render(retry, phase)
            return
        }
        renderWifi()
    }

    private fun renderWifi() {
        val pin = scenario == FirstUsePreviewScenario.PIN ||
            scenario == FirstUsePreviewScenario.PIN_REJECTED ||
            scenario == FirstUsePreviewScenario.PIN_EXPIRED
        val networks = scenario == FirstUsePreviewScenario.NETWORKS ||
            scenario == FirstUsePreviewScenario.OPEN ||
            scenario == FirstUsePreviewScenario.EMPTY ||
            scenario == FirstUsePreviewScenario.NETWORK_ERROR ||
            scenario == FirstUsePreviewScenario.SENDING
        wifi.imgCharacter.isVisible = pin || scenario == FirstUsePreviewScenario.PIN_EXPIRED ||
            scenario == FirstUsePreviewScenario.CHECKING
        wifi.txtStepSubtitle.setText(
            if (networks) R.string.wifi_setup_network_subtitle else R.string.wifi_setup_pin_subtitle,
        )
        wifi.chipBle.isVisible = scenario != FirstUsePreviewScenario.PIN_EXPIRED &&
            scenario != FirstUsePreviewScenario.CHECKING
        wifi.sectionPin.isVisible = scenario == FirstUsePreviewScenario.PIN ||
            scenario == FirstUsePreviewScenario.PIN_REJECTED
        wifi.txtPinNext.isVisible = wifi.sectionPin.isVisible
        wifi.sectionNetworks.isVisible = networks
        wifi.txtSameNetwork.isVisible = networks
        wifi.progressWifi.isVisible = scenario == FirstUsePreviewScenario.SENDING ||
            scenario == FirstUsePreviewScenario.CHECKING
        wifi.btnWifiRetry.isVisible = scenario == FirstUsePreviewScenario.PIN_EXPIRED ||
            scenario == FirstUsePreviewScenario.NETWORK_ERROR
        wifi.btnWifiBluetoothSettings.isVisible = false
        wifi.btnWifiAppSettings.isVisible = false
        wifi.txtWifiError.isVisible = scenario == FirstUsePreviewScenario.PIN_REJECTED ||
            scenario == FirstUsePreviewScenario.PIN_EXPIRED ||
            scenario == FirstUsePreviewScenario.NETWORK_ERROR
        wifi.txtWifiError.setText(
            when (scenario) {
                FirstUsePreviewScenario.PIN_REJECTED -> R.string.wifi_setup_error_pin
                FirstUsePreviewScenario.PIN_EXPIRED -> R.string.wifi_setup_error_expired
                else -> R.string.wifi_setup_error_generic
            },
        )
        wifi.txtWifiStatus.isVisible = scenario == FirstUsePreviewScenario.SENDING ||
            scenario == FirstUsePreviewScenario.CHECKING ||
            scenario == FirstUsePreviewScenario.OPEN
        wifi.txtWifiStatus.setText(
            when (scenario) {
                FirstUsePreviewScenario.CHECKING -> R.string.wifi_setup_done
                FirstUsePreviewScenario.OPEN -> R.string.wifi_setup_connecting_generic
                else -> R.string.wifi_setup_connecting_generic
            },
        )
        val showPassword = scenario == FirstUsePreviewScenario.NETWORKS ||
            scenario == FirstUsePreviewScenario.SENDING
        wifi.sectionPassword.isVisible = showPassword
        wifi.btnWifiConnect.isVisible = showPassword
        wifi.btnWifiConnect.isEnabled = scenario != FirstUsePreviewScenario.SENDING
        if (scenario == FirstUsePreviewScenario.NETWORKS && wifi.editWifiPassword.text.isNullOrEmpty()) {
            wifi.editWifiPassword.setText("ejemplo")
        }
        if (scenario == FirstUsePreviewScenario.NETWORKS) {
            wifi.txtPasswordTitle.text = activity.getString(R.string.wifi_setup_password_title, selectedSsid)
        }
        bindNetworks()
    }

    private fun bindNetworks() {
        val open = scenario == FirstUsePreviewScenario.OPEN
        val items = when (scenario) {
            FirstUsePreviewScenario.EMPTY -> emptyList()
            FirstUsePreviewScenario.OPEN -> listOf(
                WifiNetworkUi("Casa", 80, true),
                WifiNetworkUi("Plaza", 55, false),
            )
            FirstUsePreviewScenario.NETWORKS,
            FirstUsePreviewScenario.NETWORK_ERROR,
            FirstUsePreviewScenario.SENDING,
            -> listOf(
                WifiNetworkUi("Casa", 80, true),
                WifiNetworkUi("Casa invitados", 62, true),
                WifiNetworkUi("Otra red", 40, true),
            )
            else -> emptyList()
        }
        if (open) selectedSsid = "Plaza"
        wifi.containerNetworks.removeAllViews()
        wifi.txtNetworksEmpty.isVisible = items.isEmpty() && scenario == FirstUsePreviewScenario.EMPTY
        val inflater = LayoutInflater.from(activity)
        for (network in items) {
            val item = ItemWifiNetworkBinding.inflate(inflater, wifi.containerNetworks, false)
            WifiNetworkRow.bind(item, network.ssid, network.secure, network.ssid == selectedSsid)
            item.root.setOnClickListener {
                selectedSsid = network.ssid
                if (network.secure) {
                    choose(FirstUsePreviewScenario.NETWORKS)
                } else {
                    simulated()
                    choose(FirstUsePreviewScenario.OPEN)
                }
            }
            wifi.containerNetworks.addView(item.root)
        }
    }

    private fun simulated() {
        Snackbar.make(root, R.string.preview_first_simulated, Snackbar.LENGTH_SHORT).show()
    }

    fun close() {
        root.visibility = View.GONE
    }
}
