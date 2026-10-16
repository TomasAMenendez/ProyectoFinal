package com.example.aplicacionparacelular.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentConnectionBinding
import com.example.aplicacionparacelular.databinding.FragmentDiagnosticsBinding
import com.example.aplicacionparacelular.databinding.FragmentMyTeoBinding
import com.example.aplicacionparacelular.databinding.FragmentProfileEditBinding
import com.example.aplicacionparacelular.databinding.FragmentTeoSettingsBinding
import com.example.aplicacionparacelular.databinding.FragmentWifiChangeBinding
import com.example.aplicacionparacelular.databinding.ItemWifiNetworkBinding
import com.example.aplicacionparacelular.myteo.ConnectionPhase
import com.example.aplicacionparacelular.myteo.LoadState
import com.example.aplicacionparacelular.myteo.ManualAddress
import com.example.aplicacionparacelular.myteo.WifiChoice
import com.example.aplicacionparacelular.onboarding.ChildProfile
import com.example.aplicacionparacelular.ui.SystemBarInsets
import com.example.aplicacionparacelular.ui.onboarding.ChildProfileForm
import com.example.aplicacionparacelular.ui.onboarding.WifiNetworkRow
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject

enum class MyTeoPreviewChoice(val labelRes: Int) {
    SETTINGS_READY(R.string.preview_my_teo_settings_ready),
    SETTINGS_LOADING(R.string.preview_my_teo_settings_loading),
    SETTINGS_FAILED(R.string.preview_my_teo_settings_failed),
    CONNECTED(R.string.preview_my_teo_connected),
    DISCONNECTED(R.string.preview_my_teo_disconnected),
    SEARCHING(R.string.preview_my_teo_searching),
    LINK_FAILED(R.string.preview_my_teo_link_failed),
}

class MyTeoPreviewHost(
    private val activity: FragmentActivity,
    private val stage: FrameLayout,
    private val onCostumeSaved: (String) -> Unit,
) {
    private enum class Screen { MENU, SETTINGS, CONNECTION, WIFI, DIAGNOSTICS, PROFILE }

    private val menu = FragmentMyTeoBinding.inflate(activity.layoutInflater, stage, true)
    private val settings = FragmentTeoSettingsBinding.inflate(activity.layoutInflater, stage, true)
    private val connection = FragmentConnectionBinding.inflate(activity.layoutInflater, stage, true)
    private val wifi = FragmentWifiChangeBinding.inflate(activity.layoutInflater, stage, true)
    private val diagnostics = FragmentDiagnosticsBinding.inflate(activity.layoutInflater, stage, true)
    private val profile = FragmentProfileEditBinding.inflate(activity.layoutInflater, stage, true)
    private val profileForm = ChildProfileForm(profile.form)
    private val stack = ArrayDeque<Screen>()
    private val networks = listOf(
        WifiChoice("Jardin de ejemplo", true),
        WifiChoice("Invitados", true),
        WifiChoice("Biblioteca abierta", false),
    )
    private var wifiIndex = 0

    private var rendering = false
    private var settingsLoad = LoadState.READY
    private var volume = 40
    private var brightness = 70
    private var night = false
    private var phase = ConnectionPhase.CONNECTED
    private var confirmedProfile = ChildProfile()
    val costume: String get() = confirmedProfile.costume
    var visible: Boolean = false
        private set

    val scenarioLabel: String
        get() = activity.getString(
            when {
                settingsLoad == LoadState.LOADING -> MyTeoPreviewChoice.SETTINGS_LOADING.labelRes
                settingsLoad == LoadState.FAILED -> MyTeoPreviewChoice.SETTINGS_FAILED.labelRes
                phase == ConnectionPhase.DISCONNECTED -> MyTeoPreviewChoice.DISCONNECTED.labelRes
                phase == ConnectionPhase.SEARCHING -> MyTeoPreviewChoice.SEARCHING.labelRes
                phase == ConnectionPhase.FAILED -> MyTeoPreviewChoice.LINK_FAILED.labelRes
                else -> MyTeoPreviewChoice.CONNECTED.labelRes
            },
        )

    init {
        listOf(menu, settings, connection, wifi, diagnostics, profile).forEach { binding ->
            binding.root.isVisible = false
            SystemBarInsets.applyBottom(binding.root)
        }
        markExample(menu.txtExample, settings.txtExample, connection.txtExample, wifi.txtExample, diagnostics.txtExample, profile.txtExample)
        menu.txtVersion.setText(R.string.preview_my_teo_version)
        menu.btnBack.setOnClickListener { back() }
        menu.cardSettings.setOnClickListener { push(Screen.SETTINGS) }
        menu.cardConnection.setOnClickListener { push(Screen.CONNECTION) }
        menu.cardProfile.setOnClickListener { push(Screen.PROFILE) }
        settings.btnBack.setOnClickListener { back() }
        settings.sliderVolume.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || settingsLoad != LoadState.READY) return@addOnChangeListener
            volume = value.toInt()
            renderSettings()
        }
        settings.sliderBrightness.addOnChangeListener { _, value, fromUser ->
            if (!fromUser || settingsLoad != LoadState.READY) return@addOnChangeListener
            brightness = value.toInt()
            renderSettings()
        }
        settings.btnApply.setOnClickListener {
            if (settingsLoad != LoadState.READY) return@setOnClickListener
            settings.txtStatus.setText(R.string.preview_my_teo_applied)
        }
        settings.switchNight.setOnCheckedChangeListener { _, checked ->
            if (rendering || settingsLoad != LoadState.READY) return@setOnCheckedChangeListener
            night = checked
            settings.txtStatus.setText(
                if (checked) R.string.preview_my_teo_night_on else R.string.preview_my_teo_night_off,
            )
        }
        connection.btnBack.setOnClickListener { back() }
        connection.btnSearch.setOnClickListener {
            phase = ConnectionPhase.SEARCHING
            renderConnection()
            connection.root.postDelayed({
                if (phase == ConnectionPhase.SEARCHING) {
                    phase = ConnectionPhase.CONNECTED
                    renderConnection()
                }
            }, 700)
        }
        connection.btnWifi.setOnClickListener {
            if (phase == ConnectionPhase.CONNECTED) push(Screen.WIFI) else {
                Snackbar.make(connection.root, R.string.preview_my_teo_ble, Snackbar.LENGTH_LONG).show()
            }
        }
        connection.btnDiagnostics.setOnClickListener { push(Screen.DIAGNOSTICS) }
        wifi.btnBack.setOnClickListener { back() }
        wifi.btnRefresh.setOnClickListener { fillNetworks() }
        wifi.btnSend.setOnClickListener {
            phase = ConnectionPhase.SEARCHING
            Snackbar.make(wifi.root, R.string.preview_my_teo_wifi_sent, Snackbar.LENGTH_LONG).show()
            back()
        }
        diagnostics.btnBack.setOnClickListener { back() }
        diagnostics.btnConnect.setOnClickListener { simulateManual() }
        profile.btnBack.setOnClickListener { back() }
        profile.form.btnSave.setText(R.string.profile_save_changes)
        profileForm.bind { profileForm.refreshSaveEnabled() }
        profileForm.populate(confirmedProfile)
        profile.form.btnSave.setOnClickListener {
            if (!profileForm.read().canSubmit) return@setOnClickListener
            confirmedProfile = profileForm.read().sanitize().copy(complete = true)
            profileForm.markClean()
            profileForm.populate(confirmedProfile)
            onCostumeSaved(costume)
            Snackbar.make(profile.root, R.string.preview_my_teo_profile_saved, Snackbar.LENGTH_LONG).show()
        }
        fillNetworks()
    }

    fun openMenu() = open(Screen.MENU)

    fun openConnection() = open(Screen.CONNECTION)

    fun choose(choice: MyTeoPreviewChoice) {
        when (choice) {
            MyTeoPreviewChoice.SETTINGS_READY -> settingsLoad = LoadState.READY
            MyTeoPreviewChoice.SETTINGS_LOADING -> settingsLoad = LoadState.LOADING
            MyTeoPreviewChoice.SETTINGS_FAILED -> settingsLoad = LoadState.FAILED
            MyTeoPreviewChoice.CONNECTED -> phase = ConnectionPhase.CONNECTED
            MyTeoPreviewChoice.DISCONNECTED -> phase = ConnectionPhase.DISCONNECTED
            MyTeoPreviewChoice.SEARCHING -> phase = ConnectionPhase.SEARCHING
            MyTeoPreviewChoice.LINK_FAILED -> phase = ConnectionPhase.FAILED
        }
        renderSettings()
        renderConnection()
        renderDiagnostics()
    }

    fun close() {
        hide()
    }

    fun back(): Boolean {
        if (!visible) return false
        if (stack.size > 1) {
            stack.removeLast()
            showTop()
            return true
        }
        hide()
        return true
    }

    fun persist(outState: Bundle) {
        outState.putBoolean(KEY_VISIBLE, visible)
        outState.putString(KEY_SCREEN, stack.lastOrNull()?.name)
        outState.putInt(KEY_VOLUME, volume)
        outState.putInt(KEY_BRIGHTNESS, brightness)
        outState.putBoolean(KEY_NIGHT, night)
        outState.putString(KEY_PHASE, phase.name)
        outState.putString(KEY_LOAD, settingsLoad.name)
        outState.putString(KEY_PROFILE_DRAFT, profileForm.payload().toString())
        outState.putString(KEY_PROFILE_CONFIRMED, JSONObject().apply {
            put("name", confirmedProfile.name)
            put("gender", confirmedProfile.gender)
            put("interests", confirmedProfile.interests)
            put("extra", confirmedProfile.extra)
            put("costume", confirmedProfile.costume)
            put("complete", true)
        }.toString())
    }

    fun restore(saved: Bundle?) {
        if (saved == null) return
        saved.getString(KEY_PROFILE_CONFIRMED)?.let {
            confirmedProfile = ChildProfile.parse(JSONObject(it))
        }
        saved.getString(KEY_PROFILE_DRAFT)?.let {
            profileForm.restoreDraft(ChildProfile.parse(JSONObject(it)))
        }
        onCostumeSaved(costume)
        volume = saved.getInt(KEY_VOLUME, volume)
        brightness = saved.getInt(KEY_BRIGHTNESS, brightness)
        night = saved.getBoolean(KEY_NIGHT, night)
        phase = ConnectionPhase.entries.firstOrNull { it.name == saved.getString(KEY_PHASE) } ?: phase
        settingsLoad = LoadState.entries.firstOrNull { it.name == saved.getString(KEY_LOAD) } ?: settingsLoad
        if (!saved.getBoolean(KEY_VISIBLE)) return
        val screen = Screen.entries.firstOrNull { it.name == saved.getString(KEY_SCREEN) } ?: Screen.MENU
        open(screen)
    }

    private fun open(screen: Screen) {
        stack.clear()
        stack.add(screen)
        visible = true
        showTop()
    }

    private fun push(screen: Screen) {
        if (screen == Screen.PROFILE) {
            profileForm.markClean()
            profileForm.populate(confirmedProfile)
        }
        stack.add(screen)
        showTop()
    }

    private fun showTop() {
        val screen = stack.last()
        menu.root.isVisible = screen == Screen.MENU
        settings.root.isVisible = screen == Screen.SETTINGS
        connection.root.isVisible = screen == Screen.CONNECTION
        wifi.root.isVisible = screen == Screen.WIFI
        diagnostics.root.isVisible = screen == Screen.DIAGNOSTICS
        profile.root.isVisible = screen == Screen.PROFILE
        renderSettings()
        renderConnection()
        renderDiagnostics()
    }

    private fun hide() {
        visible = false
        stack.clear()
        listOf(menu.root, settings.root, connection.root, wifi.root, diagnostics.root, profile.root)
            .forEach { it.isVisible = false }
    }

    private fun renderSettings() {
        val ready = settingsLoad == LoadState.READY
        settings.sliderVolume.isEnabled = ready
        settings.sliderBrightness.isEnabled = ready
        settings.switchNight.isEnabled = ready
        settings.btnApply.isEnabled = ready
        if (ready) {
            rendering = true
            if (settings.sliderVolume.value.toInt() != volume) settings.sliderVolume.value = volume.toFloat()
            if (settings.sliderBrightness.value.toInt() != brightness) settings.sliderBrightness.value = brightness.toFloat()
            settings.txtVolume.text = activity.getString(R.string.settings_percent, volume)
            settings.txtBrightness.text = activity.getString(R.string.settings_percent, brightness)
            if (settings.switchNight.isChecked != night) settings.switchNight.isChecked = night
            rendering = false
            val status = settings.txtStatus.text?.toString().orEmpty()
            val loading = activity.getString(R.string.settings_loading)
            val failed = activity.getString(R.string.settings_failed)
            if (status.isBlank() || status == loading || status == failed) {
                settings.txtStatus.setText(R.string.my_teo_settings_hint)
            }
        } else {
            settings.txtVolume.setText(R.string.settings_unknown)
            settings.txtBrightness.setText(R.string.settings_unknown)
            settings.txtStatus.setText(
                if (settingsLoad == LoadState.LOADING) R.string.settings_loading else R.string.settings_failed,
            )
        }
    }

    private fun renderConnection() {
        connection.txtPhase.setText(
            when (phase) {
                ConnectionPhase.CONNECTED -> R.string.connection_connected
                ConnectionPhase.DISCONNECTED -> R.string.connection_disconnected
                ConnectionPhase.SEARCHING -> R.string.connection_searching
                ConnectionPhase.FAILED -> R.string.connection_failed
            },
        )
        connection.txtLive.setText(
            if (phase == ConnectionPhase.CONNECTED) R.string.connection_live else R.string.preview_my_teo_not_live,
        )
        connection.txtSaved.setText(R.string.preview_my_teo_saved_address)
        connection.progressSearch.isVisible = phase == ConnectionPhase.SEARCHING
        connection.btnSearch.setText(
            if (phase == ConnectionPhase.CONNECTED) R.string.connection_search_again else R.string.connection_search,
        )
        connection.btnWifi.setText(
            if (phase == ConnectionPhase.CONNECTED) R.string.connection_change_wifi else R.string.connection_ble,
        )
        if (phase == ConnectionPhase.CONNECTED) {
            connection.txtProfile.setText(R.string.preview_my_teo_profile)
            connection.txtProfileHelp.isVisible = true
        } else {
            connection.txtProfile.setText(R.string.connection_profile_missing)
            connection.txtProfileHelp.isVisible = false
        }
    }

    private fun renderDiagnostics() {
        val connected = phase == ConnectionPhase.CONNECTED
        diagnostics.txtStatus.text = activity.getString(
            R.string.diagnostics_status,
            activity.getString(if (connected) R.string.connection_connected else R.string.connection_disconnected),
        )
        diagnostics.txtPhone.setText(R.string.preview_my_teo_phone)
        diagnostics.txtAddress.setText(R.string.preview_my_teo_robot)
        diagnostics.txtPort.text = activity.getString(R.string.diagnostics_port, 9090)
    }

    private fun fillNetworks() {
        wifi.wifiChoices.removeAllViews()
        val inflater = LayoutInflater.from(activity)
        networks.forEachIndexed { index, choice ->
            val item = ItemWifiNetworkBinding.inflate(inflater, wifi.wifiChoices, false)
            WifiNetworkRow.bind(item, choice.ssid, choice.secure, index == wifiIndex)
            item.root.setOnClickListener {
                wifiIndex = index
                fillNetworks()
            }
            wifi.wifiChoices.addView(item.root)
        }
        showWifiSelection()
    }

    private fun showWifiSelection() {
        val choice = networks.getOrNull(wifiIndex)
        val secure = choice?.secure == true
        wifi.cardPassword.isVisible = choice != null
        wifi.tilPassword.isVisible = secure
        wifi.txtOpen.isVisible = choice != null && !secure
        if (choice != null) {
            wifi.txtPasswordLabel.text = activity.getString(R.string.wifi_setup_password_title, choice.ssid)
        }
        wifi.btnSend.isEnabled = choice != null
    }

    private fun simulateManual() {
        val typed = diagnostics.editIp.text?.toString().orEmpty()
        val problem = ManualAddress.problem(typed)
        if (problem != null) {
            diagnostics.tilIp.error = problem
            diagnostics.txtResult.text = problem
            return
        }
        diagnostics.tilIp.error = null
        val address = typed.trim()
        diagnostics.txtResult.text = activity.getString(R.string.preview_my_teo_manual_ok, address)
    }

    private fun markExample(vararg labels: android.widget.TextView) {
        labels.forEach {
            it.isVisible = true
            it.setText(R.string.preview_simulated)
        }
    }

    companion object {
        private const val KEY_VISIBLE = "preview_my_teo"
        private const val KEY_SCREEN = "preview_my_teo_screen"
        private const val KEY_VOLUME = "preview_my_teo_volume"
        private const val KEY_BRIGHTNESS = "preview_my_teo_brightness"
        private const val KEY_NIGHT = "preview_my_teo_night"
        private const val KEY_PHASE = "preview_my_teo_phase"
        private const val KEY_LOAD = "preview_my_teo_load"
        private const val KEY_PROFILE_DRAFT = "preview_profile_draft"
        private const val KEY_PROFILE_CONFIRMED = "preview_profile_confirmed"
    }
}
